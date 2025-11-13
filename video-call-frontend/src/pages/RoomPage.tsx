import React from 'react';
import { useParams } from 'react-router-dom';

export default function RoomPage() {
  const { roomId } = useParams();

  return (
    <div>
      <h2>Chào mừng bạn đến với phòng: {roomId}</h2>
      <p>(Đây là nơi sẽ hiển thị video call...)</p>
      
      <div style={{ border: '1px solid black', padding: '1rem', minHeight: '300px' }}>
        <p>[Video containers]</p>
      </div>

      <div style={{ marginTop: '1rem' }}>
        <button>Mute/Unmute</button>
        <button>Camera On/Off</button>
        <button style={{ backgroundColor: 'red', color: 'white' }}>Hang Up</button>
      </div>
    </div>
  );
}