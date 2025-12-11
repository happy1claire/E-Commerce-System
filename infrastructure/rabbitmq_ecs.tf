# CloudWatch Log Group for RabbitMQ
resource "aws_cloudwatch_log_group" "rabbitmq" {
  name              = "/ecs/rabbitmq"
  retention_in_days = 7

  tags = {
    Name = "rabbitmq-logs"
  }
}

# ECS Task Definition for RabbitMQ
resource "aws_ecs_task_definition" "rabbitmq" {
  family                   = "rabbitmq"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "1024"   # 1 vCPU
  memory                   = "3072"   # 3 GB
  execution_role_arn       = data.aws_iam_role.lab_role.arn
  task_role_arn            = data.aws_iam_role.lab_role.arn

  container_definitions = jsonencode([{
    name      = "rabbitmq"
    image     = "${aws_ecr_repository.rabbitmq.repository_url}:latest"
    essential = true

    portMappings = [
      {
        containerPort = 5672
        protocol      = "tcp"
        name          = "amqp"
      },
      {
        containerPort = 15672
        protocol      = "tcp"
        name          = "management"
      }
    ]

    environment = [
      {
        name  = "RABBITMQ_DEFAULT_USER"
        value = "guest"
      },
      {
        name  = "RABBITMQ_DEFAULT_PASS"
        value = "guest"
      }
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.rabbitmq.name
        "awslogs-region"        = "us-east-1"
        "awslogs-stream-prefix" = "rabbitmq"
      }
    }
  }])

  tags = {
    Name = "rabbitmq-task-definition"
  }
}

# ECS Service for RabbitMQ
resource "aws_ecs_service" "rabbitmq" {
  name            = "rabbitmq-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.rabbitmq.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.ecs_services.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.rabbitmq_amqp.arn
    container_name   = "rabbitmq"
    container_port   = 5672
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.rabbitmq_management.arn
    container_name   = "rabbitmq"
    container_port   = 15672
  }

  depends_on = [
    aws_lb_listener.rabbitmq_amqp,
    aws_lb_listener.rabbitmq_management
  ]

  tags = {
    Name = "rabbitmq-ecs-service"
  }
}