using System.ComponentModel.DataAnnotations;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

var builder = WebApplication.CreateBuilder(args);
builder.WebHost.ConfigureKestrel(options => options.ListenLocalhost(8082));
var connectionString = builder.Configuration["PRODUCT_DB_URL"]
    ?? "Host=localhost;Port=5432;Database=sales_product_db;Username=postgres;Password=postgres";
builder.Services.AddDbContext<ProductDbContext>(options => options.UseNpgsql(connectionString));
builder.Services.AddControllers();
builder.Services.AddCors(options => options.AddDefaultPolicy(policy =>
    policy.WithOrigins("http://localhost:5173", "http://127.0.0.1:5173").AllowAnyHeader().AllowAnyMethod()));
var app = builder.Build();
app.UseCors();
app.MapControllers();
using (var scope = app.Services.CreateScope())
    scope.ServiceProvider.GetRequiredService<ProductDbContext>().Database.EnsureCreated();
app.Run();

public class ProductDbContext(DbContextOptions<ProductDbContext> options) : DbContext(options)
{
    public DbSet<ProductEntity> Products => Set<ProductEntity>();
    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<ProductEntity>(e => {
            e.ToTable("products"); e.HasKey(x => x.Id);
            e.Property(x => x.Name).HasMaxLength(150).IsRequired();
            e.Property(x => x.Description).HasMaxLength(1000);
            e.Property(x => x.Price).HasPrecision(12,2);
        });
    }
}
public class ProductEntity
{
    public long Id { get; set; }
    public string Name { get; set; } = "";
    public string? Description { get; set; }
    public decimal Price { get; set; }
    public int Stock { get; set; }
    public string? ImageUrl { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
}
public record ProductRequest([property: Required, MinLength(2), MaxLength(150)] string Name,
    [property: MaxLength(1000)] string? Description,
    [property: Required, Range(0, 9999999999.99)] decimal Price,
    [property: Required, Range(0, int.MaxValue)] int Stock,
    [property: MaxLength(4000000)] string? ImageUrl);
public record ProductResponse(long Id, string Name, string? Description, decimal Price, int Stock,
    string? ImageUrl, DateTime CreatedAt, DateTime UpdatedAt);
public record ReserveInventoryItemRequest(long ProductId, int Quantity);
public record ReserveInventoryRequest(Guid ReservationId, [property: Required, MinLength(1)] List<ReserveInventoryItemRequest> Items);
public record ReservationItemResponse(long ProductId, string ProductName, int Quantity, decimal UnitPrice);
public record ReservationResponse(Guid ReservationId, List<ReservationItemResponse> Items);

[ApiController]
[Route("api/v1/products")]
public class ProductsController(ProductDbContext db) : ControllerBase
{
    [HttpGet] public async Task<IEnumerable<ProductResponse>> GetAll(CancellationToken ct) =>
        (await db.Products.AsNoTracking().ToListAsync(ct)).Select(ToResponse);
    [HttpGet("{id:long}")]
    public async Task<ActionResult<ProductResponse>> Get(long id, CancellationToken ct)
    {
        var product = await db.Products.FindAsync([id], ct);
        return product is null ? NotFound(new { message = "Producto no encontrado" }) : Ok(ToResponse(product));
    }
    [HttpPost]
    public async Task<ActionResult<ProductResponse>> Create([FromBody] ProductRequest request, CancellationToken ct)
    {
        var product = new ProductEntity { Name=request.Name.Trim(), Description=request.Description,
            Price=request.Price, Stock=request.Stock, ImageUrl=request.ImageUrl };
        db.Products.Add(product); await db.SaveChangesAsync(ct);
        return Created($"/api/v1/products/{product.Id}", ToResponse(product));
    }
    [HttpPut("{id:long}")]
    public async Task<ActionResult<ProductResponse>> Update(long id, [FromBody] ProductRequest request, CancellationToken ct)
    {
        var product = await db.Products.FindAsync([id], ct);
        if (product is null) return NotFound(new { message = "Producto no encontrado" });
        product.Name=request.Name.Trim(); product.Description=request.Description;
        product.Price=request.Price; product.Stock=request.Stock; product.ImageUrl=request.ImageUrl;
        product.UpdatedAt=DateTime.UtcNow; await db.SaveChangesAsync(ct);
        return Ok(ToResponse(product));
    }
    [HttpDelete("{id:long}")]
    public async Task<IActionResult> Delete(long id, CancellationToken ct)
    {
        var product = await db.Products.FindAsync([id], ct);
        if (product is null) return NotFound(new { message = "Producto no encontrado" });
        db.Products.Remove(product); await db.SaveChangesAsync(ct); return NoContent();
    }
    private static ProductResponse ToResponse(ProductEntity p) =>
        new(p.Id,p.Name,p.Description,p.Price,p.Stock,p.ImageUrl,p.CreatedAt,p.UpdatedAt);
}

[ApiController]
[Route("internal/v1/inventory/reservations")]
public class InventoryController(ProductDbContext db, IConfiguration configuration) : ControllerBase
{
    [HttpPost]
    public async Task<ActionResult<ReservationResponse>> Reserve(
        [FromHeader(Name="X-Inventory-Service-Key")] string? apiKey,
        [FromBody] ReserveInventoryRequest request, CancellationToken ct)
    {
        var configured = configuration["INVENTORY_SERVICE_API_KEY"];
        if (string.IsNullOrWhiteSpace(configured))
            return StatusCode(503, new { message = "Inventory service key is not configured" });
        if (!FixedTimeEquals(configured, apiKey))
            return Unauthorized(new { message = "Invalid inventory service key" });
        await using var transaction = await db.Database.BeginTransactionAsync(ct);
        var items = new List<ReservationItemResponse>();
        foreach (var item in request.Items)
        {
            var product = await db.Products.FindAsync([item.ProductId], ct);
            if (product is null) return NotFound(new { message = "Producto no encontrado" });
            if (product.Stock < item.Quantity) return Conflict(new { message = "Inventario insuficiente" });
            product.Stock -= item.Quantity; product.UpdatedAt=DateTime.UtcNow;
            items.Add(new ReservationItemResponse(product.Id,product.Name,item.Quantity,product.Price));
        }
        await db.SaveChangesAsync(ct);
        await transaction.CommitAsync(ct);
        return Ok(new ReservationResponse(request.ReservationId,items));
    }
    private static bool FixedTimeEquals(string a, string? b)
    {
        if (b is null || a.Length != b.Length) return false;
        var result=0;
        for (var i=0;i<a.Length;i++) result |= a[i]^b[i];
        return result==0;
    }
}
