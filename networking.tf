terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# Configure the AWS provider
provider "aws" {
  region = "us-east-1"
}

# Get default VPC (used by both ALB and NLB)
data "aws_vpc" "default" {
  default = true
}

# Get all subnets in default VPC (used by both ALB and NLB)
data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# Use existing LabRole from AWS Academy
data "aws_iam_role" "lab_role" {
  name = "LabRole"
}