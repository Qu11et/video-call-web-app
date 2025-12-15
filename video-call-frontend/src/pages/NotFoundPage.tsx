import { useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
// import '../styles/landing.css';

export default function NotFoundPage() {
  const navigate = useNavigate();
  const [countdown, setCountdown] = useState(5);

  // Auto redirect sau 5 giây
  useEffect(() => {
    if (countdown === 0) {
      navigate('/');
      return;
    }

    const timer = setTimeout(() => {
      setCountdown(countdown - 1);
    }, 1000);

    return () => clearTimeout(timer);
  }, [countdown, navigate]);

  return (
    <div className="landing-container">
      <div className="landing-card" style={{ maxWidth: '600px', textAlign: 'center' }}>
        {/* Emoji hoặc SVG illustration */}
        <div style={{ fontSize: '5rem', marginBottom: '1rem' }}>
          🔍
        </div>

        <h1 style={{ fontSize: '3rem', marginBottom: '0.5rem' }}>404</h1>
        <h2>Oops! Trang không tìm thấy</h2>
        
        <p style={{ marginTop: '1rem', color: '#666', lineHeight: '1.6' }}>
          Trang bạn đang tìm kiếm không tồn tại hoặc đã bị di chuyển.
        </p>

        {/* Countdown */}
        <p style={{ marginTop: '1rem', fontSize: '0.875rem', color: '#999' }}>
          Tự động chuyển về trang chủ sau <strong>{countdown}s</strong>
        </p>

        {/* Action buttons */}
        <div style={{ 
          display: 'flex', 
          gap: '1rem', 
          marginTop: '2rem', 
          justifyContent: 'center',
          flexWrap: 'wrap'
        }}>
          <button 
            className="btn-primary"
            onClick={() => navigate('/')}
          >
            🏠 Về trang chủ ngay
          </button>

          <button 
            className="btn-secondary"
            onClick={() => navigate(-1)}
          >
            ← Quay lại trang trước
          </button>
        </div>

        {/* Helpful links */}
        <div style={{ 
          marginTop: '2rem', 
          padding: '1rem', 
          background: '#f5f5f5', 
          borderRadius: '8px' 
        }}>
          <p style={{ fontSize: '0.875rem', marginBottom: '0.5rem', fontWeight: '500' }}>
            Có thể bạn đang tìm:
          </p>
          <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap' }}>
            <a href="/" style={{ color: '#007bff', textDecoration: 'none' }}>Trang chủ</a>
            <a href="/login" style={{ color: '#007bff', textDecoration: 'none' }}>Đăng nhập</a>
            <a href="/register" style={{ color: '#007bff', textDecoration: 'none' }}>Đăng ký</a>
          </div>
        </div>

        {/* Debug info */}
        {import.meta.env.DEV && (
          <p style={{ marginTop: '1.5rem', fontSize: '0.75rem', color: '#ccc' }}>
            <strong>Debug:</strong> {window.location.pathname}
          </p>
        )}
      </div>
    </div>
  );
}