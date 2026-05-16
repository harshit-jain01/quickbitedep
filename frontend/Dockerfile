FROM node:24-alpine AS build

ARG APP_DIR=frontend

WORKDIR /app
COPY ${APP_DIR}/package*.json ./
RUN npm ci
COPY ${APP_DIR}/ .

ARG VITE_API_BASE_URL=
ARG VITE_AUTH_API_BASE_URL=
ARG VITE_ADMIN_API_BASE_URL=
ARG VITE_OAUTH_BASE_URL=
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_AUTH_API_BASE_URL=$VITE_AUTH_API_BASE_URL
ENV VITE_ADMIN_API_BASE_URL=$VITE_ADMIN_API_BASE_URL
ENV VITE_OAUTH_BASE_URL=$VITE_OAUTH_BASE_URL

RUN npm run build

FROM nginx:1.27-alpine

COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
