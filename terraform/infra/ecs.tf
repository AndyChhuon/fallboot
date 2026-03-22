# Look up ECR repos from persistent stack
data "aws_ecr_repository" "backend" {
  name = "fallboot-backend"
}

data "aws_ecr_repository" "kafka" {
  name = "fallboot-kafka"
}

data "aws_ecr_repository" "mock_jwks" {
  name = "fallboot-mock-jwks"
}

resource "aws_ecs_cluster" "main" {
  name = "fallboot-cluster"

  tags = { Name = "fallboot-cluster" }
}

# Let ECS tasks pull images from ECR and write logs
resource "aws_iam_role" "ecs_task_execution" {
  name = "fallboot-ecs-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution" {
  role       = aws_iam_role.ecs_task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# CloudWatch log groups for container logs
resource "aws_cloudwatch_log_group" "backend" {
  name              = "/ecs/fallboot-backend"
  retention_in_days = 7

  tags = { Name = "fallboot-backend-logs" }
}

resource "aws_cloudwatch_log_group" "kafka" {
  name              = "/ecs/fallboot-kafka"
  retention_in_days = 7

  tags = { Name = "fallboot-kafka-logs" }
}

resource "aws_ecs_task_definition" "backend" {
  family                   = "fallboot-backend"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "2048"  # 2 vCPU
  memory                   = "4096"  # 4 GB RAM
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn

  container_definitions = jsonencode([{
    name      = "fallboot-backend"
    image     = "${data.aws_ecr_repository.backend.repository_url}:latest"
    essential = true

    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]

    environment = [
      { name = "SPRING_DATASOURCE_URL",          value = "jdbc:postgresql://${aws_db_instance.postgres.endpoint}/fallboot" },
      { name = "SPRING_DATASOURCE_USERNAME",     value = "myUser" },
      { name = "SPRING_DATASOURCE_PASSWORD",     value = var.db_password },
      { name = "SPRING_DATA_REDIS_HOST",         value = aws_elasticache_cluster.redis.cache_nodes[0].address },
      { name = "SPRING_DATA_REDIS_PORT",         value = "6379" },
      { name = "SPRING_RABBITMQ_HOST",           value = aws_instance.rabbitmq.private_ip },
      { name = "SPRING_RABBITMQ_PORT",           value = "61613" },
      { name = "SPRING_RABBITMQ_USERNAME",       value = var.rabbitmq_username },
      { name = "SPRING_RABBITMQ_PASSWORD",       value = var.db_password },
      { name = "SPRING_KAFKA_BOOTSTRAP_SERVERS", value = aws_msk_cluster.kafka.bootstrap_brokers_tls },
      { name = "SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI", value = "http://mock-jwks.fallboot.local:9999" },
      { name = "SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI", value = "http://mock-jwks.fallboot.local:9999/.well-known/jwks.json" },
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.backend.name
        "awslogs-region"        = "us-east-2"
        "awslogs-stream-prefix" = "backend"
      }
    }
  }])

  tags = { Name = "fallboot-backend" }
}

resource "aws_ecs_task_definition" "kafka" {
  family                   = "fallboot-kafka"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"  # 0.5 vCPU
  memory                   = "1024" # 1 GB RAM
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn

  container_definitions = jsonencode([{
    name      = "fallboot-kafka"
    image     = "${data.aws_ecr_repository.kafka.repository_url}:latest"
    essential = true

    environment = [
      { name = "SPRING_DATASOURCE_URL",          value = "jdbc:postgresql://${aws_db_instance.postgres.endpoint}/fallboot" },
      { name = "SPRING_DATASOURCE_USERNAME",     value = "myUser" },
      { name = "SPRING_DATASOURCE_PASSWORD",     value = var.db_password },
      { name = "SPRING_KAFKA_BOOTSTRAP_SERVERS", value = aws_msk_cluster.kafka.bootstrap_brokers_tls },
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.kafka.name
        "awslogs-region"        = "us-east-2"
        "awslogs-stream-prefix" = "kafka"
      }
    }
  }])

  tags = { Name = "fallboot-kafka" }
}

# Backend ECS service
resource "aws_ecs_service" "backend" {
  name            = "fallboot-backend"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = var.fallboot_backend_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = aws_subnet.private[*].id
    security_groups = [aws_security_group.backend.id]
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.backend.arn
    container_name   = "fallboot-backend"
    container_port   = 8080
  }

  tags = { Name = "fallboot-backend" }
}

# Kafka consumer ECS service
resource "aws_ecs_service" "kafka" {
  name            = "fallboot-kafka"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.kafka.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = aws_subnet.private[*].id
    security_groups = [aws_security_group.backend.id]
  }

  tags = { Name = "fallboot-kafka" }
}

# Mock JWKS server log group
resource "aws_cloudwatch_log_group" "mock_jwks" {
  name              = "/ecs/fallboot-mock-jwks"
  retention_in_days = 7

  tags = { Name = "fallboot-mock-jwks-logs" }
}

# Mock JWKS server task definition
resource "aws_ecs_task_definition" "mock_jwks" {
  family                   = "fallboot-mock-jwks"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"   # 0.25 vCPU
  memory                   = "512"   # 0.5 GB RAM
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn

  container_definitions = jsonencode([{
    name      = "fallboot-mock-jwks"
    image     = "${data.aws_ecr_repository.mock_jwks.repository_url}:latest"
    essential = true

    portMappings = [{
      containerPort = 9999
      protocol      = "tcp"
    }]

    environment = [
      { name = "PORT",   value = "9999" },
      { name = "ISSUER", value = "http://mock-jwks.fallboot.local:9999" },
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.mock_jwks.name
        "awslogs-region"        = "us-east-2"
        "awslogs-stream-prefix" = "mock-jwks"
      }
    }
  }])

  tags = { Name = "fallboot-mock-jwks" }
}

# Mock JWKS ECS service with service discovery + ALB
resource "aws_ecs_service" "mock_jwks" {
  name            = "fallboot-mock-jwks"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.mock_jwks.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = aws_subnet.private[*].id
    security_groups = [aws_security_group.backend.id]
  }

  service_registries {
    registry_arn = aws_service_discovery_service.mock_jwks.arn
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.mock_jwks.arn
    container_name   = "fallboot-mock-jwks"
    container_port   = 9999
  }

  tags = { Name = "fallboot-mock-jwks" }
}
