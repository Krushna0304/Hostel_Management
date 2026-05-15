import { SpinnerIcon } from '../icons/AppIcons'
import { cn } from '../../utils/cn'

const variants = {
  primary: 'bg-slate-950 text-white shadow-lg shadow-slate-950/15 hover:bg-slate-800',
  secondary: 'bg-white text-slate-700 ring-1 ring-slate-200 hover:bg-slate-50',
  ghost: 'bg-transparent text-slate-600 hover:bg-slate-100 hover:text-slate-900',
  danger: 'bg-rose-600 text-white shadow-lg shadow-rose-500/20 hover:bg-rose-500',
  success: 'bg-emerald-600 text-white shadow-lg shadow-emerald-500/20 hover:bg-emerald-500',
}

const sizes = {
  sm: 'h-9 px-3 text-sm',
  md: 'h-11 px-4 text-sm',
  lg: 'h-12 px-5 text-sm',
}

function Button({
  type = 'button',
  label,
  children,
  onClick,
  disabled = false,
  loading = false,
  variant = 'primary',
  size = 'md',
  className = '',
  fullWidth = false,
  icon,
}) {
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled || loading}
      className={cn(
        'inline-flex items-center justify-center gap-2 rounded-2xl font-semibold transition duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500/50 disabled:cursor-not-allowed disabled:opacity-60',
        variants[variant],
        sizes[size],
        fullWidth && 'w-full',
        className,
      )}
    >
      {loading ? <SpinnerIcon className="h-4 w-4 animate-spin" /> : icon}
      <span>{label || children}</span>
    </button>
  )
}

export { Button }
export default Button
