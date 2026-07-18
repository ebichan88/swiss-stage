import UploadFileIcon from '@mui/icons-material/UploadFile';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { useRef, useState } from 'react';

import { ApiError } from '../../../services/apiClient';

const PREVIEW_MAX_ROWS = 10;

interface CsvPreview {
  header: string[];
  rows: string[][];
  totalDataRows: number;
}

/** UTF-8 / Shift_JIS を自動判定してデコードする(バックエンドと同じ方針。プレビュー用) */
function decodeCsv(buffer: ArrayBuffer): string {
  try {
    return new TextDecoder('utf-8', { fatal: true }).decode(buffer);
  } catch {
    return new TextDecoder('shift_jis').decode(buffer);
  }
}

/** プレビュー用の簡易パース(クォート等の厳密な解釈はバックエンドが行う) */
function parseCsvPreview(text: string): CsvPreview {
  const lines = text
    .split(/\r\n|\r|\n/)
    .map((line) => line.trim())
    .filter((line) => line !== '');
  const header = (lines[0] ?? '').split(',');
  const dataRows = lines.slice(1);
  return {
    header,
    rows: dataRows.slice(0, PREVIEW_MAX_ROWS).map((line) => line.split(',')),
    totalDataRows: dataRows.length,
  };
}

export interface CsvImportDialogProps {
  open: boolean;
  loading: boolean;
  /** インポート失敗時のエラー(行番号付き details を表示する)。成功時は null */
  error: unknown;
  onImport: (file: File) => void;
  onClose: () => void;
}

/** CSVインポート(02_component_design.md §4: D&D+ファイル選択の両対応、取り込み前プレビュー) */
export function CsvImportDialog({ open, loading, error, onImport, onClose }: CsvImportDialogProps) {
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<CsvPreview | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const handleFile = (selected: File) => {
    setFile(selected);
    void selected.arrayBuffer().then((buffer) => {
      setPreview(parseCsvPreview(decodeCsv(buffer)));
    });
  };

  const resetState = () => {
    setFile(null);
    setPreview(null);
    setDragOver(false);
  };

  const apiError = error instanceof ApiError ? error : null;

  return (
    <Dialog
      open={open}
      onClose={loading ? undefined : onClose}
      maxWidth="sm"
      fullWidth
      slotProps={{ transition: { onExited: resetState } }}
    >
      <DialogTitle>参加者をCSVでインポート</DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" gutterBottom>
          ヘッダー行(氏名,所属,段級位)が必要です。グループ分けする場合は4列目に「グループ」列
          を追加できます(定義済みのグループ名を指定)。文字コードは UTF-8 / Shift_JIS
          に対応。1行でもエラーがあると全行取り込まれません。
        </Typography>
        <Box
          onDragOver={(e) => {
            e.preventDefault();
            setDragOver(true);
          }}
          onDragLeave={() => setDragOver(false)}
          onDrop={(e) => {
            e.preventDefault();
            setDragOver(false);
            const dropped = e.dataTransfer.files[0];
            if (dropped) {
              handleFile(dropped);
            }
          }}
          sx={{
            mt: 1,
            p: 3,
            border: '2px dashed',
            borderColor: dragOver ? 'primary.main' : 'divider',
            borderRadius: 2,
            textAlign: 'center',
            bgcolor: dragOver ? 'primary.light' : 'transparent',
          }}
        >
          <UploadFileIcon color="action" sx={{ fontSize: 40 }} />
          <Typography variant="body2" gutterBottom>
            {file ? file.name : 'ここにCSVファイルをドラッグ&ドロップ'}
          </Typography>
          <Button variant="outlined" size="small" onClick={() => inputRef.current?.click()}>
            ファイルを選択
          </Button>
          <input
            ref={inputRef}
            type="file"
            accept=".csv,text/csv"
            hidden
            onChange={(e) => {
              const selected = e.target.files?.[0];
              if (selected) {
                handleFile(selected);
              }
              e.target.value = '';
            }}
          />
        </Box>

        {preview && (
          <Box sx={{ mt: 2 }}>
            <Typography variant="body2" gutterBottom>
              プレビュー(全{preview.totalDataRows}行
              {preview.totalDataRows > PREVIEW_MAX_ROWS && `、先頭${PREVIEW_MAX_ROWS}行を表示`})
            </Typography>
            <TableContainer sx={{ maxHeight: 240, overflowX: 'auto' }}>
              <Table size="small" stickyHeader>
                <TableHead>
                  <TableRow>
                    {preview.header.map((cell, i) => (
                      <TableCell key={i}>{cell}</TableCell>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {preview.rows.map((row, i) => (
                    <TableRow key={i}>
                      {preview.header.map((_, j) => (
                        <TableCell key={j}>{row[j] ?? ''}</TableCell>
                      ))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Box>
        )}

        {apiError && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {apiError.message}
            {apiError.details && (
              <Box component="ul" sx={{ m: 0, pl: 2.5 }}>
                {apiError.details.map((detail, i) => (
                  <li key={i}>
                    {detail.field}: {detail.reason}
                  </li>
                ))}
              </Box>
            )}
          </Alert>
        )}
      </DialogContent>
      <DialogActions>
        <Button variant="outlined" onClick={onClose} disabled={loading}>
          キャンセル
        </Button>
        <Button
          variant="contained"
          disabled={!file || loading}
          onClick={() => file && onImport(file)}
          startIcon={loading ? <CircularProgress size={16} color="inherit" /> : undefined}
        >
          インポートする
        </Button>
      </DialogActions>
    </Dialog>
  );
}
