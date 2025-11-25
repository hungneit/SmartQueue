import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Layout, Card, Button, List, Badge, Statistic, 
  Row, Col, message, Typography, Space, Tag 
} from 'antd';
import { 
  ClockCircleOutlined, TeamOutlined, ThunderboltOutlined, 
  BellOutlined, UserOutlined, LogoutOutlined, ReloadOutlined 
} from '@ant-design/icons';
import { queueService } from '../services/queueService';
import { userService } from '../services/userService';
import { QueueInfo, Ticket } from '../types/index';
import TicketDetailModal from './TicketDetailModal';

const { Header, Content } = Layout;
const { Title, Text } = Typography;

const Dashboard: React.FC = () => {
  const [queues, setQueues] = useState<QueueInfo[]>([]);
  const [myTickets, setMyTickets] = useState<Ticket[]>([]);
  const [loading, setLoading] = useState(false);
  const [joining, setJoining] = useState<string | null>(null);
  const [selectedTicket, setSelectedTicket] = useState<Ticket | null>(null);
  const [showTicketModal, setShowTicketModal] = useState(false);
  const navigate = useNavigate();

  const currentUserId = userService.getCurrentUserId();
  const currentUserEmail = localStorage.getItem('userEmail');

  console.log('🔍 Dashboard userId:', currentUserId);
  console.log('🔍 Dashboard email:', currentUserEmail);

  const logout = () => {
    localStorage.removeItem('currentUser');
    localStorage.removeItem('userId');
    localStorage.removeItem('userEmail');
    message.info('👋 Logged out successfully');
    navigate('/login');
  };

  useEffect(() => {
    if (!currentUserId) {
      logout();
      return;
    }
    loadQueues();
    loadMyTickets();
  }, [currentUserId]);

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
    // Always try to load from backend first to ensure sync
    try {
      if (!currentUserId) {
        console.log('No userId, skipping loadMyTickets');
        setMyTickets([]);
        return;
      }

      console.log('Loading tickets from backend for userId:', currentUserId);
      const backendTickets = await queueService.getUserTickets(currentUserId);
      
      // Filter to only show WAITING tickets
      const activeTickets = backendTickets.filter(t => t.status === 'WAITING');
      setMyTickets(activeTickets);
      
      // Update localStorage with fresh data from backend
      localStorage.setItem('myTickets', JSON.stringify(activeTickets));
      console.log('Loaded and synced tickets from backend:', activeTickets);
      
    } catch (error) {
      console.error('Error loading tickets from backend:', error);
      
      // Fallback to localStorage if backend fails
      try {
        const storedTickets = localStorage.getItem('myTickets');
        if (storedTickets) {
          const tickets = JSON.parse(storedTickets) as Ticket[];
          const activeTickets = tickets.filter(t => t.status === 'WAITING');
          setMyTickets(activeTickets);
          console.log('Fallback: Loaded tickets from localStorage:', activeTickets);
        } else {
          setMyTickets([]);
          console.log('No tickets in localStorage');
        }
      } catch (storageError) {
        console.error('Error reading localStorage:', storageError);
        setMyTickets([]);
      }
    }
  };

  // Check if user already has a ticket in a specific queue
  const hasTicketInQueue = (queueId: string): boolean => {
    return myTickets.some(ticket => ticket.queueId === queueId && ticket.status === 'WAITING');
  };

  const joinQueue = async (queueId: string) => {
    if (!currentUserId) {
      message.error('❌ User ID not found! Please login again.');
      logout();
      return;
    }
    
    // Check if user already joined this queue
    if (hasTicketInQueue(queueId)) {
      message.warning('You already have an active ticket in this queue!');
      return;
    }
    
    setJoining(queueId);
    try {
      console.log(`📤 Joining queue with userId: ${currentUserId}, queueId: ${queueId}`);
      const ticket = await queueService.joinQueue(queueId, currentUserId);
      
      message.success(`✅ Successfully joined queue! Position: ${ticket.position}`);
      
      // Show ticket details in modal
      setSelectedTicket(ticket);
      setShowTicketModal(true);
      
      // Reload tickets from backend to ensure sync (backend will persist correctly)
      // DO NOT save manually to localStorage - just reload from BE
      await loadMyTickets();
      
    } catch (error: any) {
      console.error('❌ Join queue error:', error);
      message.error(error.response?.data?.message || 'Failed to join queue');
    } finally {
      setJoining(null);
    }
  };

  const getQueueStatusColor = (waitingCount: number) => {
    if (waitingCount <= 5) return 'green';
    if (waitingCount <= 15) return 'orange';
    return 'red';
  };

  return (
    <Layout style={{ minHeight: '100vh', background: 'transparent' }}>
      <Header style={{ 
        background: 'linear-gradient(135deg, #16a34a 0%, #15803d 100%)',
        padding: '0 24px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: '16px',
        boxShadow: '0 4px 15px rgba(22, 163, 74, 0.3)'
      }}>
        <div style={{ flex: '0 0 auto' }}>
          <Title level={3} style={{ color: 'white', margin: 0 }}>
            📋 SmartQueue
          </Title>
        </div>
        <Space wrap>
          <Text style={{ color: 'white' }}>
            <UserOutlined /> {currentUserEmail}
          </Text>
          <Button 
            icon={<ReloadOutlined />} 
            onClick={loadQueues}
            style={{ border: 'none', background: 'rgba(255,255,255,0.15)', color: 'white' }}
          >
            Refresh
          </Button>
          <Button 
            onClick={() => navigate('/admin')}
            style={{ border: 'none', background: 'rgba(255,255,255,0.15)', color: 'white' }}
          >
            🎛️ Admin Panel
          </Button>
          <Button 
            icon={<LogoutOutlined />} 
            onClick={logout}
            style={{ border: 'none', background: 'rgba(255,255,255,0.15)', color: 'white' }}
          >
            Logout
          </Button>
        </Space>
      </Header>
      
      <Content style={{ 
        padding: '24px', 
        minHeight: 'calc(100vh - 64px)',
        background: 'transparent'
      }}>
        <div style={{ maxWidth: 1400, margin: '0 auto' }}>
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
                        disabled={hasTicketInQueue(queue.queueId)}
                        icon={<ThunderboltOutlined />}
                        title={hasTicketInQueue(queue.queueId) ? 'You already have an active ticket in this queue' : 'Join this queue'}
                        style={{
                          background: hasTicketInQueue(queue.queueId) 
                            ? '#9ca3af' 
                            : 'linear-gradient(135deg, #16a34a 0%, #15803d 100%)',
                          border: 'none',
                          color: 'white'
                        }}
                      >
                        {hasTicketInQueue(queue.queueId) ? '✓ Already Joined' : 'Join Queue'}
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
                    <List.Item
                      actions={[
                        <Button 
                          type="link" 
                          onClick={() => {
                            setSelectedTicket(ticket);
                            setShowTicketModal(true);
                          }}
                        >
                          View Details
                        </Button>
                      ]}
                    >
                      <List.Item.Meta
                        title={`Position ${ticket.position}`}
                        description={
                          <Space direction="vertical" size="small">
                            <Tag color="blue">{ticket.status}</Tag>
                            <Text type="secondary">
                              Ticket ID: {ticket.ticketId?.substring(0, 8)}...
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
        </div>
      </Content>

      {/* Ticket Detail Modal */}
      <TicketDetailModal
        visible={showTicketModal}
        ticket={selectedTicket}
        onClose={() => setShowTicketModal(false)}
      />
    </Layout>
  );
};

export default Dashboard;
