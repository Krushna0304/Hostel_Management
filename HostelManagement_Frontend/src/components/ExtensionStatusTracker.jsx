import { useEffect, useState } from 'react'
import { Badge } from './ui/Badge'
import { Button } from './ui/Button'
import { Card, CardContent, CardHeader } from './ui/Card'
import Skeleton from './ui/Skeleton'
import EmptyState from './ui/EmptyState'
import extensionService from '../services/extensionService'

// Status flow: PENDING → APPROVED → PAYMENT_DONE (happy path)
//              PENDING → REJECTED (terminal rejection)
const STATUS_STEPS = ['PENDING', 'APPROVED', 'PAYMENT_DONE']

const STATUS_CONFIG = {
  PENDING: {
    label: 'Pending',
    variant: 'warning',
    description: 'Waiting for owner approval',
    icon: '⏳',
  },
  APPROVED: {
    label: 'Approved',
    variant: 'info',
    description: 'Approved — payment required',
    icon: '✅',
  },
  PAYMENT_PENDING: {
    label: 'Payment Pending',
    variant: 'info',
    description: 'Approved — payment required',
    icon: '💳',
  },
  PAYMENT_DONE: {
    label: 'Active',
    variant: 'success',
    description: 'Extension is active',
    icon: '🎉',
  },
  REJECTED: {
    label: 'Rejected',
    variant: 'danger',
    description: 'Request was declined by owner',
    icon: '❌',
  },
}

/** Map any backend status onto the 3-step timeline (PENDING → APPROVED → PAYMENT_DONE) */
function getStepIndex(status) {
  if (!status) return 0
  if (status === 'PAYMENT_DONE') return 2
  if (status === 'APPROVED' || status === 'PAYMENT_PENDING') return 1
  return 0 // PENDING or unknown
}

function StatusBadge({ status }) {
  const config = STATUS_CONFIG[status] || { label: status, variant: 'neutral', icon: '•' }
  return (
    <Badge variant={config.variant}>
      {config.icon} {config.label}
    </Badge>
  )
}

function StatusTimeline({ status }) {
  if (status === 'REJECTED') {
    return (
      <div className="flex items-center gap-2 mt-2">
        <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-rose-50 border border-rose-200">
          <span className="text-xs text-rose-600 font-medium">❌ Request Rejected</span>
        </div>
      </div>
    )
  }

  const currentStep = getStepIndex(status)

  return (
    <div className="flex items-center gap-0 mt-3">
      {STATUS_STEPS.map((step, idx) => {
        const completed = idx < currentStep
        const active = idx === currentStep
        const config = STATUS_CONFIG[step] || {}

        return (
          <div key={step} className="flex items-center">
            {/* Step circle */}
            <div className="flex flex-col items-center">
              <div
                className={[
                  'w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold border-2 transition-all',
                  completed
                    ? 'bg-emerald-500 border-emerald-500 text-white'
                    : active
                    ? 'bg-sky-500 border-sky-500 text-white'
                    : 'bg-white border-slate-200 text-slate-400',
                ].join(' ')}
              >
                {completed ? '✓' : idx + 1}
              </div>
              <span
                className={[
                  'mt-1 text-[10px] font-medium whitespace-nowrap',
                  completed ? 'text-emerald-600' : active ? 'text-sky-600' : 'text-slate-400',
                ].join(' ')}
              >
                {config.label || step}
              </span>
            </div>

            {/* Connector line between steps */}
            {idx < STATUS_STEPS.length - 1 && (
              <div
                className={[
                  'h-0.5 w-8 sm:w-12 mx-1 mb-4 transition-all',
                  idx < currentStep ? 'bg-emerald-400' : 'bg-slate-200',
                ].join(' ')}
              />
            )}
          </div>
        )
      })}
    </div>
  )
}

