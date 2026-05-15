import { Field, NumericInput } from '../ui'

const inputCls = "w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-100"
const selectCls = inputCls

export default function RentSection({ form, set }) {
  return (
    <div className="rounded-2xl border border-slate-200">
      <div className="px-4 py-3 bg-slate-50 rounded-t-2xl border-b border-slate-200">
        <h3 className="text-sm font-semibold text-slate-900">💰 Monthly Rent (Required)</h3>
        <p className="text-xs text-slate-500 mt-1">Base monthly rent amount</p>
      </div>
      <div className="px-4 py-4 space-y-3">
        <div className="grid grid-cols-2 gap-3">
          <Field label="Monthly Rent (₹) *">
            <NumericInput 
              className={inputCls} 
              value={form.rentDetails.monthlyRent} 
              onChange={e => set('rentDetails.monthlyRent', e.target.value)} 
              placeholder="8500"
              required
              min="0"
            />
          </Field>
          <Field label="Currency">
            <select 
              className={selectCls} 
              value={form.rentDetails.currency} 
              onChange={e => set('rentDetails.currency', e.target.value)}
            >
              <option value="INR">INR</option>
              {/* <option value="USD">USD</option>
              <option value="EUR">EUR</option> */}
            </select>
          </Field>
        </div>
      </div>
    </div>
  )
}