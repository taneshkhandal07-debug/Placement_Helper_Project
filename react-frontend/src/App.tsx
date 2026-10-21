import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './shared/auth/AuthProvider';
import { ProtectedRoute } from './shared/components/ProtectedRoute';
import { AppLayout } from './shared/layout/AppLayout';
import { LoginPage } from './pages/LoginPage';
import { DashboardStub } from './pages/DashboardStub';
import { UpcomingDrivesPage } from './modules/placement/pages/UpcomingDrivesPage';
import { StudentErpPage } from './modules/erp/pages/StudentErpPage';
import { CrtPage } from './modules/crt/pages/CrtPage';
import { PlacementIntegrationPage } from './modules/integration/pages/PlacementIntegrationPage';
import { NotFoundPage } from './pages/NotFoundPage';

export const App: React.FC = () => {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* Public login route */}
          <Route path="/login" element={<LoginPage />} />

          {/* Protected routes wrapped in AppLayout */}
          <Route
            element={
              <ProtectedRoute>
                <AppLayout />
              </ProtectedRoute>
            }
          >
            <Route path="/dashboard" element={<DashboardStub />} />
            <Route path="/drives" element={<UpcomingDrivesPage />} />
            <Route path="/erp" element={<StudentErpPage />} />
            <Route path="/crt" element={<CrtPage />} />
            <Route path="/integration" element={<PlacementIntegrationPage />} />
          </Route>

          {/* Root redirect */}
          <Route path="/" element={<Navigate to="/dashboard" replace />} />

          {/* 404 fallback */}
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
};

export default App;
