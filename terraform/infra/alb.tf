resource "aws_lb" "main" {
  name               = "fallboot-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.public[*].id

  tags = { Name = "fallboot-alb" }
}

resource "aws_lb_target_group" "backend" {
  name        = "fallboot-backend-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    path                = "/public/test"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  # Sticky sessions, keep WebSocket user on the same backend instance
  stickiness {
    type            = "lb_cookie"
    cookie_duration = 86400 # 24 hours
    enabled         = true
  }

  tags = { Name = "fallboot-backend-tg" }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
  }
}

# Mock JWKS server target group
resource "aws_lb_target_group" "mock_jwks" {
  name        = "fallboot-mock-jwks-tg"
  port        = 9999
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    path                = "/.well-known/jwks.json"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = { Name = "fallboot-mock-jwks-tg" }
}

# Mock JWKS listener on port 9999
resource "aws_lb_listener" "mock_jwks" {
  load_balancer_arn = aws_lb.main.arn
  port              = 9999
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.mock_jwks.arn
  }
}
