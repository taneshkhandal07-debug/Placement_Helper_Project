import { apiClient } from '@/shared/api/client';
import {
  IntegrationOverview,
  ScheduleConflict,
  PlacementParticipation,
  DutyExemption,
  ParticipationVerifyRequest,
} from '../types/integration.types';

export const integrationApi = {
  getOverview: async (): Promise<IntegrationOverview> => {
    const res = await apiClient.get<IntegrationOverview>('/placement-integration/overview');
    return res.data;
  },

  getConflicts: async (driveId?: number, studentId?: number, facultyId?: number): Promise<ScheduleConflict[]> => {
    const params: Record<string, any> = {};
    if (driveId) params.driveId = driveId;
    if (studentId) params.studentId = studentId;
    if (facultyId) params.facultyId = facultyId;

    const res = await apiClient.get<ScheduleConflict[]>('/placement-integration/conflicts', { params });
    return res.data;
  },

  detectConflicts: async (driveId: number): Promise<{ message: string; totalDetected: number; conflicts: ScheduleConflict[] }> => {
    const res = await apiClient.post(`/placement-integration/conflicts/detect/${driveId}`);
    return res.data;
  },

  notifyConflictsForDrive: async (driveId: number, forceResend: boolean = false): Promise<{ message: string; notificationsSent: number }> => {
    const res = await apiClient.post(`/placement-integration/conflicts/notify-all/${driveId}?forceResend=${forceResend}`);
    return res.data;
  },

  notifySingleConflict: async (conflictId: number): Promise<{ success: boolean }> => {
    const res = await apiClient.post(`/placement-integration/conflicts/${conflictId}/notify`);
    return res.data;
  },

  getParticipationsByDrive: async (driveId: number): Promise<PlacementParticipation[]> => {
    const res = await apiClient.get<PlacementParticipation[]>(`/placement-integration/participations/${driveId}`);
    return res.data;
  },

  getMyParticipations: async (): Promise<PlacementParticipation[]> => {
    const res = await apiClient.get<PlacementParticipation[]>('/placement-integration/my-participations');
    return res.data;
  },

  getMyExemptions: async (): Promise<DutyExemption[]> => {
    const res = await apiClient.get<DutyExemption[]>('/placement-integration/my-exemptions');
    return res.data;
  },

  verifyParticipation: async (request: ParticipationVerifyRequest): Promise<PlacementParticipation> => {
    const res = await apiClient.post<PlacementParticipation>('/placement-integration/participations/verify', request);
    return res.data;
  },

  getAllExemptions: async (): Promise<DutyExemption[]> => {
    const res = await apiClient.get<DutyExemption[]>('/placement-integration/exemptions');
    return res.data;
  },

  applyExemption: async (participationId: number, remarks?: string): Promise<{ message: string; exemptions: DutyExemption[] }> => {
    const res = await apiClient.post('/placement-integration/exemptions/apply', { participationId, remarks });
    return res.data;
  },

  revokeExemption: async (exemptionId: number, reason?: string): Promise<DutyExemption> => {
    const res = await apiClient.post<DutyExemption>(`/placement-integration/exemptions/${exemptionId}/revoke`, { reason });
    return res.data;
  },

  handleDriveLifecycle: async (driveId: number, action: 'RESCHEDULED' | 'CANCELLED', reason?: string): Promise<any> => {
    const res = await apiClient.post(`/placement-integration/drives/${driveId}/lifecycle`, { action, reason });
    return res.data;
  },
};
