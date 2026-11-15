import {
  Alert,
  Box,
  Card,
  Chip,
  CircularProgress,
  Pagination,
  Stack,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tabs,
  Typography,
} from '@mui/material'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useNavigate, useSearchParams } from 'react-router'
import { getApiError } from '../api/errors'
import { getRentals, type AgreementStatus } from '../api/rentals'
import { formatDateTime, formatMoney } from '../utils/format'
import { STATUS_COLORS, STATUS_LABELS } from '../utils/status'

const PAGE_SIZE = 15

const TABS: { status: AgreementStatus | ''; label: string }[] = [
  { status: 'RESERVED', label: 'Do wydania' },
  { status: 'ACTIVE', label: 'Wydane' },
  { status: 'OVERDUE', label: 'Przeterminowane' },
  { status: 'RETURNED', label: 'Do rozliczenia' },
  { status: 'SETTLED', label: 'Rozliczone' },
  { status: 'CANCELLED', label: 'Anulowane' },
  { status: '', label: 'Wszystkie' },
]

// Bieżące umowy wg terminu, archiwum od najnowszych
const UPCOMING: (AgreementStatus | '')[] = ['RESERVED', 'ACTIVE', 'OVERDUE']

export function RentalsPage() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const status = (searchParams.get('status') ?? 'RESERVED') as AgreementStatus | ''
  const page = Math.max(Number(searchParams.get('page') ?? 1) || 1, 1)

  const sort = UPCOMING.includes(status) ? 'startDate,asc' : 'createdAt,desc'
  const query = { status, page: page - 1, size: PAGE_SIZE, sort }
  const rentals = useQuery({
    queryKey: ['rentals', 'all', query],
    queryFn: () => getRentals(query),
    placeholderData: keepPreviousData,
  })

  const setParams = (nextStatus: string, nextPage = 1) => {
    const params = new URLSearchParams()
    params.set('status', nextStatus)
    if (nextPage > 1) params.set('page', String(nextPage))
    setSearchParams(params)
  }

  const result = rentals.data

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Umowy</Typography>

      <Tabs value={status} onChange={(_, value) => setParams(value)} variant="scrollable" scrollButtons="auto">
        {TABS.map((tab) => (
          <Tab key={tab.label} value={tab.status} label={tab.label} />
        ))}
      </Tabs>

      {rentals.isPending && <CircularProgress />}
      {rentals.isError && <Alert severity="error">{getApiError(rentals.error).message}</Alert>}
      {result && result.content.length === 0 && <Alert severity="info">Brak umów w tej zakładce.</Alert>}

      {result && result.content.length > 0 && (
        <TableContainer component={Card} sx={{ opacity: rentals.isPlaceholderData ? 0.6 : 1 }}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Nr</TableCell>
                <TableCell>Klient</TableCell>
                <TableCell>Sprzęt</TableCell>
                <TableCell>Termin</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Cena</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {result.content.map((rental) => (
                <TableRow
                  key={rental.id}
                  hover
                  onClick={() => navigate(`/rentals/${rental.id}`)}
                  sx={{ cursor: 'pointer' }}
                >
                  <TableCell>{rental.id}</TableCell>
                  <TableCell>{rental.clientName}</TableCell>
                  <TableCell>
                    {rental.equipmentName}
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
                  <TableCell align="right">{formatMoney(rental.basePrice)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {result && result.totalPages > 1 && (
        <Box sx={{ display: 'flex', justifyContent: 'center' }}>
          <Pagination color="primary" count={result.totalPages} page={page} onChange={(_, value) => setParams(status, value)} />
        </Box>
      )}
    </Stack>
  )
}
