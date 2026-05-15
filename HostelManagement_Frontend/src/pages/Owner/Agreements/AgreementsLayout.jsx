import { Route, Routes, useNavigate } from 'react-router-dom'
import AgreementStepper from './AgreementStepper'
import AgreementList from './AgreementList'
import { Button, PageHeader } from '../../../components/ui'

export default function AgreementsLayout() {
  const navigate = useNavigate()

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Agreement operations"
        title="Agreements"
        description="Manage tenant onboarding agreements, creation flows, and status tracking from a cleaner interface."
        action={<Button label="Create agreement" onClick={() => navigate('/owner/agreements/create')} />}
        secondaryAction={<Button label="Back to dashboard" variant="secondary" onClick={() => navigate('/owner/dashboard')} />}
      />

      <Routes>
        <Route path="/" element={<AgreementList />} />
        <Route path="/create" element={<AgreementStepper />} />
      </Routes>
    </div>
  )
}
