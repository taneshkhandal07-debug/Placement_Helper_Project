import React from 'react';
import { PlacementParticipation } from '../types/integration.types';
import { Badge } from '@/shared/components/Badge';
import { EmptyState } from '@/shared/components/EmptyState';

interface ParticipationsTableProps {
  participations: PlacementParticipation[];
  onVerifyClick?: (participation: PlacementParticipation) => void;
  isAdmin?: boolean;
}

export const ParticipationsTable: React.FC<ParticipationsTableProps> = ({
  participations,
  onVerifyClick,
  isAdmin = false,
}) => {
  if (!participations || participations.length === 0) {
    return (
      <EmptyState
        title="No Participation Records"
        description="No students have registered or checked in for this drive yet."
      />
    );
  }

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'ATTENDED_VERIFIED':
        return <Badge variant="success">Attended & Verified</Badge>;
      case 'ABSENT_UNVERIFIED':
        return <Badge variant="danger">Absent (Unverified)</Badge>;
      case 'EXCUSED':
        return <Badge variant="info">Excused</Badge>;
      case 'REGISTERED':
      default:
        return <Badge variant="warning">Registered (Pending Verification)</Badge>;
    }
  };

  return (
    <div style={{ overflowX: 'auto' }}>
      <table className="table" style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
        <thead>
          <tr style={{ borderBottom: '1px solid var(--border-color)', textAlign: 'left', color: 'var(--text-muted)' }}>
            <th style={{ padding: '0.75rem 1rem' }}>Placement Drive</th>
            <th style={{ padding: '0.75rem 1rem' }}>Student</th>
            <th style={{ padding: '0.75rem 1rem' }}>Drive Date & Venue</th>
            <th style={{ padding: '0.75rem 1rem' }}>Verification Status</th>
            <th style={{ padding: '0.75rem 1rem' }}>Verification Source / By</th>
            <th style={{ padding: '0.75rem 1rem' }}>Remarks</th>
            {isAdmin && <th style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>Actions</th>}
          </tr>
        </thead>
        <tbody>
          {participations.map((p) => (
            <tr key={p.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
              <td style={{ padding: '0.85rem 1rem' }}>
                <div style={{ fontWeight: 600 }}>{p.companyName}</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--primary)' }}>{p.jobRole}</div>
              </td>
              <td style={{ padding: '0.85rem 1rem' }}>
                <div style={{ fontWeight: 500 }}>{p.studentName}</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  {p.studentRollNumber || p.studentEmail}
                </div>
              </td>
              <td style={{ padding: '0.85rem 1rem' }}>
                <div>{p.driveDate || 'Scheduled'}</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{p.venue || 'Main Campus'}</div>
              </td>
              <td style={{ padding: '0.85rem 1rem' }}>
                {getStatusBadge(p.verificationStatus)}
              </td>
              <td style={{ padding: '0.85rem 1rem' }}>
                {p.verificationSource ? (
                  <div>
                    <span style={{ fontSize: '0.8rem', fontWeight: 500 }}>{p.verificationSource}</span>
                    {p.verifiedByName && (
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                        By: {p.verifiedByName}
                      </div>
                    )}
                  </div>
                ) : (
                  <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>Unverified</span>
                )}
              </td>
              <td style={{ padding: '0.85rem 1rem', color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                {p.remarks || '-'}
              </td>
              {isAdmin && (
                <td style={{ padding: '0.85rem 1rem', textAlign: 'right' }}>
                  <button
                    className="btn btn-secondary"
                    style={{ fontSize: '0.75rem', padding: '0.35rem 0.65rem' }}
                    onClick={() => onVerifyClick && onVerifyClick(p)}
                  >
                    Verify / Update
                  </button>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
