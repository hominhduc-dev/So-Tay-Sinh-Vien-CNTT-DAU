# Web client Next.js — build: docker build ./web (docker compose làm sẵn).
FROM node:22-slim AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

FROM deps AS build
# next.config.ts chuyển tiếp /api/* tới địa chỉ này. Giá trị bị đóng băng LÚC BUILD
# (bản standalone giữ nguyên cấu hình), nên truyền bằng build arg, không phải biến lúc chạy.
ARG API_INTERNAL_URL=http://api:8080
ENV API_INTERNAL_URL=$API_INTERNAL_URL NEXT_TELEMETRY_DISABLED=1
COPY . .
RUN npm run build

FROM node:22-slim AS runtime
ENV NODE_ENV=production PORT=3000 HOSTNAME=0.0.0.0 NEXT_TELEMETRY_DISABLED=1
WORKDIR /app
# standalone không tự chép .next/static và public — thiếu là mất CSS, JS và ảnh.
COPY --from=build --chown=node:node /app/.next/standalone ./
COPY --from=build --chown=node:node /app/.next/static ./.next/static
COPY --from=build --chown=node:node /app/public ./public
USER node
EXPOSE 3000
CMD ["node", "server.js"]
