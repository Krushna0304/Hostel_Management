import { useState } from 'react'
import { Field, Button, NumericInput } from '../ui'

const inputCls = "w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-100"

export default function OneTimeChargesSection({ form, set }) {
  const [newRefundable, setNewRefundable] = useState({ name: '', amount: '' })
  const [newNonRefundable, setNewNonRefundable] = useState({ name: '', amount: '' })

  const addCharge = (charge, refundable) => {
    if (!charge.name.trim() || !charge.amount) return
    const charges = form.oneTimeCharges || []
    set('oneTimeCharges', [...charges, {
      chargeName: charge.name.trim(),
      amount: Number(charge.amount),
      refundable,
      timing: 'AT_AGREEMENT',
      category: 'ONE_TIME_CHARGE',
      applicable: true,
    }])
  }

  const removeOneTimeCharge = (index) => {
    const charges = form.oneTimeCharges || []
    set('oneTimeCharges', charges.filter((_, i) => i !== index))
  }

  const refundableCharges = (form.oneTimeCharges || [])
    .map((c, index) => ({ ...c, index }))
    .filter(c => c.refundable)
  const nonRefundableCharges = (form.oneTimeCharges || [])
    .map((c, index) => ({ ...c, index }))
    .filter(c => !c.refundable)

  return (
    <div className="rounded-2xl border border-slate-200">
      <div className="px-4 py-3 bg-slate-50 rounded-t-2xl border-b border-slate-200">
        <h3 className="text-sm font-semibold text-slate-900">🏷️ One-Time Charges</h3>
        <p className="text-xs text-slate-500 mt-1">Registration, admission, setup fees collected at agreement acceptance</p>
      </div>
      <div className="px-4 py-4 grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* ── Refundable Charges ── */}
        <div className="rounded-xl border border-slate-200 p-3 space-y-3">
          <h4 className="text-sm font-semibold text-emerald-700">♻️ Refundable Charges</h4>

          <Field label="Security Deposit (₹)">
            <NumericInput
              className={inputCls}
              value={form.charges?.securityDeposit?.amount || ''}
              onChange={e => set('charges.securityDeposit.amount', e.target.value)}
              placeholder="17000"
              min="0"
            />
          </Field>

          <div className="border-t border-slate-100 pt-3 space-y-2">
            <p className="text-xs font-semibold text-slate-600">Add Refundable Charge</p>
            <input
              className={inputCls}
              value={newRefundable.name}
              onChange={e => setNewRefundable(prev => ({ ...prev, name: e.target.value }))}
              placeholder="Charge name"
            />
            <NumericInput
              className={inputCls}
              value={newRefundable.amount}
              onChange={e => setNewRefundable(prev => ({ ...prev, amount: e.target.value }))}
              placeholder="Amount"
              min="0"
            />
            <Button
              type="button"
              size="sm"
              label="Add"
              fullWidth
              onClick={() => { addCharge(newRefundable, true); setNewRefundable({ name: '', amount: '' }) }}
            />
          </div>

          {refundableCharges.length > 0 && (
            <div className="space-y-2">
              {refundableCharges.map(charge => (
                <div key={charge.index} className="flex items-center justify-between bg-emerald-50 border border-emerald-200 rounded-xl px-3 py-2">
                  <div className="text-sm">
                    <span className="font-medium text-emerald-900">{charge.chargeName}:</span>
                    <span className="text-emerald-800 ml-1">₹{charge.amount}</span>
                  </div>
                  <button type="button" onClick={() => removeOneTimeCharge(charge.index)} className="text-emerald-400 hover:text-red-500 ml-2">✕</button>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* ── Non-Refundable Charges ── */}
        <div className="rounded-xl border border-slate-200 p-3 space-y-3">
          <h4 className="text-sm font-semibold text-rose-700">🚫 Non-Refundable Charges</h4>

          <Field label="One-time Maintenance (₹)">
            <NumericInput
              className={inputCls}
              value={form.charges?.maintenanceCharges?.oneTimeMaintenanceCharge?.amount || ''}
              onChange={e => set('charges.maintenanceCharges.oneTimeMaintenanceCharge.amount', e.target.value)}
              placeholder="2000"
              min="0"
            />
          </Field>

          <div className="border-t border-slate-100 pt-3 space-y-2">
            <p className="text-xs font-semibold text-slate-600">Add Non-Refundable Charge</p>
            <input
              className={inputCls}
              value={newNonRefundable.name}
              onChange={e => setNewNonRefundable(prev => ({ ...prev, name: e.target.value }))}
              placeholder="Charge name"
            />
            <NumericInput
              className={inputCls}
              value={newNonRefundable.amount}
              onChange={e => setNewNonRefundable(prev => ({ ...prev, amount: e.target.value }))}
              placeholder="Amount"
              min="0"
            />
            <Button
              type="button"
              size="sm"
              label="Add"
              fullWidth
              onClick={() => { addCharge(newNonRefundable, false); setNewNonRefundable({ name: '', amount: '' }) }}
            />
          </div>

          {nonRefundableCharges.length > 0 && (
            <div className="space-y-2">
              {nonRefundableCharges.map(charge => (
                <div key={charge.index} className="flex items-center justify-between bg-rose-50 border border-rose-200 rounded-xl px-3 py-2">
                  <div className="text-sm">
                    <span className="font-medium text-rose-900">{charge.chargeName}:</span>
                    <span className="text-rose-800 ml-1">₹{charge.amount}</span>
                  </div>
                  <button type="button" onClick={() => removeOneTimeCharge(charge.index)} className="text-rose-400 hover:text-red-500 ml-2">✕</button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
