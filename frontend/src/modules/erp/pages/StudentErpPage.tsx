import React, { useEffect, useState } from 'react';
import { useAuth } from '@/shared/auth/useAuth';
import { erpApi } from '../api/erpApi';
import {
  StudentAcademicProfile,
  EnrolledSubject,
  TimetableSlot,
  FacultyAssignment,
  StudentOverallAttendance,
  AttendanceHistoryItem,
  AttendanceStatus,
} from '../types/erp.types';
import { PageHeader } from '@/shared/components/PageHeader';
import { Badge } from '@/shared/components/Badge';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { Alert } from '@/shared/components/Alert';
import { EmptyState } from '@/shared/components/EmptyState';
import { Modal } from '@/shared/components/Modal';
import { CourseworkAssignmentsSection } from '../components/CourseworkAssignmentsSection';
import { AssessmentsAndMarksSection } from '../components/AssessmentsAndMarksSection';
import { ReportCardSection } from '../components/ReportCardSection';

type ErpTab =
  | 'profile'
  | 'subjects'
  | 'attendance'
  | 'timetable'
  | 'faculty'
  | 'assignments'
  | 'assessments'
  | 'reportCard';

export const StudentErpPage: React.FC = () => {
  const { user } = useAuth();
  const isFaculty = user?.role === 'ROLE_FACULTY';

  const [activeTab, setActiveTab] = useState<ErpTab>(isFaculty ? 'faculty' : 'attendance');
  const [profile, setProfile] = useState<StudentAcademicProfile | null>(null);
  const [subjects, setSubjects] = useState<EnrolledSubject[]>([]);
  const [timetable, setTimetable] = useState<TimetableSlot[]>([]);
  const [attendance, setAttendance] = useState<StudentOverallAttendance | null>(null);
  const [attendanceHistory, setAttendanceHistory] = useState<AttendanceHistoryItem[]>([]);
  const [facultyAssignments, setFacultyAssignments] = useState<FacultyAssignment[]>([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Correction Modal State
  const [selectedRecord, setSelectedRecord] = useState<AttendanceHistoryItem | null>(null);
  const [correctionStatus, setCorrectionStatus] = useState<AttendanceStatus>('APPROVED_LEAVE');
  const [correctionReason, setCorrectionReason] = useState('');
  const [submittingCorrection, setSubmittingCorrection] = useState(false);

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);

      if (isFaculty) {
        const [assignmentsData, timetableData] = await Promise.all([
          erpApi.getFacultyAssignments(),
          erpApi.getFacultyTimetable(),
        ]);
        setFacultyAssignments(assignmentsData);
        setTimetable(timetableData);
      } else {
        const [profileData, subjectsData, timetableData, attendanceData, historyData] = await Promise.all([
          erpApi.getStudentProfile(),
          erpApi.getStudentSubjects(),
          erpApi.getStudentTimetable(),
          erpApi.getAttendanceSummary(),
          erpApi.getAttendanceHistory(),
        ]);
        setProfile(profileData);
        setSubjects(subjectsData);
        setTimetable(timetableData);
        setAttendance(attendanceData);
        setAttendanceHistory(historyData);
      }
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Failed to load academic records.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [isFaculty]);

  const handleOpenCorrection = (record: AttendanceHistoryItem) => {
    setSelectedRecord(record);
    setCorrectionStatus('APPROVED_LEAVE');
    setCorrectionReason('');
  };

  const handleSubmitCorrection = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedRecord) return;

    try {
      setSubmittingCorrection(true);
      const res = await erpApi.submitCorrection({
        attendanceRecordId: selectedRecord.recordId,
        requestedStatus: correctionStatus,
        reason: correctionReason,
      });
      setSuccessMsg(res.message);
      setSelectedRecord(null);
      await loadData();
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Failed to submit correction.');
    } finally {
      setSubmittingCorrection(false);
    }
  };

  const daysOfWeek = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];

  return (
    <div>
      <PageHeader
        title={isFaculty ? 'Faculty Academic Portal' : 'Student Academic ERP & Attendance'}
        subtitle={
          isFaculty
            ? 'Department instruction schedules, assigned lecture sections, and regular attendance marking.'
            : 'Institutional student enrollment, registered subjects, and auditable class attendance records.'
        }
        action={
          <button onClick={loadData} className="btn btn-secondary" style={{ fontSize: '0.85rem' }}>
            Refresh Records
          </button>
        }
      />

      {successMsg && (
        <Alert type="success" onClose={() => setSuccessMsg(null)}>
          {successMsg}
        </Alert>
      )}

      {error && <Alert type="danger" onClose={() => setError(null)}>{error}</Alert>}

      {/* Module Policy Banner */}
      <div
        style={{
          padding: '0.75rem 1rem',
          backgroundColor: '#f8fafc',
          border: '1px solid var(--border-color)',
          borderRadius: '8px',
          marginBottom: '1.5rem',
          fontSize: '0.85rem',
          color: 'var(--text-muted)',
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem',
        }}
      >
        <span>
          <strong>Attendance Safeguard:</strong> Regular curricular attendance represents actual class instruction.
          CRT mock interview attendance is completely decoupled and never contaminates regular class records.
        </span>
      </div>

      {/* Tabs */}
      <div
        style={{
          display: 'flex',
          gap: '0.75rem',
          marginBottom: '1.5rem',
          borderBottom: '1px solid var(--border-color)',
          paddingBottom: '0.75rem',
          overflowX: 'auto',
        }}
      >
        {!isFaculty && (
          <>
            <button
              onClick={() => setActiveTab('attendance')}
              className={`btn ${activeTab === 'attendance' ? 'btn-primary' : 'btn-secondary'}`}
            >
              Class Attendance {attendance ? `(${attendance.overallPercentage}%)` : ''}
            </button>
            <button
              onClick={() => setActiveTab('assignments')}
              className={`btn ${activeTab === 'assignments' ? 'btn-primary' : 'btn-secondary'}`}
            >
              Coursework &amp; Assignments
            </button>
            <button
              onClick={() => setActiveTab('assessments')}
              className={`btn ${activeTab === 'assessments' ? 'btn-primary' : 'btn-secondary'}`}
            >
              Assessments &amp; Marks
            </button>
            <button
              onClick={() => setActiveTab('reportCard')}
              className={`btn ${activeTab === 'reportCard' ? 'btn-primary' : 'btn-secondary'}`}
            >
              Semester Report Card
            </button>
            <button
              onClick={() => setActiveTab('profile')}
              className={`btn ${activeTab === 'profile' ? 'btn-primary' : 'btn-secondary'}`}
            >
              Academic Profile
            </button>
            <button
              onClick={() => setActiveTab('subjects')}
              className={`btn ${activeTab === 'subjects' ? 'btn-primary' : 'btn-secondary'}`}
            >
              Enrolled Subjects ({subjects.length})
            </button>
            <button
              onClick={() => setActiveTab('timetable')}
              className={`btn ${activeTab === 'timetable' ? 'btn-primary' : 'btn-secondary'}`}
            >
              Section Timetable
            </button>
          </>
        )}

        {isFaculty && (
          <>
            <button
              onClick={() => setActiveTab('faculty')}
              className={`btn ${activeTab === 'faculty' ? 'btn-primary' : 'btn-secondary'}`}
            >
              Teaching Assignments ({facultyAssignments.length})
            </button>
            <button
              onClick={() => setActiveTab('assignments')}
              className={`btn ${activeTab === 'assignments' ? 'btn-primary' : 'btn-secondary'}`}
            >
              Coursework Assignments
            </button>
            <button
              onClick={() => setActiveTab('assessments')}
              className={`btn ${activeTab === 'assessments' ? 'btn-primary' : 'btn-secondary'}`}
            >
              Assessments &amp; Marks
            </button>
            <button
              onClick={() => setActiveTab('timetable')}
              className={`btn ${activeTab === 'timetable' ? 'btn-primary' : 'btn-secondary'}`}
            >
              Lecture Schedule
            </button>
          </>
        )}
      </div>

      {loading ? (
        <LoadingSpinner message="Retrieving institutional records from registrar database..." />
      ) : (
        <>
          {/* TAB 1: Attendance Module */}
          {activeTab === 'attendance' && !isFaculty && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
              {attendance && (
                <>
                  {/* Overall KPI Cards */}
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
                    <div className="card">
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                        Cumulative Attendance
                      </div>
                      <div
                        style={{
                          fontSize: '1.75rem',
                          fontWeight: 700,
                          color: attendance.anyDebarmentWarning ? 'var(--danger)' : 'var(--success)',
                        }}
                      >
                        {attendance.overallPercentage}%
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                        Required Minimum: {attendance.requiredThreshold}%
                      </div>
                    </div>

                    <div className="card">
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                        Total Lectures Held
                      </div>
                      <div style={{ fontSize: '1.75rem', fontWeight: 700, color: 'var(--primary)' }}>
                        {attendance.totalSessions}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                        Across {attendance.totalSubjects} Registered Courses
                      </div>
                    </div>

                    <div className="card">
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                        Sessions Attended
                      </div>
                      <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#0284c7' }}>
                        {attendance.totalPresent}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
                        Physically / Officially Verified
                      </div>
                    </div>

                    <div className="card">
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                        Debarment Status
                      </div>
                      <div style={{ marginTop: '0.25rem' }}>
                        {attendance.anyDebarmentWarning ? (
                          <Badge variant="danger">Debarment Risk</Badge>
                        ) : (
                          <Badge variant="success">Good Academic Standing</Badge>
                        )}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.4rem' }}>
                        Eligible for Term-End Exams
                      </div>
                    </div>
                  </div>

                  {/* Subject-Wise Summary Cards */}
                  <div className="card">
                    <h3 style={{ fontSize: '1.15rem', fontWeight: 600, marginBottom: '1rem' }}>
                      Subject-Wise Attendance Metrics
                    </h3>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '1rem' }}>
                      {attendance.subjectSummaries.map((sub) => (
                        <div
                          key={sub.subjectId}
                          style={{
                            padding: '1rem',
                            backgroundColor: '#f8fafc',
                            borderRadius: '8px',
                            border: '1px solid var(--border-color)',
                          }}
                        >
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
                            <div>
                              <span style={{ fontWeight: 700, color: 'var(--primary)', fontSize: '0.9rem' }}>
                                {sub.subjectCode}
                              </span>
                              <div style={{ fontWeight: 600, fontSize: '0.95rem' }}>{sub.subjectName}</div>
                            </div>
                            <Badge variant={sub.debarred ? 'danger' : 'success'}>
                              {sub.attendancePercentage}%
                            </Badge>
                          </div>

                          {/* Progress bar */}
                          <div style={{ height: '8px', backgroundColor: '#e2e8f0', borderRadius: '4px', overflow: 'hidden', margin: '0.75rem 0' }}>
                            <div
                              style={{
                                height: '100%',
                                width: `${Math.min(100, sub.attendancePercentage)}%`,
                                backgroundColor: sub.debarred ? 'var(--danger)' : 'var(--success)',
                                transition: 'width 0.3s ease',
                              }}
                            />
                          </div>

                          {/* Metrics breakdown */}
                          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                            <span>Total: <strong>{sub.totalSessions}</strong></span>
                            <span>Present: <strong style={{ color: 'var(--success)' }}>{sub.presentCount}</strong></span>
                            <span>Absent: <strong style={{ color: 'var(--danger)' }}>{sub.absentCount}</strong></span>
                            <span>Leave/Duty: <strong>{sub.approvedLeaveCount + sub.officialDutyCount}</strong></span>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* Session-by-Session History Log */}
                  <div className="card">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                      <h3 style={{ fontSize: '1.15rem', fontWeight: 600, margin: 0 }}>
                        Recent Class Sessions & Attendance Log
                      </h3>
                      <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                        Audited by Department Faculty
                      </span>
                    </div>

                    <div style={{ overflowX: 'auto' }}>
                      <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.875rem' }}>
                        <thead>
                          <tr style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid var(--border-color)' }}>
                            <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Date & Time</th>
                            <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Course</th>
                            <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Topic Covered</th>
                            <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Instructor</th>
                            <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Status</th>
                            <th style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>Actions</th>
                          </tr>
                        </thead>
                        <tbody>
                          {attendanceHistory.map((item) => (
                            <tr key={item.recordId} style={{ borderBottom: '1px solid var(--border-color)' }}>
                              <td style={{ padding: '0.75rem 1rem', whiteSpace: 'nowrap' }}>
                                <div style={{ fontWeight: 600 }}>{item.sessionDate}</div>
                                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{item.sessionTime}</div>
                              </td>
                              <td style={{ padding: '0.75rem 1rem', whiteSpace: 'nowrap' }}>
                                <span style={{ fontWeight: 600, color: 'var(--primary)' }}>{item.subjectCode}</span>
                              </td>
                              <td style={{ padding: '0.75rem 1rem' }}>
                                <div>{item.topicCovered || 'Regular Curriculum'}</div>
                                {item.remarks && (
                                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontStyle: 'italic' }}>
                                    "{item.remarks}"
                                  </div>
                                )}
                              </td>
                              <td style={{ padding: '0.75rem 1rem', color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>
                                {item.facultyName}
                              </td>
                              <td style={{ padding: '0.75rem 1rem' }}>
                                <Badge
                                  variant={
                                    item.status === 'PRESENT'
                                      ? 'success'
                                      : item.status === 'ABSENT'
                                      ? 'danger'
                                      : 'info'
                                  }
                                >
                                  {item.status.replace('_', ' ')}
                                </Badge>
                              </td>
                              <td style={{ padding: '0.75rem 1rem' }}>
                                {item.status === 'ABSENT' ? (
                                  <button
                                    onClick={() => handleOpenCorrection(item)}
                                    className="btn btn-secondary"
                                    style={{ padding: '0.3rem 0.6rem', fontSize: '0.75rem' }}
                                  >
                                    Request Leave/Duty
                                  </button>
                                ) : (
                                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Verified</span>
                                )}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                </>
              )}
            </div>
          )}

          {/* TAB 2: Academic Profile */}
          {activeTab === 'profile' && !isFaculty && (
            <div>
              {profile && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                  <div className="card">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                      <h3 style={{ fontSize: '1.15rem', fontWeight: 600, margin: 0 }}>Enrollment Verification</h3>
                      <Badge variant="success">{profile.status}</Badge>
                    </div>

                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1rem', fontSize: '0.9rem' }}>
                      <div><strong>Roll Number:</strong> {profile.rollNumber}</div>
                      <div><strong>Student Name:</strong> {profile.studentName}</div>
                      <div><strong>University Email:</strong> {profile.email}</div>
                      <div><strong>Department:</strong> {profile.departmentName} ({profile.departmentCode})</div>
                      <div><strong>Academic Term:</strong> {profile.termName}</div>
                      <div><strong>Assigned Section:</strong> {profile.sectionName}</div>
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* TAB 3: Enrolled Subjects */}
          {activeTab === 'subjects' && !isFaculty && (
            <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
                <thead>
                  <tr style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid var(--border-color)' }}>
                    <th style={{ padding: '0.85rem 1rem', fontWeight: 600 }}>Course Code</th>
                    <th style={{ padding: '0.85rem 1rem', fontWeight: 600 }}>Subject Title</th>
                    <th style={{ padding: '0.85rem 1rem', fontWeight: 600 }}>Type</th>
                    <th style={{ padding: '0.85rem 1rem', fontWeight: 600 }}>Credits</th>
                    <th style={{ padding: '0.85rem 1rem', fontWeight: 600 }}>Instructor</th>
                    <th style={{ padding: '0.85rem 1rem', fontWeight: 600 }}>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {subjects.map((sub) => (
                    <tr key={sub.subjectId} style={{ borderBottom: '1px solid var(--border-color)' }}>
                      <td style={{ padding: '0.85rem 1rem', fontWeight: 600, color: 'var(--primary)' }}>
                        {sub.code}
                      </td>
                      <td style={{ padding: '0.85rem 1rem', fontWeight: 500 }}>{sub.name}</td>
                      <td style={{ padding: '0.85rem 1rem' }}>
                        <Badge variant={sub.type === 'LAB' ? 'warning' : 'neutral'}>{sub.type}</Badge>
                      </td>
                      <td style={{ padding: '0.85rem 1rem' }}>{sub.credits}</td>
                      <td style={{ padding: '0.85rem 1rem', color: 'var(--text-muted)' }}>{sub.instructorName}</td>
                      <td style={{ padding: '0.85rem 1rem' }}>
                        <Badge variant="success">{sub.enrollmentStatus}</Badge>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* TAB 4: Timetable */}
          {activeTab === 'timetable' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
              {daysOfWeek.map((day) => {
                const slotsForDay = timetable.filter((t) => t.dayOfWeek.toUpperCase() === day);
                if (slotsForDay.length === 0) return null;

                return (
                  <div key={day} className="card">
                    <div style={{ fontSize: '0.9rem', fontWeight: 700, color: 'var(--primary)', marginBottom: '0.75rem', textTransform: 'uppercase' }}>
                      {day}
                    </div>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '0.85rem' }}>
                      {slotsForDay.map((slot) => (
                        <div key={slot.id} style={{ padding: '0.85rem', backgroundColor: '#f8fafc', border: '1px solid var(--border-color)', borderRadius: '6px' }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.35rem' }}>
                            <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)' }}>
                              {slot.startTime.substring(0, 5)} - {slot.endTime.substring(0, 5)}
                            </span>
                            <Badge variant="neutral">Room: {slot.roomNumber}</Badge>
                          </div>
                          <div style={{ fontWeight: 600, fontSize: '0.95rem' }}>
                            {slot.subjectCode} — {slot.subjectName}
                          </div>
                          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                            Instructor: <strong>{slot.facultyName}</strong>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {/* TAB 5: Faculty Assignments */}
          {activeTab === 'faculty' && isFaculty && (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '1.25rem' }}>
              {facultyAssignments.map((fa) => (
                <div key={fa.id} className="card">
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                    <span style={{ fontWeight: 700, color: 'var(--primary)', fontSize: '1.1rem' }}>{fa.subjectCode}</span>
                    <Badge variant="info">{fa.roleTitle}</Badge>
                  </div>
                  <h4 style={{ margin: '0 0 0.5rem 0', fontSize: '1rem', fontWeight: 600 }}>{fa.subjectName}</h4>
                  <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '0.75rem' }}>
                    Section: <strong>{fa.sectionName}</strong>
                  </div>
                  <div style={{ padding: '0.5rem 0.75rem', backgroundColor: '#f1f5f9', borderRadius: '6px', fontSize: '0.8rem', display: 'flex', justifyContent: 'space-between' }}>
                    <span>Enrolled Students</span>
                    <strong>{fa.studentCount} active</strong>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* TAB 6: Coursework Assignments */}
          {activeTab === 'assignments' && (
            <CourseworkAssignmentsSection
              isFaculty={isFaculty}
              facultySubjects={facultyAssignments.map((fa) => ({
                subjectId: fa.subjectId,
                subjectCode: fa.subjectCode,
                subjectName: fa.subjectName,
                sectionId: fa.sectionId,
                sectionName: fa.sectionName,
              }))}
            />
          )}

          {/* TAB 7: Assessments & Marks */}
          {activeTab === 'assessments' && (
            <AssessmentsAndMarksSection
              isFaculty={isFaculty}
              facultySubjects={facultyAssignments.map((fa) => ({
                subjectId: fa.subjectId,
                subjectCode: fa.subjectCode,
                subjectName: fa.subjectName,
                sectionId: fa.sectionId,
                sectionName: fa.sectionName,
              }))}
            />
          )}

          {/* TAB 8: Semester Report Card (Student) */}
          {activeTab === 'reportCard' && !isFaculty && (
            <ReportCardSection />
          )}
        </>
      )}

      {/* Attendance Correction Modal */}
      {selectedRecord && (
        <Modal
          isOpen={true}
          onClose={() => setSelectedRecord(null)}
          title={`Attendance Correction Request — ${selectedRecord.subjectCode}`}
          footer={
            <>
              <button
                type="button"
                onClick={() => setSelectedRecord(null)}
                className="btn btn-secondary"
                disabled={submittingCorrection}
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleSubmitCorrection}
                className="btn btn-primary"
                disabled={submittingCorrection || !correctionReason.trim()}
              >
                {submittingCorrection ? 'Submitting Request...' : 'Submit to Faculty'}
              </button>
            </>
          }
        >
          <form onSubmit={handleSubmitCorrection}>
            <div style={{ marginBottom: '1rem', padding: '0.75rem', backgroundColor: '#f8fafc', borderRadius: '6px', fontSize: '0.85rem' }}>
              <div><strong>Lecture Session:</strong> {selectedRecord.sessionDate} ({selectedRecord.sessionTime})</div>
              <div><strong>Recorded Status:</strong> <Badge variant="danger">{selectedRecord.status}</Badge></div>
              <div><strong>Topic:</strong> {selectedRecord.topicCovered}</div>
            </div>

            <div className="form-group">
              <label htmlFor="req-status">Requested Exemption / Correction Status</label>
              <select
                id="req-status"
                className="form-control"
                value={correctionStatus}
                onChange={(e) => setCorrectionStatus(e.target.value as AttendanceStatus)}
              >
                <option value="APPROVED_LEAVE">APPROVED LEAVE (Medical / Sanctioned Emergency)</option>
                <option value="OFFICIAL_DUTY">OFFICIAL DUTY (Academic Event / University Council)</option>
                <option value="PRESENT">PRESENT (Marking Error / Attendance Rectification)</option>
              </select>
            </div>

            <div className="form-group">
              <label htmlFor="req-reason">Justification & Documentation Details</label>
              <textarea
                id="req-reason"
                className="form-control"
                rows={3}
                placeholder="Explain the reason for absence and mention any medical certificates or event sanction letters..."
                value={correctionReason}
                onChange={(e) => setCorrectionReason(e.target.value)}
                required
              />
            </div>

            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              Note: Attendance corrections are subject to verification and approval by your assigned course instructor.
              All modifications are recorded in the institutional audit log.
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};
