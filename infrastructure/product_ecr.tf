# ECR Repository for product Service
resource "aws_ecr_repository" "product" {
  name                 = "product-service"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name = "product-service"
  }
}

# Output the repository URL
output "product_ecr_repository_url" {
  description = "ECR repository URL for product-service"
  value       = aws_ecr_repository.product.repository_url
}