import api from './api';
import { CreateUserRequest, LoginRequest, User } from '../types';

export const userService = {
  // Register new user
  async register(userData: CreateUserRequest): Promise<User> {
    const response = await api.post('/users/register', userData);
    return response.data;
  },

  // Login user
  async login(credentials: LoginRequest): Promise<string> {
    const response = await api.post('/users/login', credentials);
    const token = response.data;

    // Store token for future requests
    localStorage.setItem('authToken', token);

    return token;
  },

  // Get user profile
  async getProfile(userId: string): Promise<User> {
    const response = await api.get(`/users/${userId}`);
    return response.data;
  },

  // Update notification preferences
  async updateNotificationPreferences(
    userId: string,
    emailEnabled: boolean,
    smsEnabled: boolean
  ): Promise<User> {
    const response = await api.put(
      `/users/${userId}/notifications`,
      null,
      {
        params: { emailEnabled, smsEnabled }
      }
    );
    return response.data;
  },

  // Logout
  logout(): void {
    localStorage.removeItem('authToken');
    localStorage.removeItem('userId');
    localStorage.removeItem('userEmail');
  },

  // Get stored user info
  getCurrentUserId(): string | null {
    return localStorage.getItem('userId');
  },

  // Set user info after login
  setUserInfo(userId: string, email: string): void {
    localStorage.setItem('userId', userId);
    localStorage.setItem('userEmail', email);
  }
};
