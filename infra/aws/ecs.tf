locals {
  # Atributos uniformes por microservicio (Terraform exige un mapa homogéneo para for_each).
  servicios = {
    "ms-organizacion" = { usuario = "ORG_APP", clave = aws_secretsmanager_secret.db_org.arn }
    "ms-empleados"    = { usuario = "EMP_APP", clave = aws_secretsmanager_secret.db_emp.arn }
  }
  dominio_interno = aws_service_discovery_private_dns_namespace.interno.name
  entorno_extra = {
    "ms-organizacion" = tomap({
      TJ_EMPLEADOS_URL = "http://ms-empleados.${local.dominio_interno}:8080"
    })
    "ms-empleados" = tomap({
      TJ_ORGANIZACION_URL = "http://ms-organizacion.${local.dominio_interno}:8080"
      TJ_ENTRA_INQUILINO  = var.entra_inquilino
      TJ_ENTRA_CLIENTE_ID = var.entra_audiencia
      TJ_URL_APLICACION   = var.url_web_publica
    })
  }
  secretos_extra = {
    "ms-organizacion" = []
    "ms-empleados"    = [{ name = "TJ_ENTRA_SECRETO", valueFrom = aws_secretsmanager_secret.entra.arn }]
  }
}

# ---- Registro de imágenes ----
resource "aws_ecr_repository" "servicio" {
  for_each             = toset(keys(local.servicios))
  name                 = "turnojusto/${each.key}"
  image_tag_mutability = "MUTABLE"
  force_delete         = true
  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "limpieza" {
  for_each   = aws_ecr_repository.servicio
  repository = each.value.name
  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Conservar las 10 imágenes más recientes"
      selection    = { tagStatus = "any", countType = "imageCountMoreThan", countNumber = 10 }
      action       = { type = "expire" }
    }]
  })
}

# ---- Secretos (contraseñas de Oracle) ----
resource "aws_secretsmanager_secret" "db_org" {
  name                    = "turnojusto/${var.ambiente}/db/org_app"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "db_org" {
  secret_id     = aws_secretsmanager_secret.db_org.id
  secret_string = var.db_clave_org
}

resource "aws_secretsmanager_secret" "db_emp" {
  name                    = "turnojusto/${var.ambiente}/db/emp_app"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "db_emp" {
  secret_id     = aws_secretsmanager_secret.db_emp.id
  secret_string = var.db_clave_emp
}

resource "aws_secretsmanager_secret" "entra" {
  name                    = "turnojusto/${var.ambiente}/entra/secreto"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "entra" {
  secret_id     = aws_secretsmanager_secret.entra.id
  secret_string = var.entra_secreto
}

resource "aws_secretsmanager_secret" "db_url" {
  name                    = "turnojusto/${var.ambiente}/db/url"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "db_url" {
  secret_id     = aws_secretsmanager_secret.db_url.id
  secret_string = var.db_url
}

# ---- Descubrimiento de servicios (AWS Cloud Map) ----
resource "aws_service_discovery_private_dns_namespace" "interno" {
  name        = "turnojusto.local"
  description = "Nombres internos de los microservicios"
  vpc         = data.aws_vpc.principal.id
}

resource "aws_service_discovery_service" "servicio" {
  for_each = local.servicios
  name     = each.key
  dns_config {
    namespace_id   = aws_service_discovery_private_dns_namespace.interno.id
    routing_policy = "MULTIVALUE"
    dns_records {
      type = "A"
      ttl  = 10
    }
    dns_records {
      type = "SRV"
      ttl  = 10
    }
  }
  health_check_custom_config {}
  force_destroy = true
}

# ---- Cluster y servicios Fargate ----
resource "aws_ecs_cluster" "principal" {
  name = "turnojusto-${var.ambiente}"
  setting {
    name  = "containerInsights"
    value = "disabled"
  }
}

resource "aws_cloudwatch_log_group" "servicio" {
  for_each          = local.servicios
  name              = "/turnojusto/${var.ambiente}/${each.key}"
  retention_in_days = 7
}

resource "aws_ecs_task_definition" "servicio" {
  for_each                 = local.servicios
  family                   = "tj-${var.ambiente}-${each.key}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = data.aws_iam_role.lab.arn
  task_role_arn            = data.aws_iam_role.lab.arn
  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }
  container_definitions = jsonencode([{
    name         = each.key
    image        = "${aws_ecr_repository.servicio[each.key].repository_url}:${var.imagen_tag}"
    essential    = true
    portMappings = [{ containerPort = 8080, protocol = "tcp", name = "http" }]
    environment = concat([
      { name = "TJ_DB_USUARIO", value = each.value.usuario },
      { name = "TJ_JWT_EMISOR", value = var.entra_emisor },
      { name = "TJ_JWT_AUDIENCIA", value = var.entra_audiencia },
      { name = "TJ_CORS_ORIGENES", value = "https://${aws_cloudfront_distribution.web.domain_name},${var.url_web_publica}" },
      { name = "JAVA_TOOL_OPTIONS", value = "-Duser.timezone=America/Santiago" },
    ], [for k, v in local.entorno_extra[each.key] : { name = k, value = v }])
    secrets = concat([
      { name = "TJ_DB_URL", valueFrom = aws_secretsmanager_secret.db_url.arn },
      { name = "TJ_DB_CLAVE", valueFrom = each.value.clave },
    ], local.secretos_extra[each.key])
    healthCheck = {
      command     = ["CMD-SHELL", "curl -fs http://localhost:8080/actuator/health/liveness || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 120
    }
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.servicio[each.key].name
        awslogs-region        = var.region
        awslogs-stream-prefix = "ecs"
      }
    }
  }])
}

resource "aws_ecs_service" "servicio" {
  for_each        = { for k, v in local.servicios : k => v if var.desplegar_servicios }
  name            = each.key
  cluster         = aws_ecs_cluster.principal.id
  task_definition = aws_ecs_task_definition.servicio[each.key].arn
  desired_count   = 1
  launch_type     = "FARGATE"

  deployment_minimum_healthy_percent = 0
  deployment_maximum_percent         = 200
  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  network_configuration {
    subnets          = data.aws_subnets.publicas.ids
    security_groups  = [aws_security_group.servicios.id]
    assign_public_ip = true # sin NAT Gateway: la IP pública permite salir a Oracle y a Entra
  }

  service_registries {
    registry_arn = aws_service_discovery_service.servicio[each.key].arn
    port         = 8080
  }

  wait_for_steady_state = false
}
