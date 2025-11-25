import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Button, Card, message, Typography, Space } from 'antd';
import { MailOutlined, LockOutlined } from '@ant-design/icons';
import { userService } from '../services/userService';
import { LoginRequest } from '../types/user';

const { Title, Text } = Typography;

const LoginPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const [form] = Form.useForm();

  const onFinish = async (values: LoginRequest) => {
    setLoading(true);
    try {
      await userService.login(values);
      message.success('🎉 Login successful!');
      navigate('/dashboard');
    } catch (error: any) {
      message.error(error.response?.data?.message || '❌ Login failed');
    } finally {
      setLoading(false);
    }
  };


  return (
    <div style={{ 
      minHeight: '100vh', 
      background: 'linear-gradient(135deg, #16a34a 0%, #0f2818 100%)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '20px',
      backgroundImage: `
        repeating-linear-gradient(
          0deg,
          transparent,
          transparent 39px,
          rgba(255, 255, 255, 0.03) 39px,
          rgba(255, 255, 255, 0.03) 40px
        ),
        repeating-linear-gradient(
          90deg,
          transparent,
          transparent 39px,
          rgba(255, 255, 255, 0.03) 39px,
          rgba(255, 255, 255, 0.03) 40px
        ),
        linear-gradient(135deg, #16a34a 0%, #0f2818 100%)
      `,
      backgroundAttachment: 'fixed'
    }}>
      <Card 
        style={{ 
          width: '100%', 
          maxWidth: '400px',
          borderRadius: '16px',
          boxShadow: '0 20px 40px rgba(22, 163, 74, 0.2)',
          border: '1px solid rgba(22, 163, 74, 0.3)'
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: '30px' }}>
          <Title level={2} style={{ color: '#16a34a', margin: 0 }}>
            📋 SmartQueue
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
                background: 'linear-gradient(135deg, #16a34a 0%, #15803d 100%)',
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
              <Button type="link" onClick={() => navigate('/register')} style={{ color: '#16a34a' }}>
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
