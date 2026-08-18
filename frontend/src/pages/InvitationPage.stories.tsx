import GroupAddIcon from '@mui/icons-material/GroupAdd';
import LinkOffIcon from '@mui/icons-material/LinkOff';
import { Box, Button, Card, CardContent, Chip, Container, Stack, Typography } from '@mui/material';
import type { Meta, StoryObj } from '@storybook/react-vite';

import { ErrorState, FullPageSpinner } from '../components/ui/QueryStates';

/**
 * S14 招待の受諾(`/invite/:token`)のUI合意用ストーリー。
 *
 * 本物のページ(`src/pages/InvitationPage.tsx`)はまだ存在しないため、Plan PRの例外として
 * インラインのプレースホルダー実装を置く(`04_development_process.md` §5.1.1、
 * `06_adr/12_story_first_existing_page_placeholder.md`)。実装PRで本物のページを
 * このレイアウトに合わせて作成し、ここを実importへ書き換える。
 *
 * 仕様は `.claude/07_plans/14_tournament_collaboration.md` §3。
 */

type InvitationView = 'default' | 'invalid' | 'alreadyMember' | 'loading' | 'error';

interface InvitationPagePlaceholderProps {
  view: InvitationView;
}

const SAMPLE = {
  tournamentName: '第30回実業団囲碁大会',
  gameType: '囲碁',
  eventDate: '2026年9月1日',
  expiresAt: '2026/08/19 12:00',
};

/** カード枠。375pxでは全幅、デスクトップでは480pxで中央寄せ(中央寄せカード1枚の構図はLoginPageと同じトーン) */
function InvitationCard({ children }: { children: React.ReactNode }) {
  return (
    <Container maxWidth={false} sx={{ py: 6, display: 'flex', justifyContent: 'center' }}>
      <Card variant="outlined" sx={{ width: '100%', maxWidth: 480 }}>
        <CardContent sx={{ p: 3 }}>{children}</CardContent>
      </Card>
    </Container>
  );
}

function InvitationPagePlaceholder({ view }: InvitationPagePlaceholderProps) {
  if (view === 'loading') {
    return <FullPageSpinner />;
  }

  if (view === 'error') {
    return (
      <InvitationCard>
        <ErrorState message="招待情報の取得に失敗しました" onRetry={() => undefined} />
      </InvitationCard>
    );
  }

  if (view === 'invalid') {
    return (
      <InvitationCard>
        <Stack spacing={2} sx={{ alignItems: 'center', textAlign: 'center' }}>
          <Box sx={{ fontSize: 48, color: 'text.disabled', lineHeight: 1 }}>
            <LinkOffIcon fontSize="inherit" />
          </Box>
          <Typography variant="h2" component="h1">
            この招待リンクは無効です
          </Typography>
          <Typography variant="body2" color="text.secondary">
            大会の運営者に新しいリンクの発行を依頼してください。
          </Typography>
          <Button variant="outlined" fullWidth>
            大会一覧へ
          </Button>
        </Stack>
      </InvitationCard>
    );
  }

  if (view === 'alreadyMember') {
    return (
      <InvitationCard>
        <Stack spacing={2} sx={{ alignItems: 'center', textAlign: 'center' }}>
          <Typography variant="h2" component="h1">
            すでにこの大会に参加しています
          </Typography>
          <Typography variant="h3" component="p">
            {SAMPLE.tournamentName}
          </Typography>
          <Button variant="contained" fullWidth>
            大会を開く
          </Button>
        </Stack>
      </InvitationCard>
    );
  }

  return (
    <InvitationCard>
      <Stack spacing={2} sx={{ alignItems: 'center', textAlign: 'center' }}>
        <Box sx={{ fontSize: 48, color: 'primary.main', lineHeight: 1 }}>
          <GroupAddIcon fontSize="inherit" />
        </Box>
        <Typography variant="h2" component="h1">
          大会の共同管理に招待されています
        </Typography>
        <Typography variant="h2" component="h2">
          {SAMPLE.tournamentName}
        </Typography>
        <Stack direction="row" spacing={1} sx={{ justifyContent: 'center' }}>
          <Chip label={SAMPLE.gameType} size="small" />
          <Chip label={SAMPLE.eventDate} size="small" variant="outlined" />
        </Stack>
        <Typography variant="body2" color="text.secondary">
          承諾すると、この大会の参加者管理・ラウンド進行・結果入力ができるようになります。
          大会設定の変更と大会の削除はできません。
        </Typography>
        <Typography variant="caption" color="text.secondary">
          この招待は {SAMPLE.expiresAt} まで有効です
        </Typography>
        <Stack spacing={1} sx={{ width: '100%', pt: 1 }}>
          <Button variant="contained" fullWidth>
            参加する
          </Button>
          <Button variant="text" fullWidth>
            キャンセル
          </Button>
        </Stack>
      </Stack>
    </InvitationCard>
  );
}

const meta: Meta<typeof InvitationPagePlaceholder> = {
  component: InvitationPagePlaceholder,
  parameters: {
    route: '/invite/sample-invite-token',
    routePath: '/invite/:token',
  },
};

export default meta;

type Story = StoryObj<typeof InvitationPagePlaceholder>;

/** 通常(承諾前。MBR-AC-015) */
export const Default: Story = {
  args: { view: 'default' },
};

/** 招待が無効(期限切れ・人数枠切れ・失効済み・不正トークンを区別せず同一表示。MBR-AC-004, MBR-AC-015) */
export const Invalid: Story = {
  args: { view: 'invalid' },
};

/** すでにメンバー(OWNER本人・既存MAINTAINER。MBR-AC-011, MBR-AC-015) */
export const AlreadyMember: Story = {
  args: { view: 'alreadyMember' },
};

/** ローディング */
export const Loading: Story = {
  args: { view: 'loading' },
};

/** 通信エラー(403の「無効」とは区別し、再試行を出す) */
export const ErrorOnLoad: Story = {
  args: { view: 'error' },
};

/** スマホ幅(375px)。招待リンクはスマホで開かれる可能性が高い */
export const Mobile: Story = {
  args: { view: 'default' },
  globals: { viewport: { value: '375-812' } },
};
