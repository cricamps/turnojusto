#!/bin/bash
# TurnoJusto · Despliegue continuo de la SPA en cPanel (turnojusto.xkcx.cl)
# Cron cada 5 minutos: descarga la rama "web" (la publica el workflow Despliegue QA)
# y, si cambió, la copia en el directorio del subdominio.
set -euo pipefail
DESTINO="$HOME/public_html/turnojusto.xkcx.cl"
TMP="$HOME/tmp/turnojusto-web"
mkdir -p "$HOME/tmp"
curl -fsSL -o "$TMP.zip" "https://github.com/cricamps/turnojusto/archive/refs/heads/web.zip"
SUMA=$(md5sum "$TMP.zip" | cut -d' ' -f1)
[ "$SUMA" = "$(cat "$HOME/.turnojusto_web_md5" 2>/dev/null || true)" ] && exit 0
rm -rf "$TMP" && mkdir -p "$TMP"
unzip -q -o "$TMP.zip" -d "$TMP"
cp -a "$TMP"/turnojusto-web/. "$DESTINO"/
echo "$SUMA" > "$HOME/.turnojusto_web_md5"
echo "$(date '+%F %T') SPA actualizada ($SUMA)" >> "$HOME/logs/turnojusto_web.log" 2>/dev/null || true
