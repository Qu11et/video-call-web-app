# ✅ ĐÃ SỬA CÁC VẤN ĐỀ WEBSOCKET VÀ LIVEKIT

## 🔍 Các vấn đề đã được sửa:

### 1. **Spring Security chặn WebSocket endpoint** (403 Forbidden)
- **File:** `video-call-web-app/src/main/java/anhtaikhau/example/video_call_web_app/config/SecurityConfig.java`
- **Thay đổi:** Thêm `.requestMatchers("/ws/**").permitAll()` để cho phép truy cập public vào WebSocket endpoint

### 2. **Nginx Proxy Manager không proxy /ws và /api đến backend**
- **File mới:** `operation/data/nginx/custom/server_proxy.conf`
- **Nội dung:** Cấu hình Nginx proxy cho:
  - `/api` → `http://operation-backend-1:8080`
  - `/ws` → `http://operation-backend-1:8080` (với WebSocket headers)

### 3. **Cookie domain không khớp với domain mới**
- **File:** `video-call-web-app/src/main/resources/application.properties`
- **Thay đổi:** 
  - `application.cookie.domain` từ `lkht.id.vn` → `dev.lkht.id.vn`

### 4. **LiveKit URL không đúng**
- **File:** `operation/docker-compose.yml`
- **Thay đổi:** `LIVEKIT_URL` từ `wss://livekit.lkht.id.vn` → `wss://livekit.dev.lkht.id.vn`

## 🚀 Các service đã được restart:
- ✅ Backend container (rebuilt và restarted)
- ✅ Nginx Proxy Manager (restarted)

## 📝 Cách test:

### Test 1: Kiểm tra WebSocket endpoint
```bash
curl -I https://dev.lkht.id.vn/ws/info
```
**Kết quả mong đợi:** Status 200 hoặc 404 (không phải 403)

### Test 2: Kiểm tra API endpoint
```bash
curl https://dev.lkht.id.vn/api/rooms/health || curl https://dev.lkht.id.vn/api/v1/auth/check
```

### Test 3: Vào trang web và test P2P call
1. Mở browser: https://dev.lkht.id.vn
2. Đăng nhập
3. Vào phòng P2P
4. Kiểm tra console - không còn lỗi 403

### Test 4: Kiểm tra LiveKit connection
1. Tạo meeting SFU/Group call
2. Join vào phòng
3. Kiểm tra người dùng có thể join và ở lại phòng

## 📊 Kiểm tra logs:

### Backend logs:
```bash
docker logs operation-backend-1 -f
```

### Nginx logs:
```bash
sudo tail -f /home/TaiKhau/video-call-web-app/operation/data/logs/proxy-host-1_error.log
sudo tail -f /home/TaiKhau/video-call-web-app/operation/data/logs/proxy-host-1_access.log
```

### LiveKit logs:
```bash
docker logs operation-livekit-1 -f
```

## ⚠️ Nếu vẫn gặp vấn đề:

### Vấn đề 1: "operation-frontend-1 could not be resolved"
Lỗi này xuất hiện vì Nginx không thể tìm container. Đã được sửa bằng cách:
- Tạo custom nginx config để proxy đến đúng container name
- Restart npm để load config mới

### Vấn đề 2: WebSocket vẫn 403
Kiểm tra:
```bash
# Xem backend có nhận được request không
docker logs operation-backend-1 | grep "/ws"

# Test trực tiếp đến backend (bỏ qua nginx)
docker exec -it operation-npm-1 curl http://operation-backend-1:8080/ws/info
```

### Vấn đề 3: Cookie không hoạt động
- Xóa cookies cũ trong browser
- Đăng nhập lại
- Kiểm tra Application > Cookies trong DevTools

### Vấn đề 4: LiveKit không connect
Kiểm tra:
```bash
# Test LiveKit từ bên trong backend container
docker exec -it operation-backend-1 curl -v wss://livekit.dev.lkht.id.vn

# Xem biến môi trường
docker exec -it operation-backend-1 env | grep LIVEKIT
```

## 🔧 Rebuild và restart nếu cần:

```bash
cd /home/TaiKhau/video-call-web-app/operation

# Rebuild backend
docker compose build backend

# Restart tất cả services
docker compose restart

# Hoặc chỉ restart backend và npm
docker compose restart backend npm
```

## 📁 File cấu hình quan trọng:

1. `/home/TaiKhau/video-call-web-app/operation/data/nginx/custom/server_proxy.conf` - Nginx custom config
2. `/home/TaiKhau/video-call-web-app/video-call-web-app/src/main/java/anhtaikhau/example/video_call_web_app/config/SecurityConfig.java` - Spring Security
3. `/home/TaiKhau/video-call-web-app/video-call-web-app/src/main/resources/application.properties` - Application properties
4. `/home/TaiKhau/video-call-web-app/operation/docker-compose.yml` - Docker compose

## 🎯 Tóm tắt:
- ✅ WebSocket endpoint được cho phép trong Spring Security
- ✅ Nginx proxy `/ws` và `/api` đến backend
- ✅ Cookie domain được cập nhật
- ✅ LiveKit URL được cập nhật
- ✅ Backend và Nginx đã được restart

Hệ thống bây giờ nên hoạt động bình thường!
