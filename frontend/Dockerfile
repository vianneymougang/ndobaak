FROM oven/bun:1 AS deps
WORKDIR /app
COPY package.json bun.lock ./
RUN bun install --frozen-lockfile

FROM node:22-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npx ng build --configuration production

FROM node:22-alpine AS runner
WORKDIR /app
RUN addgroup -S angular && adduser -S angular -G angular
COPY --from=builder --chown=angular:angular /app/dist/Ndobaak/ ./
USER angular
EXPOSE 4000
HEALTHCHECK --interval=30s --timeout=3s --retries=3 --start-period=10s \
  CMD wget -qO- http://localhost:4000/health || exit 1
CMD ["node", "server/server.mjs"]
