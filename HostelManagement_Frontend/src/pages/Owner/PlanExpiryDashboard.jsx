import { useEffect, useState, useCallback } from 'react'
import planExpiryNotificationService from '../../services/planExpiryNotificationService'
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

// ── Stat card ─────────────────────────────────────────────────────────────────

function StatBadge({ label, value, color }) {
  return (
    <div className={`rounded-2xl px-4 py-3 ${color}`}>
      <p className="text-xs uppercase tracking-[0.15em] opacity-70">{label}</p>
      <p className="mt-1 text-2xl font-bold">{value ?? '—'}</p>
    </div>
  )
}

// ── Type badge ────────────────────────────────────────────────────────────────

const TYPE_COLORS = {
  PLAN_EXPIRY_REMINDER:   'bg-blue-100 text-blue-800 border-blue-200',
  SETTLEMENT_WINDOW_OPEN: 'bg-green-100 text-green-800 border-green-200',
  URGENT_ACTION_REQUIRED: 'bg-orange-100 text-orange-800 border-orange-200',
  FINAL_NOTICE:           'bg-red-100 text-red-800 border-red-200',
}

function TypeChip({ type }) {
  return (
    <span className={`px-2 py-0.5 text-xs font-semibold rounded-full border ${TYPE_COLORS[type] || 'bg-slate-100 text-slate-700 border-slate-200'}`}>
      {type?.replaceAll('_', ' ')}
    </span>
  )
}

// ── Main component ────────────────────────────────────────────────────────────

export default function PlanExpiryDashboard() {
  const [stats, setStats] = useState(null)
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [processing, setProcessing] = useState(false)
  const [processMsg, setProcessMsg] = useState('')

  const fetchData = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [statsRes, summaryRes] = await Promise.all([
        planExpiryNotificationService.getDeliveryStatistics(),
        planExpiryNotificationService.getPendingNotificationsSummary(),
      ])
      setStats(statsRes.data?.statistics || {})
      setSummary(summaryRes.data?.summary || {})
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to load dashboard data.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { fetchData() }, [fetchData])

  const handleTriggerProcessing = async () => {
    setProcessing(true)
    setProcessMsg('')
    try {
      await planExpiryNotificationService.triggerProcessing()
      setProcessMsg('✅ Notification processing triggered successfully.')
      fetchData()
    } catch (err) {
      setProcessMsg('❌ Failed to trigger processing: ' + (err?.response?.data?.message || err.message))
    } finally {
      setProcessing(false)
    }
  }

  const deliveryCounts = stats?.deliveryStatusCounts || {}
  const typeCounts = stats?.notificationTypeCounts || {}
  const successRate = typeof stats?.deliverySuccessRate === 'number'
    ? `${(stats.deliverySuccessRate * 100).toFixed(1)}%`
    : '—'
  const todayPending = stats?.todaysPendingCount ?? '—'

  return (
    <div className="space-y-5">
      <PageHeader
        title="Plan Expiry Dashboard"
        description="Monitor and manage plan expiry notifications across all tenants."
        action={
          <Button
            label={processing ? 'Processing…' : 'Trigger Processing'}
            onClick={handleTriggerProcessing}
            disabled={processing}
          />
        }
        secondaryAction={
          <Button label="Refresh" variant="secondary" onClick={fetchData} />
        }
      />

      {processMsg && (
        <Alert tone={processMsg.startsWith('✅') ? 'success' : 'error'}>
          {processMsg}
        </Alert>
      )}

      {loading ? (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {[1, 2, 3, 4].map((i) => <Skeleton key={i} className="h-20 rounded-2xl" />)}
        </div>
      ) : error ? (
        <Alert tone="error">{error}</Alert>
      ) : (
        <>
          {/* Delivery stats */}
          <Card>
            <CardHeader title="Delivery Overview" description="Notification delivery status counts" />
            <CardContent className="pt-0">
              <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                <StatBadge label="Today Pending"  value={todayPending}                     color="bg-yellow-50 text-yellow-900" />
                <StatBadge label="Delivered"       value={deliveryCounts.DELIVERED ?? 0}   color="bg-green-50 text-green-900" />
                <StatBadge label="Failed"          value={deliveryCounts.FAILED ?? 0}      color="bg-red-50 text-red-900" />
                <StatBadge label="Success Rate"    value={successRate}                      color="bg-sky-50 text-sky-900" />
              </div>
            </CardContent>
          </Card>

          {/* Status breakdown */}
          <div className="grid gap-4 md:grid-cols-2">
            {/* Delivery status breakdown */}
            <Card>
              <CardHeader title="Status Breakdown" />
              <CardContent className="pt-0">
                {Object.keys(deliveryCounts).length === 0 ? (
                  <EmptyState title="No data" description="No notification statistics available yet." />
                ) : (
                  <div className="space-y-2">
                    {Object.entries(deliveryCounts).map(([status, count]) => (
                      <div key={status} className="flex items-center justify-between rounded-xl bg-slate-50 px-3 py-2">
                        <Badge variant={
                          status === 'DELIVERED' ? 'success' :
                          status === 'FAILED' ? 'danger' :
                          status === 'PENDING' ? 'warning' : 'neutral'
                        }>{status}</Badge>
                        <span className="text-sm font-semibold text-slate-700">{count}</span>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>

            {/* Notification type breakdown */}
            <Card>
              <CardHeader title="Notification Types" />
              <CardContent className="pt-0">
                {Object.keys(typeCounts).length === 0 ? (
                  <EmptyState title="No data" description="No notification type data available yet." />
                ) : (
                  <div className="space-y-2">
                    {Object.entries(typeCounts).map(([type, count]) => (
                      <div key={type} className="flex items-center justify-between rounded-xl bg-slate-50 px-3 py-2">
                        <TypeChip type={type} />
                        <span className="text-sm font-semibold text-slate-700">{count}</span>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </div>

          {/* Pending summary */}
          {summary && Object.keys(summary).length > 0 && (
            <Card>
              <CardHeader title="Pending Summary" description="Upcoming notifications by urgency" />
              <CardContent className="pt-0">
                <div className="grid gap-3 sm:grid-cols-3">
                  {summary.urgentCount != null && (
                    <StatBadge label="Urgent (≤3 days)"  value={summary.urgentCount}  color="bg-red-50 text-red-900" />
                  )}
                  {summary.upcomingCount != null && (
                    <StatBadge label="Upcoming (4-7 days)" value={summary.upcomingCount} color="bg-orange-50 text-orange-900" />
                  )}
                  {summary.totalPending != null && (
                    <StatBadge label="Total Pending" value={summary.totalPending} color="bg-slate-50 text-slate-900" />
                  )}
                </div>
              </CardContent>
            </Card>
          )}
        </>
      )}
    </div>
  )
}
