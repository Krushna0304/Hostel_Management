import { useState } from 'react'
import { Field, Button, NumericInput } from '../ui'

const inputCls = "w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-100"
const selectCls = inputCls

export default function OtherChargesSection({ form, set }) {
  const [newCharge, setNewCharge] = useState({ 
    name: '', 
    amount: '', 
    description: '',
    installmentEnabled: false,
    installmentCount: 2
  })

  const addOtherCharge = () => {
    if (!newCharge.name.trim() || !newCharge.amount) return
    
    const charges = form.otherCharges || []
    const updatedCharges = [...charges, {
      chargeName: newCharge.name.trim(),
      description: newCharge.description.trim(),
      amount: Number(newCharge.amount),
      installmentEnabled: newCharge.installmentEnabled,
      installmentCount: newCharge.installmentEnabled ? newCharge.installmentCount : null,
      timing: 'ON_DEMAND',
      category: 'OTHER_CHARGE',
      applicable: true
    }]
    
    set('otherCharges', updatedCharges)
    setNewCharge({ 
      name: '', 
      amount: '', 
      description: '',
      installmentEnabled: false,
      installmentCount: 2
    })
  }

  const removeOtherCharge = (index) => {
    const charges = form.otherCharges || []
    const updatedCharges = charges.filter((_, i) => i !== index)
    set('otherCharges', updatedCharges)
  }

  const updateCharge = (index, field, value) => {
    const charges = form.otherCharges || []
    const updatedCharges = [...charges]
    updatedCharges[index] = { ...updatedCharges[index], [field]: value }
    set('otherCharges', updatedCharges)
  }

  return (
    <div className="rounded-2xl border border-slate-200">
      <div className="px-4 py-3 bg-slate-50 rounded-t-2xl border-b border-slate-200">
        <h3 className="text-sm font-semibold text-slate-900">⚡ Other Charges</h3>
        <p className="text-xs text-slate-500 mt-1">Additional charges that can be applied to tenants or rooms</p>
      </div>
      
      <div className="px-4 py-4 space-y-4">
        {/* Existing Other Charges */}
        {form.otherCharges && form.otherCharges.length > 0 && (
          <div className="space-y-3">
            <h4 className="text-sm font-medium text-slate-700">Configured Other Charges</h4>
            {form.otherCharges.map((charge, index) => (
              <div key={index} className="p-3 bg-slate-50 rounded-lg border border-slate-200">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-2">
                      <span className="font-medium text-slate-900">{charge.chargeName}</span>
                      <span className="text-sm text-slate-600">₹{charge.amount.toLocaleString()}</span>
                      {charge.installmentEnabled && (
                        <span className="px-2 py-1 bg-blue-100 text-blue-700 text-xs rounded-full">
                          {charge.installmentCount} installments
                        </span>
                      )}
                    </div>
                    {charge.description && (
                      <p className="text-xs text-slate-600 mb-2">{charge.description}</p>
                    )}
                    
                    {/* Installment Settings */}
                    <div className="grid grid-cols-2 gap-2 mt-2">
                      <Field label="Enable Installments">
                        <select 
                          className={selectCls}
                          value={charge.installmentEnabled}
                          onChange={e => updateCharge(index, 'installmentEnabled', e.target.value === 'true')}
                        >
                          <option value={false}>No</option>
                          <option value={true}>Yes</option>
                        </select>
                      </Field>
                      
                      {charge.installmentEnabled && (
                        <Field label="Installment Count">
                          <select 
                            className={selectCls}
                            value={charge.installmentCount || 2}
                            onChange={e => updateCharge(index, 'installmentCount', Number(e.target.value))}
                          >
                            {[2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map(count => (
                              <option key={count} value={count}>{count} months</option>
                            ))}
                          </select>
                        </Field>
                      )}
                    </div>
                  </div>
                  
                  <Button 
                    variant="ghost" 
                    size="sm" 
                    onClick={() => removeOtherCharge(index)}
                    className="text-red-600 hover:text-red-700 hover:bg-red-50"
                  >
                    Remove
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Add New Other Charge */}
        <div className="border-t border-slate-200 pt-4">
          <h4 className="text-sm font-medium text-slate-700 mb-3">Add New Other Charge</h4>
          
          <div className="grid grid-cols-2 gap-3 mb-3">
            <Field label="Charge Name">
              <input 
                className={inputCls}
                value={newCharge.name}
                onChange={e => setNewCharge(prev => ({ ...prev, name: e.target.value }))}
                placeholder="e.g., Electricity Bill, Maintenance Fee"
              />
            </Field>
            
            <Field label="Amount (₹)">
              <NumericInput 
                className={inputCls}
                value={newCharge.amount}
                onChange={e => setNewCharge(prev => ({ ...prev, amount: e.target.value }))}
                placeholder="1500"
                min="0"
              />
            </Field>
          </div>

          <div className="mb-3">
            <Field label="Description (Optional)">
              <input 
                className={inputCls}
                value={newCharge.description}
                onChange={e => setNewCharge(prev => ({ ...prev, description: e.target.value }))}
                placeholder="Brief description of the charge"
              />
            </Field>
          </div>

          <div className="grid grid-cols-2 gap-3 mb-3">
            <Field label="Enable Installments">
              <select 
                className={selectCls}
                value={newCharge.installmentEnabled}
                onChange={e => setNewCharge(prev => ({ 
                  ...prev, 
                  installmentEnabled: e.target.value === 'true' 
                }))}
              >
                <option value={false}>No - Full payment only</option>
                <option value={true}>Yes - Allow installments</option>
              </select>
            </Field>
            
            {newCharge.installmentEnabled && (
              <Field label="Installment Count">
                <select 
                  className={selectCls}
                  value={newCharge.installmentCount}
                  onChange={e => setNewCharge(prev => ({ 
                    ...prev, 
                    installmentCount: Number(e.target.value) 
                  }))}
                >
                  {[2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map(count => (
                    <option key={count} value={count}>{count} months</option>
                  ))}
                </select>
              </Field>
            )}
          </div>

          <Button 
            onClick={addOtherCharge}
            disabled={!newCharge.name.trim() || !newCharge.amount}
            className="w-full"
          >
            Add Other Charge
          </Button>
        </div>

        {/* Information Box */}
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
          <div className="flex items-start gap-2">
            <div className="text-blue-600 mt-0.5">ℹ️</div>
            <div className="text-xs text-blue-800">
              <p className="font-medium mb-1">About Other Charges:</p>
              <ul className="space-y-1 text-blue-700">
                <li>• These charges can be applied to specific tenants or entire rooms</li>
                <li>• Room charges are automatically split among current tenants</li>
                <li>• Installment payments allow tenants to pay in monthly installments</li>
                <li>• Owners can collect payments through the collection dashboard</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}