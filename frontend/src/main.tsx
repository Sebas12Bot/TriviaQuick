import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import '@fontsource-variable/archivo/wdth.css';
import '@fontsource-variable/onest';
import './styles/theme.css';
import './styles/app.css';
import './styles/components.css';
import { App } from './App';
import { AuthProvider } from './context/AuthContext';
import { AudioProvider } from './context/AudioContext';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <AudioProvider>
          <App />
        </AudioProvider>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
);
