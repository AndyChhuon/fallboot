resource "aws_msk_configuration" "kafka" {
  name              = "fallboot-kafka-config"
  kafka_versions    = ["3.7.x.kraft"]
  server_properties = <<-EOF
    auto.create.topics.enable=true
    num.partitions=50
  EOF
}

resource "aws_msk_cluster" "kafka" {
  cluster_name           = "fallboot-kafka"
  kafka_version          = "3.7.x.kraft"
  number_of_broker_nodes = 3

  configuration_info {
    arn      = aws_msk_configuration.kafka.arn
    revision = aws_msk_configuration.kafka.latest_revision
  }

  broker_node_group_info {
    instance_type   = "kafka.m5.large"
    client_subnets  = aws_subnet.private[*].id
    security_groups = [aws_security_group.services.id]

    storage_info {
      ebs_storage_info {
        volume_size = 20
      }
    }
  }

  tags = { Name = "fallboot-kafka" }
}
