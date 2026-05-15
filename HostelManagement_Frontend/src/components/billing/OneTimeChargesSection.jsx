import { useState } from 'react'
import { Field, Button, NumericInput } from '../ui'

const inputCls = "w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-100"
const selectCls = inputCls

export default function OneTimeChargesSection({ form, set }) {
  const [newCharge, setNewCharge] = useState({ name: '', amount: '', refundable: false })

  const handleRefundableChange = (path, value) => {
    set(path, value === 'true')
  }

  const addOneTimeCharge = () => {
    if (!newCharge.name.trim() || !newCharge.amount) return
    
    const charges = form.oneTimeCharges || []
    const updatedCharges = [...charges, {
      chargeName: newCharge.name.trim(),
      amount: Number(newCharge.amount),
      refundable: newCharge.refundable,
      timing: 'AT_AGREEMENT',
      category: 'ONE_TIME_CHARGE',
      applicable: true
    }]
    
    set('oneTimeCharges', updatedCharges)
    setNewCharge({ name: '', amount: '', refundable: false })
  }

  const removeOneTimeCharge = (index) => {
    const charges = form.oneTimeCharges || []
    const updatedCharges = charges.filter((_, i) => i !== index)
    set('oneTimeCharges', updatedCharges)
  }

  return (
    <div className="rounded-2xl border border-slate-200">
      <div className="px-4 py-3 bg-slate-50 rounded-t-2xl border-b border-slate-200">
        <h3 className="text-sm font-semibold text-slate-900">🏷️ One-Time Charges</h3>
        <p className="text-xs text-slate-500 mt-1">Registration, admission, setup fees collected at agreement acceptance</p>
      </div>
      <div className="px-4 py-4 space-y-3">
        {/* Security Deposit */}
        <div className="grid grid-cols-2 gap-3">
          <Field label="Security Deposit (₹)">
            <NumericInput 
              className={inputCls} 
              value={form.charges?.securityDeposit?.amount || ''} 
              onChange={e => set('charges.securityDeposit.amount', e.target.value)} 
              placeholder="17000" 
              min="0"
            />
          </Field>
          <Field label="Deposit Refundable">
            <select 
              className={selectCls} 
              value={String(form.charges?.securityDeposit?.refundable ?? true)} 
              onChange={e => handleRefundableChange('charges.securityDeposit.refundable', e.target.value)}
            >
              <option value="true">Yes</option>
              {/* <option value="false">No</option> */}
            </select>
          </Field>
        </div>

        {/* One-time Maintenance */}
        <div className="grid grid-cols-2 gap-3">
          <Field label="One-time Maintenance (₹)">
            <NumericInput 
              className={inputCls} 
              value={form.charges?.maintenanceCharges?.oneTimeMaintenanceCharge?.amount || ''} 
              onChange={e => set('charges.maintenanceCharges.oneTimeMaintenanceCharge.amount', e.target.value)} 
              placeholder="2000" 
              min="0"
            />
          </Field>
          <Field label="Maintenance Refundable">
            <select 
              className={selectCls} 
              value={String(form.charges?.maintenanceCharges?.oneTimeMaintenanceCharge?.refundable ?? false)} 
              onChange={e => handleRefundableChange('charges.maintenanceCharges.oneTimeMaintenanceCharge.refundable', e.target.value)}
            >
              <option value="false">No</option>
              {/* <option value="true">Yes</option> */}
            </select>
          </Field>
        </div>

        {/* Add Custom One-Time Charges */}
        <div className="border-t border-slate-100 pt-3">
          <p className="text-xs font-semibold text-slate-600 mb-2">Add Custom One-Time Charges</p>
          <div className="grid grid-cols-4 gap-2">
            <input 
              className={inputCls} 
              value={newCharge.name} 
              onChange={e => setNewCharge(prev => ({ ...prev, name: e.target.value }))} 
              placeholder="Charge name" 
            />
            <NumericInput 
              className={inputCls} 
              value={newCharge.amount} 
              onChange={e => setNewCharge(prev => ({ ...prev, amount: e.target.value }))} 
              placeholder="Amount" 
              min="0"
            />
            <select 
              className={selectCls} 
              value={newCharge.refundable} 
              onChange={e => setNewCharge(prev => ({ ...prev, refundable: e.target.value === 'true' }))}
            >
              <option value="false">Non-refundable</option>
              {/* <option value="true">Refundable</option> */}
            </select>
            <Button 
              type="button" 
              onClick={addOneTimeCharge} 
              size="sm"
              label="Add"
            />
          </div>
        </div>

        {/* Display Added Charges */}
        {form.oneTimeCharges && form.oneTimeCharges.length > 0 && (
          <div className="space-y-2">
            <p className="text-xs font-semibold text-slate-600">Added One-Time Charges:</p>
            {form.oneTimeCharges.map((charge, index) => (
              <div key={index} className="flex items-center justify-between bg-blue-50 border border-blue-200 rounded-xl px-3 py-2">
                <div className="text-sm">
                  <span className="font-medium text-blue-900">{charge.chargeName}:</span>
                  <span className="text-blue-800 ml-1">₹{charge.amount}</span>
                  <span className="text-blue-600 ml-2 text-xs">
                    ({charge.refundable ? 'Refundable' : 'Non-refundable'})
                  </span>
                </div>
                <button 
                  type="button" 
                  onClick={() => removeOneTimeCharge(index)} 
                  className="text-blue-400 hover:text-red-500 ml-2"
                >
                  ✕
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}