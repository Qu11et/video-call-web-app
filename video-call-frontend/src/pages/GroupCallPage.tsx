import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux'; 
import type { RootState } from '../store/store'; 
import confetti from 'canvas-confetti';
import {
  LiveKitRoom,
  VideoConference,
} from '@livekit/components-react';
import '@livekit/components-styles';

export default function GroupCallPage() {
  const { roomId } = useParams();
  const navigate = useNavigate();
  
  const { user, isAuthenticated } = useSelector((state: RootState) => state.auth);

  const [guestId] = useState("guest-" + Math.random().toString(36).substring(7));
  const [guestName] = useState("Khách " + Math.floor(Math.random() * 1000));

  const userId = isAuthenticated ? user?.email : guestId;
  const userName = isAuthenticated ? user?.fullName : guestName;

  const [token, setToken] = useState("");
  const [serverUrl, setServerUrl] = useState("");
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!roomId) return;

    const fetchToken = async () => {
      try {
        const response = await fetch(`/api/rooms/${roomId}/token`, {
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
        setServerUrl(data.serverUrl);
      } catch (error) {
        console.error(error);
        alert("Không thể lấy Token từ Backend. Hãy chắc chắn Backend đang chạy!");
        navigate('/');
      }
    };
    fetchToken();
  }, [roomId, userId, userName, navigate]);

  // ✅ HÀM COPY ROOM ID
  const handleCopyRoomId = async () => {
    if (roomId) {
      try {
        await navigator.clipboard.writeText(roomId);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000); // Hiển thị "Đã copy" 2 giây

        // 🎉 Confetti explosion từ góc phải trên (gần nút)
        confetti({
          particleCount: 120,     // Số lượng pháo giấy
          spread: 80,             // Độ lan tỏa
          origin: { x: 0, y: 1 }, // x=0: cạnh trái, y=1: đáy màn hình
          colors: ['#007bff', '#28a745', '#ffffff', '#ffdd00', '#ff6b6b'], // Màu vui mắt
          gravity: 0.8,
          ticks: 250,             // Thời gian tồn tại lâu hơn một chút
          disableForReducedMotion: true, // Tôn trọng setting giảm motion của user
        });
      } catch (err) {
        console.error('Failed to copy:', err);
      }
    }
  };
  if (!token) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', background: '#111', color: 'white' }}>
        Getting Token...
      </div>
    );
  }

  return (
    <div style={{ position: 'relative', height: '100vh' }}>
      {/* ✅ NÚT COPY ROOM ID - FLOATING Ở GÓC TRÊN PHẢI */}
      <button
        onClick={handleCopyRoomId}
        style={{
          position: 'absolute',
          bottom: '12px',    // Đổi từ top → bottom
          left: '10px',      // Đổi từ right → left
          zIndex: 1000,
          padding: '13px 5px',
          background: copied 
            ? 'rgba(40, 167, 69, 0.25)'   // Xanh lá khi đã copy (thành công)
            : 'rgba(255, 255, 255, 0.1)', // Trắng mờ bình thường
          color: 'white',
          border: '1px solid rgba(255, 255, 255, 0.2)',
          borderRadius: '10px',
          cursor: 'pointer',
          fontSize: '14px',
          // fontWeight: 'bold',
          boxShadow: '0 8px 32px rgba(0, 0, 0, 0.4)',
          backdropFilter: 'blur(12px)',           // Key cho hiệu ứng kính mờ
          WebkitBackdropFilter: 'blur(12px)',     // Support Safari
          transition: 'all 0.4s ease',
          transform: copied ? 'scale(1.08)' : 'scale(1)', // Phóng to nhẹ khi copy
        }}
        onMouseEnter={(e) => e.currentTarget.style.transform = 'scale(1.1)'}
        onMouseLeave={(e) => e.currentTarget.style.transform = copied ? 'scale(1.08)' : 'scale(1)'}
      >
        {copied ? '✅ Đã copy!' : '📋 Copy Room ID'}
      </button>

      <LiveKitRoom
        video={true}
        audio={true}
        token={token}
        serverUrl={serverUrl}
        onDisconnected={() => navigate('/')}
        data-lk-theme="default"
        style={{ height: '100vh' }}
      >
        <VideoConference />
      </LiveKitRoom>
    </div>
  );
}