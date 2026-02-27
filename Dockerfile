# ── Stage 1: Compile Scala (backend JAR + frontend JS) ───────────────────────
FROM sbtscala/scala-sbt:eclipse-temurin-21.0.8_9_1.12.4_3.8.2 AS sbt-build
WORKDIR /app
# Copy sbt config first for layer caching
COPY project/ project/
COPY build.sbt .
# Warm-up the dependency cache (best-effort)
RUN sbt update || true
# Copy all sources and build
COPY modules/ modules/
RUN sbt "backend/stage" "frontend/fullLinkJS"

# ── Stage 2: Compile Tailwind CSS ────────────────────────────────────────────
# Tailwind v4 CLI requires both tailwindcss and @tailwindcss/cli to be installed
FROM node:22-alpine AS css-build
WORKDIR /app
RUN npm install tailwindcss @tailwindcss/cli
COPY frontend-assets/css/input.css ./input.css
# Copy compiled JS so Tailwind can scan it for class usage
COPY --from=sbt-build \
     /app/modules/frontend/target/scala-3.8.2/frontend-opt/main.js \
     ./main.js
RUN npx @tailwindcss/cli -i input.css -o styles.css --minify

# ── Stage 3: Runtime image ────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
# sbt-native-packager's launch script requires bash (not present by default on Alpine)
RUN apk add --no-cache bash
WORKDIR /app

# Backend executable from sbt-native-packager
COPY --from=sbt-build /app/modules/backend/target/universal/stage ./

# Static assets
RUN mkdir -p static/assets

COPY --from=css-build /app/styles.css                   static/assets/styles.css
COPY --from=sbt-build \
     /app/modules/frontend/target/scala-3.8.2/frontend-opt/main.js \
                                                          static/assets/main.js
COPY frontend-assets/index.html                          static/index.html

# Persistent H2 data volume
VOLUME ["/data"]

EXPOSE 8080

ENV ZBOOKS_STATIC_DIR=/app/static
ENV ZBOOKS_DB_PATH=/data/zbooks

CMD ["bin/backend"]
