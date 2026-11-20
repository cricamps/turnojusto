# Red: se usa la VPC por defecto del Learner Lab con sus subredes públicas.
data "aws_vpc" "principal" {
  default = true
}

# El VPC Link de API Gateway no está disponible en la zona use1-az3, por eso se
# filtran las subredes por identificador de zona (el nombre us-east-1x varía por cuenta).
data "aws_subnets" "publicas" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.principal.id]
  }
  filter {
    name   = "default-for-az"
    values = ["true"]
  }
  filter {
    name   = "availability-zone-id"
    values = var.zonas_permitidas
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

# Balanceador interno: recibe el tráfico del VPC Link y las llamadas entre microservicios.
resource "aws_security_group" "alb" {
  name        = "tj-${var.ambiente}-alb"
  description = "ALB interno de TurnoJusto"
  vpc_id      = data.aws_vpc.principal.id

  ingress {
    description     = "Desde API Gateway (VPC Link)"
    from_port       = 8081
    to_port         = 8082
    protocol        = "tcp"
    security_groups = [aws_security_group.vpc_link.id]
  }

  ingress {
    description = "Llamadas entre microservicios"
    from_port   = 8081
    to_port     = 8082
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.principal.cidr_block]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = [data.aws_vpc.principal.cidr_block]
  }
}

resource "aws_security_group" "servicios" {
  name        = "tj-${var.ambiente}-servicios"
  description = "Microservicios de TurnoJusto en Fargate"
  vpc_id      = data.aws_vpc.principal.id

  ingress {
    description     = "Trafico desde el ALB interno"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    description = "Salida a Oracle Cloud, Microsoft Entra y ECR"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}
