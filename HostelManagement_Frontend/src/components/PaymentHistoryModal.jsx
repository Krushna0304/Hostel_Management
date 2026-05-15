import { useEffect, useState } from 'react'
import { Alert, Badge, Skeleton } from './ui'

// ── helpers ───────────────────────────────────────────────────────────────────

const fmt = (a) => `₹${(a ?? 0).toLocaleString('en-IN')}`

function formatDate(d) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })
}

function formatDateTime(d) {
  if (!d) return '—'
  return new Date(d).toLocaleString('en-IN', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

function statusConfig(status) {
  switch (status) {
    case 'COMPLETED':    return { variant: 'success', label: 'Paid',     icon: '✓' }
    case 'OVERDUE':      return { variant: 'danger',  label: 'Overdue',  icon: '!' }
    case 'PARTIALLY_PAID': return { variant: 'warning', label: 'Partial', icon: '~' }
    case 'SCHEDULED':    return { variant: 'neutral', label: 'Upcoming', icon: '○' }
    default:             return { variant: 'neutral', label: status,     icon: '○' }
  }
}

function modeIcon(mode) {
  if (mode === 'ONLINE') return '💳'
  if (mode === 'CASH')   return '💵'
  return '—'
}

// ── Summary bar ───────────────────────────────────────────────────────────────

function SummaryBar({ ledger }) {
  const paid    = ledger.totalPaid ?? 0
  const pending = ledger.totalPending ?? 0
  const total   = paid + pending
  const pct     = total > 0 ? Math.round((paid / total) * 100) : 0

  return (
    <div className="rounded-2xl bg-slate-950 p-5 text-white">
      <div className="flex items-end justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-300">Total paid</p>
          <p className="mt-1 text-3xl font-bold">{fmt(paid)}</p>
        </div>
        <div className="text-right">
          <p className="text-xs text-slate-400">Remaining</p>
          <p className="text-lg font-semibold text-slate-200">{fmt(pending)}</p>
        </div>
      </div>

      {/* Progress bar */}
      <div className="mt-4">
        <div className="h-2 w-full rounded-full bg-white/10">
          <div
            className="h-2 rounded-full bg-sky-400 transition-all duration-500"
            style={{ width: `${pct}%` }}
          />
        </div>
        <div className="mt-1.5 flex justify-between text-xs text-slate-400">
          <span>{pct}% paid</span>
          <span>{ledger.overdueCount ?? 0} overdue</span>
        </div>
      </div>

      <div className="mt-4 grid grid-cols-3 gap-3 border-t border-white/10 pt-4 text-center text-xs">
        <div>
          <p className="text-slate-400">Installment</p>
          <p className="mt-0.5 font-semibold">{fmt(ledger.installmentAmount)}</p>
        </div>
        <div>
          <p className="text-slate-400">Start</p>
          <p className="mt-0.5 font-semibold">{formatDate(ledger.startDate)}</p>
        </div>
        <div>
          <p className="text-slate-400">End</p>
          <p className="mt-0.5 font-semibold">{formatDate(ledger.endDate)}</p>
        </div>
      </div>
    </div>
  )
}

// ── Single transaction card (GPay style) ─────────────────────────────────────

function TransactionCard({ inst }) {
  const cfg = statusConfig(inst.paymentStatus)
  const isCompleted = inst.paymentStatus === 'COMPLETED'

  return (
    <div className="flex items-center gap-4 rounded-2xl border border-slate-100 bg-white px-4 py-3.5 shadow-sm">
      {/* Icon circle */}
      <div
        className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-full text-lg font-bold
          ${isCompleted ? 'bg-emerald-100 text-emerald-700' :
            inst.paymentStatus === 'OVERDUE' ? 'bg-rose-100 text-rose-600' :
            inst.paymentStatus === 'PARTIALLY_PAID' ? 'bg-amber-100 text-amber-700' :
            'bg-slate-100 text-slate-500'}`}
      >
        {cfg.icon}
      </div>

      {/* Main info */}
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <p className="text-sm font-semibold text-slate-900">
            Installment #{inst.installmentNumber}
          </p>
          <Badge variant={cfg.variant}>{cfg.label}</Badge>
          {inst.paymentMode && (
            <span className="text-xs text-slate-400">
              {modeIcon(inst.paymentMode)} {inst.paymentMode}
            </span>
          )}
        </div>
        <p className="mt-0.5 text-xs text-slate-500">
          {isCompleted
            ? `Paid on ${formatDateTime(inst.paidAt)}`
            : `Due ${formatDate(inst.dueDate)}`}
        </p>
        {inst.lateFeeApplied > 0 && (
          <p className="mt-0.5 text-xs text-rose-500">
            Late fee: {fmt(inst.lateFeeApplied)}
          </p>
        )}
      </div>

      {/* Amount */}
      <div className="text-right">
        <p className={`text-base font-bold ${isCompleted ? 'text-emerald-700' : 'text-slate-900'}`}>
          {isCompleted ? fmt(inst.paidAmount) : fmt(inst.amount)}
        </p>
        {inst.paymentStatus === 'PARTIALLY_PAID' && (
          <p className="text-xs text-amber-600">
            {fmt(inst.paidAmount)} / {fmt(inst.amount)}
          </p>
        )}
      </div>
    </div>
  )
}

// ── Main modal ────────────────────────────────────────────────────────────────

/**
 * PaymentHistoryModal
 *
 * Props:
 *   tenantName  — string, shown in header (optional for tenant-self view)
 *   ledger      — PaymentLedgerResponse (pre-fetched) OR null to trigger fetch
 *   fetchLedger — async fn() → PaymentLedgerResponse (called when ledger is null)
 *   onClose     — fn()
 */
export default function PaymentHistoryModal({ tenantName, ledger: initialLedger, fetchLedger, onClose }) {
  const [ledger, setLedger]   = useState(initialLedger || null)
  const [loading, setLoading] = useState(!initialLedger)
  const [error, setError]     = useState('')
  const [filter, setFilter]   = useState('ALL') // ALL | COMPLETED | PENDING | OVERDUE

  useEffect(() => {
    if (!initialLedger && fetchLedger) {
      load()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const load = async () => {
    try {
      setLoading(true)
      setError('')
      const data = await fetchLedger()
      setLedger(data)
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to load payment history.')
    } finally {
      setLoading(false)
    }
  }

  // Filter installments
  const filtered = (ledger?.installments ?? []).filter((inst) => {
    if (filter === 'ALL')       return true
    if (filter === 'COMPLETED') return inst.paymentStatus === 'COMPLETED'
    if (filter === 'OVERDUE')   return inst.paymentStatus === 'OVERDUE'
    if (filter === 'PENDING')   return inst.paymentStatus === 'SCHEDULED' || inst.paymentStatus === 'PARTIALLY_PAID'
    return true
  })

  // Counts for filter tabs
  const counts = (ledger?.installments ?? []).reduce((acc, inst) => {
    acc.ALL++
    if (inst.paymentStatus === 'COMPLETED')    acc.COMPLETED++
    if (inst.paymentStatus === 'OVERDUE')      acc.OVERDUE++
    if (inst.paymentStatus === 'SCHEDULED' || inst.paymentStatus === 'PARTIALLY_PAID') acc.PENDING++
    return acc
  }, { ALL: 0, COMPLETED: 0, OVERDUE: 0, PENDING: 0 })

  const tabs = [
    { key: 'ALL',       label: 'All',      count: counts.ALL },
    { key: 'COMPLETED', label: 'Paid',     count: counts.COMPLETED },
    { key: 'PENDING',   label: 'Upcoming', count: counts.PENDING },
    { key: 'OVERDUE',   label: 'Overdue',  count: counts.OVERDUE },
  ]

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4"
      onClick={onClose}
    >
      <div
        className="relative flex w-full max-w-lg flex-col rounded-3xl bg-slate-50 shadow-2xl"
        style={{ maxHeight: '92vh' }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* ── Header ── */}
        <div className="flex items-center justify-between rounded-t-3xl bg-white px-6 py-4 shadow-sm">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-600">
              Payment History
            </p>
            <h2 className="mt-0.5 text-lg font-bold text-slate-950">
              {tenantName ? tenantName : 'My Payments'}
            </h2>
          </div>
          <button
            onClick={onClose}
            className="rounded-full p-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
            aria-label="Close"
          >
            ✕
          </button>
        </div>

        {/* ── Scrollable body ── */}
        <div className="flex-1 overflow-y-auto px-5 py-5 space-y-4">
          {error && <Alert tone="error">{error}</Alert>}

          {loading ? (
            <div className="space-y-3">
              <Skeleton className="h-36 rounded-2xl" />
              {Array.from({ length: 5 }).map((_, i) => (
                <Skeleton key={i} className="h-16 rounded-2xl" />
              ))}
            </div>
          ) : ledger ? (
            <>
              {/* Summary */}
              <SummaryBar ledger={ledger} />

              {/* Filter tabs */}
              <div className="flex gap-1 rounded-xl bg-white p-1 shadow-sm">
                {tabs.map((tab) => (
                  <button
                    key={tab.key}
                    onClick={() => setFilter(tab.key)}
                    className={`flex flex-1 items-center justify-center gap-1.5 rounded-lg px-2 py-2 text-xs font-semibold transition
                      ${filter === tab.key
                        ? 'bg-slate-950 text-white shadow'
                        : 'text-slate-500 hover:text-slate-800'}`}
                  >
                    {tab.label}
                    <span
                      className={`rounded-full px-1.5 py-0.5 text-[10px] font-bold
                        ${filter === tab.key ? 'bg-white/20 text-white' : 'bg-slate-100 text-slate-600'}`}
                    >
                      {tab.count}
                    </span>
                  </button>
                ))}
              </div>

              {/* Transaction list */}
              {filtered.length === 0 ? (
                <div className="rounded-2xl border border-slate-200 bg-white px-6 py-10 text-center">
                  <p className="text-sm text-slate-500">No transactions in this category.</p>
                </div>
              ) : (
                <div className="space-y-2">
                  {filtered.map((inst) => (
                    <TransactionCard key={inst.scheduleId} inst={inst} />
                  ))}
                </div>
              )}
            </>
          ) : null}
        </div>

        {/* ── Footer ── */}
        <div className="rounded-b-3xl bg-white px-5 py-4 shadow-[0_-1px_0_0_rgba(0,0,0,0.06)]">
          <button
            onClick={onClose}
            className="w-full rounded-2xl bg-slate-950 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  )
}
