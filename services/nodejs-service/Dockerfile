# --- Stage 1: Builder (Dùng để build code TS sang JS) ---
FROM node:22-alpine AS builder

# Cài đặt libc6-compat nếu dùng các thư viện native (như bcrypt, sharp, prisma)
# Nếu không dùng thì có thể bỏ dòng này cho nhẹ hơn nữa
RUN apk add --no-cache libc6-compat

WORKDIR /app

# Copy package để tận dụng Docker Cache
COPY package*.json ./

# Cài đặt tất cả dependencies (cả devDependencies để build)
RUN npm ci

# Copy toàn bộ source code
COPY . .

# Build TypeScript sang JavaScript (Output thường nằm trong thư mục dist)
RUN npm run build

# Xóa bớt các thư viện devDependencies không cần thiết cho production
RUN npm prune --production

# --- Stage 2: Runner (Chỉ chứa những gì cần thiết để chạy) ---
FROM node:22-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production

RUN apk add --no-cache dumb-init

RUN addgroup -g 1001 -S nodejs && adduser -S nodejs -u 1001

COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/package.json ./

USER nodejs

EXPOSE 3000

ENTRYPOINT ["/usr/bin/dumb-init", "--"]

CMD ["node", "dist/server.js"]