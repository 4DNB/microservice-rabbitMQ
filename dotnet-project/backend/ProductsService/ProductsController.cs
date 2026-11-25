using Microsoft.AspNetCore.Mvc;
using ProductsService.Application;
using ProductsService.Domain;

namespace ProductsService.Api;

[ApiController]
[Route("api/v1/products")]
public sealed class ProductsController(IProductsApplicationService products) : ControllerBase
{
    [HttpGet]
    public async Task<IEnumerable<ProductResponse>> GetAll(CancellationToken cancellationToken) =>
        (await products.GetAllAsync(cancellationToken)).Select(ToResponse);

    [HttpGet("{id:long}")]
    public async Task<ActionResult<ProductResponse>> Get(long id, CancellationToken cancellationToken)
    {
        var product = await products.GetByIdAsync(id, cancellationToken);
        return product is null
            ? NotFound(new { message = "Producto no encontrado" })
            : Ok(ToResponse(product));
    }

    [HttpPost]
    public async Task<ActionResult<ProductResponse>> Create(
        [FromBody] ProductRequest request,
        CancellationToken cancellationToken)
    {
        var product = await products.CreateAsync(request, cancellationToken);
        return Created($"/api/v1/products/{product.Id}", ToResponse(product));
    }

    [HttpPut("{id:long}")]
    public async Task<ActionResult<ProductResponse>> Update(
        long id,
        [FromBody] ProductRequest request,
        CancellationToken cancellationToken)
    {
        var product = await products.UpdateAsync(id, request, cancellationToken);
        return product is null
            ? NotFound(new { message = "Producto no encontrado" })
            : Ok(ToResponse(product));
    }

    [HttpDelete("{id:long}")]
    public async Task<IActionResult> Delete(long id, CancellationToken cancellationToken) =>
        await products.DeleteAsync(id, cancellationToken)
            ? NoContent()
            : NotFound(new { message = "Producto no encontrado" });

    private static ProductResponse ToResponse(Product product) =>
        new(product.Id, product.Name, product.Description, product.Price, product.Stock,
            product.ImageUrl, product.CreatedAt, product.UpdatedAt);
}

public record ProductResponse(
    long Id,
    string Name,
    string? Description,
    decimal Price,
    int Stock,
    string? ImageUrl,
    DateTime CreatedAt,
    DateTime UpdatedAt);

[ApiController]
[Route("internal/v1/inventory/reservations")]
public sealed class InventoryController(
    IProductsApplicationService products,
    IConfiguration configuration) : ControllerBase
{
    [HttpPost]
    public async Task<ActionResult<ReservationResponse>> Reserve(
        [FromHeader(Name = "X-Inventory-Service-Key")] string? apiKey,
        [FromBody] ReserveInventoryRequest request,
        CancellationToken cancellationToken)
    {
        var configuredKey = configuration["INVENTORY_SERVICE_API_KEY"];
        if (string.IsNullOrWhiteSpace(configuredKey))
            return StatusCode(503, new { message = "Inventory service key is not configured" });
        if (!FixedTimeEquals(configuredKey, apiKey))
            return Unauthorized(new { message = "Invalid inventory service key" });

        var result = await products.ReserveInventoryAsync(request, cancellationToken);
        return result.Status switch
        {
            InventoryReservationStatus.Success => Ok(result.Reservation),
            InventoryReservationStatus.ProductNotFound => NotFound(new { message = "Producto no encontrado" }),
            InventoryReservationStatus.InsufficientStock => Conflict(new { message = "Inventario insuficiente" }),
            _ => throw new InvalidOperationException($"Unknown reservation status: {result.Status}")
        };
    }

    private static bool FixedTimeEquals(string expected, string? provided)
    {
        if (provided is null || expected.Length != provided.Length)
            return false;

        var difference = 0;
        for (var index = 0; index < expected.Length; index++)
            difference |= expected[index] ^ provided[index];
        return difference == 0;
    }
}
