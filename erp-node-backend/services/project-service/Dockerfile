FROM node:22-alpine

WORKDIR /app

COPY package*.json ./
COPY tsconfig.base.json ./
COPY packages ./packages
COPY services/project-service ./services/project-service

RUN npm install

WORKDIR /app/services/project-service

RUN npm run prisma:generate
RUN npm run build

CMD ["sh", "-c", "npx prisma db push --schema prisma/schema.prisma && node /app/dist/services/project-service/src/main.js"]
