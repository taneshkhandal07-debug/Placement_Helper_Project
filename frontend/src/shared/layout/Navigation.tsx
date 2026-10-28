import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { UserRole } from '../types/auth.types';

interface NavItem {
  label: string;
  to: string;
  icon: React.ReactNode;
  allowedRoles?: UserRole[];
}

const NAV_ITEMS: NavItem[] = [
  {
    label: 'Overview',
    to: '/dashboard',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <rect x="3" y="3" width="7" height="7"></rect>
        <rect x="14" y="3" width="7" height="7"></rect>
        <rect x="14" y="14" width="7" height="7"></rect>
        <rect x="3" y="14" width="7" height="7"></rect>
      </svg>
    ),
  },
  {
    label: 'Upcoming Drives',
    to: '/drives',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
      </svg>
    ),
    allowedRoles: ['ROLE_STUDENT', 'ROLE_ADMIN'],
  },
  {
    label: 'Student ERP',
    to: '/erp',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
        <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
      </svg>
    ),
    allowedRoles: ['ROLE_STUDENT', 'ROLE_ADMIN', 'ROLE_FACULTY'],
  },
  {
    label: 'CRT Training',
    to: '/crt',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="12" cy="12" r="10"></circle>
        <circle cx="12" cy="12" r="6"></circle>
        <circle cx="12" cy="12" r="2"></circle>
      </svg>
    ),
    allowedRoles: ['ROLE_STUDENT', 'ROLE_ADMIN', 'ROLE_FACULTY'],
  },
  {
    label: 'Drive Duty & Integration',
    to: '/integration',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"></path>
        <circle cx="9" cy="7" r="4"></circle>
        <polyline points="16 11 18 13 22 9"></polyline>
      </svg>
    ),
    allowedRoles: ['ROLE_STUDENT', 'ROLE_ADMIN', 'ROLE_FACULTY'],
  },
];

export const Navigation: React.FC<{ onItemClick?: () => void }> = ({ onItemClick }) => {
  const { user } = useAuth();
  const role = user?.role;

  const visibleItems = NAV_ITEMS.filter((item) => {
    if (!item.allowedRoles) return true;
    return role ? item.allowedRoles.includes(role) : false;
  });

  return (
    <nav style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
      {visibleItems.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          onClick={onItemClick}
          style={({ isActive }) => ({
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem',
            padding: '0.65rem 0.9rem',
            borderRadius: '8px',
            textDecoration: 'none',
            fontSize: '0.925rem',
            fontWeight: 500,
            transition: 'all 0.15s ease',
            backgroundColor: isActive ? 'var(--primary-light)' : 'transparent',
            color: isActive ? 'var(--primary)' : 'var(--text-muted)',
          })}
        >
          <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>{item.icon}</span>
          <span>{item.label}</span>
        </NavLink>
      ))}
    </nav>
  );
};
