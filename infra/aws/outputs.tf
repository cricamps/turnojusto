output "url_web" {
  description = "URL pública de la aplicación (CloudFront)"
  value       = "https://${aws_cloudfront_distribution.web.domain_name}"
}

output "url_api" {
  description = "URL pública del API Gateway"
  value       = aws_apigatewayv2_api.principal.api_endpoint
}

output "bucket_web" {
  value = aws_s3_bucket.web.bucket
}

output "cloudfront_id" {
  value = aws_cloudfront_distribution.web.id
}

output "ecr" {
  value = { for k, r in aws_ecr_repository.servicio : k => r.repository_url }
}

output "cluster" {
  value = aws_ecs_cluster.principal.name
}
