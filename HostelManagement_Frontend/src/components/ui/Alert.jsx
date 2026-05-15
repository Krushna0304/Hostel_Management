import { cn } from '../../utils/cn'

const toneStyles = {
  info: 'border-sky-200 bg-sky-50 text-sky-800',
  success: 'border-emerald-200 bg-emerald-50 text-emerald-800',
  warning: 'border-amber-200 bg-amber-50 text-amber-800',
  error: 'border-rose-200 bg-rose-50 text-rose-800',
}

function Alert({ type = 'info', tone, title, message, children, onClose, className = '' }) {
  // Support both 'type' and 'tone' props for compatibility
  const alertType = type || tone || 'info'
  
  return (
    <div className={cn('rounded-2xl border px-4 py-3 text-sm relative', toneStyles[alertType], className)}>
      {onClose && (
        <button
          onClick={onClose}
          className="absolute top-3 right-3 text-current opacity-50 hover:opacity-100 transition"
          aria-label="Close alert"
        >
          ×
        </button>
      )}
      {title && <p className="font-semibold">{title}</p>}
      <div className={title ? 'mt-1' : ''}>
        {message || children}
      </div>
    </div>
  )
}

export { Alert }
export default Alert
