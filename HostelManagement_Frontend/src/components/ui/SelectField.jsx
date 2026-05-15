import { cn } from '../../utils/cn'

function SelectField({
  label,
  name,
  value,
  onChange,
  options = [],
  placeholder,
  required = false,
  error = '',
  hint = '',
  className = '',
  ...props
}) {
  return (
    <label className={cn('block space-y-2', className)}>
      {label ? (
        <span className="flex items-center gap-1 text-sm font-medium text-slate-700">
          {label}
          {required ? <span className="text-rose-500">*</span> : null}
        </span>
      ) : null}
      <select
        name={name}
        value={value}
        onChange={onChange}
        className={cn(
          'h-12 w-full rounded-2xl border bg-white px-4 text-sm text-slate-900 shadow-sm outline-none transition focus:border-sky-400 focus:ring-4 focus:ring-sky-100',
          error ? 'border-rose-300 focus:border-rose-400 focus:ring-rose-100' : 'border-slate-200',
        )}
        {...props}
      >
        <option value="">{placeholder || 'Select an option'}</option>
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {error ? <p className="text-sm text-rose-600">{error}</p> : null}
      {!error && hint ? <p className="text-sm text-slate-500">{hint}</p> : null}
    </label>
  )
}

export default SelectField
