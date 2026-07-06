import { useEffect, useState } from 'react'
import { Alert, Badge, Skeleton } from './ui'
import electricityBillService from '../services/electricityBillService'

const fmt = (a) => `₹${Number(a ?? 0).toLocaleString('en-IN')}`

function formatDateTime(d) {
  if (!d) return '—'
  return new Date(d).toLocaleString('en-IN', {
    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

const modeIcon = (mode) => (mode === 'ONLINE' ? '💳' : mode === 'CASH' ? '💵' : '—')

/**
 * Owner-side electricity payment history for a single tenant.
 *
 * Props:
 *   tenantName — string shown in header
 *   tenantId   — used to fetch the tenant's electricity shares
 *   onClose    — fn()
 */
export default function ElectricityCollectionHistoryModal({ tenantName, tenantId, onClose }) {
  const [shares, setShares] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    ;(async () => {
      try {
        setLoading(true)
        const data = await electricityBillService.getTenantElectricityHistory(tenantId)
        if (active) setShares(data)
      } catch (err) {
        if (active) setError(err?.response?.data?.message || 'Failed to load history.')
      } finally {
        if (active) setLoading(false)
      }
    })()
    return () => { active = false }
  }, [tenantId])

  const paid = shares.filter((s) => s.status === 'COMPLETED')
  const totalPaid = paid.reduce((sum, s) => sum + Number(s.amount), 0)
  const totalPending = shares.filter((s) => s.status !== 'COMPLETED')
    .reduce((sum, s) => sum + Number(s.amount), 0)

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4" onClick={onClose}>
      <div
        className="relative flex w-full max-w-lg flex-col rounded-3xl bg-slate-50 shadow-2xl"
        style={{ maxHeight: '92vh' }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between rounded-t-3xl bg-white px-6 py-4 shadow-sm">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-600">Electricity History</p>
            <h2 className="mt-0.5 text-lg font-bold text-slate-950">{tenantName || 'Tenant'}</h2>
          </div>
          <button
            onClick={onClose}
            className="rounded-full p-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
            aria-label="Close"
          >
            ✕
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 space-y-4 overflow-y-auto px-5 py-5">
          {error && <Alert tone="error">{error}</Alert>}

          {loading ? (
            <div className="space-y-3">
              <Skeleton className="h-24 rounded-2xl" />
              {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-16 rounded-2xl" />)}
            </div>
          ) : (
            <>
              {/* Summary */}
              <div className="rounded-2xl bg-slate-950 p-5 text-white">
                <div className="flex items-end justify-between">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-300">Total paid</p>
                    <p className="mt-1 text-3xl font-bold">{fmt(totalPaid)}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-xs text-slate-400">Outstanding</p>
                    <p className="text-lg font-semibold text-slate-200">{fmt(totalPending)}</p>
                  </div>
                </div>
              </div>

              {/* Transactions */}
              {shares.length === 0 ? (
                <div className="rounded-2xl border border-slate-200 bg-white px-6 py-10 text-center">
                  <p className="text-sm text-slate-500">No electricity bills for this tenant.</p>
                </div>
              ) : (
                <div className="space-y-2">
                  {shares.map((s) => {
                    const isPaid = s.status === 'COMPLETED'
                    return (
                      <div key={s.paymentId} className="flex items-center gap-4 rounded-2xl border border-slate-100 bg-white px-4 py-3.5 shadow-sm">
                        <div className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-full text-lg font-bold ${
                          isPaid ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'
                        }`}>
                          {isPaid ? '✓' : '○'}
                        </div>
                        <div className="min-w-0 flex-1">
                          <div className="flex items-center gap-2">
                            <p className="text-sm font-semibold text-slate-900">{s.billPeriod || 'Electricity'}</p>
                            <Badge variant={isPaid ? 'success' : 'warning'}>{isPaid ? 'Paid' : 'Pending'}</Badge>
                            {s.paymentMode && (
                              <span className="text-xs text-slate-400">{modeIcon(s.paymentMode)} {s.paymentMode}</span>
                            )}
                          </div>
                          <p className="mt-0.5 text-xs text-slate-500">
                            {s.roomNumber ? `Room ${s.roomNumber}` : ''}
                            {isPaid ? ` • Paid on ${formatDateTime(s.paidAt)}` : ''}
                          </p>
                        </div>
                        <div className="text-right">
                          <p className={`text-base font-bold ${isPaid ? 'text-emerald-700' : 'text-slate-900'}`}>
                            {fmt(s.amount)}
                          </p>
                        </div>
                      </div>
                    )
                  })}
                </div>
              )}
            </>
          )}
        </div>

        {/* Footer */}
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
