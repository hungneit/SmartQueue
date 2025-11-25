import { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider, message } from 'antd';
import RegisterPage from './components/RegisterPage';
import LoginPage from './components/LoginPage';
import Dashboard from './components/Dashboard';
import AdminPanel from './components/AdminPanel';
import { userService } from './services/userService';
import './App.css';

function App() {
  useEffect(() => {
    // Request notification permission on app load
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission();
    }
  }, []);

  const isAuthenticated = () => {
    return !!userService.getCurrentUserId();
  };

  const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
    if (!isAuthenticated()) {
      message.warning('Please login first');
      return <Navigate to="/login" replace />;
    }
    return <>{children}</>;
  };

  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: '#667eea',
          borderRadius: 8,
        },
      }}
    >
      <Router>
        <div className="App">
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route 
              path="/dashboard" 
              element={
                <ProtectedRoute>
                  <Dashboard />
                </ProtectedRoute>
              } 
            />
            <Route 
              path="/admin" 
              element={
                <ProtectedRoute>
                  <AdminPanel />
                </ProtectedRoute>
              } 
            />
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="*" element={<Navigate to="/login" replace />} />
          </Routes>
        </div>
      </Router>
    </ConfigProvider>
  );
}

export default App;