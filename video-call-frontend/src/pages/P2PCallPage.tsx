import React, { useState, useRef, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import SimplePeer from 'simple-peer';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

interface SignalMessage {
  type: string;
  senderSessionId: string;
  targetSessionId?: string;
  sessionId?: string;
  data?: any;
}

// Cấu hình STUN Server (QUAN TRỌNG ĐỂ KẾT NỐI P2P)
const rtcConfig = {
  iceServers: [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:global.stun.twilio.com:3478' }
  ]
};

export default function RoomPage() {
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
  
  // Lưu Session ID của mình
  const mySessionId = useRef(Math.random().toString(36).substring(7));

  // 1. State quản lý trạng thái kết nối WebSocket
  const [connectionStatus, setConnectionStatus] = useState<'connected' | 'reconnecting' | 'disconnected'>('disconnected');

  // 2. State quản lý danh sách thông báo (Toasts)
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  // Helper: Hàm thêm thông báo (tự động biến mất sau 3s)
  const addToast = (message: string, type: 'info' | 'success' | 'error' | 'warning' = 'info') => {
    const id = Date.now();
    setToasts(prev => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id));
    }, 3000); // 3 giây
  };

  interface ToastMessage {
    id: number;
    message: string;
    type: 'info' | 'success' | 'error' | 'warning';
  }
  
  // --- HÀM DỌN DẸP KẾT NỐI (FIX LỖI ĐÔNG CỨNG HÌNH) ---
  const cleanupRemoteConnection = () => {
    console.log("🧹 Dọn dẹp kết nối remote...");
    
    // 1. Xóa hình ảnh trên thẻ video ngay lập tức
    if (remoteVideoRef.current) {
      remoteVideoRef.current.srcObject = null;
    }

    // 2. Reset State về ban đầu
    setRemoteStream(null);
    setIsRemoteConnected(false);
    setRemoteParticipantName('');
    setParticipantCount(1);

    // 3. Hủy Peer Connection
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

    // Cập nhật trạng thái UI
    setConnectionStatus('reconnecting');
    addToast("Đang kết nối đến máy chủ...", "info");

    const socket = new SockJS('http://localhost:8080/ws');
    const client = new Client({
      webSocketFactory: () => socket,

      // --- CẤU HÌNH RECONNECT & HEARTBEAT ---
      reconnectDelay: 5000, // Tự động thử kết nối lại sau 5s nếu mất mạng
      heartbeatIncoming: 4000, // Kiểm tra kết nối mỗi 4s
      heartbeatOutgoing: 4000,

      onConnect: () => {
        console.log(`--> Đã kết nối WS! ID của tôi: ${mySessionId.current}`);
        setConnectionStatus('connected'); // Cập nhật trạng thái Xanh
        addToast("Kết nối thành công!", "success");
        
        client.subscribe(`/topic/room/${roomId}`, (message) => {
          const payload = JSON.parse(message.body) as SignalMessage;
          handleSignalingData(payload, stream);
        });

        // --- SỬA ĐỔI TẠI ĐÂY: Thêm myID vào body ---
        client.publish({
          destination: '/app/join',
          body: JSON.stringify({ 
            roomId: roomId, 
            myID: mySessionId.current // <--- QUAN TRỌNG
          }),
          // Headers có thể bỏ hoặc giữ cũng được
        });
      },

      // Xử lý khi mất kết nối WebSocket
      onWebSocketClose: () => {
        console.warn("Mất kết nối WebSocket!");
        setConnectionStatus('disconnected');
        addToast("Mất kết nối! Đang thử lại...", "error");

        // ✅ THÊM: Dọn dẹp khi mất kết nối WS
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
    // ❌ BỎ DÒNG NÀY - Đang làm bỏ qua user-left
  // if (payload.senderSessionId === mySessionId.current || payload.sessionId === mySessionId.current) return;

  // ✅ CHỈ BỎ QUA KHI LÀ MESSAGE TỪ CHÍNH MÌNH (trừ user-left)
  if (payload.type !== 'user-left') {
    if (payload.senderSessionId === mySessionId.current || payload.sessionId === mySessionId.current) return;
    if (payload.targetSessionId && payload.targetSessionId !== mySessionId.current) return;
  }

    switch (payload.type) {
      case 'user-joined':
        addToast("Có người mới tham gia!", "success"); // <--- THÊM
        console.log(`User mới ${payload.sessionId} vào phòng. Mình (Initiator) sẽ gọi.`);
        cleanupRemoteConnection(); 
        setParticipantCount(2);
        setRemoteParticipantName(`User ${payload.sessionId?.substring(0, 6)}`);
        if (peerRef.current) peerRef.current.destroy();
        createPeer(payload.sessionId!, stream); 
        break;

      case 'offer':
        addToast("Nhận cuộc gọi từ người khác", "info"); // ← SỬA MESSAGE
        console.log(`Nhận Offer từ ${payload.senderSessionId}. Mình (Receiver) sẽ trả lời.`);
        cleanupRemoteConnection(); 
        setParticipantCount(2);
        setRemoteParticipantName(`User ${payload.senderSessionId?.substring(0, 6)}`);
        addPeer(payload.data, payload.senderSessionId, stream);
        break;

      case 'answer':
        console.log(`Nhận Answer từ ${payload.senderSessionId}. Kết nối P2P...`);
        if (peerRef.current) peerRef.current.signal(payload.data);
        break;

      case 'ice-candidate': // Hỗ trợ trường hợp backend gửi ICE riêng lẻ (nếu trickle: true)
        if (peerRef.current) peerRef.current.signal(payload.data);
        break;
        
      case 'user-left':
        // ✅ LUÔN XỬ LÝ user-left, không quan tâm sessionId
        console.log(`User ${payload.sessionId || payload.senderSessionId} đã rời phòng.`);
        addToast("Người kia đã rời khỏi phòng", "warning");
        cleanupRemoteConnection(); // Gọi 1 lần duy nhất
        break;
    }
  };

  // --- 1. NGƯỜI GỌI (INITIATOR) ---
  const createPeer = (targetSessionId: string, stream: MediaStream) => {
    const peer = new SimplePeer({
      initiator: true,
      trickle: false, // Gom SDP + ICE thành 1 cục
      stream: stream,
      config: rtcConfig // <--- THÊM CẤU HÌNH STUN SERVER
    });

    peer.on('signal', (signal) => {
      // Chỉ gửi khi đã gom đủ tín hiệu (type: 'offer')
      if (signal.type === 'offer') {
          stompClientRef.current?.publish({
            destination: '/app/signal',
            body: JSON.stringify({
              type: 'offer',
              data: signal,
              targetSessionId: targetSessionId,
              senderSessionId: mySessionId.current
            })
          });
      }
    });

    setupPeerEvents(peer); // Cài đặt các log sự kiện chung
    peerRef.current = peer;
  };

  // --- 2. NGƯỜI NHẬN (RECEIVER) ---
  const addPeer = (incomingSignal: any, senderSessionId: string, stream: MediaStream) => {
    // Nếu có peer cũ, hủy đi để nhận cuộc gọi mới
    if (peerRef.current) peerRef.current.destroy();

    const peer = new SimplePeer({
      initiator: false,
      trickle: false,
      stream: stream,
      config: rtcConfig // <--- THÊM CẤU HÌNH STUN SERVER
    });

    peer.on('signal', (signal) => {
      if (signal.type === 'answer') {
          stompClientRef.current?.publish({
            destination: '/app/signal',
            body: JSON.stringify({
              type: 'answer',
              data: signal,
              targetSessionId: senderSessionId,
              senderSessionId: mySessionId.current
            })
          });
      }
    });

    peer.signal(incomingSignal); // Nạp Offer vào
    setupPeerEvents(peer);       // Cài đặt các log sự kiện chung
    peerRef.current = peer;
  };

  // --- HÀM HELPER: LẮNG NGHE SỰ KIỆN P2P ---
  const setupPeerEvents = (peer: SimplePeer.Instance) => {
    peer.on('stream', (stream) => {
      console.log(">>> ĐÃ NHẬN ĐƯỢC REMOTE STREAM! <<<");
      console.log("Remote stream tracks:", stream.getTracks());
      setRemoteStream(stream);
      setIsRemoteConnected(true);
      addToast("Kết nối video thành công!", "success"); // ← THÊM
      console.log("Remote stream saved to state, UI will re-render");
    });

    peer.on('connect', () => {
      console.log(">>> KẾT NỐI P2P THÀNH CÔNG! (Status: Connected) <<<");
    });

    peer.on('close', () => {
      console.log(">>> KẾT NỐI P2P ĐÃ ĐÓNG <<<");
      addToast("Người kia đã ngắt kết nối", "warning");
      cleanupRemoteConnection(); // ← ĐÃ CÓ
    });

    peer.on('error', (err) => {
      console.error(">>> LỖI P2P:", err);
      addToast("Lỗi kết nối Video (P2P): " + err.message, "error"); // <--- THÊM
      cleanupRemoteConnection();
      // Tùy chọn: Có thể thử gọi lại hoặc yêu cầu reload
    });
  };

  useEffect(() => {
    if (isRemoteConnected && remoteVideoRef.current && remoteStream) {
      console.log("Gắn remote stream vào video element...");
      remoteVideoRef.current.srcObject = remoteStream;
      
      // Debug events
      remoteVideoRef.current.onloadedmetadata = () => {
        console.log("Remote video metadata loaded!");
      };
      
      remoteVideoRef.current.onplay = () => {
        console.log("Remote video started playing!");
      };
      
      console.log("✅ Remote stream attached successfully!");
    }
  }, [isRemoteConnected, remoteStream]); // Chạy lại khi 2 biến này thay đổi

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
          <div className="room-id">
            📹 {roomId}
          </div>
    
          {/* --- BADGE TRẠNG THÁI --- */}
          <div className={`connection-badge status-${connectionStatus}`}>
            <div className="connection-dot"></div>
            <span>
              {connectionStatus === 'connected' && "Online"}
              {connectionStatus === 'reconnecting' && "Reconnecting..."}
              {connectionStatus === 'disconnected' && "Offline"}
            </span>
          </div>

          <div className="participant-count">
            👥 {participantCount} participant{participantCount > 1 ? 's' : ''}
          </div>
        </div>
        
        <button 
          className="btn-secondary" 
          onClick={() => navigator.clipboard.writeText(`${window.location.origin}/room/${roomId}`)}
          style={{ padding: '0.5rem 1rem', fontSize: '0.9rem' }}
        >
          📋 Copy Link
        </button>
      </div>

      {/* Video Grid */}
      <div className={`video-grid ${participantCount === 1 ? 'single-participant' : 'two-participants'}`}>
        {/* REMOTE VIDEO - Luôn hiển thị slot này */}
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
                style={{ 
                  width: '100%', 
                  height: '100%', 
                  objectFit: 'cover',
                  transform: 'scaleX(-1)'
                }}
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

        {/* LOCAL VIDEO - Hiển thị khác nhau tùy trạng thái */}
        {isRemoteConnected ? (
          // Picture-in-Picture khi có remote user
          <div 
            className={`video-card local-video ${isLocalVideoMinimized ? 'minimized' : ''}`}
            onClick={() => setIsLocalVideoMinimized(!isLocalVideoMinimized)}
          >
            {isCameraOff && (
              <div className="camera-off-overlay">
                <div className="icon">📷</div>
                <div className="text">Camera Off</div>
              </div>
            )}
          
            <video 
              ref={localVideoRef} 
              autoPlay 
              muted 
              playsInline 
              style={{ 
                width: '100%', 
                height: '100%', 
                objectFit: 'cover', 
                opacity: isCameraOff ? 0 : 1, 
                transform: 'scaleX(-1)' 
              }} 
            />
            
            <div className="participant-overlay">
              <div className="participant-name">You</div>
              <div className="participant-status">
                <div className={`status-indicator ${isMuted ? 'muted' : ''}`}>
                  {isMuted ? '🔇' : '🎤'}
                </div>
                <div className={`status-indicator ${isCameraOff ? 'camera-off' : ''}`}>
                  {isCameraOff ? '🚫' : '📹'}
                </div>
              </div>
            </div>
          </div>
        ) : (
          // Full size khi chưa có remote user
          <div className="video-card">
            {isCameraOff && (
              <div className="camera-off-overlay">
                <div className="icon">📷</div>
                <div className="text">Your Camera is Off</div>
              </div>
            )}
            
            <video 
              ref={localVideoRef} 
              autoPlay 
              muted 
              playsInline 
              style={{ 
                width: '100%', 
                height: '100%', 
                objectFit: 'cover', 
                opacity: isCameraOff ? 0 : 1, 
                transform: 'scaleX(-1)' 
              }} 
            />

            <div className="participant-overlay">
              <div className="participant-name">You (Waiting for others)</div>
              <div className="participant-status">
                <div className={`status-indicator ${isMuted ? 'muted' : ''}`}>
                  {isMuted ? '🔇' : '🎤'}
                </div>
                <div className={`status-indicator ${isCameraOff ? 'camera-off' : ''}`}>
                  {isCameraOff ? '🚫' : '📹'}
                </div>
              </div>
            </div>
          </div>
        )}
        
        <div className="toast-container">
          {toasts.map(toast => (
            <div key={toast.id} className={`toast ${toast.type}`}>
              {/* Icon tương ứng */}
              {toast.type === 'success' && '✅'}
              {toast.type === 'error' && '❌'}
              {toast.type === 'warning' && '⚠️'}
              {toast.type === 'info' && 'ℹ️'}
              <span>{toast.message}</span>
            </div>
          ))}
        </div>
      </div>
      {/* Controls */}
      <div className="controls-bar">
        <button 
          className={`control-btn ${isMuted ? 'off' : ''}`} 
          onClick={toggleMic}
        >
          {isMuted ? "🔇" : "🎤"}
          <div className="control-tooltip">
            {isMuted ? 'Unmute' : 'Mute'}
          </div>
        </button>
        
        <button 
          className={`control-btn ${isCameraOff ? 'off' : ''}`} 
          onClick={toggleCamera}
        >
          {isCameraOff ? "🚫" : "📹"}
          <div className="control-tooltip">
            {isCameraOff ? 'Turn on camera' : 'Turn off camera'}
          </div>
        </button>
        
        <button className="control-btn btn-hangup" onClick={handleHangUp}>
          📞
          <div className="control-tooltip">Leave call</div>
        </button>
      </div>
    </div>
  );
}