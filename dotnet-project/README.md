# .NET Sales Application

ASP.NET Core replacement for the Java sales microservices.

- `backend/UserService`: users on port 8081
- `backend/ProductsService`: products and inventory on port 8082
- `backend/OrderService`: orders on port 8083
- Uses PostgreSQL databases `sales_user_db`, `sales_product_db`, and `sales_order_db`
- Targets .NET 9

Create the databases once:

```sql
CREATE DATABASE sales_user_db;
CREATE DATABASE sales_product_db;
CREATE DATABASE sales_order_db;
```

Set the inventory key in the products and order processes:

```powershell
$env:INVENTORY_SERVICE_API_KEY = "replace-with-a-random-secret"
```

Run each service in a separate terminal:

```powershell
dotnet run --project backend/UserService
dotnet run --project backend/ProductsService
dotnet run --project backend/OrderService
```

Run the existing React frontend from `../java_sales_application/frontend`; its Vite proxy already targets ports 8081, 8082, and 8083.
