# FallBoot

A real-time collaborative pixel canvas (r/Place like) built to scale under REST + WebSocket load.

## Goal

Push requests per second and concurrent users as high as possible for a scenario where a sudden burst of users each:
1. Fetch available rooms (`GET /api/rooms`)
2. Fetch all pixels in a room (`GET /api/pixels/room/{roomId}`) to initialize the canvas (1000x1000)
3. Connect via WebSocket (STOMP)
4. Send and receive real-time pixel updates

## Tech Stack

- Java 25 / Spring Boot 4.0.2 / Maven
- PostgreSQL 17
- Redis (distributed cache + cross-instance Pub/Sub broadcasting)
- Caffeine (in-process cache)
- Apache Kafka (async persistence of writes)
- STOMP over WebSocket
- Java virtual threads
- Gatling (load testing)

### AWS Infrastructure

- Network Load Balancer across 3 AZs
- RDS PostgreSQL, ElastiCache Redis, MSK Kafka
- EC2 (load test instance)
- ECR (Docker image registry)
- ECS Fargate (backend + Kafka consumer + mock JWKS server)
- CloudWatch (container logging)
- Terraform for AWS provisioning

## Load Test Runs

**Peak stability:** 10,000 concurrent users | 99.2% success | ~10,388 req/sec (AWS)

### Run 1 — Baseline ([`60a5891`](../../commit/60a5891))
| Metric | Value |
|---|-------|
| Concurrent users | 1,000 |
| Success rate | 5.3%  |
| Peak req/sec | ~502  |

**What changed:** No caching, synchronous DB writes, single instance.

**Bottleneck:** HikariCP connection pool exhaustion. Every request checked the PostgreSQL DB to provision the user on auth, leading to `Unable to acquire JDBC Connection, request timed out after 30000ms`. Under load, all connections were used and threads starved.

[Results](load-test-runs/run-1-60a5891/req-results.png) | [Req/sec](load-test-runs/run-1-60a5891/req-per-sec.png) | [Bottleneck](load-test-runs/run-1-60a5891/bottleneck.png)

---

### Run 2 — Redis cache ([`b9a2d77`](../../commit/b9a2d77))
| Metric | Value |
|---|---|
| Concurrent users | 1,000 |
| Success rate | 5.1% |
| Peak req/sec | ~421 |

**What changed:** Added Redis as a distributed cache for user lookups and pixel data.

**Bottleneck:** Lettuce (Redis client) thread contention. Redis handled caching, but under load the Lettuce threads themselves became blocked, stalling requests. Redis alone didn't move the needle.

[Results](load-test-runs/run-2-b9a2d77/req-results.png) | [Req/sec](load-test-runs/run-2-b9a2d77/req-per-sec.png) | [Bottleneck](load-test-runs/run-2-b9a2d77/bottleneck.png)

---

### Run 3 — Caffeine in-process cache ([`f8db95b`](../../commit/f8db95b))
| Metric | Value |
|---|---|
| Concurrent users | 2,000 |
| Success rate | 96.1% |
| Peak req/sec | ~3,592 |

**What changed:** Added Caffeine as an in-process cache in front of Redis. User lookups and pixel data served from JVM heap with zero network hops.

**Bottleneck:** STOMP `clientInboundChannel` thread contention. With reads being fast, the write path became the bottleneck. Synchronous DB writes on the inbound channel threads blocked STOMP message handling and outbound broadcasts.

[Results](load-test-runs/run-3-f8db95b/req-results.png) | [Req/sec](load-test-runs/run-3-f8db95b/req-per-sec.png) | [Bottleneck](load-test-runs/run-3-f8db95b/bottleneck.png)

---

### Run 4 — Async thread pool for persistence ([`c19c271`](../../commit/c19c271))
| Metric | Value  |
|---|--------|
| Concurrent users | 2,000  |
| Success rate | 99.5%  |
| Peak req/sec | ~5,784 |

**What changed:** Moved DB writes to an async thread pool so STOMP inbound threads aren't blocked waiting on PostgreSQL.

**Bottleneck:** Under extreme load, the async thread pool (50 threads) and queue (20,000) saturate. With queue being full, `AbortPolicy` throws `TaskRejectedException`, killing the STOMP broadcast for that message.

[Results](load-test-runs/run-4-c19c271/req-results.png) | [Req/sec](load-test-runs/run-4-c19c271/req-per-sec.png) | [Bottleneck](load-test-runs/run-4-c19c271/bottleneck.png)

