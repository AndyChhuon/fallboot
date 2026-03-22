# FallBoot

A real-time collaborative pixel canvas (r/Place like) built to scale under REST + WebSocket load.

## Goal

Push requests per second and concurrent users as high as possible for a scenario where a suddent burst of users each:
1. Fetch available rooms (`GET /api/rooms`)
2. Fetch all pixels in a room (`GET /api/pixels/room/{roomId}`) to initialize the canvas (1000x1000)
3. Connect via WebSocket (STOMP)
4. Send and receive real-time pixel updates

## Tech Stack

- Java 25 / Spring Boot 4.0.2 / Maven
- PostgreSQL 17 (persistence)
- Redis (distributed cache)
- Caffeine (in-process cache)
- Apache Kafka (async processing of writes)
- STOMP over WebSocket (real-time messaging)
- Gatling (load testing)

## Load Test Runs

**Peak:** 10,000 concurrent users | 91.7% success | ~11,628 req/sec  
**Stable:** 5,000 concurrent users | 100% success | ~7,751 req/sec

### Run 1 — Baseline ([`60a5891`](../../commit/60a5891))
| Metric | Value |
|---|-------|
| Concurrent users | 1,000 |
| Success rate | 5.3%  |
| Peak req/sec | ~502  |

**Bottleneck:** HikariCP connection pool exhaustion. Every request checked the PostgreSQL DB to provision the user on auth, leading to `Unable to acquire JDBC Connection, request timed out after 30000ms`. Under load, all connections were used and threads starved.

[Results](load-test-runs/run-1-60a5891/req-results.png) | [Req/sec](load-test-runs/run-1-60a5891/req-per-sec.png) | [Bottleneck](load-test-runs/run-1-60a5891/bottleneck.png)

---

### Run 2 — Redis cache ([`b9a2d77`](../../commit/b9a2d77))
| Metric | Value |
|---|---|
| Concurrent users | 1,000 |
| Success rate | 5.1% |
| Peak req/sec | ~421 |

**Bottleneck:** Lettuce (Redis client) thread contention. Redis handled caching, but under load the Lettuce threads themselves became blocked, stalling requests. Redis alone didn't move the needle.

[Results](load-test-runs/run-2-b9a2d77/req-results.png) | [Req/sec](load-test-runs/run-2-b9a2d77/req-per-sec.png) | [Bottleneck](load-test-runs/run-2-b9a2d77/bottleneck.png)

---

### Run 3 — Caffeine in-process cache ([`f8db95b`](../../commit/f8db95b))
| Metric | Value |
|---|---|
| Concurrent users | 2,000 |
| Success rate | 96.1% |
| Peak req/sec | ~3,592 |

**Bottleneck:** STOMP `clientInboundChannel` thread contention. With reads being fast, the write path became the bottleneck. Synchronous DB writes on the inbound channel threads blocked STOMP message handling and outbound broadcasts.

[Results](load-test-runs/run-3-f8db95b/req-results.png) | [Req/sec](load-test-runs/run-3-f8db95b/req-per-sec.png) | [Bottleneck](load-test-runs/run-3-f8db95b/bottleneck.png)

---

### Run 4 — Async thread pool for persistence ([`c19c271`](../../commit/c19c271))
| Metric | Value  |
|---|--------|
| Concurrent users | 2,000  |
| Success rate | 99.5%  |
| Peak req/sec | ~5,784 |

**Bottleneck:** Under extreme load, the async thread pool (50 threads) and queue (20,000) saturate. With queue being full, `AbortPolicy` throws `TaskRejectedException`, killing the STOMP broadcast for that message.

[Results](load-test-runs/run-4-c19c271/req-results.png) | [Req/sec](load-test-runs/run-4-c19c271/req-per-sec.png) | [Bottleneck](load-test-runs/run-4-c19c271/bottleneck.png)

---

### Run 5 — Kafka + batched WebSocket broadcasts ([`06e2670`](../../commit/06e2670))
| Metric | Value |
|---|---|
| Concurrent users | 10,000 |
| Success rate | 91.7% |
| Peak req/sec | ~11,628 |

**Bottleneck:** 10k users trigger `findByCognitoId` DB lookups simultaneously through `JwtUserProvisioningFilter` to provision the user, which overwhelms the 50 HikariCP connections.

[Results](load-test-runs/run-5-06e2670/req-result.png) | [Req/sec](load-test-runs/run-5-06e2670/req-per-sec.png) | [Bottleneck](load-test-runs/run-5-06e2670/bottleneck.png)

