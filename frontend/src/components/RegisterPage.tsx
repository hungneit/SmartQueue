import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Form, Input, Button, Card, Switch, message, Typography, Space } from 'antd';
import { UserOutlined, MailOutlined, PhoneOutlined, LockOutlined } from '@ant-design/icons';
import { userService } from '../services/userService';
import { CreateUserRequest } from '../types';

const { Title, Text } = Typography;

const RegisterPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();
  const navigate = useNavigate();

  const onFinish = async (values: CreateUserRequest) => {
    setLoading(true);
    try {
      const user = await userService.register(values);
      message.success(`Welcome ${user.name}! Registration successful.`);
      
      // Save user info and redirect to dashboard
      userService.setUserInfo(user.userId, user.email);
      localStorage.setItem('currentUser', JSON.stringify(user));
      
      navigate('/dashboard');
    } catch (error: any) {
      message.error(error.response?.data?.message || 'Registration failed');
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
          maxWidth: '500px',
          borderRadius: '16px',
          boxShadow: '0 20px 40px rgba(0,0,0,0.1)'
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: '30px' }}>
          <Title level={2} style={{ color: '#667eea', margin: 0 }}>
            🎯 SmartQueue
          </Title>
          <Text type="secondary">Create your account</Text>
        </div>

        <Form
          form={form}
          name="register"
          onFinish={onFinish}
          layout="vertical"
          initialValues={{
            emailNotificationEnabled: true,
            smsNotificationEnabled: false
          }}
        >
          <Form.Item
            name="name"
            label="Full Name"
            rules={[
              { required: true, message: 'Please enter your name' },
              { min: 2, message: 'Name must be at least 2 characters' }
            ]}
          >
            <Input 
              prefix={<UserOutlined />} 
              placeholder="Enter your full name"
              size="large"
            />
          </Form.Item>

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
            name="phone"
            label="Phone Number"
            rules={[
              { required: true, message: 'Please enter your phone number' },
              { pattern: /^\+?[1-9]\d{1,14}$/, message: 'Please enter a valid phone number' }
            ]}
          >
            <Input 
              prefix={<PhoneOutlined />} 
              placeholder="+1234567890"
              size="large"
            />
          </Form.Item>

          <Form.Item
            name="password"
            label="Password"
            rules={[
              { required: true, message: 'Please enter your password' },
              { min: 8, message: 'Password must be at least 8 characters' },
              { 
                pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).*$/, 
                message: 'Password must contain uppercase, lowercase and number' 
              }
            ]}
          >
            <Input.Password 
              prefix={<LockOutlined />} 
              placeholder="Enter your password"
              size="large"
            />
          </Form.Item>

          <Form.Item
            name="confirmPassword"
            label="Confirm Password"
            dependencies={['password']}
            rules={[
              { required: true, message: 'Please confirm your password' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('password') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject('Passwords do not match');
                }
              })
            ]}
          >
            <Input.Password 
              prefix={<LockOutlined />} 
              placeholder="Confirm your password"
              size="large"
            />
          </Form.Item>

          <Card size="small" style={{ background: '#f8f9fa', marginBottom: '20px' }}>
            <Title level={5} style={{ margin: '0 0 16px 0' }}>📱 Notification Preferences</Title>
            
            <Form.Item 
              name="emailNotificationEnabled" 
              valuePropName="checked"
              style={{ marginBottom: '12px' }}
            >
              <Space>
                <Switch />
                <Text>Email notifications</Text>
              </Space>
            </Form.Item>

            <Form.Item 
              name="smsNotificationEnabled" 
              valuePropName="checked"
              style={{ marginBottom: 0 }}
            >
              <Space>
                <Switch />
                <Text>SMS notifications</Text>
              </Space>
            </Form.Item>
          </Card>

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
              Create Account
            </Button>
          </Form.Item>

          <div style={{ textAlign: 'center' }}>
            <Text type="secondary">
              Already have an account? <Button type="link" onClick={() => navigate('/login')}>Sign in here</Button>
            </Text>
          </div>
        </Form>
      </Card>
    </div>
  );
};

export default RegisterPage;