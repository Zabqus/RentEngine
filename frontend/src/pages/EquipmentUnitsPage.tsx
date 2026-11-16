import { useState } from 'react'
import {
  Alert,
  Box,
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
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import DeleteIcon from '@mui/icons-material/Delete'
import EditIcon from '@mui/icons-material/Edit'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link as RouterLink, useParams } from 'react-router'
import { changeUnitStatus, deleteUnit, getEquipment, getUnits, type Unit, type UnitStatus } from '../api/catalog'
import { getApiError } from '../api/errors'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { UnitDialog } from '../components/admin/UnitDialog'
import { useNotify } from '../notifications/useNotify'
import { counterLabel, formatMoney, formatNumber } from '../utils/format'

export function EquipmentUnitsPage() {
  const id = Number(useParams().id)
  const notify = useNotify()
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState<Unit | null | undefined>(undefined)
  const [toDelete, setToDelete] = useState<Unit | null>(null)

  const equipment = useQuery({ queryKey: ['equipment', id], queryFn: () => getEquipment(id) })
  const units = useQuery({ queryKey: ['units', id], queryFn: () => getUnits(id) })

  const status = useMutation({
    mutationFn: ({ unit, next }: { unit: Unit; next: UnitStatus }) => changeUnitStatus(unit.id, next),
    onSuccess: (unit) => {
      queryClient.invalidateQueries({ queryKey: ['units'] })
      notify.success(
        unit.status === 'AVAILABLE' ? `${unit.serialOrPlate} wrócił do wypożyczania.` : `${unit.serialOrPlate} w serwisie.`,
      )
    },
    onError: (error) => notify.apiError(error),
  })

  const remove = useMutation({
    mutationFn: (unitId: number) => deleteUnit(unitId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['units'] })
      notify.success(`Usunięto egzemplarz ${toDelete?.serialOrPlate}.`)
      setToDelete(null)
    },
    onError: (error) => {
      notify.apiError(error)
      setToDelete(null)
    },
  })

  if (equipment.isPending) return <CircularProgress />
  if (equipment.isError) return <Alert severity="error">{getApiError(equipment.error).message}</Alert>
  const item = equipment.data

  return (
    <Stack spacing={3}>
      <Box>
        <Button startIcon={<ArrowBackIcon />} component={RouterLink} to="/admin">
          Administracja
        </Button>
      </Box>

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ justifyContent: 'space-between' }}>
        <Box>
          <Typography variant="h4">{item.name}</Typography>
          <Typography color="text.secondary">
            {item.categoryName} · {formatMoney(item.dailyPrice)} / doba
          </Typography>
        </Box>
        <Box>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setEditing(null)}>
            Dodaj egzemplarz
          </Button>
        </Box>
      </Stack>

      {units.isPending && <CircularProgress />}
      {units.isError && <Alert severity="error">{getApiError(units.error).message}</Alert>}
      {units.data?.length === 0 && <Alert severity="info">Ten sprzęt nie ma jeszcze egzemplarzy.</Alert>}

      {units.data && units.data.length > 0 && (
        <TableContainer component={Card}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>{item.categoryType === 'CAR' ? 'Rejestracja' : 'Numer seryjny'}</TableCell>
                <TableCell align="right">{counterLabel(item.categoryType)}</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Akcje</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {units.data.map((unit) => (
                <TableRow key={unit.id} hover>
                  <TableCell>{unit.serialOrPlate}</TableCell>
                  <TableCell align="right">{formatNumber(unit.mileageOrHours)}</TableCell>
                  <TableCell>
                    {unit.status === 'AVAILABLE' ? (
                      <Chip label="Dostępny" color="success" size="small" />
                    ) : (
                      <Chip label="W serwisie" color="warning" size="small" />
                    )}
                  </TableCell>
                  <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                    <Button
                      size="small"
                      disabled={status.isPending}
                      onClick={() =>
                        status.mutate({ unit, next: unit.status === 'AVAILABLE' ? 'IN_SERVICE' : 'AVAILABLE' })
                      }
                    >
                      {unit.status === 'AVAILABLE' ? 'Do serwisu' : 'Przywróć'}
                    </Button>
                    <Tooltip title="Edytuj">
                      <IconButton size="small" onClick={() => setEditing(unit)}>
                        <EditIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Usuń">
                      <IconButton size="small" color="error" onClick={() => setToDelete(unit)}>
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

      {editing !== undefined && (
        <UnitDialog equipmentId={id} categoryType={item.categoryType} unit={editing} onClose={() => setEditing(undefined)} />
      )}
      <ConfirmDialog
        open={toDelete !== null}
        title="Usunąć egzemplarz?"
        message={`${toDelete?.serialOrPlate}. Egzemplarza z umowami nie da się usunąć.`}
        confirmLabel="Usuń"
        pending={remove.isPending}
        onConfirm={() => toDelete && remove.mutate(toDelete.id)}
        onClose={() => setToDelete(null)}
      />
    </Stack>
  )
}
