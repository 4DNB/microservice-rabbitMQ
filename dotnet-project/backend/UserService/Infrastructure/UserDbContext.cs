using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using UserService.Application;
using UserService.Domain;

namespace UserService.Infrastructure;

public sealed class UserDbContext(DbContextOptions<UserDbContext> options) : DbContext(options)
{
    public DbSet<User> Users => Set<User>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<User>(entity =>
        {
            entity.ToTable("users");
            entity.HasKey(user => user.Id);
            entity.Property(user => user.Id).HasColumnName("id");
            entity.Property(user => user.Name).HasColumnName("name").HasMaxLength(100).IsRequired();
            entity.Property(user => user.Email).HasColumnName("email").IsRequired();
            entity.HasIndex(user => user.Email).IsUnique();
            entity.Property(user => user.Role).HasColumnName("role").HasConversion<string>().HasMaxLength(20);
            entity.Property(user => user.Status).HasColumnName("status").HasConversion<string>().HasMaxLength(20);
            entity.Property(user => user.CreatedAt).HasColumnName("created_at");
            entity.Property(user => user.UpdatedAt).HasColumnName("updated_at");
        });
    }
}

internal sealed class UserRepository(UserDbContext dbContext) : IUserRepository
{
    public async Task<IReadOnlyList<User>> GetAllAsync(CancellationToken cancellationToken) =>
        await dbContext.Users.AsNoTracking().ToListAsync(cancellationToken);

    public Task<User?> GetByIdAsync(long id, CancellationToken cancellationToken) =>
        dbContext.Users.FindAsync([id], cancellationToken).AsTask();

    public Task<bool> EmailExistsAsync(string email, long? excludingId, CancellationToken cancellationToken) =>
        dbContext.Users.AnyAsync(
            user => user.Email == email && (!excludingId.HasValue || user.Id != excludingId.Value),
            cancellationToken);

    public Task AddAsync(User user, CancellationToken cancellationToken) =>
        dbContext.Users.AddAsync(user, cancellationToken).AsTask();

    public Task SaveChangesAsync(CancellationToken cancellationToken) =>
        dbContext.SaveChangesAsync(cancellationToken);

    public void Remove(User user) => dbContext.Users.Remove(user);
}

public static class UserServiceCollectionExtensions
{
    public static IServiceCollection AddUserInfrastructure(
        this IServiceCollection services,
        IConfiguration configuration)
    {
        var connectionString = configuration["DB_URL"]
            ?? "Host=localhost;Port=5432;Database=sales_user_db;Username=postgres;Password=postgres";
        services.AddDbContext<UserDbContext>(options => options.UseNpgsql(connectionString));
        services.AddScoped<IUserRepository, UserRepository>();
        return services;
    }
}
