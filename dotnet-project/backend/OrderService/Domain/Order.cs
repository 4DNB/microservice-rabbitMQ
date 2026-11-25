namespace OrderService.Domain;

public enum OrderStatus { PENDING, CONFIRMED, REJECTED }

public class Order
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
