# ✅ ĐÃ SỬA VẤN ĐỀ LOGOUT - COOKIE KHÔNG BỊ XÓA

## 🔍 Vấn đề:
Khi đăng xuất, cookie `access_token` và `refresh_token` không bị xóa, khiến khi F5 (refresh) thì tự động đăng nhập lại.

## 🛠️ Các thay đổi đã thực hiện:

### 1. **Backend - AuthCookieManager.java**
**File:** `video-call-web-app/src/main/java/.../util/AuthCookieManager.java`

**Vấn đề cũ:**
- Chỉ xóa `access_token` với path `/`
- Chỉ xóa `refresh_token` với path `/api/v1/auth/refresh`
- Nếu cookie được tạo với path khác, sẽ không bị xóa

**Sửa:**
```java
public void clearCookies(HttpServletResponse response) {
    // 1. Xóa Access Token với path="/"
    Cookie accessCookie1 = new Cookie("access_token", "");
    accessCookie1.setPath("/");
    accessCookie1.setDomain(domain);
    accessCookie1.setSecure(secure);
    accessCookie1.setHttpOnly(httpOnly); 
    accessCookie1.setMaxAge(0);
    response.addCookie(accessCookie1);
    
    // 2. Xóa Refresh Token với path="/api/v1/auth/refresh"
    Cookie refreshCookie = new Cookie("refresh_token", "");
    refreshCookie.setPath("/api/v1/auth/refresh");
    refreshCookie.setDomain(domain);
    refreshCookie.setSecure(secure);
    refreshCookie.setHttpOnly(httpOnly);
    refreshCookie.setMaxAge(0);
    response.addCookie(refreshCookie);
    
    // 3. Xóa Refresh Token với path="/" (phòng trường hợp cũ)
    Cookie refreshCookie2 = new Cookie("refresh_token", "");
    refreshCookie2.setPath("/");
    refreshCookie2.setDomain(domain);
    refreshCookie2.setSecure(secure);
    refreshCookie2.setHttpOnly(httpOnly);
    refreshCookie2.setMaxAge(0);
    response.addCookie(refreshCookie2);
}
```

**Lý do:** Cookie phải được xóa với **CHÍNH XÁC** cùng path, domain, secure mà nó được tạo. Nếu không match, cookie không bị xóa.

### 2. **Backend - SecurityConfig.java**
**File:** `video-call-web-app/src/main/java/.../config/SecurityConfig.java`

**Vấn đề cũ:**
```java
.requestMatchers("/api/v1/auth/**", ...).permitAll()
```
Điều này cho phép tất cả `/api/v1/auth/*` (bao gồm cả logout), nhưng để rõ ràng hơn:

**Sửa:**
```java
.requestMatchers("/api/v1/auth/sign-in", "/api/v1/auth/logout", ...).permitAll()
```

### 3. **Frontend - api.ts**
**File:** `video-call-frontend/src/api.ts`

**Vấn đề cũ:**
```typescript
logout: async () => {
    await fetch(`${API_BASE}/auth/logout`, {
        method: 'POST',
    });
    return { success: true };
}
```

**Sửa:**
```typescript
logout: async () => {
    try {
        const response = await fetch(`${API_BASE}/auth/logout`, {
            method: 'POST',
            credentials: 'include', // Đảm bảo gửi cookies
        });
        return { success: response.ok };
    } catch (error) {
        console.error('Logout error:', error);
        return { success: false };
    }
}
```

## 📋 Cách test:

### Test 1: Đăng nhập và kiểm tra cookie
1. Mở DevTools (F12) → Application → Cookies
2. Đăng nhập vào https://dev.lkht.id.vn
3. Kiểm tra có 2 cookies:
   - `access_token` (path: `/`, domain: `dev.lkht.id.vn`)
   - `refresh_token` (path: `/api/v1/auth/refresh`, domain: `dev.lkht.id.vn`)

### Test 2: Đăng xuất và kiểm tra cookie bị xóa
1. Click nút "Đăng xuất"
2. Kiểm tra trong DevTools → Cookies
3. **Kết quả mong đợi:** 
   - Cookie `access_token` **PHẢI biến mất**
   - Cookie `refresh_token` **PHẢI biến mất**

### Test 3: F5 sau khi logout
1. Sau khi đăng xuất, nhấn F5 (refresh trang)
2. **Kết quả mong đợi:**
   - Không tự động đăng nhập lại
   - Hiển thị trang login/landing page

### Test 4: Kiểm tra log backend
```bash
docker logs operation-backend-1 -f | grep -i "logout\|cookie"
```

Khi đăng xuất, bạn sẽ thấy:
```
Cookie before logout: access_token = eyJhbGc...
Cookie before logout: refresh_token = eyJhbGc...
Logout successful, cookies cleared
```

## 🔍 Debug nếu vẫn gặp vấn đề:

### Vấn đề 1: Cookie vẫn còn sau logout
**Nguyên nhân có thể:**
- Domain của cookie không khớp
- Path của cookie không khớp
- Secure flag không khớp

**Kiểm tra:**
1. Xem cookie trong DevTools:
   ```
   Name: access_token
   Value: eyJ...
   Domain: dev.lkht.id.vn  <-- Phải khớp với application.properties
   Path: /                   <-- Phải khớp với clearCookies()
   ```

2. Kiểm tra `application.properties`:
   ```properties
   application.cookie.domain=dev.lkht.id.vn
   application.cookie.secure=false
   ```

3. Xóa cookie thủ công trong DevTools để test

### Vấn đề 2: Backend trả về 403 khi logout
**Nguyên nhân:** SecurityConfig chặn endpoint

**Sửa:** Đảm bảo trong SecurityConfig có:
```java
.requestMatchers("/api/v1/auth/logout").permitAll()
```

### Vấn đề 3: Frontend gặp CORS error
**Kiểm tra:** Nginx custom config có proxy `/api` đúng không
```bash
sudo cat /home/TaiKhau/video-call-web-app/operation/data/nginx/custom/server_proxy.conf
```

## 🚀 Rebuild và restart:

```bash
cd /home/TaiKhau/video-call-web-app/operation

# Build lại
docker compose build backend frontend

# Restart
docker compose up -d backend frontend

# Kiểm tra status
docker ps

# Xem log
docker logs operation-backend-1 -f
```

## 📊 Kiểm tra cookie bằng curl:

```bash
# Login và lưu cookies
curl -c cookies.txt -X POST https://dev.lkht.id.vn/api/v1/auth/sign-in \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'

# Xem cookies
cat cookies.txt

# Logout (gửi cookies)
curl -b cookies.txt -X POST https://dev.lkht.id.vn/api/v1/auth/logout -v

# Kiểm tra response headers - phải thấy Set-Cookie với MaxAge=0
```

## ✅ Tổng kết:

**Đã sửa:**
1. ✅ Backend xóa cả `access_token` và `refresh_token` với đầy đủ paths
2. ✅ SecurityConfig cho phép endpoint `/api/v1/auth/logout`
3. ✅ Frontend gửi `credentials: 'include'` khi logout
4. ✅ Rebuild và restart backend + frontend

**Kết quả mong đợi:**
- Khi đăng xuất, cookies bị xóa hoàn toàn
- F5 sau logout KHÔNG tự động đăng nhập lại
- Phải đăng nhập lại thủ công

---

**Lưu ý quan trọng về Cookie:**
- Cookie chỉ bị xóa khi request xóa cookie có **CHÍNH XÁC**:
  - Cùng `name`
  - Cùng `path`
  - Cùng `domain`
  - Cùng `secure` flag
- Thiếu 1 trong các điều kiện trên → Cookie không bị xóa!
