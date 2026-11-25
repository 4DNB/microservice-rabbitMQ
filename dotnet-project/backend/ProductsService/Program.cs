using Microsoft.EntityFrameworkCore;
using ProductsService.Application;
using ProductsService.Infrastructure;

var builder = WebApplication.CreateBuilder(args);
builder.WebHost.ConfigureKestrel(options => options.ListenLocalhost(8082));
builder.Services.AddControllers();
builder.Services.AddCors(options => options.AddDefaultPolicy(policy =>
    policy.WithOrigins("http://localhost:5173", "http://127.0.0.1:5173").AllowAnyHeader().AllowAnyMethod()));
builder.Services.AddScoped<IProductsApplicationService, ProductsApplicationService>();
builder.Services.AddProductsInfrastructure(builder.Configuration);

var app = builder.Build();
app.UseCors();
app.MapControllers();

await using (var scope = app.Services.CreateAsyncScope())
{
    await scope.ServiceProvider.GetRequiredService<ProductDbContext>().Database.EnsureCreatedAsync();
}

app.Run();
