import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import { Box, Tooltip } from '@mui/material';

export interface TableHeaderTooltipProps {
  label: string;
  tooltip: string;
}

/** テーブル見出しラベルの右にinfoアイコンを添え、ホバーで補足説明を表示する */
export function TableHeaderTooltip({ label, tooltip }: TableHeaderTooltipProps) {
  return (
    <Box component="span" sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5 }}>
      {label}
      <Tooltip title={tooltip}>
        <InfoOutlinedIcon fontSize="small" sx={{ verticalAlign: 'middle' }} />
      </Tooltip>
    </Box>
  );
}
