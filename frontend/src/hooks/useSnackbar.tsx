import { Alert, Snackbar } from '@mui/material';
import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import type { ReactNode } from 'react';

/**
 * Snackbarの一元管理(02_component_design.md §2)。
 * 成功=3秒自動クローズ / エラー=手動クローズ。同時に複数表示せず後発が上書きする。
 */

interface SnackbarState {
  message: string;
  severity: 'success' | 'error';
}

interface SnackbarContextValue {
  showSuccess: (message: string) => void;
  showError: (message: string) => void;
}

const SnackbarContext = createContext<SnackbarContextValue | null>(null);

export function SnackbarProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<SnackbarState | null>(null);
  const [open, setOpen] = useState(false);

  const showSuccess = useCallback((message: string) => {
    setState({ message, severity: 'success' });
    setOpen(true);
  }, []);

  const showError = useCallback((message: string) => {
    setState({ message, severity: 'error' });
    setOpen(true);
  }, []);

  const value = useMemo(() => ({ showSuccess, showError }), [showSuccess, showError]);

  const handleClose = (_event: unknown, reason?: string) => {
    // エラーは手動クローズのみ(画面クリックで消えて見逃すのを防ぐ)
    if (state?.severity === 'error' && reason === 'clickaway') {
      return;
    }
    setOpen(false);
  };

  return (
    <SnackbarContext.Provider value={value}>
      {children}
      <Snackbar
        open={open}
        autoHideDuration={state?.severity === 'success' ? 3000 : null}
        onClose={handleClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert
          severity={state?.severity ?? 'success'}
          onClose={() => setOpen(false)}
          variant="filled"
          sx={{ width: '100%' }}
        >
          {state?.message}
        </Alert>
      </Snackbar>
    </SnackbarContext.Provider>
  );
}

export function useSnackbar(): SnackbarContextValue {
  const context = useContext(SnackbarContext);
  if (!context) {
    throw new Error('useSnackbar は SnackbarProvider の内側で使ってください');
  }
  return context;
}
