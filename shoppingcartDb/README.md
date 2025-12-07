# Leaderless Replication with Quorum (R = 3, W = 3)


To ensure consistency across 5 replicas, the system uses quorum-based replication:

Write Quorum: W = 3

Read Quorum: R = 3



You can adjust quorum values without modifying code in application.properties

```
propagation.read.quorum=3   # R value
propagation.write.quorum=3  # W value
```

## How to Run?

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


### To test locally for write and read

After run the 5 instances

Step 1: Write to node 3

```bash
curl -X POST "http://localhost:8083/item/userA" \
-H "Content-Type: application/json" \
-d '{"1": 2, "5": 3}'
```

Step 2: Immediately read node1 (stale read possible)

```bash
curl http://localhost:8081/get/userA
```

Step 3: Local read to show version

```bash
curl http://localhost:8081/local_read/userA
```

Step 4: Read after propagation completes

```bash
curl http://localhost:8081/get/userA
curl http://localhost:8082/get/userA
curl http://localhost:8083/get/userA
curl http://localhost:8084/get/userA
curl http://localhost:8085/get/userA
```

### To test locally for update and read

After run the 5 instances

Step 1: add the product on node3
```bash
curl -X POST "http://localhost:8083/item/userA" \
-H "Content-Type: application/json" \
-d '{"1": 2, "5": 3}'
```

Step 2: Update the product on node3

```bash
curl -X POST "http://localhost:8083/item/userA" \
-H "Content-Type: application/json" \
-d '{"1": 99, "8": 4}'
```

Step 3: Immediately read node1 

```bash
curl http://localhost:8081/get/userA
```

Step 4: Local read to show updated version

```bash
curl http://localhost:8081/local_read/userA
```

Step 5: Read after propagation completes

```bash
curl http://localhost:8081/get/userA
curl http://localhost:8082/get/userA
curl http://localhost:8083/get/userA
curl http://localhost:8084/get/userA
curl http://localhost:8085/get/userA
```



