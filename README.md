# Midas Core

A Spring Boot application built as part of the [JPMC Advanced Software Engineering Forage](https://www.theforage.com/) program. This project simulates a financial transaction processing system that consumes Kafka messages, validates and processes peer-to-peer transfers, integrates with an external incentive API, and exposes a REST endpoint for balance queries.

## Branch Structure

| Branch    | Description                                         |
|-----------|-----------------------------------------------------|
| `develop` | Completed implementation (default branch)           |
| `flow`    | Original starter template from the Forage program   |

## Tech Stack

- **Java 17**
- **Spring Boot 3.2** — Web, Data JPA
- **Spring Kafka** — message consumption with custom serializers
- **H2** — in-memory database
- **JUnit 5 + Mockito** — unit tests
- **Embedded Kafka** — integration test support

## Completed Tasks

| Task | Objective | Key Implementation |
|------|-----------|-------------------|
| **Task 1** | Project setup | Added Spring Boot, Kafka, JPA, H2, and test dependencies; configured application properties |
| **Task 2** | Kafka ingestion | Implemented a Kafka listener and custom `Transaction` serializer/deserializer for embedded Kafka tests |
| **Task 3** | Transaction persistence | Added transaction validation, JPA entities/repositories, balance updates, and valid-transaction persistence |
| **Task 4** | Incentive API integration | Added `RestTemplate` client logic to call the Incentive API and credit recipient incentives safely |
| **Task 5** | Balance REST endpoint | Exposed `GET /balance?userId=` returning a JSON `Balance`, defaulting to `0` for unknown users |

## Features

- **Kafka consumer** listens on a configurable topic and deserializes `Transaction` messages
- **Transaction processing** validates sender/recipient existence and sender balance before updating accounts
- **Invalid transaction handling** discards missing-user or insufficient-funds transactions without mutating balances
- **Incentive integration** calls an external API per transaction; positive incentives are credited to the recipient, with graceful fallback on failure
- **Persistence** stores processed transactions as `TransactionRecord` entities via Spring Data JPA
- **Balance API** returns a user's current balance via `GET /balance?userId={id}`
- **Unit tests** for `TransactionService` covering success, insufficient funds, missing users, and incentive edge cases

## Design Notes

- **Kafka boundary:** `TransactionConsumer` keeps message ingestion separate from transaction processing by delegating business logic to `TransactionService`.
- **Transactional processing:** Balance updates and transaction persistence run inside a single service-level transaction.
- **Validation first:** Invalid transactions are rejected before any database writes are performed.
- **Incentive fallback:** If the Incentive API is unavailable or returns no usable response, the transaction is still processed with a `0` incentive.

## Project Structure

```
src/main/java/com/jpmc/midascore/
├── config/          # Spring bean configuration (RestTemplate)
├── controller/      # REST endpoints (balance query)
├── dto/             # Data transfer objects (Incentive)
├── entity/          # JPA entities (UserRecord, TransactionRecord)
├── exception/       # Custom exceptions
├── kafka/
│   ├── consumer/    # Kafka message listener
│   └── serializer/  # Custom Kafka serializers
├── repository/      # Spring Data JPA repositories
└── service/         # Core business logic (TransactionService)
```

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.6+

### Run the Application

```bash
./mvnw spring-boot:run
```

The server starts on port **33400** (configured in `application.yml`).

### Run Tests

Run all Forage verification tests:

```bash
./mvnw test
```

Or run a specific task test:

```bash
./mvnw test -Dtest=TaskOneTests
./mvnw test -Dtest=TaskTwoTests
./mvnw test -Dtest=TaskThreeTests
./mvnw test -Dtest=TaskFourTests
./mvnw test -Dtest=TaskFiveTests
```

Run unit tests:

```bash
./mvnw test -Dtest=TransactionServiceTest
```

For Task 4 and Task 5 verification, start the provided Incentive API first:

```bash
java -jar services/transaction-incentive-api.jar
```

## API

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/balance?userId={id}` | Returns the balance for the given user |

## Acknowledgments

Starter code and task framework provided by the JPMC Forage program.
