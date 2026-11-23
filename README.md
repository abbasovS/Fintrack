# FinTrack

FinTrack is a personal finance application for tracking transactions, categories, budgets, subscriptions, recurring expenses, and financial reports.

The project contains:

- A Spring Boot API with PostgreSQL, Liquibase migrations, JWT authentication, email verification, and OpenAPI support.
- A React + TypeScript frontend with dashboard, transactions, categories, budgets, subscriptions, recurring expenses, reports, and profile management.
- Local infrastructure for PostgreSQL, Mailpit, Vault, Consul, Prometheus, and Grafana through Docker Compose.

## Technology stack

- Java 21 and Spring Boot
- PostgreSQL and Liquibase
- React, TypeScript, and Vite
- Docker Compose
- Prometheus and Grafana

## Requirements

- Java 21
- Node.js 20.19 or newer
- Docker Desktop or Docker Engine with Compose

## Local setup

1. Create a local environment file from the safe template:

   ```bash
   cp .env.example .env
   ```

2. Replace the SMTP placeholders in `.env` if you want to use Gmail or another provider. For local development, Mailpit can be used without real credentials:

   ```dotenv
   SMTP_HOST=localhost
   SMTP_PORT=1025
   SMTP_USERNAME=
   SMTP_PASSWORD=
   SMTP_AUTH=false
   SMTP_STARTTLS=false
   MAIL_FROM=no-reply@fintrack.local
   ```

3. Start the local services:

   ```bash
   docker compose up -d
   ```

4. Start the API. The application expects database and secret values such as `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `VAULT_TOKEN` to be provided by the local environment or a secret manager.

   ```bash
   ./gradlew bootRun
   ```

   The API runs on `http://localhost:8081`. OpenAPI UI is available at `/swagger-ui/index.html`.

5. Start the frontend in another terminal:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   Open `http://localhost:5173`.

Mailpit is available at `http://localhost:8025`. Prometheus uses port `9090`, Grafana uses port `3000`, Vault uses port `8200`, and Consul uses port `8500`.

## Tests and builds

Run backend tests:

```bash
./gradlew test
```

Build the frontend:

```bash
cd frontend
npm run build
```

## Security

Never commit `.env`, SMTP credentials, Vault tokens, private keys, database dumps, IDE settings, or generated build output. Use `.env.example` only as a placeholder template and store real deployment secrets in the deployment platform's secret manager.

If a credential has ever been exposed, rotate or revoke it before publishing the repository.

## Project structure

```text
src/main/java/       Spring Boot API
src/main/resources/  application configuration and Liquibase migrations
src/test/             backend tests
frontend/             React frontend
monitoring/           Prometheus and Grafana configuration
vault/                local Vault configuration
docker-compose.yml    local infrastructure
```
