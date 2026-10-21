import React, { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { Navigation } from './Navigation';
import { Badge } from '../components/Badge';

export const AppLayout: React.FC = () => {
  const { user, logout } = useAuth();
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  const getRoleBadgeVariant = () => {
    if (user?.role === 'ROLE_ADMIN') return 'danger';
    if (user?.role === 'ROLE_ALUMNI') return 'info';
    return 'success';
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* Top Navbar */}
      <header
        style={{
          height: '64px',
          backgroundColor: '#ffffff',
          borderBottom: '1px solid var(--border-color)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0 1.5rem',
          position: 'sticky',
          top: 0,
          zIndex: 30,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <button
            onClick={() => setMobileNavOpen(!mobileNavOpen)}
            className="btn btn-secondary mobile-only"
            aria-label="Toggle navigation"
            style={{ padding: '0.4rem 0.6rem' }}
          >
            ☰
          </button>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 700, color: 'var(--primary)', fontSize: '1.2rem' }}>
            <span>You_can</span>
          </div>
        </div>

        {/* User profile & controls */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          {user && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
              <div style={{ textAlign: 'right' }} className="desktop-only">
                <div style={{ fontSize: '0.875rem', fontWeight: 600 }}>{user.fullName || user.email}</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{user.branch || user.email}</div>
              </div>
              <Badge variant={getRoleBadgeVariant()}>{user.role.replace('ROLE_', '')}</Badge>
            </div>
          )}
          <button
            onClick={() => logout()}
            className="btn btn-secondary"
            style={{ padding: '0.4rem 0.8rem', fontSize: '0.85rem' }}
          >
            Sign Out
          </button>
        </div>
      </header>

      {/* Main Layout Container with Sidebar and Content */}
      <div style={{ display: 'flex', flex: 1, minHeight: 'calc(100vh - 64px)' }}>
        {/* Sidebar for Desktop */}
        <aside
          className="desktop-only"
          style={{
            width: '240px',
            backgroundColor: '#ffffff',
            borderRight: '1px solid var(--border-color)',
            padding: '1.5rem 1rem',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
          }}
        >
          <div>
            <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.75rem', paddingLeft: '0.5rem' }}>
              Main Navigation
            </div>
            <Navigation />
          </div>

          {/* Quick link to legacy Thymeleaf */}
          <div style={{ padding: '0.75rem', background: '#f8fafc', borderRadius: '8px', border: '1px solid var(--border-color)' }}>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '0.35rem' }}>Legacy Portal</div>
            <a href="/dashboard" style={{ fontSize: '0.825rem', color: 'var(--primary)', textDecoration: 'none', fontWeight: 500 }}>
              Thymeleaf Portal ↗
            </a>
          </div>
        </aside>

        {/* Mobile Sidebar Overlay */}
        {mobileNavOpen && (
          <div
            style={{
              position: 'fixed',
              inset: 0,
              zIndex: 40,
              backgroundColor: 'rgba(15, 23, 42, 0.5)',
            }}
            onClick={() => setMobileNavOpen(false)}
          >
            <div
              style={{
                width: '260px',
                height: '100%',
                backgroundColor: '#ffffff',
                padding: '1.5rem',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
              }}
              onClick={(e) => e.stopPropagation()}
            >
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                  <span style={{ fontWeight: 700, color: 'var(--primary)' }}>You_can</span>
                  <button onClick={() => setMobileNavOpen(false)} style={{ background: 'none', border: 'none', fontSize: '1.25rem' }}>✕</button>
                </div>
                <Navigation onItemClick={() => setMobileNavOpen(false)} />
              </div>
            </div>
          </div>
        )}

        {/* Content Outlet */}
        <main style={{ flex: 1, padding: '1.5rem', maxWidth: '1200px', margin: '0 auto', width: '100%' }}>
          <Outlet />
        </main>
      </div>

      <style>{`
        @media (max-width: 768px) {
          .desktop-only { display: none !important; }
        }
        @media (min-width: 769px) {
          .mobile-only { display: none !important; }
        }
      `}</style>
    </div>
  );
};
