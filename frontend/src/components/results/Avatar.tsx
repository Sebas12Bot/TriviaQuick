interface Props {
  initials: string;
  name: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  tone?: 'gold' | 'silver' | 'bronze' | 'plain' | 'self';
}

/** Avatar de iniciales: sin fotos de perfil, la identidad es el nombre. */
export function Avatar({ initials, name, size = 'md', tone = 'plain' }: Props) {
  return (
    <span className={`tq-avatar tq-avatar--${size} tq-avatar--${tone}`} aria-hidden="true" title={name}>
      {initials}
    </span>
  );
}
