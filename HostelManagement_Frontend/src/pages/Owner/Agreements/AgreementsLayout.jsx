import { Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import AgreementStepper from './AgreementStepper'
import AgreementList from './AgreementList'
import { Button, PageHeader } from '../../../components/ui'

export default function AgreementsLayout() {
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const isCreating = pathname.includes('/create')

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Agreement operations"
        title="Agreements"
        description="Manage tenant onboarding agreements, creation flows, and status tracking from a cleaner interface."
        action={isCreating ? null : <Button label="Create agreement" onClick={() => navigate('/owner/agreements/create')} />}
        secondaryAction={isCreating ? <Button label="Back to Dashboard" variant="secondary" onClick={() => navigate('/owner/dashboard')} /> : null}
      />

      <Routes>
        <Route path="/" element={<AgreementList />} />
        <Route path="/create" element={<AgreementStepper />} />
      </Routes>
    </div>
  )
}
