import { useEffect, useState } from 'react'
import { Alert, Button, Card, CardContent, CardHeader, PageHeader, Skeleton } from '../../components/ui'
import reminderService from '../../services/reminderService'

// ─── Reminder type metadata ───────────────────────────────────────────────────
const REMINDER_TYPES = [
  {
    key: 'BEFORE_DUE_DATE',
    label: '1 Day Before Due Date',
    description: 'Sent automatically 1 day before each installment due date.',
    placeholders: ['{tenantName}', '{amount}', '{dueDate}', '{hostelName}', '{roomNumber}'],
  },
  {
    key: 'ON_DUE_DATE',
    label: 'On Due Date',
    description: 'Sent on the day the installment is due.',
    placeholders: ['{tenantName}', '{amount}', '{dueDate}', '{hostelName}', '{roomNumber}'],
  },
  {
    key: 'AFTER_DUE_DATE',
    label: 'Overdue Reminder',
    description: 'Sent daily for overdue installments.',
    placeholders: ['{tenantName}', '{amount}', '{dueDate}', '{hostelName}', '{roomNumber}', '{lateFee}', '{totalAmount}'],
  },
  {
    key: 'OTHER_CHARGE',
    label: 'New Other Charge',
    description: 'Sent immediately when you create a new other charge for a tenant.',
    placeholders: ['{tenantName}', '{chargeName}', '{amount}', '{hostelName}', '{roomNumber}', '{dueDate}'],
  },
]

// ─── Toggle switch ────────────────────────────────────────────────────────────
function Toggle({ enabled, onChange, disabled }) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={enabled}
      disabled={disabled}
      onClick={() => onChange(!enabled)}
      className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 ${
        enabled ? 'bg-sky-600' : 'bg-slate-200'
      }`}
    >
      <span
        className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ${
          enabled ? 'translate-x-5' : 'translate-x-0'
        }`}
      />
    </button>
  )
}

// ─── Template editor card ─────────────────────────────────────────────────────
function TemplateCard({ type, currentTemplate, defaultTemplate, canCustomise, onSave, onReset }) {
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState(currentTemplate || defaultTemplate || '')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  // Sync draft when parent data changes
  useEffect(() => {
    if (!editing) setDraft(currentTemplate || defaultTemplate || '')
  }, [currentTemplate, defaultTemplate, editing])

  const handleSave = async () => {
    if (!draft.trim()) { setError('Template cannot be empty.'); return }
    setSaving(true)
    setError('')
    try {
      await onSave(type.key, draft.trim())
      setEditing(false)
    } catch (e) {
      setError(e?.response?.data?.error || 'Failed to save template.')
    } finally {
      setSaving(false)
    }
  }

  const handleReset = async () => {
    setSaving(true)
    setError('')
    try {
      await onReset(type.key)
      setDraft(defaultTemplate || '')
      setEditing(false)
    } catch (e) {
      setError(e?.response?.data?.error || 'Failed to reset template.')
    } finally {
      setSaving(false)
    }
  }

  const isCustomised = currentTemplate && currentTemplate !== defaultTemplate

  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 space-y-3">
      {/* Header */}
      <div className="flex items-start justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            <p className="font-semibold text-slate-900 text-sm">{type.label}</p>
            {isCustomised && (
              <span className="rounded-full bg-sky-100 px-2 py-0.5 text-xs font-medium text-sky-700">
                Custom
              </span>
            )}
          </div>
          <p className="mt-0.5 text-xs text-slate-500">{type.description}</p>
        </div>
        {canCustomise && !editing && (
          <button
            onClick={() => { setEditing(true); setDraft(currentTemplate || defaultTemplate || '') }}
            className="shrink-0 rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-medium text-slate-600 hover:bg-slate-50 transition"
          >
            {isCustomised ? 'Edit' : 'Customise'}
          </button>
        )}
      </div>

      {/* Template preview / editor */}
      {editing ? (
        <div className="space-y-3">
          <textarea
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            rows={4}
            maxLength={500}
            className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-sky-500 resize-none"
          />
          <div className="flex flex-wrap gap-1">
            {type.placeholders.map((p) => (
              <button
                key={p}
                type="button"
                onClick={() => setDraft((d) => d + p)}
                className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-mono text-slate-600 hover:bg-sky-100 hover:text-sky-700 transition"
                title={`Insert ${p}`}
              >
                {p}
              </button>
            ))}
          </div>
          <p className="text-xs text-slate-400">{draft.length}/500 characters</p>
          {error && <p className="text-xs text-red-600">{error}</p>}
          <div className="flex gap-2">
            <Button label={saving ? 'Saving…' : 'Save'} variant="primary" onClick={handleSave} disabled={saving} />
            <Button label="Cancel" variant="secondary" onClick={() => { setEditing(false); setError('') }} disabled={saving} />
            {isCustomised && (
              <Button label="Reset to default" variant="secondary" onClick={handleReset} disabled={saving} />
            )}
          </div>
        </div>
      ) : (
        <div className="rounded-xl bg-slate-50 px-3 py-2.5">
          <p className="text-xs text-slate-600 leading-relaxed whitespace-pre-wrap font-mono">
            {currentTemplate || defaultTemplate || '—'}
          </p>
        </div>
      )}

      {/* Placeholder reference (collapsed) */}
      {!editing && (
        <div className="flex flex-wrap gap-1">
          {type.placeholders.map((p) => (
            <span key={p} className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-mono text-slate-500">
              {p}
            </span>
          ))}
        </div>
      )}

      {!canCustomise && (
        <p className="text-xs text-amber-600 bg-amber-50 rounded-lg px-3 py-2">
          Custom templates require a PRO or ENTERPRISE subscription.
        </p>
      )}
    </div>
  )
}

