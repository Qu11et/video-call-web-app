# Stage 1: Build
FROM node:18-alpine as build
WORKDIR /app

# 1. Chỉ copy file package trước để tận dụng cache của Docker
COPY package*.json ./

# 2. Cài đặt thư viện (QUAN TRỌNG: Thêm --legacy-peer-deps để tránh lỗi version)
# npm ci giúp cài chính xác phiên bản trong lock file
RUN npm install --legacy-peer-deps

# 3. Copy toàn bộ source code vào (Lúc này đã có .dockerignore chặn node_modules rác)
COPY . .

# 4. Build React App
# Lệnh này sẽ dùng các module vừa cài ở bước 2
RUN npm run build

# Stage 2: Serve bằng Nginx
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]