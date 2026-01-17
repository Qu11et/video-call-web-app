import type { UserCreationRequest, SignInRequest, UserProfile } from './types/auth'; // Nhớ update types/auth.ts nếu thiếu UserProfile

// Interface cho Meeting (Mapping với Model MongoDB)
export interface ParticipantInfo {
  identity: string;
  name: string;
  joinedAt: string;
  leftAt: string;
}

export interface Meeting {
  id: string;
  roomId: string;
  startTime: string;
  endTime: string;
  durationSeconds: number;
  participants: ParticipantInfo[];
}

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number; // Current page
}

// --- USER PROFILE API ---

export interface MeetingHistory {
  roomId: string;
  joinedAt: string;       // ISO Date String
  durationSeconds: number; // Thời lượng cuộc họp
}

export interface UserProfileResponse {
  email: string;
  fullName: string;
  role: string;
  //history: MeetingHistory[];
}

// 2. Dùng lại PageResponse đã định nghĩa ở phần Admin
// (Nếu chưa có thì định nghĩa lại ở đây)
export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
}

export const userApi = {
  // GET /api/v1/users/profile
  getProfile: async (): Promise<UserProfileResponse | null> => {
    try {
      const response = await fetch(`${API_BASE}/users/profile`, {
        method: 'GET',
        // Cookie HttpOnly tự động được gửi đi để xác thực
      });

      if (response.ok) {
        return await response.json();
      }
      return null; // Trả về null nếu lỗi (401, 403, 500)
    } catch (error) {
      console.error("Lỗi lấy profile:", error);
      return null;
    }
  },

  // HÀM MỚI: Lấy lịch sử phân trang
  getHistory: async (page: number = 0, size: number = 5): Promise<PageResponse<MeetingHistory> | null> => {
    try {
      const response = await fetch(`${API_BASE}/users/history?page=${page}&size=${size}`, {
        method: 'GET',
      });
      if (response.ok) {
        return await response.json();
      }
      return null;
    } catch (error) {
      console.error("Lỗi lấy lịch sử:", error);
      return null;
    }
  }
};

export const adminApi = {
  // GET /api/v1/admin/meetings?page=0&size=10
  getMeetings: async (page: number = 0, size: number = 10): Promise<PageResponse<Meeting> | null> => {
    try {
      const response = await fetch(`${API_BASE}/admin/meetings?page=${page}&size=${size}`, {
        method: 'GET',
        // Cookie sẽ tự động được gửi đi để xác thực ADMIN
      });

      if (response.ok) {
        return await response.json();
      }
      return null;
    } catch (error) {
      console.error("Lỗi lấy lịch sử cuộc họp:", error);
      return null;
    }
  }
};

const API_BASE_URL = "/api/rooms";

export const createRoomApi = async (type: 'P2P' | 'GROUP' = 'P2P'): Promise<string | null> => {
  try {
    const response = await fetch(`${API_BASE_URL}/create`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ type }) // Gửi type lên server
    });
    
    if (response.ok) {
      const data = await response.json();
      return data.roomId;
    }
    return null;
  } catch (error) {
    console.error("Lỗi khi tạo phòng:", error);
    return null;
  }
};

export const checkRoomExistsApi = async (roomId: string): Promise<{ exists: boolean, type?: string }> => {
  try {
    const response = await fetch(`${API_BASE_URL}/${roomId}/join`, {
      method: "POST",
    });
    
    if (response.ok) {
      const data = await response.json();
      return { exists: true, type: data.type }; // Lấy type từ server trả về
    }
    return { exists: false };
  } catch (error) {
    console.error("Lỗi khi kiểm tra phòng:", error);
    return { exists: false };
  }
};

const API_BASE = "/api/v1";

export const authApi = {
  // Đăng ký tài khoản
  register: async (data: UserCreationRequest): Promise<{ success: boolean; message?: string }> => {
    try {
      const response = await fetch(`${API_BASE}/users/registration`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(data),
      });

      if (response.ok) {
        return { success: true };
      } else {
        const errorData = await response.text(); // Hoặc json() tùy backend trả về lỗi gì
        return { success: false, message: errorData || 'Đăng ký thất bại' };
      }
    } catch (error) {
      return { success: false, message: 'Lỗi kết nối server' };
    }
  },

  // Đăng nhập
  login: async (data: SignInRequest): Promise<{ success: boolean; user?: UserProfile; message?: string }> => {
    try {
      const response = await fetch(`${API_BASE}/auth/sign-in`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
        // KHÔNG CẦN credentials: 'include' ở đây vì cookie được set bởi response
      });

      if (response.ok) {
        const result = await response.json(); // Backend trả TokenExchangeResponse
        // Sau khi login thành công, gọi ngay API verify để lấy info đầy đủ
        return authApi.verifyToken(); 
      } else {
        // --- ĐOẠN NÀY QUAN TRỌNG: Đọc message lỗi từ GlobalExceptionHandler ---
        const errorData = await response.json().catch(() => ({ message: 'Lỗi không xác định' }));
        return { success: false, message: errorData.message || 'Đăng nhập thất bại' };
      }
    } catch (error) {
      return { success: false, message: 'Lỗi kết nối' };
    }
  },

  // Verify Token (Dùng để auto login khi F5)
  verifyToken: async (): Promise<{ success: boolean; user?: UserProfile; message?: string }> => {
    try {
      const response = await fetch(`${API_BASE}/auth/verify`, {
        method: 'GET',
        // QUAN TRỌNG: Gửi cookie đi kèm request
        // Vì Backend và Frontend cùng domain (nhờ Nginx), nên trình duyệt tự gửi.
        // Nhưng nếu test local khác port, có thể cần credentials: 'include'
      });

      if (response.ok) {
        const userData = await response.json();
        return { success: true, user: userData };
      }
      return { success: false };
    } catch (error) {
      return { success: false };
    }
  },

  // HÀM MỚI: Logout
  logout: async (): Promise<{ success: boolean }> => {
    try {
      const response = await fetch(`${API_BASE}/auth/logout`, {
        method: 'POST',
        credentials: 'include', // Đảm bảo gửi cookies
      });
      // Cookie sẽ được xóa bởi backend
      return { success: response.ok };
    } catch (error) {
      console.error('Logout error:', error);
      return { success: false };
    }
  }
};