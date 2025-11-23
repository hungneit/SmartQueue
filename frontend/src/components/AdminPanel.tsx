import React, { useState, useEffect } from 'react';
import { Card, Button, Table, Statistic, Row, Col, message, Modal, Space, Tag, Tabs } from 'antd';
import { 
  UserOutlined, 
  ClockCircleOutlined, 
  CheckCircleOutlined,
  ThunderboltOutlined,
  ReloadOutlined,
  ArrowLeftOutlined,
  SettingOutlined,
  TeamOutlined
} from '@ant-design/icons';
import { queueService } from '../services/queueService';
import { useInterval } from '../hooks/useInterval';
import QueueManagement from './QueueManagement';

interface AdminPanelProps {
  onBack?: () => void;
}

interface QueueStats {
  queueId: string;
  queueName: string;
  waitingCount: number;
  isActive: boolean;
  maxCapacity: number;
}

const AdminPanel: React.FC<AdminPanelProps> = ({ onBack }) => {
  const [queues, setQueues] = useState<QueueStats[]>([]);
  const [loading, setLoading] = useState(false);
  const [processing, setProcessing] = useState<string | null>(null);

  const loadQueues = async () => {
    try {
      const queueList = await queueService.getQueues();
      const stats: QueueStats[] = queueList.map(q => ({
        queueId: q.queueId,
        queueName: q.name,
        waitingCount: q.currentWaitingCount || 0,
        isActive: q.isActive ?? true,
        maxCapacity: q.maxCapacity || 100
      }));
      setQueues(stats);
    } catch (error) {
      console.error('Failed to load queues:', error);
    }
  };

  useEffect(() => {
    loadQueues();
  }, []);

  // Auto-refresh every 3 seconds
  useInterval(() => {
    loadQueues();
  }, 3000);

  const handleProcessNext = async (queueId: string, count: number = 1) => {
    setProcessing(queueId);
    try {
      const response = await queueService.processNext(queueId, count);
      
      if (response.processedCount > 0) {
        message.success(`✅ Đã xử lý ${response.processedCount} khách hàng!`);
      } else {
        message.info('Hàng đợi trống');
      }
      
      // Reload data
      await loadQueues();
    } catch (error: any) {
      console.error('Process next error:', error);
      message.error('⚠️ Lỗi backend: Service đang dùng DynamoDB thay vì InMemory. ' +
        'Cần restart với USE_IN_MEMORY=true. Chi tiết: ' + (error.response?.data?.message || error.message));
    } finally {
      setProcessing(null);
    }
  };

  const handleProcessAll = (queueId: string) => {
    Modal.confirm({
      title: 'Xử lý hết hàng đợi?',
      content: 'Bạn có chắc muốn xử lý TẤT CẢ khách hàng đang chờ?',
      okText: 'Xử lý hết',
      cancelText: 'Hủy',
      okButtonProps: { danger: true },
      onOk: async () => {
        const queue = queues.find(q => q.queueId === queueId);
        if (queue && queue.waitingCount > 0) {
          await handleProcessNext(queueId, queue.waitingCount);
        }
      }
    });
  };

  const columns = [
    {
      title: 'Hàng đợi',
      dataIndex: 'queueName',
      key: 'queueName',
      render: (text: string, record: QueueStats) => (
        <Space>
          <span style={{ fontWeight: 'bold' }}>{text}</span>
          {record.isActive ? (
            <Tag color="green">Hoạt động</Tag>
          ) : (
            <Tag color="red">Đóng</Tag>
          )}
        </Space>
      )
    },
    {
      title: 'Đang chờ',
      dataIndex: 'waitingCount',
      key: 'waitingCount',
      render: (count: number) => (
        <Tag color={count > 10 ? 'red' : count > 5 ? 'orange' : 'green'} style={{ fontSize: 16 }}>
          <UserOutlined /> {count}
        </Tag>
      )
    },
    {
      title: 'Sức chứa',
      dataIndex: 'maxCapacity',
      key: 'maxCapacity',
      render: (max: number, record: QueueStats) => {
        const percentage = Math.round((record.waitingCount / max) * 100);
        return `${record.waitingCount}/${max} (${percentage}%)`;
      }
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_: any, record: QueueStats) => (
        <Space>
          <Button
            type="primary"
            icon={<ThunderboltOutlined />}
            loading={processing === record.queueId}
            disabled={record.waitingCount === 0}
            onClick={() => handleProcessNext(record.queueId, 1)}
          >
            Gọi 1 khách
          </Button>
          <Button
            icon={<ThunderboltOutlined />}
            loading={processing === record.queueId}
            disabled={record.waitingCount < 3}
            onClick={() => handleProcessNext(record.queueId, 3)}
          >
            Gọi 3 khách
          </Button>
          <Button
            danger
            loading={processing === record.queueId}
            disabled={record.waitingCount === 0}
            onClick={() => handleProcessAll(record.queueId)}
          >
            Xử lý hết
          </Button>
        </Space>
      )
    }
  ];

  const totalWaiting = queues.reduce((sum, q) => sum + q.waitingCount, 0);
  const activeQueues = queues.filter(q => q.isActive).length;

  return (
    <div style={{ padding: 24 }}>
      <div style={{ marginBottom: 24, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <h1>🎛️ Admin Panel - Quản lý hàng đợi</h1>
          <p style={{ color: '#666' }}>
            Quản lý queues và xử lý khách hàng. Hệ thống tự động refresh mỗi 3 giây.
          </p>
        </div>
        {onBack && (
          <Button icon={<ArrowLeftOutlined />} onClick={onBack} size="large">
            Back to Dashboard
          </Button>
        )}
      </div>

      {/* Statistics */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="Tổng đang chờ"
              value={totalWaiting}
              prefix={<UserOutlined />}
              valueStyle={{ color: totalWaiting > 20 ? '#cf1322' : '#3f8600' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Hàng đợi hoạt động"
              value={activeQueues}
              suffix={`/ ${queues.length}`}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Trạng thái"
              value="Hoạt động"
              prefix={<ClockCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Button
              block
              size="large"
              icon={<ReloadOutlined />}
              loading={loading}
              onClick={() => {
                setLoading(true);
                loadQueues().finally(() => setLoading(false));
              }}
            >
              Làm mới
            </Button>
          </Card>
        </Col>
      </Row>

      {/* Tabs for different admin functions */}
      <Tabs 
        defaultActiveKey="process"
        items={[
          {
            key: 'process',
            label: (
              <span>
                <TeamOutlined /> Process Customers
              </span>
            ),
            children: (
              <>
                <Card
                  title="📊 Danh sách hàng đợi"
                  extra={<Tag color="blue">Auto-refresh: 3s</Tag>}
                >
                  <Table
                    columns={columns}
                    dataSource={queues}
                    rowKey="queueId"
                    pagination={false}
                    loading={loading}
                  />
                </Card>

                {/* Instructions */}
                <Card title="💡 Hướng dẫn" style={{ marginTop: 16 }}>
                  <ul>
                    <li><strong>Gọi 1 khách:</strong> Xử lý 1 khách hàng tiếp theo, position của mọi người giảm 1</li>
                    <li><strong>Gọi 3 khách:</strong> Xử lý 3 khách hàng cùng lúc (phục vụ nhanh giờ cao điểm)</li>
                    <li><strong>Xử lý hết:</strong> Xóa toàn bộ hàng đợi (dùng khi kết thúc ca)</li>
                    <li><strong>Auto-refresh:</strong> Bảng tự động cập nhật mỗi 3 giây</li>
                  </ul>
                  <p style={{ marginTop: 16, color: '#666', fontStyle: 'italic' }}>
                    💡 Mẹo: Mở Dashboard ở tab khác để xem real-time position update khi bạn nhấn "Gọi khách"
                  </p>
                </Card>
              </>
            ),
          },
          {
            key: 'management',
            label: (
              <span>
                <SettingOutlined /> Queue Management
              </span>
            ),
            children: <QueueManagement onRefresh={loadQueues} />,
          },
        ]}
      />
    </div>
  );
};

export default AdminPanel;
