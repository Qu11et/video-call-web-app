// Request gửi lên Backend khi đăng ký
export interface UserCreationRequest {
  email: string;
  password: string;
  fullName: string;
  // role?: 'RENTER' | 'LANDLORD'; // Tạm thời backend đang mặc định USER, mở rộng sau
}

// Request gửi lên Backend khi đăng nhập
export interface SignInRequest {
  email: string;
  password: string;
}

// Mở rộng UserProfile để handle trạng thái
export interface UserProfile {
  id: string; // Nên có ID
  email: string;
  fullName: string;
  role: string;
  avatarUrl?: string;
  // ✅ Flag để biết user đã active hay chưa, thay vì check null
  status: 'ACTIVE' | 'PENDING_SETUP' | 'BANNED'; 
}

// Interface cho Raw Response từ Backend (cấu trúc có thể thay đổi)
export interface BackendLoginResponse {
  token?: string; // JWT
  user?: {        // Cấu trúc cũ
    email: string;
    fullName: string;
    role: string;
  };
  data?: {        // Cấu trúc mới giả định
    profile?: any;
  };
}

// State của Auth Slice
export interface AuthState {
  user: UserProfile | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
}