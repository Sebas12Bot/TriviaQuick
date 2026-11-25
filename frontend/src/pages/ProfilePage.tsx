import { useEffect, useRef, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { Camera, KeyRound, Mail, Trash2, UserRound, Volume2 } from 'lucide-react';
import { AppHeader } from '../components/common/AppHeader';
import { UserAvatar } from '../components/common/UserAvatar';
import { useAuth } from '../context/AuthContext';
import { useAudio } from '../context/AudioContext';
import { ApiError } from '../services/api';
import { profileService } from '../services/profileService';
import type { AudioSettings } from '../services/audio/audioEngine';
import '../styles/profile.css';

type Notice = { tone: 'ok' | 'error'; text: string } | null;

const errorText = (e: unknown, fallback: string) => (e instanceof ApiError ? e.message : fallback);

/** Perfil y configuración: datos personales, foto, seguridad, preferencias y eliminación de cuenta. */
export function ProfilePage() {
  const { profile } = useAuth();
  if (!profile) {
    return <div className="tq-app"><AppHeader /><p className="tq-boot" role="status">Cargando perfil…</p></div>;
  }
  return (
    <div className="tq-app">
      <AppHeader />
      <main className="tq-profile">
        <h1>Perfil y configuración</h1>
        <nav className="tq-profile__nav" aria-label="Secciones">
          <a href="#datos"><UserRound size={16} aria-hidden="true" /> Datos personales</a>
          <a href="#foto"><Camera size={16} aria-hidden="true" /> Fotografía</a>
          <a href="#seguridad"><KeyRound size={16} aria-hidden="true" /> Seguridad</a>
          <a href="#preferencias"><Volume2 size={16} aria-hidden="true" /> Sonido</a>
          <a href="#cuenta"><Trash2 size={16} aria-hidden="true" /> Eliminar cuenta</a>
        </nav>
        <div className="tq-profile__sections">
          <PersonalSection />
          <PhotoSection />
          <SecuritySection />
          <PreferencesSection />
          <DeleteSection />
        </div>
      </main>
    </div>
  );
}

function NoticeLine({ notice }: { notice: Notice }) {
  if (!notice) return null;
  return <p className={`tq-notice tq-notice--${notice.tone}`} role={notice.tone === 'error' ? 'alert' : 'status'}>{notice.text}</p>;
}

function PersonalSection() {
  const { profile, setProfile } = useAuth();
  const [notice, setNotice] = useState<Notice>(null);
  const [emailNotice, setEmailNotice] = useState<Notice>(null);
  const [busy, setBusy] = useState(false);
  if (!profile) return null;

  async function rename(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setBusy(true);
    try {
      setProfile(await profileService.rename(String(new FormData(e.currentTarget).get('displayName'))));
      setNotice({ tone: 'ok', text: 'Nombre actualizado.' });
    } catch (err) {
      setNotice({ tone: 'error', text: errorText(err, 'No se pudo guardar el nombre.') });
    } finally {
      setBusy(false);
    }
  }

  async function changeEmail(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    const formEl = e.currentTarget;
    setBusy(true);
    try {
      const r = await profileService.requestEmailChange(String(form.get('newEmail')), String(form.get('currentPassword')));
      setProfile({ ...profile!, pendingEmail: r.pendingEmail });
      formEl.reset();
      setEmailNotice({
        tone: 'ok',
        text: r.emailSent
          ? `Te enviamos un enlace a ${r.pendingEmail}. Ábrelo en los próximos 30 minutos para confirmar el cambio.`
          : 'Solicitud registrada. En este entorno no hay correo configurado: el enlace de verificación está en el registro del servidor.',
      });
    } catch (err) {
      setEmailNotice({ tone: 'error', text: errorText(err, 'No se pudo solicitar el cambio.') });
    } finally {
      setBusy(false);
    }
  }

  return (
    <section id="datos" className="tq-panel tq-profile__section" aria-labelledby="datos-title">
      <h2 id="datos-title">Datos personales</h2>
      <form className="tq-form tq-form--row" onSubmit={rename}>
        <div className="tq-field">
          <label htmlFor="p-name">Nombre visible</label>
          <input id="p-name" name="displayName" defaultValue={profile.displayName} minLength={2} maxLength={60} required />
        </div>
        <button type="submit" className="tq-btn tq-btn--ghost" disabled={busy}>Guardar nombre</button>
      </form>
      <NoticeLine notice={notice} />

      <div className="tq-profile__email">
        <p><Mail size={16} aria-hidden="true" /> <span>Correo actual: <strong>{profile.email}</strong></span></p>
        {profile.pendingEmail && (
          <p className="tq-profile__pending">
            Pendiente de verificar: <strong>{profile.pendingEmail}</strong>
            <button type="button" className="tq-link-btn" onClick={async () => setProfile(await profileService.cancelEmailChange())}>Cancelar</button>
          </p>
        )}
      </div>
      <form className="tq-form" onSubmit={changeEmail}>
        <div className="tq-grid-2">
          <div className="tq-field">
            <label htmlFor="p-newemail">Nuevo correo</label>
            <input id="p-newemail" name="newEmail" type="email" autoComplete="email" required maxLength={160} />
          </div>
          <div className="tq-field">
            <label htmlFor="p-emailpass">Contraseña actual</label>
            <input id="p-emailpass" name="currentPassword" type="password" autoComplete="current-password" required />
          </div>
        </div>
        <p className="tq-field__hint">Enviaremos un enlace al nuevo correo. Tu correo no cambia hasta que lo confirmes.</p>
        <button type="submit" className="tq-btn tq-btn--ghost tq-btn--fit" disabled={busy}>Cambiar correo</button>
      </form>
      <NoticeLine notice={emailNotice} />
    </section>
  );
}

function PhotoSection() {
  const { profile, setProfile } = useAuth();
  const input = useRef<HTMLInputElement>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [file, setFile] = useState<File | null>(null);
  const [notice, setNotice] = useState<Notice>(null);
  const [busy, setBusy] = useState(false);
  if (!profile) return null;

  function pick(f: File | undefined) {
    setNotice(null);
    if (!f) return;
    if (!['image/png', 'image/jpeg'].includes(f.type)) {
      setNotice({ tone: 'error', text: 'Usa una imagen PNG o JPG.' });
      return;
    }
    if (f.size > 3 * 1024 * 1024) {
      setNotice({ tone: 'error', text: 'La imagen supera los 3 MB.' });
      return;
    }
    setFile(f);
    setPreview(URL.createObjectURL(f));
  }

  async function upload() {
    if (!file) return;
    setBusy(true);
    try {
      setProfile(await profileService.uploadAvatar(file));
      setNotice({ tone: 'ok', text: 'Fotografía actualizada.' });
      setFile(null);
      setPreview(null);
    } catch (err) {
      setNotice({ tone: 'error', text: errorText(err, 'No se pudo subir la fotografía.') });
    } finally {
      setBusy(false);
      if (input.current) input.current.value = '';
    }
  }

  return (
    <section id="foto" className="tq-panel tq-profile__section" aria-labelledby="foto-title">
      <h2 id="foto-title">Fotografía de perfil</h2>
      <div className="tq-photo">
        <div className="tq-photo__preview">
          {preview ? <img src={preview} alt="Vista previa de la nueva fotografía" /> : <UserAvatar name={profile.displayName} url={profile.avatarUrl} size={112} />}
        </div>
        <div className="tq-photo__actions">
          <p className="tq-field__hint">PNG o JPG, hasta 3 MB. Se recorta en cuadrado y se guarda optimizada.</p>
          <div className="tq-row">
            <button type="button" className="tq-btn tq-btn--ghost" onClick={() => input.current?.click()} disabled={busy}>
              <Camera size={16} aria-hidden="true" /> {profile.avatarUrl ? 'Cambiar fotografía' : 'Subir fotografía'}
            </button>
            {file && <button type="button" className="tq-btn tq-btn--primary" onClick={() => void upload()} disabled={busy}>{busy ? 'Subiendo…' : 'Guardar'}</button>}
            {file && <button type="button" className="tq-btn tq-btn--quiet" onClick={() => { setFile(null); setPreview(null); }}>Descartar</button>}
            {!file && profile.avatarUrl && (
              <button type="button" className="tq-btn tq-btn--danger" disabled={busy}
                onClick={async () => { setProfile(await profileService.removeAvatar()); setNotice({ tone: 'ok', text: 'Fotografía eliminada.' }); }}>
                Eliminar
              </button>
            )}
          </div>
          <input ref={input} type="file" accept="image/png,image/jpeg" hidden onChange={(e) => pick(e.target.files?.[0])} />
        </div>
      </div>
      <NoticeLine notice={notice} />
    </section>
  );
}

function SecuritySection() {
  const { setProfile } = useAuth();
  const [notice, setNotice] = useState<Notice>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    const el = e.currentTarget;
    if (form.get('newPassword') !== form.get('confirmPassword')) {
      setNotice({ tone: 'error', text: 'La confirmación no coincide con la nueva contraseña.' });
      return;
    }
    setBusy(true);
    try {
      setProfile(await profileService.changePassword(String(form.get('currentPassword')), String(form.get('newPassword')),
        String(form.get('confirmPassword'))));
      el.reset();
      setNotice({ tone: 'ok', text: 'Contraseña actualizada. Se cerraron tus otras sesiones abiertas.' });
    } catch (err) {
      setNotice({ tone: 'error', text: errorText(err, 'No se pudo cambiar la contraseña.') });
    } finally {
      setBusy(false);
    }
  }

  return (
    <section id="seguridad" className="tq-panel tq-profile__section" aria-labelledby="seg-title">
      <h2 id="seg-title">Seguridad</h2>
      <form className="tq-form" onSubmit={submit}>
        <div className="tq-field">
          <label htmlFor="s-current">Contraseña actual</label>
          <input id="s-current" name="currentPassword" type="password" autoComplete="current-password" required />
        </div>
        <div className="tq-grid-2">
          <div className="tq-field">
            <label htmlFor="s-new">Nueva contraseña</label>
            <input id="s-new" name="newPassword" type="password" autoComplete="new-password" minLength={8} required />
          </div>
          <div className="tq-field">
            <label htmlFor="s-confirm">Confirmar nueva contraseña</label>
            <input id="s-confirm" name="confirmPassword" type="password" autoComplete="new-password" minLength={8} required />
          </div>
        </div>
        <p className="tq-field__hint">Mínimo 8 caracteres. Al cambiarla se cierran las demás sesiones.</p>
        <button type="submit" className="tq-btn tq-btn--ghost tq-btn--fit" disabled={busy}>Cambiar contraseña</button>
      </form>
      <NoticeLine notice={notice} />
    </section>
  );
}

