# Fallboot Load Testing

Gatling-based load tests for the Fallboot backend (REST + WebSocket/STOMP).

## Quick Start

1. Start the backend with loadtest profile (from fallboot root):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=loadtest
```

2. Run a simulation (from `fallboot-load-testing/`):

```bash
# Full simulation: 100 users, 30s ramp, 60s duration
./mvnw gatling:test -Dgatling.simulationClass=com.andy.loadtest.simulation.FullLoadSimulation -Dusers=100 -DrampSeconds=30 -DdurationSeconds=60

# REST only
./mvnw gatling:test -Dgatling.simulationClass=com.andy.loadtest.simulation.RestApiSimulation -Dusers=10 -DrampSeconds=10

# WebSocket only
./mvnw gatling:test -Dgatling.simulationClass=com.andy.loadtest.simulation.WebSocketStompSimulation -Dusers=10 -DrampSeconds=10
```

3. Press Enter in the simulation terminal once the backend is ready.

4. View the report:

```bash
open target/gatling/*/index.html
```

## Configuration

All values are configurable via `-D` system properties:

| Property | Default | Description |
|----------|---------|-------------|
| `users` | `100` | Number of virtual users |
| `rampSeconds` | `30` | Ramp-up duration (seconds) |
| `durationSeconds` | `30` | Test duration (seconds) |
| `pauseMillis` | `500` | Pause between pixel sends (ms) |
| `baseUrl` | `http://localhost:8080` | Backend HTTP URL |
| `wsUrl` | `ws://localhost:8080` | Backend WebSocket URL |
| `roomId` | `c55c81a0-...` | Room ID for pixel tests |