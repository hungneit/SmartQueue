import { useState, useEffect } from 'react';
import { 
  Card, Statistic, Row, Col, List, Tag, Space, Typography, 
  Button, Progress, Spin, Empty 
} from 'antd';
import { 
  ClockCircleOutlined, TeamOutlined, ThunderboltOutlined,
  ArrowLeftOutlined, ReloadOutlined
} from '@ant-design/icons';
import { queueService } from '../services/queueService';
import { QueueInfo, Ticket } from '../types';
import { useInterval } from '../hooks/useInterval';

const { Title, Text } = Typography;

interface QueueDetailPageProps {
  queueId: string;
  onBack: () => void;
}

const QueueDetailPage: React.FC<QueueDetailPageProps> = ({ queueId, onBack }) => {
  const [queue, setQueue] = useState<QueueInfo | null>(null);
  const [activeTickets, setActiveTickets] = useState<Ticket[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadQueueDetails();
  }, [queueId]);

  // Poll for updates every 5 seconds
  useInterval(() => {
    loadQueueDetails();
  }, 5000);

  const loadQueueDetails = async () => {
    try {
      const queueData = await queueService.getQueue(queueId);
      setQueue(queueData);
      
      // In a real app, you'd fetch actual tickets from backend
      // For now, show mock data
      setActiveTickets([]);
    } catch (error) {
      console.error('Failed to load queue details:', error);
    } finally {
      setLoading(false);
    }
  };

  const getQueueHealthColor = (waitingCount: number) => {
    if (waitingCount < 5) return '#52c41a';
    if (waitingCount < 10) return '#faad14';
    return '#f5222d';
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '100px 20px' }}>
        <Spin size="large" />
        <br /><br />
        <Text>Loading queue details...</Text>
      </div>
    );
  }

  if (!queue) {
    return (
      <Empty 
        description="Queue not found"
        style={{ padding: '100px 20px' }}
      >
        <Button onClick={onBack}>Go Back</Button>
      </Empty>
    );
  }

  return (
    <div style={{ padding: '24px', maxWidth: '1200px', margin: '0 auto' }}>
      {/* Header */}
      <Space style={{ marginBottom: '24px' }}>
        <Button 
          icon={<ArrowLeftOutlined />} 
          onClick={onBack}
        >
          Back
        </Button>
        <Title level={2} style={{ margin: 0 }}>
          {queue.name}
        </Title>
        <Tag color={queue.isActive ? 'green' : 'red'}>
          {queue.isActive ? 'Active' : 'Inactive'}
        </Tag>
      </Space>

      {/* Quick Stats */}
      <Row gutter={16} style={{ marginBottom: '24px' }}>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="Currently Waiting"
              value={queue.currentWaitingCount || 0}
              valueStyle={{ color: getQueueHealthColor(queue.currentWaitingCount || 0) }}
              prefix={<TeamOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="Average Wait Time"
              value={queue.averageServiceTimeMinutes || 5}
              suffix="min"
              valueStyle={{ color: '#1890ff' }}
              prefix={<ClockCircleOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="Queue Capacity"
              value={`${queue.currentWaitingCount || 0}/${queue.maxCapacity || 100}`}
              prefix={<ThunderboltOutlined />}
            />
            <Progress 
              percent={Math.min(100, ((queue.currentWaitingCount || 0) / (queue.maxCapacity || 100)) * 100)}
              strokeColor={getQueueHealthColor(queue.currentWaitingCount || 0)}
              showInfo={false}
              style={{ marginTop: '12px' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Description */}
      {queue.description && (
        <Card style={{ marginBottom: '24px' }}>
          <Title level={4}>About This Queue</Title>
          <Text>{queue.description}</Text>
        </Card>
      )}

      {/* Current Waiting List */}
      <Card 
        title={
          <Space>
            <TeamOutlined />
            Current Waiting List
            <Button 
              icon={<ReloadOutlined />}
              size="small"
              type="text"
              onClick={loadQueueDetails}
            >
              Refresh
            </Button>
          </Space>
        }
      >
        {activeTickets.length === 0 ? (
          <Empty 
            description="No one in queue yet"
            style={{ padding: '40px 0' }}
          />
        ) : (
          <List
            dataSource={activeTickets}
            renderItem={(ticket, index) => (
              <List.Item>
                <List.Item.Meta
                  avatar={
                    <div style={{
                      width: '40px',
                      height: '40px',
                      borderRadius: '50%',
                      background: '#1890ff',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: 'white',
                      fontWeight: 'bold'
                    }}>
                      #{index + 1}
                    </div>
                  }
                  title={`Ticket ${ticket.ticketId?.substring(0, 8)}...`}
                  description={
                    <Space>
                      <Tag color="blue">{ticket.status}</Tag>
                      <Text type="secondary">
                        Joined: {new Date(ticket.joinedAt).toLocaleTimeString()}
                      </Text>
                    </Space>
                  }
                />
              </List.Item>
            )}
          />
        )}
      </Card>

      {/* Real-time indicator */}
      <div style={{ textAlign: 'center', marginTop: '16px' }}>
        <Text type="secondary" style={{ fontSize: '12px' }}>
          🔄 Auto-updating every 5 seconds
        </Text>
      </div>
    </div>
  );
};

export default QueueDetailPage;
