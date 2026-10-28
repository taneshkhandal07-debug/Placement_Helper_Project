import React, { useState } from 'react';
import { DutyExemption } from '../types/integration.types';
import { Badge } from '@/shared/components/Badge';
import { EmptyState } from '@/shared/components/EmptyState';

interface DutyExemptionsTableProps {
  exemptions: DutyExemption[];
  onRevoke?: (exemptionId: number, reason: string) => Promise<void>;
  isAdmin?: boolean;
}

export const DutyExemptionsTable: React.FC<DutyExemptionsTableProps> = ({
  exemptions,
  onRevoke,
  isAdmin = false,
}) => {
  const [revokingId, setRevokingId] = useState<number | null>(null);

  if (!exemptions || exemptions.length === 0) {
    return (
      <EmptyState
        title="No Placement Duty Exemptions"
        description="No academic class or CRT duty exemptions have been processed yet."
      />
    );
  }

  const handleRevoke = async (id: number) => {
    if (!onRevoke) return;
    const reason = window.prompt('Please enter the administrative reason for revoking this exemption:');
    if (!reason || !reason.trim()) return;

    setRevokingId(id);
    try {
      await onRevoke(id, reason.trim());
    } finally {
      setRevokingId(null);
    }
  };

  return (
    <div style={{ overflowX: 'auto' }}>
      <table className="table" style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
        <thead>
          <tr style={{ borderBottom: '1px solid var(--border-color)', textAlign: 'left', color: 'var(--text-muted)' }}>
            <th style={{ padding: '0.75rem 1rem' }}>Placement Drive</th>
            <th style={{ padding: '0.75rem 1rem' }}>Student</th>
            <th style={{ padding: '0.75rem 1rem' }}>Target Type & Status</th>
            <th style={{ padding: '0.75rem 1rem' }}>Conflict Date & Timing</th>
            <th style={{ padding: '0.75rem 1rem' }}>Original Status</th>
            <th style={{ padding: '0.75rem 1rem' }}>Exemption Status</th>
            <th style={{ padding: '0.75rem 1rem' }}>Policy & Approver</th>
            {isAdmin && <th style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>Actions</th>}
          </tr>
        </thead>
        <tbody>
          {exemptions.map((e) => (
            <tr key={e.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
              <td style={{ padding: '0.85rem 1rem', fontWeight: 600 }}>
                {e.companyName}
              </td>
              <td style={{ padding: '0.85rem 1rem' }}>
                <div style={{ fontWeight: 500 }}>{e.studentName}</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{e.studentRollNumber}</div>
              </td>
              <td style={{ padding: '0.85rem 1rem' }}>
                {e.targetType === 'REGULAR_CLASS' ? (
                  <div>
                    <Badge variant="info">Regular Class</Badge>
                    <div style={{ fontSize: '0.75rem', color: '#1e40af', fontWeight: 600, marginTop: '0.2rem' }}>
                      Marked: OFFICIAL_DUTY
                    </div>
                  </div>
                ) : (
                  <div>
                    <Badge variant="warning">CRT Training</Badge>
                    <div style={{ fontSize: '0.75rem', color: '#854d0e', fontWeight: 600, marginTop: '0.2rem' }}>
                      Marked: EXCUSED
                    </div>
                  </div>
                )}
              </td>
              <td style={{ padding: '0.85rem 1rem' }}>
                <div style={{ fontWeight: 500 }}>{e.conflictDate}</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  {e.startTime} - {e.endTime}
                </div>
              </td>
              <td style={{ padding: '0.85rem 1rem' }}>
                <span style={{ fontSize: '0.8rem', padding: '0.2rem 0.5rem', backgroundColor: '#f1f5f9', borderRadius: '4px', fontWeight: 600, color: 'var(--text-muted)' }}>
                  {e.originalStatus || 'ABSENT'}
                </span>
              </td>
              <td style={{ padding: '0.85rem 1rem' }}>
                <Badge variant={e.exemptionStatus === 'GRANTED' ? 'success' : 'danger'}>
                  {e.exemptionStatus}
                </Badge>
              </td>
              <td style={{ padding: '0.85rem 1rem', fontSize: '0.8rem' }}>
                <div style={{ fontWeight: 500 }}>{e.dutyPolicyCode}</div>
                <div style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>
                  By: {e.approvedByName || 'Authorized Administrator'}
                </div>
              </td>
              {isAdmin && (
                <td style={{ padding: '0.85rem 1rem', textAlign: 'right' }}>
                  {e.exemptionStatus === 'GRANTED' ? (
                    <button
                      className="btn btn-secondary"
                      style={{ fontSize: '0.75rem', padding: '0.35rem 0.65rem', color: '#dc2626', borderColor: '#fca5a5' }}
                      onClick={() => handleRevoke(e.id)}
                      disabled={revokingId === e.id}
                    >
                      {revokingId === e.id ? 'Revoking...' : 'Revoke Exemption'}
                    </button>
                  ) : (
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Revoked</span>
                  )}
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
