import { useNavigate } from 'react-router-dom'
import { Button } from '../../../components/ui'

export default function AgreementTypeStep({ nextStep, setFormData }) {
  const navigate = useNavigate()

  const handleTypeSelect = (type) => {
    setFormData((prev) => ({ ...prev, agreementType: type }))
    nextStep()
  }

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-semibold text-slate-950">Choose agreement type</h2>
        <p className="mt-2 text-sm leading-6 text-slate-500">
          Start with the agreement flow you need. The next steps will adapt the user role and property selection flow automatically.
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <button
          type="button"
          onClick={() => handleTypeSelect('ROOM')}
          className="rounded-3xl border border-slate-200 bg-slate-50 p-6 text-left transition hover:border-sky-200 hover:bg-white hover:shadow-lg"
        >
          <p className="text-lg font-semibold text-slate-950">Room agreement</p>
          <p className="mt-2 text-sm leading-6 text-slate-500">Use this for tenant onboarding tied to a room, plan, and activation QR.</p>
        </button>

        <button
          type="button"
          onClick={() => handleTypeSelect('WORKER')}
          className="rounded-3xl border border-slate-200 bg-slate-50 p-6 text-left transition hover:border-sky-200 hover:bg-white hover:shadow-lg"
        >
          <p className="text-lg font-semibold text-slate-950">Worker agreement</p>
          <p className="mt-2 text-sm leading-6 text-slate-500">Create agreements for operational staff such as cleaners with role-aware user selection.</p>
        </button>

        <button
          type="button"
          onClick={() => handleTypeSelect('FLAT')}
          className="rounded-3xl border border-slate-200 bg-slate-50 p-6 text-left transition hover:border-sky-200 hover:bg-white hover:shadow-lg"
        >
          <p className="text-lg font-semibold text-slate-950">Flat agreement</p>
          <p className="mt-2 text-sm leading-6 text-slate-500">Create agreements for flat units with a single primary tenant and optional co-tenant names.</p>
        </button>
      </div>

      <Button label="Cancel" variant="secondary" onClick={() => navigate('/owner/agreements')} />
    </div>
  )
}
