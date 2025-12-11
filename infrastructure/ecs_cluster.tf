# ECS Cluster (if you don't already have one)
resource "aws_ecs_cluster" "main" {
  name = "ecommerce-cluster"

  tags = {
    Name = "ecommerce-cluster"
  }
}