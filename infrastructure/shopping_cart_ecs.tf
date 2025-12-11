# CloudWatch Log Group for Shopping Cart
resource "aws_cloudwatch_log_group" "shopping_cart" {
  name              = "/ecs/shopping-cart"
  retention_in_days = 7

  tags = {
    Name = "shopping-cart-logs"
  }
}

# ECS Task Definition for Shopping Cart
resource "aws_ecs_task_definition" "shopping_cart" {
  family                   = "shopping-cart"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "2048"   # 1 vCPU
  memory                   = "4096" # 2 GB
  execution_role_arn       = data.aws_iam_role.lab_role.arn
  task_role_arn            = data.aws_iam_role.lab_role.arn

  container_definitions = jsonencode([{
    name      = "shopping-cart"
    image     = "${aws_ecr_repository.shopping_cart.repository_url}:latest"
    essential = true

    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]

    environment = [
      {
        name  = "RABBITMQ_HOST"
        value = aws_lb.rabbitmq.dns_name  # NLB DNS name!
      },
      {
        name  = "RABBITMQ_PORT"
        value = "5672"
      },
      {
        name  = "RABBITMQ_USER"
        value = "guest"
      },
      {
        name  = "RABBITMQ_PASS"
        value = "guest"
      },
      {
        name  = "RABBITMQ_MQ_CHANNEL_SIZE"
        value = "50"
      },
      {
        name  = "RABBITMQ_MQ_CONNECTION_LIMIT"
        value = "10"
      },
      {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "prod"
      },
      {
        name = "AUTH_SERVICE_URL",
        value = aws_lb.main.dns_name  # ALB DNS name for auth service
      },
      {
        name = "WAREHOUSE_SERVICE_URL",
        value = aws_lb.main.dns_name  # ALB DNS name for warehouse service
      },
      {
        name = "SERVER_PORT",
        value = "8080"
      },
      {
        name  = "CUSTOMER_DB_BASE_URL",
        value = aws_lb.customerdb.dns_name  # ALB DNS name for customer DB service
      },
      {
        name  = "SHOPPINGCART_DB_BASE_URL",
        value = aws_lb.shoppingcartdb.dns_name  # ALB DNS name for shopping cart DB service
      }
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.shopping_cart.name
        "awslogs-region"        = "us-east-1"
        "awslogs-stream-prefix" = "shopping-cart"
      }
    }
  }])

  tags = {
    Name = "shopping-cart-task-definition"
  }
}

# ECS Service for Shopping Cart
resource "aws_ecs_service" "shopping_cart" {
  name            = "shopping-cart-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.shopping_cart.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.ecs_services.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.cart_tg.arn
    container_name   = "shopping-cart"
    container_port   = 8080
  }

  depends_on = [
    aws_lb_listener.http,
    aws_ecs_service.rabbitmq  # Wait for RabbitMQ to be running
  ]

  tags = {
    Name = "shopping-cart-ecs-service"
  }
}

# Outputs
output "shopping_cart_service_name" {
  description = "Shopping Cart ECS Service name"
  value       = aws_ecs_service.shopping_cart.name
}