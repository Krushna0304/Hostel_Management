import { useState } from 'react'
import { Field, NumericInput, Button } from '../ui'

const inputCls = "w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-100"

export default function RecurringChargesSection({ form, set }) {
  const [newRecurringCharge, setNewRecurringCharge] = useState({ name: '', amount: '' })

  const addRecurringCharge = () => {
    if (!newRecurringCharge.name.trim() || !newRecurringCharge.amount) return
    
    const charges = form.monthlyRecurringCharges || []
    const updatedCharges = [...charges, {
      chargeName: newRecurringCharge.name.trim(),
      amount: Number(newRecurringCharge.amount),
      timing: 'IN_INSTALLMENTS',
      category: 'RECURRING_CHARGE',
      refundable: false,
      applicable: true
    }]
    
    set('monthlyRecurringCharges', updatedCharges)
    setNewRecurringCharge({ name: '', amount: '' })
  }

  const removeRecurringCharge = (index) => {
    const charges = form.monthlyRecurringCharges || []
    const updatedCharges = charges.filter((_, i) => i !== index)
    set('monthlyRecurringCharges', updatedCharges)
  }

  return (
    <div className="rounded-2xl border border-slate-200">
      <div className="px-4 py-3 bg-slate-50 rounded-t-2xl border-b border-slate-200">
        <h3 className="text-sm font-semibold text-slate-900">🔄 Monthly Recurring Charges</h3>
        <p className="text-xs text-slate-500 mt-1">Charges included in monthly installments</p>
      </div>
      <div className="px-4 py-4 space-y-3">
        <div className="grid grid-cols-2 gap-3">
          <Field label="Monthly Cleaning (₹)">
            <NumericInput 
              className={inputCls} 
              value={form.charges?.cleaningCharges?.monthlyCleaningCharge?.amount || ''} 
              onChange={e => set('charges.cleaningCharges.monthlyCleaningCharge.amount', e.target.value)} 
              placeholder="500" 
              min="0"
            />
          </Field>
          <Field label="Monthly Maintenance (₹)">
            <NumericInput 
              className={inputCls} 
              value={form.charges?.maintenanceCharges?.monthlyMaintenanceCharge?.amount || ''} 
              onChange={e => set('charges.maintenanceCharges.monthlyMaintenanceCharge.amount', e.target.value)} 
              placeholder="300" 
              min="0"
            />
          </Field>
          <Field label="Fixed Electricity (₹)">
            <NumericInput 
              className={inputCls} 
              value={form.charges?.utilityCharges?.electricity?.fixedAmount || ''} 
              onChange={e => set('charges.utilityCharges.electricity.fixedAmount', e.target.value)} 
              placeholder="800" 
              min="0"
            />
          </Field>
          <Field label="Water Charges (₹)">
            <NumericInput 
              className={inputCls} 
              value={form.charges?.utilityCharges?.water?.monthlyAmount || ''} 
              onChange={e => set('charges.utilityCharges.water.monthlyAmount', e.target.value)} 
              placeholder="300" 
              min="0"
            />
          </Field>
        </div>

        {/* Add Custom Monthly Recurring Charges */}
        <div className="border-t border-slate-100 pt-3">
          <p className="text-xs font-semibold text-slate-600 mb-2">Add Custom Monthly Recurring Charges</p>
          <div className="grid grid-cols-3 gap-2">
            <input 
              className={inputCls} 
              value={newRecurringCharge.name} 
              onChange={e => setNewRecurringCharge(prev => ({ ...prev, name: e.target.value }))} 
              placeholder="Charge name (e.g. WiFi, Laundry)" 
            />
            <NumericInput 
              className={inputCls} 
              value={newRecurringCharge.amount} 
              onChange={e => setNewRecurringCharge(prev => ({ ...prev, amount: e.target.value }))} 
              placeholder="Amount" 
              min="0"
            />
            <Button 
              type="button" 
              onClick={addRecurringCharge} 
              size="sm"
              label="Add"
            />
          </div>
        </div>

        {/* Display Added Recurring Charges */}
        {form.monthlyRecurringCharges && form.monthlyRecurringCharges.length > 0 && (
          <div className="space-y-2">
            <p className="text-xs font-semibold text-slate-600">Added Monthly Recurring Charges:</p>
            {form.monthlyRecurringCharges.map((charge, index) => (
              <div key={index} className="flex items-center justify-between bg-green-50 border border-green-200 rounded-xl px-3 py-2">
                <div className="text-sm">
                  <span className="font-medium text-green-900">{charge.chargeName}:</span>
                  <span className="text-green-800 ml-1">₹{charge.amount}/month</span>
                  <span className="text-green-600 ml-2 text-xs">(Non-refundable)</span>
                </div>
                <button 
                  type="button" 
                  onClick={() => removeRecurringCharge(index)} 
                  className="text-green-400 hover:text-red-500 ml-2"
                >
                  ✕
                </button>
              </div>
            ))}
          </div>
        )}
        
        <div className="bg-blue-50 border border-blue-200 rounded-xl px-3 py-2">
          <p className="text-xs text-blue-700">
            <strong>Note:</strong> These charges are added to the base rent and included in monthly installments. 
            They are non-refundable and considered as recurring revenue.
          </p>
        </div>
      </div>
    </div>
  )
}