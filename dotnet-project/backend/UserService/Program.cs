using System.ComponentModel.DataAnnotations;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

var builder = WebApplication.CreateBuilder(args);
builder.WebHost.ConfigureKestrel(options => options.ListenLocalhost(8081));
var connectionString = builder.Configuration["DB_URL"]
    ?? "Host=localhost;Port=5432;Database=sales_user_db;Username=postgres;Password=postgres";
builder.Services.AddDbContext<UserDbContext>(options => options.UseNpgsql(connectionString));
builder.Services.AddControllers();
builder.Services.AddCors(options => options.AddDefaultPolicy(policy =>
    policy.WithOrigins("http://localhost:5173", "http://127.0.0.1:5173").AllowAnyHeader().AllowAnyMethod()));
var app = builder.Build();
app.UseCors();
app.MapControllers();
using (var scope = app.Services.CreateScope())
    scope.ServiceProvider.GetRequiredService<UserDbContext>().Database.EnsureCreated();
app.Run();

public class UserDbContext(DbContextOptions<UserDbContext> options) : DbContext(options)
{
    public DbSet<UserEntity> Users => Set<UserEntity>();
    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<UserEntity>(e => {
            e.ToTable("users"); e.HasKey(x => x.Id);
            e.Property(x => x.Id).HasColumnName("id");
            e.Property(x => x.Name).HasColumnName("name").HasMaxLength(100).IsRequired();
            e.Property(x => x.Email).HasColumnName("email").IsRequired();
            e.HasIndex(x => x.Email).IsUnique();
            e.Property(x => x.Role).HasColumnName("role").HasConversion<string>().HasMaxLength(20);
            e.Property(x => x.Status).HasColumnName("status").HasConversion<string>().HasMaxLength(20);
            e.Property(x => x.CreatedAt).HasColumnName("created_at");
            e.Property(x => x.UpdatedAt).HasColumnName("updated_at");
        });
    }
}
public enum UserRole { CUSTOMER, SELLER, MANAGER, ADMIN }
public enum UserStatus { ACTIVE, INACTIVE, SUSPENDED }
public class UserEntity
{
    public long Id { get; set; }
    public string Name { get; set; } = "";
    public string Email { get; set; } = "";
    public UserRole Role { get; set; } = UserRole.CUSTOMER;
    public UserStatus Status { get; set; } = UserStatus.ACTIVE;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
}
public record CreateUserRequest([property: Required, MinLength(2), MaxLength(100)] string Name,
    [property: Required, EmailAddress] string Email, UserRole? Role);
public record UpdateUserRequest([property: Required, MinLength(2), MaxLength(100)] string Name,
    [property: Required, EmailAddress] string Email, UserRole? Role, UserStatus? Status);
public record UserResponse(long Id, string Name, string Email, UserRole Role, UserStatus Status,
    DateTime CreatedAt, DateTime UpdatedAt);

[ApiController]
[Route("api/v1/users")]
public class UsersController(UserDbContext db) : ControllerBase
{
    [HttpGet]
    public async Task<ActionResult<IEnumerable<UserResponse>>> GetAll(CancellationToken ct) =>
        Ok((await db.Users.AsNoTracking().ToListAsync(ct)).Select(ToResponse));
    [HttpGet("{id:long}")]
    public async Task<ActionResult<UserResponse>> Get(long id, CancellationToken ct)
    {
        var user = await db.Users.FindAsync([id], ct);
        return user is null ? NotFound(new { message = "Usuario no encontrado" }) : Ok(ToResponse(user));
    }
    [HttpPost]
    public async Task<ActionResult<UserResponse>> Create([FromBody] CreateUserRequest request, CancellationToken ct)
    {
        var email = request.Email.Trim().ToLowerInvariant();
        if (await db.Users.AnyAsync(x => x.Email == email, ct))
            return Conflict(new { message = "Ya existe un usuario con ese correo electrónico" });
        var user = new UserEntity { Name=request.Name.Trim(), Email=email, Role=request.Role ?? UserRole.CUSTOMER };
        db.Users.Add(user); await db.SaveChangesAsync(ct);
        return Created($"/api/v1/users/{user.Id}", ToResponse(user));
    }
    [HttpPut("{id:long}")]
    public async Task<ActionResult<UserResponse>> Update(long id, [FromBody] UpdateUserRequest request, CancellationToken ct)
    {
        var user = await db.Users.FindAsync([id], ct);
        if (user is null) return NotFound(new { message = "Usuario no encontrado" });
        var email = request.Email.Trim().ToLowerInvariant();
        if (await db.Users.AnyAsync(x => x.Id != id && x.Email == email, ct))
            return Conflict(new { message = "Ya existe un usuario con ese correo electrónico" });
        user.Name=request.Name.Trim(); user.Email=email;
        if (request.Role.HasValue) user.Role=request.Role.Value;
        if (request.Status.HasValue) user.Status=request.Status.Value;
        user.UpdatedAt=DateTime.UtcNow; await db.SaveChangesAsync(ct);
        return Ok(ToResponse(user));
    }
    [HttpDelete("{id:long}")]
    public async Task<IActionResult> Delete(long id, CancellationToken ct)
    {
        var user = await db.Users.FindAsync([id], ct);
        if (user is null) return NotFound(new { message = "Usuario no encontrado" });
        db.Users.Remove(user); await db.SaveChangesAsync(ct); return NoContent();
    }
    private static UserResponse ToResponse(UserEntity u) =>
        new(u.Id,u.Name,u.Email,u.Role,u.Status,u.CreatedAt,u.UpdatedAt);
}