// ─── Main page ────────────────────────────────────────────────────────────────
export default function ReminderSettings() {
  const [subscription, setSubscription] = useState(null)
  const [templates, setTemplates] = useState({})       // { REMINDER_TYPE: string }
  const [customTemplates, setCustomTemplates] = useState([]) // raw list from API
  const [loading, setLoading] = useState(true)
  const [toggling, setToggling] = useState(false)
  const [error, setError] = useState('')
  const [successMsg, setSuccessMsg] = useState('')

  useEffect(() => {
    loadAll()
  }, [])

  const loadAll = async () => {
    setLoading(true)
    setError('')
    try {
      const [subRes, tplRes, customRes] = await Promise.all([
        reminderService.getSubscription(),
        reminderService.getAllTemplates(),
        reminderService.getCustomTemplates(),
      ])
      setSubscription(subRes.data)
      setTemplates(tplRes.data)          // { BEFORE_DUE_DATE: "...", ... }
      setCustomTemplates(customRes.data) // [{ templateId, reminderType, templateContent }]
    } catch (e) {
      setError(e?.response?.data?.message || 'Failed to load reminder settings.')
    } finally {
      setLoading(false)
    }
  }

  const handleToggle = async (enabled) => {
    setToggling(true)
    setError('')
    setSuccessMsg('')
    try {
      const res = await reminderService.toggleSmsReminders(enabled)
      setSubscription(res.data)
      setSuccessMsg(`SMS reminders ${enabled ? 'enabled' : 'disabled'} successfully.`)
      setTimeout(() => setSuccessMsg(''), 3000)
    } catch (e) {
      setError(e?.response?.data?.error || 'Failed to update reminder setting.')
    } finally {
      setToggling(false)
    }
  }

  const handleSaveTemplate = async (reminderType, content) => {
    await reminderService.saveTemplate(reminderType, content)
    await loadAll()
  }

  const handleResetTemplate = async (reminderType) => {
    const custom = customTemplates.find((t) => t.reminderType === reminderType)
    if (custom) {
      await reminderService.deleteTemplate(custom.templateId)
      await loadAll()
    }
  }

  const getCustomContent = (reminderType) => {
    const custom = customTemplates.find((t) => t.reminderType === reminderType)
    return custom ? custom.templateContent : null
  }

  const smsEnabled = subscription?.smsRemindersEnabled ?? false
  const canCustomise = subscription?.customTemplatesEnabled ?? false

  if (loading) {
    return (
      <div className="space-y-6">
        <PageHeader title="Reminder Settings" />
        <Skeleton className="h-32 rounded-3xl" />
        <div className="space-y-4">
          {[1, 2, 3, 4].map((i) => <Skeleton key={i} className="h-40 rounded-2xl" />)}
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Reminder Settings"
        subtitle="Control automated SMS reminders sent to tenants for due payments and new charges."
      />

      {error && (
        <Alert tone="error" title="Error">
          {error}
        </Alert>
      )}
      {successMsg && (
        <Alert tone="success" title="Saved">
          {successMsg}
        </Alert>
      )}

      {/* ── Master SMS toggle ── */}
      <Card>
        <CardContent className="p-6">
          <div className="flex items-center justify-between gap-6">
            <div>
              <p className="font-semibold text-slate-900">SMS Reminders</p>
              <p className="mt-1 text-sm text-slate-500">
                When enabled, tenants automatically receive SMS alerts 1 day before their
                installment due date, on the due date, when overdue, and when a new charge
                is created for them.
              </p>
              <div className="mt-3 flex items-center gap-2">
                <span
                  className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold ${
                    smsEnabled
                      ? 'bg-emerald-100 text-emerald-700'
                      : 'bg-slate-100 text-slate-500'
                  }`}
                >
                  <span
                    className={`h-1.5 w-1.5 rounded-full ${smsEnabled ? 'bg-emerald-500' : 'bg-slate-400'}`}
                  />
                  {smsEnabled ? 'Active' : 'Inactive'}
                </span>
                <span className="text-xs text-slate-400">
                  Plan: <span className="font-medium text-slate-600">{subscription?.tier ?? '—'}</span>
                </span>
              </div>
            </div>
            <Toggle enabled={smsEnabled} onChange={handleToggle} disabled={toggling} />
          </div>
        </CardContent>
      </Card>

      {/* ── SMS Templates ── */}
      <Card>
        <CardHeader
          title="SMS Templates"
          description={
            canCustomise
              ? 'Customise the message sent for each reminder type. Click the placeholders to insert them.'
              : 'Default templates are shown below. Upgrade to PRO or ENTERPRISE to customise them.'
          }
        />
        <CardContent className="p-6 space-y-4">
          {REMINDER_TYPES.map((type) => (
            <TemplateCard
              key={type.key}
              type={type}
              currentTemplate={getCustomContent(type.key) || templates[type.key]}
              defaultTemplate={templates[type.key]}
              canCustomise={canCustomise}
              onSave={handleSaveTemplate}
              onReset={handleResetTemplate}
            />
          ))}
        </CardContent>
      </Card>

      {/* ── How it works ── */}
      <Card>
        <CardHeader title="How it works" />
        <CardContent className="p-6">
          <ol className="space-y-3 text-sm text-slate-600 list-decimal list-inside">
            <li>
              <span className="font-medium text-slate-800">1 day before due date</span> — a reminder
              SMS is sent at 8:00 AM to every tenant with an upcoming installment.
            </li>
            <li>
              <span className="font-medium text-slate-800">On due date</span> — a reminder SMS is
              sent at 9:00 AM if the installment is still unpaid.
            </li>
            <li>
              <span className="font-medium text-slate-800">Overdue</span> — a daily SMS is sent at
              10:00 AM for every overdue installment until it is paid.
            </li>
            <li>
              <span className="font-medium text-slate-800">New other charge</span> — an SMS is sent
              immediately when you create a new charge for a tenant or room.
            </li>
          </ol>
          <p className="mt-4 text-xs text-slate-400">
            All times are in IST. Reminders are only sent when SMS Reminders is toggled ON.
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
