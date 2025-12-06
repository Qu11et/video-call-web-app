import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createRoomApi, checkRoomExistsApi } from '../api'; 

export default function LandingPage() {
  const [roomId, setRoomId] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const navigate = useNavigate();

  const handleCreateRoom = async () => {
    setIsLoading(true);
    const newRoomId = await createRoomApi();
    setIsLoading(false);

    if (newRoomId) {
      navigate(`/room/${newRoomId}`);
    } else {
      alert('Không thể tạo phòng. Vui lòng kiểm tra Backend!');
    }
  };

  const handleJoinRoom = async () => {
    if (!roomId.trim()) {
      alert('Vui lòng nhập ID phòng');
      return;
    }

    setIsLoading(true);
    const exists = await checkRoomExistsApi(roomId);
    setIsLoading(false);

    if (exists) {
      navigate(`/room/${roomId}`);
    } else {
      alert('Phòng không tồn tại hoặc ID sai!');
    }
  };

  return (
    <div className="landing-container" >
      <div className="landing-card">
        <h1>📹 Video Call App</h1>
        <p> Kết nối dễ dàng, mọi lúc mọi nơi.</p>

        {/* --- TÔI ĐÃ GOM NHÓM VÀO ĐÂY --- */}
        <div className="action-container" >
          
          <button 
            className="btn-primary" 
            onClick={handleCreateRoom}
            disabled={isLoading}
          >
            {isLoading ? 'Đang xử lý...' : 'Tạo cuộc họp mới'}
          </button>

          {/* Container cho separator + input-group */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', width: '100%', marginTop: '20px' }}>
            <div className="separator" >
              hoặc
            </div>

            <div className="input-group" >
              <input 
                type="text" 
                placeholder="Nhập mã cuộc họp"
                value={roomId}
                onChange={(e) => setRoomId(e.target.value)}
              />
              <button 
                className="btn-secondary" 
                onClick={handleJoinRoom}
                disabled={isLoading}
              >
                Tham gia
              </button>
            </div>
          </div>
          
        </div>
        {/* --- HẾT PHẦN GOM NHÓM --- */}
      </div>
    </div>
  );
}