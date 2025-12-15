import React, { useEffect } from 'react';
import { Routes, Route } from 'react-router-dom';
import { useDispatch } from 'react-redux'; // Import
import type { AppDispatch } from './store/store';
import { verifyTokenAsync } from './store/authSlice';
import LandingPage from './pages/LandingPage';
import P2PCallPage from './pages/P2PCallPage';     // File cũ đã đổi tên
import GroupCallPage from './pages/GroupCallPage'; // File mới placeholder
import RegisterPage from './pages/RegisterPage';
import LoginPage from './pages/LoginPage';
import NotFoundPage from './pages/NotFoundPage';

function App() {
  const dispatch = useDispatch<AppDispatch>();

  // --- AUTO LOGIN CHECK ---
  useEffect(() => {
    dispatch(verifyTokenAsync());
  }, [dispatch]);

  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/register" element={<RegisterPage />} /> {/* <--- Route mới */}
      
      <Route path="/login" element={<LoginPage />} />

      <Route path="/room/p2p/:roomId" element={<P2PCallPage />} />
      <Route path="/room/group/:roomId" element={<GroupCallPage />} />

      {/* Catch-all route - PHẢI ĐẶT CUỐI CÙNG */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}

export default App;
