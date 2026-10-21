import React from 'react';

export const CrtStub: React.FC = () => {
  return (
    <div className="card">
      <h2>Campus Recruitment Training (CRT) Module</h2>
      <p style={{ color: 'var(--text-muted)' }}>
        Foundation established. Training cohorts, specialized aptitude tracks, mock interviews, and dedicated CRT attendance
        will be connected in upcoming phases.
      </p>
      <div style={{ marginTop: '1rem', padding: '1rem', background: '#f8fafc', borderRadius: '8px' }}>
        <strong>Strict Policy:</strong> CRT requires explicit opt-in. Year of study alone never implies enrollment.
      </div>
    </div>
  );
};
