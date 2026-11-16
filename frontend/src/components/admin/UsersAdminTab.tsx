import { useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Chip,
  CircularProgress,
  IconButton,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import EditIcon from '@mui/icons-material/Edit'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { User } from '../../api/auth'
import { getApiError } from '../../api/errors'
import { deleteUser, getUsers } from '../../api/users'
import { ROLE_LABELS } from '../../auth/roles'
import { useAuth } from '../../auth/useAuth'
import { useNotify } from '../../notifications/useNotify'
import { ConfirmDialog } from '../ConfirmDialog'
import { UserDialog } from './UserDialog'

const ROLE_COLORS = { CLIENT: 'default', EMPLOYEE: 'info', ADMIN: 'secondary' } as const

export function UsersAdminTab() {
  const notify = useNotify()
  const queryClient = useQueryClient()
  const { user: me } = useAuth()
  const [editing, setEditing] = useState<User | null | undefined>(undefined)
  const [toDelete, setToDelete] = useState<User | null>(null)

  const users = useQuery({ queryKey: ['users'], queryFn: getUsers })

  const remove = useMutation({
    mutationFn: (id: number) => deleteUser(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      notify.success(`Usunięto ${toDelete?.email}.`)
      setToDelete(null)
    },
    onError: (error) => {
      notify.apiError(error)
      setToDelete(null)
    },
  })

  return (
    <Stack spacing={2}>
      <Stack direction="row" sx={{ justifyContent: 'flex-end' }}>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setEditing(null)}>
          Dodaj użytkownika
        </Button>
      </Stack>

      {users.isPending && <CircularProgress />}
      {users.isError && <Alert severity="error">{getApiError(users.error).message}</Alert>}

      {users.data && (
        <TableContainer component={Card}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Imię i nazwisko</TableCell>
                <TableCell>E-mail</TableCell>
                <TableCell>Rola</TableCell>
                <TableCell align="right">Akcje</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {users.data.map((user) => (
                <TableRow key={user.id} hover>
                  <TableCell>
                    {user.firstName} {user.lastName}
                    {user.id === me?.id && <Chip label="Ty" size="small" sx={{ ml: 1 }} />}
                  </TableCell>
                  <TableCell>{user.email}</TableCell>
                  <TableCell>
                    <Chip label={ROLE_LABELS[user.role]} color={ROLE_COLORS[user.role]} size="small" />
                  </TableCell>
                  <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                    <Tooltip title="Edytuj">
                      <IconButton size="small" onClick={() => setEditing(user)}>
                        <EditIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title={user.id === me?.id ? 'Nie możesz usunąć siebie' : 'Usuń'}>
                      <span>
                        <IconButton
                          size="small"
                          color="error"
                          disabled={user.id === me?.id}
                          onClick={() => setToDelete(user)}
                        >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </span>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {editing !== undefined && (
        <UserDialog user={editing} isSelf={editing?.id === me?.id} onClose={() => setEditing(undefined)} />
      )}
      <ConfirmDialog
        open={toDelete !== null}
        title="Usunąć użytkownika?"
        message={`${toDelete?.email}. Użytkownika z umowami nie da się usunąć.`}
        confirmLabel="Usuń"
        pending={remove.isPending}
        onConfirm={() => toDelete && remove.mutate(toDelete.id)}
        onClose={() => setToDelete(null)}
      />
    </Stack>
  )
}
