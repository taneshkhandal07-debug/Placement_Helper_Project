export type CrtEnrollmentStatus = 'REQUESTED' | 'VERIFIED' | 'REJECTED' | 'WITHDRAWN';
export type CrtSessionStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';
export type CrtAttendanceStatus = 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED';

export interface CrtEligibility {
  fourthYearEligible: boolean;
  eligibilityReason: string;
  currentAcademicSemester: string;
  graduationYear?: number;
  enrollmentStatus?: CrtEnrollmentStatus;
  enrollmentId?: number;
  batchId?: number;
  batchName?: string;
  batchCode?: string;
  optInNotes?: string;
  optedInAt?: string;
  verifiedAt?: string;
  verificationRemarks?: string;
}

export interface CrtBatch {
  id: number;
  batchCode: string;
  name: string;
  startDate: string;
  endDate: string;
  isActive: boolean;
  capacity: number;
}

export interface CrtSession {
  id: number;
  batchId: number;
  batchName: string;
  batchCode: string;
  trackId: number;
  trackCode: string;
  trackName: string;
  trainerId: number;
  trainerName: string;
  sessionDate: string;
  startTime: string;
  endTime: string;
  topic: string;
  locationOrLink?: string;
  status: CrtSessionStatus;
}

export interface CrtAttendanceRecord {
  id: number;
  sessionId: number;
  trackCode: string;
  trackName: string;
  topic: string;
  sessionDate: string;
  startTime: string;
  endTime: string;
  trainerName: string;
  studentId: number;
  studentName: string;
  studentRollNumber: string;
  status: CrtAttendanceStatus;
  remarks?: string;
  recordedAt: string;
}

export interface CrtAttendanceSummary {
  totalSessions: number;
  attendedSessions: number;
  lateSessions: number;
  absentSessions: number;
  excusedSessions: number;
  attendancePercentage: number;
  debarmentRisk: boolean;
  standingStatus: 'EXCELLENT' | 'GOOD' | 'DEBARMENT_RISK';
}

export interface CrtAssessmentReport {
  assessmentId: number;
  title: string;
  trackName: string;
  assessmentDate: string;
  maxMarks: number;
  passPercentage: number;
  marksObtained: number;
  percentage: number;
  passed: boolean;
  remarks?: string;
}

export interface CrtStudentDashboard {
  eligibility: CrtEligibility;
  attendanceSummary: CrtAttendanceSummary | null;
  upcomingSessions: CrtSession[];
  recentAttendance: CrtAttendanceRecord[];
  assessmentReports: CrtAssessmentReport[];
}

export interface PendingEnrollment {
  id: number;
  studentId: number;
  studentName: string;
  studentEmail: string;
  branch: string;
  graduationYear: number;
  optInNotes: string;
  optedInAt: string;
  status: string;
}

export interface PendingCorrection {
  id: number;
  recordId: number;
  studentName: string;
  studentEmail: string;
  sessionTopic: string;
  sessionDate: string;
  currentStatus: string;
  requestedStatus: string;
  reason: string;
  createdAt: string;
}
