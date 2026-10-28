import React, { useState } from 'react';
import { Modal } from '@/shared/components/Modal';
import { PlacementParticipation, PlacementParticipationStatus, ParticipationVerifyRequest } from '../types/integration.types';

interface StudentVerificationModalProps {
  isOpen: boolean;
  onClose: () => void;
  participation: PlacementParticipation | null;
  onSubmit: (request: ParticipationVerifyRequest) => Promise<void>;
}

export const StudentVerificationModal: React.FC<StudentVerificationModalProps> = ({
  isOpen,
  onClose,
  participation,
  onSubmit,
}) => {
  const [status, setStatus] = useState<PlacementParticipationStatus>('ATTENDED_VERIFIED');
  const [source, setSource] = useState('PLACEMENT_OFFICER_PORTAL');
  const [remarks, setRemarks] = useState('');
  const [autoApplyDuty, setAutoApplyDuty] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!participation) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      await onSubmit({
        participationId: participation.id,
        status,
        verificationSource: source,
        remarks: remarks.trim() || undefined,
        autoApplyDutyExemption: status === 'ATTENDED_VERIFIED' && autoApplyDuty,
      });
      onClose();
    } catch (err: any) {
      setError(err?.response?.data?.error || err.message || 'Failed to verify participation');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Verify Placement Drive Attendance"
    >
      <div style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginBottom: '1rem' }}>
        Record authorized check-in or participation verification for {participation.studentName}
      </div>
      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        {error && (
          <div style={{ padding: '0.65rem 0.85rem', backgroundColor: '#fef2f2', border: '1px solid #fecaca', borderRadius: '6px', color: '#991b1b', fontSize: '0.85rem' }}>
            {error}
          </div>
        )}

        <div style={{ backgroundColor: '#f8fafc', padding: '0.85rem', borderRadius: '6px', fontSize: '0.85rem', border: '1px solid #e2e8f0' }}>
          <div style={{ fontWeight: 600, color: 'var(--text-main)', marginBottom: '0.25rem' }}>
            {participation.companyName} — {participation.jobRole}
          </div>
          <div style={{ color: 'var(--text-muted)' }}>
            Student: <strong>{participation.studentName}</strong> ({participation.studentRollNumber})
          </div>
          <div style={{ color: 'var(--text-muted)', marginTop: '0.25rem' }}>
            Drive Date: {participation.driveDate} | Venue: {participation.venue || 'Main Campus'}
          </div>
        </div>

        <div>
          <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '0.35rem' }}>
            Verification Status *
          </label>
          <select
            className="input"
            value={status}
            onChange={(e) => setStatus(e.target.value as PlacementParticipationStatus)}
            style={{ width: '100%', padding: '0.5rem 0.75rem', borderRadius: '6px', border: '1px solid var(--border-color)' }}
          >
            <option value="ATTENDED_VERIFIED">ATTENDED_VERIFIED (Physically / Online Participated)</option>
            <option value="ABSENT_UNVERIFIED">ABSENT_UNVERIFIED (Registered but Did Not Show)</option>
            <option value="EXCUSED">EXCUSED (Prior Exemption / Withdrawn)</option>
          </select>
          <small style={{ color: 'var(--text-muted)', display: 'block', marginTop: '0.25rem' }}>
            Institutional policy requires verified participation before granting class/CRT exemptions.
          </small>
        </div>

        <div>
          <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '0.35rem' }}>
            Verification Source / Method *
          </label>
          <select
            className="input"
            value={source}
            onChange={(e) => setSource(e.target.value)}
            style={{ width: '100%', padding: '0.5rem 0.75rem', borderRadius: '6px', border: '1px solid var(--border-color)' }}
          >
            <option value="PLACEMENT_OFFICER_PORTAL">Placement Officer Portal Check-in</option>
            <option value="AUDITORIUM_CHECKIN_DESK">Physical Check-in Desk / Kiosk</option>
            <option value="BIOMETRIC_CHECKIN">Biometric Verification Station</option>
            <option value="APPROVED_ATTENDANCE_IMPORT">Company Official Attendance Sheet Import</option>
          </select>
        </div>

        <div>
          <label style={{ display: 'block', fontWeight: 600, fontSize: '0.85rem', marginBottom: '0.35rem' }}>
            Verification Remarks / Notes
          </label>
          <input
            type="text"
            className="input"
            placeholder="e.g. Cleared round 1 online assessment on-site"
            value={remarks}
            onChange={(e) => setRemarks(e.target.value)}
            style={{ width: '100%', padding: '0.5rem 0.75rem', borderRadius: '6px', border: '1px solid var(--border-color)' }}
          />
        </div>

        {status === 'ATTENDED_VERIFIED' && (
          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.65rem', padding: '0.75rem', backgroundColor: '#eff6ff', borderRadius: '6px', border: '1px solid #bfdbfe' }}>
            <input
              type="checkbox"
              id="autoApply"
              checked={autoApplyDuty}
              onChange={(e) => setAutoApplyDuty(e.target.checked)}
              style={{ marginTop: '0.2rem' }}
            />
            <label htmlFor="autoApply" style={{ fontSize: '0.825rem', color: '#1e3a8a', cursor: 'pointer' }}>
              <strong>Apply Official Duty Exemption Automatically</strong>
              <div style={{ marginTop: '0.15rem' }}>
                Converts overlapping regular class records to OFFICIAL_DUTY (preserving audit history) and CRT sessions to EXCUSED under institution policy INST_PLACEMENT_DUTY_V1.
              </div>
            </label>
          </div>
        )}

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
          <button type="button" className="btn btn-secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Confirming...' : 'Save Verification'}
          </button>
        </div>
      </form>
    </Modal>
  );
};
