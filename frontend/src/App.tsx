import { Route, Routes } from 'react-router'
import { RequireAuth } from './auth/RequireAuth'
import { Layout } from './components/Layout'
import { CatalogPage } from './pages/CatalogPage'
import { ComingSoonPage } from './pages/ComingSoonPage'
import { EquipmentDetailsPage } from './pages/EquipmentDetailsPage'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { RegisterPage } from './pages/RegisterPage'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<HomePage />} />
        <Route path="catalog" element={<CatalogPage />} />
        <Route path="catalog/:id" element={<EquipmentDetailsPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        {/* Strony ról dojdą w Dniu 10 */}
        <Route
          path="my-rentals"
          element={
            <RequireAuth roles={['CLIENT']}>
              <ComingSoonPage title="Moje umowy" />
            </RequireAuth>
          }
        />
        <Route
          path="rentals"
          element={
            <RequireAuth roles={['EMPLOYEE', 'ADMIN']}>
              <ComingSoonPage title="Umowy" />
            </RequireAuth>
          }
        />
        <Route
          path="admin"
          element={
            <RequireAuth roles={['ADMIN']}>
              <ComingSoonPage title="Administracja" />
            </RequireAuth>
          }
        />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
