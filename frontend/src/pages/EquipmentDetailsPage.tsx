import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableRow,
  Typography,
} from '@mui/material'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import { useQuery } from '@tanstack/react-query'
import { Link as RouterLink, useLocation, useNavigate, useParams } from 'react-router'
import { getEquipment, getUnits } from '../api/catalog'
import { getApiError } from '../api/errors'
import { useAuth } from '../auth/useAuth'
import { counterLabel, formatMoney, formatNumber, formatParam, paramLabel } from '../utils/format'

export function EquipmentDetailsPage() {
  const id = Number(useParams().id)
  const navigate = useNavigate()
  const location = useLocation()
  const { role } = useAuth()

  const equipment = useQuery({
    queryKey: ['equipment', id],
    queryFn: () => getEquipment(id),
    enabled: Number.isInteger(id),
  })
  const units = useQuery({
    queryKey: ['units', id],
    queryFn: () => getUnits(id),
    enabled: Number.isInteger(id),
  })

  if (!Number.isInteger(id)) {
    return <Alert severity="error">Nieprawidłowy adres sprzętu.</Alert>
  }
  if (equipment.isPending) {
    return <CircularProgress />
  }
  if (equipment.isError) {
    return <Alert severity="error">{getApiError(equipment.error).message}</Alert>
  }

  const item = equipment.data
  const available = units.data?.filter((unit) => unit.status === 'AVAILABLE').length ?? 0

  const facts: [string, string][] = [
    ['Cena za dobę', formatMoney(item.dailyPrice)],
    ['Kaucja', formatMoney(item.deposit)],
    ['Zbiornik paliwa', item.tankCapacity != null ? `${formatNumber(item.tankCapacity)} l` : 'brak'],
    ...Object.entries(item.params).map(([key, value]): [string, string] => [paramLabel(key), formatParam(value)]),
  ]

  return (
    <Stack spacing={3}>
      <Box>
        <Button startIcon={<ArrowBackIcon />} onClick={() => navigate(-1)}>
          Wróć
        </Button>
      </Box>

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={3} sx={{ alignItems: 'flex-start' }}>
        <Card sx={{ flex: 2, width: '100%' }}>
          <CardContent>
            <Chip
              label={item.categoryName}
              size="small"
              component={RouterLink}
              to={`/catalog?categoryId=${item.categoryId}`}
              clickable
            />
            <Typography variant="h4" sx={{ mt: 1, mb: 2 }}>
              {item.name}
            </Typography>
            <Table size="small">
              <TableBody>
                {facts.map(([label, value]) => (
                  <TableRow key={label}>
                    <TableCell sx={{ color: 'text.secondary', width: '45%' }}>{label}</TableCell>
                    <TableCell>{value}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>

        <Card sx={{ flex: 1, width: '100%' }}>
          <CardContent>
            <Typography variant="h4" color="primary">
              {formatMoney(item.dailyPrice)}
            </Typography>
            <Typography color="text.secondary" gutterBottom>
              za dobę, kaucja {formatMoney(item.deposit)}
            </Typography>
            <Typography sx={{ my: 2 }}>
              Egzemplarze dostępne: <strong>{available}</strong> z {units.data?.length ?? 0}
            </Typography>
            {!role && (
              <Button
                variant="contained"
                fullWidth
                component={RouterLink}
                to="/login"
                state={{ from: location.pathname }}
              >
                Zaloguj się, aby zarezerwować
              </Button>
            )}
            {role === 'CLIENT' && <Alert severity="info">Rezerwacja online będzie dostępna wkrótce.</Alert>}
          </CardContent>
        </Card>
      </Stack>

      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Egzemplarze
          </Typography>
          {units.isError && <Alert severity="error">{getApiError(units.error).message}</Alert>}
          {units.data?.length === 0 && <Typography color="text.secondary">Brak egzemplarzy.</Typography>}
          <Table size="small">
            <TableBody>
              {units.data?.map((unit) => (
                <TableRow key={unit.id}>
                  <TableCell>{unit.serialOrPlate}</TableCell>
                  <TableCell>
                    {counterLabel(item.categoryType)}: {formatNumber(unit.mileageOrHours)}
                  </TableCell>
                  <TableCell align="right">
                    {unit.status === 'AVAILABLE' ? (
                      <Chip label="Dostępny" color="success" size="small" />
                    ) : (
                      <Chip label="W serwisie" color="warning" size="small" />
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </Stack>
  )
}
