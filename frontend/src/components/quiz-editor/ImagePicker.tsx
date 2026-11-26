import { useRef, useState } from 'react';
import { Film, ImagePlus, X } from 'lucide-react';
import { ApiError } from '../../services/api';
import { triviaService } from '../../services/triviaService';

interface Props {
  label: string;
  /** URL del archivo actual (imagen o video). */
  imageUrl: string | null;
  kind?: 'image' | 'video';
  disabled?: boolean;
  onChange: (value: { key: string; url: string } | null) => void;
}

const LIMITS = {
  image: { bytes: 10 * 1024 * 1024, accept: 'image/png,image/jpeg,image/gif,image/webp,image/heic,image/heif,.heic,.heif',
    hint: 'PNG, JPG, WebP o HEIC (fotos de celular), máx. 10 MB' },
  video: { bytes: 50 * 1024 * 1024, accept: 'video/mp4,video/quicktime,video/webm,.mov',
    hint: 'MP4, MOV o WebM, máx. 50 MB' },
};

/** Selector de imagen o video. El servidor valida el contenido real del archivo. */
export function ImagePicker({ label, imageUrl, kind = 'image', disabled, onChange }: Props) {
  const input = useRef<HTMLInputElement>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const limit = LIMITS[kind];

  async function onFile(file: File | undefined) {
    if (!file) return;
    if (file.size > limit.bytes) {
      setError(`El archivo supera los ${limit.bytes / (1024 * 1024)} MB.`);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      onChange(kind === 'video' ? await triviaService.uploadVideo(file) : await triviaService.uploadImage(file));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo subir el archivo.');
    } finally {
      setBusy(false);
      if (input.current) input.current.value = '';
    }
  }

  const Icon = kind === 'video' ? Film : ImagePlus;
  return (
    <div className="tq-image-picker">
      <span className="tq-image-picker__label">{label}</span>
      {imageUrl ? (
        <div className="tq-image-picker__preview">
          {kind === 'video'
            ? <video src={imageUrl} controls preload="metadata" playsInline />
            : <img src={imageUrl} alt="" />}
          {!disabled && (
            <button type="button" className="tq-image-picker__remove" onClick={() => onChange(null)}
              aria-label={kind === 'video' ? 'Quitar video' : 'Quitar imagen'}>
              <X size={16} aria-hidden="true" />
            </button>
          )}
        </div>
      ) : (
        <button type="button" className="tq-image-picker__drop" disabled={disabled || busy} onClick={() => input.current?.click()}>
          <Icon size={20} aria-hidden="true" />
          {busy ? (kind === 'video' ? 'Subiendo video…' : 'Subiendo…') : `Añadir ${kind === 'video' ? 'video' : 'imagen'} (${limit.hint})`}
        </button>
      )}
      <input ref={input} type="file" accept={limit.accept} hidden onChange={(e) => void onFile(e.target.files?.[0])} />
      {error && <p className="tq-field__error" role="alert">{error}</p>}
    </div>
  );
}
