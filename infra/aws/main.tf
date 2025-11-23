

terraform {
  required_version = ">= 1.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
    local = {
      source  = "hashicorp/local"
      version = "~> 2.0"
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
    Name = "${var.project_name}-vpc"
  }
}

resource "aws_internet_gateway" "gw" {
  vpc_id = aws_vpc.main.id
  tags = {
    Name = "${var.project_name}-igw"
  }
}

resource "aws_route_table" "rt" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.gw.id
  }

  tags = {
    Name = "${var.project_name}-rt"
  }
}

resource "aws_route_table_association" "a" {
  subnet_id      = aws_subnet.main.id
  route_table_id = aws_route_table.rt.id
}
  # Security Group for EC2
  resource "aws_security_group" "backend_sg" {
    name        = "${var.project_name}-backend-sg"
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
      Name = "${var.project_name}-backend-sg"
    }
  }

resource "aws_subnet" "main" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.1.0/24"
  map_public_ip_on_launch = true
  availability_zone       = "${var.aws_region}a"
  tags = {
    Name = "${var.project_name}-subnet"
  }
}

data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*-x86_64-gp2"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# SSH Key Generation
resource "tls_private_key" "pk" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "aws_key_pair" "kp" {
  key_name   = "${var.project_name}-key-${var.environment}"
  public_key = tls_private_key.pk.public_key_openssh
}

resource "local_file" "ssh_key" {
  filename        = "${path.module}/generated_key.pem"
  content         = tls_private_key.pk.private_key_pem
  file_permission = "0400"
}

resource "aws_instance" "backend" {
  ami           = data.aws_ami.amazon_linux.id
  instance_type = "t3.micro" # t3.micro is often the alternative Free Tier option
  key_name      = aws_key_pair.kp.key_name
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

output "ssh_private_key_path" {
  description = "Path to the generated private key"
  value       = local_file.ssh_key.filename
}

output "ssh_connection_command" {
  description = "Command to connect to the instance"
  value       = "ssh -i ${local_file.ssh_key.filename} ec2-user@${aws_instance.backend.public_ip}"
}


