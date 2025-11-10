import { useState } from 'react';
import { Form, Input, Button, Card, message, Typography, Space } from 'antd';
import { MailOutlined, LockOutlined } from '@ant-design/icons';
import { userService } from '../services/userService';
import { LoginRequest } from '../types/user';

const { Title, Text } = Typography;

interface LoginPageProps {
  onLoginSuccess: (user: any) => void;
  onSwitchToRegister: () => void;
}

const LoginPage: React.FC<LoginPageProps> = ({ onLoginSuccess, onSwitchToRegister }) => {
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();

  const onFinish = async (values: LoginRequest) => {
    setLoading(true);
    try {
      await userService.login(values);
      
      // Mock user data since login just returns success message
      const mockUser = {
        userId: 'mock-user-id',
        email: values.email,
        name: 'User'
      };
      
      localStorage.setItem('currentUser', JSON.stringify(mockUser));
      message.success('🎉 Login successful!');
      onLoginSuccess(mockUser);
    } catch (error: any) {
      message.error(error.message || '❌ Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ 
      minHeight: '100vh', 
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '20px'
    }}>
      <Card 
        style={{ 
          width: '100%', 
          maxWidth: '400px',
          borderRadius: '16px',
          boxShadow: '0 20px 40px rgba(0,0,0,0.1)'
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: '30px' }}>
          <Title level={2} style={{ color: '#667eea', margin: 0 }}>
            🎯 SmartQueue
          </Title>
          <Text type="secondary">Sign in to your account</Text>
        </div>

        <Form
          form={form}
          name="login"
          onFinish={onFinish}
          layout="vertical"
        >
          <Form.Item
            name="email"
            label="Email Address"
            rules={[
              { required: true, message: 'Please enter your email' },
              { type: 'email', message: 'Please enter a valid email' }
            ]}
          >
            <Input 
              prefix={<MailOutlined />} 
              placeholder="Enter your email"
              size="large"
            />
          </Form.Item>

          <Form.Item
            name="password"
            label="Password"
            rules={[
              { required: true, message: 'Please enter your password' }
            ]}
          >
            <Input.Password 
              prefix={<LockOutlined />} 
              placeholder="Enter your password"
              size="large"
            />
          </Form.Item>

          <Form.Item>
            <Button 
              type="primary" 
              htmlType="submit" 
              loading={loading}
              size="large"
              block
              style={{ 
                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                border: 'none',
                height: '50px',
                fontSize: '16px',
                fontWeight: 'bold'
              }}
            >
              Sign In
            </Button>
          </Form.Item>

          <div style={{ textAlign: 'center' }}>
            <Space>
              <Text type="secondary">Don't have an account?</Text>
              <Button type="link" onClick={onSwitchToRegister}>
                Sign up now
              </Button>
            </Space>
          </div>
        </Form>
      </Card>
    </div>
  );
};

export default LoginPage;