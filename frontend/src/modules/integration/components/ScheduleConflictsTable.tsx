import React, { useState } from 'react';
import { ScheduleConflict } from '../types/integration.types';
import { Badge } from '@/shared/components/Badge';
import { EmptyState } from '@/shared/components/EmptyState';

interface ScheduleConflictsTableProps {
  conflicts: ScheduleConflict[];
  onNotifySingle?: (conflictId: number) => Promise<void>;
  isAdmin?: boolean;
}

export const ScheduleConflictsTable: React.FC<ScheduleConflictsTableProps> = ({
  conflicts,
  onNotifySingle,
  isAdmin = false,
}) => {
  const [notifyingId, setNotifyingId] = useState<number | null>(null);

  if (!conflicts || conflicts.length === 0) {
    return (
      <EmptyState
        title="No Schedule Conflicts Detected"
        description="There are currently no timetable or CRT training conflicts recorded for upcoming placement drives."
      />
    );
  }

  const handleNotify = async (id: number) => {
    if (!onNotifySingle) return;
    setNotifyingId(id);
    try {
      await onNotifySingle(id);
    } finally {
      setNotifyingId(null);
    }
  };

  return (
    <div style={{ overflowX: 'auto' }}>
      <table className="table" style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
        <thead>
          <tr style={{ borderBottom: '1px solid var(--border-color)', textAlign: 'left', color: 'var(--text-muted)' }}>
            <th style={{ padding: '0.75rem 1rem' }}>Placement Drive</th>
            <th style={{ padding: '0.75rem 1rem' }}>Student</th>
            <th style={{ padding: '0.75rem 1rem' }}>Conflict Type</th>
            <th style={{ padding: '0.75rem 1rem' }}>Overlapping Session</th>
            <th style={{ padding: '0.75rem 1rem' }}>Date & Timing</th>
            <th style={{ padding: '0.75rem 1rem' }}>Faculty / Trainer</th>
            <th style={{ padding: '0.75rem 1rem' }}>Notification Status</th>
            {isAdmin && <th style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>Actions</th>}
          </tr>
        </thead>
        <tbody>
          {conflicts.map((c) => (
            <tr key={c.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
              <td style={{ padding: '0.85rem 1rem', fontWeight: 600 }}>
                {c.companyName}
              </td>
              <td style={{ padding: '0.85rem 1rem' }}>
                <div style={{ fontWeight: 500 }}>{c.studentName}</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{c.studentRollNumber}</div>
              </td>
              <td style={{ padding: '0.85rem 1rem' }}>
                <Badge variant={c.conflictType === 'REGULAR_CLASS' ? 'info' : 'warning'}>
                  {c.conflictType === 'REGULAR_CLASS' ? 'Regular Class' : 'CRT Training'}
                </Badge>
              </td>
              <td style={{ padding: '0.85rem 1rem', color: 'var(--text-main)' }}>
                {c.sessionDescription}
              </td>
              <td style={{ padding: '0.85rem 1rem', whiteSpace: 'nowrap' }}>
                <div style={{ fontWeight: 500 }}>{c.conflictDate}</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  {c.conflictStart} - {c.conflictEnd}
                </div>
              </td>
              <td style={{ padding: '0.85rem 1rem', color: 'var(--text-muted)' }}>
                {c.facultyName || 'Department Faculty'}
              </td>
              <td style={{ padding: '0.85rem 1rem' }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                  <span style={{ fontSize: '0.75rem' }}>
                    Student: {c.studentNotified ? (
                      <span style={{ color: 'var(--success)', fontWeight: 600 }}>Notified</span>
                    ) : (
                      <span style={{ color: 'var(--text-muted)' }}>Pending</span>
                    )}
                  </span>
                  <span style={{ fontSize: '0.75rem' }}>
                    Faculty: {c.facultyNotified ? (
                      <span style={{ color: 'var(--success)', fontWeight: 600 }}>Notified</span>
                    ) : (
                      <span style={{ color: 'var(--text-muted)' }}>Pending</span>
                    )}
                  </span>
                </div>
              </td>
              {isAdmin && (
                <td style={{ padding: '0.85rem 1rem', textAlign: 'right' }}>
                  <button
                    className="btn btn-secondary"
                    style={{ fontSize: '0.75rem', padding: '0.35rem 0.65rem' }}
                    onClick={() => handleNotify(c.id)}
                    disabled={notifyingId === c.id}
                  >
                    {notifyingId === c.id ? 'Sending...' : 'Send Notice'}
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