## Getting Started (Local)

### 1. Start dependencies

```bash
docker compose up -d
```

This starts PostgreSQL 17, Redis, Kafka, and RabbitMQ locally.

### 2. Run the application

```bash
./mvnw clean install

# With Cognito auth
./mvnw spring-boot:run -pl fallboot-backend

# With mock JWT auth (dev/load testing — no Cognito needed)
./mvnw spring-boot:run -pl fallboot-backend -Dspring-boot.run.profiles=loadtest
```

When using the `loadtest` profile, start a load test simulation first to run the mock JWKS server on port 9999. See `fallboot-load-testing/README.md` for details.

### 3. Run the kafka microservice

```bash
./mvnw spring-boot:run -pl fallboot-kafka -Dspring-boot.run.profiles=dev
```

### 4. Run the load test

```bash
cd fallboot-load-testing

# Full simulation: 2000 users, 10s ramp, 10s duration
./mvnw gatling:test -Dgatling.simulationClass=com.andy.loadtest.simulation.FullLoadSimulation -Dusers=2000 -DrampSeconds=10 -DdurationSeconds=10
```

Press Enter in the simulation terminal once the backend is ready. View the report:

```bash
open target/gatling/*/index.html
```

See [fallboot-load-testing/README.md](fallboot-load-testing/README.md) for more configuration options.

### 5. Clear the services

```bash
docker exec fallboot-postgres-dev psql -U myUser -d fallboot -c "DELETE FROM pixel;"
docker exec fallboot-postgres-dev psql -U myUser -d fallboot -c "INSERT INTO room (id, room_name) VALUES ('c55c81a0-806c-4108-b393-500d88851d88', 'default') ON CONFLICT DO NOTHING"
docker exec fallboot-redis-1 redis-cli FLUSHALL
docker exec fallboot-kafka-1 /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic pixel-updates
docker restart fallboot-rabbitmq-dev
```

### 6. Stop the services

```bash
docker compose down
```

To wipe the database and start fresh:

```bash
docker compose down -v
```

---

## Deploying to AWS

### Prerequisites

- [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) installed and configured (`aws configure sso`)
- [Terraform](https://developer.hashicorp.com/terraform/install) installed
- [Docker](https://docs.docker.com/get-docker/) installed
- Copy `terraform/infra/terraform.tfvars.example` to `terraform/infra/terraform.tfvars` and set your RDS password

### Infrastructure

The Terraform config is split into two directories:

- `terraform/persistent/` — ECR image repositories (kept alive between test runs)
- `terraform/infra/` — everything else: VPC, ECS, RDS, ElastiCache, Amazon MQ, MSK, ALB (destroyed after testing to save cost)

### 1. Create ECR repositories (one-time)

```bash
cd terraform/persistent
terraform init
terraform apply
```

Note the ECR URLs from the output.

### 2. Build and push Docker images

```bash
./scripts/push-images.sh
```

This reads the ECR URLs from the persistent Terraform state, builds both images, and pushes them.

### 3. Deploy infrastructure

```bash
cd terraform/infra
terraform init
terraform apply
```

Takes ~15-20 minutes. Outputs the ALB URL when done.

### 4. Create the room

RDS is publicly accessible for testing purposes, so connect directly from your machine:

```bash
psql -h $(cd terraform/infra && terraform output -raw rds_endpoint | cut -d: -f1) -U myUser -d fallboot -c "INSERT INTO room (id, room_name) VALUES ('c55c81a0-806c-4108-b393-500d88851d88', 'default') ON CONFLICT DO NOTHING;"
```

### 5. Run load test against AWS

```bash
cd fallboot-load-testing

./mvnw gatling:test \
  -Dgatling.simulationClass=com.andy.loadtest.simulation.FullLoadSimulation \
  -DbaseUrl=http://<alb-dns-name> \
  -DwsUrl=ws://<alb-dns-name> \
  -DmockJwksUrl=http://<alb-dns-name>:9999 \
  -Dusers=10000 \
  -DrampSeconds=10 \
  -DdurationSeconds=10
```

Replace `<alb-dns-name>` with the ALB URL from `terraform output`. The `-DmockJwksUrl` flag tells the load test to fetch JWT tokens from the deployed mock JWKS server (port 9999 on the same ALB) instead of running one locally.

### 6. Scale backend instances

```bash
terraform apply -var="fallboot_backend_count=8"
```

### 7. Tear down

```bash
terraform destroy
```

This removes all infrastructure except ECR repos. Images are preserved for the next deploy.
