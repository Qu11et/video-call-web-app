import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const navigate = useNavigate();
  const [status, setStatus] = useState<'verifying' | 'success' | 'error'>('verifying');

  useEffect(() => {
    if (!token) {
      setStatus('error');
      return;
    }

    // Gọi API Backend xác thực
    fetch(`/api/v1/users/verify?token=${token}`)
      .then(res => {
        if (res.ok) setStatus('success');
        else setStatus('error');
      })
      .catch(() => setStatus('error'));
  }, [token]);

  return (
    <div className="landing-container">
      <div className="landing-card">
        {status === 'verifying' && <h1>⏳ Đang xác thực...</h1>}
        
        {status === 'success' && (
          <>
            <h1 style={{color: '#4ade80'}}>✅ Xác thực thành công!</h1>
            <p>Tài khoản của bạn đã được kích hoạt.</p>
            <button className="btn-primary" onClick={() => navigate('/login')}>
              Đăng nhập ngay
            </button>
          </>
        )}

        {status === 'error' && (
          <>
            <h1 style={{color: '#ea4335'}}>❌ Xác thực thất bại</h1>
            <p>Token không hợp lệ hoặc đã hết hạn.</p>
            <button className="btn-secondary" onClick={() => navigate('/')}>
              Về trang chủ
            </button>
          </>
        )}
      </div>
    </div>
  );
}