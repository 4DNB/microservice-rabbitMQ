# Sales Application Frontend

React 19, TypeScript, and Vite interface for the sales microservices.

## Prerequisites

- Node.js
- npm
- A running Java or .NET backend

## Run

```powershell
npm install
npm run dev
```

Open the URL printed by Vite, normally `http://localhost:5173`.

## Proxy configuration

Vite forwards frontend API calls to the local backend ports:

| Frontend path | Backend |
|---|---|
| `/api/v1/users` | `http://localhost:8081` |
| `/api/v1/products` | `http://localhost:8082` |
| `/api/v1/orders` | `http://localhost:8083` |

## Commands

```powershell
npm run dev      # Start the development server
npm run build    # Type-check and build for production
npm run lint     # Run Oxlint
npm run preview  # Preview the production build
```
