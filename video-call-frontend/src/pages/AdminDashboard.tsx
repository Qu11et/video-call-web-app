import React, { useEffect, useState } from 'react';
import type { Meeting, PageResponse } from '../api';
import { adminApi } from '../api';
import { useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import type { RootState } from '../store/store';

export default function AdminDashboard() {
  const navigate = useNavigate();
  const { user } = useSelector((state: RootState) => state.auth);

  const [data, setData] = useState<PageResponse<Meeting> | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [isLoading, setIsLoading] = useState(false);

  // Bảo vệ Client-side: Nếu không phải ADMIN -> Đá về trang chủ
  useEffect(() => {
    if (user?.role !== 'ADMIN') {
      alert("Bạn không có quyền truy cập trang này!");
      navigate('/');
    }
  }, [user, navigate]);

  const fetchMeetings = async (page: number) => {
    setIsLoading(true);
    const res = await adminApi.getMeetings(page, 10); // Lấy 10 dòng mỗi trang
    if (res) {
      setData(res);
      setCurrentPage(page);
    } else {
      alert("Không thể tải dữ liệu (Lỗi 403 hoặc lỗi mạng)");
    }
    setIsLoading(false);
  };

  useEffect(() => {
    fetchMeetings(0);
  }, []);

  // Helper format thời gian
  const formatTime = (isoString: string) => {
    if (!isoString) return "-";
    return new Date(isoString).toLocaleString('vi-VN');
  };

  const formatDuration = (seconds: number) => {
    const min = Math.floor(seconds / 60);
    const sec = seconds % 60;
    return `${min}p ${sec}s`;
  };

  return (
    <div className="landing-container" style={{ justifyContent: 'flex-start', paddingTop: '40px' }}>
      <div className="landing-card" style={{ maxWidth: '1200px', width: '95%', textAlign: 'left' }}>
        
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
          <h1 style={{ fontSize: '1.8rem', margin: 0 }}>🛡️ Admin Dashboard</h1>
          <button className="btn-secondary" onClick={() => navigate('/')}>Về trang chủ</button>
        </div>

        <h3>Lịch sử cuộc họp</h3>

        {isLoading ? (
          <p>Đang tải dữ liệu...</p>
        ) : (
          <>
            <div className="table-responsive">
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>Room ID</th>
                    <th>Bắt đầu</th>
                    <th>Kết thúc</th>
                    <th>Thời lượng</th>
                    <th>Thành viên</th>
                    <th>Chi tiết</th>
                  </tr>
                </thead>
                <tbody>
                  {data?.content.map((meeting) => (
                    <tr key={meeting.id}>
                      <td><span className="room-badge">{meeting.roomId}</span></td>
                      <td>{formatTime(meeting.startTime)}</td>
                      <td>{formatTime(meeting.endTime)}</td>
                      <td>{formatDuration(meeting.durationSeconds)}</td>
                      <td>{meeting.participants?.length || 0} người</td>
                      <td>
                        <button 
                          className="btn-secondary" 
                          style={{ padding: '4px 8px', fontSize: '0.8rem' }}
                          onClick={() => alert(JSON.stringify(meeting.participants, null, 2))}
                        >
                          Xem User
                        </button>
                      </td>
                    </tr>
                  ))}
                  {data?.content.length === 0 && (
                    <tr>
                      <td colSpan={6} style={{ textAlign: 'center', padding: '20px' }}>Chưa có cuộc họp nào được ghi lại.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>

            {/* Pagination Controls */}
            <div style={{ display: 'flex', justifyContent: 'center', gap: '10px', marginTop: '20px' }}>
              <button 
                className="btn-secondary" 
                disabled={data?.number === 0}
                onClick={() => fetchMeetings(currentPage - 1)}
              >
                &lt; Trước
              </button>
              <span style={{ display: 'flex', alignItems: 'center' }}>
                Trang {currentPage + 1} / {data?.totalPages || 1}
              </span>
              <button 
                className="btn-secondary" 
                disabled={data ? currentPage >= data.totalPages - 1 : true}
                onClick={() => fetchMeetings(currentPage + 1)}
              >
                Sau &gt;
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}