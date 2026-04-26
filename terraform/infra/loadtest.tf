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

resource "aws_instance" "loadtest" {
  ami                         = data.aws_ami.amazon_linux.id
  instance_type               = "c5.4xlarge"
  subnet_id                   = aws_subnet.public[0].id
  vpc_security_group_ids      = [aws_security_group.backend.id]
  associate_public_ip_address = true
  key_name                    = var.key_pair_name

  # NOTE: contents must be flush-left — cloud-init runs the script verbatim and a
  # leading-whitespace shebang gets ignored, dropping the script. Don't indent.
  user_data = <<EOF
#!/bin/bash
set -e
yum update -y
yum install -y java-21-amazon-corretto-devel git

# File-descriptor limits (per-process socket cap)
cat >> /etc/security/limits.conf <<'LIMITS'
* soft nofile 1048576
* hard nofile 1048576
root soft nofile 1048576
root hard nofile 1048576
LIMITS

# systemd default for service units (login sessions inherit /etc/security)
mkdir -p /etc/systemd/system.conf.d
cat > /etc/systemd/system.conf.d/limits.conf <<'SYSD'
[Manager]
DefaultLimitNOFILE=1048576
SYSD

# Kernel-level limits
cat > /etc/sysctl.d/99-loadtest.conf <<'SYSCTL'
fs.file-max = 2097152
fs.nr_open = 2097152
net.ipv4.ip_local_port_range = 1024 65535
net.ipv4.tcp_tw_reuse = 1
net.ipv4.tcp_fin_timeout = 15
net.core.somaxconn = 65535
net.core.netdev_max_backlog = 65535
net.ipv4.tcp_max_syn_backlog = 65535
net.core.rmem_max = 16777216
net.core.wmem_max = 16777216
net.ipv4.tcp_rmem = 4096 87380 16777216
net.ipv4.tcp_wmem = 4096 65536 16777216
SYSCTL
sysctl --system || true
EOF

  # The settings inside user_data are documentation only after first boot.
  # Don't replace the instance just because we tuned them — apply live via SSH.
  lifecycle {
    ignore_changes = [user_data, ami]
  }

  tags = { Name = "fallboot-loadtest" }
}
