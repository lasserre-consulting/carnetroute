# -- Build stage --
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci
COPY . .
RUN npx ng build --configuration production

# -- Serve stage --
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/carnetroute/browser /usr/share/nginx/html
RUN chown -R nginx:nginx /usr/share/nginx/html && chmod -R 755 /usr/share/nginx/html
EXPOSE 80
USER nginx
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -qO- http://localhost:80/ || exit 1
