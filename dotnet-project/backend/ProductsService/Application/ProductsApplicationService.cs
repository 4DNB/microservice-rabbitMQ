using System.ComponentModel.DataAnnotations;
using ProductsService.Domain;

namespace ProductsService.Application;

public record ProductRequest(
    [property: Required, MinLength(2), MaxLength(150)] string Name,
    [property: MaxLength(1000)] string? Description,
    [property: Required, Range(0, 9999999999.99)] decimal Price,
    [property: Required, Range(0, int.MaxValue)] int Stock,
    [property: MaxLength(4000000)] string? ImageUrl);

public record ReserveInventoryItemRequest(long ProductId, int Quantity);

public record ReserveInventoryRequest(
    Guid ReservationId,
    [property: Required, MinLength(1)] List<ReserveInventoryItemRequest> Items);

public record ReservationItemResponse(long ProductId, string ProductName, int Quantity, decimal UnitPrice);
public record ReservationResponse(Guid ReservationId, List<ReservationItemResponse> Items);

public enum InventoryReservationStatus { Success, ProductNotFound, InsufficientStock }

public record InventoryReservationResult(
    InventoryReservationStatus Status,
    ReservationResponse? Reservation = null);

public interface IProductRepository
{
    Task<IReadOnlyList<Product>> GetAllAsync(CancellationToken cancellationToken);
    Task<Product?> GetByIdAsync(long id, CancellationToken cancellationToken);
    Task AddAsync(Product product, CancellationToken cancellationToken);
    void Remove(Product product);
    Task SaveChangesAsync(CancellationToken cancellationToken);
    Task<InventoryReservationResult> ReserveInventoryAsync(
        Guid reservationId,
        IReadOnlyList<ReserveInventoryItemRequest> items,
        CancellationToken cancellationToken);
}

public interface IProductsApplicationService
{
    Task<IReadOnlyList<Product>> GetAllAsync(CancellationToken cancellationToken);
    Task<Product?> GetByIdAsync(long id, CancellationToken cancellationToken);
    Task<Product> CreateAsync(ProductRequest request, CancellationToken cancellationToken);
    Task<Product?> UpdateAsync(long id, ProductRequest request, CancellationToken cancellationToken);
    Task<bool> DeleteAsync(long id, CancellationToken cancellationToken);
    Task<InventoryReservationResult> ReserveInventoryAsync(
        ReserveInventoryRequest request,
        CancellationToken cancellationToken);
}

public sealed class ProductsApplicationService(IProductRepository repository) : IProductsApplicationService
{
    public Task<IReadOnlyList<Product>> GetAllAsync(CancellationToken cancellationToken) =>
        repository.GetAllAsync(cancellationToken);

    public Task<Product?> GetByIdAsync(long id, CancellationToken cancellationToken) =>
        repository.GetByIdAsync(id, cancellationToken);

    public async Task<Product> CreateAsync(ProductRequest request, CancellationToken cancellationToken)
    {
        var product = new Product
        {
            Name = request.Name.Trim(),
            Description = request.Description,
            Price = request.Price,
            Stock = request.Stock,
            ImageUrl = request.ImageUrl
        };
        await repository.AddAsync(product, cancellationToken);
        await repository.SaveChangesAsync(cancellationToken);
        return product;
    }

    public async Task<Product?> UpdateAsync(long id, ProductRequest request, CancellationToken cancellationToken)
    {
        var product = await repository.GetByIdAsync(id, cancellationToken);
        if (product is null)
            return null;

        product.Name = request.Name.Trim();
        product.Description = request.Description;
        product.Price = request.Price;
        product.Stock = request.Stock;
        product.ImageUrl = request.ImageUrl;
        product.UpdatedAt = DateTime.UtcNow;
        await repository.SaveChangesAsync(cancellationToken);
        return product;
    }

    public async Task<bool> DeleteAsync(long id, CancellationToken cancellationToken)
    {
        var product = await repository.GetByIdAsync(id, cancellationToken);
        if (product is null)
            return false;

        repository.Remove(product);
        await repository.SaveChangesAsync(cancellationToken);
        return true;
    }

    public Task<InventoryReservationResult> ReserveInventoryAsync(
        ReserveInventoryRequest request,
        CancellationToken cancellationToken) =>
        repository.ReserveInventoryAsync(request.ReservationId, request.Items, cancellationToken);
}
