# ECR Repository for ProductDB
resource "aws_ecr_repository" "productdb" {
  name                 = "productdb"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name = "productdb"
  }
}

# Output the repository URL
output "productdb_ecr_repository_url" {
  description = "ECR repository URL for ProductDB"
  value       = aws_ecr_repository.productdb.repository_url
}