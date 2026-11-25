import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ChevronDown, LogOut, UserRound } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { BrandMark } from './BrandMark';
import { UserAvatar } from './UserAvatar';

/** Cabecera de las pantallas de trabajo con menú de cuenta en el avatar. */
export function AppHeader() {
  const { user, profile, logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const wrap = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onDown = (e: PointerEvent) => {
      if (wrap.current && !wrap.current.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setOpen(false);
    document.addEventListener('pointerdown', onDown);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('pointerdown', onDown);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  if (!user) return null;
  const name = profile?.displayName ?? user.displayName;

  return (
    <header className="tq-topbar">
      <Link to="/" className="tq-topbar__home" aria-label="TriviaQuick, ir al panel"><BrandMark /></Link>
      <div className="tq-account" ref={wrap}>
        <button type="button" className="tq-account__trigger" aria-expanded={open} aria-haspopup="menu" onClick={() => setOpen((v) => !v)}>
          <UserAvatar name={name} url={profile?.avatarUrl ?? null} size={34} />
          <span className="tq-account__name">
            <strong>{name}</strong>
            <small>{user.role === 'TEACHER' ? 'Docente' : 'Estudiante'}</small>
          </span>
          <ChevronDown size={16} aria-hidden="true" />
        </button>
        {open && (
          <div className="tq-account__menu" role="menu">
            <button type="button" role="menuitem" onClick={() => { setOpen(false); navigate('/profile'); }}>
              <UserRound size={16} aria-hidden="true" /> Perfil y configuración
            </button>
            <button type="button" role="menuitem" onClick={() => { setOpen(false); void logout(); }}>
              <LogOut size={16} aria-hidden="true" /> Cerrar sesión
            </button>
          </div>
        )}
      </div>
    </header>
  );
}
