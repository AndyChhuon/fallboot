resource "aws_iam_role" "kafka_task" {
  name = "fallboot-kafka-task-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy" "kafka_s3_put" {
  name = "fallboot-kafka-s3-put"
  role = aws_iam_role.kafka_task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "s3:PutObject",
        "s3:PutObjectAcl"
      ]
      Resource = "${aws_s3_bucket.snapshots.arn}/rooms/*"
    }]
  })
}
