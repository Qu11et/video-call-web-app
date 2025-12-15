import type { BackendLoginResponse, UserProfile } from '../types/auth';

export const adaptUserProfile = (response: any): UserProfile => {
  // Logic ưu tiên để lấy dữ liệu từ các cấu trúc khác nhau
  const rawUser = response.user || response.data?.profile || response || {};

  return {
    id: rawUser.id || 'unknown',
    email: rawUser.email || '',
    fullName: rawUser.fullName || 'New User',
    role: rawUser.role || 'USER',
    avatarUrl: rawUser.avatar || undefined,
    // --- SỬA ĐỔI TẠI ĐÂY ---
    // Vì backend chưa trả về isActive, ta tạm thời mặc định là ACTIVE
    // Nếu sau này backend có field isActive, ta sẽ uncomment đoạn logic cũ
    status: 'ACTIVE' 
    // status: rawUser.isActive ? 'ACTIVE' : 'PENDING_SETUP'
  };
};