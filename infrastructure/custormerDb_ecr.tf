# ECR Repository for customerdb
resource "aws_ecr_repository" "customerdb" {
  name                 = "customerdb"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name = "customerdb"
  }
}

# Output the repository URL
output "customerdb_ecr_repository_url" {
  description = "ECR repository URL for customerdb"
  value       = aws_ecr_repository.customerdb.repository_url
}