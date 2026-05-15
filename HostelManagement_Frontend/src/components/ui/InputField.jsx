import { cn } from '../../utils/cn'

function InputField({
  label,
  name,
  type = 'text',
  value,
  onChange,
  placeholder,
  required = false,
  disabled = false,
  error = '',
  hint = '',
  className = '',
  inputClassName = '',
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
      <input
        name={name}
        type={type}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        disabled={disabled}
        className={cn(
          'h-12 w-full rounded-2xl border bg-white px-4 text-sm text-slate-900 shadow-sm outline-none transition placeholder:text-slate-400 focus:border-sky-400 focus:ring-4 focus:ring-sky-100',
          error ? 'border-rose-300 focus:border-rose-400 focus:ring-rose-100' : 'border-slate-200',
          disabled && 'opacity-60 cursor-not-allowed bg-slate-50',
          inputClassName,
        )}
        {...props}
      />
      {error ? <p className="text-sm text-rose-600">{error}</p> : null}
      {!error && hint ? <p className="text-sm text-slate-500">{hint}</p> : null}
    </label>
  )
}

export { InputField }
export default InputField
