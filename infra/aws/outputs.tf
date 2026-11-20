output "url_web" {
  description = "URL pública de la aplicación"
  value       = var.url_web_publica
}

output "url_api" {
  description = "URL pública del API Gateway"
  value       = aws_apigatewayv2_api.principal.api_endpoint
}

output "alb_interno" {
  description = "DNS del balanceador interno (solo accesible dentro de la VPC)"
  value       = aws_lb.interno.dns_name
}

output "ecr" {
  value = { for k, r in aws_ecr_repository.servicio : k => r.repository_url }
}

output "cluster" {
  value = aws_ecs_cluster.principal.name
}

output "url_web_publica" {
  description = "URL pública de revisión (cPanel)"
  value       = var.url_web_publica
}
