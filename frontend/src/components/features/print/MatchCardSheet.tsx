import { Box, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';

import type { CardLayout, MatchCard } from './matchCardData';
import { avoidBreakSx, breakAfterPageSx, writableGridSx } from './printSx';

export interface MatchCardSheetProps {
  /** `chunkIntoPages()` 済み。1要素=1ページ分のカード */
  pages: MatchCard[][];
  layout: CardLayout;
}

function IndividualCard({ card }: { card: MatchCard }) {
  return (
    <Box
      sx={(theme) => ({
        // A8で切り取る想定のため外枠は破線。既定のdivider色は薄すぎて見えないため text.primary
        border: '1px dashed',
        borderColor: 'text.primary',
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
        {card.organization ? `(${card.organization})` : ''} {card.rankText}
      </Typography>
      <Table size="small" sx={[{ fontSize: 'inherit', '& td, & th': { p: 0.25 } }, writableGridSx]}>
        <TableHead>
          <TableRow>
            <TableCell>R</TableCell>
            <TableCell>卓</TableCell>
            <TableCell>相手</TableCell>
            <TableCell>結果</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {Array.from({ length: card.rowCount }, (_, i) => (
            <TableRow key={i}>
              <TableCell>{i + 1}</TableCell>
              <TableCell />
              <TableCell />
              <TableCell />
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Box>
  );
}

/**
 * 個人戦の対局カード(印刷)。大会前に全員分を一括印刷し、卓番号・対戦相手・結果は参加者が手書きする。
 * A4 1枚に `layout.columns × layout.rows` 枚を面付けする(`matchCardData.ts` の `decideLayout()`)
 */
export function MatchCardSheet({ pages, layout }: MatchCardSheetProps) {
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
              <IndividualCard key={card.entryOrder} card={card} />
            ))}
          </Box>
        </Box>
      ))}
    </>
  );
}
