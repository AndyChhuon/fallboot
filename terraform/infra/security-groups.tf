# Application load balancer, accept traffic from public
resource "aws_security_group" "alb" {
  name   = "fallboot-alb-sg"
  vpc_id = aws_vpc.main.id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Mock JWKS server
  ingress {
    from_port   = 9999
    to_port     = 9999
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "fallboot-alb-sg" }
}

# Fallboot Backend, only accept traffic from the ALB
resource "aws_security_group" "backend" {
  name   = "fallboot-backend-sg"
  vpc_id = aws_vpc.main.id

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  # Mock JWKS server — from ALB (external access)
  ingress {
    from_port       = 9999
    to_port         = 9999
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  # Mock JWKS server — from backend itself (JWT validation)
  ingress {
    from_port   = 9999
    to_port     = 9999
    protocol    = "tcp"
    self        = true
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "fallboot-backend-sg" }
}

# Services layer, only accept traffic from backend
resource "aws_security_group" "services" {
  name   = "fallboot-services-sg"
  vpc_id = aws_vpc.main.id

  # PostgreSQL — from backend
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.backend.id]
  }

  # PostgreSQL — direct psql access from local machine (disable in production)
  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"] # TODO: restrict to your IP for better security
  }

  # Redis
  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.backend.id]
  }

  # RabbitMQ STOMP (61613 local, 61614 Amazon MQ TLS)
  ingress {
    from_port       = 61613
    to_port         = 61614
    protocol        = "tcp"
    security_groups = [aws_security_group.backend.id]
  }

  # RabbitMQ AMQP
  ingress {
    from_port       = 5671
    to_port         = 5671
    protocol        = "tcp"
    security_groups = [aws_security_group.backend.id]
  }

  # Kafka (plaintext + TLS)
  ingress {
    from_port       = 9092
    to_port         = 9094
    protocol        = "tcp"
    security_groups = [aws_security_group.backend.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "fallboot-services-sg" }
}
