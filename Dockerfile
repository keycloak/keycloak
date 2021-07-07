ARG DEFAULT_KEYCLOAK_ENDPOINT='http:\/\/localhost:8180'

FROM node:16 as builder

ARG DEFAULT_KEYCLOAK_ENDPOINT
ARG KEYCLOAK_ENDPOINT

WORKDIR /app
COPY . .

# replace Keycloak endpoints
RUN sed -i "s/${DEFAULT_KEYCLOAK_ENDPOINT}/${KEYCLOAK_ENDPOINT}/g" import.js &&\
    sed -i "s/${DEFAULT_KEYCLOAK_ENDPOINT}/${KEYCLOAK_ENDPOINT}/g" src/context/auth/keycloak.ts &&\
    sed -i "s/adminv2//g" snowpack.config.js

# install dependencies and build application
RUN npm ci
RUN npm run build

FROM nginx:stable
ARG DEFAULT_KEYCLOAK_ENDPOINT
ARG KEYCLOAK_ENDPOINT

COPY --from=builder /app/build /usr/share/nginx/html

# setup reverse proxy for Keycloak endpoint
COPY nginx-default.conf /etc/nginx/conf.d/default.conf

# replace Keycloak endpoints
RUN sed -i "s/${DEFAULT_KEYCLOAK_ENDPOINT}/${KEYCLOAK_ENDPOINT}/g" /etc/nginx/conf.d/default.conf

EXPOSE 80
