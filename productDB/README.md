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

Step 2: Run the Integration Tests

With your 5 nodes running, open a sixth terminal and execute the tests using Maven

```bash
mvn test -Dtest=LeaderlessIntegrationTests
```

or you can run the tests directly from intelliJ


To test locally

After run the 5 instances

Step 1: Write to node 3

```bash
curl -X POST "http://localhost:8083/set" \
-H "Content-Type: application/json" \
-d '{"key":"foo","value":"bar"}'
```

Step 2: Immediately read node1 (stale read possible)

```bash
curl http://localhost:8081/get/foo
```

Step 3: Local read to show version

```bash
curl http://localhost:8081/local_read/foo
```

Step 4: Read after propagation completes

```bash
curl http://localhost:8081/get/foo
curl http://localhost:8082/get/foo
curl http://localhost:8084/get/foo
curl http://localhost:8085/get/foo
```


Use Docker to test:

Step 1: Start the cluster: open a terminal in your project's root directory

```bash
docker-compose up --build
```

Step 2: Run integration tests on new terminal

```bash
mvn test -Dtest=LeaderlessIntegrationTests
```

Step 3: Shut down the cluster

```bash
docker-compose down
```


