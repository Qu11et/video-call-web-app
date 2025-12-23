import React, { useState, useRef, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import SimplePeer from 'simple-peer';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { useSelector } from 'react-redux';
import type { RootState } from '../store/store';

interface SignalMessage {
  type: string;
  senderSessionId: string;
  senderName?: string;
  targetSessionId?: string;
  sessionId?: string;
  data?: any;
}

interface ToastMessage {
  id: number;
  message: string;
  type: 'info' | 'success' | 'error' | 'warning';
}

const rtcConfig = {
  iceServers: [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:global.stun.twilio.com:3478' }
  ]
};

export default function P2PCallPage() {
  const { roomId } = useParams();
  const navigate = useNavigate();

  const [isMuted, setIsMuted] = useState(false);
  const [isCameraOff, setIsCameraOff] = useState(false);
  const [localStream, setLocalStream] = useState<MediaStream | null>(null);
  const [isLocalVideoMinimized, setIsLocalVideoMinimized] = useState(false);
  const [participantCount, setParticipantCount] = useState(1);
  const [remoteParticipantName, setRemoteParticipantName] = useState('');
  const [isRemoteConnected, setIsRemoteConnected] = useState(false);
  const [remoteStream, setRemoteStream] = useState<MediaStream | null>(null);

  const localVideoRef = useRef<HTMLVideoElement>(null);
  const remoteVideoRef = useRef<HTMLVideoElement>(null);
  const peerRef = useRef<SimplePeer.Instance | null>(null);
  const stompClientRef = useRef<Client | null>(null);
  const started = useRef(false);
  
  // Lấy user từ Redux
  const { user, isAuthenticated } = useSelector((state: RootState) => state.auth);

  // Logic tạo ID/Name cho Guest
  const guestId = useRef("guest-" + Math.random().toString(36).substring(7)).current;
  const guestName = useRef("Khách " + Math.floor(Math.random() * 1000)).current;

  // Chọn ID/Name để sử dụng
  const mySessionId = useRef(isAuthenticated ? user?.email : guestId);
  const myName = isAuthenticated ? user?.fullName : guestName;

  const [connectionStatus, setConnectionStatus] = useState<'connected' | 'reconnecting' | 'disconnected'>('disconnected');
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const addToast = (message: string, type: 'info' | 'success' | 'error' | 'warning' = 'info') => {
    const id = Date.now();
    setToasts(prev => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id));
    }, 3000);
  };

  const cleanupRemoteConnection = () => {
    console.log("🧹 Dọn dẹp kết nối remote...");
    if (remoteVideoRef.current) {
      remoteVideoRef.current.srcObject = null;
    }
    setRemoteStream(null);
    setIsRemoteConnected(false);
    setRemoteParticipantName('');
    setParticipantCount(1);
    if (peerRef.current) {
      peerRef.current.destroy();
      peerRef.current = null;
    }
  };

  useEffect(() => {
    if (started.current) return;
    started.current = true;

    const init = async () => {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
        setLocalStream(stream);
        if (localVideoRef.current) localVideoRef.current.srcObject = stream;
        connectToWebSocket(stream);
      } catch (error) {
        console.error("Lỗi:", error);
        alert("Không thể truy cập Camera/Mic!");
      }
    };
    init();

    return () => {
      if (stompClientRef.current) stompClientRef.current.deactivate();
      if (peerRef.current) peerRef.current.destroy();
      if (localStream) localStream.getTracks().forEach(track => track.stop());
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const connectToWebSocket = (stream: MediaStream) => {
    setConnectionStatus('reconnecting');
    addToast("Đang kết nối đến máy chủ...", "info");

    // --- LOGIC CHỌN URL THÔNG MINH ---
    // 1. Nếu chạy Localhost: Dùng cứng cổng 8080 (vì không có Nginx proxy ở local dev)
    // 2. Nếu chạy VPS (Production): Dùng đường dẫn tương đối '/ws'. 
    //    Nginx (ở cổng 80) sẽ tự động nhận request '/ws' và chuyển tiếp sang Backend (8080).
    
    const socketUrl = window.location.hostname === 'localhost' 
        ? 'http://localhost:8080/ws' 
        : '/ws';

    console.log("Connecting to WebSocket at:", socketUrl);

    const socket = new SockJS(socketUrl);
    const client = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      onConnect: () => {
        console.log(`--> Đã kết nối WS! ID của tôi: ${mySessionId.current}`);
        setConnectionStatus('connected');
        addToast("Kết nối thành công!", "success");
        
        client.subscribe(`/topic/room/${roomId}`, (message) => {
          const payload = JSON.parse(message.body) as SignalMessage;
          handleSignalingData(payload, stream);
        });

        client.publish({
          destination: '/app/join',
          body: JSON.stringify({ 
            roomId: roomId, 
            myID: mySessionId.current,
            displayName: myName // <--- Gửi tên lên để Backend báo cho người khác
          }),
        });
      },

      onWebSocketClose: () => {
        console.warn("Mất kết nối WebSocket!");
        setConnectionStatus('disconnected');
        addToast("Mất kết nối! Đang thử lại...", "error");
        cleanupRemoteConnection();
      },

      onStompError: (frame) => {
        console.error('Lỗi Broker: ' + frame.headers['message']);
        addToast("Lỗi hệ thống: " + frame.headers['message'], "error");
      },
    });
    client.activate();
    stompClientRef.current = client;
  };

  const handleSignalingData = (payload: SignalMessage, stream: MediaStream) => {
    if (payload.type !== 'user-left') {
      if (payload.senderSessionId === mySessionId.current || payload.sessionId === mySessionId.current) return;
      if (payload.targetSessionId && payload.targetSessionId !== mySessionId.current) return;
    }

    switch (payload.type) {
      case 'user-joined':
        addToast("Có người mới tham gia!", "success");
        console.log(`User mới ${payload.sessionId} vào phòng.`);
        cleanupRemoteConnection(); 
        setParticipantCount(2);
        setRemoteParticipantName(`User ${payload.sessionId?.substring(0, 6)}`);
        if (peerRef.current) peerRef.current.destroy();
        createPeer(payload.sessionId!, stream); 
        break;

      case 'offer':
        addToast("Nhận cuộc gọi từ người khác", "info");
        console.log(`Nhận Offer từ ${payload.senderSessionId}.`);
        cleanupRemoteConnection(); 
        setParticipantCount(2);
        // Hiển thị tên thật của người gọi đến
        setRemoteParticipantName(payload.senderName || `User ${payload.senderSessionId?.substring(0, 6)}`);
        addPeer(payload.data, payload.senderSessionId, stream);
        break;

      case 'answer':
        console.log(`Nhận Answer từ ${payload.senderSessionId}.`);
        if (payload.senderName) {
            setRemoteParticipantName(payload.senderName);
        }
        if (peerRef.current) peerRef.current.signal(payload.data);
        break;

      case 'ice-candidate':
        if (peerRef.current) peerRef.current.signal(payload.data);
        break;
        
      case 'user-left':
        console.log(`User ${payload.sessionId || payload.senderSessionId} đã rời phòng.`);
        addToast("Người kia đã rời khỏi phòng", "warning");
        cleanupRemoteConnection();
        break;
    }
  };

  // --- 1. NGƯỜI GỌI (INITIATOR) ---
  const createPeer = (targetSessionId: string, stream: MediaStream) => {
    const peer = new SimplePeer({
      initiator: true,
      trickle: false,
      stream: stream,
      config: rtcConfig
    });

    peer.on('signal', (signal) => {
      if (signal.type === 'offer') {
        stompClientRef.current?.publish({
          destination: '/app/signal',
          body: JSON.stringify({
            type: 'offer',
            data: signal,
            targetSessionId: targetSessionId,
            senderSessionId: mySessionId.current,
            senderName: myName // <--- Gửi tên mình kèm Offer
          })
        });
      }
    });

    setupPeerEvents(peer);
    peerRef.current = peer;
  };

  // --- 2. NGƯỜI NHẬN (RECEIVER) ---
  const addPeer = (incomingSignal: any, senderSessionId: string, stream: MediaStream) => {
    if (peerRef.current) peerRef.current.destroy();

    const peer = new SimplePeer({
      initiator: false,
      trickle: false,
      stream: stream,
      config: rtcConfig
    });

    peer.on('signal', (signal) => {
      if (signal.type === 'answer') {
        stompClientRef.current?.publish({
          destination: '/app/signal',
          body: JSON.stringify({
            type: 'answer',
            data: signal,
            targetSessionId: senderSessionId,
            senderSessionId: mySessionId.current,
            senderName: myName // <--- Gửi tên mình kèm Answer
          })
        });
      }
    });

    peer.signal(incomingSignal);
    setupPeerEvents(peer);
    peerRef.current = peer;
  };

  const setupPeerEvents = (peer: SimplePeer.Instance) => {
    peer.on('stream', (stream) => {
      console.log(">>> ĐÃ NHẬN ĐƯỢC REMOTE STREAM! <<<");
      setRemoteStream(stream);
      setIsRemoteConnected(true);
      addToast("Kết nối video thành công!", "success");
    });

    peer.on('close', () => {
      addToast("Người kia đã ngắt kết nối", "warning");
      cleanupRemoteConnection();
    });

    peer.on('error', (err) => {
      console.error(">>> LỖI P2P:", err);
      addToast("Lỗi kết nối Video (P2P): " + err.message, "error");
      cleanupRemoteConnection();
    });
  };

  useEffect(() => {
    if (isRemoteConnected && remoteVideoRef.current && remoteStream) {
      remoteVideoRef.current.srcObject = remoteStream;
    }
  }, [isRemoteConnected, remoteStream]);

  // --- UI LOGIC ---
  const toggleMic = () => { if (localStream) { const t = localStream.getAudioTracks()[0]; if(t) { t.enabled = !t.enabled; setIsMuted(!t.enabled); } } };
  const toggleCamera = () => { if (localStream) { const t = localStream.getVideoTracks()[0]; if(t) { t.enabled = !t.enabled; setIsCameraOff(!t.enabled); } } };
  const handleHangUp = () => { 
      if (localStream) localStream.getTracks().forEach(t => t.stop()); 
      if (stompClientRef.current) stompClientRef.current.deactivate(); 
      navigate('/'); 
  };

  return (
    <div className="room-container">
      {/* Header */}
      <div className="room-header">
        <div className="room-info">
          <div className="room-id">📹 {roomId}</div>
          <div className={`connection-badge status-${connectionStatus}`}>
            <div className="connection-dot"></div>
            <span>
              {connectionStatus === 'connected' && "Online"}
              {connectionStatus === 'reconnecting' && "Reconnecting..."}
              {connectionStatus === 'disconnected' && "Offline"}
            </span>
          </div>
          <div className="participant-count">👥 {participantCount} participant{participantCount > 1 ? 's' : ''}</div>
        </div>
        <button className="btn-secondary" onClick={() => navigator.clipboard.writeText(`${window.location.origin}/room/${roomId}`)} style={{ padding: '0.5rem 1rem', fontSize: '0.9rem' }}>
          📋 Copy Link
        </button>
      </div>

      {/* Video Grid */}
      <div className={`video-grid ${participantCount === 1 ? 'single-participant' : 'two-participants'}`}>
        {/* REMOTE VIDEO */}
        <div className="video-card">
          {!isRemoteConnected ? (
            <div className="waiting-participant">
              <div className="icon">👥</div>
              <div className="text">Waiting for others to join...</div>
              <div className="subtext">Share the room link to invite participants</div>
            </div>
          ) : (
            <>
              <video 
                ref={remoteVideoRef} 
                autoPlay 
                playsInline 
                style={{ width: '100%', height: '100%', objectFit: 'cover', transform: 'scaleX(-1)' }}
              />
              <div className="participant-overlay">
                <div className="participant-name">{remoteParticipantName}</div>
                <div className="participant-status">
                  <div className="status-indicator">🎤</div>
                  <div className="status-indicator">📹</div>
                </div>
              </div>
            </>
          )}
        </div>

        {/* LOCAL VIDEO */}
        {isRemoteConnected ? (
          <div 
            className={`video-card local-video ${isLocalVideoMinimized ? 'minimized' : ''}`}
            onClick={() => setIsLocalVideoMinimized(!isLocalVideoMinimized)}
          >
            {isCameraOff && <div className="camera-off-overlay"><div className="icon">📷</div><div className="text">Camera Off</div></div>}
            <video 
              ref={localVideoRef} autoPlay muted playsInline 
              style={{ width: '100%', height: '100%', objectFit: 'cover', opacity: isCameraOff ? 0 : 1, transform: 'scaleX(-1)' }} 
            />
            <div className="participant-overlay">
              <div className="participant-name">You</div>
            </div>
          </div>
        ) : (
          <div className="video-card">
            {isCameraOff && <div className="camera-off-overlay"><div className="icon">📷</div><div className="text">Your Camera is Off</div></div>}
            <video 
              ref={localVideoRef} autoPlay muted playsInline 
              style={{ width: '100%', height: '100%', objectFit: 'cover', opacity: isCameraOff ? 0 : 1, transform: 'scaleX(-1)' }} 
            />
            <div className="participant-overlay">
              <div className="participant-name">You (Waiting)</div>
            </div>
          </div>
        )}
      </div>

      {/* Controls */}
      <div className="controls-bar">
        <button className={`control-btn ${isMuted ? 'off' : ''}`} onClick={toggleMic}>
          {isMuted ? "🔇" : "🎤"} <div className="control-tooltip">{isMuted ? 'Unmute' : 'Mute'}</div>
        </button>
        <button className={`control-btn ${isCameraOff ? 'off' : ''}`} onClick={toggleCamera}>
          {isCameraOff ? "🚫" : "📹"} <div className="control-tooltip">{isCameraOff ? 'Turn on camera' : 'Turn off camera'}</div>
        </button>
        <button className="control-btn btn-hangup" onClick={handleHangUp}>
          📞 <div className="control-tooltip">Leave call</div>
        </button>
      </div>

      {/* Toasts */}
      <div className="toast-container">
        {toasts.map(toast => (
          <div key={toast.id} className={`toast ${toast.type}`}>
            <span>{toast.message}</span>
          </div>
        ))}
      </div>
    </div>
  );
}