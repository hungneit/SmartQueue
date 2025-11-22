terraform {
  required_version = ">= 1.0"
  required_providers {
    alicloud = {
      source  = "aliyun/alicloud"
      version = "~> 1.200"
    }
  }
}

provider "alicloud" {
  region = var.aliyun_region
}

# Variables
variable "aliyun_region" {
  description = "Aliyun region"
  type        = string
  default     = "cn-hangzhou"
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

# Security Group for ECS
resource "alicloud_security_group" "backend_sg" {
  security_group_name = "${var.project_name}-backend-sg"
  description = "Allow SSH and HTTP"
  vpc_id      = alicloud_vpc.main.id
}

resource "alicloud_security_group_rule" "ssh" {
  type              = "ingress"
  ip_protocol       = "tcp"
  nic_type          = "intranet"
  policy            = "accept"
  port_range        = "22/22"
  priority          = 1
  security_group_id = alicloud_security_group.backend_sg.id
  cidr_ip           = "0.0.0.0/0"
}

resource "alicloud_security_group_rule" "http" {
  type              = "ingress"
  ip_protocol       = "tcp"
  nic_type          = "intranet"
  policy            = "accept"
  port_range        = "80/80"
  priority          = 1
  security_group_id = alicloud_security_group.backend_sg.id
  cidr_ip           = "0.0.0.0/0"
}

resource "alicloud_security_group_rule" "egress" {
  type              = "egress"
  ip_protocol       = "all"
  nic_type          = "intranet"
  policy            = "accept"
  port_range        = "1/65535"
  priority          = 1
  security_group_id = alicloud_security_group.backend_sg.id
  cidr_ip           = "0.0.0.0/0"
} 

# VPC and VSwitch for ECS
resource "alicloud_vpc" "main" {
  vpc_name   = "smartqueue-vpc"
  cidr_block = "172.16.0.0/16"
}

resource "alicloud_vswitch" "main" {
  vswitch_name = "smartqueue-vswitch"
  cidr_block   = "172.16.1.0/24"
  vpc_id       = alicloud_vpc.main.id
  zone_id      = "cn-hangzhou-b" # Update as needed
}

resource "alicloud_instance" "backend" {
  instance_name = "${var.project_name}-backend-${var.environment}"
  image_id      = "ubuntu_20_04_x64_20G_alibase_20230907.vhd" # Update as needed
  instance_type = "ecs.t5-lc2m1.nano"
  security_groups = [alicloud_security_group.backend_sg.id]
  vswitch_id      = alicloud_vswitch.main.id
  internet_charge_type = "PayByTraffic"
  internet_max_bandwidth_out = 10
  system_disk_category = "cloud_efficiency"
  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# TableStore (OTS) Instance
resource "alicloud_ots_instance" "eta_stats" {
  name = "${var.project_name}-ots-${var.environment}"
  description   = "SmartQueue ETA Statistics Storage"
  accessed_by   = "Any"
  instance_type = "HighPerformance"

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

resource "alicloud_ots_table" "eta_stats" {
  instance_name = alicloud_ots_instance.eta_stats.name
  table_name    = "eta_stats"
  time_to_live  = -1
  max_version   = 1

  primary_key {
    name = "queueId"
    type = "String"
  }

  primary_key {
    name = "timeWindow"
    type = "String"
  }
}

# Message Service (MNS) Queue
resource "alicloud_mns_queue" "notifications" {
  name                      = "${var.project_name}-notifications-${var.environment}"
  delay_seconds             = 0
  maximum_message_size      = 65536
  message_retention_period  = 345600
  visibility_timeout_seconds = 30
  receive_message_wait_time_seconds = 0
  polling_wait_seconds      = 0
}

# OSS Bucket for Frontend (Alternative to AWS S3)
resource "alicloud_oss_bucket" "frontend" {
  bucket = "${var.project_name}-frontend-${var.environment}-${random_string.bucket_suffix.result}"
  acl    = "public-read"

  website {
    index_document = "index.html"
    error_document = "index.html"
  }

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

resource "random_string" "bucket_suffix" {
  length  = 8
  special = false
  upper   = false
}

# CDN Domain for Frontend
resource "alicloud_cdn_domain_new" "frontend" {
  domain_name = "${var.project_name}-${var.environment}.example.com"
  cdn_type    = "web"
  scope       = "overseas"

  sources {
    content  = alicloud_oss_bucket.frontend.extranet_endpoint
    type     = "oss"
    priority = 20
    port     = 80
    weight   = 15
  }

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# Function Compute Service
resource "alicloud_fc_service" "eta_service" {
  name        = "${var.project_name}-eta-${var.environment}"
  description = "SmartQueue ETA and Notification Service"

  log_config {
    project  = alicloud_log_project.smartqueue.name
    logstore = alicloud_log_store.function_logs.name
  }

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# Log Service for monitoring
resource "alicloud_log_project" "smartqueue" {
  name        = "${var.project_name}-logs-${var.environment}"
  description = "SmartQueue Logging Project"
}

resource "alicloud_log_store" "function_logs" {
  project          = alicloud_log_project.smartqueue.name
  name             = "function-logs"
  retention_period = 7
  shard_count      = 1
}

# API Gateway
resource "alicloud_api_gateway_group" "eta_api" {
  name        = "${var.project_name}-eta-api-${var.environment}"
  description = "SmartQueue ETA API Gateway"
}

# Outputs
output "ots_instance_name" {
  value = alicloud_ots_instance.eta_stats.name
}

output "mns_queue_name" {
  value = alicloud_mns_queue.notifications.name
}

output "oss_bucket_name" {
  value = alicloud_oss_bucket.frontend.bucket
}

output "cdn_domain" {
  value = alicloud_cdn_domain_new.frontend.domain_name
}

output "fc_service_name" {
  value = alicloud_fc_service.eta_service.name
}

output "log_project_name" {
  value = alicloud_log_project.smartqueue.name
}
