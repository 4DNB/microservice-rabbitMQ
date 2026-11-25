namespace UserService.Domain;

public enum UserRole { CUSTOMER, SELLER, MANAGER, ADMIN }
public enum UserStatus { ACTIVE, INACTIVE, SUSPENDED }

public class User
{
    public long Id { get; set; }
    public string Name { get; set; } = "";
    public string Email { get; set; } = "";
    public UserRole Role { get; set; } = UserRole.CUSTOMER;
    public UserStatus Status { get; set; } = UserStatus.ACTIVE;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
}
