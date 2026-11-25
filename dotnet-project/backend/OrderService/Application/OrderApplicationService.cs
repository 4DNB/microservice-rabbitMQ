using System.ComponentModel.DataAnnotations;
using System.Security.Cryptography;
using System.Text;
using OrderService.Domain;

namespace OrderService.Application;

public record OrderItemRequest(
    [property: Required, Range(1, long.MaxValue)] long ProductId,
    [property: Required, Range(1, int.MaxValue)] int Quantity);

public record CreateOrderRequest(
    [property: Required, Range(1, long.MaxValue)] long UserId,
    [property: Required, MaxLength(100)] string CustomerName,
    [property: Required, EmailAddress, MaxLength(254)] string CustomerEmail,
    [property: Required, MinLength(1)] List<OrderItemRequest> Items);

public record OrderItemResponse(long ProductId, string ProductName, int Quantity, decimal UnitPrice);

public record OrderResponse(
    Guid Id,
    long UserId,
    string CustomerName,
    string CustomerEmail,
    string Status,
    decimal Total,
    DateTime CreatedAt,
    List<OrderItemResponse> Items);

public record InventoryReservationItemRequest(long ProductId, int Quantity);
public record InventoryReservationRequest(Guid ReservationId, List<InventoryReservationItemRequest> Items);
public record InventoryReservationItemResponse(long ProductId, string ProductName, int Quantity, decimal UnitPrice);
public record InventoryReservationResponse(Guid ReservationId, List<InventoryReservationItemResponse> Items);

public enum InventoryClientStatus { Success, BadRequest, NotFound, Conflict, Unavailable }
public record InventoryClientResult(InventoryClientStatus Status, InventoryReservationResponse? Reservation);

public enum OrderCreationStatus
{
    Created,
    AlreadyConfirmed,
    IdempotencyConflict,
    AlreadyRejected,
    InventoryRejected,
    InventoryUnavailable,
    ReservationMismatch
}

public record OrderCreationResult(
    OrderCreationStatus Status,
    string? Message = null,
    OrderResponse? Response = null,
    InventoryClientStatus? InventoryFailure = null);

public interface IOrderRepository
{
    Task<Order?> GetByIdempotencyKeyAsync(Guid idempotencyKey, CancellationToken cancellationToken);
    Task AddAsync(Order order, CancellationToken cancellationToken);
    Task SaveChangesAsync(CancellationToken cancellationToken);
}

public interface IInventoryReservationClient
{
    Task<InventoryClientResult> ReserveAsync(
        InventoryReservationRequest request,
        CancellationToken cancellationToken);
}

public interface IOrderApplicationService
{
    Task<OrderCreationResult> CreateAsync(
        Guid idempotencyKey,
        CreateOrderRequest request,
        CancellationToken cancellationToken);
}

public sealed class OrderApplicationService(
    IOrderRepository orders,
    IInventoryReservationClient inventory) : IOrderApplicationService
{
    public async Task<OrderCreationResult> CreateAsync(
        Guid idempotencyKey,
        CreateOrderRequest request,
        CancellationToken cancellationToken)
    {
        var fingerprint = Fingerprint(request);
        var existing = await orders.GetByIdempotencyKeyAsync(idempotencyKey, cancellationToken);
        if (existing is not null)
        {
            if (existing.RequestFingerprint != fingerprint)
                return new OrderCreationResult(
                    OrderCreationStatus.IdempotencyConflict,
                    "La clave de idempotencia ya se usó con otra solicitud");
            if (existing.Status == OrderStatus.CONFIRMED)
                return new OrderCreationResult(
                    OrderCreationStatus.AlreadyConfirmed,
                    Response: ToResponse(existing));
            if (existing.Status == OrderStatus.REJECTED)
                return new OrderCreationResult(OrderCreationStatus.AlreadyRejected, "El pedido ya fue rechazado");
            return await ConfirmAsync(existing, request, cancellationToken);
        }

        var order = new Order
        {
            Id = Guid.NewGuid(),
            IdempotencyKey = idempotencyKey,
            RequestFingerprint = fingerprint,
            UserId = request.UserId,
            CustomerName = request.CustomerName.Trim(),
            CustomerEmail = request.CustomerEmail.Trim().ToLowerInvariant(),
            Status = OrderStatus.PENDING
        };
        await orders.AddAsync(order, cancellationToken);
        await orders.SaveChangesAsync(cancellationToken);
        return await ConfirmAsync(order, request, cancellationToken);
    }

    private async Task<OrderCreationResult> ConfirmAsync(
        Order order,
        CreateOrderRequest request,
        CancellationToken cancellationToken)
    {
        try
        {
            var reservation = await inventory.ReserveAsync(
                new InventoryReservationRequest(
                    order.Id,
                    request.Items.Select(item =>
                        new InventoryReservationItemRequest(item.ProductId, item.Quantity)).ToList()),
                cancellationToken);

            if (reservation.Status is InventoryClientStatus.BadRequest
                or InventoryClientStatus.NotFound
                or InventoryClientStatus.Conflict)
            {
                order.Status = OrderStatus.REJECTED;
                await orders.SaveChangesAsync(cancellationToken);
                return new OrderCreationResult(
                    OrderCreationStatus.InventoryRejected,
                    "No se pudo reservar el inventario para este pedido",
                    InventoryFailure: reservation.Status);
            }

            if (reservation.Status != InventoryClientStatus.Success)
            {
                await orders.SaveChangesAsync(cancellationToken);
                return new OrderCreationResult(
                    OrderCreationStatus.InventoryUnavailable,
                    "El pedido está pendiente; inténtalo de nuevo más tarde");
            }

            var inventoryReservation = reservation.Reservation;
            if (inventoryReservation is null || inventoryReservation.ReservationId != order.Id)
                return new OrderCreationResult(
                    OrderCreationStatus.ReservationMismatch,
                    "La reserva de inventario no coincide con el pedido");

            order.Items = inventoryReservation.Items.Select(item => new OrderItemSnapshot
            {
                ProductId = item.ProductId,
                ProductName = item.ProductName,
                Quantity = item.Quantity,
                UnitPrice = item.UnitPrice
            }).ToList();
            order.Total = order.Items.Sum(item => item.UnitPrice * item.Quantity);
            order.Status = OrderStatus.CONFIRMED;
            await orders.SaveChangesAsync(cancellationToken);
            return new OrderCreationResult(OrderCreationStatus.Created, Response: ToResponse(order));
        }
        catch (HttpRequestException)
        {
            await orders.SaveChangesAsync(cancellationToken);
            return new OrderCreationResult(
                OrderCreationStatus.InventoryUnavailable,
                "El pedido está pendiente; inténtalo de nuevo más tarde");
        }
    }

    private static string Fingerprint(CreateOrderRequest request)
    {
        var items = request.Items.OrderBy(item => item.ProductId)
            .Select(item => $"{item.ProductId}:{item.Quantity}");
        var text = $"{request.UserId}\n{request.CustomerName.Length}:{request.CustomerName}\n" +
            $"{request.CustomerEmail.Length}:{request.CustomerEmail}\n{string.Join(",", items)}";
        return Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(text))).ToLowerInvariant();
    }

    private static OrderResponse ToResponse(Order order) =>
        new(order.Id, order.UserId, order.CustomerName, order.CustomerEmail, order.Status.ToString(),
            order.Total, order.CreatedAt,
            order.Items.Select(item =>
                new OrderItemResponse(item.ProductId, item.ProductName, item.Quantity, item.UnitPrice)).ToList());
}
