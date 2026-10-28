import React, { useState, useEffect } from 'react';
import { PageHeader } from '@/shared/components/PageHeader';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { Alert } from '@/shared/components/Alert';
import { useAuth } from '@/shared/auth/useAuth';
import { integrationApi } from '../api/integrationApi';
import {
  IntegrationOverview,
  ScheduleConflict,
  PlacementParticipation,
  DutyExemption,
  ParticipationVerifyRequest,
} from '../types/integration.types';
import { ScheduleConflictsTable } from '../components/ScheduleConflictsTable';
import { ParticipationsTable } from '../components/ParticipationsTable';
import { DutyExemptionsTable } from '../components/DutyExemptionsTable';
import { StudentVerificationModal } from '../components/StudentVerificationModal';

export const PlacementIntegrationPage: React.FC = () => {
  const { user } = useAuth();
  const isAdminOrFaculty = user?.role === 'ROLE_ADMIN' || user?.role === 'ROLE_FACULTY';
  const isAdmin = user?.role === 'ROLE_ADMIN';

  const [activeTab, setActiveTab] = useState<'conflicts' | 'participations' | 'exemptions'>('conflicts');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const [overview, setOverview] = useState<IntegrationOverview | null>(null);
  const [conflicts, setConflicts] = useState<ScheduleConflict[]>([]);
  const [participations, setParticipations] = useState<PlacementParticipation[]>([]);
  const [exemptions, setExemptions] = useState<DutyExemption[]>([]);

  // Verification modal state
  const [selectedParticipation, setSelectedParticipation] = useState<PlacementParticipation | null>(null);
  const [isVerifyModalOpen, setIsVerifyModalOpen] = useState(false);

  const [detecting, setDetecting] = useState(false);
  const [notifyingAll, setNotifyingAll] = useState(false);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      if (isAdminOrFaculty) {
        const [ov, confList, exList] = await Promise.all([
          integrationApi.getOverview(),
          integrationApi.getConflicts(),
          integrationApi.getAllExemptions(),
        ]);
        setOverview(ov);
        setConflicts(confList);
        setParticipations(ov.recentParticipations || []);
        setExemptions(exList);
      } else {
        // Student view
        const [myPart, myEx, myConf] = await Promise.all([
          integrationApi.getMyParticipations(),
          integrationApi.getMyExemptions(),
          integrationApi.getConflicts(),
        ]);
        setParticipations(myPart);
        setExemptions(myEx);
        setConflicts(myConf);
      }
    } catch (err: any) {
      setError(err?.response?.data?.error || err.message || 'Failed to load placement integration data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [user]);

  const handleNotifySingle = async (conflictId: number) => {
    try {
      await integrationApi.notifySingleConflict(conflictId);
      setSuccessMsg('Advance notification dispatched to student and faculty successfully.');
      loadData();
    } catch (err: any) {
      setError(err?.response?.data?.error || err.message || 'Failed to dispatch notification');
    }
  };

  const handleNotifyAll = async () => {
    if (!overview?.recentParticipations?.[0]?.upcomingDriveId) {
      setError('No upcoming drive selected for bulk notification');
      return;
    }
    const driveId = overview.recentParticipations[0].upcomingDriveId;
    setNotifyingAll(true);
    setError(null);
    try {
      const res = await integrationApi.notifyConflictsForDrive(driveId, false);
      setSuccessMsg(`Advance notifications delivered to ${res.notificationsSent} recipients without duplicates.`);
      loadData();
    } catch (err: any) {
      setError(err?.response?.data?.error || err.message || 'Failed to dispatch bulk notifications');
    } finally {
      setNotifyingAll(false);
    }
  };

  const handleDetectConflicts = async () => {
    if (!overview?.recentParticipations?.[0]?.upcomingDriveId) {
      setError('No upcoming drive found to run conflict detection');
      return;
    }
    const driveId = overview.recentParticipations[0].upcomingDriveId;
    setDetecting(true);
    setError(null);
    try {
      const res = await integrationApi.detectConflicts(driveId);
      setSuccessMsg(`Conflict scan complete: ${res.totalDetected} timetable overlaps recorded.`);
      loadData();
    } catch (err: any) {
      setError(err?.response?.data?.error || err.message || 'Failed to detect conflicts');
    } finally {
      setDetecting(false);
    }
  };

  const handleOpenVerify = (participation: PlacementParticipation) => {
    setSelectedParticipation(participation);
    setIsVerifyModalOpen(true);
  };

  const handleVerifySubmit = async (req: ParticipationVerifyRequest) => {
    await integrationApi.verifyParticipation(req);
    setSuccessMsg('Student attendance verified and duty exemption policies processed.');
    loadData();
  };

  const handleRevokeExemption = async (exemptionId: number, reason: string) => {
    try {
      await integrationApi.revokeExemption(exemptionId, reason);
      setSuccessMsg('Duty exemption revoked and original class attendance status restored.');
      loadData();
    } catch (err: any) {
      setError(err?.response?.data?.error || err.message || 'Failed to revoke duty exemption');
    }
  };

  return (
    <div className="container" style={{ padding: '2rem 1.5rem', maxWidth: '1200px', margin: '0 auto' }}>
      <PageHeader
        title="Placement Duty & Attendance Integration"
        subtitle="Unified conflict management between placement drives, regular academic timetables, and CRT training sessions."
      />

      {error && (
        <Alert type="danger" style={{ marginBottom: '1.25rem' }}>
          {error}
        </Alert>
      )}

      {successMsg && (
        <Alert type="success" style={{ marginBottom: '1.25rem' }}>
          {successMsg}
        </Alert>
      )}

      {/* Metrics Banner */}
      {isAdminOrFaculty && overview && (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
            gap: '1rem',
            marginBottom: '1.75rem',
          }}
        >
          <div className="card" style={{ padding: '1.25rem', borderRadius: '8px' }}>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>
              Drives with Timings
            </div>
            <div style={{ fontSize: '1.75rem', fontWeight: 700, color: 'var(--text-main)', marginTop: '0.25rem' }}>
              {overview.totalDrivesWithTimings}
            </div>
          </div>

          <div className="card" style={{ padding: '1.25rem', borderRadius: '8px' }}>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>
              Total Registrations
            </div>
            <div style={{ fontSize: '1.75rem', fontWeight: 700, color: 'var(--text-main)', marginTop: '0.25rem' }}>
              {overview.totalRegistrations}
            </div>
          </div>

          <div className="card" style={{ padding: '1.25rem', borderRadius: '8px' }}>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>
              Verified Attendees
            </div>
            <div style={{ fontSize: '1.75rem', fontWeight: 700, color: 'var(--success)', marginTop: '0.25rem' }}>
              {overview.totalVerifiedParticipants}
            </div>
          </div>

          <div className="card" style={{ padding: '1.25rem', borderRadius: '8px' }}>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>
              Timetable Conflicts
            </div>
            <div style={{ fontSize: '1.75rem', fontWeight: 700, color: 'var(--warning)', marginTop: '0.25rem' }}>
              {overview.totalConflictsDetected}
            </div>
          </div>

          <div className="card" style={{ padding: '1.25rem', borderRadius: '8px' }}>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase' }}>
              Active Duty Exemptions
            </div>
            <div style={{ fontSize: '1.75rem', fontWeight: 700, color: 'var(--primary)', marginTop: '0.25rem' }}>
              {overview.totalDutyExemptionsGranted}
            </div>
          </div>
        </div>
      )}

      {/* Tabs Bar */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          borderBottom: '1px solid var(--border-color)',
          marginBottom: '1.5rem',
          flexWrap: 'wrap',
          gap: '1rem',
        }}
      >
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button
            className="btn"
            onClick={() => setActiveTab('conflicts')}
            style={{
              padding: '0.65rem 1.25rem',
              fontWeight: 600,
              borderBottom: activeTab === 'conflicts' ? '2px solid var(--primary)' : '2px solid transparent',
              color: activeTab === 'conflicts' ? 'var(--primary)' : 'var(--text-muted)',
              borderRadius: '0',
              backgroundColor: 'transparent',
            }}
          >
            {isAdminOrFaculty ? 'Schedule Conflicts' : 'My Schedule Conflicts'} ({conflicts.length})
          </button>

          <button
            className="btn"
            onClick={() => setActiveTab('participations')}
            style={{
              padding: '0.65rem 1.25rem',
              fontWeight: 600,
              borderBottom: activeTab === 'participations' ? '2px solid var(--primary)' : '2px solid transparent',
              color: activeTab === 'participations' ? 'var(--primary)' : 'var(--text-muted)',
              borderRadius: '0',
              backgroundColor: 'transparent',
            }}
          >
            {isAdminOrFaculty ? 'Drive Check-In & Verification' : 'My Participations'} ({participations.length})
          </button>

          <button
            className="btn"
            onClick={() => setActiveTab('exemptions')}
            style={{
              padding: '0.65rem 1.25rem',
              fontWeight: 600,
              borderBottom: activeTab === 'exemptions' ? '2px solid var(--primary)' : '2px solid transparent',
              color: activeTab === 'exemptions' ? 'var(--primary)' : 'var(--text-muted)',
              borderRadius: '0',
              backgroundColor: 'transparent',
            }}
          >
            {isAdminOrFaculty ? 'Official Duty Exemptions' : 'My Approved Exemptions'} ({exemptions.length})
          </button>
        </div>

        {/* Action Controls for Admin */}
        {isAdmin && activeTab === 'conflicts' && (
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button
              className="btn btn-secondary"
              onClick={handleDetectConflicts}
              disabled={detecting}
              style={{ fontSize: '0.85rem' }}
            >
              {detecting ? 'Scanning Timetables...' : 'Run Conflict Detection'}
            </button>
            <button
              className="btn btn-primary"
              onClick={handleNotifyAll}
              disabled={notifyingAll}
              style={{ fontSize: '0.85rem' }}
            >
              {notifyingAll ? 'Dispatching...' : 'Dispatch Advance Notices'}
            </button>
          </div>
        )}
      </div>

      {/* Main Content Area */}
      {loading ? (
        <LoadingSpinner message="Loading placement integration records..." />
      ) : (
        <div className="card" style={{ padding: '1.25rem', borderRadius: '8px' }}>
          {activeTab === 'conflicts' && (
            <ScheduleConflictsTable
              conflicts={conflicts}
              onNotifySingle={isAdmin ? handleNotifySingle : undefined}
              isAdmin={isAdmin}
            />
          )}

          {activeTab === 'participations' && (
            <ParticipationsTable
              participations={participations}
              onVerifyClick={isAdmin ? handleOpenVerify : undefined}
              isAdmin={isAdmin}
            />
          )}

          {activeTab === 'exemptions' && (
            <DutyExemptionsTable
              exemptions={exemptions}
              onRevoke={isAdmin ? handleRevokeExemption : undefined}
              isAdmin={isAdmin}
            />
          )}
        </div>
      )}

      {/* Verification Modal */}
      <StudentVerificationModal
        isOpen={isVerifyModalOpen}
        onClose={() => setIsVerifyModalOpen(false)}
        participation={selectedParticipation}
        onSubmit={handleVerifySubmit}
      />
    </div>
  );
};
