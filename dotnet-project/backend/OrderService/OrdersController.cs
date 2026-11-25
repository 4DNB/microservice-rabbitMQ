using Microsoft.AspNetCore.Mvc;
using OrderService.Application;

namespace OrderService.Api;

[ApiController]
[Route("api/v1/orders")]
public sealed class OrdersController(IOrderApplicationService orders) : ControllerBase
{
    [HttpPost]
    public async Task<IActionResult> Create(
        [FromHeader(Name = "Idempotency-Key")] Guid idempotencyKey,
        [FromBody] CreateOrderRequest request,
        CancellationToken cancellationToken)
    {
        var result = await orders.CreateAsync(idempotencyKey, request, cancellationToken);
        return result.Status switch
        {
            OrderCreationStatus.Created => StatusCode(201, result.Response),
            OrderCreationStatus.AlreadyConfirmed => Ok(result.Response),
            OrderCreationStatus.IdempotencyConflict or OrderCreationStatus.AlreadyRejected =>
                Conflict(new { message = result.Message }),
            OrderCreationStatus.InventoryRejected => StatusCode(
                result.InventoryFailure switch
                {
                    InventoryClientStatus.BadRequest => 400,
                    InventoryClientStatus.NotFound => 404,
                    InventoryClientStatus.Conflict => 409,
                    _ => throw new InvalidOperationException("Rejected inventory result has no failure status.")
                },
                new { message = result.Message }),
            OrderCreationStatus.InventoryUnavailable =>
                StatusCode(503, new { message = result.Message }),
            OrderCreationStatus.ReservationMismatch =>
                StatusCode(502, new { message = result.Message }),
            _ => throw new InvalidOperationException($"Unknown order creation status: {result.Status}")
        };
    }
}
