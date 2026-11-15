import { Chip, Table, TableBody, TableCell, TableRow } from '@mui/material'
import type { Settlement } from '../api/rentals'
import { formatDateTime, formatMoney } from '../utils/format'

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

// Rozbicie kwot rozliczenia; wspólne dla klienta i pracownika
export function SettlementSummary({ settlement }: { settlement: Settlement }) {
  return (
    <Table size="small">
      <TableBody>
        {rows(settlement).map(([label, value]) => (
          <TableRow key={label}>
            <TableCell sx={{ color: 'text.secondary', whiteSpace: 'pre' }}>{label}</TableCell>
            <TableCell align="right">{value}</TableCell>
          </TableRow>
        ))}
        <TableRow>
          <TableCell sx={{ fontWeight: 700 }}>Do zapłaty</TableCell>
          <TableCell align="right" sx={{ fontWeight: 700 }}>
            {formatMoney(settlement.total)}
          </TableCell>
        </TableRow>
        <TableRow>
          <TableCell>Status</TableCell>
          <TableCell align="right">
            {settlement.paid ? (
              <Chip
                label={`Opłacone ${settlement.paidAt ? formatDateTime(settlement.paidAt) : ''}`}
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
  )
}
