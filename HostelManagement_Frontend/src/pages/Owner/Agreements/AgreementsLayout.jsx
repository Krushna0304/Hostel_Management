import { useState } from 'react'
import { Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import AgreementStepper from './AgreementStepper'
import AgreementList from './AgreementList'
import ExistingTenantOnboarding from './ExistingTenantOnboarding'
import { Button, PageHeader } from '../../../components/ui'

export default function AgreementsLayout() {
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const isCreating = pathname.includes('/create')
  const [searchQuery, setSearchQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')

  return (
    <div className="space-y-6">
      <PageHeader
        title="Agreements"
        toolbar={isCreating ? (
          <Button label="Back to Dashboard" variant="secondary" onClick={() => navigate('/owner/dashboard')} />
        ) : (
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
            <input
              type="search"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              placeholder="Search agreements..."
              className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900 outline-none transition focus:border-sky-500 focus:ring-2 focus:ring-sky-100 sm:w-56"
            />
            <select
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value)}
              aria-label="Filter agreements"
              className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm font-medium text-slate-700 outline-none transition focus:border-sky-500 focus:ring-2 focus:ring-sky-100"
            >
              <option value="ALL">Filter</option>
              <option value="ACTIVE">Active</option>
              <option value="PENDING_TENANT_ACTION">Pending</option>
              <option value="REJECTED">Rejected</option>
            </select>
            <Button label="Create agreement" onClick={() => navigate('/owner/agreements/create')} />
            <Button label="Onboard Existing Tenant" variant="secondary" onClick={() => navigate('/owner/agreements/onboard-existing')} />
          </div>
        )}
      />

      <Routes>
        <Route path="/" element={<AgreementList searchQuery={searchQuery} statusFilter={statusFilter} />} />
        <Route path="/create" element={<AgreementStepper />} />
        <Route path="/onboard-existing" element={<ExistingTenantOnboarding />} />
      </Routes>
    </div>
  )
}
