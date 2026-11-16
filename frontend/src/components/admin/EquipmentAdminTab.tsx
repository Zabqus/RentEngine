import { useState, type FormEvent } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CircularProgress,
  IconButton,
  InputAdornment,
  Pagination,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import EditIcon from '@mui/icons-material/Edit'
import SearchIcon from '@mui/icons-material/Search'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link as RouterLink } from 'react-router'
import { deleteEquipment, getEquipmentPage, type Equipment } from '../../api/catalog'
import { getApiError } from '../../api/errors'
import { useNotify } from '../../notifications/useNotify'
import { formatMoney } from '../../utils/format'
import { ConfirmDialog } from '../ConfirmDialog'
import { EquipmentDialog } from './EquipmentDialog'

const PAGE_SIZE = 10

export function EquipmentAdminTab() {
  const notify = useNotify()
  const queryClient = useQueryClient()
  const [search, setSearch] = useState('')
  const [q, setQ] = useState('')
  const [page, setPage] = useState(1)
  // undefined = zamknięty, null = nowy
  const [editing, setEditing] = useState<Equipment | null | undefined>(undefined)
  const [toDelete, setToDelete] = useState<Equipment | null>(null)

  const query = { q, page: page - 1, size: PAGE_SIZE, sort: 'name,asc' }
  const equipment = useQuery({
    queryKey: ['equipment', 'admin', query],
    queryFn: () => getEquipmentPage(query),
    placeholderData: keepPreviousData,
  })

  const remove = useMutation({
    mutationFn: (id: number) => deleteEquipment(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['equipment'] })
      notify.success(`Usunięto „${toDelete?.name}”.`)
      setToDelete(null)
    },
    onError: (error) => {
      notify.apiError(error)
      setToDelete(null)
    },
  })

  const handleSearch = (event: FormEvent) => {
    event.preventDefault()
    setQ(search.trim())
    setPage(1)
  }

  const result = equipment.data

  return (
    <Stack spacing={2}>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ justifyContent: 'space-between' }}>
        <Box component="form" onSubmit={handleSearch}>
          <TextField
            size="small"
            placeholder="Szukaj po nazwie"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            slotProps={{
              input: {
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                ),
              },
            }}
          />
        </Box>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setEditing(null)}>
          Dodaj sprzęt
        </Button>
      </Stack>

      {equipment.isPending && <CircularProgress />}
      {equipment.isError && <Alert severity="error">{getApiError(equipment.error).message}</Alert>}
      {result?.content.length === 0 && <Alert severity="info">Brak sprzętu.</Alert>}

      {result && result.content.length > 0 && (
        <TableContainer component={Card} sx={{ opacity: equipment.isPlaceholderData ? 0.6 : 1 }}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Nazwa</TableCell>
                <TableCell>Kategoria</TableCell>
                <TableCell align="right">Cena / doba</TableCell>
                <TableCell align="right">Kaucja</TableCell>
                <TableCell align="right">Akcje</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {result.content.map((item) => (
                <TableRow key={item.id} hover>
                  <TableCell>{item.name}</TableCell>
                  <TableCell>{item.categoryName}</TableCell>
                  <TableCell align="right">{formatMoney(item.dailyPrice)}</TableCell>
                  <TableCell align="right">{formatMoney(item.deposit)}</TableCell>
                  <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                    <Button size="small" component={RouterLink} to={`/admin/equipment/${item.id}`}>
                      Egzemplarze
                    </Button>
                    <Tooltip title="Edytuj">
                      <IconButton size="small" onClick={() => setEditing(item)}>
                        <EditIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Usuń">
                      <IconButton size="small" color="error" onClick={() => setToDelete(item)}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {result && result.totalPages > 1 && (
        <Box sx={{ display: 'flex', justifyContent: 'center' }}>
          <Pagination color="primary" count={result.totalPages} page={page} onChange={(_, value) => setPage(value)} />
        </Box>
      )}

      {editing !== undefined && <EquipmentDialog equipment={editing} onClose={() => setEditing(undefined)} />}
      <ConfirmDialog
        open={toDelete !== null}
        title="Usunąć sprzęt?"
        message={`„${toDelete?.name}” zniknie z katalogu. Sprzętu z egzemplarzami nie da się usunąć.`}
        confirmLabel="Usuń"
        pending={remove.isPending}
        onConfirm={() => toDelete && remove.mutate(toDelete.id)}
        onClose={() => setToDelete(null)}
      />
    </Stack>
  )
}
