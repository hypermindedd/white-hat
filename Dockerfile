FROM eclipse-temurin:21-jdk AS kotlin-build
WORKDIR /source
COPY . .
RUN ./gradlew --no-daemon :server:api:installDist

FROM node:24-trixie-slim AS web-deps
WORKDIR /web
COPY apps/web-bff/package.json apps/web-bff/package-lock.json ./
RUN npm ci --omit=dev --ignore-scripts

FROM node:24-trixie-slim
RUN apt-get update && apt-get install -y --no-install-recommends \
      ca-certificates curl openjdk-21-jre-headless tini \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=kotlin-build /source/server/api/build/install/api ./api
COPY --from=web-deps /web/node_modules ./web/node_modules
COPY apps/web-bff/package.json apps/web-bff/server.mjs apps/web-bff/session-store.mjs ./web/
COPY apps/web-bff/public ./web/public
COPY apps/web-bff/db ./web/db
COPY deploy/railway/start.sh ./start.sh
USER node
EXPOSE 8080
ENTRYPOINT ["/usr/bin/tini", "--"]
CMD ["/bin/bash", "/app/start.sh"]
