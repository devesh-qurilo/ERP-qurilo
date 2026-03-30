FROM node:22-alpine AS base
WORKDIR /app

FROM base AS deps
COPY package.json tsconfig.base.json ./
COPY packages ./packages
COPY services/client-service ./services/client-service
RUN npm install

FROM deps AS build
RUN npm run build -w @erp/shared-config
RUN npm run build -w @erp/shared-auth
RUN npm run build -w @erp/shared-logger
RUN npm run build -w @erp/shared-storage
RUN npm run build -w @erp/client-service

FROM node:22-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
COPY --from=deps /app/package.json ./package.json
COPY --from=deps /app/node_modules ./node_modules
COPY --from=build /app/packages ./packages
COPY --from=build /app/services/client-service ./services/client-service
COPY --from=build /app/dist ./dist
EXPOSE 8084
CMD ["sh", "-c", "npm run prisma:push -w @erp/client-service && node dist/services/client-service/src/main.js"]
