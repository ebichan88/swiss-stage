import { Box, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';

import type { CardLayout, MatchCard } from './matchCardData';
import { avoidBreakSx, breakAfterPageSx } from './printSx';

export interface TeamMatchCardSheetProps {
  /** `chunkIntoPages()` 済み。1要素=1ページ分のカード */
  pages: MatchCard[][];
  layout: CardLayout;
}

function TeamCard({ card }: { card: MatchCard }) {
  return (
    <Box
      sx={(theme) => ({
        border: '1px dashed',
        borderColor: 'divider',
        p: 0.5,
        fontSize: theme.print.cardFontSize,
        ...avoidBreakSx,
      })}
    >
      <Typography sx={{ fontSize: 'inherit', fontWeight: 600 }}>
        {card.groupName ? `[${card.groupName}] ` : ''}
        No.{card.entryOrder}
      </Typography>
      <Typography
        sx={{
          fontSize: 'inherit',
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
        }}
      >
        {card.name}
      </Typography>
      <Table size="small" sx={{ fontSize: 'inherit' }}>
        <TableHead>
          <TableRow>
            <TableCell sx={{ fontSize: 'inherit', p: 0.25 }}>R</TableCell>
            <TableCell sx={{ fontSize: 'inherit', p: 0.25 }}>卓</TableCell>
            <TableCell sx={{ fontSize: 'inherit', p: 0.25 }}>対戦チーム</TableCell>
            {card.boardLabels.map((label) => (
              <TableCell key={label} sx={{ fontSize: 'inherit', p: 0.25 }}>
                {label}
              </TableCell>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {Array.from({ length: card.rowCount }, (_, i) => (
            <TableRow key={i}>
              <TableCell sx={{ fontSize: 'inherit', p: 0.25 }}>{i + 1}</TableCell>
              <TableCell sx={{ fontSize: 'inherit', p: 0.25 }} />
              <TableCell sx={{ fontSize: 'inherit', p: 0.25 }} />
              {card.boardLabels.map((label) => (
                <TableCell key={label} sx={{ fontSize: 'inherit', p: 0.25 }} />
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Box>
  );
}

/**
 * 団体戦の対局カード(印刷)。1チーム1枚・チーム名+ボード役割(主将・副将…)欄のみで構成し、
 * メンバーの個人名は一切出さない(04_screen_transition_design.md §5-2)。
 * ボード列が入るため個人戦より広い面が必要で、A4 1枚に `layout.columns × layout.rows` 枚(既定8面)を面付けする
 */
export function TeamMatchCardSheet({ pages, layout }: TeamMatchCardSheetProps) {
  return (
    <>
      {pages.map((cards, pageIndex) => (
        <Box key={pageIndex} sx={pageIndex < pages.length - 1 ? breakAfterPageSx : undefined}>
          <Box
            sx={(theme) => ({
              display: 'grid',
              gridTemplateColumns: `repeat(${layout.columns}, 1fr)`,
              gap: theme.print.cardGap,
            })}
          >
            {cards.map((card) => (
              <TeamCard key={card.entryOrder} card={card} />
            ))}
          </Box>
        </Box>
      ))}
    </>
  );
}
