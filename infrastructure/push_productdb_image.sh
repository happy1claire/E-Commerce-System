#!/bin/bash

# Get the ECR repository URL from Terraform output
ECR_URL=$(terraform output -raw productdb_ecr_repository_url)
ECR_BASE=$(echo $ECR_URL | cut -d'/' -f1)
AWS_REGION="us-east-1"

echo "ECR Repository URL: $ECR_URL"

# Authenticate Docker to ECR
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_BASE

# Build the Spring Boot application first with Maven
echo "Building Spring Boot application with Maven..."
cd ../productDB

# Build Docker image for linux/amd64
echo "Building Docker image for linux/amd64..."
docker build --platform linux/amd64 -t productdb .
cd ../

# Tag the image for ECR
echo "Tagging image..."
docker tag productdb $ECR_URL:latest

# Push to ECR
echo "Pushing to ECR..."
docker push $ECR_URL:latest

echo "Done! Image pushed to $ECR_URL:latest"