export type PlacementParticipationStatus = 'REGISTERED' | 'ATTENDED_VERIFIED' | 'ABSENT_UNVERIFIED' | 'EXCUSED';

export interface PlacementParticipation {
  id: number;
  upcomingDriveId: number;
  companyName: string;
  jobRole: string;
  driveDate: string | null;
  startTime: string | null;
  endTime: string | null;
  venue: string | null;
  studentId: number;
  studentName: string;
  studentRollNumber: string;
  studentEmail: string;
  verificationStatus: PlacementParticipationStatus;
  verificationSource: string | null;
  verifiedAt: string | null;
  verifiedByName: string | null;
  remarks: string | null;
  createdAt: string;
}

export interface ScheduleConflict {
  id: number;
  upcomingDriveId: number;
  companyName: string;
  studentId: number;
  studentName: string;
  studentRollNumber: string;
  conflictType: 'REGULAR_CLASS' | 'CRT_TRAINING';
  sessionDescription: string;
  conflictDate: string;
  conflictStart: string;
  conflictEnd: string;
  facultyId: number | null;
  facultyName: string | null;
  studentNotified: boolean;
  facultyNotified: boolean;
  detectedAt: string;
}

export interface DutyExemption {
  id: number;
  participationId: number;
  upcomingDriveId: number;
  companyName: string;
  studentId: number;
  studentName: string;
  studentRollNumber: string;
  targetType: 'REGULAR_CLASS' | 'CRT_TRAINING';
  attendanceRecordId: number | null;
  crtAttendanceRecordId: number | null;
  conflictDate: string;
  startTime: string;
  endTime: string;
  originalStatus: string;
  exemptionStatus: 'GRANTED' | 'REVOKED';
  dutyPolicyCode: string;
  approvedById: number | null;
  approvedByName: string | null;
  approvedAt: string;
  remarks: string | null;
  createdAt: string;
}

export interface IntegrationOverview {
  totalDrivesWithTimings: number;
  totalRegistrations: number;
  totalVerifiedParticipants: number;
  totalConflictsDetected: number;
  totalDutyExemptionsGranted: number;
  recentParticipations: PlacementParticipation[];
  upcomingConflicts: ScheduleConflict[];
  recentExemptions: DutyExemption[];
}

export interface ParticipationVerifyRequest {
  participationId?: number;
  upcomingDriveId?: number;
  studentId?: number;
  status: PlacementParticipationStatus;
  verificationSource?: string;
  remarks?: string;
  autoApplyDutyExemption?: boolean;
}
