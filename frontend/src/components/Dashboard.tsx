import { useState, useEffect } from 'react';
import { 
  Layout, Card, Button, List, Badge, Statistic, 
  Row, Col, message, Typography, Space, Tag, Modal, Progress 
} from 'antd';
import { 
  ClockCircleOutlined, TeamOutlined, ThunderboltOutlined, 
  BellOutlined, UserOutlined, LogoutOutlined, ReloadOutlined 
} from '@ant-design/icons';
import { queueService } from '../services/queueService';
import { userService } from '../services/userService';
import { QueueInfo, Ticket, EtaResponse } from '../types/index';

const { Header, Content } = Layout;
const { Title, Text } = Typography;

interface DashboardProps {
  user: any;
  onLogout: () => void;
}

const Dashboard: React.FC<DashboardProps> = ({ user, onLogout }) => {
  const [queues, setQueues] = useState<QueueInfo[]>([]);
  const [myTickets, setMyTickets] = useState<Ticket[]>([]);
  const [loading, setLoading] = useState(false);
  const [joining, setJoining] = useState<string | null>(null);

  const currentUserId = user?.userId || userService.getCurrentUserId();
  const currentUserEmail = localStorage.getItem('userEmail');

  useEffect(() => {
    if (!currentUserId) {
      onLogout();
      return;
    }
    loadQueues();
    loadMyTickets();
  }, [currentUserId, onLogout]);

  const loadQueues = async () => {
    setLoading(true);
    try {
      const queueData = await queueService.getQueues();
      setQueues(queueData);
    } catch (error) {
      message.error('Failed to load queues');
    } finally {
      setLoading(false);
    }
  };

  const loadMyTickets = async () => {
    // In a real app, you'd have an API to get user's tickets
    // For now, we'll simulate this
    const tickets: Ticket[] = [];
    setMyTickets(tickets);
  };

  const joinQueue = async (queueId: string) => {
    if (!currentUserId) return;
    
    setJoining(queueId);
    try {
      const ticket = await queueService.joinQueue(queueId, currentUserId);
      
      // Get ETA
      const etaResponse = await queueService.getETA(queueId, ticket.ticketId, ticket.position);
      
      message.success(`Successfully joined queue! Position: ${ticket.position}`);
      
      // Show ticket details
      showTicketModal(ticket, etaResponse);
      
      setMyTickets(prev => [...prev, ticket]);
      
    } catch (error: any) {
      message.error(error.response?.data?.message || 'Failed to join queue');
    } finally {
      setJoining(null);
    }
  };

  const showTicketModal = (ticket: Ticket, eta: EtaResponse) => {
    Modal.info({
      title: '🎫 Your Queue Ticket',
      width: 500,
      content: (
        <div style={{ padding: '20px 0' }}>
          <Card>
            <Statistic 
              title="Your Position" 
              value={ticket.position} 
              prefix={<TeamOutlined />}
              suffix="in line"
            />
            <br />
            <Statistic 
              title="🧠 Smart ETA" 
              value={eta.estimatedWaitMinutes} 
              prefix={<ClockCircleOutlined />}
              suffix="minutes"
              valueStyle={{ color: '#1890ff' }}
            />
            <br />
            <Text type="secondary">
              🎯 Ticket ID: {ticket.ticketId}
              <br />
              📧 Notifications enabled for {currentUserEmail}
            </Text>
            
            <div style={{ marginTop: '20px' }}>
              <Progress 
                percent={Math.max(5, 100 - (ticket.position * 10))} 
                status="active"
                strokeColor={{
                  '0%': '#108ee9',
                  '100%': '#87d068',
                }}
              />
              <Text style={{ fontSize: '12px', color: '#666' }}>
                Queue progress estimation
              </Text>
            </div>
          </Card>
        </div>
      ),
    });
  };

  const logout = () => {
    userService.logout();
    onLogout();
  };

  const getQueueStatusColor = (waitingCount: number) => {
    if (waitingCount <= 5) return 'green';
    if (waitingCount <= 15) return 'orange';
    return 'red';
  };

  return (
    <Layout style={{ minHeight: '100vh', background: '#f0f2f5' }}>
      <Header style={{ 
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        padding: '0 24px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between'
      }}>
        <div>
          <Title level={3} style={{ color: 'white', margin: 0 }}>
            🎯 SmartQueue
          </Title>
        </div>
        <Space>
          <Text style={{ color: 'white' }}>
            <UserOutlined /> {currentUserEmail}
          </Text>
          <Button 
            icon={<ReloadOutlined />} 
            onClick={loadQueues}
            style={{ border: 'none', background: 'rgba(255,255,255,0.2)', color: 'white' }}
          >
            Refresh
          </Button>
          <Button 
            icon={<LogoutOutlined />} 
            onClick={logout}
            style={{ border: 'none', background: 'rgba(255,255,255,0.2)', color: 'white' }}
          >
            Logout
          </Button>
        </Space>
      </Header>
      
      <Content style={{ padding: '24px' }}>
        <Row gutter={[24, 24]}>
          {/* Available Queues */}
          <Col xs={24} lg={16}>
            <Card 
              title={
                <Space>
                  <TeamOutlined />
                  Available Queues
                  <Badge count={queues.length} style={{ backgroundColor: '#52c41a' }} />
                </Space>
              }
              loading={loading}
            >
              <List
                dataSource={queues}
                renderItem={(queue) => (
                  <List.Item
                    actions={[
                      <Button
                        type="primary"
                        key="join"
                        loading={joining === queue.queueId}
                        onClick={() => joinQueue(queue.queueId)}
                        icon={<ThunderboltOutlined />}
                        style={{
                          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                          border: 'none'
                        }}
                      >
                        Join Queue
                      </Button>
                    ]}
                  >
                    <List.Item.Meta
                      title={
                        <Space>
                          {queue.name}
                          <Tag color={getQueueStatusColor(queue.currentWaitingCount)}>
                            {queue.currentWaitingCount} waiting
                          </Tag>
                        </Space>
                      }
                      description={
                        <Space direction="vertical" size="small">
                          <Text>{queue.description}</Text>
                          <Space>
                            <Tag icon={<ClockCircleOutlined />} color="blue">
                              ~{queue.averageServiceTimeMinutes} min avg
                            </Tag>
                            <Tag color={queue.isActive ? 'green' : 'red'}>
                              {queue.isActive ? 'Active' : 'Inactive'}
                            </Tag>
                          </Space>
                        </Space>
                      }
                    />
                  </List.Item>
                )}
              />
            </Card>
          </Col>
          
          {/* My Tickets */}
          <Col xs={24} lg={8}>
            <Card 
              title={
                <Space>
                  <BellOutlined />
                  My Tickets
                  <Badge count={myTickets.length} />
                </Space>
              }
            >
              {myTickets.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '40px 0', color: '#999' }}>
                  <BellOutlined style={{ fontSize: '48px', marginBottom: '16px' }} />
                  <br />
                  No active tickets
                  <br />
                  <Text type="secondary">Join a queue to get started!</Text>
                </div>
              ) : (
                <List
                  dataSource={myTickets}
                  renderItem={(ticket) => (
                    <List.Item>
                      <List.Item.Meta
                        title={`Position ${ticket.position}`}
                        description={
                          <Space direction="vertical" size="small">
                            <Tag color="blue">{ticket.status}</Tag>
                            <Text type="secondary">
                              ETA: ~{ticket.estimatedWaitMinutes} min
                            </Text>
                          </Space>
                        }
                      />
                    </List.Item>
                  )}
                />
              )}
            </Card>
            
            {/* Quick Stats */}
            <Card title="📊 Smart Queue Stats" style={{ marginTop: '16px' }}>
              <Row gutter={16}>
                <Col span={12}>
                  <Statistic
                    title="Avg Wait Time"
                    value={8.5}
                    suffix="min"
                    valueStyle={{ color: '#3f8600' }}
                    prefix={<ClockCircleOutlined />}
                  />
                </Col>
                <Col span={12}>
                  <Statistic
                    title="Served Today"
                    value={245}
                    valueStyle={{ color: '#1890ff' }}
                    prefix={<TeamOutlined />}
                  />
                </Col>
              </Row>
              
              <div style={{ marginTop: '16px', padding: '12px', background: '#f0f2f5', borderRadius: '8px' }}>
                <Text style={{ fontSize: '12px', color: '#666' }}>
                  🧠 Powered by Smart ETA Algorithm
                  <br />
                  ☁️ Multi-cloud: AWS + Aliyun
                </Text>
              </div>
            </Card>
          </Col>
        </Row>
      </Content>
    </Layout>
  );
};

export default Dashboard;
