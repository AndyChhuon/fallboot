variable "fallboot_backend_count" {
  description = "Number of backend instances"
  type        = number
  default     = 4
}

variable "db_password" {
  description = "RDS password"
  type        = string
  sensitive   = true
}

variable "rabbitmq_username" {
  description = "RabbitMQ username"
  type        = string
  default     = "fallboot"
}
