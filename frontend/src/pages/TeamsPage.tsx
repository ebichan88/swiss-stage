import CategoryIcon from '@mui/icons-material/Category';
import GroupsIcon from '@mui/icons-material/Groups';
import GroupAddIcon from '@mui/icons-material/GroupAdd';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import { Box, Button, Typography } from '@mui/material';
import { useState } from 'react';

import { GroupManagerDialog } from '../components/features/participant/GroupManagerDialog';
import type { TeamFormValues } from '../components/features/team/TeamFormDialog';
import { TeamCsvImportDialog } from '../components/features/team/TeamCsvImportDialog';
import { TeamFormDialog } from '../components/features/team/TeamFormDialog';
import { TeamMemberManagerDialog } from '../components/features/team/TeamMemberManagerDialog';
import { TeamTable } from '../components/features/team/TeamTable';
import { useTournamentContext } from '../components/layouts/TournamentLayout';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState, LoadingState } from '../components/ui/QueryStates';
import { useGroups } from '../hooks/useGroups';
import {
  useCreateTeam,
  useDeleteTeam,
  useImportTeamsCsv,
  useTeams,
  useUpdateTeam,
} from '../hooks/useTeams';
import { useSnackbar } from '../hooks/useSnackbar';
import { ApiError } from '../services/apiClient';
import type { Team } from '../types/team';

type DialogState =
  | { kind: 'add' }
  | { kind: 'edit'; team: Team }
  | { kind: 'withdraw'; team: Team }
  | { kind: 'delete'; team: Team }
  | { kind: 'members'; team: Team }
  | { kind: 'import' }
  | { kind: 'groups' }
  | null;

