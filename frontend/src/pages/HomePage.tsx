import { Alert, Box, Button, Card, CardActionArea, CardContent, CircularProgress, Stack, Typography } from '@mui/material'
import ArrowForwardIcon from '@mui/icons-material/ArrowForward'
import { Link as RouterLink } from 'react-router'
import { useCategories } from '../api/useCategories'
import { paramLabel } from '../utils/format'

export function HomePage() {
  const { data: categories, isPending, isError } = useCategories()

  return (
    <Stack spacing={3}>
      <Box>
        <Typography variant="h4" gutterBottom>
          Wypożyczalnia samochodów i maszyn
        </Typography>
        <Typography color="text.secondary" gutterBottom>
          Samochody, koparki, kosiarki i ciągniki na doby.
        </Typography>
        <Button variant="contained" component={RouterLink} to="/catalog" endIcon={<ArrowForwardIcon />} sx={{ mt: 1 }}>
          Przeglądaj katalog
        </Button>
      </Box>

      <Typography variant="h5">Kategorie</Typography>
      {isPending && <CircularProgress />}
      {isError && <Alert severity="error">Nie udało się pobrać kategorii. Czy backend działa?</Alert>}
      {categories && (
        <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))' }}>
          {categories.map((category) => (
            <Card key={category.id}>
              <CardActionArea component={RouterLink} to={`/catalog?categoryId=${category.id}`} sx={{ height: '100%' }}>
                <CardContent>
                  <Typography variant="h6">{category.name}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {category.requiredParams.map(paramLabel).join(' · ')}
                  </Typography>
                </CardContent>
              </CardActionArea>
            </Card>
          ))}
        </Box>
      )}
    </Stack>
  )
}
