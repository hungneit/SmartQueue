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

# Outputs
output "ots_instance_name" {
  value = alicloud_ots_instance.eta_stats.name
}

