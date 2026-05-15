import { useEffect, useMemo, useState } from 'react'
import { Alert, Badge, Button, PageHeader, Skeleton } from '../../components/ui'
import { paymentService } from '../../services/paymentService'

// ── helpers ───────────────────────────────────────────────────────────────────

const fmt = (a) =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(a ?? 0)

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

/** Group transactions by calendar date label */
function groupByDate(txns) {
  const groups = {}
  txns.forEach((tx) => {
    const d = new Date(tx.createdAt)
    const today = new Date()
    const yesterday = new Date(today)
    yesterday.setDate(today.getDate() - 1)

    let label
    if (d.toDateString() === today.toDateString()) label = 'Today'
    else if (d.toDateString() === yesterday.toDateString()) label = 'Yesterday'
    else label = formatDate(tx.createdAt)

    if (!groups[label]) groups[label] = []
    groups[label].push(tx)
  })
  return groups
}

// ── Status / mode config ──────────────────────────────────────────────────────

function statusBadge(status) {
  switch (status) {
    case 'COMPLETED': return { variant: 'success', label: 'Success' }
    case 'FAILED':    return { variant: 'danger',  label: 'Failed' }
    case 'PENDING':   return { variant: 'warning', label: 'Pending' }
    case 'CANCELLED': return { variant: 'neutral', label: 'Cancelled' }
    default:          return { variant: 'neutral', label: status }
  }
}

