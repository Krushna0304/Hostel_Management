import { cn } from '../../utils/cn'

const variants = {
  neutral: 'bg-slate-100 text-slate-700',
  secondary: 'bg-gray-100 text-gray-700',
  success: 'bg-emerald-100 text-emerald-700',
  warning: 'bg-amber-100 text-amber-700',
  danger: 'bg-rose-100 text-rose-700',
  info: 'bg-sky-100 text-sky-700',
}

export function Badge({ children, variant = 'neutral', className = '' }) {
  return (
    <span className={cn('inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold', variants[variant], className)}>
      {children}
    </span>
  )
}

export default Badge
