# ECR Repository for Warehouse Service
resource "aws_ecr_repository" "warehouse" {
  name                 = "warehouse-service"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name = "warehouse-service"
  }
}

# Output the repository URL
output "warehouse_ecr_repository_url" {
  description = "ECR repository URL for warehouse"
  value       = aws_ecr_repository.warehouse.repository_url
}