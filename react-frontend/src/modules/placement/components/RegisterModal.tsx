import React, { useState } from 'react';
import { Modal } from '@/shared/components/Modal';
import { Alert } from '@/shared/components/Alert';
import { UpcomingDrive } from '../types/drives.types';

interface RegisterModalProps {
  drive: UpcomingDrive | null;
  isOpen: boolean;
  onClose: () => void;
  onRegister: (driveId: number, notes: string) => Promise<void>;
}

export const RegisterModal: React.FC<RegisterModalProps> = ({
  drive,
  isOpen,
  onClose,
  onRegister,
}) => {
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!drive) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await onRegister(drive.id, notes);
      setNotes('');
      onClose();
    } catch (err: any) {
      const msg = err.response?.data?.error || err.message || 'Registration failed';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Register for ${drive.companyName}`}
      footer={
        <>
          <button
            type="button"
            onClick={onClose}
            className="btn btn-secondary"
            disabled={submitting}
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            className="btn btn-primary"
            disabled={submitting || !drive.eligible}
          >
            {submitting ? 'Submitting Application...' : 'Confirm Registration'}
          </button>
        </>
      }
    >
      <div>
        {error && <Alert type="danger">{error}</Alert>}

        {/* Drive Overview */}
        <div style={{ marginBottom: '1.25rem', padding: '0.85rem', backgroundColor: '#f8fafc', borderRadius: '8px', border: '1px solid var(--border-color)' }}>
          <div style={{ fontWeight: 600, fontSize: '1rem', color: 'var(--text-main)' }}>
            {drive.rolesOffered}
          </div>
          <div style={{ display: 'flex', gap: '1rem', marginTop: '0.4rem', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            <span>CTC: <strong>{drive.ctcOrStipend}</strong></span>
            <span>Drive Date: <strong>{drive.formattedDriveDate || 'TBD'}</strong></span>
          </div>
          <div style={{ marginTop: '0.4rem', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            Deadline: <strong style={{ color: 'var(--danger)' }}>{drive.formattedDeadline}</strong>
          </div>
        </div>

        {/* Eligibility Check Status */}
        {!drive.eligible && (
          <Alert type="warning">
            <strong>Eligibility Criteria Not Met:</strong>
            <ul style={{ margin: '0.35rem 0 0 1rem', padding: 0 }}>
              {drive.exclusionReasons.map((r, idx) => (
                <li key={idx}>{r}</li>
              ))}
            </ul>
          </Alert>
        )}

        {/* Form Form */}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="reg-notes">
              Application Notes or Relevant Projects (Optional)
            </label>
            <textarea
              id="reg-notes"
              className="form-control"
              rows={3}
              placeholder="Mention your preferred tech stack, major projects, or certifications..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              disabled={submitting || !drive.eligible}
            />
          </div>

          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', lineHeight: 1.4 }}>
            By registering, your academic details will be shared with the Training & Placement Cell for shortlist generation.
          </div>
        </form>
      </div>
    </Modal>
  );
};
