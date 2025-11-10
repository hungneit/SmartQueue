import api, { etaApi } from './api';
import { JoinQueueRequest, Ticket, QueueInfo, EtaResponse } from '../types';

export const queueService = {
  // Get all available queues
  async getQueues(): Promise<QueueInfo[]> {
    const response = await api.get('/api/v1/queues');
    return response.data;
  },

  // Get specific queue info
  async getQueue(queueId: string): Promise<QueueInfo> {
    const response = await api.get(`/api/v1/queues/${queueId}`);
    return response.data;
  },

  // Join a queue
  async joinQueue(queueId: string, userId: string): Promise<Ticket> {
    const joinRequest: JoinQueueRequest = { userId };
    const response = await api.post(`/api/v1/queues/${queueId}/join`, joinRequest);
    return response.data;
  },

  // Get queue status for a ticket
  async getQueueStatus(queueId: string, ticketId: string): Promise<Ticket> {
    const response = await api.get(`/api/v1/queues/${queueId}/status/${ticketId}`);
    return response.data;
  },

  // Get ETA from Aliyun service
  async getETA(queueId: string, ticketId: string, position: number): Promise<EtaResponse> {
    const response = await etaApi.get('/eta', {
      params: { queueId, ticketId, position }
    });
    return response.data;
  },

  // Process next in queue (admin function)
  async processNext(queueId: string, count: number = 1): Promise<any> {
    const response = await api.post(`/api/v1/queues/${queueId}/next`, { count });
    return response.data;
  },

  // Create a new queue (admin function)
  async createQueue(queueData: Partial<QueueInfo>): Promise<QueueInfo> {
    const response = await api.post('/api/v1/queues', queueData);
    return response.data;
  }
};