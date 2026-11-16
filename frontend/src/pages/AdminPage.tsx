import { Stack, Tab, Tabs, Typography } from '@mui/material'
import { useSearchParams } from 'react-router'
import { EquipmentAdminTab } from '../components/admin/EquipmentAdminTab'
import { UsersAdminTab } from '../components/admin/UsersAdminTab'

export function AdminPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const tab = searchParams.get('tab') === 'users' ? 'users' : 'equipment'

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Administracja</Typography>
      <Tabs value={tab} onChange={(_, value) => setSearchParams(value === 'users' ? { tab: 'users' } : {})}>
        <Tab value="equipment" label="Sprzęt" />
        <Tab value="users" label="Użytkownicy" />
      </Tabs>
      {tab === 'equipment' ? <EquipmentAdminTab /> : <UsersAdminTab />}
    </Stack>
  )
}
