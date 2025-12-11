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
  cpu                      = "512"  # 0.5 vCPU
  memory                   = "1024" # 1 GB
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
      },
      {
        name  = "WAREHOUSE_SHIP_ENDPOINT"
        value = "http://${aws_lb.main.dns_name}/warehouse/ship"
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

    lifecycle {
    ignore_changes = [desired_count]
  }
}

resource "aws_appautoscaling_target" "warehouse_target" {
  max_capacity       = 3    # Maximum number of tasks to run
  min_capacity       = 1    # Minimum number of tasks to run
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.warehouse.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "warehouse_cpu" {
  name               = "warehouse-cpu-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.warehouse_target.resource_id
  scalable_dimension = aws_appautoscaling_target.warehouse_target.scalable_dimension
  service_namespace  = aws_appautoscaling_target.warehouse_target.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    
    # Keep average CPU at 70%. If it goes higher, scale up. Lower, scale down.
    target_value       = 70.0 
    scale_in_cooldown  = 300
    scale_out_cooldown = 60
  }
}

resource "aws_appautoscaling_policy" "warehouse_memory" {
  name               = "warehouse-memory-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.warehouse_target.resource_id
  scalable_dimension = aws_appautoscaling_target.warehouse_target.scalable_dimension
  service_namespace  = aws_appautoscaling_target.warehouse_target.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageMemoryUtilization"
    }
    
    target_value       = 80.0
    scale_in_cooldown  = 300
    scale_out_cooldown = 60
  }
}

# Outputs
output "warehouse_service_name" {
  description = "warehouse ECS Service name"
  value       = aws_ecs_service.warehouse.name
}