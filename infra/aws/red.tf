# Red: se usa la VPC por defecto del Learner Lab con sus subredes públicas.
data "aws_vpc" "principal" {
  default = true
}

data "aws_subnets" "publicas" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.principal.id]
  }
  filter {
    name   = "default-for-az"
    values = ["true"]
  }
}

data "aws_caller_identity" "actual" {}

data "aws_iam_role" "lab" {
  name = var.rol_lab
}

# El VPC Link del API Gateway es el único que puede llegar a los contenedores.
resource "aws_security_group" "vpc_link" {
  name        = "tj-${var.ambiente}-vpclink"
  description = "VPC Link de API Gateway hacia los microservicios"
  vpc_id      = data.aws_vpc.principal.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "servicios" {
  name        = "tj-${var.ambiente}-servicios"
  description = "Microservicios de TurnoJusto en Fargate"
  vpc_id      = data.aws_vpc.principal.id

  ingress {
    description     = "Trafico desde API Gateway (VPC Link)"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.vpc_link.id]
  }

  ingress {
    description = "Llamadas entre microservicios (Cloud Map)"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    self        = true
  }

  egress {
    description = "Salida a Oracle Cloud, Entra External ID y ECR"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}
