import type { ReactNode } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { DashboardPage } from './pages/DashboardPage';
import { HostPage } from './pages/HostPage';
import { PlayPage } from './pages/PlayPage';
import { ResultsPage } from './pages/ResultsPage';
import { QuizEditorPage } from './pages/QuizEditorPage';
import { ProfilePage } from './pages/ProfilePage';
import { VerifyEmailPage } from './pages/VerifyEmailPage';
import type { Role } from './types/api';

/** Protección de rutas en la interfaz. La autorización real la aplica siempre el backend. */
function Protected({ children, role }: { children: ReactNode; role?: Role }) {
  const { user, loading } = useAuth();
  const location = useLocation();
  if (loading) {
    return (
      <div className="tq-boot" role="status">
        Cargando…
      </div>
    );
  }
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (role && user.role !== role) return <Navigate to="/" replace />;
  return <>{children}</>;
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/verify-email" element={<VerifyEmailPage />} />
      <Route path="/" element={<Protected><DashboardPage /></Protected>} />
      <Route path="/profile" element={<Protected><ProfilePage /></Protected>} />
      <Route path="/teacher/quizzes/new" element={<Protected role="TEACHER"><QuizEditorPage /></Protected>} />
      <Route path="/teacher/quizzes/:quizId" element={<Protected role="TEACHER"><QuizEditorPage /></Protected>} />
      <Route path="/host/:sessionId" element={<Protected role="TEACHER"><HostPage /></Protected>} />
      <Route path="/play/:sessionId" element={<Protected role="STUDENT"><PlayPage /></Protected>} />
      <Route path="/sessions/:sessionId/results" element={<Protected><ResultsPage /></Protected>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
