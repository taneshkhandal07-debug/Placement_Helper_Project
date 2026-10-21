import React, { useEffect, useState, useMemo } from 'react';
import { drivesApi } from '../api/drivesApi';
import { UpcomingDrive } from '../types/drives.types';
import { DriveCard } from '../components/DriveCard';
import { RegisterModal } from '../components/RegisterModal';
import { PageHeader } from '@/shared/components/PageHeader';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { Alert } from '@/shared/components/Alert';
import { EmptyState } from '@/shared/components/EmptyState';

export const UpcomingDrivesPage: React.FC = () => {
  const [drives, setDrives] = useState<UpcomingDrive[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Filter state
  const [search, setSearch] = useState('');
  const [filterEligibleOnly, setFilterEligibleOnly] = useState(false);

  // Registration modal state
  const [selectedDrive, setSelectedDrive] = useState<UpcomingDrive | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const fetchDrives = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await drivesApi.getUpcomingDrives();
      setDrives(data);
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Failed to load upcoming drives.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDrives();
  }, []);

  const handleOpenRegister = (drive: UpcomingDrive) => {
    setSelectedDrive(drive);
    setIsModalOpen(true);
  };

  const handleRegisterSubmit = async (driveId: number, notes: string) => {
    const response = await drivesApi.register(driveId, { notes });
    setSuccessMsg(response.message || 'Registration confirmed successfully!');

    // Update local state to reflect registration immediately
    setDrives((prev) =>
      prev.map((d) =>
        d.id === driveId
          ? { ...d, registered: true, registrationStatus: 'REGISTERED', registrationNotes: notes }
          : d
      )
    );
  };

  const filteredDrives = useMemo(() => {
    return drives.filter((drive) => {
      const matchSearch =
        drive.companyName.toLowerCase().includes(search.toLowerCase()) ||
        drive.rolesOffered.toLowerCase().includes(search.toLowerCase()) ||
        drive.eligibleBranches.toLowerCase().includes(search.toLowerCase());

      const matchEligibility = filterEligibleOnly ? drive.eligible : true;
      return matchSearch && matchEligibility;
    });
  }, [drives, search, filterEligibleOnly]);

  const eligibleCount = useMemo(() => drives.filter((d) => d.eligible).length, [drives]);
  const registeredCount = useMemo(() => drives.filter((d) => d.registered).length, [drives]);

  return (
    <div>
      <PageHeader
        title="Upcoming Campus Recruitment Drives"
        subtitle="Live schedule, university criteria evaluations, and placement registration pipeline."
        action={
          <button onClick={fetchDrives} className="btn btn-secondary" style={{ fontSize: '0.85rem' }}>
            Refresh Drives
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

      {/* Summary KPI Strip */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
          gap: '1rem',
          marginBottom: '1.5rem',
        }}
      >
        <div className="card" style={{ padding: '1rem' }}>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Total Drives</div>
          <div style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--primary)' }}>{drives.length}</div>
        </div>
        <div className="card" style={{ padding: '1rem' }}>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Eligible For You</div>
          <div style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--success)' }}>{eligibleCount}</div>
        </div>
        <div className="card" style={{ padding: '1rem' }}>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>My Applications</div>
          <div style={{ fontSize: '1.5rem', fontWeight: 700, color: '#0284c7' }}>{registeredCount}</div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div
        className="card"
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: '1rem',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: '1.5rem',
          padding: '1rem',
        }}
      >
        <div style={{ flex: 1, minWidth: '240px' }}>
          <input
            type="text"
            className="form-control"
            placeholder="Search by company, role, or branch..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.875rem', cursor: 'pointer' }}>
            <input
              type="checkbox"
              checked={filterEligibleOnly}
              onChange={(e) => setFilterEligibleOnly(e.target.checked)}
            />
            Show eligible drives only
          </label>
        </div>
      </div>

      {/* Drives Grid */}
      {loading ? (
        <LoadingSpinner message="Evaluating placement drives with your academic profile..." />
      ) : filteredDrives.length === 0 ? (
        <EmptyState
          title="No drives match your criteria"
          description={
            search || filterEligibleOnly
              ? 'Try clearing the search query or unchecking the eligibility filter.'
              : 'There are currently no upcoming drives scheduled. Please check back soon.'
          }
          action={
            (search || filterEligibleOnly) ? (
              <button
                onClick={() => {
                  setSearch('');
                  setFilterEligibleOnly(false);
                }}
                className="btn btn-secondary"
              >
                Reset Filters
              </button>
            ) : undefined
          }
        />
      ) : (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
            gap: '1.5rem',
          }}
        >
          {filteredDrives.map((drive) => (
            <DriveCard
              key={drive.id}
              drive={drive}
              onOpenRegister={handleOpenRegister}
            />
          ))}
        </div>
      )}

      {/* Registration Modal */}
      <RegisterModal
        drive={selectedDrive}
        isOpen={isModalOpen}
        onClose={() => {
          setIsModalOpen(false);
          setSelectedDrive(null);
        }}
        onRegister={handleRegisterSubmit}
      />
    </div>
  );
};
