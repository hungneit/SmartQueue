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

