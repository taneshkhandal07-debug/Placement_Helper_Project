import { apiClient } from '@/shared/api/client';
import {
  CrtEligibility,
  CrtStudentDashboard,
  CrtSession,
  CrtBatch,
  CrtAttendanceRecord,
  PendingEnrollment,
  PendingCorrection,
} from '../types/crt.types';

export const crtApi = {
  // Student
  getEligibility: async (): Promise<CrtEligibility> => {
    const res = await apiClient.get<CrtEligibility>('/crt/eligibility');
    return res.data;
  },

  optIn: async (notes: string, preferredBatchId?: number): Promise<{ message: string; enrollmentId: number }> => {
    const res = await apiClient.post('/crt/opt-in', { optInNotes: notes, preferredBatchId });
    return res.data;
  },

  getDashboard: async (): Promise<CrtStudentDashboard | any> => {
    const res = await apiClient.get('/crt/dashboard');
    return res.data;
  },

  requestCorrection: async (recordId: number, requestedStatus: string, reason: string): Promise<any> => {
    const res = await apiClient.post('/crt/corrections', {
      recordId,
      requestedStatus,
      reason,
    });
    return res.data;
  },

  // Shared / Coordinator
  getActiveBatches: async (): Promise<CrtBatch[]> => {
    const res = await apiClient.get<CrtBatch[]>('/crt/batches');
    return res.data;
  },

  getBatchSessions: async (batchId: number): Promise<CrtSession[]> => {
    const res = await apiClient.get<CrtSession[]>(`/crt/batches/${batchId}/sessions`);
    return res.data;
  },

  createSession: async (batchId: number, trackId: number, sessionData: any): Promise<any> => {
    const res = await apiClient.post(`/crt/batches/${batchId}/sessions?trackId=${trackId}`, sessionData);
    return res.data;
  },

  getSessionAttendance: async (sessionId: number): Promise<CrtAttendanceRecord[]> => {
    const res = await apiClient.get<CrtAttendanceRecord[]>(`/crt/sessions/${sessionId}/attendance`);
    return res.data;
  },

  markSessionAttendance: async (sessionId: number, records: any[]): Promise<any> => {
    const res = await apiClient.post(`/crt/sessions/${sessionId}/attendance`, records);
    return res.data;
  },

  // Coordinator reviews
  getPendingEnrollments: async (): Promise<PendingEnrollment[]> => {
    const res = await apiClient.get<PendingEnrollment[]>('/crt/enrollments/pending');
    return res.data;
  },

  verifyEnrollment: async (enrollmentId: number, action: 'APPROVE' | 'REJECT', assignedBatchId?: number, remarks?: string): Promise<any> => {
    const res = await apiClient.post(`/crt/enrollments/${enrollmentId}/verify`, {
      action,
      assignedBatchId,
      remarks,
    });
    return res.data;
  },

  getPendingCorrections: async (): Promise<PendingCorrection[]> => {
    const res = await apiClient.get<PendingCorrection[]>('/crt/corrections/pending');
    return res.data;
  },

  reviewCorrection: async (correctionId: number, approve: boolean, remarks: string): Promise<any> => {
    const res = await apiClient.post(`/crt/corrections/${correctionId}/review`, {
      approve,
      remarks,
    });
    return res.data;
  },

  recordScore: async (assessmentId: number, studentId: number, marksObtained: number, remarks: string): Promise<any> => {
    const res = await apiClient.post(`/crt/assessments/${assessmentId}/scores`, {
      studentId,
      marksObtained,
      remarks,
    });
    return res.data;
  },
};
