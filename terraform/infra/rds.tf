resource "aws_db_subnet_group" "main" {
  name       = "fallboot-db-subnet"
  subnet_ids = aws_subnet.public[*].id # Public subnets for direct psql access from local machine

  tags = { Name = "fallboot-db-subnet" }
}

resource "aws_db_instance" "postgres" {
  identifier             = "fallboot-db"
  engine                 = "postgres"
  engine_version         = "17"
  instance_class         = "db.t3.large"
  allocated_storage      = 20
  db_name                = "fallboot"
  username               = "myUser"
  password               = var.db_password
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.services.id]
  skip_final_snapshot    = true
  publicly_accessible    = true
  apply_immediately      = true

  tags = { Name = "fallboot-db" }
}
