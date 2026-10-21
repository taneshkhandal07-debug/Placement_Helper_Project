import React, { useEffect, useState } from 'react';
import { useAuth } from '@/shared/auth/useAuth';
import { PageHeader } from '@/shared/components/PageHeader';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { Alert } from '@/shared/components/Alert';
import { Badge } from '@/shared/components/Badge';
import { Modal } from '@/shared/components/Modal';
import { crtApi } from '../api/crtApi';
import {
  CrtStudentDashboard,
  CrtEligibility,
  PendingEnrollment,
  PendingCorrection,
  CrtAttendanceRecord,
  CrtBatch,
} from '../types/crt.types';

export const CrtPage: React.FC = () => {
  const { user } = useAuth();
  const isFacultyOrAdmin = user?.role === 'ROLE_FACULTY' || user?.role === 'ROLE_ADMIN';

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Student State
  const [studentDashboard, setStudentDashboard] = useState<CrtStudentDashboard | null>(null);

  // Coordinator State
  const [activeBatches, setActiveBatches] = useState<CrtBatch[]>([]);
  const [pendingEnrollments, setPendingEnrollments] = useState<PendingEnrollment[]>([]);
  const [pendingCorrections, setPendingCorrections] = useState<PendingCorrection[]>([]);
  const [coordinatorTab, setCoordinatorTab] = useState<'enrollments' | 'corrections' | 'batches'>('enrollments');

  // Opt-In Modal State (Student)
  const [optInModalOpen, setOptInModalOpen] = useState(false);
  const [optInNotes, setOptInNotes] = useState('');
  const [submittingOptIn, setSubmittingOptIn] = useState(false);

  // Correction Modal State (Student)
  const [correctionModalOpen, setCorrectionModalOpen] = useState(false);
  const [selectedRecord, setSelectedRecord] = useState<CrtAttendanceRecord | null>(null);
  const [requestedStatus, setRequestedStatus] = useState<string>('EXCUSED');
  const [correctionReason, setCorrectionReason] = useState('');
  const [submittingCorrection, setSubmittingCorrection] = useState(false);

  // Action Processing (Coordinator)
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);

      if (isFacultyOrAdmin) {
        const [batches, enrollments, corrections] = await Promise.all([
          crtApi.getActiveBatches(),
          crtApi.getPendingEnrollments(),
          crtApi.getPendingCorrections(),
        ]);
        setActiveBatches(batches);
        setPendingEnrollments(enrollments);
        setPendingCorrections(corrections);
      } else {
        const dashboard = await crtApi.getDashboard();
        setStudentDashboard(dashboard);
      }
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Failed to load CRT module data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [user]);

  // Handle Student Opt-in Submission
  const handleOptInSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSubmittingOptIn(true);
      setError(null);
      const res = await crtApi.optIn(optInNotes);
      setSuccessMsg(res.message || 'Opt-in submitted successfully');
      setOptInModalOpen(false);
      setOptInNotes('');
      await loadData();
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Failed to submit opt-in');
    } finally {
      setSubmittingOptIn(false);
    }
  };

  // Handle Student Attendance Correction Request
  const handleCorrectionSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedRecord) return;
    try {
      setSubmittingCorrection(true);
      setError(null);
      await crtApi.requestCorrection(selectedRecord.id, requestedStatus, correctionReason);
      setSuccessMsg('Correction request submitted for trainer review.');
      setCorrectionModalOpen(false);
      setSelectedRecord(null);
      setCorrectionReason('');
      await loadData();
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Failed to submit correction');
    } finally {
      setSubmittingCorrection(false);
    }
  };

  // Handle Coordinator Verification
  const handleVerifyEnrollment = async (enrollmentId: number, action: 'APPROVE' | 'REJECT') => {
    try {
      setActionLoadingId(enrollmentId);
      await crtApi.verifyEnrollment(enrollmentId, action, 1, action === 'APPROVE' ? 'Approved by coordinator' : 'Rejected');
      setSuccessMsg(`Opt-in application ${action.toLowerCase()}d successfully.`);
      await loadData();
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Action failed');
    } finally {
      setActionLoadingId(null);
    }
  };

  // Handle Coordinator Correction Review
  const handleReviewCorrection = async (correctionId: number, approve: boolean) => {
    try {
      setActionLoadingId(correctionId);
      await crtApi.reviewCorrection(correctionId, approve, approve ? 'Approved correction' : 'Rejected correction');
      setSuccessMsg(`Attendance correction ${approve ? 'approved' : 'rejected'}.`);
      await loadData();
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Review failed');
    } finally {
      setActionLoadingId(null);
    }
  };

  if (loading) {
    return <LoadingSpinner message="Loading Campus Recruitment Training module..." />;
  }

  return (
    <div>
      <PageHeader
        title="Campus Recruitment Training (CRT)"
        subtitle={
          isFacultyOrAdmin
            ? 'Trainer & Coordinator operations: student opt-in verification, session logs, and attendance audit.'
            : 'Pre-placement aptitude, coding tracks, mock interview cohorts, and dedicated training attendance.'
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

      {error && (
        <Alert type="danger" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Module Boundary Policy Banner */}
      <div
        style={{
          padding: '0.75rem 1rem',
          backgroundColor: '#f8fafc',
          border: '1px solid var(--border-color)',
          borderRadius: '8px',
          marginBottom: '1.5rem',
          fontSize: '0.85rem',
          color: 'var(--text-muted)',
        }}
      >
        <strong>Strict Isolation Policy:</strong> CRT attendance is calculated and stored completely independently from regular semester academic attendance.
        A CRT training session does not alter or count toward regular curricular class requirements.
      </div>

      {/* ========================================================================= */}
      {/* COORDINATOR / TRAINER VIEW                                                */}
      {/* ========================================================================= */}
      {isFacultyOrAdmin && (
        <div>
          {/* Navigation Tabs */}
          <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '1.5rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.75rem' }}>
            <button
              onClick={() => setCoordinatorTab('enrollments')}
              className={`btn ${coordinatorTab === 'enrollments' ? 'btn-primary' : 'btn-secondary'}`}
              style={{ fontSize: '0.875rem' }}
            >
              Opt-In Applications ({pendingEnrollments.length})
            </button>
            <button
              onClick={() => setCoordinatorTab('corrections')}
              className={`btn ${coordinatorTab === 'corrections' ? 'btn-primary' : 'btn-secondary'}`}
              style={{ fontSize: '0.875rem' }}
            >
              Attendance Corrections ({pendingCorrections.length})
            </button>
            <button
              onClick={() => setCoordinatorTab('batches')}
              className={`btn ${coordinatorTab === 'batches' ? 'btn-primary' : 'btn-secondary'}`}
              style={{ fontSize: '0.875rem' }}
            >
              Active Batches ({activeBatches.length})
            </button>
          </div>

          {/* Tab 1: Opt-In Verification Applications */}
          {coordinatorTab === 'enrollments' && (
            <div className="card">
              <h2 style={{ fontSize: '1.15rem', marginBottom: '0.5rem' }}>
                Pending Fourth-Year Opt-In Applications
              </h2>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginBottom: '1rem' }}>
                Only verified enrollees are granted access to CRT sessions and receive attendance entries.
              </p>

              {pendingEnrollments.length === 0 ? (
                <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                  No pending opt-in requests awaiting coordinator review.
                </div>
              ) : (
                <div style={{ overflowX: 'auto' }}>
                  <table className="table" style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
                    <thead>
                      <tr style={{ borderBottom: '1px solid var(--border-color)', textAlign: 'left' }}>
                        <th style={{ padding: '0.75rem' }}>Student</th>
                        <th style={{ padding: '0.75rem' }}>Branch & Year</th>
                        <th style={{ padding: '0.75rem' }}>Opt-In Notes</th>
                        <th style={{ padding: '0.75rem' }}>Submitted At</th>
                        <th style={{ padding: '0.75rem', textAlign: 'right' }}>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {pendingEnrollments.map((item) => (
                        <tr key={item.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
                          <td style={{ padding: '0.75rem' }}>
                            <div style={{ fontWeight: 600 }}>{item.studentName}</div>
                            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{item.studentEmail}</div>
                          </td>
                          <td style={{ padding: '0.75rem' }}>
                            {item.branch} (Batch {item.graduationYear})
                          </td>
                          <td style={{ padding: '0.75rem', color: 'var(--text-muted)', maxWidth: '300px' }}>
                            {item.optInNotes || 'No notes specified'}
                          </td>
                          <td style={{ padding: '0.75rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                            {item.optedInAt ? new Date(item.optedInAt).toLocaleDateString() : 'Recent'}
                          </td>
                          <td style={{ padding: '0.75rem', textAlign: 'right' }}>
                            <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                              <button
                                onClick={() => handleVerifyEnrollment(item.id, 'APPROVE')}
                                disabled={actionLoadingId === item.id}
                                className="btn btn-primary"
                                style={{ padding: '0.35rem 0.75rem', fontSize: '0.8rem' }}
                              >
                                {actionLoadingId === item.id ? 'Saving...' : 'Verify & Enroll'}
                              </button>
                              <button
                                onClick={() => handleVerifyEnrollment(item.id, 'REJECT')}
                                disabled={actionLoadingId === item.id}
                                className="btn btn-secondary"
                                style={{ padding: '0.35rem 0.75rem', fontSize: '0.8rem' }}
                              >
                                Reject
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {/* Tab 2: Attendance Corrections */}
          {coordinatorTab === 'corrections' && (
            <div className="card">
              <h2 style={{ fontSize: '1.15rem', marginBottom: '0.5rem' }}>
                Pending Attendance Correction Requests
              </h2>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginBottom: '1rem' }}>
                All approved corrections automatically update the attendance register and record an audit log entry.
              </p>

              {pendingCorrections.length === 0 ? (
                <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                  No pending attendance corrections awaiting review.
                </div>
              ) : (
                <div style={{ overflowX: 'auto' }}>
                  <table className="table" style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
                    <thead>
                      <tr style={{ borderBottom: '1px solid var(--border-color)', textAlign: 'left' }}>
                        <th style={{ padding: '0.75rem' }}>Student</th>
                        <th style={{ padding: '0.75rem' }}>Session Topic</th>
                        <th style={{ padding: '0.75rem' }}>Date</th>
                        <th style={{ padding: '0.75rem' }}>Current Status</th>
                        <th style={{ padding: '0.75rem' }}>Requested</th>
                        <th style={{ padding: '0.75rem' }}>Reason</th>
                        <th style={{ padding: '0.75rem', textAlign: 'right' }}>Review</th>
                      </tr>
                    </thead>
                    <tbody>
                      {pendingCorrections.map((corr) => (
                        <tr key={corr.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
                          <td style={{ padding: '0.75rem' }}>
                            <div style={{ fontWeight: 600 }}>{corr.studentName}</div>
                            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{corr.studentEmail}</div>
                          </td>
                          <td style={{ padding: '0.75rem', fontWeight: 500 }}>{corr.sessionTopic}</td>
                          <td style={{ padding: '0.75rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>{corr.sessionDate}</td>
                          <td style={{ padding: '0.75rem' }}>
                            <Badge variant="danger">{corr.currentStatus}</Badge>
                          </td>
                          <td style={{ padding: '0.75rem' }}>
                            <Badge variant="neutral">{corr.requestedStatus}</Badge>
                          </td>
                          <td style={{ padding: '0.75rem', fontSize: '0.8rem', maxWidth: '250px' }}>{corr.reason}</td>
                          <td style={{ padding: '0.75rem', textAlign: 'right' }}>
                            <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                              <button
                                onClick={() => handleReviewCorrection(corr.id, true)}
                                disabled={actionLoadingId === corr.id}
                                className="btn btn-primary"
                                style={{ padding: '0.35rem 0.75rem', fontSize: '0.8rem' }}
                              >
                                Approve
                              </button>
                              <button
                                onClick={() => handleReviewCorrection(corr.id, false)}
                                disabled={actionLoadingId === corr.id}
                                className="btn btn-secondary"
                                style={{ padding: '0.35rem 0.75rem', fontSize: '0.8rem' }}
                              >
                                Reject
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {/* Tab 3: Active Batches */}
          {coordinatorTab === 'batches' && (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '1.25rem' }}>
              {activeBatches.map((b) => (
                <div key={b.id} className="card">
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                    <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)' }}>{b.batchCode}</span>
                    <Badge variant="success">Active</Badge>
                  </div>
                  <h3 style={{ fontSize: '1.1rem', margin: '0 0 0.5rem 0' }}>{b.name}</h3>
                  <div style={{ fontSize: '0.825rem', color: 'var(--text-muted)', marginBottom: '0.75rem' }}>
                    <div>Duration: {b.startDate} to {b.endDate}</div>
                    <div>Capacity: {b.capacity} candidates</div>
                  </div>
                  <div style={{ borderTop: '1px solid var(--border-color)', paddingTop: '0.75rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                    Tracks: Aptitude, Data Structures, Soft Skills, Mock Interviews
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ========================================================================= */}
      {/* STUDENT VIEW                                                              */}
      {/* ========================================================================= */}
      {!isFacultyOrAdmin && studentDashboard && (
        <div>
          {/* State 1: Ineligible (Not in 4th year) */}
          {!studentDashboard.eligibility.fourthYearEligible && (
            <div className="card" style={{ borderColor: '#fde68a', backgroundColor: '#fffbeb' }}>
              <h2 style={{ fontSize: '1.2rem', color: '#92400e', marginBottom: '0.5rem' }}>
                Campus Recruitment Training Enrollment Policy
              </h2>
              <p style={{ color: '#78350f', fontSize: '0.9rem', marginBottom: '0.75rem' }}>
                {studentDashboard.eligibility.eligibilityReason}
              </p>
              <div style={{ fontSize: '0.85rem', color: '#78350f' }}>
                Current Academic Record: <strong>{studentDashboard.eligibility.currentAcademicSemester}</strong>
              </div>
            </div>
          )}

          {/* State 2: Eligible but NOT yet opted in */}
          {studentDashboard.eligibility.fourthYearEligible && !studentDashboard.eligibility.enrollmentStatus && (
            <div className="card" style={{ padding: '2rem', textAlign: 'center', border: '1px solid var(--primary-light)' }}>
              <div style={{ maxWidth: '600px', margin: '0 auto' }}>
                <span style={{ padding: '0.35rem 0.85rem', backgroundColor: 'var(--primary-light)', color: 'var(--primary)', borderRadius: '999px', fontSize: '0.8rem', fontWeight: 600 }}>
                  Fourth-Year Eligibility Confirmed
                </span>
                <h2 style={{ fontSize: '1.5rem', marginTop: '1rem', marginBottom: '0.75rem' }}>
                  Opt-In to Campus Recruitment Training (CRT)
                </h2>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.925rem', marginBottom: '1.5rem', lineHeight: 1.6 }}>
                  Being in your fourth year does not automatically enroll you. CRT is an intensive training track covering
                  Quantitative Aptitude, Technical DSA problem solving, and corporate mock interviews. Submit your opt-in
                  request to begin training.
                </p>
                <button onClick={() => setOptInModalOpen(true)} className="btn btn-primary" style={{ padding: '0.65rem 1.5rem', fontSize: '0.95rem' }}>
                  Submit CRT Opt-In Application
                </button>
              </div>
            </div>
          )}

          {/* State 3: Opt-In Submitted and PENDING verification */}
          {studentDashboard.eligibility.enrollmentStatus === 'REQUESTED' && (
            <div className="card" style={{ backgroundColor: '#f0fdf4', borderColor: '#bbf7d0' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontWeight: 600, color: '#166534' }}>Application Status: Pending Verification</span>
                <Badge variant="warning">Under Review</Badge>
              </div>
              <p style={{ color: '#14532d', fontSize: '0.875rem', margin: '0 0 0.5rem 0' }}>
                Your opt-in application was submitted on{' '}
                {studentDashboard.eligibility.optedInAt ? new Date(studentDashboard.eligibility.optedInAt).toLocaleDateString() : 'today'}.
                The Placement & CRT coordinator will verify your academic enrollment before assigning you to a training batch.
              </p>
              {studentDashboard.eligibility.optInNotes && (
                <div style={{ fontSize: '0.8rem', color: '#14532d', fontStyle: 'italic' }}>
                  Submitted Notes: "{studentDashboard.eligibility.optInNotes}"
                </div>
              )}
            </div>
          )}

          {/* State 4: Opt-In Rejected */}
          {studentDashboard.eligibility.enrollmentStatus === 'REJECTED' && (
            <div className="card" style={{ backgroundColor: '#fef2f2', borderColor: '#fecaca' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontWeight: 600, color: '#991b1b' }}>Enrollment Rejected</span>
                <Badge variant="danger">Rejected</Badge>
              </div>
              <p style={{ color: '#7f1d1d', fontSize: '0.875rem' }}>
                Reason: {studentDashboard.eligibility.verificationRemarks || 'Academic criteria or capacity constraint.'}
              </p>
              <button onClick={() => setOptInModalOpen(true)} className="btn btn-secondary" style={{ marginTop: '0.5rem', fontSize: '0.825rem' }}>
                Re-apply for Next Cohort
              </button>
            </div>
          )}

          {/* State 5: VERIFIED ENROLLEE (Active Training Hub) */}
          {studentDashboard.eligibility.enrollmentStatus === 'VERIFIED' && (
            <div>
              {/* Enrollee Header Strip */}
              <div
                className="card"
                style={{
                  display: 'flex',
                  flexWrap: 'wrap',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  gap: '1rem',
                  marginBottom: '1.5rem',
                  padding: '1rem 1.25rem',
                }}
              >
                <div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                    Assigned Training Batch
                  </div>
                  <div style={{ fontSize: '1.2rem', fontWeight: 700, color: 'var(--primary)' }}>
                    {studentDashboard.eligibility.batchName || studentDashboard.eligibility.batchCode}
                  </div>
                </div>
                <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                  <Badge variant="success">Verified Enrollee</Badge>
                  <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                    Verified on {studentDashboard.eligibility.verifiedAt ? new Date(studentDashboard.eligibility.verifiedAt).toLocaleDateString() : 'Active'}
                  </span>
                </div>
              </div>

              {/* Attendance KPI Strip */}
              {studentDashboard.attendanceSummary && (
                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
                    gap: '1rem',
                    marginBottom: '1.5rem',
                  }}
                >
                  <div className="card" style={{ padding: '1rem' }}>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Total Sessions</div>
                    <div style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--primary)' }}>
                      {studentDashboard.attendanceSummary.totalSessions}
                    </div>
                  </div>
                  <div className="card" style={{ padding: '1rem' }}>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Attended</div>
                    <div style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--success)' }}>
                      {studentDashboard.attendanceSummary.attendedSessions}
                    </div>
                  </div>
                  <div className="card" style={{ padding: '1rem' }}>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Attendance %</div>
                    <div style={{ fontSize: '1.5rem', fontWeight: 700, color: studentDashboard.attendanceSummary.debarmentRisk ? 'var(--danger)' : 'var(--primary)' }}>
                      {studentDashboard.attendanceSummary.attendancePercentage}%
                    </div>
                  </div>
                  <div className="card" style={{ padding: '1rem' }}>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Training Standing</div>
                    <div style={{ marginTop: '0.25rem' }}>
                      <Badge variant={studentDashboard.attendanceSummary.debarmentRisk ? 'danger' : 'success'}>
                        {studentDashboard.attendanceSummary.standingStatus.replace('_', ' ')}
                      </Badge>
                    </div>
                  </div>
                </div>
              )}

              {/* Upcoming Scheduled Training Sessions */}
              <div className="card" style={{ marginBottom: '1.5rem' }}>
                <h3 style={{ fontSize: '1.15rem', marginBottom: '0.75rem' }}>Upcoming CRT Training Sessions</h3>
                {studentDashboard.upcomingSessions.length === 0 ? (
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
                    No upcoming sessions scheduled for this week.
                  </p>
                ) : (
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1rem' }}>
                    {studentDashboard.upcomingSessions.map((session) => (
                      <div
                        key={session.id}
                        style={{
                          padding: '1rem',
                          backgroundColor: '#f8fafc',
                          border: '1px solid var(--border-color)',
                          borderRadius: '8px',
                        }}
                      >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                          <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--primary)' }}>{session.trackCode}</span>
                          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{session.sessionDate}</span>
                        </div>
                        <div style={{ fontWeight: 600, fontSize: '0.95rem', marginBottom: '0.35rem' }}>
                          {session.topic}
                        </div>
                        <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                          Time: {session.startTime.substring(0, 5)} - {session.endTime.substring(0, 5)}
                        </div>
                        <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                          Trainer: <strong>{session.trainerName}</strong>
                        </div>
                        {session.locationOrLink && (
                          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                            Venue: {session.locationOrLink}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Training Attendance History */}
              <div className="card" style={{ marginBottom: '1.5rem' }}>
                <h3 style={{ fontSize: '1.15rem', marginBottom: '0.75rem' }}>CRT Attendance Register</h3>
                {studentDashboard.recentAttendance.length === 0 ? (
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
                    No session attendance records logged yet.
                  </p>
                ) : (
                  <div style={{ overflowX: 'auto' }}>
                    <table className="table" style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
                      <thead>
                        <tr style={{ borderBottom: '1px solid var(--border-color)', textAlign: 'left' }}>
                          <th style={{ padding: '0.65rem' }}>Date</th>
                          <th style={{ padding: '0.65rem' }}>Track</th>
                          <th style={{ padding: '0.65rem' }}>Topic</th>
                          <th style={{ padding: '0.65rem' }}>Trainer</th>
                          <th style={{ padding: '0.65rem' }}>Status</th>
                          <th style={{ padding: '0.65rem', textAlign: 'right' }}>Correction</th>
                        </tr>
                      </thead>
                      <tbody>
                        {studentDashboard.recentAttendance.map((rec) => (
                          <tr key={rec.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
                            <td style={{ padding: '0.65rem', color: 'var(--text-muted)' }}>{rec.sessionDate}</td>
                            <td style={{ padding: '0.65rem' }}>
                              <Badge variant="neutral">{rec.trackCode}</Badge>
                            </td>
                            <td style={{ padding: '0.65rem', fontWeight: 500 }}>{rec.topic}</td>
                            <td style={{ padding: '0.65rem', color: 'var(--text-muted)' }}>{rec.trainerName}</td>
                            <td style={{ padding: '0.65rem' }}>
                              <Badge variant={rec.status === 'PRESENT' ? 'success' : rec.status === 'LATE' ? 'warning' : 'danger'}>
                                {rec.status}
                              </Badge>
                            </td>
                            <td style={{ padding: '0.65rem', textAlign: 'right' }}>
                              <button
                                onClick={() => {
                                  setSelectedRecord(rec);
                                  setCorrectionModalOpen(true);
                                }}
                                className="btn btn-secondary"
                                style={{ padding: '0.25rem 0.6rem', fontSize: '0.75rem' }}
                              >
                                Request Correction
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>

              {/* Assessment Scorecard */}
              <div className="card">
                <h3 style={{ fontSize: '1.15rem', marginBottom: '0.75rem' }}>CRT Diagnostic & Mock Assessments</h3>
                {studentDashboard.assessmentReports.length === 0 ? (
                  <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
                    No assessment scores published yet.
                  </p>
                ) : (
                  <div style={{ overflowX: 'auto' }}>
                    <table className="table" style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
                      <thead>
                        <tr style={{ borderBottom: '1px solid var(--border-color)', textAlign: 'left' }}>
                          <th style={{ padding: '0.65rem' }}>Assessment</th>
                          <th style={{ padding: '0.65rem' }}>Track</th>
                          <th style={{ padding: '0.65rem' }}>Date</th>
                          <th style={{ padding: '0.65rem' }}>Marks Scored</th>
                          <th style={{ padding: '0.65rem' }}>Percentage</th>
                          <th style={{ padding: '0.65rem' }}>Result</th>
                          <th style={{ padding: '0.65rem' }}>Feedback</th>
                        </tr>
                      </thead>
                      <tbody>
                        {studentDashboard.assessmentReports.map((report) => (
                          <tr key={report.assessmentId} style={{ borderBottom: '1px solid var(--border-color)' }}>
                            <td style={{ padding: '0.65rem', fontWeight: 600 }}>{report.title}</td>
                            <td style={{ padding: '0.65rem' }}>{report.trackName}</td>
                            <td style={{ padding: '0.65rem', color: 'var(--text-muted)' }}>{report.assessmentDate}</td>
                            <td style={{ padding: '0.65rem', fontWeight: 600 }}>
                              {report.marksObtained} / {report.maxMarks}
                            </td>
                            <td style={{ padding: '0.65rem' }}>{report.percentage}%</td>
                            <td style={{ padding: '0.65rem' }}>
                              <Badge variant={report.passed ? 'success' : 'danger'}>
                                {report.passed ? 'PASSED' : 'NEEDS PRACTICE'}
                              </Badge>
                            </td>
                            <td style={{ padding: '0.65rem', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                              {report.remarks || 'No remarks'}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL: Student Opt-In Application Form                                   */}
      {/* ========================================================================= */}
      <Modal
        isOpen={optInModalOpen}
        onClose={() => setOptInModalOpen(false)}
        title="CRT Training Opt-In Application"
        footer={
          <>
            <button type="button" onClick={() => setOptInModalOpen(false)} className="btn btn-secondary" disabled={submittingOptIn}>
              Cancel
            </button>
            <button type="button" onClick={handleOptInSubmit} className="btn btn-primary" disabled={submittingOptIn}>
              {submittingOptIn ? 'Submitting...' : 'Confirm Opt-In Application'}
            </button>
          </>
        }
      >
        <form onSubmit={handleOptInSubmit}>
          <div style={{ marginBottom: '1rem', fontSize: '0.85rem', color: 'var(--text-muted)', lineHeight: 1.5 }}>
            You are applying for <strong>Cohort 2026 CRT Training</strong>. By opting in, you commit to attending scheduled
            sessions for Quantitative Aptitude, Technical DSA, and Soft Skills.
          </div>
          <div className="form-group">
            <label htmlFor="optInNotes">Career Goal & Specialization Notes (Optional)</label>
            <textarea
              id="optInNotes"
              className="form-control"
              rows={3}
              placeholder="e.g. Aiming for Tier-1 product roles; interested in advanced DSA and System Design..."
              value={optInNotes}
              onChange={(e) => setOptInNotes(e.target.value)}
              disabled={submittingOptIn}
            />
          </div>
        </form>
      </Modal>

      {/* ========================================================================= */}
      {/* MODAL: Student Attendance Correction Request                              */}
      {/* ========================================================================= */}
      <Modal
        isOpen={correctionModalOpen}
        onClose={() => {
          setCorrectionModalOpen(false);
          setSelectedRecord(null);
        }}
        title={`Request Correction: ${selectedRecord?.topic || 'Session'}`}
        footer={
          <>
            <button
              type="button"
              onClick={() => {
                setCorrectionModalOpen(false);
                setSelectedRecord(null);
              }}
              className="btn btn-secondary"
              disabled={submittingCorrection}
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={handleCorrectionSubmit}
              className="btn btn-primary"
              disabled={submittingCorrection || !correctionReason.trim()}
            >
              {submittingCorrection ? 'Submitting...' : 'Submit Request'}
            </button>
          </>
        }
      >
        <form onSubmit={handleCorrectionSubmit}>
          <div style={{ marginBottom: '1rem', padding: '0.75rem', backgroundColor: '#f8fafc', borderRadius: '6px' }}>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Current Status: <strong>{selectedRecord?.status}</strong></div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Session Date: <strong>{selectedRecord?.sessionDate}</strong></div>
          </div>

          <div className="form-group">
            <label htmlFor="requestedStatus">Requested Status</label>
            <select
              id="requestedStatus"
              className="form-control"
              value={requestedStatus}
              onChange={(e) => setRequestedStatus(e.target.value)}
            >
              <option value="PRESENT">PRESENT</option>
              <option value="EXCUSED">EXCUSED (Approved Duty / Medical)</option>
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="correctionReason">Justification</label>
            <textarea
              id="correctionReason"
              className="form-control"
              rows={3}
              placeholder="Provide reason for correction (e.g. academic lab extension, attended online mode)..."
              value={correctionReason}
              onChange={(e) => setCorrectionReason(e.target.value)}
              required
            />
          </div>
        </form>
      </Modal>
    </div>
  );
};
