# ECR Repository for RabbitMQ
resource "aws_ecr_repository" "rabbitmq" {
  name                 = "rabbitmq-management"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name = "rabbitmq-management"
  }
}

# Output the repository URL
output "rabbitmq_ecr_repository_url" {
  description = "ECR repository URL for RabbitMQ"
  value       = aws_ecr_repository.rabbitmq.repository_url
}