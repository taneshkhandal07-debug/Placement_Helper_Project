import { apiClient } from '@/shared/api/client';
import {
  StudentAcademicProfile,
  EnrolledSubject,
  TimetableSlot,
  FacultyAssignment,
  StudentOverallAttendance,
  AttendanceHistoryItem,
  CorrectionRequestInput,
} from '../types/erp.types';

export const erpApi = {
  async getStudentProfile(): Promise<StudentAcademicProfile> {
    const response = await apiClient.get<StudentAcademicProfile>('/erp/student/profile');
    return response.data;
  },

  async getStudentSubjects(): Promise<EnrolledSubject[]> {
    const response = await apiClient.get<EnrolledSubject[]>('/erp/student/subjects');
    return response.data;
  },

  async getStudentTimetable(): Promise<TimetableSlot[]> {
    const response = await apiClient.get<TimetableSlot[]>('/erp/student/timetable');
    return response.data;
  },

  async getFacultyAssignments(): Promise<FacultyAssignment[]> {
    const response = await apiClient.get<FacultyAssignment[]>('/erp/faculty/assignments');
    return response.data;
  },

  async getFacultyTimetable(): Promise<TimetableSlot[]> {
    const response = await apiClient.get<TimetableSlot[]>('/erp/faculty/timetable');
    return response.data;
  },

  // -------------------------------------------------------------
  // Regular Academic Class Attendance
  // -------------------------------------------------------------
  async getAttendanceSummary(): Promise<StudentOverallAttendance> {
    const response = await apiClient.get<StudentOverallAttendance>('/erp/attendance/summary');
    return response.data;
  },

  async getAttendanceHistory(subjectId?: number): Promise<AttendanceHistoryItem[]> {
    const params = subjectId ? { subjectId } : {};
    const response = await apiClient.get<AttendanceHistoryItem[]>('/erp/attendance/history', { params });
    return response.data;
  },

  async submitCorrection(input: CorrectionRequestInput): Promise<{ success: boolean; message: string; requestId: number }> {
    const response = await apiClient.post<{ success: boolean; message: string; requestId: number }>(
      '/erp/attendance/correction',
      input
    );
    return response.data;
  },
};
