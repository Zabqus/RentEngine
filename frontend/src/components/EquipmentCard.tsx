import { Box, Card, CardActionArea, CardContent, Chip, Stack, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router'
import type { Equipment } from '../api/catalog'
import { formatMoney, formatParam, paramLabel } from '../utils/format'

export function EquipmentCard({ equipment }: { equipment: Equipment }) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardActionArea component={RouterLink} to={`/catalog/${equipment.id}`} sx={{ height: '100%', alignItems: 'stretch' }}>
        <CardContent sx={{ height: '100%', display: 'flex', flexDirection: 'column', gap: 1.5 }}>
          <Chip label={equipment.categoryName} size="small" sx={{ alignSelf: 'flex-start' }} />
          <Typography variant="h6" sx={{ lineHeight: 1.3 }}>
            {equipment.name}
          </Typography>
          <Stack spacing={0.25} sx={{ flex: 1 }}>
            {Object.entries(equipment.params)
              .slice(0, 3)
              .map(([key, value]) => (
                <Typography key={key} variant="body2" color="text.secondary">
                  {paramLabel(key)}: {formatParam(value)}
                </Typography>
              ))}
          </Stack>
          <Box>
            <Typography variant="h6" color="primary" component="span">
              {formatMoney(equipment.dailyPrice)}
            </Typography>
            <Typography variant="body2" color="text.secondary" component="span">
              {' '}
              / doba
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Kaucja {formatMoney(equipment.deposit)}
            </Typography>
          </Box>
        </CardContent>
      </CardActionArea>
    </Card>
  )
}
