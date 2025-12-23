import React, { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import type { AppDispatch, RootState } from '../store/store';
import { loginAsync, clearError } from '../store/authSlice';

export default function LoginPage() {
  const dispatch = useDispatch<AppDispatch>();
  const navigate = useNavigate();
  
 // Vẫn lấy state loading để disable button
  const { isLoading, error } = useSelector((state: RootState) => state.auth);

  const [formData, setFormData] = useState({
    email: '',
    password: '',
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    if (error) dispatch(clearError());
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      // ✅ CÁCH 1: Dùng unwrap() - Best Practice cho logic luồng
      // Nếu login thất bại, nó sẽ ném lỗi xuống catch
      // Nếu thành công, nó trả về UserProfile ngay tại đây
      const user = await dispatch(loginAsync({
        email: formData.email,
        password: formData.password
      })).unwrap();

      // Tạm thời bỏ qua logic onboarding, luôn về trang chủ
      navigate('/');

      // // Xử lý logic dựa trên status user (Point 2 của bạn)
      // if (user.status === 'PENDING_SETUP') {
      //   navigate('/onboarding');
      // } else {
      //   navigate('/');
      // }

    } catch (errorMsg) {
      // ❌ Xử lý lỗi (Rejected case)
      // errorMsg chính là string từ rejectWithValue
      console.error("Login failed:", errorMsg);
      // Có thể show Toast error tại đây nếu không muốn dùng state.error global
    }
  };

  return (
    <div className="landing-container">
      <div className="landing-card" style={{ maxWidth: '400px' }}>
        <h1 style={{ fontSize: '2rem' }}>Đăng Nhập</h1>
        <p style={{ marginBottom: '2rem' }}>Chào mừng quay trở lại!</p>

        {error && <div style={{ color: '#ea4335', marginBottom: '1rem', background: 'rgba(234, 67, 53, 0.1)', padding: '10px', borderRadius: '4px' }}>{error}</div>}

        <form onSubmit={handleSubmit} style={{ width: '100%', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          
          <input
            type="email"
            name="email"
            placeholder="Email"
            value={formData.email}
            onChange={handleChange}
            autoFocus
          />

          <input
            type="password"
            name="password"
            placeholder="Mật khẩu"
            value={formData.password}
            onChange={handleChange}
          />

          <button 
            type="submit" 
            className="btn-primary" 
            disabled={isLoading}
            style={{ marginTop: '1rem' }}
          >
            {isLoading ? 'Đang xác thực...' : 'Đăng Nhập'}
          </button>
        </form>

        <div style={{ marginTop: '1.5rem', fontSize: '0.9rem' }}>
          Chưa có tài khoản? <span style={{ color: '#8ab4f8', cursor: 'pointer' }} onClick={() => navigate('/register')}>Đăng ký ngay</span>
        </div>

        <button 
            className="btn-secondary"
            style={{marginTop: '10px'}}
            onClick={() => navigate('/')}
        >
            Về trang chủ
        </button>
      </div>
    </div>
  );
}