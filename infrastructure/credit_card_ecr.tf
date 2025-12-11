# ECR Repository for credit-card-service
resource "aws_ecr_repository" "credit_card" {
  name                 = "credit-card-service"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name = "credit-card-service"
  }
}

# Output the repository URL
output "credit_card_ecr_repository_url" {
  description = "ECR repository URL for credit-card-service"
  value       = aws_ecr_repository.credit_card.repository_url
}