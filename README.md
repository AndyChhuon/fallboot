# FallBoot

Not Spring Boot, Not football, Fall Boot

## Tech Stack

- Java 25
- Spring Boot 4.0.2
- PostgreSQL (database)
- Maven (build)

## Prerequisites

- Java 25
- Docker & Docker Compose
- Maven (or use the included `./mvnw` wrapper)

## Getting Started

### 1. Start the database

```bash
docker compose up -d
```

This starts a PostgreSQL 17 container on `localhost:5432`.

### 2. Run the application

```bash
./mvnw spring-boot:run
```

### 3. Stop the database

```bash
docker compose down
```

To wipe the database and start fresh:

```bash
docker compose down -v
```
