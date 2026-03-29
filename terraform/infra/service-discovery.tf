resource "aws_service_discovery_private_dns_namespace" "main" {
  name = "fallboot.local"
  vpc  = aws_vpc.main.id

  tags = { Name = "fallboot-namespace" }
}

resource "aws_service_discovery_service" "mock_jwks" {
  name = "mock-jwks"

  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.main.id

    dns_records {
      ttl  = 10
      type = "A"
    }

    routing_policy = "MULTIVALUE"
  }

  health_check_custom_config {
    failure_threshold = 1
  }

  tags = { Name = "fallboot-mock-jwks" }
}
