/// <reference types="vite/client" />

// User types
export interface User {
  userId: string;
  name: string;
  email: string;
  phone: string;
  emailNotificationEnabled: boolean;
  smsNotificationEnabled: boolean;
  createdAt: string;
  lastLoginAt: string;
  isActive: boolean;
}

export interface CreateUserRequest {
  name: string;
  email: string;
  phone: string;
  password: string;
  emailNotificationEnabled?: boolean;
  smsNotificationEnabled?: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

// Queue types
export interface JoinQueueRequest {
  userId: string;
}

export interface Ticket {
  ticketId: string;
  queueId: string;
  userId: string;
  userEmail: string;
  userName: string;
  userPhone: string;
  status: 'WAITING' | 'SERVED' | 'CANCELLED' | 'EXPIRED' | 'NOTIFIED';
  position: number;
  joinedAt: string;
  estimatedWaitMinutes: number;
}

export interface QueueInfo {
  queueId: string;
  name: string;
  description: string;
  currentWaitingCount: number;
  averageServiceTimeMinutes: number;
  maxCapacity?: number;
  isActive: boolean;
}

export interface EtaResponse {
  queueId: string;
  ticketId: string;
  estimatedWaitMinutes: number;
  p90WaitMinutes: number;
  p50WaitMinutes: number;
  serviceRate: number;
  updatedAt: string;
}

// API Response types
export interface ApiResponse<T> {
  data: T;
  message: string;
  success: boolean;
}