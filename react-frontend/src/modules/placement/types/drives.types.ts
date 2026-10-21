export interface UpcomingDrive {
  id: number;
  companyName: string;
  companyId: number | null;
  rolesOffered: string;
  ctcOrStipend: string;
  eligibleBranches: string;
  minCgpa: number | null;
  maxBacklogs: number;
  driveDate: string | null;
  formattedDriveDate: string;
  startTime?: string | null;
  endTime?: string | null;
  venue?: string | null;
  formattedTimings?: string | null;
  deadline: string;
  formattedDeadline: string;
  registrationLink: string | null;
  eligible: boolean;
  exclusionReasons: string[];
  registered: boolean;
  registrationStatus: string;
  registrationNotes: string | null;
}

export interface RegisterDriveRequest {
  notes?: string;
}

export interface RegisterDriveResponse {
  success: boolean;
  message: string;
  driveId: number;
  status: string;
}
