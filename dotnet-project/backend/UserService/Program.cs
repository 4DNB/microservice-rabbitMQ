using Microsoft.EntityFrameworkCore;
using UserService.Application;
using UserService.Infrastructure;

var builder = WebApplication.CreateBuilder(args);
builder.WebHost.ConfigureKestrel(options => options.ListenLocalhost(8081));
builder.Services.AddControllers();
builder.Services.AddCors(options => options.AddDefaultPolicy(policy =>
    policy.WithOrigins("http://localhost:5173", "http://127.0.0.1:5173").AllowAnyHeader().AllowAnyMethod()));
builder.Services.AddScoped<IUserApplicationService, UserApplicationService>();
builder.Services.AddUserInfrastructure(builder.Configuration);

var app = builder.Build();
app.UseCors();
app.MapControllers();

await using (var scope = app.Services.CreateAsyncScope())
{
    await scope.ServiceProvider.GetRequiredService<UserDbContext>().Database.EnsureCreatedAsync();
}

app.Run();
