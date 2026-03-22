resource "aws_ecr_repository" "backend" {
  name                 = "fallboot-backend"
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  tags = { Name = "fallboot-backend" }
}

resource "aws_ecr_repository" "kafka" {
  name                 = "fallboot-kafka"
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  tags = { Name = "fallboot-kafka" }
}

resource "aws_ecr_repository" "mock_jwks" {
  name                 = "fallboot-mock-jwks"
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  tags = { Name = "fallboot-mock-jwks" }
}
