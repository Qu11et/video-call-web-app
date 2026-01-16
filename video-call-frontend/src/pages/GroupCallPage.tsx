import React, { useEffect, useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import type { RootState } from '../store/store';
import confetti from 'canvas-confetti';
import {
  LiveKitRoom,
  GridLayout,
  ParticipantTile,
  RoomAudioRenderer,
  ControlBar,
  useTracks,
} from '@livekit/components-react';
import { Track } from 'livekit-client';
import '@livekit/components-styles';

export default function GroupCallPage() {
  const { roomId } = useParams();
  const navigate = useNavigate();
  
  const { user, isAuthenticated } = useSelector((state: RootState) => state.auth);

  const { userId, userName } = useMemo(() => {
    if (isAuthenticated && user) {
      return { 
        userId: user.email, 
        userName: user.fullName 
      };
    }

    let storedId = sessionStorage.getItem('guest_id');
    let storedName = sessionStorage.getItem('guest_name');

    if (!storedId) {
      storedId = "guest-" + Math.random().toString(36).substring(7);
      storedName = "Khách " + Math.floor(Math.random() * 1000);
      sessionStorage.setItem('guest_id', storedId);
      sessionStorage.setItem('guest_name', storedName);
    }

    return { 
      userId: storedId, 
      userName: storedName 
    };
  }, [isAuthenticated, user]);

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

  const handleCopyRoomId = async () => {
    if (roomId) {
      try {
        await navigator.clipboard.writeText(roomId);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);

        confetti({
          particleCount: 120,
          spread: 80,
          origin: { x: 0, y: 1 },
          colors: ['#007bff', '#28a745', '#ffffff', '#ffdd00', '#ff6b6b'],
          gravity: 0.8,
          ticks: 250,
          disableForReducedMotion: true,
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
      <button
        onClick={handleCopyRoomId}
        style={{
          position: 'absolute',
          bottom: '12px',
          left: '10px',
          zIndex: 1000,
          padding: '13px 5px',
          background: copied 
            ? 'rgba(40, 167, 69, 0.25)'
            : 'rgba(255, 255, 255, 0.1)',
          color: 'white',
          border: '1px solid rgba(255, 255, 255, 0.2)',
          borderRadius: '10px',
          cursor: 'pointer',
          fontSize: '14px',
          boxShadow: '0 8px 32px rgba(0, 0, 0, 0.4)',
          backdropFilter: 'blur(12px)',
          WebkitBackdropFilter: 'blur(12px)',
          transition: 'all 0.4s ease',
          transform: copied ? 'scale(1.08)' : 'scale(1)',
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
        serverUrl={serverUrl || "wss://livekit.lkht.id.vn"}
        onDisconnected={() => navigate('/')}
        data-lk-theme="default"
        style={{ height: '100vh', display: 'flex', flexDirection: 'column' }}
      >
        <RoomAudioRenderer />

        {/* ✅ THÊM key để force re-render đúng */}
        <div key={`grid-${userId}`} style={{ flex: 1, overflow: 'hidden', padding: '10px' }}>
          <MyVideoGrid />
        </div>

        <ControlBar />
      </LiveKitRoom>
    </div>
  );
}

// ✅ SỬA: Tắt placeholder và thêm logic lọc track
function MyVideoGrid() {
  const tracks = useTracks(
    [
      // ✅ QUAN TRỌNG: Tắt withPlaceholder
      { source: Track.Source.Camera, withPlaceholder: false },
      { source: Track.Source.ScreenShare, withPlaceholder: false },
    ],
    { onlySubscribed: false }
  );

  // ✅ THÊM: Lọc bỏ các track trùng lặp
  const uniqueTracks = useMemo(() => {
    const trackMap = new Map();
    
    tracks.forEach((track) => {
      const key = `${track.participant.identity}_${track.source}`;
      
      // Ưu tiên track có publication (track thật) hơn placeholder
      if (!trackMap.has(key) || track.publication) {
        trackMap.set(key, track);
      }
    });
    
    return Array.from(trackMap.values());
  }, [tracks]);

  return (
    <GridLayout tracks={uniqueTracks} style={{ height: '100%' }}>
      <ParticipantTile />
    </GridLayout>
  );
}