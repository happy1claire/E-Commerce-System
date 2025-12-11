# --------------------------------------------------------------------------
# 3. Target Groups
# One for each of your microservices.
# --------------------------------------------------------------------------

resource "aws_lb_target_group" "product_tg" {
  name        = "product-service-tg"
  port        = 8080 # Your Spring Boot port
  protocol    = "HTTP"
  vpc_id      = data.aws_vpc.default.id
  target_type = "ip" # Required for ECS Fargate (awsvpc)

  health_check {
    enabled             = true
    path                = "/actuator/health"
    protocol            = "HTTP"
    port                = "8080"
    healthy_threshold   = 10
    unhealthy_threshold = 2
    timeout             = 5
    interval            = 20           # Check every 20 seconds
    matcher             = "200"        # Expected HTTP status code
  }
}

resource "aws_lb_target_group" "cart_tg" {
  name        = "shopping-cart-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = data.aws_vpc.default.id
  target_type = "ip"

  health_check {
    enabled             = true
    path                = "/actuator/health"
    protocol            = "HTTP"
    port                = "8080"
    healthy_threshold   = 2
    unhealthy_threshold = 5          # More tolerant
    timeout             = 5
    interval            = 60           # Check every 60 seconds
    matcher             = "200"        # Expected HTTP status code
  }
}

resource "aws_lb_target_group" "payment_tg" {
  name        = "credit-card-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = data.aws_vpc.default.id
  target_type = "ip"

  health_check {
    enabled             = true
    path                = "/actuator/health"
    protocol            = "HTTP"
    port                = "8080"
    healthy_threshold   = 2
    unhealthy_threshold = 5          # More tolerant
    timeout             = 5
    interval            = 60           # Check every 60 seconds
    matcher             = "200"        # Expected HTTP status code
  }
}

resource "aws_lb_target_group" "warehouse_tg" {
  name        = "warehouse-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = data.aws_vpc.default.id
  target_type = "ip"

  health_check {
    enabled             = true
    path                = "/actuator/health"
    protocol            = "HTTP"
    port                = "8080"
    healthy_threshold   = 2
    unhealthy_threshold = 5          # More tolerant
    timeout             = 5
    interval            = 60           # Check every 60 seconds
    matcher             = "200"        # Expected HTTP status code
  }
}

# --------------------------------------------------------------------------
# 4. The Application Load Balancer
# --------------------------------------------------------------------------

resource "aws_lb" "main" {
  name               = "ecommerce-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.lb.id]
  subnets            = data.aws_subnets.default.ids

  tags = {
    Name = "ecommerce-alb"
  }
}

# --------------------------------------------------------------------------
# 5. Listeners and Rules (HTTP ONLY)
# --------------------------------------------------------------------------

# --- Main Listener for HTTP (Port 80) ---
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  # Default action: If no rules match, return a 404 error
  default_action {
    type = "fixed-response"
    fixed_response {
      content_type = "application/json"
      message_body = "{\"error\": \"NOT_FOUND\", \"message\": \"No matching API path found\"}"
      status_code  = "404"
    }
  }
}

# --- Rule for Product Service ---
resource "aws_lb_listener_rule" "product_rule" {
  listener_arn = aws_lb_listener.http.arn # Attached to the HTTP listener
  priority     = 10

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.product_tg.arn
  }

  condition {
    path_pattern {
      values = ["/product*", "/products*"]
    }
  }
}

resource "aws_lb_listener_rule" "product_rule_http_header_only" {
  listener_arn = aws_lb_listener.http.arn # Attached to the HTTP listener
  priority     = 15

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.product_tg.arn
  }

  condition {
    http_header {
      http_header_name = "X-Service-Type"
      values           = ["product"]
    }
  }
}

# --- Rule for Shopping Cart Service ---
resource "aws_lb_listener_rule" "cart_rule" {
  listener_arn = aws_lb_listener.http.arn # Attached to the HTTP listener
  priority     = 20

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.cart_tg.arn
  }

  condition {
    path_pattern {
      values = ["/shopping-cart*", "/shopping-carts*"]
    }
  }
}

resource "aws_lb_listener_rule" "cart_rule_http_header_only" {
  listener_arn = aws_lb_listener.http.arn # Attached to the HTTP listener
  priority     = 25

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.cart_tg.arn
  }

  condition {
    http_header {
      http_header_name = "X-Service-Type"
      values           = ["shopping-cart"]
    }
  }
}

# --- Rule for Credit Card Service ---
resource "aws_lb_listener_rule" "payment_rule" {
  listener_arn = aws_lb_listener.http.arn # Attached to the HTTP listener
  priority     = 30

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.payment_tg.arn
  }

  condition {
    path_pattern {
      values = ["/credit-card-authorizer*"]
    }
  }
}

resource "aws_lb_listener_rule" "payment_rule_http_header_only" {
  listener_arn = aws_lb_listener.http.arn # Attached to the HTTP listener
  priority     = 35

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.payment_tg.arn
  }

  condition {
    http_header {
      http_header_name = "X-Service-Type"
      values           = ["credit-card"]
    }
  }
}

# --- Rule for Warehouse Service ---
resource "aws_lb_listener_rule" "warehouse_rule" {
  listener_arn = aws_lb_listener.http.arn # Attached to the HTTP listener
  priority     = 40

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.warehouse_tg.arn
  }

  condition {
    path_pattern {
      values = ["/warehouse*"]
    }
  }
}

# --------------------------------------------------------------------------
# 6. Outputs
# --------------------------------------------------------------------------

output "alb_dns_name" {
  description = "The DNS name of the ALB (your API endpoint)"
  value       = aws_lb.main.dns_name
}