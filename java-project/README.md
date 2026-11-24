# Java Sales Microservices

Spring Boot 4 implementation of the sales application.

- `backend/user-service/`: User microservice
- `backend/products/`: Product catalog and inventory microservice
- `backend/order-service/`: Order microservice and inventory client

The services target Java 21 and use PostgreSQL with Spring Data JPA.

## Run

Create each PostgreSQL database only if it does not already exist:

```sql
CREATE DATABASE sales_user_db;
CREATE DATABASE sales_product_db;
CREATE DATABASE sales_order_db;
```

Each service owns its database. Existing product and order records in the old shared `sales_user_db` are not copied automatically; migrate them before switching existing installations.

### User service

```powershell
cd backend\user-service
.\mvnw.cmd spring-boot:run
```

The API runs at `http://localhost:8081/api/v1/users`.

### Products and inventory service

```powershell
cd backend\products
..\..\mvnw.cmd spring-boot:run
```

The catalog API runs at `http://localhost:8082/api/v1/products`. Inventory reservations are exposed to the order service at `/internal/v1/inventory/reservations`. By default, this service uses `sales_product_db`; configure `PRODUCT_DB_URL`, `PRODUCT_DB_USERNAME`, and `PRODUCT_DB_PASSWORD` to override it.
Set the same `INVENTORY_SERVICE_API_KEY` environment variable for the products and order services; the internal inventory endpoint rejects requests if it is missing or invalid.

```powershell
$env:INVENTORY_SERVICE_API_KEY = "replace-with-a-random-secret"
```

### Order service

```powershell
cd backend\order-service
..\..\mvnw.cmd spring-boot:run
```

The order API runs at `http://localhost:8083/api/v1/orders` and persists orders in its own `sales_order_db` database. It coordinates inventory over the products service API; it stores the user-service ID as an external `user_id` reference, not a cross-service database foreign key. Pending requests can be retried with the same `Idempotency-Key` header. Payment processing is not included.

## Build and test

From this directory:

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

## Frontend

The shared React frontend is outside this directory:

```powershell
cd ..\frontend
npm install
npm run dev
```

It runs at `http://localhost:5173` and proxies user requests to port 8081, product requests to port 8082, and order requests to port 8083.