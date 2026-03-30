output "backend_ecr_url" {
  description = "ECR repository for backend images"
  value       = aws_ecr_repository.backend.repository_url
}

output "kafka_ecr_url" {
  description = "ECR repository for kafka consumer images"
  value       = aws_ecr_repository.kafka.repository_url
}

output "mock_jwks_ecr_url" {
  description = "ECR repository for mock JWKS server images"
  value       = aws_ecr_repository.mock_jwks.repository_url
}
