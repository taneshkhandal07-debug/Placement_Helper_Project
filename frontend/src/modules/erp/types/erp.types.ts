export interface StudentAcademicProfile {
  enrollmentId: number;
  userId: number;
  studentName: string;
  email: string;
  rollNumber: string;
  departmentCode: string;
  departmentName: string;
  semesterNumber: number;
  termName: string;
  sectionId: number;
  sectionName: string;
  status: string;
  totalCredits: number;
  enrolledSubjectsCount: number;
}

export interface EnrolledSubject {
  subjectId: number;
  code: string;
  name: string;
  credits: number;
  type: string;
  enrollmentStatus: string;
  instructorName: string;
}

export interface TimetableSlot {
  id: number;
  sectionId: number;
  sectionName: string;
  subjectCode: string;
  subjectName: string;
  facultyName: string;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  roomNumber: string;
}

export interface FacultyAssignment {
  id: number;
  sectionId: number;
  sectionName: string;
  subjectId: number;
  subjectCode: string;
  subjectName: string;
  roleTitle: string;
  studentCount: number;
}

export type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'APPROVED_LEAVE' | 'OFFICIAL_DUTY';

export interface SubjectAttendanceSummary {
  subjectId: number;
  subjectCode: string;
  subjectName: string;
  totalSessions: number;
  presentCount: number;
  absentCount: number;
  approvedLeaveCount: number;
  officialDutyCount: number;
  effectiveAttendedCount: number;
  attendancePercentage: number;
  debarred: boolean;
  statusText: string;
}

export interface StudentOverallAttendance {
  studentId: number;
  studentName: string;
  rollNumber: string;
  sectionName: string;
  totalSubjects: number;
  totalSessions: number;
  totalPresent: number;
  overallPercentage: number;
  requiredThreshold: number;
  anyDebarmentWarning: boolean;
  subjectSummaries: SubjectAttendanceSummary[];
}

export interface AttendanceHistoryItem {
  recordId: number;
  sessionId: number;
  subjectCode: string;
  subjectName: string;
  sessionDate: string;
  sessionTime: string;
  topicCovered: string;
  status: AttendanceStatus;
  remarks: string;
  facultyName: string;
  hasPendingCorrection: boolean;
}

export interface CorrectionRequestInput {
  attendanceRecordId: number;
  requestedStatus: AttendanceStatus;
  reason: string;
  documentUrl?: string;
}
