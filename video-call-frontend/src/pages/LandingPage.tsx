import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createRoomApi, checkRoomExistsApi } from '../api'; 

export default function LandingPage() {
  const [roomId, setRoomId] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

 const handleCreateRoom = async (type: 'P2P' | 'GROUP') => {
    setIsLoading(true);
    const newRoomId = await createRoomApi(type); // Gọi API với type
    setIsLoading(false);

    if (newRoomId) {
      // Điều hướng dựa trên type
      if (type === 'P2P') {
        navigate(`/room/p2p/${newRoomId}`);
      } else {
        navigate(`/room/group/${newRoomId}`);
      }
    } else {
      alert('Lỗi kết nối Backend!');
    }
  };

  const handleJoinRoom = async () => {
    if (!roomId.trim()) {
      alert('Vui lòng nhập ID phòng');
      return;
    }
    setIsLoading(true);
    
    // Kiểm tra phòng và lấy loại phòng
    const result = await checkRoomExistsApi(roomId);
    setIsLoading(false);

    if (result.exists) {
      // Tự động điều hướng đến đúng giao diện dựa trên loại phòng
      if (result.type === 'GROUP') {
        navigate(`/room/group/${roomId}`);
      } else {
        navigate(`/room/p2p/${roomId}`);
      }
    } else {
      alert('Phòng không tồn tại!');
    }
  };

  return (
    <div className="landing-container">
      <div className="landing-card">
        <h1>📹 Video Call App</h1>
        <p>Chọn chế độ gọi phù hợp</p>

        <div className="action-container">
          
          {/* Nút tạo P2P */}
          <button 
            className="btn-primary" 
            onClick={() => handleCreateRoom('P2P')}
            disabled={isLoading}
            style={{ width: '100%', marginBottom: '10px' }}
          >
            {isLoading ? '...' : '👤 Gọi 1-1 (P2P)'}
          </button>

          {/* Nút tạo Group (SFU) */}
          <button 
            className="btn-secondary" 
            onClick={() => handleCreateRoom('GROUP')}
            disabled={isLoading}
            style={{ width: '100%', borderColor: '#8ab4f8', color: '#8ab4f8' }}
          >
            {isLoading ? '...' : '👥 Gọi nhóm (SFU)'}
          </button>

          <div className="separator">hoặc tham gia</div>

          <div className="input-group">
            <input 
              type="text" 
              placeholder="Nhập mã phòng..."
              value={roomId}
              onChange={(e) => setRoomId(e.target.value)}
            />
            <button 
              className="btn-secondary" 
              onClick={handleJoinRoom}
              disabled={isLoading}
            >
              Vào
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}