import AutorenewIcon from '@mui/icons-material/Autorenew';
import LeaderboardIcon from '@mui/icons-material/Leaderboard';
import PhoneIphoneIcon from '@mui/icons-material/PhoneIphone';
import { Box, Button, Container, Stack, Typography } from '@mui/material';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { Link } from 'react-router-dom';

import { paths } from '../routes';

/**
 * S01 トップ(LP)。Plan PR時点のプレースホルダー実装(Issue #173、
 * `04_development_process.md` §5.1の例外)。`TopPage.tsx` 本体はまだ旧レイアウトのため、
 * ここに提案レイアウトを直接書く。実装PRで `TopPage.tsx` を書き換え、このストーリーを
 * 実importへ差し替える(`.claude/07_plans/11_login_top_redesign.md` 参照)。
 */
const features = [
  {
    icon: AutorenewIcon,
    title: 'スイス方式マッチングを自動生成',
    description: '再戦禁止・BYE重複禁止を守りながら、次ラウンドの組み合わせを自動で作成します。',
  },
  {
    icon: LeaderboardIcon,
    title: '結果集計・順位表示を自動化',
    description: '対局結果を入力するだけで、勝点・SOS/SOSOSに基づく順位が即座に反映されます。',
  },
  {
    icon: PhoneIphoneIcon,
    title: '参加者はログイン不要',
    description: '共有URLをスマホで開くだけで、自分の対局・結果入力・順位を確認できます。',
  },
];

function TopPagePlaceholder() {
  return (
    <Container component="main" maxWidth="sm">
      <Box sx={{ py: 8, textAlign: 'center' }}>
        <Box
          component="img"
          src="/swiss-stage.svg"
          alt=""
          sx={{ height: 64, width: 64, mb: 2, borderRadius: 1 }}
        />
        <Typography variant="h1" gutterBottom>
          Swiss Stage
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 1 }}>
          大会といえば Swiss Stage
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 5 }}>
          囲碁・将棋大会の運営をもっとシンプルに
        </Typography>

        <Stack spacing={3} sx={{ mb: 5, textAlign: 'left' }}>
          {features.map(({ icon: Icon, title, description }) => (
            <Stack key={title} direction="row" spacing={2} sx={{ alignItems: 'flex-start' }}>
              <Icon color="primary" sx={{ mt: 0.5 }} />
              <Box>
                <Typography variant="subtitle1" component="p">
                  {title}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {description}
                </Typography>
              </Box>
            </Stack>
          ))}
        </Stack>

        <Stack direction="row" spacing={1.5} sx={{ justifyContent: 'center', mb: 4 }}>
          <Box component="img" src="/igo.svg" alt="" sx={{ height: 32, width: 32, opacity: 0.6 }} />
          <Box
            component="img"
            src="/shogi.svg"
            alt=""
            sx={{ height: 32, width: 32, opacity: 0.6 }}
          />
        </Stack>

        <Button variant="contained" size="large" component={Link} to={paths.login}>
          運営者ログイン
        </Button>
      </Box>
    </Container>
  );
}

const meta: Meta<typeof TopPagePlaceholder> = {
  title: 'pages/TopPage',
  component: TopPagePlaceholder,
};

export default meta;

type Story = StoryObj<typeof TopPagePlaceholder>;

/** 通常(提案レイアウト。§3 UI仕様参照) */
export const Default: Story = {};
