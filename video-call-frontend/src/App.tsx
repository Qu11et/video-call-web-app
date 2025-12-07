import React from 'react';
import { Routes, Route } from 'react-router-dom';
import LandingPage from './pages/LandingPage';
import P2PCallPage from './pages/P2PCallPage';     // File cũ đã đổi tên
import GroupCallPage from './pages/GroupCallPage'; // File mới placeholder

function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      
      {/* Route cho gọi 1-1 (Code cũ) */}
      <Route path="/room/p2p/:roomId" element={<P2PCallPage />} />
      
      {/* Route cho gọi nhóm (Code mới - SFU) */}
      <Route path="/room/group/:roomId" element={<GroupCallPage />} />
    </Routes>
  );
}

export default App;
