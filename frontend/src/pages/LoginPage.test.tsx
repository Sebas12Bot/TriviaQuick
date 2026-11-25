import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { LoginPage } from './LoginPage';
import { RegisterPage } from './RegisterPage';
import { ApiError } from '../services/api';

const login = vi.fn();
const register = vi.fn();
vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({ user: null, login, register }),
}));

const mood = () => document.querySelector('.tq-mascot')?.getAttribute('data-mood');

describe('Login con Coby', () => {
  beforeEach(() => {
    login.mockReset();
    register.mockReset();
  });

  it('saluda al entrar, mira el correo y se tapa los ojos con la contraseña', async () => {
    render(<MemoryRouter><LoginPage /></MemoryRouter>);
    expect(mood()).toBe('greet');
    fireEvent.focus(screen.getByLabelText('Correo'));
    expect(mood()).toBe('watch');
    fireEvent.focus(screen.getByLabelText('Contraseña'));
    expect(mood()).toBe('shy');
    await userEvent.click(screen.getByRole('button', { name: 'Mostrar contraseña' }));
    expect(mood()).toBe('peek');
    expect(screen.getByLabelText('Contraseña')).toHaveAttribute('type', 'text');
  });

  it('muestra confusión ante un error de acceso', async () => {
    login.mockRejectedValue(new ApiError(401, 'INVALID_CREDENTIALS', 'Correo o contraseña incorrectos.'));
    render(<MemoryRouter><LoginPage /></MemoryRouter>);
    await userEvent.type(screen.getByLabelText('Correo'), 'a@b.com');
    await userEvent.type(screen.getByLabelText('Contraseña'), 'xxxxxxxx');
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Correo o contraseña incorrectos.');
    expect(mood()).toBe('error');
  });

  it('el formulario funciona aunque el modelo 3D no esté disponible (muestra la silueta)', () => {
    render(<MemoryRouter><LoginPage /></MemoryRouter>);
    expect(document.querySelector('.tq-mascot__fallback')).not.toBeNull();
    expect(screen.getByRole('button', { name: 'Entrar' })).toBeEnabled();
  });
});

describe('Registro con Coby', () => {
  it('tiene una pose propia y celebra al terminar', async () => {
    register.mockResolvedValue({ id: 1, email: 'a@b.com', displayName: 'Ana', role: 'STUDENT' });
    render(<MemoryRouter><RegisterPage /></MemoryRouter>);
    await new Promise((r) => setTimeout(r, 0));
    fireEvent.focus(screen.getByLabelText('Nombre visible'));
    expect(mood()).toBe('watch');
    fireEvent.blur(screen.getByLabelText('Nombre visible'));
    await userEvent.type(screen.getByLabelText('Nombre visible'), 'Ana');
    await userEvent.type(screen.getByLabelText('Correo'), 'a@b.com');
    await userEvent.type(screen.getByLabelText('Contraseña'), 'contrasena1');
    await userEvent.type(screen.getByLabelText('Confirmar contraseña'), 'contrasena1');
    await userEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }));
    expect(await screen.findByText('¡Bienvenido a TriviaQuick!')).toBeInTheDocument();
    expect(mood()).toBe('celebrate');
  });

  it('avisa si las contraseñas no coinciden', async () => {
    render(<MemoryRouter><RegisterPage /></MemoryRouter>);
    await userEvent.type(screen.getByLabelText('Nombre visible'), 'Ana');
    await userEvent.type(screen.getByLabelText('Correo'), 'a@b.com');
    await userEvent.type(screen.getByLabelText('Contraseña'), 'contrasena1');
    await userEvent.type(screen.getByLabelText('Confirmar contraseña'), 'otra-cosa-9');
    await userEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }));
    expect(screen.getByText('Las contraseñas no coinciden.')).toBeInTheDocument();
    expect(mood()).toBe('error');
  });
});