---

### Run 5 — Kafka + batched WebSocket broadcasts ([`06e2670`](../../commit/06e2670))
| Metric | Value |
|---|---|
| Concurrent users | 10,000 |
| Success rate | 91.7% |
| Peak req/sec | ~11,628 |

**What changed:** Replaced async thread pool with Kafka for pixel persistence. Added batched WebSocket broadcasts `PixelBatchService` which flushes every 50ms instead of broadcasting per-pixel.

**Bottleneck:** 10k users trigger `findByCognitoId` DB lookups simultaneously through `JwtUserProvisioningFilter` to provision the user, which overwhelms the 50 HikariCP connections.

[Results](load-test-runs/run-5-06e2670/req-result.png) | [Req/sec](load-test-runs/run-5-06e2670/req-per-sec.png) | [Bottleneck](load-test-runs/run-5-06e2670/bottleneck.png)

---

### Run 6 — Scale to AWS: Redis Pub/Sub + direct WebSocket broadcasts ([`f3c2693`](../../commit/f3c2693))
| Metric | Value   |
|---|---------|
| Concurrent users | 10,000  |
| Success rate | 99.2%   |
| Peak req/sec | ~10,388 |

| AWS Resource | Spec |
|---|---|
| fallboot-backend | 12 × ECS Fargate (4 vCPU / 8 GB) |
| fallboot-kafka | 1 × ECS Fargate (0.5 vCPU / 1 GB) |
| Database | RDS db.t3.large (PostgreSQL 17) |
| Cache | ElastiCache cache.t3.micro (Redis) |
| Kafka brokers | MSK 3 × kafka.m5.large |
| Load balancer | NLB across 3 AZs |
| Load test | EC2 c5.4xlarge (16 vCPU / 32 GB) |

**What changed:** Deployed to AWS with Terraform (ECS Fargate, NLB, RDS, ElastiCache, MSK). Replaced `messagingTemplate.convertAndSend()` with Redis Pub/Sub + `BroadcastSessionManager` that writes directly to WebSocket sessions, bypassing SimpleBroker's synchronized `DefaultSubscriptionRegistry`. Moved user provisioning off the hot path to Kafka (use `cognitoId` from JWT directly, no DB lookup). Switched from ALB to NLB for faster burst connection handling.

**Bottleneck:** `DefaultSubscriptionRegistry` synchronized lock causes STOMP's platform threads to bottleneck during CONNECTED handshake. 572 users timed out waiting for STOMP CONNECTED.

[Results](load-test-runs/run-6-f3c2693/req-result.png) | [Req/sec](load-test-runs/run-6-f3c2693/req-per-sec.png) | [Bottleneck](load-test-runs/run-6-f3c2693/bottleneck.png)

## Getting Started (Local)

### 1. Start dependencies

```bash
docker compose up -d
```

This starts PostgreSQL 17, Redis, and Kafka locally.

### 2. Run the application

```bash
./mvnw clean install

./mvnw spring-boot:run -pl fallboot-backend -Dspring-boot.run.profiles=loadtest
```

The `loadtest` profile uses the mock JWKS server (started by the load test) for JWT auth instead of AWS Cognito.

### 3. Run the Kafka consumer

```bash
./mvnw spring-boot:run -pl fallboot-kafka -Dspring-boot.run.profiles=dev
```

### 4. Run the load test

```bash
cd fallboot-load-testing

./mvnw gatling:test -Dgatling.simulationClass=com.andy.loadtest.simulation.FullLoadSimulation -Dusers=2000 -DrampSeconds=10 -DdurationSeconds=10
```

Press Enter in the simulation terminal once the backend is ready. View the report:

```bash
open target/gatling/*/index.html
```

### 5. Reset between runs

```bash
docker exec fallboot-postgres-dev psql -U myUser -d fallboot -c "DELETE FROM pixel; DELETE FROM users;"
docker exec fallboot-postgres-dev psql -U myUser -d fallboot -c "INSERT INTO room (id, room_name) VALUES ('c55c81a0-806c-4108-b393-500d88851d88', 'default') ON CONFLICT DO NOTHING"
docker exec fallboot-redis-1 redis-cli FLUSHALL
```

### 6. Stop services

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

