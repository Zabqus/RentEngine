import { useState, type ChangeEvent, type FormEvent } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  InputAdornment,
  MenuItem,
  Pagination,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import SearchIcon from '@mui/icons-material/Search'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router'
import { getEquipmentPage, type EquipmentQuery } from '../api/catalog'
import { getApiError } from '../api/errors'
import { useCategories } from '../api/useCategories'
import { EquipmentCard } from '../components/EquipmentCard'

const PAGE_SIZE = 9
const FILTER_KEYS = ['q', 'categoryId', 'minPrice', 'maxPrice', 'from', 'to'] as const
type FilterKey = (typeof FILTER_KEYS)[number]
type Filters = Record<FilterKey, string>

const SORT_OPTIONS = [
  { value: 'name,asc', label: 'Nazwa A–Z' },
  { value: 'dailyPrice,asc', label: 'Cena rosnąco' },
  { value: 'dailyPrice,desc', label: 'Cena malejąco' },
  { value: 'deposit,asc', label: 'Kaucja rosnąco' },
]

function readFilters(params: URLSearchParams): Filters {
  return Object.fromEntries(FILTER_KEYS.map((key) => [key, params.get(key) ?? ''])) as Filters
}

// Proste sprawdzenia przed wysłaniem; resztę i tak waliduje backend
function validate(filters: Filters): Partial<Filters> {
  const errors: Partial<Filters> = {}
  if (filters.minPrice && filters.maxPrice && Number(filters.minPrice) > Number(filters.maxPrice)) {
    errors.maxPrice = 'Cena „do” musi być większa od „od”.'
  }
  if (Boolean(filters.from) !== Boolean(filters.to)) {
    errors[filters.from ? 'to' : 'from'] = 'Podaj oba końce terminu.'
  } else if (filters.from && filters.to <= filters.from) {
    errors.to = 'Koniec musi być po początku.'
  }
  return errors
}

export function CatalogPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const applied = readFilters(searchParams)
  const sort = searchParams.get('sort') ?? SORT_OPTIONS[0].value
  const page = Math.max(Number(searchParams.get('page') ?? 1) || 1, 1)

  // Szkic formularza; do URL trafia po kliknięciu „Szukaj”
  const [draft, setDraft] = useState<Filters>(applied)
  const [errors, setErrors] = useState<Partial<Filters>>({})

  // Zmiana URL z zewnątrz (wstecz, link do kategorii) nadpisuje szkic
  const filtersKey = FILTER_KEYS.map((key) => applied[key]).join('|')
  const [syncedKey, setSyncedKey] = useState(filtersKey)
  if (syncedKey !== filtersKey) {
    setSyncedKey(filtersKey)
    setDraft(applied)
    setErrors({})
  }

  const categories = useCategories()

  const query: EquipmentQuery = { ...applied, sort, page: page - 1, size: PAGE_SIZE }
  const equipment = useQuery({
    queryKey: ['equipment', query],
    queryFn: () => getEquipmentPage(query),
    placeholderData: keepPreviousData,
  })

  const updateParams = (changes: Record<string, string>) => {
    const next = new URLSearchParams(searchParams)
    Object.entries(changes).forEach(([key, value]) => (value ? next.set(key, value) : next.delete(key)))
    if (!('page' in changes)) {
      next.delete('page')
    }
    setSearchParams(next)
  }

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    const validation = validate(draft)
    setErrors(validation)
    if (Object.keys(validation).length === 0) {
      updateParams({ ...draft, q: draft.q.trim() })
    }
  }

  const handleClear = () => {
    const empty = readFilters(new URLSearchParams())
    setDraft(empty)
    setErrors({})
    updateParams(empty)
  }

  const field = (key: FilterKey) => ({
    value: draft[key],
    onChange: (event: ChangeEvent<HTMLInputElement>) => {
      setDraft({ ...draft, [key]: event.target.value })
      setErrors({ ...errors, [key]: undefined })
    },
    error: Boolean(errors[key]),
    helperText: errors[key],
    size: 'small' as const,
    fullWidth: true,
  })

  const apiError = equipment.isError ? getApiError(equipment.error) : null
  const result = equipment.data

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Katalog sprzętu</Typography>

      <Card>
        <CardContent>
          <Box
            component="form"
            onSubmit={handleSubmit}
            noValidate
            sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: 'repeat(4, 1fr)' } }}
          >
            <Box sx={{ gridColumn: { sm: 'span 2' } }}>
              <TextField
                label="Szukaj po nazwie"
                {...field('q')}
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
            <TextField select label="Kategoria" {...field('categoryId')}>
              <MenuItem value="">Wszystkie</MenuItem>
              {categories.data?.map((category) => (
                <MenuItem key={category.id} value={String(category.id)}>
                  {category.name}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              select
              label="Sortowanie"
              size="small"
              fullWidth
              value={sort}
              onChange={(event) => updateParams({ sort: event.target.value })}
            >
              {SORT_OPTIONS.map((option) => (
                <MenuItem key={option.value} value={option.value}>
                  {option.label}
                </MenuItem>
              ))}
            </TextField>
            <TextField label="Cena za dobę od (zł)" type="number" {...field('minPrice')} slotProps={{ htmlInput: { min: 0 } }} />
            <TextField label="Cena za dobę do (zł)" type="number" {...field('maxPrice')} slotProps={{ htmlInput: { min: 0 } }} />
            <TextField label="Wolny od" type="datetime-local" {...field('from')} slotProps={{ inputLabel: { shrink: true } }} />
            <TextField label="Wolny do" type="datetime-local" {...field('to')} slotProps={{ inputLabel: { shrink: true } }} />
            <Stack direction="row" spacing={1} sx={{ gridColumn: '1 / -1', justifyContent: 'flex-end' }}>
              <Button onClick={handleClear}>Wyczyść</Button>
              <Button type="submit" variant="contained" startIcon={<SearchIcon />}>
                Szukaj
              </Button>
            </Stack>
          </Box>
        </CardContent>
      </Card>

      {apiError && (
        <Alert severity="error">
          {apiError.message}
          {Object.values(apiError.fieldErrors).map((message) => (
            <div key={message}>{message}</div>
          ))}
        </Alert>
      )}

      {equipment.isPending && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress />
        </Box>
      )}

      {result && (
        <>
          <Typography color="text.secondary">
            Znaleziono: {result.totalElements}
            {applied.from && applied.to && ' (z wolnym egzemplarzem w wybranym terminie)'}
          </Typography>

          {result.content.length === 0 ? (
            <Alert severity="info">Brak sprzętu spełniającego kryteria. Zmień filtry.</Alert>
          ) : (
            <Box
              sx={{
                display: 'grid',
                gap: 2,
                gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))',
                opacity: equipment.isPlaceholderData ? 0.6 : 1,
              }}
            >
              {result.content.map((item) => (
                <EquipmentCard key={item.id} equipment={item} />
              ))}
            </Box>
          )}

          {result.totalPages > 1 && (
            <Pagination
              sx={{ alignSelf: 'center' }}
              color="primary"
              count={result.totalPages}
              page={page}
              onChange={(_, value) => updateParams({ page: value > 1 ? String(value) : '' })}
            />
          )}
        </>
      )}
    </Stack>
  )
}
