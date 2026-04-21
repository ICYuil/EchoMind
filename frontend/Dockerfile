# ============================================================
# 第一阶段：构建阶段 (Builder Stage)
# 使用 Node.js 镜像来构建 Vue 应用
# ============================================================
FROM node:20-alpine AS builder

# 设置工作目录
WORKDIR /app

# 复制 package.json 和 package-lock.json
# 这一步利用 Docker 缓存层，避免每次都重新下载依赖
COPY package*.json ./

# 安装依赖
RUN npm install --registry=https://registry.npmmirror.com

# 复制源代码
COPY . .

# 构建生产版本
RUN npm run build

# ============================================================
# 第二阶段：运行阶段 (Runtime Stage)
# 使用 Nginx 镜像来提供静态文件服务
# ============================================================
FROM nginx:alpine

# 从 builder 阶段复制构建产物到 nginx 目录
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 Nginx 配置文件（可选，用于自定义配置）
# COPY nginx.conf /etc/nginx/conf.d/default.conf

# 暴露 80 端口
EXPOSE 80

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]
