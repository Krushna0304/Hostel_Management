import { useState, useEffect } from 'react'
import { Field, Button, NumericInput } from '../ui'
import { planService } from '../../services/agreementService'

const inputCls = "w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-100"
const selectCls = inputCls

export default function InstallmentModelSection({ form, set }) {
  const [installmentPreview, setInstallmentPreview] = useState(null)
  const [loading, setLoading] = useState(false)

  const calculateInstallmentPreview = async () => {
    if (!form.duration?.value || !form.paymentModel?.installments) return

    setLoading(true)
    try {
      const response = await planService.calculateInstallments({
        totalMonths: form.duration.value,
        numberOfInstallments: form.paymentModel.installments,
        distributionStrategy: 'AUTO'
      })
      
      setInstallmentPreview(response.data)
    } catch (error) {
      console.error('Failed to calculate installment preview:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    calculateInstallmentPreview()
  }, [form.duration?.value, form.paymentModel?.installments])

  return (
    <div className="rounded-2xl border border-slate-200">
      <div className="px-4 py-3 bg-slate-50 rounded-t-2xl border-b border-slate-200">
        <h3 className="text-sm font-semibold text-slate-900">📊 Installment Model</h3>
        <p className="text-xs text-slate-500 mt-1">Configure flexible installment distribution</p>
      </div>
      <div className="px-4 py-4 space-y-3">
        {/* Duration */}
        <div className="grid grid-cols-3 gap-3">
          <Field label="Total Duration">
            <NumericInput 
              className={inputCls} 
              value={form.duration?.value || ''} 
              onChange={e => set('duration.value', Number(e.target.value))} 
              placeholder="12"
              min="1"
              max="60"
            />
          </Field>
          <Field label="Duration Unit">
            <select 
              className={selectCls} 
              value={form.duration?.unit || 'MONTH'} 
              onChange={e => set('duration.unit', e.target.value)}
            >
              <option value="MONTH">Months</option>
              <option value="YEAR">Years</option>
            </select>
          </Field>
          <Field label="Min Stay (months)">
            <NumericInput 
              className={inputCls} 
              value={form.duration?.minimumStayMonths || ''} 
              onChange={e => set('duration.minimumStayMonths', Number(e.target.value))} 
              placeholder="3"
              min="1"
              max="12"
            />
          </Field>
        </div>

        {/* Payment Model */}
        <div className="grid grid-cols-3 gap-3">
          <Field label="Number of Installments">
            <NumericInput 
              className={inputCls} 
              value={form.paymentModel?.installments || ''} 
              onChange={e => set('paymentModel.installments', Number(e.target.value))} 
              placeholder="3"
              min="1"
              max="12"
            />
          </Field>
          <Field label="Payment Timing">
            <select 
              className={selectCls} 
              value={form.paymentModel?.paymentTiming || 'PREPAID'} 
              onChange={e => set('paymentModel.paymentTiming', e.target.value)}
            >
              <option value="PREPAID">Prepaid</option>
              <option value="POSTPAID">Postpaid</option>
            </select>
          </Field>
          <Field label="Due Day of Month">
            <NumericInput 
              className={inputCls} 
              value={form.paymentModel?.dueDayOfMonth || ''} 
              onChange={e => set('paymentModel.dueDayOfMonth', Number(e.target.value))} 
              placeholder="5"
              min="1"
              max="28"
            />
          </Field>
        </div>

        {/* Installment Preview */}
        {installmentPreview && (
          <div className="bg-green-50 border border-green-200 rounded-xl px-4 py-3">
            <p className="text-sm font-semibold text-green-900 mb-2">📋 Installment Distribution Preview</p>
            <div className="space-y-2">
              {installmentPreview.installmentGroups?.map((group, index) => (
                <div key={index} className="flex justify-between items-center text-sm">
                  <span className="text-green-800">
                    Installment {group.installmentNumber}: {group.description}
                  </span>
                  <span className="text-green-700 font-medium">
                    {group.monthCount} month{group.monthCount > 1 ? 's' : ''}
                  </span>
                </div>
              ))}
            </div>
            <p className="text-xs text-green-600 mt-2">
              Strategy: {installmentPreview.distributionStrategy} | 
              Total: {installmentPreview.totalMonths} months across {installmentPreview.numberOfInstallments} installments
            </p>
          </div>
        )}

        {loading && (
          <div className="bg-slate-50 border border-slate-200 rounded-xl px-4 py-3">
            <p className="text-sm text-slate-600">Calculating installment distribution...</p>
          </div>
        )}
      </div>
    </div>
  )
}