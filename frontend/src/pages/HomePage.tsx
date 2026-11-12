import { Alert, Card, CardContent, Chip, CircularProgress, Stack, Typography } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { getCategories } from '../api/catalog'

export function HomePage() {
  const { data: categories, isPending, isError } = useQuery({
    queryKey: ['categories'],
    queryFn: getCategories,
  })

  return (
    <Stack spacing={3}>
      <div>
        <Typography variant="h4" gutterBottom>
          Wypożyczalnia samochodów i maszyn
        </Typography>
        <Typography color="text.secondary">
          Samochody, koparki, kosiarki i ciągniki na doby.
        </Typography>
      </div>

      <Typography variant="h5">Kategorie</Typography>
      {isPending && <CircularProgress />}
      {isError && <Alert severity="error">Nie udało się pobrać kategorii. Czy backend działa?</Alert>}
      {categories && (
        <Stack direction="row" spacing={2} useFlexGap sx={{ flexWrap: 'wrap' }}>
          {categories.map((category) => (
            <Card key={category.id} sx={{ width: 240 }}>
              <CardContent>
                <Typography variant="h6">{category.name}</Typography>
                <Stack direction="row" spacing={0.5} useFlexGap sx={{ flexWrap: 'wrap', mt: 1 }}>
                  {category.requiredParams.map((param) => (
                    <Chip key={param} label={param} size="small" />
                  ))}
                </Stack>
              </CardContent>
            </Card>
          ))}
        </Stack>
      )}
    </Stack>
  )
}
