/**
 * Silueta plana de Coby: se muestra mientras carga el modelo 3D y como respaldo si el
 * navegador no admite WebGL. Mismas proporciones y colores que el personaje.
 */
export function CobraSilhouette({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 200 220" aria-hidden="true">
      <ellipse cx="100" cy="206" rx="70" ry="10" fill="#071a3b" opacity="0.25" />
      <path d="M40 196c-6-20 18-34 58-34s62 12 58 30c-3 14-30 18-60 17-32-1-52-5-56-13z" fill="#2a93c4" />
      <path d="M96 168c-14-22-10-46 4-70" stroke="#38b6dd" strokeWidth="30" strokeLinecap="round" fill="none" />
      <path d="M58 92c-8-34 16-62 42-62s50 28 42 62c-6 24-24 36-42 36s-36-12-42-36z" fill="#38b6dd" />
      <path d="M70 92c-4-22 12-42 30-42s34 20 30 42c-4 16-16 24-30 24s-26-8-30-24z" fill="#ff806f" />
      <circle cx="100" cy="66" r="28" fill="#38b6dd" />
      <ellipse cx="90" cy="64" rx="8" ry="9.5" fill="#fff" />
      <ellipse cx="110" cy="64" rx="8" ry="9.5" fill="#fff" />
      <circle cx="91" cy="66" r="4.6" fill="#071a3b" />
      <circle cx="111" cy="66" r="4.6" fill="#071a3b" />
      <path d="M94 80q6 5 12 0" stroke="#164e83" strokeWidth="2.6" fill="none" strokeLinecap="round" />
    </svg>
  );
}