- [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) installed
- [Terraform](https://developer.hashicorp.com/terraform/install) installed
- [Docker](https://docs.docker.com/get-docker/) installed

### 1. Configure AWS SSO (one-time)

```bash
aws configure sso
```

When prompted, use your SSO start URL, select your account/role, set region to `us-east-2`, and name the profile `fallboot`.

Login (repeat when session expires):

```bash
aws sso login --profile fallboot
```

### 2. Create ECR repositories (one-time)

```bash
cd terraform/persistent
terraform init
terraform apply
```

### 3. Create an SSH key pair for the load test EC2 (one-time)

```bash
aws ec2 create-key-pair --key-name fallboot-loadtest --profile fallboot --query 'KeyMaterial' --output text > ~/.ssh/fallboot-loadtest.pem && chmod 400 ~/.ssh/fallboot-loadtest.pem
```

### 4. Set your database password

```bash
cp terraform/infra/terraform.tfvars.example terraform/infra/terraform.tfvars
```

Edit `terraform/infra/terraform.tfvars` and set `db_password`. No spaces, `/`, `@`, `"`, `,`, `:`, or `=`.

### 5. Build and push Docker images

```bash
./scripts/push-images.sh
```

Reads ECR URLs from Terraform state, builds all 3 images for `linux/amd64`, and pushes to ECR.

### 6. Deploy infrastructure

```bash
cd terraform/infra
terraform init
terraform apply
```

Takes ~15-20 minutes (MSK + RDS). Outputs:
- `nlb_url` — backend NLB URL
- `mock_jwks_url` — mock JWKS server URL
- `rds_endpoint` — PostgreSQL endpoint
- `loadtest_ip` — EC2 instance IP for running Gatling

### 7. Create the room

```bash
PGPASSWORD=<your-db-password> psql -h $(cd terraform/infra && terraform output -raw rds_endpoint | cut -d: -f1) -U myUser -d fallboot -c "INSERT INTO room (id, room_name) VALUES ('c55c81a0-806c-4108-b393-500d88851d88', 'default') ON CONFLICT DO NOTHING;"
```

### 8. Set up the load test EC2

```bash
tar czf /tmp/loadtest.tar.gz --exclude='target' --exclude='.idea' fallboot-load-testing/
scp -i ~/.ssh/fallboot-loadtest.pem /tmp/loadtest.tar.gz ec2-user@$(cd terraform/infra && terraform output -raw loadtest_ip):~/
ssh -i ~/.ssh/fallboot-loadtest.pem ec2-user@$(cd terraform/infra && terraform output -raw loadtest_ip)
```

On the EC2 instance:

```bash
tar xzf loadtest.tar.gz && chmod +x fallboot-load-testing/mvnw
sudo yum install -y java-21-amazon-corretto-devel
```

### 9. Run load test from EC2

Replace `<nlb-dns>` with the NLB URL from `terraform output -raw nlb_url` (without `http://`):

```bash
export MAVEN_OPTS="-Xmx16g -Xms8g"
cd ~/fallboot-load-testing
yes "" | ./mvnw gatling:test \
  -Dgatling.simulationClass=com.andy.loadtest.simulation.FullLoadSimulation \
  -DbaseUrl=http://<nlb-dns> \
  -DwsUrl=ws://<nlb-dns> \
  -DmockJwksUrl=http://<nlb-dns>:443 \
  -Dusers=10000 \
  -DrampSeconds=10 \
  -DdurationSeconds=10
```

Copy the Gatling report to your machine to view:

```bash
scp -r -i ~/.ssh/fallboot-loadtest.pem ec2-user@<loadtest-ip>:~/fallboot-load-testing/target/gatling/<report-dir> /tmp/gatling-report && open /tmp/gatling-report/index.html
```

### 10. Reset between test runs

```bash
PGPASSWORD=<your-db-password> psql -h $(cd terraform/infra && terraform output -raw rds_endpoint | cut -d: -f1) -U myUser -d fallboot -c "DELETE FROM pixel; DELETE FROM users; INSERT INTO room (id, room_name) VALUES ('c55c81a0-806c-4108-b393-500d88851d88', 'default') ON CONFLICT DO NOTHING;"
```

### 11. Scale backend instances

```bash
cd terraform/infra
terraform apply -var="fallboot_backend_count=20"
```

### 12. Monitor during tests

```bash
aws logs tail /ecs/fallboot-backend --follow --profile fallboot

aws ecs describe-services --cluster fallboot-cluster --services fallboot-backend --profile fallboot --query 'services[0].{running:runningCount,desired:desiredCount}'
```

### 13. Tear down

```bash
cd terraform/infra
terraform destroy
```

Removes all infrastructure except ECR repos. Images are preserved for the next deploy.
