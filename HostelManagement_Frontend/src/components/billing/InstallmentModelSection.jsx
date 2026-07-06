import { Field, NumericInput } from '../ui'

const inputCls = "w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-100"
const selectCls = inputCls

export default function InstallmentModelSection({ form, set }) {
  const isNotFixed = form.duration?.durationType === 'NOT_FIXED'

  const handleDurationTypeChange = (e) => {
    const durationType = e.target.value
    set('duration.durationType', durationType)
    if (durationType === 'NOT_FIXED') {
      // For not-fixed: installments is always 1 (monthly), value is irrelevant
      set('paymentModel.installments', 1)
      set('duration.value', 0)
    } else {
      // Restore sensible defaults for fixed
      set('duration.value', 12)
      set('paymentModel.installments', 3)
    }
  }

  return (
    <div className="rounded-2xl border border-slate-200">
      <div className="px-4 py-3 bg-slate-50 rounded-t-2xl border-b border-slate-200">
        <h3 className="text-sm font-semibold text-slate-900">📊 Installment Model</h3>
        <p className="text-xs text-slate-500 mt-1">Configure flexible installment distribution</p>
      </div>
      <div className="px-4 py-4 space-y-3">

        {/* Duration Type */}
        <Field label="Duration Type">
          <select
            className={selectCls}
            value={form.duration?.durationType || 'FIXED'}
            onChange={handleDurationTypeChange}
          >
            <option value="FIXED">Fixed Duration</option>
            <option value="NOT_FIXED">Not Fixed Duration (Monthly rolling)</option>
          </select>
        </Field>

        {isNotFixed && (
          <div className="rounded-xl border border-sky-100 bg-sky-50 px-3 py-2 text-xs text-sky-700">
            Tenant pays month-to-month with no fixed end date. Installments covering the minimum stay are generated upfront; additional months continue rolling after that.
          </div>
        )}

        {/* Duration fields — hidden for NOT_FIXED since no total duration applies */}
        <div className={`grid gap-3 ${isNotFixed ? 'grid-cols-1' : 'grid-cols-3'}`}>
          {!isNotFixed && (
            <>
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
                </select>
              </Field>
            </>
          )}
          <Field label={isNotFixed ? 'Minimum Stay (months)' : 'Min Stay (months)'}>
            <NumericInput
              className={inputCls}
              value={form.duration?.minimumStayMonths || ''}
              onChange={e => set('duration.minimumStayMonths', Number(e.target.value))}
              placeholder="3"
              min="1"
              max="60"
            />
          </Field>
        </div>

        {/* Payment Model */}
        <div className={`grid gap-3 ${isNotFixed ? 'grid-cols-2' : 'grid-cols-3'}`}>
          {!isNotFixed && (
            <Field label="Number of Installments">
              <NumericInput
                className={inputCls}
                value={form.paymentModel?.installments || ''}
                onChange={e => set('paymentModel.installments', Number(e.target.value))}
                placeholder="3"
                min="1"
                max="60"
              />
            </Field>
          )}
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

        {isNotFixed && (
          <p className="text-xs text-slate-400">
            Monthly installments are generated automatically. Number of installments = 1 per month (fixed).
          </p>
        )}
      </div>
    </div>
  )
}
