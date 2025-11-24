import { useEffect, useState } from 'react';
import { Modal, Card, Statistic, Progress, Space, Tag, Button, Typography, Divider, Alert } from 'antd';
import { 
  ClockCircleOutlined, TeamOutlined, ThunderboltOutlined, 
  QrcodeOutlined, ReloadOutlined
} from '@ant-design/icons';
import { queueService } from '../services/queueService';
import { Ticket, EtaResponse } from '../types';

const { Title, Text } = Typography;

interface TicketDetailModalProps {
  visible: boolean;
  ticket: Ticket | null;
  onClose: () => void;
}

const TicketDetailModal: React.FC<TicketDetailModalProps> = ({ visible, ticket, onClose }) => {
  const [loading, setLoading] = useState(false);
  const [currentTicket, setCurrentTicket] = useState<Ticket | null>(ticket);
  const [eta, setEta] = useState<EtaResponse | null>(null);
  const [refreshInterval, setRefreshInterval] = useState<number>(0);
  const [lastNotifiedPosition, setLastNotifiedPosition] = useState<number | null>(null);

  useEffect(() => {
    if (visible && ticket) {
      setCurrentTicket(ticket);
      loadTicketStatus();
      
      // Auto-refresh every 10 seconds
      const interval = setInterval(() => {
        loadTicketStatus();
        setRefreshInterval(prev => prev + 1);
      }, 10000);

      return () => clearInterval(interval);
    }
  }, [visible, ticket]);

  // Show browser notification when position changes
  useEffect(() => {
    if (currentTicket?.position && currentTicket.position <= 3) {
      // Only notify once per position
      if (lastNotifiedPosition !== currentTicket.position) {
        showBrowserNotification(currentTicket.position);
        setLastNotifiedPosition(currentTicket.position);
      }
    }
  }, [currentTicket?.position]);

  const showBrowserNotification = (position: number) => {
    if ('Notification' in window && Notification.permission === 'granted') {
      const notification = new Notification('🔔 SmartQueue - Your Turn is Coming!', {
        body: `You are now position ${position} in the queue. Please get ready!`,
        icon: '/logo.png', // Add your logo
        badge: '/logo.png',
        tag: 'smartqueue-position',
        requireInteraction: position === 1, // Keep notification visible if position is 1
      });

      // Play sound
      const audio = new Audio('/notification.mp3'); // Add notification sound
      audio.play().catch(err => console.log('Audio play failed:', err));

      notification.onclick = () => {
        window.focus();
        notification.close();
      };
    }
  };

  const loadTicketStatus = async () => {
    if (!ticket) return;
    
    setLoading(true);
    try {
      // Get updated ticket status
      const updatedTicket = await queueService.getQueueStatus(ticket.queueId, ticket.ticketId);
      setCurrentTicket(updatedTicket);

      // Get updated ETA
      if (updatedTicket.position && updatedTicket.position > 0) {
        const etaResponse = await queueService.getETA(
          ticket.queueId, 
          ticket.ticketId, 
          updatedTicket.position
        );
        setEta(etaResponse);
      }
    } catch (error) {
      console.error('Failed to load ticket status:', error);
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'WAITING': return 'blue';
      case 'CALLED': return 'orange';
      case 'SERVING': return 'green';
      case 'COMPLETED': return 'default';
      case 'CANCELLED': return 'red';
      default: return 'default';
    }
  };

  const getProgressPercent = () => {
    if (!currentTicket || !currentTicket.position) return 0;
    const totalAhead = currentTicket.position - 1;
    if (totalAhead === 0) return 100;
    return Math.max(0, 100 - (currentTicket.position * 10));
  };

  const shouldNotify = currentTicket?.position && currentTicket.position <= 3;

  return (
    <Modal
      title={null}
      open={visible}
      onCancel={onClose}
      footer={null}
      width={600}
      centered
    >
      {currentTicket && (
        <div style={{ padding: '20px 0' }}>
          {/* Header */}
          <div style={{ textAlign: 'center', marginBottom: '24px' }}>
            <Title level={3} style={{ margin: 0 }}>
              🎫 Your Queue Ticket
            </Title>
            <Text type="secondary">Ticket ID: {currentTicket.ticketId?.substring(0, 8)}...</Text>
          </div>

          {/* Alert for near turn */}
          {shouldNotify && (
            <Alert
              message="⏰ Your turn is coming soon!"
              description="Please be ready. You'll be called shortly."
              type="warning"
              showIcon
              style={{ marginBottom: '16px' }}
            />
          )}

          {/* Main Stats */}
          <Card>
            <Space direction="vertical" size="large" style={{ width: '100%' }}>
              {/* Position */}
              <div style={{ textAlign: 'center' }}>
                <Statistic 
                  title={
                    <Space>
                      <TeamOutlined />
                      Your Position in Queue
                    </Space>
                  }
                  value={currentTicket.position || 0}
                  valueStyle={{ 
                    fontSize: '48px', 
                    fontWeight: 'bold',
                    color: shouldNotify ? '#fa8c16' : '#1890ff'
                  }}
                />
                <Progress 
                  percent={getProgressPercent()} 
                  strokeColor={{
                    '0%': '#108ee9',
                    '100%': '#87d068',
                  }}
                  showInfo={false}
                  style={{ marginTop: '8px' }}
                />
              </div>

              <Divider />

              {/* ETA Information */}
              {eta && (
                <>
                  <div style={{ textAlign: 'center' }}>
                    <Space size="large">
                      <Statistic 
                        title={
                          <Space>
                            <ClockCircleOutlined />
                            🧠 Smart ETA
                          </Space>
                        }
                        value={eta.estimatedWaitMinutes} 
                        suffix="min"
                        valueStyle={{ color: '#3f8600' }}
                      />
                      <Statistic 
                        title="P90 Wait Time" 
                        value={eta.p90WaitMinutes} 
                        suffix="min"
                        valueStyle={{ color: '#cf1322' }}
                      />
                    </Space>
                  </div>

                  <div style={{ 
                    padding: '12px', 
                    background: '#f0f2f5', 
                    borderRadius: '8px',
                    textAlign: 'center'
                  }}>
                    <Text style={{ fontSize: '13px' }}>
                      <ThunderboltOutlined /> Updated by smart algorithm
                      <br />
                      <Text type="secondary" style={{ fontSize: '12px' }}>
                        Last updated: {new Date().toLocaleTimeString()}
                      </Text>
                    </Text>
                  </div>
                </>
              )}

              <Divider />

              {/* Status */}
              <div style={{ textAlign: 'center' }}>
                <Space direction="vertical">
                  <Text strong>Status:</Text>
                  <Tag 
                    color={getStatusColor(currentTicket.status)} 
                    style={{ fontSize: '16px', padding: '8px 16px' }}
                  >
                    {currentTicket.status}
                  </Tag>
                </Space>
              </div>

              {/* QR Code Placeholder */}
              <div style={{ 
                textAlign: 'center', 
                padding: '20px',
                background: '#fafafa',
                borderRadius: '8px'
              }}>
                <QrcodeOutlined style={{ fontSize: '80px', color: '#666' }} />
                <br />
                <Text type="secondary">Scan to track your ticket</Text>
              </div>

              {/* Refresh Button */}
              <Button 
                icon={<ReloadOutlined spin={loading} />}
                onClick={loadTicketStatus}
                loading={loading}
                block
                size="large"
              >
                Refresh Status
              </Button>

              {/* Auto-refresh indicator */}
              <Text type="secondary" style={{ textAlign: 'center', display: 'block', fontSize: '12px' }}>
                🔄 Auto-refreshing every 10 seconds
                {refreshInterval > 0 && ` (${refreshInterval} updates)`}
              </Text>
            </Space>
          </Card>

          {/* Footer Info */}
          <div style={{ marginTop: '16px', textAlign: 'center' }}>
            <Text type="secondary" style={{ fontSize: '12px' }}>
              💡 Keep this window open to receive real-time updates
              <br />
              You'll be notified when your turn is near
            </Text>
          </div>
        </div>
      )}
    </Modal>
  );
};

export default TicketDetailModal;
