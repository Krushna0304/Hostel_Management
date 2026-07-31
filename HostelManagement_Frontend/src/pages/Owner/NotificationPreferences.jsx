import { useEffect, useState, useCallback } from 'react'
import planExpiryNotificationService from '../../services/planExpiryNotificationService'
import {
  Alert,
  Button,
  Card,
  CardContent,
  CardHeader,
  PageHeader,
  Skeleton,
} from '../../components/ui'

// ── Config form ───────────────────────────────────────────────────────────────

function ConfigForm({ config, onSave, saving }) {
  const [form, setForm] = useState({
    enabled: config?.enabled ?? true,
    maxRetryAttempts: config?.maxRetryAttempts ?? 3,
    retryDelayMinutes: config?.retryDelayMinutes ?? 5,
    cleanupDaysThreshold: config?.cleanupDaysThreshold ?? 30,
    defaultLeadTimes: (config?.defaultLeadTimes || [30, 15, 7, 3, 1]).join(', '),
  })

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target
    setForm((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }))
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    // Parse lead times string back to array
    const leadTimes = form.defaultLeadTimes
      .split(',')
      .map((s) => parseInt(s.trim(), 10))
      .filter((n) => !isNaN(n) && n > 0)

    onSave({
      enabled: form.enabled,
      maxRetryAttempts: parseInt(form.maxRetryAttempts, 10),
      retryDelayMinutes: parseInt(form.retryDelayMinutes, 10),
      cleanupDaysThreshold: parseInt(form.cleanupDaysThreshold, 10),
      defaultLeadTimes: leadTimes,
    })
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {/* Enable toggle */}
      <label className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 cursor-pointer">
        <input
          type="checkbox"
          name="enabled"
          checked={form.enabled}
          onChange={handleChange}
          className="h-4 w-4 rounded border-slate-300 text-sky-600 focus:ring-sky-500"
        />
        <div>
          <p className="text-sm font-medium text-slate-900">Plan expiry notifications enabled</p>
          <p className="text-xs text-slate-500">When disabled, no automatic notifications will be sent.</p>
        </div>
      </label>

      {/* Lead times */}
      <div>
        <label className="block text-sm font-medium text-slate-700 mb-1">
          Lead times (days before expiry)
          <span className="ml-1 text-xs text-slate-400">— comma separated</span>
        </label>
        <input
          type="text"
          name="defaultLeadTimes"
          value={form.defaultLeadTimes}
          onChange={handleChange}
          placeholder="30, 15, 7, 3, 1"
          className="w-full px-4 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-sky-300"
        />
        <p className="text-xs text-slate-400 mt-1">
          Notifications will be sent at each of these lead times before the agreement expires.
        </p>
      </div>

      {/* Retry config */}
      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">Max retry attempts</label>
          <input
            type="number"
            name="maxRetryAttempts"
            value={form.maxRetryAttempts}
            onChange={handleChange}
            min={1}
            max={10}
            className="w-full px-4 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-sky-300"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">Retry delay (minutes)</label>
          <input
            type="number"
            name="retryDelayMinutes"
            value={form.retryDelayMinutes}
            onChange={handleChange}
            min={1}
            className="w-full px-4 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-sky-300"
          />
        </div>
      </div>

      {/* Cleanup threshold */}
      <div>
        <label className="block text-sm font-medium text-slate-700 mb-1">
          Cleanup threshold (days)
          <span className="ml-1 text-xs text-slate-400">— remove old delivered notifications after N days</span>
        </label>
        <input
          type="number"
          name="cleanupDaysThreshold"
          value={form.cleanupDaysThreshold}
          onChange={handleChange}
          min={7}
          className="w-full px-4 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-sky-300"
        />
      </div>

      <div className="pt-2">
        <Button
          type="submit"
          label={saving ? 'Saving…' : 'Save Configuration'}
          disabled={saving}
        />
      </div>
    </form>
  )
}

// ── Main component ────────────────────────────────────────────────────────────

export default function NotificationPreferences() {
  const [config, setConfig] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [saveError, setSaveError] = useState('')
  const [saveSuccess, setSaveSuccess] = useState(false)
  const [saving, setSaving] = useState(false)

  const fetchConfig = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const res = await planExpiryNotificationService.getConfiguration()
      setConfig(res.data?.configuration || {})
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to load notification configuration.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { fetchConfig() }, [fetchConfig])

  const handleSave = async (updatedConfig) => {
    setSaving(true)
    setSaveError('')
    setSaveSuccess(false)
    try {
      await planExpiryNotificationService.updateConfiguration(updatedConfig)
      setSaveSuccess(true)
      setConfig(updatedConfig)
      setTimeout(() => setSaveSuccess(false), 4000)
    } catch (err) {
      setSaveError(err?.response?.data?.message || 'Failed to save configuration.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-5">
      <PageHeader
        title="Notification Preferences"
        description="Configure plan expiry notification behaviour, lead times, and retry settings."
        secondaryAction={<Button label="Refresh" variant="secondary" onClick={fetchConfig} />}
      />

      {saveSuccess && <Alert tone="success">Configuration saved successfully.</Alert>}
      {saveError && <Alert tone="error">{saveError}</Alert>}

      <Card>
        <CardHeader
          title="Plan Expiry Notification Configuration"
          description="Settings apply globally to all plan expiry notifications."
        />
        <CardContent className="pt-0">
          {loading ? (
            <div className="space-y-3">
              {[1, 2, 3].map((i) => <Skeleton key={i} className="h-10 rounded-xl" />)}
            </div>
          ) : error ? (
            <Alert tone="error">{error}</Alert>
          ) : (
            <ConfigForm config={config} onSave={handleSave} saving={saving} />
          )}
        </CardContent>
      </Card>
    </div>
  )
}
