using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using ProductsService.Application;
using ProductsService.Domain;

namespace ProductsService.Infrastructure;

public sealed class ProductDbContext(DbContextOptions<ProductDbContext> options) : DbContext(options)
{
    public DbSet<Product> Products => Set<Product>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Product>(entity =>
        {
            entity.ToTable("products");
            entity.HasKey(product => product.Id);
            entity.Property(product => product.Name).HasMaxLength(150).IsRequired();
            entity.Property(product => product.Description).HasMaxLength(1000);
            entity.Property(product => product.Price).HasPrecision(12, 2);
        });
    }
}

internal sealed class ProductRepository(ProductDbContext dbContext) : IProductRepository
{
    public async Task<IReadOnlyList<Product>> GetAllAsync(CancellationToken cancellationToken) =>
        await dbContext.Products.AsNoTracking().ToListAsync(cancellationToken);

    public Task<Product?> GetByIdAsync(long id, CancellationToken cancellationToken) =>
        dbContext.Products.FindAsync([id], cancellationToken).AsTask();

    public Task AddAsync(Product product, CancellationToken cancellationToken) =>
        dbContext.Products.AddAsync(product, cancellationToken).AsTask();

    public void Remove(Product product) => dbContext.Products.Remove(product);

    public Task SaveChangesAsync(CancellationToken cancellationToken) =>
        dbContext.SaveChangesAsync(cancellationToken);

    public async Task<InventoryReservationResult> ReserveInventoryAsync(
        Guid reservationId,
        IReadOnlyList<ReserveInventoryItemRequest> items,
        CancellationToken cancellationToken)
    {
        await using var transaction = await dbContext.Database.BeginTransactionAsync(cancellationToken);
        var reservationItems = new List<ReservationItemResponse>();

        foreach (var item in items)
        {
            var product = await dbContext.Products.FindAsync([item.ProductId], cancellationToken);
            if (product is null)
                return new InventoryReservationResult(InventoryReservationStatus.ProductNotFound);
            if (product.Stock < item.Quantity)
                return new InventoryReservationResult(InventoryReservationStatus.InsufficientStock);

            product.Stock -= item.Quantity;
            product.UpdatedAt = DateTime.UtcNow;
            reservationItems.Add(new ReservationItemResponse(
                product.Id, product.Name, item.Quantity, product.Price));
        }

        await dbContext.SaveChangesAsync(cancellationToken);
        await transaction.CommitAsync(cancellationToken);
        return new InventoryReservationResult(
            InventoryReservationStatus.Success,
            new ReservationResponse(reservationId, reservationItems));
    }
}

public static class ProductsServiceCollectionExtensions
{
    public static IServiceCollection AddProductsInfrastructure(
        this IServiceCollection services,
        IConfiguration configuration)
    {
        var connectionString = configuration["PRODUCT_DB_URL"]
            ?? "Host=localhost;Port=5432;Database=sales_product_db;Username=postgres;Password=postgres";
        services.AddDbContext<ProductDbContext>(options => options.UseNpgsql(connectionString));
        services.AddScoped<IProductRepository, ProductRepository>();
        return services;
    }
}
