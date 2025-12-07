import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  LiveKitRoom,
  VideoConference,
  GridLayout,
  ParticipantTile,
  RoomAudioRenderer,
  ControlBar,
  useTracks,
} from '@livekit/components-react';
import '@livekit/components-styles'; // Import CSS mặc định của LiveKit (Rất quan trọng)
import { Track } from 'livekit-client';

export default function GroupCallPage() {
  const { roomId } = useParams();
  const navigate = useNavigate();
  
  const [token, setToken] = useState("");

  // Giả lập thông tin user (Trong thực tế nên lấy từ Login)
  // Mỗi lần vào sẽ random một ID để test nhiều tab
  const [userId] = useState("user-" + Math.random().toString(36).substring(7));
  const [userName] = useState("User " + Math.floor(Math.random() * 100));

  useEffect(() => {
    if (!roomId) return;

    const fetchToken = async () => {
      try {
        // Gọi API Backend Spring Boot để lấy Token
        const response = await fetch(`http://localhost:8080/api/rooms/${roomId}/token`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ 
            userId: userId,
            name: userName
          })
        });

        if (!response.ok) {
          throw new Error('Failed to fetch token');
        }

        const data = await response.json();
        setToken(data.token);
      } catch (error) {
        console.error(error);
        alert("Không thể lấy Token từ Backend. Hãy chắc chắn Backend đang chạy!");
        navigate('/');
      }
    };

    fetchToken();
  }, [roomId, userId, userName, navigate]);

  if (!token) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', background: '#111', color: 'white' }}>
        Getting Token...
      </div>
    );
  }

  return (
    // LiveKitRoom là component bao bọc, tự động xử lý kết nối
    <LiveKitRoom
      video={true} // Tự động bật cam
      audio={true} // Tự động bật mic
      token={token}
      serverUrl="ws://localhost:7880" // Địa chỉ LiveKit Server (Docker)
      // Khi người dùng bấm nút rời phòng trên giao diện LiveKit
      onDisconnected={() => navigate('/')}
      data-lk-theme="default"
      style={{ height: '100vh' }} // Full màn hình
    >
      {/* VideoConference là component "All-in-one" của LiveKit.
        Nó tự động hiển thị Grid Video, Speaker View, và thanh Control Bar.
       */}
      <VideoConference />

      {/* Nếu bạn muốn custom giao diện thay vì dùng VideoConference mặc định,
        bạn có thể xóa <VideoConference /> và dùng các component nhỏ lẻ như:
        <GridLayout tracks={...}> ... </GridLayout>
        Nhưng để bắt đầu, VideoConference là tốt nhất.
      */}
      
    </LiveKitRoom>
  );
}