function PreferencesSection() {
  const { settings, save, play } = useAudio();
  const [draft, setDraft] = useState<AudioSettings>(settings);
  const [notice, setNotice] = useState<Notice>(null);
  useEffect(() => setDraft(settings), [settings]);
  const set = (patch: Partial<AudioSettings>) => {
    setDraft((d) => ({ ...d, ...patch }));
    setNotice(null);
  };

  async function submit() {
    try {
      await save(draft);
      setNotice({ tone: 'ok', text: 'Preferencias guardadas en tu cuenta.' });
    } catch (err) {
      setNotice({ tone: 'error', text: errorText(err, 'No se pudieron guardar las preferencias.') });
    }
  }

  return (
    <section id="preferencias" className="tq-panel tq-profile__section" aria-labelledby="pref-title">
      <h2 id="pref-title">Sonido</h2>
      <label className="tq-switch">
        <input type="checkbox" checked={draft.enabled} onChange={(e) => set({ enabled: e.target.checked })} />
        <span>Sonidos activados</span>
      </label>
      <fieldset className="tq-sliders" disabled={!draft.enabled}>
        <legend className="visually-hidden">Volumen</legend>
        <div className="tq-slider">
          <label htmlFor="v-master">Volumen general</label>
          <input id="v-master" type="range" min={0} max={100} value={draft.masterVolume} onChange={(e) => set({ masterVolume: Number(e.target.value) })} />
          <output htmlFor="v-master" className="tabular">{draft.masterVolume}%</output>
        </div>
        <div className="tq-slider">
          <label htmlFor="v-fx">Volumen de efectos</label>
          <input id="v-fx" type="range" min={0} max={100} value={draft.effectsVolume} onChange={(e) => set({ effectsVolume: Number(e.target.value) })} />
          <output htmlFor="v-fx" className="tabular">{draft.effectsVolume}%</output>
        </div>
        <label className="tq-check">
          <input type="checkbox" checked={draft.countdownSounds} onChange={(e) => set({ countdownSounds: e.target.checked })} />
          Sonido de cuenta regresiva en preguntas y minijuegos
        </label>
      </fieldset>
      <p className="tq-field__hint">Durante una partida también puedes silenciar los efectos con el botón de volumen.</p>
      <div className="tq-row">
        <button type="button" className="tq-btn tq-btn--primary" onClick={() => void submit()}>Guardar preferencias</button>
        <button type="button" className="tq-btn tq-btn--coral" disabled={!draft.enabled} onClick={() => play('reward')}>Probar sonido</button>
      </div>
      <NoticeLine notice={notice} />
    </section>
  );
}

