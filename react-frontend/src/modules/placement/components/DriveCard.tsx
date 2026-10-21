import React from 'react';
import { UpcomingDrive } from '../types/drives.types';
import { Badge } from '@/shared/components/Badge';

interface DriveCardProps {
  drive: UpcomingDrive;
  onOpenRegister: (drive: UpcomingDrive) => void;
}

export const DriveCard: React.FC<DriveCardProps> = ({ drive, onOpenRegister }) => {
  return (
    <div
      className="card"
      style={{
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        height: '100%',
        position: 'relative',
        transition: 'transform 0.15s ease, box-shadow 0.15s ease',
      }}
    >
      <div>
        {/* Top Badges */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.85rem' }}>
          <Badge variant={drive.eligible ? 'success' : 'danger'}>
            {drive.eligible ? 'Eligible for You' : 'Not Eligible'}
          </Badge>

          {drive.registered && (
            <Badge variant="success">Registered</Badge>
          )}
        </div>

        {/* Company & Role */}
        <h3 style={{ fontSize: '1.25rem', fontWeight: 700, margin: '0 0 0.25rem 0', color: 'var(--text-main)' }}>
          {drive.companyName}
        </h3>
        <p style={{ color: 'var(--primary)', fontWeight: 600, fontSize: '0.9rem', marginBottom: '0.85rem' }}>
          {drive.rolesOffered}
        </p>

        {/* Package & Criteria Badges */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', marginBottom: '0.85rem' }}>
          <span style={{ fontSize: '0.825rem', padding: '0.2rem 0.5rem', backgroundColor: '#f1f5f9', borderRadius: '6px', fontWeight: 500 }}>
            CTC: {drive.ctcOrStipend}
          </span>
          <span style={{ fontSize: '0.825rem', padding: '0.2rem 0.5rem', backgroundColor: '#f1f5f9', borderRadius: '6px', fontWeight: 500 }}>
            Min CGPA: {drive.minCgpa ?? 'None'}
          </span>
        </div>

        {/* Branches & Backlogs */}
        <div style={{ fontSize: '0.825rem', color: 'var(--text-muted)', marginBottom: '0.85rem', lineHeight: 1.5 }}>
          <div><strong>Branches:</strong> {drive.eligibleBranches || 'All Branches'}</div>
          <div><strong>Max Backlogs:</strong> {drive.maxBacklogs}</div>
          <div style={{ marginTop: '0.35rem', color: 'var(--text-main)', fontWeight: 500 }}>
            <strong>Event Schedule:</strong> {drive.formattedDriveDate} ({drive.formattedTimings || '09:00 - 17:00'})
          </div>
          {drive.venue && (
            <div style={{ color: 'var(--text-main)' }}>
              <strong>Venue:</strong> {drive.venue}
            </div>
          )}
        </div>

        {/* Exclusion Reasons */}
        {!drive.eligible && drive.exclusionReasons && drive.exclusionReasons.length > 0 && (
          <div
            style={{
              padding: '0.5rem 0.75rem',
              backgroundColor: '#fef2f2',
              borderRadius: '6px',
              border: '1px solid #fecaca',
              fontSize: '0.8rem',
              color: '#991b1b',
              marginBottom: '0.85rem',
            }}
          >
            <strong>Requirement mismatch:</strong>
            <ul style={{ margin: '0.2rem 0 0 1rem', padding: 0 }}>
              {drive.exclusionReasons.map((reason, idx) => (
                <li key={idx}>{reason}</li>
              ))}
            </ul>
          </div>
        )}
      </div>

      {/* Footer Details & Action */}
      <div style={{ borderTop: '1px solid var(--border-color)', paddingTop: '0.85rem', marginTop: '0.85rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '0.75rem' }}>
          <span>Deadline:</span>
          <span style={{ fontWeight: 600, color: 'var(--danger)' }}>
            {drive.formattedDeadline}
          </span>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
          {drive.registered ? (
            <button
              className="btn btn-secondary"
              disabled
              style={{ width: '100%', fontSize: '0.875rem', backgroundColor: '#f0fdf4', color: '#166534', borderColor: '#bbf7d0' }}
            >
              ✓ Application Submitted
            </button>
          ) : (
            <button
              onClick={() => onOpenRegister(drive)}
              className="btn btn-primary"
              style={{ width: '100%', fontSize: '0.875rem' }}
              disabled={!drive.eligible}
            >
              {drive.eligible ? 'Register Now →' : 'Ineligible to Register'}
            </button>
          )}

          {drive.companyId && (
            <a
              href={`/companies/${drive.companyId}`}
              className="btn btn-secondary"
              style={{ width: '100%', fontSize: '0.8rem', padding: '0.4rem' }}
            >
              View Company Archive ↗
            </a>
          )}
        </div>
      </div>
    </div>
  );
};
