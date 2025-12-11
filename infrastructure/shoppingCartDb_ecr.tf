# ECR Repository for shoppingcartdb
resource "aws_ecr_repository" "shoppingcartdb" {
  name                 = "shoppingcartdb"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name = "shoppingcartdb"
  }
}

# Output the repository URL
output "shoppingcartdb_ecr_repository_url" {
  description = "ECR repository URL for shoppingcartdb"
  value       = aws_ecr_repository.shoppingcartdb.repository_url
}