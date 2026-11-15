import {
  Alert,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Table,
  TableBody,
  TableCell,
  TableRow,
} from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { getApiError } from '../api/errors'
import { getSettlement, type Settlement } from '../api/rentals'
import { formatDateTime, formatMoney } from '../utils/format'

interface Props {
  rentalId: number | null
  onClose: () => void
}

function rows(settlement: Settlement): [string, string][] {
  return [
    ['Wynajem', formatMoney(settlement.baseAmount)],
    [`Kara za spóźnienie (${settlement.lateDays} dób)`, formatMoney(settlement.lateFee)],
    ['Brakujące paliwo', formatMoney(settlement.fuelFee)],
    ['Uszkodzenia razem', formatMoney(settlement.damageCost)],
    ['  w tym z kaucji', formatMoney(settlement.depositDeduction)],
    ['  w tym ponad kaucję', formatMoney(settlement.damageFee)],
    ['Kaucja wpłacona', formatMoney(settlement.deposit)],
    ['Kaucja do zwrotu', formatMoney(settlement.depositRefund)],
  ]
}

export function SettlementDialog({ rentalId, onClose }: Props) {
  const settlement = useQuery({
    queryKey: ['settlement', rentalId],
    queryFn: () => getSettlement(rentalId!),
    enabled: rentalId !== null,
  })

  return (
    <Dialog open={rentalId !== null} onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>Rozliczenie umowy nr {rentalId}</DialogTitle>
      <DialogContent>
        {settlement.isPending && <CircularProgress />}
        {settlement.isError && <Alert severity="error">{getApiError(settlement.error).message}</Alert>}
        {settlement.data === null && <Alert severity="info">Rozliczenie nie zostało jeszcze wygenerowane.</Alert>}
        {settlement.data && (
          <Table size="small">
            <TableBody>
              {rows(settlement.data).map(([label, value]) => (
                <TableRow key={label}>
                  <TableCell sx={{ color: 'text.secondary', whiteSpace: 'pre' }}>{label}</TableCell>
                  <TableCell align="right">{value}</TableCell>
                </TableRow>
              ))}
              <TableRow>
                <TableCell sx={{ fontWeight: 700 }}>Do zapłaty</TableCell>
                <TableCell align="right" sx={{ fontWeight: 700 }}>
                  {formatMoney(settlement.data.total)}
                </TableCell>
              </TableRow>
              <TableRow>
                <TableCell>Status</TableCell>
                <TableCell align="right">
                  {settlement.data.paid ? (
                    <Chip
                      label={`Opłacone ${settlement.data.paidAt ? formatDateTime(settlement.data.paidAt) : ''}`}
                      color="success"
                      size="small"
                    />
                  ) : (
                    <Chip label="Do zapłaty" color="warning" size="small" />
                  )}
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Zamknij</Button>
      </DialogActions>
    </Dialog>
  )
}
