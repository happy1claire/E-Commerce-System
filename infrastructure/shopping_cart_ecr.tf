# ECR Repository for Shopping Cart Service
resource "aws_ecr_repository" "shopping_cart" {
  name                 = "shopping-cart-service"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name = "shopping-cart-service"
  }
}

# Output the repository URL
output "shopping_cart_ecr_repository_url" {
  description = "ECR repository URL for Shopping Cart"
  value       = aws_ecr_repository.shopping_cart.repository_url
}