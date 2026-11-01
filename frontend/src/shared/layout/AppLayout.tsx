import React, { useEffect, useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';

export const AppLayout: React.FC = () => {
  const { user, logout } = useAuth();
  const location = useLocation();

  // Synchronize theme with the main Superset Pro portal
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    return (localStorage.getItem('youcan-theme') as 'light' | 'dark') || 'light';
  });

  useEffect(() => {
    document.documentElement.setAttribute('data-bs-theme', theme);
    localStorage.setItem('youcan-theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme((prev) => (prev === 'dark' ? 'light' : 'dark'));
  };

  // Identify current active module metadata for the top header breadcrumb
  const getModuleInfo = () => {
    const path = location.pathname;
    if (path.startsWith('/erp')) {
      return {
        title: 'Student ERP',
        badge: 'Academic & Marks',
        color: '#10b981',
        icon: '📚',
      };
    }
    if (path.startsWith('/crt')) {
      return {
        title: 'CRT Training',
        badge: 'Training & Batches',
        color: '#f59e0b',
        icon: '🎯',
      };
    }
    if (path.startsWith('/integration')) {
      return {
        title: 'Drive Duty & Integration',
        badge: 'Duty & Conflicts',
        color: '#06b6d4',
        icon: '🔄',
      };
    }
    if (path.startsWith('/drives')) {
      return {
        title: 'Upcoming Drives',
        badge: 'Placement Intelligence',
        color: '#2563eb',
        icon: '📢',
      };
    }
    return {
      title: 'Campus Operations',
      badge: 'Unified Module',
      color: '#4f46e5',
      icon: '🏛️',
    };
  };

  const moduleInfo = getModuleInfo();
  const displayName = user?.fullName || 'Tanesh Khandal';
  const displayRole = user?.role ? user.role.replace('ROLE_', '') : 'STUDENT';
  const displayBranch = user?.branch || 'CSE';

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        backgroundColor: 'var(--bg-page, #f8fafc)',
        color: 'var(--text-main, #0f172a)',
        fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, sans-serif",
      }}
    >
      {/* ==============================================================
          UNIFIED TOP BAR: Matches Main Superset Pro Portal Navigation
          No redundant sidebar, no secondary navigation channel between 4 modules
          ============================================================== */}
      <header
        style={{
          height: '66px',
          backgroundColor: 'var(--bg-card, #ffffff)',
          borderBottom: '1px solid var(--border-color, #e2e8f0)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0 1.5rem',
          position: 'sticky',
          top: 0,
          zIndex: 40,
          boxShadow: 'var(--shadow-sm, 0 1px 3px rgba(0,0,0,0.04))',
          transition: 'background-color 0.2s ease, border-color 0.2s ease',
        }}
      >
        {/* Left: Brand Identity & Direct Back to Dashboard Navigation */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          {/* Main Brand */}
          <a
            href="/dashboard"
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.65rem',
              textDecoration: 'none',
              color: 'inherit',
            }}
          >
            <div
              style={{
                width: '2.4rem',
                height: '2.4rem',
                backgroundColor: 'var(--primary, #2563eb)',
                color: '#ffffff',
                borderRadius: '10px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '1.25rem',
                boxShadow: '0 2px 6px rgba(37,99,235,0.25)',
              }}
            >
              🎓
            </div>
            <div style={{ display: 'flex', flexDirection: 'column' }}>
              <span
                style={{
                  fontFamily: "'Outfit', 'Inter', sans-serif",
                  fontWeight: 800,
                  fontSize: '1.15rem',
                  lineHeight: 1.1,
                  color: 'var(--primary, #2563eb)',
                  letterSpacing: '-0.02em',
                }}
              >
                Superset Pro
              </span>
              <span
                style={{
                  fontSize: '0.65rem',
                  letterSpacing: '0.08em',
                  color: 'var(--text-muted, #64748b)',
                  fontWeight: 700,
                  textTransform: 'uppercase',
                }}
              >
                JECRC University
              </span>
            </div>
          </a>

          {/* Divider */}
          <div
            style={{
              height: '24px',
              width: '1px',
              backgroundColor: 'var(--border-color, #e2e8f0)',
              margin: '0 0.25rem',
            }}
          />

          {/* Direct Return to Dashboard Button */}
          <a
            href="/dashboard"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.4rem',
              padding: '0.35rem 0.85rem',
              borderRadius: '9999px',
              border: '1px solid var(--border-color, #e2e8f0)',
              backgroundColor: 'var(--bg-page, #f8fafc)',
              color: 'var(--text-main, #0f172a)',
              fontSize: '0.825rem',
              fontWeight: 600,
              textDecoration: 'none',
              transition: 'all 0.15s ease',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.borderColor = 'var(--primary, #2563eb)';
              e.currentTarget.style.color = 'var(--primary, #2563eb)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.borderColor = 'var(--border-color, #e2e8f0)';
              e.currentTarget.style.color = 'var(--text-main, #0f172a)';
            }}
          >
            <span style={{ fontSize: '0.9rem' }}>←</span> Return to Dashboard
          </a>

          {/* Active Module Title Pill */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.45rem',
              padding: '0.25rem 0.75rem',
              borderRadius: '9999px',
              backgroundColor: `${moduleInfo.color}15`,
              color: moduleInfo.color,
              border: `1px solid ${moduleInfo.color}35`,
              fontSize: '0.8rem',
              fontWeight: 700,
              letterSpacing: '0.01em',
            }}
          >
            <span>{moduleInfo.icon}</span>
            <span>{moduleInfo.title}</span>
            <span
              style={{
                fontSize: '0.68rem',
                opacity: 0.8,
                marginLeft: '0.25rem',
                borderLeft: `1px solid ${moduleInfo.color}40`,
                paddingLeft: '0.45rem',
              }}
            >
              {moduleInfo.badge}
            </span>
          </div>
        </div>

        {/* Right: Theme Switcher & User Details */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
          {/* Theme Toggle Button */}
          <button
            onClick={toggleTheme}
            type="button"
            title={theme === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode'}
            aria-label="Toggle Theme"
            style={{
              width: '36px',
              height: '36px',
              borderRadius: '50%',
              border: '1px solid var(--border-color, #e2e8f0)',
              backgroundColor: 'var(--bg-page, #f8fafc)',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '1rem',
              transition: 'all 0.15s ease',
            }}
          >
            {theme === 'dark' ? '☀️' : '🌙'}
          </button>

          {/* User Profile Info */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
            <div style={{ textAlign: 'right' }}>
              <div
                style={{
                  fontSize: '0.875rem',
                  fontWeight: 700,
                  color: 'var(--text-main, #0f172a)',
                  lineHeight: 1.2,
                }}
              >
                {displayName}
              </div>
              <div
                style={{
                  fontSize: '0.725rem',
                  color: 'var(--text-muted, #64748b)',
                  fontWeight: 500,
                }}
              >
                {displayBranch} &bull;{' '}
                <span style={{ fontWeight: 600, color: 'var(--primary, #2563eb)' }}>
                  {displayRole}
                </span>
              </div>
            </div>

            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: '50%',
                backgroundColor: 'var(--primary, #2563eb)',
                color: '#ffffff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontWeight: 700,
                fontSize: '0.875rem',
                boxShadow: '0 2px 4px rgba(37,99,235,0.2)',
              }}
            >
              {displayName.charAt(0).toUpperCase()}
            </div>
          </div>

          {/* Sign Out */}
          <button
            onClick={() => logout()}
            type="button"
            style={{
              padding: '0.35rem 0.75rem',
              fontSize: '0.8rem',
              fontWeight: 600,
              borderRadius: '6px',
              border: '1px solid var(--border-color, #e2e8f0)',
              backgroundColor: 'transparent',
              color: 'var(--text-muted, #64748b)',
              cursor: 'pointer',
              transition: 'all 0.15s ease',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.borderColor = '#ef4444';
              e.currentTarget.style.color = '#ef4444';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.borderColor = 'var(--border-color, #e2e8f0)';
              e.currentTarget.style.color = 'var(--text-muted, #64748b)';
            }}
          >
            Sign Out
          </button>
        </div>
      </header>

      {/* ==============================================================
          MAIN CONTENT AREA: Full-Width, Spacious, Distraction-Free
          No sidebar squeezing tables, schedules, attendance, or cards
          ============================================================== */}
      <main
        style={{
          flex: 1,
          padding: '2rem 1.5rem',
          maxWidth: '1360px',
          margin: '0 auto',
          width: '100%',
          boxSizing: 'border-box',
        }}
      >
        <Outlet />
      </main>
    </div>
  );
};
