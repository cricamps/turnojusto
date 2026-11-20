terraform {
  required_version = ">= 1.9"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
  # El estado se guarda en un bucket S3 de la cuenta (lo crea el pipeline si no existe).
  backend "s3" {}
}

provider "aws" {
  region = var.region
  default_tags {
    tags = {
      Proyecto = "TurnoJusto"
      Curso    = "TSY2201"
      Ambiente = var.ambiente
    }
  }
}
