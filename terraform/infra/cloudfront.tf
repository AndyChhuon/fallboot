resource "aws_cloudfront_origin_access_control" "snapshots" {
  name                              = "fallboot-snapshots-oac"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_cloudfront_cache_policy" "snapshots" {
  name        = "fallboot-snapshots-cache"
  default_ttl = 31536000
  max_ttl     = 31536000
  min_ttl     = 0

  parameters_in_cache_key_and_forwarded_to_origin {
    enable_accept_encoding_brotli = true
    enable_accept_encoding_gzip   = true

    cookies_config { cookie_behavior = "none" }
    headers_config { header_behavior = "none" }

    query_strings_config {
      query_string_behavior = "whitelist"
      query_strings { items = ["v"] }
    }
  }
}

resource "aws_cloudfront_response_headers_policy" "snapshots_cors" {
  name = "fallboot-snapshots-cors"

  cors_config {
    access_control_allow_credentials = false
    access_control_allow_headers { items = ["*"] }
    access_control_allow_methods { items = ["GET", "HEAD"] }
    access_control_allow_origins { items = ["*"] }
    access_control_max_age_sec       = 3600
    origin_override                  = true
  }
}

resource "aws_cloudfront_distribution" "snapshots" {
  enabled         = true
  is_ipv6_enabled = true
  comment         = "fallboot pixel snapshots"

  origin {
    domain_name              = aws_s3_bucket.snapshots.bucket_regional_domain_name
    origin_id                = "fallboot-snapshots-s3"
    origin_access_control_id = aws_cloudfront_origin_access_control.snapshots.id
  }

  default_cache_behavior {
    target_origin_id           = "fallboot-snapshots-s3"
    viewer_protocol_policy     = "redirect-to-https"
    allowed_methods            = ["GET", "HEAD"]
    cached_methods             = ["GET", "HEAD"]
    cache_policy_id            = aws_cloudfront_cache_policy.snapshots.id
    response_headers_policy_id = aws_cloudfront_response_headers_policy.snapshots_cors.id
    compress                   = true
  }

  restrictions {
    geo_restriction { restriction_type = "none" }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }

  price_class = "PriceClass_100"

  tags = { Name = "fallboot-snapshots" }
}
