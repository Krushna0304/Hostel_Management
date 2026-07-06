import { useState, useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { planService } from '../../services/agreementService'
import { Alert, Button, Card, CardContent, CardHeader, Field, NumericInput, PageHeader } from '../../components/ui'
import RentSection from '../../components/billing/RentSection'
import OneTimeChargesSection from '../../components/billing/OneTimeChargesSection'
import RecurringChargesSection from '../../components/billing/RecurringChargesSection'
import InstallmentModelSection from '../../components/billing/InstallmentModelSection'
import BillingPreviewSection from '../../components/billing/BillingPreviewSection'

const INITIAL_FORM = {
  planName: '',
  planType: '',
  rentDetails: { monthlyRent: '', currency: 'INR' },
  duration: { durationType: 'FIXED', value: 12, unit: 'MONTH', minimumStayMonths: 3 },
  paymentModel: { mode: 'MONTHLY', paymentTiming: 'PREPAID', installments: 3, dueDayOfMonth: 5 },
  charges: {
    securityDeposit: { amount: '', refundable: true, paymentTiming: 'AT_AGREEMENT' },
    cleaningCharges: {
      monthlyCleaningCharge: { applicable: false, amount: '', paymentTiming: 'IN_INSTALLMENTS', refundable: false },
      deepCleaningOnExit: { applicable: false, amount: '', paymentTiming: 'AT_END', refundable: false },
    },
    maintenanceCharges: {
      oneTimeMaintenanceCharge: { applicable: false, amount: '', paymentTiming: 'AT_AGREEMENT', refundable: false },
      monthlyMaintenanceCharge: { applicable: false, amount: '', paymentTiming: 'IN_INSTALLMENTS', refundable: false },
    },
    utilityCharges: {
      electricity: { type: 'INCLUDED', fixedAmount: '', paymentTiming: 'IN_INSTALLMENTS', refundable: false },
      water: { type: 'INCLUDED', monthlyAmount: '', paymentTiming: 'IN_INSTALLMENTS', refundable: false },
    },
  },
  oneTimeCharges: [],
  monthlyRecurringCharges: [],
  freeFacilities: { included: true, facilities: [] },
  latePaymentPolicy: { gracePeriodDays: 5, penalty: { type: 'PER_DAY', amount: '', maxAmount: '' } },
  rulesAndRegulations: {
    houseRules: { smokingAllowed: false, petsAllowed: false, quietHours: { from: '22:00', to: '06:00' } },
    facilityUsageRules: [],
  },
  agreementCancellationRules: {
    tenantCancellation: { allowed: false, noticePeriodDays: '', earlyExitPenalty: { type: '', value: '' } },
  },
  customFields: {},
}

function Section({ title, children }) {
  const [open, setOpen] = useState(false)
  return (
    <div className="rounded-2xl border border-slate-200">
      <button
        type="button"
        onClick={() => setOpen(o => !o)}
        className="flex w-full items-center justify-between px-4 py-3 text-sm font-semibold text-slate-900 hover:bg-slate-50 rounded-2xl transition"
      >
        {title}
        <span className="text-slate-400">{open ? '▲' : '▼'}</span>
      </button>
      {open && <div className="border-t border-slate-100 px-4 py-4 space-y-3">{children}</div>}
    </div>
  )
}

const inputCls = 'w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-100'
const selectCls = inputCls

export default function CreatePlanPage() {
  const navigate = useNavigate()
  const location = useLocation()

  const planToEdit = location.state?.plan ?? null
  const editMode = Boolean(planToEdit)

  const [form, setForm] = useState(editMode ? planToEdit : INITIAL_FORM)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [alert, setAlert] = useState(null)
  const [newFacility, setNewFacility] = useState('')
  const [newRule, setNewRule] = useState('')
  const [newCustomKey, setNewCustomKey] = useState('')
  const [newCustomValue, setNewCustomValue] = useState('')
  const [showGuide, setShowGuide] = useState(true)

  useEffect(() => {
    if (editMode && planToEdit) setForm(planToEdit)
  }, [editMode, planToEdit])

  useEffect(() => {
    const saved = localStorage.getItem('planGuideVisible')
    if (saved !== null) setShowGuide(JSON.parse(saved))
  }, [])

  const toggleGuide = () => {
    const next = !showGuide
    setShowGuide(next)
    localStorage.setItem('planGuideVisible', JSON.stringify(next))
  }

  const set = (path, value) => {
    setForm(prev => {
      const next = structuredClone(prev)
      const keys = path.split('.')
      let obj = next
      for (let i = 0; i < keys.length - 1; i++) {
        if (!obj[keys[i]]) obj[keys[i]] = {}
        obj = obj[keys[i]]
      }
      obj[keys[keys.length - 1]] = value
      return next
    })
  }

  const addFacility = () => {
    if (!newFacility.trim()) return
    setForm(prev => ({
      ...prev,
      freeFacilities: {
        ...prev.freeFacilities,
        facilities: [...prev.freeFacilities.facilities, { name: newFacility.trim(), description: '', availability: '24x7' }],
      },
    }))
    setNewFacility('')
  }

  const removeFacility = (i) =>
    setForm(prev => ({
      ...prev,
      freeFacilities: { ...prev.freeFacilities, facilities: prev.freeFacilities.facilities.filter((_, idx) => idx !== i) },
    }))

  const addRule = () => {
    if (!newRule.trim()) return
    setForm(prev => ({
      ...prev,
      rulesAndRegulations: {
        ...prev.rulesAndRegulations,
        facilityUsageRules: [...prev.rulesAndRegulations.facilityUsageRules, newRule.trim()],
      },
    }))
    setNewRule('')
  }

  const removeRule = (i) =>
    setForm(prev => ({
      ...prev,
      rulesAndRegulations: {
        ...prev.rulesAndRegulations,
        facilityUsageRules: prev.rulesAndRegulations.facilityUsageRules.filter((_, idx) => idx !== i),
      },
    }))

  const addCustomField = () => {
    if (!newCustomKey.trim()) return
    setForm(prev => ({ ...prev, customFields: { ...prev.customFields, [newCustomKey.trim()]: newCustomValue.trim() } }))
    setNewCustomKey('')
    setNewCustomValue('')
  }

  const removeCustomField = (key) =>
    setForm(prev => {
      const cf = { ...prev.customFields }
      delete cf[key]
      return { ...prev, customFields: cf }
    })

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (loading) return
    if (!form.planName.trim()) { setError('Plan name is required.'); return }
    if (!form.planType) { setError('Plan type is required.'); return }
    if (!form.rentDetails.monthlyRent) { setError('Monthly rent is required.'); return }

    try {
      setLoading(true)
      setError('')

      const payload = {
        ...form,
        rentDetails: { ...form.rentDetails, monthlyRent: Number(form.rentDetails.monthlyRent) },
        charges: {
          securityDeposit: { ...form.charges.securityDeposit, amount: Number(form.charges.securityDeposit.amount) || 0, paymentTiming: 'AT_AGREEMENT' },
          cleaningCharges: {
            monthlyCleaningCharge: { applicable: Boolean(form.charges.cleaningCharges?.monthlyCleaningCharge?.amount), amount: Number(form.charges.cleaningCharges?.monthlyCleaningCharge?.amount) || 0, paymentTiming: 'IN_INSTALLMENTS', refundable: false },
            deepCleaningOnExit: { applicable: Boolean(form.charges.cleaningCharges?.deepCleaningOnExit?.amount), amount: Number(form.charges.cleaningCharges?.deepCleaningOnExit?.amount) || 0, paymentTiming: 'AT_END', refundable: form.charges.cleaningCharges?.deepCleaningOnExit?.refundable ?? false },
          },
          maintenanceCharges: {
            oneTimeMaintenanceCharge: { applicable: Boolean(form.charges.maintenanceCharges?.oneTimeMaintenanceCharge?.amount), amount: Number(form.charges.maintenanceCharges?.oneTimeMaintenanceCharge?.amount) || 0, paymentTiming: 'AT_AGREEMENT', refundable: form.charges.maintenanceCharges?.oneTimeMaintenanceCharge?.refundable ?? false },
            monthlyMaintenanceCharge: { applicable: Boolean(form.charges.maintenanceCharges?.monthlyMaintenanceCharge?.amount), amount: Number(form.charges.maintenanceCharges?.monthlyMaintenanceCharge?.amount) || 0, paymentTiming: 'IN_INSTALLMENTS', refundable: false },
          },
          utilityCharges: {
            electricity: { type: 'INCLUDED', fixedAmount: Number(form.charges.utilityCharges?.electricity?.fixedAmount) || 0, paymentTiming: 'IN_INSTALLMENTS', refundable: false },
            water: { type: 'INCLUDED', monthlyAmount: Number(form.charges.utilityCharges?.water?.monthlyAmount) || 0, paymentTiming: 'IN_INSTALLMENTS', refundable: false },
          },
          customCharges: {
            oneTimeCharges: form.oneTimeCharges || [],
            monthlyRecurringCharges: form.monthlyRecurringCharges || [],
          },
        },
        oneTimeCharges: form.oneTimeCharges || [],
        monthlyRecurringCharges: form.monthlyRecurringCharges || [],
        customFields: form.customFields || {},
        agreementCancellationRules: {
          tenantCancellation: {
            allowed: Boolean(form.agreementCancellationRules?.tenantCancellation?.allowed),
            noticePeriodDays: form.agreementCancellationRules?.tenantCancellation?.allowed && form.agreementCancellationRules?.tenantCancellation?.noticePeriodDays
              ? Number(form.agreementCancellationRules.tenantCancellation.noticePeriodDays)
              : null,
            earlyExitPenalty: form.agreementCancellationRules?.tenantCancellation?.allowed && form.agreementCancellationRules?.tenantCancellation?.earlyExitPenalty?.type
              ? { type: form.agreementCancellationRules.tenantCancellation.earlyExitPenalty.type, value: form.agreementCancellationRules.tenantCancellation.earlyExitPenalty.value ? Number(form.agreementCancellationRules.tenantCancellation.earlyExitPenalty.value) : 0 }
              : null,
          },
        },
        latePaymentPolicy: {
          ...form.latePaymentPolicy,
          penalty: { ...form.latePaymentPolicy.penalty, amount: Number(form.latePaymentPolicy.penalty.amount) || 0, maxAmount: Number(form.latePaymentPolicy.penalty.maxAmount) || 0 },
        },
      }

      const res = editMode
        ? await planService.updatePlan(planToEdit.id, payload)
        : await planService.createPlan(payload)

      setAlert({ tone: 'success', message: editMode ? '✅ Plan updated successfully!' : '✅ Plan created successfully!' })
      setTimeout(() => {
        navigate('/owner/plans', { state: { updatedPlan: res.data } })
      }, 2000)
    } catch (err) {
      const errorMessage = err?.response?.data?.message || err?.message || `Failed to ${editMode ? 'update' : 'create'} plan.`
      setError(errorMessage)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Plan setup"
        title={editMode ? 'Edit tenant plan' : 'Create tenant plan'}
        description="Define rent, charges, payment model, and rules. Plans are reused across agreements to keep billing consistent."
        secondaryAction={<Button label="Back to plans" variant="secondary" onClick={() => navigate('/owner/plans')} />}
      />

      {alert ? (
        <div className="fixed top-4 right-4 z-[9999] max-w-md">
          <Alert tone={alert.tone} onClose={() => setAlert(null)} className="shadow-lg border-2">
            {alert.message}
          </Alert>
        </div>
      ) : null}

      <div className={`grid gap-6 ${showGuide ? 'xl:grid-cols-[1.3fr_0.7fr]' : 'grid-cols-1'}`}>
        <Card>
          <CardHeader
            title="Plan details"
            description="Fill in the billing structure. All charge sections are optional except monthly rent."
          />
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              {error ? <Alert tone="error">{error}</Alert> : null}

              <Field label="Plan name *">
                <input className={inputCls} value={form.planName} onChange={e => set('planName', e.target.value)} placeholder="e.g. Standard Monthly Room Plan" />
              </Field>

              <Field label="Plan type *">
                <select className={selectCls} value={form.planType} onChange={e => set('planType', e.target.value)} required>
                  <option value="">Select plan type</option>
                  <option value="PG_ROOM">PG Room</option>
                  <option value="FLAT">Flat</option>
                </select>
              </Field>

              <RentSection form={form} set={set} />
              <RecurringChargesSection form={form} set={set} />
              <OneTimeChargesSection form={form} set={set} />
              <InstallmentModelSection form={form} set={set} />
              <BillingPreviewSection form={form} />

              <Section title="⚠️ Late Payment Policy">
                <div className="grid grid-cols-2 gap-3">
                  <Field label="Grace Period (days)">
                    <NumericInput className={inputCls} value={form.latePaymentPolicy.gracePeriodDays} onChange={e => set('latePaymentPolicy.gracePeriodDays', Number(e.target.value))} min="0" max="30" />
                  </Field>
                  <Field label="Penalty Type">
                    <select className={selectCls} value={form.latePaymentPolicy.penalty.type} onChange={e => set('latePaymentPolicy.penalty.type', e.target.value)}>
                      <option>PER_DAY</option><option>FLAT</option><option>PERCENTAGE</option>
                    </select>
                  </Field>
                  <Field label="Penalty Amount (₹)">
                    <NumericInput className={inputCls} value={form.latePaymentPolicy.penalty.amount} onChange={e => set('latePaymentPolicy.penalty.amount', e.target.value)} min="0" />
                  </Field>
                  <Field label="Max Penalty (₹)">
                    <NumericInput className={inputCls} value={form.latePaymentPolicy.penalty.maxAmount} onChange={e => set('latePaymentPolicy.penalty.maxAmount', e.target.value)} min="0" />
                  </Field>
                </div>
              </Section>

              <Section title="🚪 Cancellation Policy">
                <div className="grid grid-cols-2 gap-3">
                  <Field label="Tenant Cancellation Allowed">
                    <select className={selectCls} value={form.agreementCancellationRules?.tenantCancellation?.allowed ?? false} onChange={e => set('agreementCancellationRules.tenantCancellation.allowed', e.target.value === 'true')}>
                      <option value="false">No</option><option value="true">Yes</option>
                    </select>
                  </Field>
                  {form.agreementCancellationRules?.tenantCancellation?.allowed ? (
                    <>
                      <Field label="Notice Period (days)">
                        <NumericInput className={inputCls} value={form.agreementCancellationRules?.tenantCancellation?.noticePeriodDays ?? ''} onChange={e => set('agreementCancellationRules.tenantCancellation.noticePeriodDays', e.target.value)} min="0" placeholder="e.g. 30" />
                      </Field>
                      <Field label="Early Exit Penalty Type">
                        <select className={selectCls} value={form.agreementCancellationRules?.tenantCancellation?.earlyExitPenalty?.type ?? ''} onChange={e => set('agreementCancellationRules.tenantCancellation.earlyExitPenalty.type', e.target.value)}>
                          <option value="">No Penalty</option>
                          <option value="MONTH_RENT">Months of Rent</option>
                          <option value="FIXED">Fixed Amount (₹)</option>
                        </select>
                      </Field>
                      {form.agreementCancellationRules?.tenantCancellation?.earlyExitPenalty?.type && (
                        <Field label={form.agreementCancellationRules.tenantCancellation.earlyExitPenalty.type === 'MONTH_RENT' ? 'Number of Months' : 'Penalty Amount (₹)'}>
                          <NumericInput className={inputCls} value={form.agreementCancellationRules?.tenantCancellation?.earlyExitPenalty?.value ?? ''} onChange={e => set('agreementCancellationRules.tenantCancellation.earlyExitPenalty.value', e.target.value)} min="0" />
                        </Field>
                      )}
                    </>
                  ) : null}
                </div>
              </Section>

              <Section title="✨ Free Facilities">
                <div className="flex gap-2">
                  <input className={inputCls} value={newFacility} onChange={e => setNewFacility(e.target.value)} placeholder="e.g. Wi-Fi" onKeyDown={e => e.key === 'Enter' && (e.preventDefault(), addFacility())} />
                  <button type="button" onClick={addFacility} className="px-4 py-2 bg-slate-950 text-white rounded-xl text-sm font-medium hover:bg-slate-800 transition">Add</button>
                </div>
                <div className="flex flex-wrap gap-2 mt-2">
                  {form.freeFacilities.facilities.map((f, i) => (
                    <span key={i} className="flex items-center gap-1 px-3 py-1 bg-green-100 text-green-800 rounded-full text-xs">
                      {f.name}
                      <button type="button" onClick={() => removeFacility(i)} className="text-green-600 hover:text-red-600 ml-1">✕</button>
                    </span>
                  ))}
                </div>
              </Section>

              <Section title="📋 House Rules">
                <div className="grid grid-cols-2 gap-3">
                  <Field label="Smoking Allowed">
                    <select className={selectCls} value={form.rulesAndRegulations.houseRules.smokingAllowed} onChange={e => set('rulesAndRegulations.houseRules.smokingAllowed', e.target.value === 'true')}>
                      <option value="false">No</option><option value="true">Yes</option>
                    </select>
                  </Field>
                  <Field label="Pets Allowed">
                    <select className={selectCls} value={form.rulesAndRegulations.houseRules.petsAllowed} onChange={e => set('rulesAndRegulations.houseRules.petsAllowed', e.target.value === 'true')}>
                      <option value="false">No</option><option value="true">Yes</option>
                    </select>
                  </Field>
                  <Field label="Quiet Hours From">
                    <input type="time" className={inputCls} value={form.rulesAndRegulations.houseRules.quietHours.from} onChange={e => set('rulesAndRegulations.houseRules.quietHours.from', e.target.value)} />
                  </Field>
                  <Field label="Quiet Hours To">
                    <input type="time" className={inputCls} value={form.rulesAndRegulations.houseRules.quietHours.to} onChange={e => set('rulesAndRegulations.houseRules.quietHours.to', e.target.value)} />
                  </Field>
                </div>
                <Field label="Facility Usage Rules">
                  <div className="flex gap-2">
                    <input className={inputCls} value={newRule} onChange={e => setNewRule(e.target.value)} placeholder="e.g. No loud music after 10 PM" onKeyDown={e => e.key === 'Enter' && (e.preventDefault(), addRule())} />
                    <button type="button" onClick={addRule} className="px-4 py-2 bg-slate-950 text-white rounded-xl text-sm font-medium hover:bg-slate-800 transition">Add</button>
                  </div>
                  <ul className="mt-2 space-y-1">
                    {form.rulesAndRegulations.facilityUsageRules.map((r, i) => (
                      <li key={i} className="flex items-center justify-between text-sm text-slate-700 bg-slate-50 rounded-xl px-3 py-2">
                        {r}
                        <button type="button" onClick={() => removeRule(i)} className="text-slate-400 hover:text-red-500 ml-2">✕</button>
                      </li>
                    ))}
                  </ul>
                </Field>
              </Section>

              <Section title="🔧 Custom Fields">
                <p className="text-xs text-slate-500 mb-2">Add any additional key-value fields specific to your plan.</p>
                <div className="flex gap-2">
                  <input className={inputCls} value={newCustomKey} onChange={e => setNewCustomKey(e.target.value)} placeholder="Field name" />
                  <input className={inputCls} value={newCustomValue} onChange={e => setNewCustomValue(e.target.value)} placeholder="Value" />
                  <button type="button" onClick={addCustomField} className="px-4 py-2 bg-slate-950 text-white rounded-xl text-sm font-medium hover:bg-slate-800 transition whitespace-nowrap">Add</button>
                </div>
                {Object.keys(form.customFields).length > 0 && (
                  <div className="mt-2 space-y-1">
                    {Object.entries(form.customFields).map(([key, value]) => (
                      <div key={key} className="flex items-center justify-between text-sm bg-amber-50 border border-amber-200 rounded-xl px-3 py-2">
                        <span><span className="font-medium text-amber-900">{key}:</span> <span className="text-amber-800">{value}</span></span>
                        <button type="button" onClick={() => removeCustomField(key)} className="text-amber-400 hover:text-red-500 ml-2">✕</button>
                      </div>
                    ))}
                  </div>
                )}
              </Section>

              <div className="flex gap-3 pt-2">
                <Button type="submit" label={editMode ? 'Update plan' : 'Create plan'} loading={loading} fullWidth />
                <Button type="button" label="Cancel" variant="secondary" fullWidth onClick={() => navigate('/owner/plans')} />
              </div>
            </form>
          </CardContent>
        </Card>

        {showGuide ? (
          <div className="relative">
            {/* Toggle Button */}
            <button
              onClick={toggleGuide}
              className="absolute -top-2 -right-2 z-10 flex h-8 w-8 items-center justify-center rounded-full bg-slate-800 text-white shadow-lg transition-all hover:bg-slate-700 hover:scale-105"
              title="Hide guide"
            >
              <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>

            <div className="rounded-[1.75rem] border border-slate-200 bg-slate-950 p-6 text-white">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-300">How plans work</p>
              <div className="mt-6 space-y-4">
                {[
                  'A plan defines rent, charges, and payment rules once — then gets reused across agreements.',
                  'Monthly rent and charges are combined into installment amounts based on your payment model.',
                  'Deposits and one-time fees are collected at agreement activation, separate from installments.',
                  'You can attach any saved plan when creating a new agreement for a tenant.',
                ].map((item, index) => (
                  <div key={index} className="flex gap-3">
                    <div className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-white/10 text-sm font-semibold">
                      {index + 1}
                    </div>
                    <p className="text-sm leading-6 text-slate-300">{item}</p>
                  </div>
                ))}
              </div>

              <div className="mt-6 border-t border-white/10 pt-5">
                <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-300">Tip</p>
                <p className="mt-3 text-sm leading-6 text-slate-300">
                  Required: <span className="text-white font-medium">Plan name, Plan type, Monthly rent.</span>
                  <br /><br />
                  All charge sections (deposits, utilities, maintenance) are optional — expand only what applies to your property.
                </p>
              </div>
            </div>
          </div>
        ) : null}
      </div>

      {/* Show guide button - only when hidden */}
      {!showGuide && (
        <div className="fixed bottom-6 right-6 z-50">
          <button
            onClick={toggleGuide}
            className="flex h-12 w-12 items-center justify-center rounded-full bg-slate-950 text-white shadow-lg transition-all hover:bg-slate-800 hover:scale-105"
            title="Show guide"
          >
            <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </button>
        </div>
      )}
    </div>
  )
}
