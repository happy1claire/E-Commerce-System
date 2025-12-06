# Product Service

This is a simple Spring Boot microservice for managing products. It includes functionality to simulate a "bad" service for testing load balancing and health checks.

## Local Development

You can run and test the service on your local machine.

The service will start on `http://localhost:8080`.

#### Bad Service Mode

To simulate a bad service, run the service with the `BAD_SERVICE_MODE` environment variable set to `true`.

In this mode:
*   The `POST /product` endpoint will fail 50% of the time with a `503 Service Unavailable` error.
*   The `/actuator/health` endpoint will report `DOWN` 50% of the time.

### 3. Test the Endpoints

You can use `curl` or any API client to test the endpoints.

*   **Custom Health Check:**
    ```bash
    curl http://localhost:8080/health
    ```

*   **Spring Actuator Health Check:**
    ```bash
    curl http://localhost:8080/actuator/health
    ```

*   **Get a Product:**
    ```bash
    curl http://localhost:8080/products/123
    ```

*   **Create a Product:**
    ```bash
    curl -X POST -H "Content-Type: application/json" \
    -d '{"sku":"NEW-SKU-1", "manufacturer":"New Corp", "categoryId": 1, "weight": 100, "someOtherId": 200}' \
    http://localhost:8080/product
    ```

## AWS Deployment with Terraform

Refer to the instructions on README in assignment3 to guide you through deploying the service to AWS using ECR for container storage and ECS for container orchestration, all managed by Terraform.


### Test the Deployed Service

Use the `alb_dns_name` from the Terraform output to test your deployed `product-service`. The Application Load Balancer routes requests with paths starting with `/products` or `/product` to this service.

*   **Get a Product:**
    This request matches the `/products*` path rule.
    ```bash
    curl http://<alb_dns_name>/products/123
    ```

*   **Create a Product:**
    This request matches the `/product*` path rule. If you deployed a "bad" service instance, this request may fail 50% of the time with a `503 Service Unavailable` error.
    ```bash
    curl -X POST -H "Content-Type: application/json" \
    -d '{"sku":"NEW-SKU-1", "manufacturer":"New Corp", "categoryId": 1, "weight": 100, "someOtherId": 200}' \
    http://<alb_dns_name>/product
    ```

*   **Health Check:**
    Note that you cannot directly hit the `/actuator/health` endpoint through the ALB. The health check is performed internally by the ALB on the target group.
    The load balancer will automatically route traffic away from any instances that report an unhealthy status.

