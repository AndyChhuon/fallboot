variable "fallboot_backend_count" {
  description = "Number of backend instances"
  type        = number
  default     = 12
}

variable "db_password" {
  description = "RDS password"
  type        = string
  sensitive   = true
}

variable "key_pair_name" {
  description = "EC2 key pair name for SSH access to load test instance"
  type        = string
  default     = "fallboot-loadtest"
}
