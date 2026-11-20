variable "region" {
  description = "Región de AWS (AWS Academy Learner Lab trabaja en us-east-1)"
  type        = string
  default     = "us-east-1"
}

variable "ambiente" {
  description = "Nombre del ambiente"
  type        = string
  default     = "qa"
}

variable "rol_lab" {
  description = "Rol IAM disponible en AWS Academy para ejecutar tareas de ECS"
  type        = string
  default     = "LabRole"
}

variable "imagen_tag" {
  description = "Etiqueta de las imágenes en ECR (SHA del commit)"
  type        = string
  default     = "latest"
}

variable "desplegar_servicios" {
  description = "Falso en la primera ejecución, antes de que existan imágenes en ECR"
  type        = bool
  default     = true
}

variable "entra_inquilino" {
  description = "Id. del inquilino de Microsoft Entra"
  type        = string
}

variable "entra_secreto" {
  description = "Secreto de cliente de la app TurnoJusto (Microsoft Graph: invitaciones de usuarios)"
  type        = string
  sensitive   = true
}

variable "entra_emisor" {
  description = "Emisor (iss) de los tokens v2 de Entra: https://login.microsoftonline.com/<tenant-id>/v2.0"
  type        = string
}

variable "entra_audiencia" {
  description = "Audiencia (aud) de los tokens: Client ID de la aplicación TurnoJusto en Entra"
  type        = string
}

variable "db_url" {
  description = "URL JDBC TLS de Oracle Autonomous Database"
  type        = string
  sensitive   = true
}

variable "db_clave_org" {
  description = "Contraseña del usuario ORG_APP"
  type        = string
  sensitive   = true
}

variable "db_clave_emp" {
  description = "Contraseña del usuario EMP_APP"
  type        = string
  sensitive   = true
}
