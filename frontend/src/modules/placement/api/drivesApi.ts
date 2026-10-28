import { apiClient } from '@/shared/api/client';
import { UpcomingDrive, RegisterDriveRequest, RegisterDriveResponse } from '../types/drives.types';

export const drivesApi = {
  async getUpcomingDrives(): Promise<UpcomingDrive[]> {
    const response = await apiClient.get<UpcomingDrive[]>('/drives/upcoming');
    return response.data;
  },

  async getDriveDetail(id: number): Promise<UpcomingDrive> {
    const response = await apiClient.get<UpcomingDrive>(`/drives/upcoming/${id}`);
    return response.data;
  },

  async register(id: number, request: RegisterDriveRequest): Promise<RegisterDriveResponse> {
    const response = await apiClient.post<RegisterDriveResponse>(`/drives/upcoming/${id}/register`, request);
    return response.data;
  },
};
