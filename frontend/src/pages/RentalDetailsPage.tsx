import { useState } from 'react'
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
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router'
import { getEquipment, getUnit } from '../api/catalog'
import { getApiError } from '../api/errors'
import { getProtocols, type Protocol, type ProtocolType } from '../api/protocols'
import { cancelRental, createSettlement, getRental, getSettlement, paySettlement } from '../api/rentals'
import { useAuth } from '../auth/useAuth'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { DamageDialog } from '../components/DamageDialog'
import { DamagePhoto } from '../components/DamagePhoto'
import { PhotoUploadButton } from '../components/PhotoUploadButton'
import { ProtocolDialog } from '../components/ProtocolDialog'
import { SettlementSummary } from '../components/SettlementSummary'
import { useNotify } from '../notifications/useNotify'
import { counterLabel, formatDateTime, formatMoney, formatNumber } from '../utils/format'
import { STATUS_COLORS, STATUS_LABELS } from '../utils/status'

const PROTOCOL_LABELS: Record<ProtocolType, string> = { ISSUE: 'Wydanie', RETURN: 'Zwrot' }

export function RentalDetailsPage() {
  const id = Number(useParams().id)
  const navigate = useNavigate()
  const notify = useNotify()
  const queryClient = useQueryClient()
  const { role } = useAuth()
  const isEmployee = role === 'EMPLOYEE'

  const [protocolType, setProtocolType] = useState<ProtocolType | null>(null)
  const [damageFor, setDamageFor] = useState<Protocol | null>(null)
  const [confirmCancel, setConfirmCancel] = useState(false)

  const rental = useQuery({ queryKey: ['rentals', 'one', id], queryFn: () => getRental(id) })
  const data = rental.data
  const equipment = useQuery({
    queryKey: ['equipment', data?.equipmentId],
    queryFn: () => getEquipment(data!.equipmentId),
    enabled: Boolean(data),
  })
  const unit = useQuery({
    queryKey: ['units', 'one', data?.unitId],
    queryFn: () => getUnit(data!.unitId),
    enabled: Boolean(data),
  })
  const protocols = useQuery({ queryKey: ['protocols', id], queryFn: () => getProtocols(id) })
  const hasSettlementStage = data?.status === 'RETURNED' || data?.status === 'SETTLED'
  const settlement = useQuery({
    queryKey: ['settlement', id],
    queryFn: () => getSettlement(id),
    enabled: hasSettlementStage,
  })

  // Po każdej zmianie odświeżamy wszystko, co mogło się zmienić (status, licznik, serwis)
  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['rentals'] })
    queryClient.invalidateQueries({ queryKey: ['protocols', id] })
    queryClient.invalidateQueries({ queryKey: ['settlement', id] })
    queryClient.invalidateQueries({ queryKey: ['units'] })
  }

  const generate = useMutation({
    mutationFn: () => createSettlement(id),
    onSuccess: () => {
      notify.success('Rozliczenie wygenerowane.')
      refresh()
    },
    onError: (error) => notify.apiError(error),
  })
  const pay = useMutation({
    mutationFn: (settlementId: number) => paySettlement(settlementId),
    onSuccess: () => {
      notify.success('Rozliczenie opłacone, umowa zamknięta.')
      refresh()
    },
    onError: (error) => notify.apiError(error),
  })
  const cancel = useMutation({
    mutationFn: () => cancelRental(id),
    onSuccess: () => {
      notify.success('Rezerwacja anulowana.')
      setConfirmCancel(false)
      refresh()
    },
    onError: (error) => {
      notify.apiError(error)
      setConfirmCancel(false)
    },
  })

  if (rental.isPending) return <CircularProgress />
  if (rental.isError) return <Alert severity="error">{getApiError(rental.error).message}</Alert>
  const r = rental.data

  const canIssue = isEmployee && r.status === 'RESERVED'
  const canReturn = isEmployee && (r.status === 'ACTIVE' || r.status === 'OVERDUE')
  // Uszkodzenia i zdjęcia można dodawać do momentu wygenerowania rozliczenia
  const canEditDamages = isEmployee && settlement.data == null && r.status !== 'SETTLED'
  const categoryType = equipment.data?.categoryType ?? 'CAR'

  const facts: [string, string][] = [
    ['Klient', r.clientName],
    ['Sprzęt', `${r.equipmentName} (${r.serialOrPlate})`],
    ['Odbiór', formatDateTime(r.startDate)],
    ['Zwrot', formatDateTime(r.endDate)],
    ['Cena', formatMoney(r.basePrice)],
    ['Kaucja', formatMoney(r.deposit)],
    ['Utworzona', formatDateTime(r.createdAt)],
  ]

  return (
    <Stack spacing={3}>
      <Box>
        <Button startIcon={<ArrowBackIcon />} onClick={() => navigate(-1)}>
          Wróć
        </Button>
      </Box>

      <Card>
        <CardContent>
          <Stack direction="row" spacing={2} sx={{ alignItems: 'center', mb: 2, flexWrap: 'wrap' }}>
            <Typography variant="h4">Umowa nr {r.id}</Typography>
            <Chip label={STATUS_LABELS[r.status]} color={STATUS_COLORS[r.status]} />
          </Stack>
          <Table size="small">
            <TableBody>
              {facts.map(([label, value]) => (
                <TableRow key={label}>
                  <TableCell sx={{ color: 'text.secondary', width: '30%' }}>{label}</TableCell>
                  <TableCell>{value}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <Stack direction="row" spacing={1} sx={{ mt: 2, flexWrap: 'wrap' }}>
            {canIssue && (
              <Button variant="contained" onClick={() => setProtocolType('ISSUE')} disabled={!unit.data || !equipment.data}>
                Wydaj sprzęt
              </Button>
            )}
            {canReturn && (
              <Button variant="contained" onClick={() => setProtocolType('RETURN')} disabled={!unit.data || !equipment.data}>
                Przyjmij zwrot
              </Button>
            )}
            {role === 'ADMIN' && r.status === 'RESERVED' && (
              <Button color="error" onClick={() => setConfirmCancel(true)}>
                Anuluj rezerwację
              </Button>
            )}
          </Stack>
          {r.status === 'OVERDUE' && (
            <Alert severity="warning" sx={{ mt: 2 }}>
              Termin zwrotu minął. Kara naliczy się w rozliczeniu (+10% stawki za każdą rozpoczętą dobę).
            </Alert>
          )}
        </CardContent>
      </Card>

      <Typography variant="h5">Protokoły</Typography>
      {protocols.isPending && <CircularProgress />}
      {protocols.isError && <Alert severity="error">{getApiError(protocols.error).message}</Alert>}
      {protocols.data?.length === 0 && <Typography color="text.secondary">Sprzęt nie został jeszcze wydany.</Typography>}
      {protocols.data?.map((protocol) => (
        <Card key={protocol.id}>
          <CardContent>
            <Stack direction="row" spacing={2} sx={{ justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap' }}>
              <Typography variant="h6">{PROTOCOL_LABELS[protocol.type]}</Typography>
              <Typography variant="body2" color="text.secondary">
                {formatDateTime(protocol.createdAt)}, {protocol.employeeName}
              </Typography>
            </Stack>
            <Typography sx={{ mt: 1 }}>
              {protocol.fuelLevel !== null && <>Paliwo: {formatNumber(protocol.fuelLevel)} l · </>}
              {counterLabel(categoryType)}: {formatNumber(protocol.mileageOrHours)}
            </Typography>
            {protocol.notes && (
              <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                {protocol.notes}
              </Typography>
            )}

            {protocol.damages.length > 0 && (
              <Table size="small" sx={{ mt: 2 }}>
                <TableHead>
                  <TableRow>
                    <TableCell>Uszkodzenie</TableCell>
                    <TableCell>Waga</TableCell>
                    <TableCell align="right">Koszt</TableCell>
                    <TableCell>Zdjęcie</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {protocol.damages.map((damage) => (
                    <TableRow key={damage.id}>
                      <TableCell>{damage.description}</TableCell>
                      <TableCell>
                        {damage.severity === 'MAJOR' ? (
                          <Chip label="Poważne" color="error" size="small" />
                        ) : (
                          <Chip label="Drobne" size="small" />
                        )}
                      </TableCell>
                      <TableCell align="right">{formatMoney(damage.cost)}</TableCell>
                      <TableCell>
                        <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                          {damage.photoUrl && <DamagePhoto damageId={damage.id} version={String(protocols.dataUpdatedAt)} />}
                          {canEditDamages && <PhotoUploadButton damage={damage} onUploaded={refresh} />}
                          {!damage.photoUrl && !canEditDamages && '—'}
                        </Stack>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
            {canEditDamages && (
              <Button startIcon={<AddIcon />} sx={{ mt: 1 }} onClick={() => setDamageFor(protocol)}>
                Dodaj uszkodzenie
              </Button>
            )}
          </CardContent>
        </Card>
      ))}

      {hasSettlementStage && (
        <>
          <Typography variant="h5">Rozliczenie</Typography>
          <Card>
            <CardContent>
              {settlement.isPending && <CircularProgress />}
              {settlement.isError && <Alert severity="error">{getApiError(settlement.error).message}</Alert>}
              {settlement.data === null && (
                <Stack spacing={2} sx={{ alignItems: 'flex-start' }}>
                  <Typography color="text.secondary">
                    Sprzęt zwrócony. Po dodaniu ewentualnych uszkodzeń wygeneruj rozliczenie (potem nie da się ich zmienić).
                  </Typography>
                  {isEmployee && (
                    <Button variant="contained" loading={generate.isPending} onClick={() => generate.mutate()}>
                      Wygeneruj rozliczenie
                    </Button>
                  )}
                </Stack>
              )}
              {settlement.data && (
                <Stack spacing={2}>
                  <SettlementSummary settlement={settlement.data} />
                  {!settlement.data.paid && (
                    <Box>
                      <Button
                        variant="contained"
                        color="success"
                        loading={pay.isPending}
                        onClick={() => pay.mutate(settlement.data!.id)}
                      >
                        Oznacz jako opłacone
                      </Button>
                    </Box>
                  )}
                </Stack>
              )}
            </CardContent>
          </Card>
        </>
      )}

      {protocolType && unit.data && equipment.data && (
        <ProtocolDialog
          open
          rentalId={id}
          type={protocolType}
          tankCapacity={equipment.data.tankCapacity}
          categoryType={equipment.data.categoryType}
          currentReading={unit.data.mileageOrHours}
          onClose={() => setProtocolType(null)}
          onSaved={() => {
            setProtocolType(null)
            refresh()
          }}
        />
      )}
      <DamageDialog
        protocol={damageFor}
        onClose={() => setDamageFor(null)}
        onSaved={() => {
          setDamageFor(null)
          refresh()
        }}
      />
      <ConfirmDialog
        open={confirmCancel}
        title="Anulować rezerwację?"
        message={`Umowa nr ${r.id}, klient ${r.clientName}.`}
        confirmLabel="Anuluj rezerwację"
        pending={cancel.isPending}
        onConfirm={() => cancel.mutate()}
        onClose={() => setConfirmCancel(false)}
      />
    </Stack>
  )
}
