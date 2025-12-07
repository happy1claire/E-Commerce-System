# --------------------------------------------------------------------------
# Security Group for Load Balancers (ALB & NLB)
# - Attached to: aws_lb.main (ALB) and aws_lb.rabbitmq (NLB)
# - Role: Receives public internet traffic on specific ports.
# --------------------------------------------------------------------------
resource "aws_security_group" "lb" {
  name        = "ecommerce-lb-sg"
  description = "Security group for all public-facing LBs (ALB & NLB)"
  vpc_id      = data.aws_vpc.default.id

  # --- Ingress (Inbound) ---

  # Port 80 - HTTP traffic from internet to ALB
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTP from internet to ALB"
  }

  # Port 15672 - RabbitMQ Management UI (for you to access dashboard via NLB)
  ingress {
    from_port   = 15672
    to_port     = 15672
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "RabbitMQ Management Console from internet to NLB"
  }

  # Port 5672 - RabbitMQ AMQP (for services within the VPC to connect to NLB)
  ingress {
    from_port   = 5672
    to_port     = 5672
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    # cidr_blocks = [data.aws_vpc.default.cidr_block] # Allow traffic from within the VPC
    description = "RabbitMQ AMQP from within VPC to NLB"
  }

  # --- Egress (Outbound) ---
  # LBs need to send traffic to the services on their respective ports.
  # We restrict this to ONLY the ecs_services security group.

  # Port 8080 - To Spring Boot services (Product, Cart, Payment)
  # egress {
  #   from_port       = 8080
  #   to_port         = 8080
  #   protocol        = "tcp"
  #   security_groups = [aws_security_group.ecs_services.id]
  #   description     = "ALB to Spring Boot tasks"
  # }

  # Port 5672 - To RabbitMQ AMQP task
  # egress {
  #   from_port       = 5672
  #   to_port         = 5672
  #   protocol        = "tcp"
  #   security_groups = [aws_security_group.ecs_services.id]
  #   description     = "NLB to RabbitMQ AMQP port"
  # }

  # Port 15672 - To RabbitMQ Management task
  # egress {
  #   from_port       = 15672
  #   to_port         = 15672
  #   protocol        = "tcp"
  #   security_groups = [aws_security_group.ecs_services.id]
  #   description     = "NLB to RabbitMQ Management port"
  # }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow all outbound traffic"
  }

  tags = {
    Name = "ecommerce-lb-sg"
  }
}

# --------------------------------------------------------------------------
# Security Group for ECS Services (Fargate Tasks)
# - Attached to: All aws_ecs_service resources.
# - Role: Receives traffic ONLY from the load balancers.
# --------------------------------------------------------------------------
resource "aws_security_group" "ecs_services" {
  name        = "ecommerce-ecs-services-sg"
  description = "Security group for all ECS services (Cart, Rabbit, etc)"
  vpc_id      = data.aws_vpc.default.id

  # --- Ingress (Inbound) ---
  # Allow traffic ONLY from the Load Balancer Security Group

  # Port 8080 - From ALB (for Spring Boot apps)
  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    # security_groups = [aws_security_group.lb.id] # ONLY allow from our LBs
    cidr_blocks     = ["0.0.0.0/0"] # Allow from anywhere (IPv4)
    description     = "Spring Boot tasks from ALB"
  }

  # Port 5672 - From NLB (for RabbitMQ AMQP)
  ingress {
    from_port       = 5672
    to_port         = 5672
    protocol        = "tcp"
    security_groups = [aws_security_group.lb.id] # ONLY allow from our LBs
    description     = "RabbitMQ AMQP from NLB"
  }

  # Port 15672 - From NLB (for RabbitMQ Management)
  ingress {
    from_port       = 15672
    to_port         = 15672
    protocol        = "tcp"
    security_groups = [aws_security_group.lb.id] # ONLY allow from our LBs
    description     = "RabbitMQ Management from NLB"
  }

  # --- Egress (Outbound) ---
  
  # Allow all outbound traffic. This is required for Fargate tasks to:
  # 1. Pull container images from ECR.
  # 2. Communicate with AWS services (e.g., CloudWatch Logs).
  # 3. Resolve and connect to the Load Balancer DNS names.
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow all outbound traffic"
  }

  tags = {
    Name = "ecommerce-ecs-services-sg"
  }
}

# Port 5672 - RabbitMQ AMQP (for services within the VPC to connect to NLB)
# resource "aws_vpc_security_group_ingress_rule" "allow_rabbitmq_amqp_from_ecs_to_nlb" {
#   security_group_id = aws_security_group.lb.id # Attaches rule to SG B

#   # Rule details
#   from_port   = 5672
#   to_port     = 5672
#   ip_protocol = "tcp"

#   # The source
#   referenced_security_group_id = aws_security_group.ecs_services.id # Allows traffic FROM SG A
# }