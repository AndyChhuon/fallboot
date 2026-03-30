resource "aws_lb" "main" {
  name                       = "fallboot-nlb"
  internal                   = false
  load_balancer_type         = "network"
  subnets                    = aws_subnet.public[*].id
  enable_cross_zone_load_balancing = true

  tags = { Name = "fallboot-nlb" }
}

resource "aws_lb_target_group" "backend" {
  name        = "fallboot-backend-tg"
  port        = 8080
  protocol    = "TCP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    protocol            = "TCP"
    interval            = 30
    healthy_threshold   = 2
    unhealthy_threshold = 2
  }

  stickiness {
    type    = "source_ip"
    enabled = true
  }

  tags = { Name = "fallboot-backend-tg" }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "TCP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
  }
}

resource "aws_lb_target_group" "mock_jwks" {
  name        = "fallboot-mock-jwks-tg"
  port        = 9999
  protocol    = "TCP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    protocol            = "TCP"
    interval            = 30
    healthy_threshold   = 2
    unhealthy_threshold = 2
  }

  tags = { Name = "fallboot-mock-jwks-tg" }
}

resource "aws_lb_listener" "mock_jwks" {
  load_balancer_arn = aws_lb.main.arn
  port              = 443
  protocol          = "TCP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.mock_jwks.arn
  }
}
