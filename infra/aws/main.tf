

terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# Variables
variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-southeast-1"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "dev"
}

variable "project_name" {
  description = "Project name"
  type        = string
  default     = "smartq"
}


# Subnet for EC2
resource "aws_vpc" "main" {
  cidr_block = "10.0.0.0/16"
  tags = {
    Name = "smartqueue-vpc"
  }
}
  # Security Group for EC2
  resource "aws_security_group" "backend_sg" {
    name        = "smartqueue-backend-sg"
    description = "Allow SSH and HTTP"
    vpc_id      = aws_vpc.main.id

    ingress {
      from_port   = 22
      to_port     = 22
      protocol    = "tcp"
      cidr_blocks = ["0.0.0.0/0"]
    }
    ingress {
      from_port   = 80
      to_port     = 80
      protocol    = "tcp"
      cidr_blocks = ["0.0.0.0/0"]
    }
    egress {
      from_port   = 0
      to_port     = 0
      protocol    = "-1"
      cidr_blocks = ["0.0.0.0/0"]
    }
    tags = {
      Name = "smartqueue-backend-sg"
    }
  }

resource "aws_subnet" "main" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.1.0/24"
  map_public_ip_on_launch = true
  availability_zone       = "${var.aws_region}a"
  tags = {
    Name = "smartqueue-subnet"
  }
}

resource "aws_instance" "backend" {
  ami           = "ami-0c55b159cbfafe1f0" # Amazon Linux 2 AMI (update as needed)
  instance_type = "t2.micro"
  subnet_id     = aws_subnet.main.id
    vpc_security_group_ids = [aws_security_group.backend_sg.id]
    tags = {
      Name        = "${var.project_name}-backend-ec2-${var.environment}"
      Environment = var.environment
      Project     = var.project_name
    }
}

# DynamoDB Tables
resource "aws_dynamodb_table" "tickets" {
  name           = "${var.project_name}-tickets-${var.environment}"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "ticketId"

  attribute {
    name = "ticketId"
    type = "S"
  }

  attribute {
    name = "queueId"
    type = "S"
  }

  global_secondary_index {
    name            = "queueId-index"
    hash_key        = "queueId"
    projection_type = "ALL"
  }

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

resource "aws_dynamodb_table" "queues" {
  name           = "${var.project_name}-queues-${var.environment}"
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "queueId"

  attribute {
    name = "queueId"
    type = "S"
  }

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}


# Outputs
output "dynamodb_tickets_table_name" {
  value = aws_dynamodb_table.tickets.name
}

output "dynamodb_queues_table_name" {
  value = aws_dynamodb_table.queues.name
}


