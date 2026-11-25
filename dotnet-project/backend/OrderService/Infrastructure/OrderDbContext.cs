using System.Net.Http.Json;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using OrderService.Application;
using OrderService.Domain;
using System.Net;

namespace OrderService.Infrastructure;

public sealed class OrderDbContext(DbContextOptions<OrderDbContext> options) : DbContext(options)
{
    public DbSet<Order> Orders => Set<Order>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Order>(entity =>
        {
            entity.ToTable("customer_orders");
            entity.HasKey(order => order.Id);
            entity.HasIndex(order => order.IdempotencyKey).IsUnique();
            entity.Property(order => order.RequestFingerprint).HasMaxLength(64);
            entity.Property(order => order.Status).HasConversion<string>().HasMaxLength(20);
            entity.Property(order => order.Total).HasPrecision(12, 2);
            entity.OwnsMany(order => order.Items, items =>
            {
                items.ToTable("order_items");
                items.WithOwner().HasForeignKey("OrderId");
                items.Property<Guid>("OrderId");
                items.Property<long>("Id");
                items.HasKey("Id");
            });
        });
    }
}

internal sealed class OrderRepository(OrderDbContext dbContext) : IOrderRepository
{
    public Task<Order?> GetByIdempotencyKeyAsync(Guid idempotencyKey, CancellationToken cancellationToken) =>
        dbContext.Orders.Include(order => order.Items)
            .FirstOrDefaultAsync(order => order.IdempotencyKey == idempotencyKey, cancellationToken);

    public Task AddAsync(Order order, CancellationToken cancellationToken) =>
        dbContext.Orders.AddAsync(order, cancellationToken).AsTask();

    public Task SaveChangesAsync(CancellationToken cancellationToken) =>
        dbContext.SaveChangesAsync(cancellationToken);
}

internal sealed class ProductsInventoryReservationClient(
    HttpClient httpClient,
    IConfiguration configuration) : IInventoryReservationClient
{
    public async Task<InventoryClientResult> ReserveAsync(
        InventoryReservationRequest request,
        CancellationToken cancellationToken)
    {
        using var message = new HttpRequestMessage(HttpMethod.Post, "/internal/v1/inventory/reservations")
        {
            Content = JsonContent.Create(request)
        };
        message.Headers.TryAddWithoutValidation(
            "X-Inventory-Service-Key",
            configuration["INVENTORY_SERVICE_API_KEY"] ?? "");

        using var response = await httpClient.SendAsync(message, cancellationToken);
        var status = response.StatusCode switch
        {
            HttpStatusCode.BadRequest => InventoryClientStatus.BadRequest,
            HttpStatusCode.NotFound => InventoryClientStatus.NotFound,
            HttpStatusCode.Conflict => InventoryClientStatus.Conflict,
            _ when response.IsSuccessStatusCode => InventoryClientStatus.Success,
            _ => InventoryClientStatus.Unavailable
        };
        var reservation = response.IsSuccessStatusCode
            ? await response.Content.ReadFromJsonAsync<InventoryReservationResponse>(
                cancellationToken: cancellationToken)
            : null;
        return new InventoryClientResult(status, reservation);
    }
}

public static class OrderServiceCollectionExtensions
{
    public static IServiceCollection AddOrderInfrastructure(
        this IServiceCollection services,
        IConfiguration configuration)
    {
        var connectionString = configuration["ORDER_DB_URL"]
            ?? "Host=localhost;Port=5432;Database=sales_order_db;Username=postgres;Password=postgres";
        services.AddDbContext<OrderDbContext>(options => options.UseNpgsql(connectionString));
        services.AddScoped<IOrderRepository, OrderRepository>();
        services.AddHttpClient<IInventoryReservationClient, ProductsInventoryReservationClient>(client =>
            client.BaseAddress = new Uri(
                configuration["PRODUCTS_SERVICE_URL"] ?? "http://localhost:8082"));
        return services;
    }
}
