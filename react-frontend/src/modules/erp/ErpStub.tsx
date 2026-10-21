import React from 'react';

export const ErpStub: React.FC = () => {
  return (
    <div className="card">
      <h2>Student ERP Module</h2>
      <p style={{ color: 'var(--text-muted)' }}>
        Foundation established. Academic records, attendance records, semester fees, and course registrations
        will be connected in upcoming phases.
      </p>
      <div style={{ marginTop: '1rem', padding: '1rem', background: '#f8fafc', borderRadius: '8px' }}>
        <strong>Boundary Isolation:</strong> ERP academic attendance is maintained separately from CRT and Placement duty.
      </div>
    </div>
  );
};
