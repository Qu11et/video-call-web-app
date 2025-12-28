import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { userApi } from '../api';
import type { UserProfileResponse, MeetingHistory, PageResponse  } from '../api';


export default function ProfilePage() {
  const navigate = useNavigate();
  // State cho User Info
  const [profile, setProfile] = useState<UserProfileResponse | null>(null);
  
  // State cho History Pagination
  const [historyData, setHistoryData] = useState<PageResponse<MeetingHistory> | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [isHistoryLoading, setIsHistoryLoading] = useState(false);

  // 1. Fetch Profile (Chạy 1 lần)
  useEffect(() => {
    userApi.getProfile()
      .then(data => {
        if (data) setProfile(data);
        else navigate('/login');
      });
  }, [navigate]);

  // 2. Fetch History (Chạy mỗi khi currentPage thay đổi)
  useEffect(() => {
    const fetchHistory = async () => {
      setIsHistoryLoading(true);
      const data = await userApi.getHistory(currentPage, 5); // Lấy 5 dòng mỗi trang
      if (data) setHistoryData(data);
      setIsHistoryLoading(false);
    };
    fetchHistory();
  }, [currentPage]);

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

  if (isHistoryLoading) {
    return (
      <div className="landing-container">
        <div style={{ color: 'white' }}>⏳ Đang tải thông tin...</div>
      </div>
    );
  }

  if (!profile) return <div className="landing-container" style={{color:'white'}}>Loading...</div>;

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

        {/* --- PHẦN LỊCH SỬ CÓ PAGINATION --- */}
        <h3 style={{ borderBottom: '1px solid #5f6368', paddingBottom: '10px', marginBottom: '15px' }}>
          📜 Lịch sử tham gia ({historyData?.totalElements || 0})
        </h3>

        {isHistoryLoading ? (
            <p style={{textAlign: 'center', color: '#9aa0a6'}}>Đang tải lịch sử...</p>
        ) : (
            <>
            <div className="table-responsive">
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>Mã phòng</th>
                    <th>Thời gian</th>
                    <th>Thời lượng</th>
                    <th>Hành động</th>
                  </tr>
                </thead>
                <tbody>
                  {historyData?.content.map((item, index) => (
                    <tr key={index}>
                      <td><span style={{ color: '#8ab4f8' }}>{item.roomId}</span></td>
                      <td>{formatDateTime(item.joinedAt)}</td>
                      <td>{formatDuration(item.durationSeconds)}</td>
                      <td>
                        <button 
                          className="btn-secondary" 
                          style={{ padding: '4px 10px', fontSize: '0.8rem' }}
                          onClick={() => navigate(`/room/group/${item.roomId}`)}
                        >
                          Vào lại
                        </button>
                      </td>
                    </tr>
                  ))}
                  {historyData?.content.length === 0 && (
                    <tr><td colSpan={4} style={{ textAlign: 'center', padding: '20px' }}>Chưa có lịch sử.</td></tr>
                  )}
                </tbody>
              </table>
            </div>

            {/* Pagination Controls */}
            {historyData && historyData.totalPages > 1 && (
                <div style={{ display: 'flex', justifyContent: 'center', gap: '10px', marginTop: '20px' }}>
                  <button 
                    className="btn-secondary" 
                    disabled={historyData.number === 0}
                    onClick={() => setCurrentPage(p => p - 1)}
                  >
                    &lt; Trước
                  </button>
                  <span style={{ display: 'flex', alignItems: 'center', fontSize: '0.9rem' }}>
                    Trang {historyData.number + 1} / {historyData.totalPages}
                  </span>
                  <button 
                    className="btn-secondary" 
                    disabled={historyData.number >= historyData.totalPages - 1}
                    onClick={() => setCurrentPage(p => p + 1)}
                  >
                    Sau &gt;
                  </button>
                </div>
            )}
            </>
        )}
        
      </div>
    </div>
  );
}