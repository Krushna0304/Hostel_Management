import { useEffect, useState, useCallback } from 'react'
import extensionService from '../../services/extensionService'
import ExtensionApprovalModal from '../../components/ExtensionApprovalModal'
import ExtensionPaymentModal from '../../components/ExtensionPaymentModal'
import {
  Alert,
  Badge,
  Button,
  Card,
  CardContent,
  CardHeader,
  EmptyState,
  PageHeader,
  Skeleton,
} from '../../components/ui'

// ── Status helpers ─────────────────────────────────────────────────────────

const STATUS_CONFIG = {
  PENDING_OWNER_APPROVAL: { label: 'Pending Approval', variant: 'warning', icon: '⏳' },
  APPROVED_PENDING_PAYMENT: { label: 'Approved', variant: 'info', icon: '✅' },
  PAYMENT_COMPLETED: { label: 'Payment Done', variant: 'success', icon: '💰' },
  AGREEMENT_CREATED: { label: 'Agreement Created', variant: 'success', icon: '📄' },
  ACTIVE: { label: 'Active', variant: 'success', icon: '🎉' },
  REJECTED: { label: 'Rejected', variant: 'danger', icon: '❌' },
  CANCELLED: { label: 'Cancelled', variant: 'neutral', icon: '🚫' },
  EXPIRED: { label: 'Expired', variant: 'neutral', icon: '⏰' },
}

function StatusBadge({ status }) {
  const config = STATUS_CONFIG[status] || { label: status, variant: 'neutral', icon: '•' }
  return (
    <Badge variant={config.variant}>
      {config.icon} {config.label}
    </Badge>
  )
}

function formatDate(d) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })
}

// ── Extension Request Card ─────────────────────────────────────────────────

function ExtensionRequestCard({ request, onApprove, onProcessPayment }) {
  const isPending = request.status === 'PENDING_OWNER_APPROVAL'
  const isApproved = request.status === 'APPROVED_PENDING_PAYMENT'

  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4 space-y-3">
      {/* Header */}
      <div className="flex items-start justify-between gap-3 flex-wrap">
        <div className="min-w-0 flex-1">
          <p className="text-xs font-semibold uppercase tracking-[0.15em] text-slate-400">
            Extension Request
          </p>
          <p className="mt-0.5 text-sm font-semibold text-slate-900">
            {request.tenantName || `Request #${String(request.requestId || '').slice(0, 8)}`}
          </p>
          <p className="text-xs text-slate-500">
            Room: {request.roomNumber || 'N/A'}
          </p>
        </div>
        <StatusBadge status={request.status} />
      </div>

      {/* Details */}
      <div className="grid grid-cols-2 gap-2 text-xs text-slate-600">
        <div>
          <span className="text-slate-400 font-medium block">Tenant</span>
          {request.tenantName || 'N/A'}
        </div>
        <div>
          <span className="text-slate-400 font-medium block">Requested</span>
          {formatDate(request.createdAt)}
        </div>
        {request.extensionPeriod && (
          <div className="col-span-2">
            <span className="text-slate-400 font-medium block">Extension Period</span>
            {request.extensionPeriod}
          </div>
        )}
        {request.totalAmount != null && (
          <div>
            <span className="text-slate-400 font-medium block">Amount</span>
            <span className="font-semibold text-slate-900">
              ₹{Number(request.totalAmount).toLocaleString()}
            </span>
          </div>
        )}
        {request.expiresAt && isPending && (
          <div>
            <span className="text-slate-400 font-medium block">Expires</span>
            {formatDate(request.expiresAt)}
          </div>
        )}
      </div>

      {/* Tenant notes */}
      {request.tenantNotes && (
        <div className="rounded-xl bg-amber-50 border border-amber-100 px-3 py-2 text-xs text-amber-700">
          <span className="font-semibold">Tenant note: </span>
          {request.tenantNotes}
        </div>
      )}

      {/* Actions */}
      {isPending && onApprove && (
        <Button
          label="Review & Approve"
          fullWidth
          onClick={() => onApprove(request)}
        />
      )}

      {isApproved && onProcessPayment && (
        <div className="rounded-xl bg-green-50 border border-green-200 px-3 py-2">
          <p className="text-xs text-green-700">
            ✅ Approved. Waiting for tenant to complete payment of{' '}
            <strong>₹{Number(request.totalAmount || 0).toLocaleString()}</strong>.
          </p>
          {request.expiresAt && (
            <p className="text-xs text-green-600 mt-1">
              Payment deadline: {new Date(request.expiresAt).toLocaleString('en-IN')}
            </p>
          )}
        </div>
      )}
    </div>
  )
}

