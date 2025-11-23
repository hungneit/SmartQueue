import React, { useState, useEffect } from 'react';
import { 
  Card, Button, Table, Modal, Form, Input, InputNumber, Switch, 
  message, Space, Tag, Popconfirm 
} from 'antd';
import { 
  PlusOutlined, EditOutlined, DeleteOutlined, 
  CheckCircleOutlined, CloseCircleOutlined 
} from '@ant-design/icons';
import { queueService } from '../services/queueService';
import { QueueInfo } from '../types';

interface QueueManagementProps {
  onRefresh?: () => void;
}

const QueueManagement: React.FC<QueueManagementProps> = ({ onRefresh }) => {
  const [queues, setQueues] = useState<QueueInfo[]>([]);
  const [loading, setLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [editingQueue, setEditingQueue] = useState<QueueInfo | null>(null);
  const [form] = Form.useForm();

  const loadQueues = async () => {
    setLoading(true);
    try {
      const data = await queueService.getQueues();
      setQueues(data);
    } catch (error: any) {
      message.error('Failed to load queues: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadQueues();
  }, []);

  const handleCreate = () => {
    setEditingQueue(null);
    form.resetFields();
    form.setFieldsValue({ isActive: true, maxCapacity: 100, openSlots: 100 });
    setShowModal(true);
  };

  const handleEdit = (queue: QueueInfo) => {
    setEditingQueue(queue);
    form.setFieldsValue({
      queueId: queue.queueId,
      name: queue.name,
      maxCapacity: queue.maxCapacity,
      openSlots: queue.currentWaitingCount || 0,
      isActive: queue.isActive,
    });
    setShowModal(true);
  };

  const handleDelete = async (queueId: string) => {
    try {
      await queueService.deleteQueue(queueId);
      message.success('Queue deleted successfully');
      loadQueues();
      onRefresh?.();
    } catch (error: any) {
      message.error('Failed to delete: ' + (error.response?.data?.error || error.message));
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      
      if (editingQueue) {
        // Update existing queue
        await queueService.updateQueue(editingQueue.queueId, {
          queueName: values.name,
          maxCapacity: values.maxCapacity,
          openSlots: values.openSlots,
          isActive: values.isActive,
        });
        message.success('Queue updated successfully');
      } else {
        // Create new queue
        await queueService.createQueue({
          queueId: values.queueId,
          queueName: values.name,
          maxCapacity: values.maxCapacity,
          openSlots: values.openSlots,
          isActive: values.isActive,
        });
        message.success('Queue created successfully');
      }
      
      setShowModal(false);
      loadQueues();
      onRefresh?.();
    } catch (error: any) {
      message.error('Failed to save: ' + (error.response?.data?.error || error.message));
    }
  };

  const columns = [
    {
      title: 'Queue ID',
      dataIndex: 'queueId',
      key: 'queueId',
      render: (text: string) => <code style={{ fontSize: 12 }}>{text}</code>,
    },
    {
      title: 'Name',
      dataIndex: 'name',
      key: 'name',
      render: (text: string) => <strong>{text}</strong>,
    },
    {
      title: 'Status',
      dataIndex: 'isActive',
      key: 'isActive',
      render: (isActive: boolean) => (
        <Tag icon={isActive ? <CheckCircleOutlined /> : <CloseCircleOutlined />} 
             color={isActive ? 'green' : 'red'}>
          {isActive ? 'Active' : 'Inactive'}
        </Tag>
      ),
    },
    {
      title: 'Capacity',
      key: 'capacity',
      render: (_: any, record: QueueInfo) => {
        const waiting = record.currentWaitingCount || 0;
        const max = record.maxCapacity || 100;
        const percentage = Math.round((waiting / max) * 100);
        return (
          <span>
            {waiting} / {max} ({percentage}%)
          </span>
        );
      },
    },
    {
      title: 'Waiting',
      dataIndex: 'currentWaitingCount',
      key: 'currentWaitingCount',
      render: (count: number) => (
        <Tag color={count > 20 ? 'red' : count > 10 ? 'orange' : 'green'}>
          {count || 0}
        </Tag>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_: any, record: QueueInfo) => (
        <Space>
          <Button 
            icon={<EditOutlined />} 
            size="small" 
            onClick={() => handleEdit(record)}
          >
            Edit
          </Button>
          <Popconfirm
            title="Delete Queue"
            description={`Are you sure to delete "${record.name}"?`}
            onConfirm={() => handleDelete(record.queueId)}
            okText="Yes"
            cancelText="No"
            okButtonProps={{ danger: true }}
          >
            <Button 
              icon={<DeleteOutlined />} 
              size="small" 
              danger
              disabled={(record.currentWaitingCount || 0) > 0}
            >
              Delete
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <Card 
      title="📋 Queue Management"
      extra={
        <Space>
          <Button onClick={loadQueues} loading={loading}>
            Refresh
          </Button>
          <Button 
            type="primary" 
            icon={<PlusOutlined />} 
            onClick={handleCreate}
          >
            Create Queue
          </Button>
        </Space>
      }
    >
      <Table
        columns={columns}
        dataSource={queues}
        rowKey="queueId"
        loading={loading}
        pagination={{ pageSize: 10 }}
      />

      <Modal
        title={editingQueue ? 'Edit Queue' : 'Create New Queue'}
        open={showModal}
        onOk={handleSubmit}
        onCancel={() => setShowModal(false)}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          style={{ marginTop: 16 }}
        >
          <Form.Item
            label="Queue ID"
            name="queueId"
            rules={[{ required: true, message: 'Please input queue ID!' }]}
          >
            <Input 
              placeholder="e.g., hospital-queue-3" 
              disabled={!!editingQueue}
            />
          </Form.Item>

          <Form.Item
            label="Queue Name"
            name="name"
            rules={[{ required: true, message: 'Please input queue name!' }]}
          >
            <Input placeholder="e.g., Dental Clinic" />
          </Form.Item>

          <Form.Item
            label="Max Capacity"
            name="maxCapacity"
            rules={[{ required: true, message: 'Please input max capacity!' }]}
          >
            <InputNumber min={1} max={1000} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            label="Open Slots"
            name="openSlots"
            rules={[{ required: true, message: 'Please input open slots!' }]}
          >
            <InputNumber min={0} max={1000} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            label="Active Status"
            name="isActive"
            valuePropName="checked"
          >
            <Switch 
              checkedChildren="Active" 
              unCheckedChildren="Inactive"
            />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default QueueManagement;
