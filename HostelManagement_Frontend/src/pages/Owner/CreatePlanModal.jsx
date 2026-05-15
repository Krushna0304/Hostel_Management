import { useState, useEffect } from 'react'
import { planService } from '../../services/agreementService'
import { Alert, Button, Field, NumericInput } from '../../components/ui'
import RentSection from '../../components/billing/RentSection'
import OneTimeChargesSection from '../../components/billing/OneTimeChargesSection'
import OtherChargesSection from '../../components/billing/OtherChargesSection'
import RecurringChargesSection from '../../components/billing/RecurringChargesSection'
import InstallmentModelSection from '../../components/billing/InstallmentModelSection'
import BillingPreviewSection from '../../components/billing/BillingPreviewSection'
import { showExtensionWarning, suppressExtensionErrors } from '../../utils/extensionDetector'

const INITIAL_FORM = {
  planName: '',
  planType: '',
  rentDetails: { monthlyRent: '', currency: 'INR' },
  duration: { value: 12, unit: 'MONTH', minimumStayMonths: 3 },
  paymentModel: { mode: 'MONTHLY', paymentTiming: 'PREPAID', installments: 3, dueDayOfMonth: 5 },
  charges: {
    securityDeposit: { amount: '', refundable: true, paymentTiming: 'AT_AGREEMENT' },
    cleaningCharges: { 
      monthlyCleaningCharge: { applicable: false, amount: '', paymentTiming: 'IN_INSTALLMENTS', refundable: false },
      deepCleaningOnExit: { applicable: false, amount: '', paymentTiming: 'AT_END', refundable: false } 
    },
    maintenanceCharges: {
      oneTimeMaintenanceCharge: { applicable: false, amount: '', paymentTiming: 'AT_AGREEMENT', refundable: false },
      monthlyMaintenanceCharge: { applicable: false, amount: '', paymentTiming: 'IN_INSTALLMENTS', refundable: false }
    },
    utilityCharges: {
      electricity: { type: 'INCLUDED', fixedAmount: '', paymentTiming: 'IN_INSTALLMENTS', refundable: false },
      water: { type: 'INCLUDED', monthlyAmount: '', paymentTiming: 'IN_INSTALLMENTS', refundable: false }
    }
  },
  oneTimeCharges: [], // Custom one-time charges
  monthlyRecurringCharges: [], // Custom monthly recurring charges
  freeFacilities: { included: true, facilities: [] },
  latePaymentPolicy: { gracePeriodDays: 5, penalty: { type: 'PER_DAY', amount: '', maxAmount: '' } },
  rulesAndRegulations: {
    houseRules: { smokingAllowed: false, petsAllowed: false, quietHours: { from: '22:00', to: '06:00' } },
    facilityUsageRules: [],
  },
  legal: { agreementLock: true, modificationAllowedAfterSign: false, jurisdiction: '' },
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

const inputCls = "w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-100"
const selectCls = inputCls

export default function CreatePlanModal({ onClose, onCreated }) {
  const [form, setForm] = useState(INITIAL_FORM)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [newFacility, setNewFacility] = useState('')
  const [newRule, setNewRule] = useState('')
  const [newCustomKey, setNewCustomKey] = useState('')
  const [newCustomValue, setNewCustomValue] = useState('')
  const [extensionWarning, setExtensionWarning] = useState(false)

  // Check for browser extension conflicts on mount
  useEffect(() => {
    suppressExtensionErrors()
    const hasConflicts = showExtensionWarning()
    setExtensionWarning(hasConflicts)
  }, [])

  const set = (path, value) => {
    try {
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
    } catch (err) {
      console.error('Form update error:', err)
      setError('Form update failed. Please try refreshing the page.')
    }
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

  const removeFacility = (i) => {
    setForm(prev => ({
      ...prev,
      freeFacilities: { ...prev.freeFacilities, facilities: prev.freeFacilities.facilities.filter((_, idx) => idx !== i) },
    }))
  }

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

  const removeRule = (i) => {
    setForm(prev => ({
      ...prev,
      rulesAndRegulations: {
        ...prev.rulesAndRegulations,
        facilityUsageRules: prev.rulesAndRegulations.facilityUsageRules.filter((_, idx) => idx !== i),
      },
    }))
  }

  const addCustomField = () => {
    if (!newCustomKey.trim()) return
    setForm(prev => ({ ...prev, customFields: { ...prev.customFields, [newCustomKey.trim()]: newCustomValue.trim() } }))
    setNewCustomKey('')
    setNewCustomValue('')
  }

  const removeCustomField = (key) => {
    setForm(prev => {
      const cf = { ...prev.customFields }
      delete cf[key]
      return { ...prev, customFields: cf }
    })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    
    // Prevent multiple submissions
    if (loading) return
    
    if (!form.planName.trim()) { setError('Plan name is required.'); return }
    if (!form.planType) { setError('Plan type is required.'); return }
    if (!form.rentDetails.monthlyRent) { setError('Monthly rent is required.'); return }

    try {
      setLoading(true)
      setError('')
      
      // Enhanced payload structure with validation
      const payload = {
        ...form,
        rentDetails: { 
          ...form.rentDetails, 
          monthlyRent: Number(form.rentDetails.monthlyRent) 
        },
        charges: {
          securityDeposit: { 
            ...form.charges.securityDeposit, 
            amount: Number(form.charges.securityDeposit.amount) || 0,
            paymentTiming: 'AT_AGREEMENT'
          },
          cleaningCharges: { 
            monthlyCleaningCharge: {
              applicable: Boolean(form.charges.cleaningCharges?.monthlyCleaningCharge?.amount),
              amount: Number(form.charges.cleaningCharges?.monthlyCleaningCharge?.amount) || 0,
              paymentTiming: 'IN_INSTALLMENTS',
              refundable: false
            },
            deepCleaningOnExit: { 
              applicable: Boolean(form.charges.cleaningCharges?.deepCleaningOnExit?.amount),
              amount: Number(form.charges.cleaningCharges?.deepCleaningOnExit?.amount) || 0,
              paymentTiming: 'AT_END',
              refundable: form.charges.cleaningCharges?.deepCleaningOnExit?.refundable ?? false
            }
          },
          maintenanceCharges: {
            oneTimeMaintenanceCharge: {
              applicable: Boolean(form.charges.maintenanceCharges?.oneTimeMaintenanceCharge?.amount),
              amount: Number(form.charges.maintenanceCharges?.oneTimeMaintenanceCharge?.amount) || 0,
              paymentTiming: 'AT_AGREEMENT',
              refundable: form.charges.maintenanceCharges?.oneTimeMaintenanceCharge?.refundable ?? false
            },
            monthlyMaintenanceCharge: {
              applicable: Boolean(form.charges.maintenanceCharges?.monthlyMaintenanceCharge?.amount),
              amount: Number(form.charges.maintenanceCharges?.monthlyMaintenanceCharge?.amount) || 0,
              paymentTiming: 'IN_INSTALLMENTS',
              refundable: false
            }
          },
          utilityCharges: {
            electricity: {
              type: 'INCLUDED',
              fixedAmount: Number(form.charges.utilityCharges?.electricity?.fixedAmount) || 0,
              paymentTiming: 'IN_INSTALLMENTS',
              refundable: false
            },
            water: {
              type: 'INCLUDED',
              monthlyAmount: Number(form.charges.utilityCharges?.water?.monthlyAmount) || 0,
              paymentTiming: 'IN_INSTALLMENTS',
              refundable: false
            }
          },
          // Custom charges nested under charges
          customCharges: {
            oneTimeCharges: form.oneTimeCharges || [],
            monthlyRecurringCharges: form.monthlyRecurringCharges || []
          }
        },
        // Include custom fields
        customFields: form.customFields || {},
        latePaymentPolicy: {
          ...form.latePaymentPolicy,
          penalty: {
            ...form.latePaymentPolicy.penalty,
            amount: Number(form.latePaymentPolicy.penalty.amount) || 0,
            maxAmount: Number(form.latePaymentPolicy.penalty.maxAmount) || 0,
          },
        },
      }
      
      const res = await planService.createPlan(payload)
      
      if (onCreated && typeof onCreated === 'function') {
        onCreated(res.data)
      }
    } catch (err) {
      // Handle different types of errors
      if (err.name === 'AbortError') {
        setError('Request was cancelled. Please try again.')
      } else if (err.code === 'NETWORK_ERROR') {
        setError('Network error. Please check your connection and try again.')
      } else {
        const errorMessage = err?.response?.data?.message || err?.message || 'Failed to create plan.'
        setError(errorMessage)
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4" onClick={onClose}>
      <div
        className="relative w-full max-w-2xl max-h-[92vh] overflow-y-auto rounded-3xl bg-white shadow-2xl"
        onClick={e => e.stopPropagation()}
      >
        {/* Header */}
        <div className="sticky top-0 z-10 flex items-center justify-between bg-white px-6 py-4 border-b border-slate-100">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-600">Owner workspace</p>
            <h2 className="text-xl font-bold text-slate-950">Create tenant plan</h2>
          </div>
          <button onClick={onClose} className="rounded-full p-2 text-slate-400 hover:bg-slate-100 transition">✕</button>
        </div>

        <form onSubmit={handleSubmit} className="px-6 py-5 space-y-4">
          {error ? <Alert tone="error">{error}</Alert> : null}
          {extensionWarning ? (
            <Alert tone="warning">
              <strong>⚠️ Browser Extension Detected:</strong> If you experience form issues, try disabling browser extensions or use incognito mode.
            </Alert>
          ) : null}
          
          {/* Plan Name */}
          <Field label="Plan name *">
            <input className={inputCls} value={form.planName} onChange={e => set('planName', e.target.value)} placeholder="e.g. Standard Monthly Room Plan" />
          </Field>

          {/* Plan Type */}
          <Field label="Plan type *">
            <select
              className={selectCls}
              value={form.planType}
              onChange={e => set('planType', e.target.value)}
              required
            >
              <option value="">Select plan type</option>
              <option value="PG_ROOM">PG Room</option>
              <option value="FLAT">Flat</option>
            </select>
          </Field>

          {/* Enhanced Billing Sections */}
          <RentSection form={form} set={set} />
          <OneTimeChargesSection form={form} set={set} />
          <OtherChargesSection form={form} set={set} />
          <RecurringChargesSection form={form} set={set} />
          <InstallmentModelSection form={form} set={set} />
          <BillingPreviewSection form={form} />

          {/* ── Facilities ── */}
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

          {/* ── Late Payment Policy ── */}
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

          {/* ── House Rules ── */}
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

          {/* ── Legal ── */}
          <Section title="⚖️ Legal">
            <div className="grid grid-cols-2 gap-3">
              <Field label="Agreement Lock">
                <select className={selectCls} value={form.legal.agreementLock} onChange={e => set('legal.agreementLock', e.target.value === 'true')}>
                  <option value="true">Yes</option><option value="false">No</option>
                </select>
              </Field>
              <Field label="Modification After Sign">
                <select className={selectCls} value={form.legal.modificationAllowedAfterSign} onChange={e => set('legal.modificationAllowedAfterSign', e.target.value === 'true')}>
                  <option value="false">Not Allowed</option><option value="true">Allowed</option>
                </select>
              </Field>
              <Field label="Jurisdiction">
                <input className={inputCls} value={form.legal.jurisdiction} onChange={e => set('legal.jurisdiction', e.target.value)} placeholder="e.g. Nagpur, Maharashtra" />
              </Field>
            </div>
          </Section>

          {/* ── Custom Fields ── */}
          <Section title="🔧 Custom Fields">
            <p className="text-xs text-slate-500 mb-2">Add any additional key-value fields specific to your plan.</p>
            <div className="flex gap-2">
              <input className={inputCls} value={newCustomKey} onChange={e => setNewCustomKey(e.target.value)} placeholder="Field name (e.g. mealPlan)" />
              <input className={inputCls} value={newCustomValue} onChange={e => setNewCustomValue(e.target.value)} placeholder="Value (e.g. Included)" />
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

          {/* Actions */}
          <div className="flex gap-3 pt-2">
            <Button type="submit" label="Create plan" loading={loading} fullWidth />
            <Button type="button" label="Cancel" variant="secondary" fullWidth onClick={onClose} />
          </div>
        </form>
      </div>
    </div>
  )
}
