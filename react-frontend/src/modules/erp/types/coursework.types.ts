export interface Assignment {
  id: number;
  subjectId: number;
  subjectCode: string;
  subjectName: string;
  sectionId?: number;
  sectionName?: string;
  facultyId: number;
  facultyName: string;
  title: string;
  description: string;
  attachmentUrl?: string;
  dueDate: string;
  maxMarks: number;
  allowLateSubmission: boolean;
  createdAt: string;
  submissionCount: number;
  isOverdue: boolean;
}

export interface Submission {
  id: number;
  assignmentId: number;
  assignmentTitle: string;
  studentEnrollmentId: number;
  studentUserId: number;
  studentName: string;
  studentRollNumber: string;
  submissionText?: string;
  attachmentUrl?: string;
  submittedAt: string;
  isLate: boolean;
  marksObtained?: number;
  facultyFeedback?: string;
  gradedAt?: string;
  gradedByName?: string;
}

export interface Assessment {
  id: number;
  subjectId: number;
  subjectCode: string;
  subjectName: string;
  semesterId: number;
  semesterNumber: number;
  name: string;
  assessmentType: 'INTERNAL' | 'MID_TERM' | 'END_TERM' | 'PRACTICAL' | 'ASSIGNMENT';
  maxMarks: number;
  passingMarks: number;
  weightagePercent: number;
  assessmentDate: string;
  isPublished: boolean;
  publishedAt?: string;
  createdAt: string;
  evaluatedCount: number;
}

export interface StudentMark {
  id: number;
  assessmentId: number;
  assessmentName: string;
  assessmentType: string;
  maxMarks: number;
  passingMarks: number;
  weightagePercent: number;
  subjectId: number;
  subjectCode: string;
  subjectName: string;
  studentEnrollmentId: number;
  studentUserId: number;
  studentName: string;
  studentRollNumber: string;
  marksObtained?: number;
  isAbsent?: boolean;
  isPassed: boolean;
  remarks?: string;
  gradedByUserId?: number;
  gradedByName?: string;
  gradedAt?: string;
}

export interface SubjectResult {
  subjectId: number;
  subjectCode: string;
  subjectName: string;
  credits: number;
  totalMaxMarks: number;
  totalMarksObtained: number;
  percentage: number;
  letterGrade: string;
  gradePoint: number;
  creditPoints: number;
  passed: boolean;
  componentMarks: StudentMark[];
}

export interface SemesterReportCard {
  studentUserId: number;
  studentName: string;
  rollNumber: string;
  departmentName: string;
  semesterId: number;
  semesterNumber: number;
  academicYear: string;
  totalCreditsRegistered: number;
  totalCreditsEarned: number;
  sgpa: number;
  academicStanding: string;
  subjectResults: SubjectResult[];
  generatedAt: string;
}

export interface MarkAuditLog {
  id: number;
  markId: number;
  previousMarks?: number;
  newMarks?: number;
  previousIsAbsent?: boolean;
  newIsAbsent?: boolean;
  reason: string;
  actionByUserName: string;
  createdAt: string;
}

export interface AssignmentCreateRequest {
  subjectId: number;
  sectionId?: number;
  title: string;
  description: string;
  attachmentUrl?: string;
  dueDate: string;
  maxMarks: number;
  allowLateSubmission: boolean;
}

export interface SubmissionSubmitRequest {
  submissionText?: string;
  attachmentUrl?: string;
}

export interface SubmissionGradeRequest {
  marksObtained: number;
  facultyFeedback?: string;
}

export interface AssessmentCreateRequest {
  subjectId: number;
  semesterId: number;
  name: string;
  assessmentType: 'INTERNAL' | 'MID_TERM' | 'END_TERM' | 'PRACTICAL' | 'ASSIGNMENT';
  maxMarks: number;
  passingMarks: number;
  weightagePercent: number;
  assessmentDate: string;
}

export interface RecordMarkRequest {
  assessmentId: number;
  studentEnrollmentId: number;
  marksObtained?: number;
  isAbsent: boolean;
  remarks?: string;
}

export interface MarkCorrectionRequest {
  markId: number;
  newMarks?: number;
  isAbsent: boolean;
  reason: string;
}
