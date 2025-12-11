# CloudWatch Log Group for Product
resource "aws_cloudwatch_log_group" "product" {
  name              = "/ecs/product"
  retention_in_days = 7

  tags = {
    Name = "product-logs"
  }
}

# ECS Task Definition for Product
resource "aws_ecs_task_definition" "product" {
  family                   = "product"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "1024"   # 1 vCPU
  memory                   = "2048" # 2 GB
  execution_role_arn       = data.aws_iam_role.lab_role.arn
  task_role_arn            = data.aws_iam_role.lab_role.arn

  container_definitions = jsonencode([{
    name      = "product"
    image     = "${aws_ecr_repository.product.repository_url}:latest"
    essential = true

    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]

    environment = [
      {
        name  = "SERVER_PORT"
        value = "8080"
      }
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.product.name
        "awslogs-region"        = "us-east-1"
        "awslogs-stream-prefix" = "product"
      }
    }
  }])

  tags = {
    Name = "product-task-definition"
  }
}

# ECS Service for product
resource "aws_ecs_service" "product" {
  name            = "product-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.product.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.ecs_services.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.product_tg.arn
    container_name   = "product"
    container_port   = 8080
  }

  depends_on = [
    aws_lb_listener.http
  ]

  tags = {
    Name = "product-ecs-service"
  }
}