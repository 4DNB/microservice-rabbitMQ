# Sales Microservices Application

This workspace contains a shared React storefront and two independent backend implementations:

- `java-project/`: Spring Boot 4 microservices with Java 21 and Maven
- `dotnet-project/`: ASP.NET Core microservices targeting .NET 9
- `frontend/`: React 19, TypeScript, and Vite storefront

The Java and .NET implementations expose compatible APIs. Run one backend implementation at a time and point the frontend proxy at its service ports.

## Architecture

| Service | Default port | Database | Responsibility |
|---|---:|---|---|
| User | 8081 | `sales_user_db` | Customer and administrative user data |
| Products | 8082 | `sales_product_db` | Product catalog and inventory |
| Orders | 8083 | `sales_order_db` | Order records and inventory reservations |

The frontend uses Vite proxy rules to route requests:

- `/api/v1/users` -> User service
- `/api/v1/products` -> Products service
- `/api/v1/orders` -> Orders service

The Orders service calls the Products service's internal inventory endpoint. It does not publish or consume RabbitMQ messages. The inventory operation is currently a synchronous HTTP request protected by a shared API key.

## Prerequisites

Choose one backend implementation:

### Java

- Java 21
- Maven is optional because the included Maven Wrapper is used
- PostgreSQL

### .NET

- .NET 9 SDK
- PostgreSQL

### Frontend

- Node.js and npm

## PostgreSQL setup

Create the three databases once:

```sql
CREATE DATABASE sales_user_db;
CREATE DATABASE sales_product_db;
CREATE DATABASE sales_order_db;
```

The services use `postgres/postgres` and `localhost:5432` by default. Override database values through environment variables described in the backend READMEs.

## Inventory API key

Set the same key in the Products and Orders processes:

```powershell
$env:INVENTORY_SERVICE_API_KEY = "replace-with-a-random-secret"
```

This key is required for the internal inventory reservation endpoint and prevents unauthorized stock changes.

## Quick start

### Java backend

From the repository root:

```powershell
$env:INVENTORY_SERVICE_API_KEY = "replace-with-a-random-secret"
```

Start the three modules in separate terminals:

```powershell
cd java-project\backend\user-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd java-project\backend\products
..\..\mvnw.cmd spring-boot:run
```

```powershell
cd java-project\backend\order-service
..\..\mvnw.cmd spring-boot:run
```

See [java-project/README.md](java-project/README.md) for environment variables and API details.

### .NET backend

From the repository root:

```powershell
$env:INVENTORY_SERVICE_API_KEY = "replace-with-a-random-secret"

cd dotnet-project
dotnet run --project backend\UserService
dotnet run --project backend\ProductsService
dotnet run --project backend\OrderService
```

Run the last three commands in separate terminals. See [dotnet-project/README.md](dotnet-project/README.md) for .NET-specific configuration.

### Frontend

In a separate terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open the local URL printed by Vite, normally `http://localhost:5173`.

## API summary

| Method and path | Description |
|---|---|
| `GET /api/v1/users` | List users |
| `POST /api/v1/users` | Create a user |
| `PUT /api/v1/users/{id}` | Update a user |
| `DELETE /api/v1/users/{id}` | Delete a user |
| `GET /api/v1/products` | List products |
| `POST /api/v1/products` | Create a product |
| `PUT /api/v1/products/{id}` | Update a product |
| `DELETE /api/v1/products/{id}` | Delete a product |
| `POST /api/v1/orders` | Create or retry an order; requires `Idempotency-Key` |

## Notes

- Each service owns its PostgreSQL database.
- Existing data is not migrated automatically between the Java and .NET implementations.
- The Orders service stores the User service ID as an external reference.
- Pending or retried orders use the supplied idempotency key to avoid duplicate processing.
- Payment processing is not implemented.
