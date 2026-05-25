import { lazy, Suspense } from 'react'
import { LoadingScreen } from '../../components/ui'

const TenantElectricityBills = lazy(() => import('../../components/TenantElectricityBills'))

export default function ElectricityBills() {
  return (
    <Suspense fallback={<LoadingScreen />}>
      <TenantElectricityBills />
    </Suspense>
  )
}