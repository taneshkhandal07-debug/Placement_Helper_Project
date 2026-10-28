import React from 'react';

export type AlertType = 'success' | 'danger' | 'warning' | 'info';

interface AlertProps {
  type?: AlertType;
  children: React.ReactNode;
  onClose?: () => void;
  style?: React.CSSProperties;
}

export const Alert: React.FC<AlertProps> = ({ type = 'info', children, onClose, style }) => {
  const getStyles = () => {
    switch (type) {
      case 'success':
        return { bg: '#ecfdf5', color: '#065f46', border: '#a7f3d0' };
      case 'danger':
        return { bg: '#fef2f2', color: '#991b1b', border: '#fecaca' };
      case 'warning':
        return { bg: '#fffbeb', color: '#92400e', border: '#fde68a' };
      case 'info':
      default:
        return { bg: '#eff6ff', color: '#1e40af', border: '#bfdbfe' };
    }
  };

  const current = getStyles();

  return (
    <div
      role="alert"
      style={{
        padding: '0.85rem 1.15rem',
        borderRadius: '8px',
        backgroundColor: current.bg,
        color: current.color,
        border: `1px solid ${current.border}`,
        fontSize: '0.9rem',
        display: 'flex',
        alignItems: 'flex-start',
        justifyContent: 'space-between',
        gap: '0.75rem',
        marginBottom: '1rem',
        ...style,
      }}
    >
      <div style={{ flex: 1 }}>{children}</div>
      {onClose && (
        <button
          onClick={onClose}
          aria-label="Close alert"
          style={{
            background: 'none',
            border: 'none',
            color: 'inherit',
            fontSize: '1.1rem',
            cursor: 'pointer',
            padding: 0,
            lineHeight: 1,
            opacity: 0.7,
          }}
        >
          &times;
        </button>
      )}
    </div>
  );
};
