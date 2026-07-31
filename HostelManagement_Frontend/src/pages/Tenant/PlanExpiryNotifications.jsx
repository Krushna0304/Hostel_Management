import { useEffect, useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import planExpiryNotificationService from '../../services/planExpiryNotificationService'
import { profileService } from '../../services/profileService'
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

// ── Constants ──────────────────────────────────────────────────────────────────

const TYPE_META = {
  PLAN_EXPIRY_REMINDER:   { label: 'Expiry Reminder',     color: 'bg-blue-100 text-blue-800 border-blue-200',       icon: '📅' },
  SETTLEMENT_WINDOW_OPEN: { label: 'Settlement Window',   color: 'bg-green-100 text-green-800 border-green-200',    icon: '🟢' },
  URGENT_ACTION_REQUIRED: { label: 'Urgent Action',       color: 'bg-orange-100 text-orange-800 border-orange-200', icon: '⚠️' },
  FINAL_NOTICE:           { label: 'Final Notice',         color: 'bg-red-100 text-red-800 border-red-200',          icon: '🚨' },
}

const STATUS_VARIANT = {
  PENDING:   'warning',
  SENT:      'neutral',
  DELIVERED: 'success',
  FAILED:    'danger',
  CANCELLED: 'neutral',
}

function getTypeMeta(type) {
  return TYPE_META[type] || { label: type, color: 'bg-slate-100 text-slate-700 border-slate-200', icon: '🔔' }
}

function urgencyLabel(leadTimeDays) {
  if (leadTimeDays == null) return null
  if (leadTimeDays <= 1)  return <span className="text-xs font-bold text-red-600">Expires tomorrow</span>
  if (leadTimeDays <= 3)  return <span className="text-xs font-bold text-orange-600">{leadTimeDays} days left</span>
  if (leadTimeDays <= 7)  return <span className="text-xs font-semibold text-yellow-600">{leadTimeDays} days left</span>
  return <span className="text-xs text-slate-500">{leadTimeDays} days left</span>
}

function fmtDate(d) {
  return d ? new Date(d).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '—'
}

// ── Notification card ─────────────────────────────────────────────────────────

function NotificationCard({ notification, onSettle, onExtend }) {
  const meta = getTypeMeta(notification.notificationType)
  const isUrgent = ['URGENT_ACTION_REQUIRED', 'FINAL_NOTICE'].includes(notification.notificationType)

  return (
    <div className={`rounded-2xl border p-4 transition-all ${isUrgent ? 'border-red-200 bg-red-50/40' : 'border-slate-200 bg-white'}`}>
      {/* Header row */}
      <div className="flex items-start justify-between gap-3 flex-wrap">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="text-lg leading-none">{meta.icon}</span>
          <span className={`px-2 py-1 text-xs font-semibold rounded-full border ${meta.color}`}>
            {meta.label}
          </span>
          <Badge variant={STATUS_VARIANT[notification.deliveryStatus] || 'neutral'}>
            {notification.deliveryStatus}
          </Badge>
        </div>
        {urgencyLabel(notification.leadTimeDays)}
      </div>

      {/* Details */}
      <div className="mt-3 grid grid-cols-2 gap-x-6 gap-y-1.5 text-sm">
        <div>
          <p className="text-xs uppercase tracking-[0.15em] text-slate-400">Scheduled</p>
          <p className="font-medium text-slate-900">{fmtDate(notification.scheduledDate)}</p>
        </div>
        {notification.sentAt && (
          <div>
            <p className="text-xs uppercase tracking-[0.15em] text-slate-400">Sent</p>
            <p className="font-medium text-slate-900">{fmtDate(notification.sentAt)}</p>
          </div>
        )}
        {notification.agreementId && (
          <div className="col-span-2">
            <p className="text-xs uppercase tracking-[0.15em] text-slate-400">Agreement ID</p>
            <p className="font-medium text-slate-700 text-xs truncate">{notification.agreementId}</p>
          </div>
        )}
      </div>

      {/* Message preview */}
      {notification.message && (
        <details className="mt-3">
          <summary className="cursor-pointer text-xs text-sky-600 hover:underline select-none">
            View message
          </summary>
          <pre className="mt-2 whitespace-pre-wrap text-xs text-slate-700 bg-slate-50 border border-slate-200 rounded-lg p-3 overflow-auto max-h-40">
            {notification.message}
          </pre>
        </details>
      )}

      {/* 6.4.3 Action buttons for urgent notifications */}
      {isUrgent && (
        <div className="mt-4 flex gap-2 flex-wrap">
          <Button label="Request Settlement" size="sm" variant="danger"    onClick={onSettle} />
          <Button label="Request Extension"  size="sm" variant="secondary" onClick={onExtend} />
        </div>
      )}
    </div>
  )
}

// ── Filter bar ────────────────────────────────────────────────────────────────

const STATUSES = ['ALL', 'PENDING', 'SENT', 'DELIVERED', 'FAILED', 'CANCELLED']
const TYPES    = ['ALL', ...Object.keys(TYPE_META)]

function FilterBar({ filter, onChange }) {
  return (
    <div className="flex flex-wrap gap-3">
      <div className="flex items-center gap-2">
        <label className="text-xs text-slate-500 font-medium whitespace-nowrap">Status</label>
        <select
          value={filter.status}
          onChange={(e) => onChange({ ...filter, status: e.target.value })}
          className="text-sm border border-slate-200 rounded-lg px-2 py-1.5 bg-white focus:outline-none focus:ring-2 focus:ring-sky-300"
        >
          {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
      </div>
      <div className="flex items-center gap-2">
        <label className="text-xs text-slate-500 font-medium whitespace-nowrap">Type</label>
        <select
          value={filter.type}
          onChange={(e) => onChange({ ...filter, type: e.target.value })}
          className="text-sm border border-slate-200 rounded-lg px-2 py-1.5 bg-white focus:outline-none focus:ring-2 focus:ring-sky-300"
        >
          {TYPES.map((t) => (
            <option key={t} value={t}>{t === 'ALL' ? 'All Types' : (TYPE_META[t]?.label ?? t)}</option>
          ))}
        </select>
      </div>
    </div>
  )
}

// ── Main page ─────────────────────────────────────────────────────────────────

export default function TenantPlanExpiryNotifications() {
  const navigate = useNavigate()

  const [tenantId, setTenantId]         = useState(null)
  const [notifications, setNotifications] = useState([])
  const [loading, setLoading]           = useState(true)
  const [error, setError]               = useState('')
  const [filter, setFilter]             = useState({ status: 'ALL', type: 'ALL' })

  // Step 1: resolve tenantId from profile (username → userId)
  useEffect(() => {
    profileService.getCurrentProfile()
      .then((profile) => setTenantId(profile?.userId))
      .catch(() => setError('Unable to load your profile. Please log in again.'))
  }, [])

  // Step 2: fetch notifications once tenantId is known or filter changes
  const fetchNotifications = useCallback(async () => {
    if (!tenantId) return
    setLoading(true)
    setError('')
    try {
      const params = {}
      if (filter.status !== 'ALL') params.status = filter.status
      if (filter.type   !== 'ALL') params.notificationType = filter.type
      const res = await planExpiryNotificationService.getTenantNotifications(tenantId, params)
      setNotifications(res.data?.notifications || [])
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to load notifications.')
    } finally {
      setLoading(false)
    }
  }, [tenantId, filter])

  useEffect(() => {
    if (tenantId) fetchNotifications()
  }, [fetchNotifications, tenantId])

  const urgentCount = notifications.filter(
    (n) => ['URGENT_ACTION_REQUIRED', 'FINAL_NOTICE'].includes(n.notificationType) &&
            n.deliveryStatus !== 'CANCELLED'
  ).length

  return (
    <div className="space-y-5">
      <PageHeader
        title="Plan Expiry Notifications"
        description="Stay informed about your agreement expiry — act before it's too late."
        secondaryAction={<Button label="Refresh" variant="secondary" onClick={fetchNotifications} />}
      />

      {/* 6.4.3 Urgent alert banner with action buttons */}
      {urgentCount > 0 && (
        <div className="flex items-start gap-3 rounded-2xl border-l-4 border-red-500 bg-red-50 px-4 py-4">
          <span className="text-xl flex-shrink-0 mt-0.5">🚨</span>
          <div className="flex-1 min-w-0">
            <p className="font-semibold text-red-800">
              {urgentCount} urgent notification{urgentCount > 1 ? 's' : ''} require your attention
            </p>
            <p className="text-sm text-red-700 mt-0.5">
              Your agreement is expiring soon. Please request a settlement or extension immediately.
            </p>
          </div>
          <div className="flex gap-2 flex-wrap flex-shrink-0">
            <Button
              label="Settle Now"
              size="sm"
              variant="danger"
              onClick={() => navigate('/tenant-portal/settlements')}
            />
            <Button
              label="Extend Stay"
              size="sm"
              variant="secondary"
              onClick={() => navigate('/tenant-portal/dashboard')}
            />
          </div>
        </div>
      )}

      {/* Filters */}
      <Card>
        <CardContent>
          <FilterBar filter={filter} onChange={setFilter} />
        </CardContent>
      </Card>

      {/* Notification list */}
      <Card>
        <CardHeader
          title="Notifications"
          description={loading ? 'Loading…' : `${notifications.length} notification${notifications.length !== 1 ? 's' : ''}`}
        />
        <CardContent className="pt-0">
          {loading ? (
            <div className="space-y-3">
              {[1, 2, 3].map((i) => <Skeleton key={i} className="h-24 rounded-2xl" />)}
            </div>
          ) : error ? (
            <Alert tone="error">{error}</Alert>
          ) : notifications.length === 0 ? (
            <EmptyState
              title="No notifications"
              description="You have no plan expiry notifications matching the current filters."
            />
          ) : (
            <div className="space-y-3">
              {notifications.map((n) => (
                <NotificationCard
                  key={n.notificationId}
                  notification={n}
                  onSettle={() => navigate('/tenant-portal/settlements')}
                  onExtend={() => navigate('/tenant-portal/dashboard')}
                />
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