// ── Filter Tabs ────────────────────────────────────────────────────────────

const FILTER_TABS = [
  { key: 'pending', label: 'Pending Approval', filter: (r) => r.status === 'PENDING_OWNER_APPROVAL' },
  { key: 'approved', label: 'Approved', filter: (r) => r.status === 'APPROVED_PENDING_PAYMENT' },
  { key: 'active', label: 'Active / Done', filter: (r) => ['ACTIVE', 'PAYMENT_COMPLETED', 'AGREEMENT_CREATED'].includes(r.status) },
  { key: 'all', label: 'All', filter: () => true },
]

// ── Main Page ──────────────────────────────────────────────────────────────

export default function ExtensionRequestsPage() {
  const [requests, setRequests] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [activeTab, setActiveTab] = useState('pending')
  const [selectedRequest, setSelectedRequest] = useState(null)
  const [showApprovalModal, setShowApprovalModal] = useState(false)
  const [showPaymentModal, setShowPaymentModal] = useState(false)

  const fetchRequests = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const data = await extensionService.getOwnerExtensionRequests()
      const list = Array.isArray(data)
        ? data
        : data?.extensionRequests || data?.data || []
      // Sort: pending first, then by date
      list.sort((a, b) => {
        const priority = { PENDING_OWNER_APPROVAL: 0, APPROVED_PENDING_PAYMENT: 1 }
        const pa = priority[a.status] ?? 9
        const pb = priority[b.status] ?? 9
        if (pa !== pb) return pa - pb
        return new Date(b.createdAt || 0) - new Date(a.createdAt || 0)
      })
      setRequests(list)
    } catch (err) {
      console.error('Failed to load extension requests:', err)
      setError(
        err?.response?.data?.message ||
          'Failed to load extension requests. Please try again.'
      )
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchRequests()
  }, [fetchRequests])

  const handleApprove = (request) => {
    setSelectedRequest(request)
    setShowApprovalModal(true)
  }

  const handleProcessPayment = (request) => {
    setSelectedRequest(request)
    setShowPaymentModal(true)
  }

  const handleModalSuccess = () => {
    setShowApprovalModal(false)
    setShowPaymentModal(false)
    setSelectedRequest(null)
    fetchRequests()
  }

  // Filter requests based on active tab
  const tabConfig = FILTER_TABS.find((t) => t.key === activeTab) || FILTER_TABS[0]
  const filteredRequests = requests.filter(tabConfig.filter)

  // Count for badges
  const pendingCount = requests.filter((r) => r.status === 'PENDING_OWNER_APPROVAL').length
  const approvedCount = requests.filter((r) => r.status === 'APPROVED_PENDING_PAYMENT').length

  return (
    <div className="space-y-5">
      <PageHeader
        title="Extension Requests"
        description="Review and manage allotment extension requests from tenants."
        secondaryAction={
          <Button label="Refresh" variant="secondary" onClick={fetchRequests} />
        }
      />

      {/* Error alert */}
      {error && (
        <Alert tone="error">
          <div className="flex items-center justify-between gap-3 flex-wrap">
            <span>{error}</span>
            <Button label="Retry" variant="secondary" onClick={fetchRequests} />
          </div>
        </Alert>
      )}

      {/* Stats summary */}
      {!loading && !error && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <div className="rounded-2xl bg-amber-50 border border-amber-200 px-4 py-3">
            <p className="text-xs uppercase tracking-[0.15em] text-amber-600">Pending</p>
            <p className="mt-1 text-2xl font-bold text-amber-900">{pendingCount}</p>
          </div>
          <div className="rounded-2xl bg-sky-50 border border-sky-200 px-4 py-3">
            <p className="text-xs uppercase tracking-[0.15em] text-sky-600">Awaiting Payment</p>
            <p className="mt-1 text-2xl font-bold text-sky-900">{approvedCount}</p>
          </div>
          <div className="rounded-2xl bg-green-50 border border-green-200 px-4 py-3">
            <p className="text-xs uppercase tracking-[0.15em] text-green-600">Active</p>
            <p className="mt-1 text-2xl font-bold text-green-900">
              {requests.filter((r) => r.status === 'ACTIVE').length}
            </p>
          </div>
          <div className="rounded-2xl bg-slate-50 border border-slate-200 px-4 py-3">
            <p className="text-xs uppercase tracking-[0.15em] text-slate-500">Total</p>
            <p className="mt-1 text-2xl font-bold text-slate-900">{requests.length}</p>
          </div>
        </div>
      )}

      {/* Filter tabs */}
      <div className="flex gap-2 overflow-x-auto pb-1">
        {FILTER_TABS.map((tab) => {
          const count =
            tab.key === 'pending' ? pendingCount
            : tab.key === 'approved' ? approvedCount
            : null
          return (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={[
                'flex items-center gap-1.5 whitespace-nowrap rounded-xl px-4 py-2 text-sm font-medium transition',
                activeTab === tab.key
                  ? 'bg-slate-950 text-white'
                  : 'bg-slate-100 text-slate-700 hover:bg-slate-200',
              ].join(' ')}
            >
              {tab.label}
              {count != null && count > 0 && (
                <span
                  className={[
                    'rounded-full px-1.5 py-0.5 text-xs font-bold',
                    activeTab === tab.key
                      ? 'bg-white/20 text-white'
                      : 'bg-amber-200 text-amber-900',
                  ].join(' ')}
                >
                  {count}
                </span>
              )}
            </button>
          )
        })}
      </div>

      {/* Request list */}
      <Card>
        <CardHeader
          title={tabConfig.label}
          description={
            loading
              ? 'Loading…'
              : `${filteredRequests.length} request${filteredRequests.length !== 1 ? 's' : ''}`
          }
        />
        <CardContent className="pt-0">
          {loading ? (
            <div className="space-y-3">
              {[1, 2, 3].map((i) => (
                <Skeleton key={i} className="h-32 rounded-2xl" />
              ))}
            </div>
          ) : filteredRequests.length === 0 ? (
            <EmptyState
              title={`No ${tabConfig.label.toLowerCase()} requests`}
              description={
                activeTab === 'pending'
                  ? 'No pending extension requests from tenants.'
                  : 'No extension requests found in this category.'
              }
            />
          ) : (
            <div className="space-y-3">
              {filteredRequests.map((request) => (
                <ExtensionRequestCard
                  key={request.requestId}
                  request={request}
                  onApprove={handleApprove}
                  onProcessPayment={handleProcessPayment}
                />
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Approval modal */}
      {showApprovalModal && selectedRequest && (
        <ExtensionApprovalModal
          request={selectedRequest}
          onClose={() => {
            setShowApprovalModal(false)
            setSelectedRequest(null)
          }}
          onSuccess={handleModalSuccess}
        />
      )}

      {/* Payment modal (for already-approved requests) */}
      {showPaymentModal && selectedRequest && (
        <ExtensionPaymentModal
          extensionRequest={selectedRequest}
          onClose={() => {
            setShowPaymentModal(false)
            setSelectedRequest(null)
          }}
          onSuccess={handleModalSuccess}
        />
      )}
    </div>
  )
}
