using Microsoft.AspNetCore.Mvc;
using UserService.Application;
using UserService.Domain;

namespace UserService.Api;

[ApiController]
[Route("api/v1/users")]
public sealed class UsersController(IUserApplicationService users) : ControllerBase
{
    [HttpGet]
    public async Task<ActionResult<IEnumerable<UserResponse>>> GetAll(CancellationToken cancellationToken) =>
        Ok((await users.GetAllAsync(cancellationToken)).Select(ToResponse));

    [HttpGet("{id:long}")]
    public async Task<ActionResult<UserResponse>> Get(long id, CancellationToken cancellationToken)
    {
        var user = await users.GetByIdAsync(id, cancellationToken);
        return user is null
            ? NotFound(new { message = "Usuario no encontrado" })
            : Ok(ToResponse(user));
    }

    [HttpPost]
    public async Task<ActionResult<UserResponse>> Create(
        [FromBody] CreateUserRequest request,
        CancellationToken cancellationToken)
    {
        var user = await users.CreateAsync(request, cancellationToken);
        if (user is null)
            return Conflict(new { message = "Ya existe un usuario con ese correo electrónico" });

        return Created($"/api/v1/users/{user.Id}", ToResponse(user));
    }

    [HttpPut("{id:long}")]
    public async Task<ActionResult<UserResponse>> Update(
        long id,
        [FromBody] UpdateUserRequest request,
        CancellationToken cancellationToken)
    {
        var result = await users.UpdateAsync(id, request, cancellationToken);
        if (result.Status == UserUpdateStatus.NotFound)
            return NotFound(new { message = "Usuario no encontrado" });
        if (result.Status == UserUpdateStatus.EmailAlreadyExists)
        {
            return Conflict(new { message = "Ya existe un usuario con ese correo electrónico" });
        }

        var updatedUser = result.User
            ?? throw new InvalidOperationException("The update operation completed without returning a user.");
        return Ok(ToResponse(updatedUser));
    }

    [HttpDelete("{id:long}")]
    public async Task<IActionResult> Delete(long id, CancellationToken cancellationToken) =>
        await users.DeleteAsync(id, cancellationToken)
            ? NoContent()
            : NotFound(new { message = "Usuario no encontrado" });

    private static UserResponse ToResponse(User user) =>
        new(user.Id, user.Name, user.Email, user.Role, user.Status, user.CreatedAt, user.UpdatedAt);
}

public record UserResponse(
    long Id,
    string Name,
    string Email,
    UserRole Role,
    UserStatus Status,
    DateTime CreatedAt,
    DateTime UpdatedAt);
