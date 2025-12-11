# CloudWatch Log Group for Credit Card
resource "aws_cloudwatch_log_group" "credit_card" {
  name              = "/ecs/credit-card"
  retention_in_days = 7

  tags = {
    Name = "credit-card-logs"
  }
}

# ECS Task Definition for Credit Card
resource "aws_ecs_task_definition" "credit_card" {
  family                   = "credit-card"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "1024"   # 1 vCPU
  memory                   = "2048" # 2 GB
  execution_role_arn       = data.aws_iam_role.lab_role.arn
  task_role_arn            = data.aws_iam_role.lab_role.arn

  container_definitions = jsonencode([{
    name      = "credit-card"
    image     = "${aws_ecr_repository.credit_card.repository_url}:latest"
    essential = true

    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]

    environment = [
      {
        name  = "PORT"
        value = "8080"
      }
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.credit_card.name
        "awslogs-region"        = "us-east-1"
        "awslogs-stream-prefix" = "credit-card"
      }
    }
  }])

  tags = {
    Name = "credit-card-task-definition"
  }
}

# ECS Service for Credit Card
resource "aws_ecs_service" "credit_card" {
  name            = "credit-card-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.credit_card.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.ecs_services.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.payment_tg.arn
    container_name   = "credit-card"
    container_port   = 8080
  }

  depends_on = [
    aws_lb_listener.http,
    aws_ecs_service.rabbitmq  # Wait for RabbitMQ to be running
  ]

  tags = {
    Name = "credit-card-ecs-service"
  }
}

# Outputs
output "credit_card_service_name" {
  description = "credit card ECS Service name"
  value       = aws_ecs_service.credit_card.name
}