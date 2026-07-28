import { Fragment } from 'react';
import { Box, Table, TableBody, TableCell, TableRow } from '@mui/material';

import type { CardLayout, MatchCard } from './matchCardData';
import { avoidBreakSx, breakAfterPageSx, writableGridSx } from './printSx';

export interface TeamMatchCardSheetProps {
  /** `chunkIntoPages()` 済み。1要素=1ページ分のカード */
  pages: MatchCard[][];
  layout: CardLayout;
}

/**
 * 団体戦の対局カード1枚(1チーム分)。実業団囲碁大会の対局カード様式に倣い、ラウンドを列・
 * 記入項目(相手/チーム勝敗/個人勝敗)を行にした転置レイアウトにする。左にチーム番号(No.)を
 * 大きく、下にチーム勝敗・個人勝敗の集計欄を置く。相手・勝敗はすべて手書き記入するため空欄で出す。
 * メンバーの個人名は一切出さない(04_screen_transition_design.md §5-2)
 */
function TeamCard({ card }: { card: MatchCard }) {
  const rounds = Array.from({ length: card.rowCount }, (_, i) => i + 1);
  const boardCount = card.boardLabels.length;

  return (
    <Box
      sx={(theme) => ({
        border: '1.5px solid',
        borderColor: 'text.primary',
        fontSize: theme.print.cardFontSize,
        ...avoidBreakSx,
      })}
    >
      <Table size="small" sx={[{ '& td': { p: 0.25 } }, writableGridSx]}>
        <TableBody>
          {/* チーム名行。No.は全行にまたがって左に大きく表示する */}
          <TableRow>
            <TableCell
              rowSpan={6}
              sx={{ width: '14mm', fontWeight: 700, fontSize: '1.6em', whiteSpace: 'nowrap' }}
            >
              {card.groupName ? `${card.groupName}-` : ''}
              {card.entryOrder}
            </TableCell>
            <TableCell colSpan={1 + rounds.length * boardCount} sx={{ fontWeight: 600 }}>
              {card.name}
            </TableCell>
          </TableRow>
          {/* ラウンド見出し */}
          <TableRow>
            <TableCell />
            {rounds.map((round) => (
              <TableCell key={round} colSpan={boardCount}>
                {round}回戦
              </TableCell>
            ))}
          </TableRow>
          {/* 相手(手書き) */}
          <TableRow sx={(theme) => ({ height: theme.print.cardWritableRowHeight })}>
            <TableCell>相手</TableCell>
            {rounds.map((round) => (
              <TableCell key={round} colSpan={boardCount} />
            ))}
          </TableRow>
          {/* チーム勝敗(手書き ○×) */}
          <TableRow sx={(theme) => ({ height: theme.print.cardWritableRowHeight })}>
            <TableCell>チーム勝敗</TableCell>
            {rounds.map((round) => (
              <TableCell key={round} colSpan={boardCount} />
            ))}
          </TableRow>
          {/* 個人勝敗の見出し(主将・副将…) */}
          <TableRow>
            <TableCell rowSpan={2}>個人勝敗</TableCell>
            {rounds.map((round) => (
              <Fragment key={round}>
                {card.boardLabels.map((label) => (
                  <TableCell key={label}>{label}</TableCell>
                ))}
              </Fragment>
            ))}
          </TableRow>
          {/* 個人勝敗の記入欄(手書き ○×) */}
          <TableRow sx={(theme) => ({ height: theme.print.cardWritableRowHeight })}>
            {rounds.map((round) => (
              <Fragment key={round}>
                {card.boardLabels.map((label) => (
                  <TableCell key={label} />
                ))}
              </Fragment>
            ))}
          </TableRow>
        </TableBody>
      </Table>
      {/* 集計欄。チーム勝敗数・個人勝敗合計を手書きする */}
      <Box sx={{ display: 'flex', borderTop: '1.5px solid', borderColor: 'text.primary' }}>
        <Box sx={{ flex: 1, p: 0.5, textAlign: 'center' }}>チーム勝敗合計 →　　　　勝</Box>
        <Box
          sx={{
            flex: 1,
            p: 0.5,
            textAlign: 'center',
            borderLeft: '1px solid',
            borderColor: 'text.primary',
          }}
        >
          個人勝敗合計 →　　　　勝
        </Box>
      </Box>
    </Box>
  );
}

/**
 * 団体戦の対局カード(印刷)。大会前に全チーム分を一括印刷する。横に広い転置レイアウトのため、
 * A4 1枚に `layout.columns × layout.rows` 枚(既定1列×3枚)を面付けする
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
