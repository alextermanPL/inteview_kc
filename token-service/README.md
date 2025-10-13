# Token Service (Camel + Quarkus + Kotlin)

- Port: `8081`
- Endpoint: `GET /token?code=<auth_code>`
- Flow: Keycloak code → access token → call transactions-service → publish to Kafka `user-transactions` → return 200.

## Run

1) Infra (Keycloak, Postgres, Kafka):

```
docker compose up -d postgres keycloak zookeeper kafka
```

2) Services:

```
./gradlew :transactions-service:quarkusDev
./gradlew :token-service:quarkusDev
```

3) OAuth login (opens Keycloak login):

```
http://localhost:8080/realms/finance-app/protocol/openid-connect/auth?client_id=finance-client&redirect_uri=http://localhost:8081/token&response_type=code&scope=openid
```

## Config

See `src/main/resources/application.properties`:
- `keycloak.token.url`: token endpoint
- `keycloak.client.id`: client id
- `transactions.api.url`: transactions-service endpoint
- `camel.component.kafka.brokers`: Kafka brokers
- `kafka.publish.endpoint`: publish endpoint (default `kafka:user-transactions`)

## Tests

```
./gradlew :token-service:test
```

- Processor unit tests.
- E2E route test (uses `direct:` and `mock:kafka`).
