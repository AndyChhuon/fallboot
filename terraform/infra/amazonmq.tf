# Self-hosted RabbitMQ on EC2 (Amazon MQ doesn't support STOMP plugin)

data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

resource "aws_instance" "rabbitmq" {
  ami                    = data.aws_ami.amazon_linux.id
  instance_type          = "t3.large"
  subnet_id              = aws_subnet.private[0].id
  vpc_security_group_ids = [aws_security_group.services.id]

  user_data = <<-EOF
    #!/bin/bash
    yum update -y
    yum install -y docker
    systemctl enable docker
    systemctl start docker
    docker run -d --restart always \
      --name rabbitmq \
      -p 5672:5672 \
      -p 15672:15672 \
      -p 61613:61613 \
      -e RABBITMQ_DEFAULT_USER=${var.rabbitmq_username} \
      -e RABBITMQ_DEFAULT_PASS=${var.db_password} \
      rabbitmq:4-management \
      bash -c "rabbitmq-plugins enable --offline rabbitmq_stomp rabbitmq_management && rabbitmq-server"
  EOF

  tags = { Name = "fallboot-rabbitmq" }
}
