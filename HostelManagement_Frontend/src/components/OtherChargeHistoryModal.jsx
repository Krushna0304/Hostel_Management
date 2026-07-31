import { useState } from 'react'
import { Badge, Button } from './ui'

const statusConfig = {
  PENDING: { color: 'yellow', label: 'Upcoming' },
  PARTIALLY_PAID: { color: 'blue', label: 'Partially paid' },
  COMPLETED: { color: 'green', label: 'Paid' },
  OVERDUE: { color: 'red', label: 'Overdue' }
}

export default function OtherChargeHistoryModal({ tenant, onClose }) {
  const [filter, setFilter] = useState('ALL')
  const now = new Date()
  const fmt = (amount) => `₹${Number(amount || 0).toLocaleString()}`
  const formatDate = (value) => value
    ? new Intl.DateTimeFormat('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(value))
    : 'No due date'

  const entries = tenant.charges.map((charge) => {
    const roomShare = charge.category === 'OTHER_CHARGE_ROOM'
      ? charge.roomTenants?.find((roomTenant) => roomTenant.tenantId === tenant.tenantId)
      : null
    const amount = roomShare?.splitAmount ?? charge.amount
    const status = roomShare?.paymentStatus ?? charge.paymentStatus ?? 'PENDING'
    const paidAmount = status === 'COMPLETED' ? amount : (charge.paidAmount || 0)
    const remainingAmount = status === 'COMPLETED' ? 0 : (roomShare ? amount : (charge.remainingAmount ?? amount))
    const overdue = status !== 'COMPLETED' && charge.dueDate && new Date(charge.dueDate) < now

    return {
      id: `${charge.chargeId}-${tenant.tenantId}`,
      chargeName: charge.chargeName,
      category: charge.category === 'OTHER_CHARGE_ROOM' ? 'Room charge' : 'Tenant charge',
      amount,
      paidAmount,
      remainingAmount,
      status: overdue ? 'OVERDUE' : status,
      dueDate: charge.dueDate,
      paidDate: roomShare?.paidAt ?? charge.paidDate
    }
  })

  const paid = entries.filter((entry) => entry.status === 'COMPLETED')
  const upcoming = entries.filter((entry) => entry.status !== 'COMPLETED' && entry.status !== 'OVERDUE')
  const overdue = entries.filter((entry) => entry.status === 'OVERDUE')
  const visibleEntries = filter === 'ALL' ? entries : filter === 'PAID' ? paid : filter === 'UPCOMING' ? upcoming : overdue
  const total = entries.reduce((sum, entry) => sum + Number(entry.amount || 0), 0)
  const totalPaid = entries.reduce((sum, entry) => sum + Number(entry.paidAmount || 0), 0)
  const remaining = entries.reduce((sum, entry) => sum + Number(entry.remainingAmount || 0), 0)

  const filters = [
    ['ALL', 'All', entries.length],
    ['PAID', 'Paid', paid.length],
    ['UPCOMING', 'Upcoming', upcoming.length],
    ['OVERDUE', 'Overdue', overdue.length]
  ]

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 p-4" role="dialog" aria-modal="true" aria-labelledby="other-charge-history-title">
      <div className="flex max-h-[90vh] w-full max-w-2xl flex-col overflow-hidden rounded-2xl bg-white shadow-2xl">
        <div className="flex items-start justify-between border-b border-slate-100 px-6 py-5">
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.2em] text-sky-600">Other charge history</p>
            <h2 id="other-charge-history-title" className="mt-1 text-xl font-semibold text-slate-950">{tenant.tenantName}</h2>
            <p className="mt-1 text-sm text-slate-500">{tenant.hostelName} · Room {tenant.roomNumber}</p>
          </div>
          <button type="button" onClick={onClose} className="rounded-lg p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-700" aria-label="Close history">✕</button>
        </div>

        <div className="overflow-y-auto px-6 py-5">
          <section className="rounded-2xl bg-slate-950 p-5 text-white">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-xs font-bold uppercase tracking-[0.16em] text-sky-300">Total paid</p>
                <p className="mt-1 text-4xl font-bold">{fmt(totalPaid)}</p>
              </div>
              <div className="text-right">
                <p className="text-sm text-slate-400">Remaining</p>
                <p className="mt-1 text-xl font-semibold">{fmt(remaining)}</p>
              </div>
            </div>
            <div className="mt-5 h-2 overflow-hidden rounded-full bg-slate-700">
              <div className="h-full rounded-full bg-sky-400" style={{ width: total ? `${Math.min((totalPaid / total) * 100, 100)}%` : '0%' }} />
            </div>
            <div className="mt-2 flex justify-between text-sm text-slate-400"><span>{total ? Math.round((totalPaid / total) * 100) : 0}% paid</span><span>{overdue.length} overdue</span></div>
          </section>

          <div className="mt-5 grid grid-cols-2 gap-2 sm:grid-cols-4">
            {filters.map(([value, label, count]) => (
              <button key={value} type="button" onClick={() => setFilter(value)} className={`rounded-xl px-3 py-2 text-sm font-semibold transition ${filter === value ? 'bg-slate-950 text-white' : 'bg-slate-100 text-slate-500 hover:bg-slate-200'}`}>
                {label} <span className={`ml-1 rounded-full px-1.5 py-0.5 text-xs ${filter === value ? 'bg-white/15 text-white' : 'bg-white text-slate-500'}`}>{count}</span>
              </button>
            ))}
          </div>

          <div className="mt-5 space-y-3">
            {visibleEntries.map((entry) => {
              const config = statusConfig[entry.status] || statusConfig.PENDING
              return <article key={entry.id} className="rounded-xl border border-slate-100 p-4 shadow-sm">
                <div className="flex justify-between gap-3">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2"><h3 className="font-semibold text-slate-900">{entry.chargeName}</h3><Badge color={config.color}>{config.label}</Badge></div>
                    <p className="mt-1 text-sm text-slate-500">{entry.category} · {entry.status === 'COMPLETED' ? `Paid on ${formatDate(entry.paidDate)}` : `Due ${formatDate(entry.dueDate)}`}</p>
                  </div>
                  <div className="shrink-0 text-right"><p className={`font-bold ${entry.status === 'COMPLETED' ? 'text-emerald-600' : 'text-slate-950'}`}>{fmt(entry.amount)}</p>{entry.status !== 'COMPLETED' && <p className="mt-1 text-xs text-slate-500">{fmt(entry.remainingAmount)} due</p>}</div>
                </div>
              </article>
            })}
            {!visibleEntries.length && <p className="py-8 text-center text-sm text-slate-500">No {filter.toLowerCase()} charges.</p>}
          </div>
        </div>

        <div className="border-t border-slate-100 p-5"><Button variant="primary" className="w-full" onClick={onClose}>Close</Button></div>
      </div>
    </div>
  )
}
