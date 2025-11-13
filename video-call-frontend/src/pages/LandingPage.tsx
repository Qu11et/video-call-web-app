import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

export default function LandingPage() {
  const [roomId, setRoomId] = useState('');
  const navigate = useNavigate();

  const createRoom = () => {
    const newRoomId = Math.random().toString(36).substring(2, 8);
    navigate(`/room/${newRoomId}`);
  };

  const joinRoom = () => {
    if (roomId.trim()) {
      navigate(`/room/${roomId.trim()}`);
    } else {
      alert('Vui lòng nhập ID phòng');
    }
  };

  return (
    <div>
      <h1>Video Call App</h1>
      
      <div>
        <button onClick={createRoom}>Tạo phòng mới</button>
      </div>

      <hr style={{ margin: '2rem 0' }} />

      <div>
        <input 
          type="text" 
          placeholder="Nhập ID phòng"
          value={roomId}
          onChange={(e) => setRoomId(e.target.value)}
        />
        <button onClick={joinRoom}>Tham gia phòng</button>
      </div>
    </div>
  );
}