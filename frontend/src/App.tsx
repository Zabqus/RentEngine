import { Route, Routes } from 'react-router'
import { RequireAuth } from './auth/RequireAuth'
import { Layout } from './components/Layout'
import { CatalogPage } from './pages/CatalogPage'
import { ComingSoonPage } from './pages/ComingSoonPage'
import { EquipmentDetailsPage } from './pages/EquipmentDetailsPage'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { MyRentalsPage } from './pages/MyRentalsPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { RegisterPage } from './pages/RegisterPage'
import { RentalDetailsPage } from './pages/RentalDetailsPage'
import { RentalsPage } from './pages/RentalsPage'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<HomePage />} />
        <Route path="catalog" element={<CatalogPage />} />
        <Route path="catalog/:id" element={<EquipmentDetailsPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route
          path="my-rentals"
          element={
            <RequireAuth roles={['CLIENT']}>
              <MyRentalsPage />
            </RequireAuth>
          }
        />
        <Route
          path="rentals"
          element={
            <RequireAuth roles={['EMPLOYEE', 'ADMIN']}>
              <RentalsPage />
            </RequireAuth>
          }
        />
        <Route
          path="rentals/:id"
          element={
            <RequireAuth roles={['EMPLOYEE', 'ADMIN']}>
              <RentalDetailsPage />
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
