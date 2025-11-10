import { useState, useEffect } from 'react';
import { ConfigProvider, message } from 'antd';
import RegisterPage from './components/RegisterPage';
import LoginPage from './components/LoginPage';
import Dashboard from './components/Dashboard';
import './App.css';

type AppState = 'login' | 'register' | 'dashboard';

function App() {
  const [currentView, setCurrentView] = useState<AppState>('login');
  const [currentUser, setCurrentUser] = useState<any>(null);

  useEffect(() => {
    // Check if user is already logged in
    const savedUser = localStorage.getItem('currentUser');
    if (savedUser) {
      try {
        const user = JSON.parse(savedUser);
        setCurrentUser(user);
        setCurrentView('dashboard');
      } catch (error) {
        console.error('Failed to parse saved user:', error);
        localStorage.removeItem('currentUser');
      }
    }
  }, []);

  const handleLoginSuccess = (user: any) => {
    setCurrentUser(user);
    setCurrentView('dashboard');
  };

  const handleRegisterSuccess = (user: any) => {
    setCurrentUser(user);
    setCurrentView('dashboard');
    message.success('🎉 Account created successfully!');
  };

  const handleLogout = () => {
    localStorage.removeItem('currentUser');
    setCurrentUser(null);
    setCurrentView('login');
    message.info('👋 Logged out successfully');
  };

  const renderCurrentView = () => {
    switch (currentView) {
      case 'register':
        return (
          <RegisterPage
            onRegisterSuccess={handleRegisterSuccess}
            onSwitchToLogin={() => setCurrentView('login')}
          />
        );
      case 'dashboard':
        return currentUser ? (
          <Dashboard
            user={currentUser}
            onLogout={handleLogout}
          />
        ) : null;
      case 'login':
      default:
        return (
          <LoginPage
            onLoginSuccess={handleLoginSuccess}
            onSwitchToRegister={() => setCurrentView('register')}
          />
        );
    }
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
      <div className="App">
        {renderCurrentView()}
      </div>
    </ConfigProvider>
  );
}

export default App;