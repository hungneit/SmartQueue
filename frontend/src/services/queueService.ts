import api from './api';
import { JoinQueueRequest, Ticket, QueueInfo, EtaResponse } from '../types';

export const queueService = {
  // Get all available queues
  async getQueues(): Promise<QueueInfo[]> {
    const response = await api.get('/queues');
    // Backend returns array of objects with queueId, queueName, etc
    return response.data.map((q: any) => {
      // Backend should return waitingCount directly
      // If not, fall back to calculation
      const currentWaitingCount = q.waitingCount ?? 0;
      
      return {
        queueId: q.queueId,
        name: q.queueName || q.name,
        description: q.description || 'No description',
        isActive: q.isActive ?? true,
        currentWaitingCount: currentWaitingCount,
        averageServiceTimeMinutes: 5,
        maxCapacity: q.maxCapacity || 100
      };
    });
  },

  // Get specific queue info
  async getQueue(queueId: string): Promise<QueueInfo> {
    const response = await api.get(`/queues/${queueId}`);
    return response.data;
  },

  // Join a queue
  async joinQueue(queueId: string, userId: string): Promise<Ticket> {
    const joinRequest: JoinQueueRequest = { userId };
    const response = await api.post(`/queues/${queueId}/join`, joinRequest);
    return response.data;
  },

  // Get queue status for a ticket
  async getQueueStatus(queueId: string, ticketId: string): Promise<Ticket> {
    const response = await api.get(`/queues/${queueId}/status`, { params: { ticketId } });
    return response.data;
  },

  // Get ETA - AWS service calls Aliyun internally, FE just calls AWS
  async getETA(queueId: string, ticketId: string, position: number): Promise<EtaResponse> {
    const response = await api.get('/eta', {
      params: { queueId, ticketId, position }
    });
    return response.data;
  },

  // Process next in queue (admin function)
  async processNext(queueId: string, count: number = 1): Promise<any> {
    const response = await api.post(`/queues/${queueId}/next`, { count });
    return response.data;
  },

  // Create a new queue (admin function)
  async createQueue(queueData: any): Promise<any> {
    const response = await api.post('/queues', queueData);
    return response.data;
  },

  // Update queue (admin function)
  async updateQueue(queueId: string, queueData: any): Promise<any> {
    const response = await api.put(`/queues/${queueId}`, queueData);
    return response.data;
  },

  // Delete queue (admin function)
  async deleteQueue(queueId: string): Promise<any> {
    const response = await api.delete(`/queues/${queueId}`);
    return response.data;
  },

  // Get queue detail
  async getQueueDetail(queueId: string): Promise<QueueInfo> {
    const response = await api.get(`/queues/${queueId}`);
    return response.data;
  },

  // Get user's tickets from backend (for syncing after page reload)
  async getUserTickets(userId: string): Promise<Ticket[]> {
    try {
      const response = await api.get(`/queues/tickets/${userId}`);
      return response.data.map((t: any) => ({
        ticketId: t.ticketId,
        queueId: t.queueId,
        status: t.status,
        position: t.position,
        userId: t.userId,
        joinedAt: t.joinedAt
      }));
    } catch (error) {
      console.error('Error fetching user tickets from backend:', error);
      return [];
    }
  }
};