function ModeChip({ mode }) {
  return (
    <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-semibold
      ${mode === 'ONLINE' ? 'bg-sky-100 text-sky-700' : 'bg-amber-100 text-amber-700'}`}>
      {mode === 'ONLINE' ? '💳' : '💵'} {mode}
    </span>
  )
}

// ── Summary strip ─────────────────────────────────────────────────────────────

function SummaryStrip({ transactions }) {
  const { totalReceived, totalSent, net } = useMemo(() => {
    let received = 0, sent = 0
    transactions.forEach((tx) => {
      if (tx.status !== 'COMPLETED') return
      if (tx.direction === 'RECEIVED') received += tx.amount
      else sent += tx.amount
    })
    return { totalReceived: received, totalSent: sent, net: received - sent }
  }, [transactions])

  return (
    <div className="grid grid-cols-3 gap-3">
      <div className="rounded-2xl bg-emerald-50 border border-emerald-200 px-4 py-4 text-center">
        <p className="text-xs font-semibold uppercase tracking-[0.15em] text-emerald-600">Received</p>
        <p className="mt-1 text-xl font-bold text-emerald-700">{fmt(totalReceived)}</p>
      </div>
      <div className="rounded-2xl bg-rose-50 border border-rose-200 px-4 py-4 text-center">
        <p className="text-xs font-semibold uppercase tracking-[0.15em] text-rose-600">Sent</p>
        <p className="mt-1 text-xl font-bold text-rose-700">{fmt(totalSent)}</p>
      </div>
      <div className="rounded-2xl bg-slate-950 px-4 py-4 text-center">
        <p className="text-xs font-semibold uppercase tracking-[0.15em] text-slate-400">Net</p>
        <p className={`mt-1 text-xl font-bold ${net >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
          {net >= 0 ? '+' : ''}{fmt(net)}
        </p>
      </div>
    </div>
  )
}

// ── Single transaction row ────────────────────────────────────────────────────

function TransactionRow({ tx, onClick }) {
  const isReceived = tx.direction === 'RECEIVED'
  const badge = statusBadge(tx.status)

  return (
    <button
      onClick={() => onClick(tx)}
      className="flex w-full items-center gap-4 rounded-2xl border border-slate-100 bg-white px-4 py-3.5 text-left shadow-sm transition hover:border-slate-200 hover:shadow-md active:scale-[0.99]"
    >
      {/* Direction circle */}
      <div className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-full text-xl
        ${isReceived
          ? 'bg-emerald-100 text-emerald-600'
          : 'bg-rose-100 text-rose-600'}`}>
        {isReceived ? '↓' : '↑'}
      </div>

      {/* Main info */}
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2 flex-wrap">
          <p className="text-sm font-semibold text-slate-900 truncate">
            {isReceived ? `From ${tx.counterpartyName}` : `To ${tx.counterpartyName}`}
          </p>
          <span className="text-xs text-slate-400 shrink-0">{tx.counterpartyRole}</span>
        </div>
        <p className="mt-0.5 text-xs text-slate-500 truncate">{tx.reason || '—'}</p>
        <div className="mt-1.5 flex items-center gap-2 flex-wrap">
          <ModeChip mode={tx.mode} />
          <Badge variant={badge.variant}>{badge.label}</Badge>
          <span className="text-[10px] text-slate-400">{formatDateTime(tx.createdAt)}</span>
        </div>
      </div>

      {/* Amount */}
      <div className="shrink-0 text-right">
        <p className={`text-base font-bold ${isReceived ? 'text-emerald-600' : 'text-rose-600'}`}>
          {isReceived ? '+' : '−'}{fmt(tx.amount)}
        </p>
      </div>
    </button>
  )
}

// ── Detail drawer / modal ─────────────────────────────────────────────────────

function TransactionDetail({ tx, onClose }) {
  if (!tx) return null
  const isReceived = tx.direction === 'RECEIVED'
  const badge = statusBadge(tx.status)

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/50 sm:items-center px-4"
      onClick={onClose}>
      <div
        className="w-full max-w-md rounded-t-3xl sm:rounded-3xl bg-white shadow-2xl overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Drag handle (mobile) */}
        <div className="flex justify-center pt-3 pb-1 sm:hidden">
          <div className="h-1 w-10 rounded-full bg-slate-200" />
        </div>

        {/* Header */}
        <div className={`px-6 py-6 ${isReceived ? 'bg-emerald-50' : 'bg-rose-50'}`}>
          <div className="flex items-center justify-between">
            <div className={`flex h-14 w-14 items-center justify-center rounded-full text-2xl font-bold
              ${isReceived ? 'bg-emerald-100 text-emerald-600' : 'bg-rose-100 text-rose-600'}`}>
              {isReceived ? '↓' : '↑'}
            </div>
            <button onClick={onClose}
              className="rounded-full p-2 text-slate-400 hover:bg-white/60 transition">✕</button>
          </div>
          <p className={`mt-3 text-3xl font-bold ${isReceived ? 'text-emerald-700' : 'text-rose-700'}`}>
            {isReceived ? '+' : '−'}{fmt(tx.amount)}
          </p>
          <p className="mt-1 text-sm text-slate-600">
            {isReceived ? `Received from ${tx.counterpartyName}` : `Sent to ${tx.counterpartyName}`}
          </p>
          <div className="mt-2">
            <Badge variant={badge.variant}>{badge.label}</Badge>
          </div>
        </div>

        {/* Details */}
        <div className="px-6 py-5 space-y-3">
          <DetailRow label="Transaction ID" value={tx.transactionId?.slice(0, 16) + '…'} mono />
          <DetailRow label="Date & Time" value={formatDateTime(tx.createdAt)} />
          {tx.confirmedAt && <DetailRow label="Confirmed At" value={formatDateTime(tx.confirmedAt)} />}
          <DetailRow label="Payment Mode" value={<ModeChip mode={tx.mode} />} />
          <DetailRow label="Counterparty" value={`${tx.counterpartyName} (${tx.counterpartyRole})`} />
          {tx.reason && <DetailRow label="Description" value={tx.reason} />}
        </div>

        <div className="px-6 pb-6">
          <button
            onClick={onClose}
            className="w-full rounded-2xl bg-slate-950 py-3.5 text-sm font-semibold text-white transition hover:bg-slate-800"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  )
}

function DetailRow({ label, value, mono = false }) {
  return (
    <div className="flex items-start justify-between gap-4 py-2 border-b border-slate-50 last:border-0">
      <span className="text-sm text-slate-500 shrink-0">{label}</span>
      <span className={`text-sm font-medium text-slate-900 text-right ${mono ? 'font-mono text-xs' : ''}`}>
        {value}
      </span>
    </div>
  )
}

// ── Filter bar ────────────────────────────────────────────────────────────────

const FILTERS = [
  { key: 'ALL',      label: 'All' },
  { key: 'RECEIVED', label: 'Received' },
  { key: 'SENT',     label: 'Sent' },
  { key: 'ONLINE',   label: 'Online' },
  { key: 'CASH',     label: 'Cash' },
]

// ── Main page ─────────────────────────────────────────────────────────────────

export default function PaymentHistory() {
  const [transactions, setTransactions] = useState([])
  const [loading, setLoading]           = useState(true)
  const [error, setError]               = useState('')
  const [filter, setFilter]             = useState('ALL')
  const [search, setSearch]             = useState('')
  const [selected, setSelected]         = useState(null)

  useEffect(() => { fetchHistory() }, [])

  const fetchHistory = async () => {
    try {
      setLoading(true)
      setError('')
      const res = await paymentService.getMyTransactionHistory()
      setTransactions(res.data || [])
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to load payment history.')
    } finally {
      setLoading(false)
    }
  }

  // Apply filter + search
  const filtered = useMemo(() => {
    return transactions.filter((tx) => {
      const matchFilter =
        filter === 'ALL'      ? true :
        filter === 'RECEIVED' ? tx.direction === 'RECEIVED' :
        filter === 'SENT'     ? tx.direction === 'SENT' :
        filter === 'ONLINE'   ? tx.mode === 'ONLINE' :
        filter === 'CASH'     ? tx.mode === 'CASH' : true

      const q = search.toLowerCase()
      const matchSearch = !q || (
        tx.counterpartyName?.toLowerCase().includes(q) ||
        tx.reason?.toLowerCase().includes(q) ||
        tx.mode?.toLowerCase().includes(q) ||
        tx.status?.toLowerCase().includes(q) ||
        tx.amount?.toString().includes(q)
      )

      return matchFilter && matchSearch
    })
  }, [transactions, filter, search])

  const grouped = useMemo(() => groupByDate(filtered), [filtered])

  // ── render ──────────────────────────────────────────────────────────────────
  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Transactions"
        title="Payment History"
        description="Every payment sent and received — all in one place."
        action={
          <Button label="Refresh" variant="secondary" onClick={fetchHistory} />
        }
      />

      {error && <Alert tone="error" onClose={() => setError('')}>{error}</Alert>}

      {/* Summary strip */}
      {!loading && transactions.length > 0 && (
        <SummaryStrip transactions={transactions} />
      )}

      {/* Search */}
      <div className="relative">
        <svg className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"
          fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
            d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <input
          type="text"
          placeholder="Search by name, description, amount…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="h-12 w-full rounded-2xl border border-slate-200 bg-white pl-11 pr-4 text-sm text-slate-900 shadow-sm outline-none transition placeholder:text-slate-400 focus:border-sky-400 focus:ring-4 focus:ring-sky-100"
        />
        {search && (
          <button onClick={() => setSearch('')}
            className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600">
            ✕
          </button>
        )}
      </div>

      {/* Filter tabs */}
      <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-none">
        {FILTERS.map((f) => {
          const count = f.key === 'ALL' ? transactions.length
            : f.key === 'RECEIVED' ? transactions.filter(t => t.direction === 'RECEIVED').length
            : f.key === 'SENT'     ? transactions.filter(t => t.direction === 'SENT').length
            : f.key === 'ONLINE'   ? transactions.filter(t => t.mode === 'ONLINE').length
            : transactions.filter(t => t.mode === 'CASH').length

          return (
            <button
              key={f.key}
              onClick={() => setFilter(f.key)}
              className={`flex shrink-0 items-center gap-1.5 rounded-full px-4 py-2 text-sm font-semibold transition
                ${filter === f.key
                  ? 'bg-slate-950 text-white shadow'
                  : 'bg-white border border-slate-200 text-slate-600 hover:border-slate-300 hover:text-slate-900'}`}
            >
              {f.label}
              <span className={`rounded-full px-1.5 py-0.5 text-[10px] font-bold
                ${filter === f.key ? 'bg-white/20 text-white' : 'bg-slate-100 text-slate-500'}`}>
                {count}
              </span>
            </button>
          )
        })}
      </div>

      {/* Transaction list */}
      {loading ? (
        <div className="space-y-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-20 rounded-2xl" />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <div className="rounded-3xl border border-slate-200 bg-white px-6 py-16 text-center">
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-slate-100 text-3xl">
            🧾
          </div>
          <p className="text-base font-semibold text-slate-700">No transactions found</p>
          <p className="mt-1 text-sm text-slate-400">
            {search ? 'Try a different search term.' : 'Your payment history will appear here once transactions are made.'}
          </p>
        </div>
      ) : (
        <div className="space-y-6">
          {Object.entries(grouped).map(([dateLabel, txns]) => (
            <div key={dateLabel}>
              {/* Date group header */}
              <div className="mb-3 flex items-center gap-3">
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">{dateLabel}</p>
                <div className="flex-1 border-t border-slate-100" />
                <p className="text-xs text-slate-400">{txns.length} transaction{txns.length !== 1 ? 's' : ''}</p>
              </div>

              <div className="space-y-2">
                {txns.map((tx) => (
                  <TransactionRow key={tx.transactionId} tx={tx} onClick={setSelected} />
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Detail drawer */}
      {selected && (
        <TransactionDetail tx={selected} onClose={() => setSelected(null)} />
      )}
    </div>
  )
}
