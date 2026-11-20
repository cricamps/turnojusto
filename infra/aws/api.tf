# ---- Amazon API Gateway (HTTP API): único punto de entrada ----
resource "aws_apigatewayv2_api" "principal" {
  name          = "turnojusto-${var.ambiente}"
  protocol_type = "HTTP"
  description   = "Puerta de entrada de TurnoJusto: valida el JWT de Entra y enruta a cada microservicio"
  cors_configuration {
    allow_origins = [var.url_web_publica, "http://localhost:5173"]
    allow_methods = ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"]
    allow_headers = ["authorization", "content-type"]
    max_age       = 3600
  }
}

resource "aws_apigatewayv2_authorizer" "entra" {
  api_id           = aws_apigatewayv2_api.principal.id
  name             = "entra-id"
  authorizer_type  = "JWT"
  identity_sources = ["$request.header.Authorization"]
  jwt_configuration {
    issuer   = var.entra_emisor
    audience = [var.entra_audiencia]
  }
}

resource "aws_apigatewayv2_vpc_link" "principal" {
  name               = "turnojusto-${var.ambiente}"
  subnet_ids         = data.aws_subnets.publicas.ids
  security_group_ids = [aws_security_group.vpc_link.id]
}

resource "aws_apigatewayv2_integration" "servicio" {
  for_each           = local.servicios
  api_id             = aws_apigatewayv2_api.principal.id
  integration_type   = "HTTP_PROXY"
  integration_method = "ANY"
  integration_uri    = aws_lb_listener.servicio[each.key].arn
  connection_type    = "VPC_LINK"
  connection_id      = aws_apigatewayv2_vpc_link.principal.id
}

locals {
  # Ruta → microservicio. Las rutas de negocio exigen token; la salud y los contratos OpenAPI son públicos.
  rutas = {
    "ANY /api/v1/empresa"            = "ms-organizacion"
    "ANY /api/v1/regiones"           = "ms-organizacion"
    "ANY /api/v1/sucursales"         = "ms-organizacion"
    "ANY /api/v1/sucursales/{proxy+}" = "ms-organizacion"
    "ANY /api/v1/equipos"            = "ms-organizacion"
    "ANY /api/v1/equipos/{proxy+}"   = "ms-organizacion"
    "ANY /api/v1/empleados"          = "ms-empleados"
    "ANY /api/v1/empleados/{proxy+}" = "ms-empleados"
  }
  publicas = {
    "GET /organizacion/actuator/health" = { servicio = "ms-organizacion", ruta = "/actuator/health" }
    "GET /empleados/actuator/health"    = { servicio = "ms-empleados", ruta = "/actuator/health" }
    "GET /organizacion/v3/api-docs"     = { servicio = "ms-organizacion", ruta = "/v3/api-docs" }
    "GET /empleados/v3/api-docs"        = { servicio = "ms-empleados", ruta = "/v3/api-docs" }
  }
}

resource "aws_apigatewayv2_route" "protegida" {
  for_each           = local.rutas
  api_id             = aws_apigatewayv2_api.principal.id
  route_key          = each.key
  target             = "integrations/${aws_apigatewayv2_integration.servicio[each.value].id}"
  authorization_type = "JWT"
  authorizer_id      = aws_apigatewayv2_authorizer.entra.id
}

resource "aws_apigatewayv2_integration" "publica" {
  for_each           = local.publicas
  api_id             = aws_apigatewayv2_api.principal.id
  integration_type   = "HTTP_PROXY"
  integration_method = "GET"
  integration_uri    = aws_lb_listener.servicio[each.value.servicio].arn
  connection_type    = "VPC_LINK"
  connection_id      = aws_apigatewayv2_vpc_link.principal.id
  request_parameters = { "overwrite:path" = each.value.ruta }
}

resource "aws_apigatewayv2_route" "publica" {
  for_each  = local.publicas
  api_id    = aws_apigatewayv2_api.principal.id
  route_key = each.key
  target    = "integrations/${aws_apigatewayv2_integration.publica[each.key].id}"
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.principal.id
  name        = "$default"
  auto_deploy = true
  default_route_settings {
    throttling_burst_limit = 50
    throttling_rate_limit  = 25
  }
}
