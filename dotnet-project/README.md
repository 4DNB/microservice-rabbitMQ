# .NET Sales Microservices

ASP.NET Core 9 implementation of the sales application. It provides the same three-service API surface as the Java implementation:

- `backend/UserService`: users on port 8081
- `backend/ProductsService`: products and inventory on port 8082
- `backend/OrderService`: orders on port 8083

## Prerequisites

- .NET 9 SDK
- PostgreSQL
- Node.js and npm for the shared frontend

## Database setup

Create the databases once:

```sql
CREATE DATABASE sales_user_db;
CREATE DATABASE sales_product_db;
CREATE DATABASE sales_order_db;
```

## Configuration

Set the inventory key in both the Products and Orders processes:

```powershell
$env:INVENTORY_SERVICE_API_KEY = "replace-with-a-random-secret"
```

Supported configuration variables:

| Variable | Service | Default |
|---|---|---|
| `DB_URL` | User | PostgreSQL connection string for `sales_user_db` |
| `PRODUCT_DB_URL` | Products | PostgreSQL connection string for `sales_product_db` |
| `ORDER_DB_URL` | Orders | PostgreSQL connection string for `sales_order_db` |
| `INVENTORY_SERVICE_API_KEY` | Products and Orders | Empty |
| `PRODUCTS_SERVICE_URL` | Orders | `http://localhost:8082` |

## Run the backend

Run each service in a separate terminal from this directory:

```powershell
dotnet run --project backend/UserService
dotnet run --project backend/ProductsService
dotnet run --project backend/OrderService
```

The services listen only on localhost:

- User service: `http://localhost:8081/api/v1/users`
- Products service: `http://localhost:8082/api/v1/products`
- Internal inventory reservation: `http://localhost:8082/internal/v1/inventory/reservations`
- Orders service: `http://localhost:8083/api/v1/orders`

EF Core creates the required tables with `EnsureCreated` on startup.

## Run the frontend

The .NET services and Vite frontend share the ports 8081-8083 and 5173:

```powershell
cd ..\frontend
npm install
npm run dev
```

Open the URL printed by Vite.

## Build

Build the complete solution:

```powershell
dotnet build .\dotnet_sales_application.sln
```

## API behavior

- Product stock is reserved synchronously during order confirmation.
- A rejected inventory reservation marks the order as rejected.
- A temporarily unavailable Products service leaves the order pending.
- `POST /api/v1/orders` requires an `Idempotency-Key` header containing a GUID.
- Reusing an idempotency key with a different request is rejected.
