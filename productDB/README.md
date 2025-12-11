# Leaderless Replication with Quorum (W = N = 5, R = 1)

This ProductDB uses leaderless replication with:

- **Write Quorum W = 5 (all replicas must acknowledge)**
- **Read Quorum R = 1 (local read, may be stale)**

A write succeeds only if **all 5 replicas** confirm the update.  
A read is served locally and may return slightly stale data before propagation completes.

---

## Product Payload Format 

Your Product object now has the following schema:

```json
{
  "id": 1,
  "sku": "ABC-123",
  "manufacturer": "Nike",
  "categoryId": 5,
  "weight": 1200
}
```

## Controller Overview — ProductDbController

Your system exposes four main endpoints:

---

### 1. `POST /product` — Create Product (W = 5)
- Creates a new product
- Increments version and writes locally
- Propagates the update to **all replicas** (W = N)
- Write succeeds only if *every* replica acknowledges
- **Returns:**
    - `product`
    - `version`
    - `timestamp`

---

### 2. `PUT /product/{id}` — Update Product (W = 5)
- Updates an existing product
- Path variable `{id}` overrides payload `id`
- Increments version
- Propagates updated product to all replicas
- **Returns:**
    - `product`
    - `version`
    - `timestamp`

---

### 3. `GET /get/{id}` — Read Product (R = 1)
- Reads product from the **local replica only**
- May return slightly stale data depending on propagation delay
- **Returns:**
    - `product`
    - `version`
    - `timestamp`

---

### 4. `GET /local_read/{id}` — Local Debug Read
- Reads directly from the local store
- Does **not** perform quorum
- Used to debug:
    - local version
    - timestamp
    - whether propagation succeeded



Step 1: Run Your 5 Spring Boot Instances

You need to open 5 separate terminal windows and run one of the following commands in each. This will start each instance on a different port and ensure they all know about each other.

Node1
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="\
--server.port=8081 \
--self.url=http://localhost:8081 \
--peers=http://localhost:8081,http://localhost:8082,http://localhost:8083,http://localhost:8084,http://localhost:8085"
```

Node2
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="\
--server.port=8082 \
--self.url=http://localhost:8082 \
--peers=http://localhost:8081,http://localhost:8082,http://localhost:8083,http://localhost:8084,http://localhost:8085"
```

Node3
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="\
--server.port=8083 \
--self.url=http://localhost:8083 \
--peers=http://localhost:8081,http://localhost:8082,http://localhost:8083,http://localhost:8084,http://localhost:8085"
```

Node4
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="\
--server.port=8084 \
--self.url=http://localhost:8084 \
--peers=http://localhost:8081,http://localhost:8082,http://localhost:8083,http://localhost:8084,http://localhost:8085"
```

Node5
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="\
--server.port=8085 \
--self.url=http://localhost:8085 \
--peers=http://localhost:8081,http://localhost:8082,http://localhost:8083,http://localhost:8084,http://localhost:8085"
```


To test locally for write and read

After run the 5 instances

Step 1: Write to node 3

```bash
curl -X POST "http://localhost:8083/product" \
-H "Content-Type: application/json" \
-d '{
  "id": 1,
  "sku": "ABC-123",
  "manufacturer": "Nike",
  "categoryId": 10,
  "weight": 250
}'
```

Step 2: Immediately read node1 (stale read possible)

```bash
curl http://localhost:8081/get/1
```

Step 3: Local read to show version

```bash
curl http://localhost:8081/local_read/1
```

Step 4: Read after propagation completes

```bash
curl http://localhost:8081/get/1
curl http://localhost:8082/get/1
curl http://localhost:8084/get/1
curl http://localhost:8085/get/1
```

To test locally for update and read

After run the 5 instances

Step 1: add the product on node3
```bash
curl -X POST "http://localhost:8083/product" \
-H "Content-Type: application/json" \
-d '{
  "id": 1,
  "sku": "ABC-123",
  "manufacturer": "Nike",
  "categoryId": 10,
  "weight": 250
}'
```

Step 2: Update the product on node3

```bash
curl -X PUT "http://localhost:8083/product/1" \
-H "Content-Type: application/json" \
-d '{
  "id": 1,
  "sku": "CED-456",
  "manufacturer": "NB",
  "categoryId": 10,
  "weight": 250
}'
```

Step 3: Immediately read node1 (stale read possible)

```bash
curl http://localhost:8081/get/2
```

Step 4: Local read to show updated version

```bash
curl http://localhost:8081/local_read/1
```

Step 5: Read after propagation completes

```bash
curl http://localhost:8081/get/1
curl http://localhost:8082/get/1
curl http://localhost:8084/get/1
curl http://localhost:8085/get/1
```



