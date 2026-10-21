import React, { useState } from 'react';
import { useAuth } from '../shared/auth/useAuth';
import { PageHeader } from '../shared/components/PageHeader';
import { ErpStub } from '../modules/erp/ErpStub';
import { CrtPage } from '../modules/crt/pages/CrtPage';
import { PlacementStub } from '../modules/placement/PlacementStub';
import { Link } from 'react-router-dom';

type ActiveTab = 'overview' | 'erp' | 'crt' | 'placement';

export const DashboardStub: React.FC = () => {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState<ActiveTab>('overview');

  return (
    <div>
      <PageHeader
        title="Unified Student Dashboard"
        subtitle="Cross-module overview across Placement Assistance, Student ERP, and CRT Training."
      />

      {/* Module Switcher Tabs */}
      <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '1.5rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.75rem' }}>
        <button
          onClick={() => setActiveTab('overview')}
          className={`btn ${activeTab === 'overview' ? 'btn-primary' : 'btn-secondary'}`}
        >
          Unified Overview
        </button>
        <button
          onClick={() => setActiveTab('erp')}
          className={`btn ${activeTab === 'erp' ? 'btn-primary' : 'btn-secondary'}`}
        >
          Student ERP
        </button>
        <button
          onClick={() => setActiveTab('crt')}
          className={`btn ${activeTab === 'crt' ? 'btn-primary' : 'btn-secondary'}`}
        >
          CRT Management
        </button>
        <button
          onClick={() => setActiveTab('placement')}
          className={`btn ${activeTab === 'placement' ? 'btn-primary' : 'btn-secondary'}`}
        >
          Placement Assistance
        </button>
      </div>

      {/* Tab Content */}
      {activeTab === 'overview' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          <div className="card">
            <h2 style={{ fontSize: '1.25rem', marginBottom: '0.5rem' }}>Student Profile & Session State</h2>
            <p style={{ color: 'var(--text-muted)', marginBottom: '1rem' }}>
              Your React session is synchronized with the Spring Boot security context.
            </p>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', marginTop: '0.5rem' }}>
              <div style={{ padding: '0.75rem', background: '#f8fafc', borderRadius: '6px', border: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Email</div>
                <div style={{ fontWeight: 600 }}>{user?.email}</div>
              </div>
              <div style={{ padding: '0.75rem', background: '#f8fafc', borderRadius: '6px', border: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Assigned Role</div>
                <div style={{ fontWeight: 600 }}>{user?.role}</div>
              </div>
              <div style={{ padding: '0.75rem', background: '#f8fafc', borderRadius: '6px', border: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Branch</div>
                <div style={{ fontWeight: 600 }}>{user?.branch || 'N/A'}</div>
              </div>
              <div style={{ padding: '0.75rem', background: '#f8fafc', borderRadius: '6px', border: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Graduation Year</div>
                <div style={{ fontWeight: 600 }}>{user?.graduationYear || 'N/A'}</div>
              </div>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '1.25rem' }}>
            <div className="card">
              <h3>Upcoming Drives Live in React</h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', margin: '0.5rem 0 1rem 0' }}>
                The first migrated placement screen is active with live eligibility rules, drive details, and student registration.
              </p>
              <Link to="/drives" className="btn btn-primary" style={{ fontSize: '0.85rem' }}>
                View Upcoming Drives
              </Link>
            </div>
            <div className="card">
              <h3>Thymeleaf Legacy App</h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', margin: '0.5rem 0 1rem 0' }}>
                All existing server-side rendered placement features remain active and unmodified.
              </p>
              <a href="/dashboard" className="btn btn-secondary" style={{ fontSize: '0.85rem' }}>
                Open Thymeleaf Portal
              </a>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'erp' && <ErpStub />}
      {activeTab === 'crt' && <CrtPage />}
      {activeTab === 'placement' && <PlacementStub />}
    </div>
  );
};
