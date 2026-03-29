output "nlb_url" {
  description = "Backend URL"
  value       = "http://${aws_lb.main.dns_name}"
}

output "rds_endpoint" {
  description = "RDS PostgreSQL endpoint"
  value       = aws_db_instance.postgres.endpoint
}

output "mock_jwks_url" {
  description = "Mock JWKS server URL (for load test -DmockJwksUrl)"
  value       = "http://${aws_lb.main.dns_name}:443"
}

output "loadtest_ip" {
  description = "Load test EC2 public IP (SSH with key pair)"
  value       = aws_instance.loadtest.public_ip
}
