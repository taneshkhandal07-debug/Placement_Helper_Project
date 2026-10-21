import { apiClient } from '@/shared/api/client';
import {
  Assignment,
  Submission,
  Assessment,
  StudentMark,
  SubjectResult,
  SemesterReportCard,
  MarkAuditLog,
  AssignmentCreateRequest,
  SubmissionSubmitRequest,
  SubmissionGradeRequest,
  AssessmentCreateRequest,
  RecordMarkRequest,
  MarkCorrectionRequest,
} from '../types/coursework.types';

export const courseworkApi = {
  // Assignments
  async getAssignments(subjectId?: number, sectionId?: number): Promise<Assignment[]> {
    const params: Record<string, number> = {};
    if (subjectId) params.subjectId = subjectId;
    if (sectionId) params.sectionId = sectionId;
    const response = await apiClient.get<Assignment[]>('/erp/coursework/assignments', { params });
    return response.data;
  },

  async createAssignment(req: AssignmentCreateRequest): Promise<Assignment> {
    const response = await apiClient.post<Assignment>('/erp/coursework/assignments', req);
    return response.data;
  },

  async getAssignment(assignmentId: number): Promise<Assignment> {
    const response = await apiClient.get<Assignment>(`/erp/coursework/assignments/${assignmentId}`);
    return response.data;
  },

  async submitAssignment(assignmentId: number, req: SubmissionSubmitRequest): Promise<Submission> {
    const response = await apiClient.post<Submission>(`/erp/coursework/assignments/${assignmentId}/submit`, req);
    return response.data;
  },

  async getMySubmission(assignmentId: number): Promise<Submission | null> {
    try {
      const response = await apiClient.get<Submission>(`/erp/coursework/assignments/${assignmentId}/my-submission`);
      return response.data;
    } catch (err: any) {
      if (err.response?.status === 404) return null;
      throw err;
    }
  },

  async getAssignmentSubmissions(assignmentId: number): Promise<Submission[]> {
    const response = await apiClient.get<Submission[]>(`/erp/coursework/assignments/${assignmentId}/submissions`);
    return response.data;
  },

  async gradeSubmission(submissionId: number, req: SubmissionGradeRequest): Promise<Submission> {
    const response = await apiClient.post<Submission>(`/erp/coursework/submissions/${submissionId}/grade`, req);
    return response.data;
  },

  // Assessments
  async createAssessment(req: AssessmentCreateRequest): Promise<Assessment> {
    const response = await apiClient.post<Assessment>('/erp/coursework/assessments', req);
    return response.data;
  },

  async getAssessments(subjectId?: number, semesterId?: number): Promise<Assessment[]> {
    const params: Record<string, number> = {};
    if (subjectId) params.subjectId = subjectId;
    if (semesterId) params.semesterId = semesterId;
    const response = await apiClient.get<Assessment[]>('/erp/coursework/assessments', { params });
    return response.data;
  },

  async publishAssessment(assessmentId: number): Promise<Assessment> {
    const response = await apiClient.post<Assessment>(`/erp/coursework/assessments/${assessmentId}/publish`);
    return response.data;
  },

  // Marks
  async recordMark(req: RecordMarkRequest): Promise<StudentMark> {
    const response = await apiClient.post<StudentMark>('/erp/coursework/marks/record', req);
    return response.data;
  },

  async correctMark(req: MarkCorrectionRequest): Promise<StudentMark> {
    const response = await apiClient.post<StudentMark>('/erp/coursework/marks/correct', req);
    return response.data;
  },

  async getAssessmentMarks(assessmentId: number): Promise<StudentMark[]> {
    const response = await apiClient.get<StudentMark[]>(`/erp/coursework/marks/assessment/${assessmentId}`);
    return response.data;
  },

  async getMyResults(semesterId?: number): Promise<SubjectResult[]> {
    const params: Record<string, number> = {};
    if (semesterId) params.semesterId = semesterId;
    const response = await apiClient.get<SubjectResult[]>('/erp/coursework/marks/my-results', { params });
    return response.data;
  },

  async getMarkAuditLogs(markId: number): Promise<MarkAuditLog[]> {
    const response = await apiClient.get<MarkAuditLog[]>(`/erp/coursework/marks/${markId}/audit-logs`);
    return response.data;
  },

  // Report Card
  async getReportCard(semesterId?: number, studentUserId?: number): Promise<SemesterReportCard> {
    const params: Record<string, number> = {};
    if (semesterId) params.semesterId = semesterId;
    if (studentUserId) params.studentUserId = studentUserId;
    const response = await apiClient.get<SemesterReportCard>('/erp/coursework/report-card', { params });
    return response.data;
  },

  async downloadReportCardPdf(semesterId?: number, studentUserId?: number): Promise<Blob> {
    const params: Record<string, number> = {};
    if (semesterId) params.semesterId = semesterId;
    if (studentUserId) params.studentUserId = studentUserId;
    const response = await apiClient.get('/erp/coursework/report-card/download', {
      params,
      responseType: 'blob',
    });
    return response.data;
  },
};
