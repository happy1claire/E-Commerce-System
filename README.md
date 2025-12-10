# cs6650-assignment5

```bash
terraform init

# first create ECR repository
terraform apply \
-target=aws_ecr_repository.productdb \

# give permisson to script files
chmod +x ./*.sh

# build and push images to ECR
./push_productdb_image.sh

# then create everything else
terraform apply

# remember to destroy when done
terraform destroy
```

To let the database work fine, we need to set the `peers` for our nodes. We can get the private IPs of the nodes from the AWS console.

The nodes has an endpoint `/config/peers` to set the peers list. We can use `curl` to set the peers for each node. For example, if we have five nodes with private IPs

```
172.31.73.175
172.31.26.167
172.31.45.54
172.31.54.120
172.31.95.30
```

we can set the peers as follows:

```bash
curl -X POST "{{baseUrl}}/config/peers" \
     -H "Content-Type: application/json" \
     -d '[
            "http://172.31.73.175:8080",
            "http://172.31.26.167:8080",
            "http://172.31.45.54:8080",
            "http://172.31.54.120:8080",
            "http://172.31.95.30:8080"
         ]'
```

The baseUrl can be replaced with the public IP of each node respectively or use the load balancer address to set the peers for all nodes at once. If using load balancer address, make sure to run the command multiple times until all nodes have their peers set.

Send create product request to database

```bash
curl -X POST http://{{aws_loadbalancer_address}}/product \
     -H "Content-Type: application/json" \
     -d '{
           "id": "prod-001",
           "name": "Wireless Headphones",
           "price": 99.99,
           "description": "Noise-cancelling over-ear headphones."
         }'
```

Send get product request to database

```bash
curl -X GET http://{{aws_loadbalancer_address}}/get/{{key}}
```

Change the `{{key}}` to the product ID you created earlier.

