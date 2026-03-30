resource "aws_elasticache_subnet_group" "main" {
  name       = "fallboot-redis-subnet"
  subnet_ids = aws_subnet.private[*].id

  tags = { Name = "fallboot-redis-subnet" }
}

resource "aws_elasticache_cluster" "redis" {
  cluster_id           = "fallboot-redis"
  engine               = "redis"
  node_type            = "cache.t3.micro"
  num_cache_nodes      = 1
  port                 = 6379
  subnet_group_name    = aws_elasticache_subnet_group.main.name
  security_group_ids   = [aws_security_group.services.id]

  tags = { Name = "fallboot-redis" }
}
