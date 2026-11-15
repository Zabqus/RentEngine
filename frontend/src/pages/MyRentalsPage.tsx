import { useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  Link,
  MenuItem,
  Pagination,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link as RouterLink, useSearchParams } from 'react-router'
import { getApiError } from '../api/errors'
import { cancelRental, getMyRentals, type AgreementStatus, type Rental } from '../api/rentals'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { SettlementDialog } from '../components/SettlementDialog'
import { useNotify } from '../notifications/useNotify'
import { formatDateTime, formatMoney } from '../utils/format'
import { ALL_STATUSES, STATUS_COLORS, STATUS_LABELS } from '../utils/status'

const PAGE_SIZE = 10

export function MyRentalsPage() {
  const notify = useNotify()
  const queryClient = useQueryClient()
  const [searchParams, setSearchParams] = useSearchParams()
  const status = (searchParams.get('status') ?? '') as AgreementStatus | ''
  const page = Math.max(Number(searchParams.get('page') ?? 1) || 1, 1)

  const [toCancel, setToCancel] = useState<Rental | null>(null)
  const [settlementFor, setSettlementFor] = useState<number | null>(null)

  const query = { status, page: page - 1, size: PAGE_SIZE, sort: 'createdAt,desc' }
  const rentals = useQuery({
    queryKey: ['rentals', 'my', query],
    queryFn: () => getMyRentals(query),
    placeholderData: keepPreviousData,
  })

  const cancel = useMutation({
    mutationFn: (id: number) => cancelRental(id),
    onSuccess: (rental) => {
      queryClient.invalidateQueries({ queryKey: ['rentals'] })
      notify.success(`Rezerwacja nr ${rental.id} anulowana.`)
      setToCancel(null)
    },
    onError: (error) => {
      notify.apiError(error)
      setToCancel(null)
    },
  })

  const setParams = (next: { status?: string; page?: number }) => {
    const params = new URLSearchParams(searchParams)
    if (next.status !== undefined) {
      if (next.status) params.set('status', next.status)
      else params.delete('status')
      params.delete('page')
    }
    if (next.page !== undefined) {
      if (next.page > 1) params.set('page', String(next.page))
      else params.delete('page')
    }
    setSearchParams(params)
  }

  const result = rentals.data

  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ justifyContent: 'space-between' }}>
        <Typography variant="h4">Moje umowy</Typography>
        <TextField
          select
          size="small"
          label="Status"
          value={status}
          onChange={(event) => setParams({ status: event.target.value })}
          sx={{ minWidth: 200 }}
        >
          <MenuItem value="">Wszystkie</MenuItem>
          {ALL_STATUSES.map((value) => (
            <MenuItem key={value} value={value}>
              {STATUS_LABELS[value]}
            </MenuItem>
          ))}
        </TextField>
      </Stack>

      {rentals.isPending && <CircularProgress />}
      {rentals.isError && <Alert severity="error">{getApiError(rentals.error).message}</Alert>}

      {result && result.content.length === 0 && (
        <Alert severity="info">
          {status ? 'Brak umów o tym statusie.' : 'Nie masz jeszcze żadnych umów. '}
          {!status && (
            <Link component={RouterLink} to="/catalog">
              Przejdź do katalogu
            </Link>
          )}
        </Alert>
      )}

      {result && result.content.length > 0 && (
        <TableContainer component={Card} sx={{ opacity: rentals.isPlaceholderData ? 0.6 : 1 }}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Nr</TableCell>
                <TableCell>Sprzęt</TableCell>
                <TableCell>Termin</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Cena</TableCell>
                <TableCell align="right">Akcje</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {result.content.map((rental) => (
                <TableRow key={rental.id} hover>
                  <TableCell>{rental.id}</TableCell>
                  <TableCell>
                    <Link component={RouterLink} to={`/catalog/${rental.equipmentId}`}>
                      {rental.equipmentName}
                    </Link>
                    <Typography variant="body2" color="text.secondary">
                      {rental.serialOrPlate}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    {formatDateTime(rental.startDate)}
                    <br />
                    {formatDateTime(rental.endDate)}
                  </TableCell>
                  <TableCell>
                    <Chip label={STATUS_LABELS[rental.status]} color={STATUS_COLORS[rental.status]} size="small" />
                  </TableCell>
                  <TableCell align="right">
                    {formatMoney(rental.basePrice)}
                    <Typography variant="body2" color="text.secondary">
                      kaucja {formatMoney(rental.deposit)}
                    </Typography>
                  </TableCell>
                  <TableCell align="right">
                    {rental.status === 'RESERVED' && (
                      <Button color="error" size="small" onClick={() => setToCancel(rental)}>
                        Anuluj
                      </Button>
                    )}
                    {(rental.status === 'RETURNED' || rental.status === 'SETTLED') && (
                      <Button size="small" onClick={() => setSettlementFor(rental.id)}>
                        Rozliczenie
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {result && result.totalPages > 1 && (
        <Box sx={{ display: 'flex', justifyContent: 'center' }}>
          <Pagination
            color="primary"
            count={result.totalPages}
            page={page}
            onChange={(_, value) => setParams({ page: value })}
          />
        </Box>
      )}

      <ConfirmDialog
        open={toCancel !== null}
        title="Anulować rezerwację?"
        message={toCancel ? `${toCancel.equipmentName} (${toCancel.serialOrPlate}), od ${formatDateTime(toCancel.startDate)}.` : ''}
        confirmLabel="Anuluj rezerwację"
        pending={cancel.isPending}
        onConfirm={() => toCancel && cancel.mutate(toCancel.id)}
        onClose={() => setToCancel(null)}
      />
      <SettlementDialog rentalId={settlementFor} onClose={() => setSettlementFor(null)} />
    </Stack>
  )
}
