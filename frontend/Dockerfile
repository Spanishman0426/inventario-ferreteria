# Nombre: Dockerfile
# Descripción: Imagen del frontend en dos etapas: compila la aplicación con Vite (npm run build) y la
#              sirve con nginx, que además reenvía /api al backend. La usa docker-compose.e2e.yml.
# Fecha de creación: 2026-10-21
# Última actualización: 2026-10-21
# Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)

# ---------- Etapa 1: compilación ----------
FROM node:24-alpine AS compilacion
WORKDIR /fuente
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
# VITE_API_URL vacía: el navegador llama a /api en el mismo origen y nginx la reenvía
ENV VITE_API_URL=
RUN npm run build

# ---------- Etapa 2: servidor estático ----------
FROM nginx:1.29-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=compilacion /fuente/dist /usr/share/nginx/html
EXPOSE 80
