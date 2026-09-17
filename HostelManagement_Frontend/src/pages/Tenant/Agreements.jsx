import { useEffect, useMemo, useState } from 'react'
import { Alert, Badge, Button, Card, CardContent, EmptyState, PageHeader, Skeleton } from '../../components/ui'
import { tenantService } from '../../services/agreementService'

const todayKey = () => new Date().toLocaleDateString('en-CA')
const dateKey = (value) => value ? String(value).slice(0, 10) : null
const formatDate = (value) => value ? new Date(value).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '—'

function groupFor(agreement) {
  const today = todayKey()
  const start = dateKey(agreement.startDate)
  const end = dateKey(agreement.endDate)
  if (agreement.status === 'PENDING_TENANT_ACTION') return 'action'
  if (agreement.status === 'ACTIVE' && start && start <= today && (!end || end >= today)) return 'current'
  if (start && start > today) return 'upcoming'
  return 'previous'
}

const groups = [
  ['current', 'Current agreement', 'The agreement in effect today.'],
  ['action', 'Action required', 'Review and accept agreements waiting for you.'],
  ['upcoming', 'Upcoming agreements', 'Accepted agreements that begin in the future.'],
  ['previous', 'Previous agreements', 'Your completed, expired, rejected, or historical agreements.'],
]

function AgreementCard({ agreement, group }) {
  const variant = agreement.status === 'ACTIVE' ? 'success' : agreement.status === 'PENDING_TENANT_ACTION' ? 'warning' : 'neutral'
  return <Card><CardContent>
    <div className="flex flex-wrap items-start justify-between gap-3">
      <div><p className="font-semibold text-slate-950">{agreement.planSnapshot?.planName || `${agreement.type?.replaceAll('_', ' ') || 'Tenant'} Agreement`}</p><p className="mt-1 text-sm text-slate-500">Room {agreement.roomNumber || '—'} · {formatDate(agreement.startDate)} – {formatDate(agreement.endDate)}</p></div>
      <Badge variant={variant}>{agreement.status?.replaceAll('_', ' ') || 'UNKNOWN'}</Badge>
    </div>
    <div className="mt-4 flex flex-wrap gap-x-6 gap-y-2 text-sm text-slate-600"><span>Agreement ID: <span className="font-mono text-xs text-slate-800">{agreement.id}</span></span>{agreement.planSnapshot?.rentDetails?.monthlyRent != null && <span>Monthly rent: ₹{Number(agreement.planSnapshot.rentDetails.monthlyRent).toLocaleString('en-IN')}</span>}</div>
    {group === 'action' && agreement.qrToken && <div className="mt-4"><Button label="Review agreement" size="sm" onClick={() => { window.location.href = `/tenant/activate?token=${agreement.qrToken}` }} /></div>}
  </CardContent></Card>
}

export default function TenantAgreements() {
  const [agreements, setAgreements] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const load = async () => { try { setLoading(true); setError(''); const result = await tenantService.getTenantAgreements(); setAgreements(Array.isArray(result) ? result : result?.data || []) } catch (err) { setError(err?.response?.data?.message || 'Failed to load your agreements.') } finally { setLoading(false) } }
  useEffect(() => { load() }, [])
  const grouped = useMemo(() => agreements.reduce((result, agreement) => { result[groupFor(agreement)].push(agreement); return result }, { current: [], action: [], upcoming: [], previous: [] }), [agreements])
  return <div className="space-y-6">
    <PageHeader title="My Agreements" description="Each extension is retained as a separate agreement for your records." secondaryAction={<Button label="Refresh" variant="secondary" onClick={load} />} />
    {error && <Alert tone="error">{error}</Alert>}
    {loading ? <Skeleton className="h-56 w-full" /> : agreements.length === 0 ? <EmptyState title="No agreements yet" description="Your agreements will appear here once they are created." /> : groups.map(([key, title, description]) => grouped[key].length > 0 && <section key={key} className="space-y-3"><div><h2 className="text-lg font-semibold text-slate-950">{title}</h2><p className="text-sm text-slate-500">{description}</p></div><div className="grid gap-3 xl:grid-cols-2">{grouped[key].map((agreement) => <AgreementCard key={agreement.id} agreement={agreement} group={key} />)}</div></section>)}
  </div>
}
