FROM node:24-alpine@sha256:d32cdf619f63fe0471182d08996dd516c6275bb5fd31ae06e55a570bd9e1ad43 AS build
WORKDIR /app
COPY package.json package-lock.json ./
COPY achat-front/package.json achat-front/package.json
RUN npm ci --workspace achat-front --include-workspace-root=false
COPY achat-front/ achat-front/
RUN npm run build --workspace achat-front

FROM nginx:stable-alpine@sha256:970364228b1e4f932abc22d321932816782cca8a20b85a77552a7cfe9acfb3b5
RUN sed -i 's@/var/run/nginx.pid@/tmp/nginx.pid@; s@/run/nginx.pid@/tmp/nginx.pid@; /^user /d' /etc/nginx/nginx.conf \
    && chown -R nginx:nginx /etc/nginx/conf.d /var/cache/nginx
COPY deploy/nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/achat-front/dist /usr/share/nginx/html
USER nginx
EXPOSE 8080
