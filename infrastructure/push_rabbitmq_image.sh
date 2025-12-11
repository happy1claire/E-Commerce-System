#!/bin/bash

# Get the ECR repository URL from Terraform output
ECR_URL=$(terraform output -raw rabbitmq_ecr_repository_url)
ECR_BASE=$(echo $ECR_URL | cut -d'/' -f1)
AWS_REGION="us-east-1"

echo "ECR Repository URL: $ECR_URL"

# Authenticate Docker to ECR
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_BASE

# Pull the official RabbitMQ image for linux/amd64
echo "Pulling rabbitmq:4-management for linux/amd64..."
docker pull --platform linux/amd64 rabbitmq:4-management@sha256:b0ac76644171313b1f702249c24d24b86cc5949a1843632efaf1eb21e8e3aba5
docker tag rabbitmq@sha256:b0ac76644171313b1f702249c24d24b86cc5949a1843632efaf1eb21e8e3aba5 rabbitmq:4-management

# Tag the image for ECR
echo "Tagging image..."
docker tag rabbitmq:4-management $ECR_URL:latest

# Push to ECR
echo "Pushing to ECR..."
docker push $ECR_URL:latest

echo "Done! Image pushed to $ECR_URL:latest"