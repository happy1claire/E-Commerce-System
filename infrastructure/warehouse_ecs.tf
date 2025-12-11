# CloudWatch Log Group for warehouse
resource "aws_cloudwatch_log_group" "warehouse" {
  name              = "/ecs/warehouse"
  retention_in_days = 7

  tags = {
    Name = "warehouse-logs"
  }
}

# ECS Task Definition for warehouse
resource "aws_ecs_task_definition" "warehouse" {
  family                   = "warehouse"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "1024"   # 1 vCPU
  memory                   = "2048" # 2 GB
  execution_role_arn       = data.aws_iam_role.lab_role.arn
  task_role_arn            = data.aws_iam_role.lab_role.arn

  container_definitions = jsonencode([{
    name      = "warehouse"
    image     = "${aws_ecr_repository.warehouse.repository_url}:latest"
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
        name  = "SPRING_PROFILES_ACTIVE"
        value = "prod"
      },
      {
        name  = "LISTENER_CONCURRENCY"
        value = "10"
      }
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.warehouse.name
        "awslogs-region"        = "us-east-1"
        "awslogs-stream-prefix" = "warehouse"
      }
    }
  }])

  tags = {
    Name = "warehouse-task-definition"
  }
}

# ECS Service for warehouse
resource "aws_ecs_service" "warehouse" {
  name            = "warehouse-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.warehouse.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.ecs_services.id]
    assign_public_ip = true
  }
  load_balancer {
    target_group_arn = aws_lb_target_group.warehouse_tg.arn
    container_name   = "warehouse"
    container_port   = 8080
  }

  depends_on = [
    aws_lb_listener.http,
    aws_ecs_service.rabbitmq  # Wait for RabbitMQ to be running
  ]


  tags = {
    Name = "warehouse-ecs-service"
  }
}

# Outputs
output "warehouse_service_name" {
  description = "warehouse ECS Service name"
  value       = aws_ecs_service.warehouse.name
}