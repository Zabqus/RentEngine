import { Alert, Button, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { getApiError } from '../api/errors'
import { getSettlement } from '../api/rentals'
import { SettlementSummary } from './SettlementSummary'

interface Props {
  rentalId: number | null
  onClose: () => void
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
        {settlement.data && <SettlementSummary settlement={settlement.data} />}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Zamknij</Button>
      </DialogActions>
    </Dialog>
  )
}
