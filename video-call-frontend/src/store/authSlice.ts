import { createSlice, createAsyncThunk} from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';
import { authApi } from '../api';
import type { UserCreationRequest, SignInRequest, AuthState, UserProfile } from '../types/auth';
import { adaptUserProfile } from '../utils/authAdapter';

const initialState: AuthState = {
  user: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,
};

// Async Thunk cho Đăng ký
export const registerAsync = createAsyncThunk(
  'auth/register',
  async (data: UserCreationRequest, { rejectWithValue }) => {
    const response = await authApi.register(data);
    if (response.success) {
      return response;
    } else {
      return rejectWithValue(response.message);
    }
  }
);

// --- THÊM ACTION LOGIN ---
export const loginAsync = createAsyncThunk<
  UserProfile,      // Return type: Chắc chắn là UserProfile chuẩn
  SignInRequest,    // Input type
  { rejectValue: string }
>(
  'auth/login',
  async (data, { rejectWithValue }) => {
    // Gọi API (authApi trả về raw response hoặc { success: boolean, ... })
    const response = await authApi.login(data);

    if (response.success) {
      // ✅ Dùng Adapter để chuẩn hóa dữ liệu
      // Dù backend trả về user hay data.profile, adapter sẽ lo hết
      return adaptUserProfile(response); 
    } else {
      return rejectWithValue(response.message || 'Login failed');
    }
  }
);

// --- MỚI: ACTION VERIFY TOKEN (AUTO LOGIN) ---
export const verifyTokenAsync = createAsyncThunk<
  UserProfile, // Trả về User nếu token valid
  void,        // Không cần tham số
  { rejectValue: string }
>(
  'auth/verify',
  async (_, { rejectWithValue }) => {
    // Gọi API /auth/verify để check cookie
    const response = await authApi.verifyToken();
    if (response.success && response.user) {
      return response.user;
    } else {
      return rejectWithValue("Phiên đăng nhập hết hạn");
    }
  }
);

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    logout: (state) => {
      state.user = null;
      state.isAuthenticated = false;
      // TODO: Gọi API logout để xóa cookie backend
    },
    clearError: (state) => {
      state.error = null;
    }
  },
  extraReducers: (builder) => {
    builder
      // Xử lý Register
      .addCase(registerAsync.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(registerAsync.fulfilled, (state) => {
        state.isLoading = false;
        // Đăng ký thành công thì chưa login ngay, đợi user login
      })
      .addCase(registerAsync.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      })

      // --- XỬ LÝ TRẠNG THÁI LOGIN ---
      .addCase(loginAsync.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(loginAsync.fulfilled, (state, action) => {
        state.isLoading = false;
        state.isAuthenticated = true; // Đánh dấu đã đăng nhập
        state.user = action.payload ?? null; // Nếu undefined → null // Lưu thông tin user
      })
      .addCase(loginAsync.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      })
      
      // --- MỚI: XỬ LÝ VERIFY TOKEN ---
      .addCase(verifyTokenAsync.pending, (state) => {
        // Tùy chọn: Có thể set isLoading = true nếu muốn hiện màn hình chờ khi mới vào app
        state.isLoading = true; 
      })
      .addCase(verifyTokenAsync.fulfilled, (state, action) => {
        state.isAuthenticated = true; // Khôi phục trạng thái đăng nhập
        state.user = action.payload;  // Khôi phục thông tin user
        state.isLoading = false;
      })
      .addCase(verifyTokenAsync.rejected, (state) => {
        state.isAuthenticated = false; // Token lỗi/hết hạn -> Logout
        state.user = null;
        state.isLoading = false;
      });
  },
});

export const { logout, clearError } = authSlice.actions;
export default authSlice.reducer;

