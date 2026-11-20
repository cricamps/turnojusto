locals {
  # Atributos uniformes por microservicio (Terraform exige un mapa homogéneo para for_each).
  servicios = {
    "ms-organizacion" = { usuario = "ORG_APP", clave = aws_secretsmanager_secret.db_org.arn, puerto = 8081 }
    "ms-empleados"    = { usuario = "EMP_APP", clave = aws_secretsmanager_secret.db_emp.arn, puerto = 8082 }
  }
  # Cada microservicio tiene su propio listener en el ALB interno (8081 y 8082).
  url_interna = { for k, v in local.servicios : k => "http://${aws_lb.interno.dns_name}:${v.puerto}" }
  entorno_extra = {
    "ms-organizacion" = tomap({
      TJ_EMPLEADOS_URL = local.url_interna["ms-empleados"]
    })
    "ms-empleados" = tomap({
      TJ_ORGANIZACION_URL = local.url_interna["ms-organizacion"]
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

# ---- Balanceo y descubrimiento: ALB interno ----
# El Learner Lab bloquea AWS Cloud Map (servicediscovery:*) por política de la organización,
# así que el descubrimiento se resuelve con un Application Load Balancer interno: un listener
# y un grupo de destino por microservicio. API Gateway llega a él por el VPC Link.
resource "aws_lb" "interno" {
  name               = "tj-${var.ambiente}-interno"
  internal           = true
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = data.aws_subnets.publicas.ids
  idle_timeout       = 60
}

resource "aws_lb_target_group" "servicio" {
  for_each             = local.servicios
  name                 = "tj-${var.ambiente}-${replace(each.key, "ms-", "")}"
  port                 = 8080
  protocol             = "HTTP"
  target_type          = "ip"
  vpc_id               = data.aws_vpc.principal.id
  deregistration_delay = 15
  health_check {
    path                = "/actuator/health/liveness"
    matcher             = "200"
    interval            = 30
    healthy_threshold   = 2
    unhealthy_threshold = 5
  }
}

resource "aws_lb_listener" "servicio" {
  for_each          = local.servicios
  load_balancer_arn = aws_lb.interno.arn
  port              = each.value.puerto
  protocol          = "HTTP"
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.servicio[each.key].arn
  }
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
      { name = "TJ_CORS_ORIGENES", value = "${var.url_web_publica},http://localhost:5173" },
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

  load_balancer {
    target_group_arn = aws_lb_target_group.servicio[each.key].arn
    container_name   = each.key
    container_port   = 8080
  }
  health_check_grace_period_seconds = 240

  depends_on = [aws_lb_listener.servicio]

  wait_for_steady_state = false
}
