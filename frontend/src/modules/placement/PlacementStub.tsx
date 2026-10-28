import React from 'react';

export const PlacementStub: React.FC = () => {
  return (
    <div className="card">
      <h2>Placement Assistance Module</h2>
      <p style={{ color: 'var(--text-muted)' }}>
        Placement intelligence, drive eligibility, application pipelines, company history, and AI tools.
      </p>
      <div style={{ marginTop: '1rem', padding: '1rem', background: '#f8fafc', borderRadius: '8px' }}>
        <strong>Current State:</strong> The full-featured Thymeleaf placement platform is actively serving users.
        REST endpoints are being added incrementally without breaking existing workflows.
      </div>
    </div>
  );
};
