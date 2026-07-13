import {
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  TextField,
} from '@mui/material';
import { useState } from 'react';

export interface ConfirmDialogProps {
  open: boolean;
  title: string;
  message: string;
  /** 確定ボタンのラベル。動詞で書く(「OK」ではなく「削除する」等) */
  confirmLabel: string;
  /** 破壊的操作は 'error'(既定)。ラウンド確定等の非破壊確認は 'primary' */
  confirmColor?: 'error' | 'primary';
  /** 指定すると、この文字列の入力が一致するまで確定できない(重大確認: 大会削除等) */
  requiredText?: string;
  loading?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

/** 破壊的操作の確認ダイアログ(02_component_design.md §2)。window.confirm は使わない */
export function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel,
  confirmColor = 'error',
  requiredText,
  loading = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  const [inputText, setInputText] = useState('');
  const confirmDisabled = loading || (requiredText !== undefined && inputText !== requiredText);

  return (
    <Dialog
      open={open}
      onClose={loading ? undefined : onCancel}
      maxWidth="xs"
      fullWidth
      // 閉じてもunmountされないため、閉じ切ったタイミングで入力をリセットする
      slotProps={{ transition: { onExited: () => setInputText('') } }}
    >
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <DialogContentText>{message}</DialogContentText>
        {requiredText !== undefined && (
          <TextField
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            label={`確認のため「${requiredText}」と入力`}
            fullWidth
            margin="normal"
            autoFocus
          />
        )}
      </DialogContent>
      <DialogActions>
        <Button variant="outlined" onClick={onCancel} disabled={loading}>
          キャンセル
        </Button>
        <Button
          variant="contained"
          color={confirmColor}
          onClick={onConfirm}
          disabled={confirmDisabled}
          startIcon={loading ? <CircularProgress size={16} color="inherit" /> : undefined}
        >
          {confirmLabel}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
