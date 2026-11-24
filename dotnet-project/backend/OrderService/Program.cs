using System.ComponentModel.DataAnnotations;
using System.Security.Cryptography;
using System.Text;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

var builder = WebApplication.CreateBuilder(args);
builder.WebHost.ConfigureKestrel(options => options.ListenLocalhost(8083));
var connectionString = builder.Configuration["ORDER_DB_URL"]
    ?? "Host=localhost;Port=5432;Database=sales_order_db;Username=postgres;Password=postgres";
builder.Services.AddDbContext<OrderDbContext>(options => options.UseNpgsql(connectionString));
builder.Services.AddHttpClient("products", client =>
    client.BaseAddress = new Uri(builder.Configuration["PRODUCTS_SERVICE_URL"] ?? "http://localhost:8082"));
builder.Services.AddControllers();
builder.Services.AddCors(options => options.AddDefaultPolicy(policy =>
    policy.WithOrigins("http://localhost:5173", "http://127.0.0.1:5173").AllowAnyHeader().AllowAnyMethod()));
var app = builder.Build();
app.UseCors();
app.MapControllers();
using (var scope = app.Services.CreateScope())
    scope.ServiceProvider.GetRequiredService<OrderDbContext>().Database.EnsureCreated();
app.Run();

public class OrderDbContext(DbContextOptions<OrderDbContext> options) : DbContext(options)
{
    public DbSet<OrderEntity> Orders => Set<OrderEntity>();
    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<OrderEntity>(e => {
            e.ToTable("customer_orders"); e.HasKey(x => x.Id);
            e.HasIndex(x => x.IdempotencyKey).IsUnique();
            e.Property(x => x.RequestFingerprint).HasMaxLength(64);
            e.Property(x => x.Status).HasConversion<string>().HasMaxLength(20);
            e.Property(x => x.Total).HasPrecision(12,2);
            e.OwnsMany(x => x.Items, i => {
                i.ToTable("order_items"); i.WithOwner().HasForeignKey("OrderId");
                i.Property<Guid>("OrderId"); i.Property<long>("Id"); i.HasKey("Id");
            });
        });
    }
}
public enum OrderStatus { PENDING, CONFIRMED, REJECTED }
public class OrderEntity
{
    public Guid Id { get; set; }
    public Guid IdempotencyKey { get; set; }
    public string RequestFingerprint { get; set; } = "";
    public long UserId { get; set; }
    public string CustomerName { get; set; } = "";
    public string CustomerEmail { get; set; } = "";
    public OrderStatus Status { get; set; } = OrderStatus.PENDING;
    public decimal Total { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public List<OrderItemSnapshot> Items { get; set; } = [];
}
public class OrderItemSnapshot
{
    public long ProductId { get; set; }
    public string ProductName { get; set; } = "";
    public int Quantity { get; set; }
    public decimal UnitPrice { get; set; }
}
public record OrderItemRequest([property: Required, Range(1,long.MaxValue)] long ProductId,
    [property: Required, Range(1,int.MaxValue)] int Quantity);
public record CreateOrderRequest([property: Required, Range(1,long.MaxValue)] long UserId,
    [property: Required, MaxLength(100)] string CustomerName,
    [property: Required, EmailAddress, MaxLength(254)] string CustomerEmail,
    [property: Required, MinLength(1)] List<OrderItemRequest> Items);
public record OrderItemResponse(long ProductId,string ProductName,int Quantity,decimal UnitPrice);
public record OrderResponse(Guid Id,long UserId,string CustomerName,string CustomerEmail,string Status,
    decimal Total,DateTime CreatedAt,List<OrderItemResponse> Items);

[ApiController]
[Route("api/v1/orders")]
public class OrdersController(OrderDbContext db,IHttpClientFactory httpClientFactory,IConfiguration configuration) : ControllerBase
{
    [HttpPost]
    public async Task<ActionResult<OrderResponse>> Create(
        [FromHeader(Name="Idempotency-Key")] Guid idempotencyKey,
        [FromBody] CreateOrderRequest request,CancellationToken ct)
    {
        var fingerprint=Fingerprint(request);
        var existing=await db.Orders.Include(x=>x.Items).FirstOrDefaultAsync(x=>x.IdempotencyKey==idempotencyKey,ct);
        if(existing is not null)
        {
            if(existing.RequestFingerprint!=fingerprint)
                return Conflict(new { message="La clave de idempotencia ya se usó con otra solicitud" });
            if(existing.Status==OrderStatus.CONFIRMED) return Ok(ToResponse(existing));
            if(existing.Status==OrderStatus.REJECTED)
                return Conflict(new { message="El pedido ya fue rechazado" });
            return await Confirm(existing,request,ct);
        }
        var order=new OrderEntity {
            Id=Guid.NewGuid(),IdempotencyKey=idempotencyKey,RequestFingerprint=fingerprint,
            UserId=request.UserId,CustomerName=request.CustomerName.Trim(),
            CustomerEmail=request.CustomerEmail.Trim().ToLowerInvariant(),Status=OrderStatus.PENDING
        };
        db.Orders.Add(order); await db.SaveChangesAsync(ct);
        return await Confirm(order,request,ct);
    }
    private async Task<ActionResult<OrderResponse>> Confirm(OrderEntity order,CreateOrderRequest request,CancellationToken ct)
    {
        try
        {
            var client=httpClientFactory.CreateClient("products");
            using var message=new HttpRequestMessage(HttpMethod.Post,"/internal/v1/inventory/reservations")
            {
                Content=JsonContent.Create(new {
                    reservationId=order.Id,
                    items=request.Items.Select(x=>new { productId=x.ProductId,quantity=x.Quantity })
                })
            };
            message.Headers.Add("X-Inventory-Service-Key",configuration["INVENTORY_SERVICE_API_KEY"] ?? "");
            using var response=await client.SendAsync(message,ct);
            if(!response.IsSuccessStatusCode)
            {
                if((int)response.StatusCode is 400 or 404 or 409)
                {
                    order.Status=OrderStatus.REJECTED; await db.SaveChangesAsync(ct);
                    return StatusCode((int)response.StatusCode,new { message="No se pudo reservar el inventario para este pedido" });
                }
                await db.SaveChangesAsync(ct);
                return StatusCode(503,new { message="El pedido está pendiente; inténtalo de nuevo más tarde" });
            }
            var reservation=await response.Content.ReadFromJsonAsync<ReservationJson>(cancellationToken:ct);
            if(reservation is null || reservation.ReservationId!=order.Id)
                return StatusCode(502,new { message="La reserva de inventario no coincide con el pedido" });
            order.Items=reservation.Items.Select(x=>new OrderItemSnapshot {
                ProductId=x.ProductId,ProductName=x.ProductName,Quantity=x.Quantity,UnitPrice=x.UnitPrice
            }).ToList();
            order.Total=order.Items.Sum(x=>x.UnitPrice*x.Quantity);
            order.Status=OrderStatus.CONFIRMED; await db.SaveChangesAsync(ct);
            return StatusCode(201,ToResponse(order));
        }
        catch(HttpRequestException)
        {
            await db.SaveChangesAsync(ct);
            return StatusCode(503,new { message="El pedido está pendiente; inténtalo de nuevo más tarde" });
        }
    }
    private static string Fingerprint(CreateOrderRequest request)
    {
        var items=request.Items.OrderBy(x=>x.ProductId).Select(x=>$"{x.ProductId}:{x.Quantity}");
        var text=$"{request.UserId}\n{request.CustomerName.Length}:{request.CustomerName}\n{request.CustomerEmail.Length}:{request.CustomerEmail}\n{string.Join(",",items)}";
        return Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(text))).ToLowerInvariant();
    }
    private static OrderResponse ToResponse(OrderEntity order) =>
        new(order.Id,order.UserId,order.CustomerName,order.CustomerEmail,order.Status.ToString(),
            order.Total,order.CreatedAt,
            order.Items.Select(x=>new OrderItemResponse(x.ProductId,x.ProductName,x.Quantity,x.UnitPrice)).ToList());
}
public record ReservationJson(Guid ReservationId,List<ReservationItemJson> Items);
public record ReservationItemJson(long ProductId,string ProductName,int Quantity,decimal UnitPrice);
