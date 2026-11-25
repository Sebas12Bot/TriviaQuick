/** Foto de perfil o, si no hay, las iniciales sobre un disco azul con aro coral. */
export function UserAvatar({ name, url, size = 36 }: { name: string; url: string | null; size?: number }) {
  const initials = name.trim().split(/\s+/).slice(0, 2).map((w) => w[0] ?? '').join('').toUpperCase() || '?';
  return (
    <span className="tq-user-avatar" style={{ width: size, height: size, fontSize: size * 0.38 }} aria-hidden="true">
      {url ? <img src={url} alt="" width={size} height={size} /> : initials}
    </span>
  );
}
