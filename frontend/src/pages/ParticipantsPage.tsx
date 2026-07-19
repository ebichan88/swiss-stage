import CategoryIcon from '@mui/icons-material/Category';
import GroupsIcon from '@mui/icons-material/Groups';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import { Box, Button, Typography } from '@mui/material';
import { useState } from 'react';

import type { ParticipantFormValues } from '../components/features/participant/ParticipantFormDialog';
import { CsvImportDialog } from '../components/features/participant/CsvImportDialog';
import { GroupManagerDialog } from '../components/features/participant/GroupManagerDialog';
import { ParticipantFormDialog } from '../components/features/participant/ParticipantFormDialog';
import { ParticipantTable } from '../components/features/participant/ParticipantTable';
import { useTournamentContext } from '../components/layouts/TournamentLayout';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState, LoadingState } from '../components/ui/QueryStates';
import { useGroups } from '../hooks/useGroups';
import {
  useAddParticipant,
  useDeleteParticipant,
  useImportParticipantsCsv,
  useParticipants,
  useUpdateParticipant,
} from '../hooks/useParticipants';
import { useSnackbar } from '../hooks/useSnackbar';
import { ApiError } from '../services/apiClient';
import type { Participant } from '../types/participant';

type DialogState =
  | { kind: 'add' }
  | { kind: 'edit'; participant: Participant }
  | { kind: 'withdraw'; participant: Participant }
  | { kind: 'delete'; participant: Participant }
  | { kind: 'import' }
  | { kind: 'groups' }
  | null;

/** S06 参加者管理。PREPARING=追加・編集・削除可 / IN_PROGRESS=棄権処理のみ / FINISHED=閲覧のみ */
export function ParticipantsPage() {
  const tournament = useTournamentContext();
  const { data: participants, isPending, isError, refetch } = useParticipants(tournament.id);
  const { data: groups } = useGroups(tournament.id);
  const addMutation = useAddParticipant(tournament.id);
  const updateMutation = useUpdateParticipant(tournament.id);
  const deleteMutation = useDeleteParticipant(tournament.id);
  const importMutation = useImportParticipantsCsv(tournament.id);
  const { showSuccess, showError } = useSnackbar();
  const [dialog, setDialog] = useState<DialogState>(null);

  const canEdit = tournament.status === 'PREPARING';
  const canWithdraw = tournament.status === 'IN_PROGRESS';

  const errorMessage = (error: unknown, fallback: string) =>
    error instanceof ApiError ? error.message : fallback;

  const handleFormSubmit = (values: ParticipantFormValues) => {
    const organization = values.organization.trim();
    if (dialog?.kind === 'add') {
      addMutation.mutate(
        {
          name: values.name.trim(),
          organization: organization === '' ? null : organization,
          rank: values.rank === '' ? null : values.rank,
          // 省略時はバックエンドが先頭グループに割り当てる
          ...(values.groupId !== '' ? { groupId: values.groupId } : {}),
        },
        {
          onSuccess: () => {
            setDialog(null);
            showSuccess('参加者を追加しました');
          },
          onError: (error) => showError(errorMessage(error, '参加者の追加に失敗しました')),
        },
      );
    } else if (dialog?.kind === 'edit') {
      updateMutation.mutate(
        {
          participantId: dialog.participant.id,
          input: {
            name: values.name.trim(),
            organization,
            ...(values.rank === '' ? { clearRank: true } : { rank: values.rank }),
            ...(values.groupId !== '' ? { groupId: values.groupId } : {}),
          },
        },
        {
          onSuccess: () => {
            setDialog(null);
            showSuccess('参加者を更新しました');
          },
          onError: (error) => showError(errorMessage(error, '参加者の更新に失敗しました')),
        },
      );
    }
  };

  const handleWithdraw = () => {
    if (dialog?.kind !== 'withdraw') return;
    updateMutation.mutate(
      { participantId: dialog.participant.id, input: { status: 'WITHDRAWN' } },
      {
        onSuccess: () => {
          setDialog(null);
          showSuccess(`${dialog.participant.name}さんを棄権にしました`);
        },
        onError: (error) => showError(errorMessage(error, '棄権処理に失敗しました')),
      },
    );
  };

  const handleDelete = () => {
    if (dialog?.kind !== 'delete') return;
    deleteMutation.mutate(dialog.participant.id, {
      onSuccess: () => {
        setDialog(null);
        showSuccess('参加者を削除しました');
      },
      onError: (error) => showError(errorMessage(error, '参加者の削除に失敗しました')),
    });
  };

  const handleChangeGroup = (participant: Participant, groupId: string) => {
    updateMutation.mutate(
      { participantId: participant.id, input: { groupId } },
      {
        onError: (error) => showError(errorMessage(error, 'グループの変更に失敗しました')),
      },
    );
  };

  const handleImport = (file: File) => {
    importMutation.mutate(file, {
      onSuccess: (result) => {
        setDialog(null);
        showSuccess(`${result.importedCount}名をインポートしました`);
      },
      // エラー詳細(行番号付き)はダイアログ内に表示するため Snackbar は出さない
    });
  };

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
          参加者
          {participants && ` (${participants.length}名)`}
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
              startIcon={<PersonAddIcon />}
              onClick={() => setDialog({ kind: 'add' })}
            >
              参加者を追加
            </Button>
          </Box>
        )}
      </Box>

      {isPending && <LoadingState />}
      {isError && (
        <ErrorState message="参加者一覧の取得に失敗しました" onRetry={() => void refetch()} />
      )}
      {participants && participants.length === 0 && (
        <EmptyState
          icon={<GroupsIcon fontSize="inherit" />}
          message="参加者がまだいません"
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
      {participants && participants.length > 0 && (
        <ParticipantTable
          participants={participants}
          groups={groups ?? []}
          canEdit={canEdit}
          canWithdraw={canWithdraw}
          onEdit={(participant) => setDialog({ kind: 'edit', participant })}
          onWithdraw={(participant) => setDialog({ kind: 'withdraw', participant })}
          onDelete={(participant) => setDialog({ kind: 'delete', participant })}
          onChangeGroup={handleChangeGroup}
        />
      )}

      <ParticipantFormDialog
        open={dialog?.kind === 'add' || dialog?.kind === 'edit'}
        participant={dialog?.kind === 'edit' ? dialog.participant : undefined}
        groups={groups ?? []}
        loading={addMutation.isPending || updateMutation.isPending}
        onSubmit={handleFormSubmit}
        onClose={() => setDialog(null)}
      />
      <GroupManagerDialog
        open={dialog?.kind === 'groups'}
        tournamentId={tournament.id}
        onClose={() => setDialog(null)}
      />
      <CsvImportDialog
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
            ? `${dialog.participant.name}さんを棄権にします。以降のラウンドの組み合わせから除外されます。`
            : ''
        }
        confirmLabel="棄権にする"
        loading={updateMutation.isPending}
        onConfirm={handleWithdraw}
        onCancel={() => setDialog(null)}
      />
      <ConfirmDialog
        open={dialog?.kind === 'delete'}
        title="参加者を削除しますか?"
        message={dialog?.kind === 'delete' ? `${dialog.participant.name}さんを削除します。` : ''}
        confirmLabel="削除する"
        loading={deleteMutation.isPending}
        onConfirm={handleDelete}
        onCancel={() => setDialog(null)}
      />
    </Box>
  );
}