function DeleteSection() {
  const { clear } = useAuth();
  const navigate = useNavigate();
  const [notice, setNotice] = useState<Notice>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    if (!window.confirm('¿Eliminar tu cuenta definitivamente? No se puede deshacer.')) return;
    setBusy(true);
    try {
      await profileService.deleteAccount(String(form.get('currentPassword')), String(form.get('confirmation')));
      clear();
      navigate('/login', { replace: true });
    } catch (err) {
      setNotice({ tone: 'error', text: errorText(err, 'No se pudo eliminar la cuenta.') });
      setBusy(false);
    }
  }

  return (
    <section id="cuenta" className="tq-panel tq-profile__section tq-profile__danger" aria-labelledby="del-title">
      <h2 id="del-title">Eliminar cuenta</h2>
      <ul className="tq-consequences">
        <li>Tu nombre, correo y fotografía se borran y ya no podrás iniciar sesión.</li>
        <li>Las partidas en las que participaste se conservan para el resto, con tu nombre sustituido por «Participante eliminado».</li>
        <li>Si eres docente, tus cuestionarios y sus resultados históricos se conservan, pero dejarás de tener acceso.</li>
      </ul>
      <form className="tq-form" onSubmit={submit}>
        <div className="tq-grid-2">
          <div className="tq-field">
            <label htmlFor="d-pass">Contraseña actual</label>
            <input id="d-pass" name="currentPassword" type="password" autoComplete="current-password" required />
          </div>
          <div className="tq-field">
            <label htmlFor="d-confirm">Escribe ELIMINAR para confirmar</label>
            <input id="d-confirm" name="confirmation" required pattern="ELIMINAR" autoComplete="off" />
          </div>
        </div>
        <button type="submit" className="tq-btn tq-btn--danger tq-btn--fit" disabled={busy}>
          <Trash2 size={16} aria-hidden="true" /> Eliminar mi cuenta
        </button>
      </form>
      <NoticeLine notice={notice} />
    </section>
  );
}
