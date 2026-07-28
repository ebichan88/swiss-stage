import { Box, Table, TableBody, TableCell, TableRow } from '@mui/material';

import type { CardLayout, MatchCard } from './matchCardData';
import { avoidBreakSx, breakAfterPageSx, writableGridSx } from './printSx';

export interface MatchCardSheetProps {
  /** `chunkIntoPages()` 済み。1要素=1ページ分のカード */
  pages: MatchCard[][];
  layout: CardLayout;
}

/**
 * 個人戦の対局カード1枚。団体戦カードと同じくラウンドを列・記入項目(相手/勝敗)を行にした
 * 転置レイアウトにする。左にNo.を大きく、氏名の横に段級位の枠を置き、下に勝敗合計の集計欄を置く。
 * 団体戦との違いは、個人勝敗(ボード別)の行が無いこと・「チーム勝敗」ではなく「勝敗」であること。
 * 相手・勝敗はすべて手書き記入するため空欄で出す
 */
function IndividualCard({ card }: { card: MatchCard }) {
  const rounds = Array.from({ length: card.rowCount }, (_, i) => i + 1);

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
          {/* 氏名・段級位行。No.は全行にまたがって左に大きく表示する */}
          <TableRow>
            <TableCell
              rowSpan={4}
              sx={{ width: '12mm', fontWeight: 700, fontSize: '1.6em', whiteSpace: 'nowrap' }}
            >
              {card.groupName ? `${card.groupName}-` : ''}
              {card.entryOrder}
            </TableCell>
            <TableCell colSpan={rounds.length} sx={{ fontWeight: 600 }}>
              {card.name}
              {card.organization ? `(${card.organization})` : ''}
            </TableCell>
            <TableCell sx={{ whiteSpace: 'nowrap' }}>{card.rankText}</TableCell>
          </TableRow>
          {/* ラウンド見出し */}
          <TableRow>
            <TableCell />
            {rounds.map((round) => (
              <TableCell key={round}>{round}回戦</TableCell>
            ))}
          </TableRow>
          {/* 相手(手書き) */}
          <TableRow sx={(theme) => ({ height: theme.print.cardWritableRowHeight })}>
            <TableCell>相手</TableCell>
            {rounds.map((round) => (
              <TableCell key={round} />
            ))}
          </TableRow>
          {/* 勝敗(手書き ○×) */}
          <TableRow sx={(theme) => ({ height: theme.print.cardWritableRowHeight })}>
            <TableCell>勝敗</TableCell>
            {rounds.map((round) => (
              <TableCell key={round} />
            ))}
          </TableRow>
        </TableBody>
      </Table>
      {/* 集計欄。勝敗数を手書きする */}
      <Box
        sx={{ p: 0.5, textAlign: 'center', borderTop: '1.5px solid', borderColor: 'text.primary' }}
      >
        勝敗合計 →　　　　勝
      </Box>
    </Box>
  );
}

/**
 * 個人戦の対局カード(印刷)。大会前に全員分を一括印刷し、対戦相手・結果は参加者が手書きする。
 * A4 1枚に `layout.columns × layout.rows` 枚(既定2列×6枚)を面付けする
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
