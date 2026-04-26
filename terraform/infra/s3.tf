resource "aws_s3_bucket" "snapshots" {
  bucket        = "fallboot-snapshots"
  force_destroy = true

  tags = { Name = "fallboot-snapshots" }
}

resource "aws_s3_bucket_public_access_block" "snapshots" {
  bucket                  = aws_s3_bucket.snapshots.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "snapshots" {
  bucket = aws_s3_bucket.snapshots.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_policy" "snapshots" {
  bucket = aws_s3_bucket.snapshots.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid       = "AllowCloudFrontRead"
      Effect    = "Allow"
      Principal = { Service = "cloudfront.amazonaws.com" }
      Action    = "s3:GetObject"
      Resource  = "${aws_s3_bucket.snapshots.arn}/*"
      Condition = {
        StringEquals = {
          "AWS:SourceArn" = aws_cloudfront_distribution.snapshots.arn
        }
      }
    }]
  })
}
