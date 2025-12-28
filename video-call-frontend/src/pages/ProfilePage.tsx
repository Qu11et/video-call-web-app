import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { userApi } from '../api';
import type { UserProfileResponse } from '../api';

export default function ProfilePage() {
  const navigate = useNavigate();
  const [profile, setProfile] = useState<UserProfileResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchProfile = async () => {
      setIsLoading(true);
      const data = await userApi.getProfile();
      if (data) {
        setProfile(data);
      } else {
        // Nếu không lấy được profile (do chưa login hoặc hết hạn token)
        // Chuyển hướng về trang login
        navigate('/login');
      }
      setIsLoading(false);
    };

    fetchProfile();
  }, [navigate]);

  // Helper format thời gian
  const formatDateTime = (isoString: string) => {
    if (!isoString) return "--";
    return new Date(isoString).toLocaleString('vi-VN', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  };

  // Helper format thời lượng (giây -> phút:giây)
  const formatDuration = (seconds: number | null) => {
    if (!seconds) return "0s";
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    
    if (minutes > 0) return `${minutes}p ${secs}s`;
    return `${secs}s`;
  };

  if (isLoading) {
    return (
      <div className="landing-container">
        <div style={{ color: 'white' }}>⏳ Đang tải thông tin...</div>
      </div>
    );
  }

  if (!profile) return null;

  return (
    <div className="landing-container" style={{ justifyContent: 'flex-start', paddingTop: '40px' }}>
      <div className="landing-card" style={{ maxWidth: '900px', width: '95%', textAlign: 'left' }}>
        
        {/* Header với nút Back */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
          <h1 style={{ fontSize: '1.8rem', margin: 0 }}>👤 Hồ sơ cá nhân</h1>
          <button className="btn-secondary" onClick={() => navigate('/')}>
            ← Về trang chủ
          </button>
        </div>

        {/* Thông tin User */}
        <div style={{ 
            display: 'grid', 
            gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', 
            gap: '20px',
            marginBottom: '30px', 
            padding: '20px', 
            background: 'rgba(255,255,255,0.05)', 
            borderRadius: '8px',
            border: '1px solid #5f6368'
        }}>
          <div>
            <div style={{ color: '#9aa0a6', fontSize: '0.9rem', marginBottom: '5px' }}>Họ và tên</div>
            <div style={{ fontSize: '1.2rem', fontWeight: 'bold' }}>{profile.fullName}</div>
          </div>
          <div>
            <div style={{ color: '#9aa0a6', fontSize: '0.9rem', marginBottom: '5px' }}>Email</div>
            <div style={{ fontSize: '1.2rem' }}>{profile.email}</div>
          </div>
          <div>
            <div style={{ color: '#9aa0a6', fontSize: '0.9rem', marginBottom: '5px' }}>Vai trò</div>
            <div>
              <span className="room-badge" style={{ textTransform: 'uppercase' }}>
                {profile.role}
              </span>
            </div>
          </div>
        </div>

        {/* Lịch sử tham gia */}
        <h3 style={{ borderBottom: '1px solid #5f6368', paddingBottom: '10px', marginBottom: '15px' }}>
          📜 Lịch sử tham gia cuộc họp
        </h3>

        <div className="table-responsive">
          <table className="admin-table">
            <thead>
              <tr>
                <th>STT</th>
                <th>Mã phòng</th>
                <th>Thời gian tham gia</th>
                <th>Thời lượng phòng</th>
                <th>Hành động</th>
              </tr>
            </thead>
            <tbody>
              {profile.history && profile.history.length > 0 ? (
                profile.history.map((item, index) => (
                  <tr key={index}>
                    <td>{index + 1}</td>
                    <td>
                      <span style={{ color: '#8ab4f8', fontWeight: '500' }}>{item.roomId}</span>
                    </td>
                    <td>{formatDateTime(item.joinedAt)}</td>
                    <td>{formatDuration(item.durationSeconds)}</td>
                    <td>
                      {/* Nút join lại phòng cũ */}
                      <button 
                        className="btn-secondary" 
                        style={{ padding: '4px 10px', fontSize: '0.8rem' }}
                        onClick={() => navigate(`/room/group/${item.roomId}`)}
                      >
                        Vào lại
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', padding: '30px', color: '#9aa0a6' }}>
                    Bạn chưa tham gia cuộc họp nào.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

      </div>
    </div>
  );
}