using System.ComponentModel.DataAnnotations;
using UserService.Domain;

namespace UserService.Application;

public record CreateUserRequest(
    [property: Required, MinLength(2), MaxLength(100)] string Name,
    [property: Required, EmailAddress] string Email,
    UserRole? Role);

public record UpdateUserRequest(
    [property: Required, MinLength(2), MaxLength(100)] string Name,
    [property: Required, EmailAddress] string Email,
    UserRole? Role,
    UserStatus? Status);

public interface IUserRepository
{
    Task<IReadOnlyList<User>> GetAllAsync(CancellationToken cancellationToken);
    Task<User?> GetByIdAsync(long id, CancellationToken cancellationToken);
    Task<bool> EmailExistsAsync(string email, long? excludingId, CancellationToken cancellationToken);
    Task AddAsync(User user, CancellationToken cancellationToken);
    Task SaveChangesAsync(CancellationToken cancellationToken);
    void Remove(User user);
}

public interface IUserApplicationService
{
    Task<IReadOnlyList<User>> GetAllAsync(CancellationToken cancellationToken);
    Task<User?> GetByIdAsync(long id, CancellationToken cancellationToken);
    Task<User?> CreateAsync(CreateUserRequest request, CancellationToken cancellationToken);
    Task<UserUpdateResult> UpdateAsync(long id, UpdateUserRequest request, CancellationToken cancellationToken);
    Task<bool> DeleteAsync(long id, CancellationToken cancellationToken);
}

public enum UserUpdateStatus { Updated, NotFound, EmailAlreadyExists }
public record UserUpdateResult(UserUpdateStatus Status, User? User = null);

public sealed class UserApplicationService(IUserRepository repository) : IUserApplicationService
{
    public Task<IReadOnlyList<User>> GetAllAsync(CancellationToken cancellationToken) =>
        repository.GetAllAsync(cancellationToken);

    public Task<User?> GetByIdAsync(long id, CancellationToken cancellationToken) =>
        repository.GetByIdAsync(id, cancellationToken);

    public async Task<User?> CreateAsync(CreateUserRequest request, CancellationToken cancellationToken)
    {
        var email = request.Email.Trim().ToLowerInvariant();
        if (await repository.EmailExistsAsync(email, null, cancellationToken))
            return null;

        var user = new User
        {
            Name = request.Name.Trim(),
            Email = email,
            Role = request.Role ?? UserRole.CUSTOMER
        };
        await repository.AddAsync(user, cancellationToken);
        await repository.SaveChangesAsync(cancellationToken);
        return user;
    }

    public async Task<UserUpdateResult> UpdateAsync(
        long id,
        UpdateUserRequest request,
        CancellationToken cancellationToken)
    {
        var user = await repository.GetByIdAsync(id, cancellationToken);
        if (user is null)
            return new UserUpdateResult(UserUpdateStatus.NotFound);

        var email = request.Email.Trim().ToLowerInvariant();
        if (await repository.EmailExistsAsync(email, id, cancellationToken))
            return new UserUpdateResult(UserUpdateStatus.EmailAlreadyExists);

        user.Name = request.Name.Trim();
        user.Email = email;
        if (request.Role.HasValue)
            user.Role = request.Role.Value;
        if (request.Status.HasValue)
            user.Status = request.Status.Value;
        user.UpdatedAt = DateTime.UtcNow;

        await repository.SaveChangesAsync(cancellationToken);
        return new UserUpdateResult(UserUpdateStatus.Updated, user);
    }

    public async Task<bool> DeleteAsync(long id, CancellationToken cancellationToken)
    {
        var user = await repository.GetByIdAsync(id, cancellationToken);
        if (user is null)
            return false;

        repository.Remove(user);
        await repository.SaveChangesAsync(cancellationToken);
        return true;
    }
}