function ExtensionRequestCard({ request, onPayNow }) {
  const formatDate = (d) => (d ? new Date(d).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) : '—')

  const needsPayment = request.status === 'APPROVED' || request.status === 'PAYMENT_PENDING'

  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 space-y-3">
      {/* Header row */}
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-xs font-semibold uppercase tracking-[0.15em] text-slate-400">
            Extension Request
          </p>
          <p className="mt-0.5 text-sm font-semibold text-slate-900 truncate">
            {request.planName || `Request #${String(request.requestId).slice(0, 8)}`}
          </p>
          <p className="text-xs text-slate-500 mt-0.5">
            ID: {String(request.requestId).slice(0, 8)}…
          </p>
        </div>
        <div className="flex-shrink-0">
          <StatusBadge status={request.status} />
        </div>
      </div>

      {/* Meta row */}
      <div className="grid grid-cols-2 gap-2 text-xs text-slate-600">
        <div>
          <span className="text-slate-400 font-medium block">Requested</span>
          {formatDate(request.requestedAt || request.createdAt)}
        </div>
        {request.roomNumber && (
          <div>
            <span className="text-slate-400 font-medium block">Room</span>
            {request.roomNumber}
          </div>
        )}
        {request.extensionPeriod && (
          <div>
            <span className="text-slate-400 font-medium block">Period</span>
            {request.extensionPeriod}
          </div>
        )}
        {request.totalAmount != null && (
          <div>
            <span className="text-slate-400 font-medium block">Amount</span>
            <span className={needsPayment ? 'font-semibold text-sky-700' : ''}>
              ₹{Number(request.totalAmount).toLocaleString()}
            </span>
          </div>
        )}
      </div>

      {/* Payment deadline warning */}
      {needsPayment && request.expiresAt && (
        <div className="rounded-xl bg-red-50 border border-red-100 px-3 py-2 text-xs text-red-700">
          ⚠️ Payment deadline: {new Date(request.expiresAt).toLocaleString('en-IN')}
        </div>
      )}

      {/* Owner notes */}
      {request.ownerNotes && (
        <div className="rounded-xl bg-slate-50 px-3 py-2 text-xs text-slate-600">
          <span className="font-medium text-slate-700">Owner note: </span>
          {request.ownerNotes}
        </div>
      )}

      {/* Status timeline */}
      <StatusTimeline status={request.status} />

      {/* Pay Now CTA */}
      {needsPayment && onPayNow && (
        <div className="pt-1">
          <Button
            label="Pay Now"
            fullWidth
            onClick={() => onPayNow(request)}
          />
        </div>
      )}
    </div>
  )
}

/**
 * ExtensionStatusTracker
 *
 * Props:
 *   - tenantId         (optional) – if provided, fetches by tenantId (admin/owner view)
 *   - requests         (optional) – pre-fetched list; skips internal fetch when supplied
 *   - onPayNow         (fn)       – called with the request when "Pay Now" is clicked
 *   - title            (string)   – section heading, defaults to "Extension Requests"
 *   - className        (string)
 */
export default function ExtensionStatusTracker({
  tenantId,
  requests: propRequests,
  onPayNow,
  title = 'Extension Requests',
  className = '',
}) {
  const [requests, setRequests] = useState(propRequests || [])
  const [loading, setLoading] = useState(!propRequests)
  const [error, setError] = useState('')

  useEffect(() => {
    // If caller supplies requests directly, use them as-is
    if (propRequests) {
      setRequests(propRequests)
      setLoading(false)
      return
    }
    loadRequests()
  }, [tenantId, propRequests])

  const loadRequests = async () => {
    try {
      setLoading(true)
      setError('')
      const data = tenantId
        ? await extensionService.getTenantExtensionRequestsById(tenantId)
        : await extensionService.getTenantExtensionRequests()

      const list = Array.isArray(data) ? data : data?.extensionRequests || data?.data || []
      // Sort: most recent first
      list.sort((a, b) => {
        const dateA = new Date(a.requestedAt || a.createdAt || 0)
        const dateB = new Date(b.requestedAt || b.createdAt || 0)
        return dateB - dateA
      })
      setRequests(list)
    } catch (err) {
      console.error('ExtensionStatusTracker: failed to load requests', err)
      setError(err?.response?.data?.message || 'Failed to load extension requests.')
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className={`space-y-3 ${className}`}>
        <Skeleton className="h-6 w-40 rounded-xl" />
        <Skeleton className="h-36 rounded-2xl" />
        <Skeleton className="h-36 rounded-2xl" />
      </div>
    )
  }

  if (error) {
    return (
      <div className={`rounded-2xl border border-rose-200 bg-rose-50 p-4 ${className}`}>
        <p className="text-sm text-rose-700">{error}</p>
        <button
          onClick={loadRequests}
          className="mt-2 text-xs text-rose-600 underline hover:no-underline"
        >
          Retry
        </button>
      </div>
    )
  }

  if (requests.length === 0) {
    return (
      <div className={className}>
        <EmptyState
          title="No extension requests"
          description="You haven't submitted any extension requests yet."
        />
      </div>
    )
  }

  return (
    <div className={`space-y-4 ${className}`}>
      <div className="flex items-center justify-between">
        <h3 className="text-base font-semibold text-slate-900">{title}</h3>
        <Badge variant="neutral">{requests.length}</Badge>
      </div>
      <div className="space-y-3">
        {requests.map((req) => (
          <ExtensionRequestCard
            key={req.requestId}
            request={req}
            onPayNow={onPayNow}
          />
        ))}
      </div>
    </div>
  )
}
