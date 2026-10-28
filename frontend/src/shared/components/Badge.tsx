import React from 'react';

export type BadgeVariant = 'success' | 'danger' | 'warning' | 'info' | 'neutral';

interface BadgeProps {
  children: React.ReactNode;
  variant?: BadgeVariant;
  style?: React.CSSProperties;
}

export const Badge: React.FC<BadgeProps> = ({ children, variant = 'neutral', style }) => {
  const getColors = (): { bg: string; color: string; border: string } => {
    switch (variant) {
      case 'success':
        return { bg: '#dcfce7', color: '#166534', border: '#bbf7d0' };
      case 'danger':
        return { bg: '#fee2e2', color: '#991b1b', border: '#fecaca' };
      case 'warning':
        return { bg: '#fef3c7', color: '#92400e', border: '#fde68a' };
      case 'info':
        return { bg: '#e0e7ff', color: '#3730a3', border: '#c7d2fe' };
      case 'neutral':
      default:
        return { bg: '#f1f5f9', color: '#334155', border: '#e2e8f0' };
    }
  };

  const colors = getColors();

  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        padding: '0.2rem 0.55rem',
        borderRadius: '9999px',
        fontSize: '0.75rem',
        fontWeight: 600,
        backgroundColor: colors.bg,
        color: colors.color,
        border: `1px solid ${colors.border}`,
        whiteSpace: 'nowrap',
        ...style,
      }}
    >
      {children}
    </span>
  );
};
