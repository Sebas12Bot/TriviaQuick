import { useRef, useState } from 'react';
import { ImagePlus, X } from 'lucide-react';
import { ApiError } from '../../services/api';
import { triviaService } from '../../services/triviaService';

interface Props {
  label: string;
  imageUrl: string | null;
  disabled?: boolean;
  onChange: (value: { key: string; url: string } | null) => void;
}

/** Selector de imagen (PNG o JPG). El servidor valida el contenido real y la re-codifica. */
export function ImagePicker({ label, imageUrl, disabled, onChange }: Props) {
  const input = useRef<HTMLInputElement>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onFile(file: File | undefined) {
    if (!file) return;
    if (file.size > 3 * 1024 * 1024) {
      setError('La imagen supera los 3 MB.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      onChange(await triviaService.uploadImage(file));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo subir la imagen.');
    } finally {
      setBusy(false);
      if (input.current) input.current.value = '';
    }
  }

  return (
    <div className="tq-image-picker">
      <span className="tq-image-picker__label">{label}</span>
      {imageUrl ? (
        <div className="tq-image-picker__preview">
          <img src={imageUrl} alt="" />
          {!disabled && (
            <button type="button" className="tq-image-picker__remove" onClick={() => onChange(null)} aria-label="Quitar imagen">
              <X size={16} aria-hidden="true" />
            </button>
          )}
        </div>
      ) : (
        <button type="button" className="tq-image-picker__drop" disabled={disabled || busy} onClick={() => input.current?.click()}>
          <ImagePlus size={20} aria-hidden="true" />
          {busy ? 'Subiendo…' : 'Añadir imagen (PNG o JPG, máx. 3 MB)'}
        </button>
      )}
      <input ref={input} type="file" accept="image/png,image/jpeg" hidden onChange={(e) => void onFile(e.target.files?.[0])} />
      {error && <p className="tq-field__error" role="alert">{error}</p>}
    </div>
  );
}
