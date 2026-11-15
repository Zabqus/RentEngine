import type { CategoryType } from '../api/catalog'

const money = new Intl.NumberFormat('pl-PL', { style: 'currency', currency: 'PLN' })
const number = new Intl.NumberFormat('pl-PL', { maximumFractionDigits: 2 })

export function formatMoney(value: number): string {
  return money.format(value)
}

export function formatNumber(value: number): string {
  return number.format(value)
}

// Nazwy parametrów sprzętu z JSONB (klucze jak w backendzie)
const PARAM_LABELS: Record<string, string> = {
  powerKw: 'Moc (kW)',
  powerHp: 'Moc (KM)',
  weightKg: 'Masa (kg)',
  digDepthM: 'Głębokość kopania (m)',
  cuttingWidthCm: 'Szerokość koszenia (cm)',
  seats: 'Liczba miejsc',
  gearbox: 'Skrzynia biegów',
  drive: 'Napęd',
}

const VALUE_LABELS: Record<string, string> = {
  MANUAL: 'manualna',
  AUTOMATIC: 'automatyczna',
}

export function paramLabel(key: string): string {
  return PARAM_LABELS[key] ?? key
}

export function formatParam(value: string | number | boolean): string {
  if (typeof value === 'number') return formatNumber(value)
  if (typeof value === 'boolean') return value ? 'tak' : 'nie'
  return VALUE_LABELS[value] ?? value
}

// Samochody mają przebieg w km, maszyny licznik motogodzin
export function counterLabel(type: CategoryType): string {
  return type === 'CAR' ? 'Przebieg (km)' : 'Motogodziny'
}