/** S06 チーム管理(団体戦)。PREPARING=追加・編集・削除可 / IN_PROGRESS=棄権処理のみ / FINISHED=閲覧のみ */
export function TeamsPage() {
  const tournament = useTournamentContext();
  const { data: teams, isPending, isError, refetch } = useTeams(tournament.id);
  const { data: groups } = useGroups(tournament.id);
  const createMutation = useCreateTeam(tournament.id);
  const updateMutation = useUpdateTeam(tournament.id);
  const deleteMutation = useDeleteTeam(tournament.id);
  const importMutation = useImportTeamsCsv(tournament.id);
  const { showSuccess, showError } = useSnackbar();
  const [dialog, setDialog] = useState<DialogState>(null);

  const canEdit = tournament.status === 'PREPARING';
  const canWithdraw = tournament.status === 'IN_PROGRESS';
  const teamSize = tournament.teamSize ?? 3;

  const errorMessage = (error: unknown, fallback: string) =>
    error instanceof ApiError ? error.message : fallback;

  const handleFormSubmit = (values: TeamFormValues) => {
    if (dialog?.kind === 'add') {
      createMutation.mutate(
        {
          name: values.name.trim(),
          ...(values.groupId !== '' ? { groupId: values.groupId } : {}),
        },
        {
          onSuccess: () => {
            setDialog(null);
            showSuccess('チームを追加しました');
          },
          onError: (error) => showError(errorMessage(error, 'チームの追加に失敗しました')),
        },
      );
    } else if (dialog?.kind === 'edit') {
      updateMutation.mutate(
        {
          teamId: dialog.team.id,
          input: {
            name: values.name.trim(),
            ...(values.groupId !== '' ? { groupId: values.groupId } : {}),
          },
        },
        {
          onSuccess: () => {
            setDialog(null);
            showSuccess('チームを更新しました');
          },
          onError: (error) => showError(errorMessage(error, 'チームの更新に失敗しました')),
        },
      );
    }
  };

  const handleWithdraw = () => {
    if (dialog?.kind !== 'withdraw') return;
    updateMutation.mutate(
      { teamId: dialog.team.id, input: { status: 'WITHDRAWN' } },
      {
        onSuccess: () => {
          setDialog(null);
          showSuccess(`${dialog.team.name}を棄権にしました`);
        },
        onError: (error) => showError(errorMessage(error, '棄権処理に失敗しました')),
      },
    );
  };

  const handleDelete = () => {
    if (dialog?.kind !== 'delete') return;
    deleteMutation.mutate(dialog.team.id, {
      onSuccess: () => {
        setDialog(null);
        showSuccess('チームを削除しました');
      },
      onError: (error) => showError(errorMessage(error, 'チームの削除に失敗しました')),
    });
  };

  const handleChangeGroup = (team: Team, groupId: string) => {
    updateMutation.mutate(
      { teamId: team.id, input: { groupId } },
      { onError: (error) => showError(errorMessage(error, 'グループの変更に失敗しました')) },
    );
  };

  const handleImport = (file: File) => {
    importMutation.mutate(file, {
      onSuccess: (result) => {
        setDialog(null);
        showSuccess(`${result.importedTeamCount}チームをインポートしました`);
      },
      // エラー詳細(行番号付き)はダイアログ内に表示するため Snackbar は出さない
    });
  };

  // メンバー管理ダイアログはチームデータ更新後も同じチームを指し続けるよう最新値を引き直す
  const managingTeam =
    dialog?.kind === 'members'
      ? (teams?.find((t) => t.id === dialog.team.id) ?? dialog.team)
      : null;

  return (
    <Box>
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          mb: 2,
          gap: 1,
          flexWrap: 'wrap',
        }}
      >
        <Typography variant="h3" component="h2">
          チーム
          {teams && ` (${teams.length}チーム)`}
        </Typography>
        {canEdit && (
          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
            <Button
              variant="outlined"
              startIcon={<CategoryIcon />}
              onClick={() => setDialog({ kind: 'groups' })}
            >
              グループ管理
            </Button>
            <Button
              variant="outlined"
              startIcon={<UploadFileIcon />}
              onClick={() => {
                importMutation.reset();
                setDialog({ kind: 'import' });
              }}
            >
              CSVインポート
            </Button>
            <Button
              variant="contained"
              startIcon={<GroupAddIcon />}
              onClick={() => setDialog({ kind: 'add' })}
            >
              チームを追加
            </Button>
          </Box>
        )}
      </Box>

      {isPending && <LoadingState />}
      {isError && (
        <ErrorState message="チーム一覧の取得に失敗しました" onRetry={() => void refetch()} />
      )}
      {teams && teams.length === 0 && (
        <EmptyState
          icon={<GroupsIcon fontSize="inherit" />}
          message="チームがまだいません"
          action={
            canEdit ? (
              <Button
                variant="outlined"
                onClick={() => {
                  importMutation.reset();
                  setDialog({ kind: 'import' });
                }}
              >
                CSVをインポート
              </Button>
            ) : undefined
          }
        />
      )}
      {teams && teams.length > 0 && (
        <TeamTable
          teams={teams}
          teamSize={teamSize}
          groups={groups ?? []}
          canEdit={canEdit}
          canWithdraw={canWithdraw}
          onEdit={(team) => setDialog({ kind: 'edit', team })}
          onManageMembers={(team) => setDialog({ kind: 'members', team })}
          onWithdraw={(team) => setDialog({ kind: 'withdraw', team })}
          onDelete={(team) => setDialog({ kind: 'delete', team })}
          onChangeGroup={handleChangeGroup}
        />
      )}

      <TeamFormDialog
        open={dialog?.kind === 'add' || dialog?.kind === 'edit'}
        team={dialog?.kind === 'edit' ? dialog.team : undefined}
        groups={groups ?? []}
        loading={createMutation.isPending || updateMutation.isPending}
        onSubmit={handleFormSubmit}
        onClose={() => setDialog(null)}
      />
      <TeamMemberManagerDialog
        open={dialog?.kind === 'members'}
        tournamentId={tournament.id}
        teamSize={teamSize}
        team={managingTeam}
        canEdit={canEdit}
        onClose={() => setDialog(null)}
      />
      <GroupManagerDialog
        open={dialog?.kind === 'groups'}
        tournamentId={tournament.id}
        onClose={() => setDialog(null)}
      />
      <TeamCsvImportDialog
        open={dialog?.kind === 'import'}
        loading={importMutation.isPending}
        error={importMutation.error}
        onImport={handleImport}
        onClose={() => setDialog(null)}
      />
      <ConfirmDialog
        open={dialog?.kind === 'withdraw'}
        title="棄権にしますか?"
        message={
          dialog?.kind === 'withdraw'
            ? `${dialog.team.name}を棄権にします。以降のラウンドの組み合わせから除外されます。`
            : ''
        }
        confirmLabel="棄権にする"
        loading={updateMutation.isPending}
        onConfirm={handleWithdraw}
        onCancel={() => setDialog(null)}
      />
      <ConfirmDialog
        open={dialog?.kind === 'delete'}
        title="チームを削除しますか?"
        message={dialog?.kind === 'delete' ? `${dialog.team.name}を削除します。` : ''}
        confirmLabel="削除する"
        loading={deleteMutation.isPending}
        onConfirm={handleDelete}
        onCancel={() => setDialog(null)}
      />
    </Box>
  );
}
