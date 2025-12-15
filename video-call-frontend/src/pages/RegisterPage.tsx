import React, { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import type { AppDispatch, RootState } from '../store/store';
import { registerAsync, clearError } from '../store/authSlice';

export default function RegisterPage() {
  const dispatch = useDispatch<AppDispatch>();
  const navigate = useNavigate();
  const { isLoading, error } = useSelector((state: RootState) => state.auth);

  const [formData, setFormData] = useState({
    email: '',
    fullName: '',
    password: '',
    confirmPassword: '',
  });

  const [validationError, setValidationError] = useState('');

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    // Clear lỗi khi user nhập lại
    if (validationError) setValidationError('');
    if (error) dispatch(clearError());
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // 1. Validate Form
    if (!formData.email || !formData.password || !formData.fullName) {
      setValidationError('Vui lòng điền đầy đủ thông tin');
      return;
    }
    if (formData.password.length < 6) {
      setValidationError('Mật khẩu phải có ít nhất 6 ký tự');
      return;
    }
    if (formData.password !== formData.confirmPassword) {
      setValidationError('Mật khẩu nhập lại không khớp');
      return;
    }

    // 2. Gọi Redux Action
    const resultAction = await dispatch(registerAsync({
      email: formData.email,
      fullName: formData.fullName,
      password: formData.password
    }));

    // 3. Xử lý kết quả
    if (registerAsync.fulfilled.match(resultAction)) {
      alert('Đăng ký thành công! Vui lòng đăng nhập.');
      navigate('/login'); // Chuyển sang trang login (sẽ tạo sau)
    }
  };

  return (
    <div className="landing-container">
      <div className="landing-card" style={{ maxWidth: '500px' }}>
        <h1 style={{fontSize: '2rem'}}>Đăng Ký</h1>
        <p style={{marginBottom: '2rem'}}>Tạo tài khoản mới</p>

        {validationError && <div style={{color: '#ea4335', marginBottom: '1rem'}}>{validationError}</div>}
        {error && <div style={{color: '#ea4335', marginBottom: '1rem'}}>{error}</div>}

        <form onSubmit={handleSubmit} style={{ width: '100%', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          
          <input
            type="text"
            name="fullName"
            placeholder="Họ và tên"
            value={formData.fullName}
            onChange={handleChange}
          />

          <input
            type="email"
            name="email"
            placeholder="Email"
            value={formData.email}
            onChange={handleChange}
          />

          <input
            type="password"
            name="password"
            placeholder="Mật khẩu"
            value={formData.password}
            onChange={handleChange}
          />

          <input
            type="password"
            name="confirmPassword"
            placeholder="Nhập lại mật khẩu"
            value={formData.confirmPassword}
            onChange={handleChange}
          />

          <button 
            type="submit" 
            className="btn-primary" 
            disabled={isLoading}
            style={{ marginTop: '1rem' }}
          >
            {isLoading ? 'Đang xử lý...' : 'Đăng Ký Ngay'}
          </button>
        </form>

        <div style={{ marginTop: '1.5rem', fontSize: '0.9rem' }}>
          Đã có tài khoản? <span style={{color: '#8ab4f8', cursor: 'pointer'}} onClick={() => navigate('/login')}>Đăng nhập</span>
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