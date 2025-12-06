# Warehouse Service

This Spring Boot application provides a simple warehouse service with endpoints for reserving and shipping items. It also listens to a RabbitMQ queue for processing orders.

## Prerequisites

- Java 17
- RabbitMQ running and accessible (defaults to `localhost:5672`)

### Start RabbitMQ with Docker
```bash
docker run -d \
  --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  rabbitmq:3-management

```
RabbitMQ server listening on 5672

Default login:

user: guest

password: guest


## Running the Service

You can run the service in two main ways:

### 1. From your IDE

You can run the application directly from your IDE (like IntelliJ IDEA or VS Code) by locating the `WarehouseServiceApplication.java` file and running its `main` method.

### 2. Using the Gradle Wrapper

Open a terminal in the root directory of the `warehouse-service` project and use the appropriate command for your operating system.

**On macOS/Linux:**
```bash
./gradlew bootRun
```

**On Windows:**
```bash
gradlew.bat bootRun
```

Once started, the service will be available at `http://localhost:8080`.

## Testing the API Endpoints

You can use a command-line tool like `curl` to interact with the API.

### Reserve Items (`/warehouse/reserve`)

This endpoint simulates an inventory check with a 90% success rate.

**Command:**
```bash
curl -X POST http://localhost:8080/warehouse/reserve \
-H "Content-Type: application/json" \
-d '{"productId": 123, "quantity": 5}'
```

- A **successful** response will return an `HTTP 200 OK` status with a confirmation message.
- A **failed** response (due to "insufficient inventory") will return an `HTTP 409 Conflict` status.

### Ship Items (`/warehouse/ship`)

This endpoint simulates a shipment, which always succeeds and returns a unique tracking number.

**Command:**
```bash
curl -X POST http://localhost:8080/warehouse/ship \
-H "Content-Type: application/json" \
-d '{"productId": 456, "quantity": 2}'
```

- This request will always return an `HTTP 200 OK` status with shipment details, including a `trackingNumber`.