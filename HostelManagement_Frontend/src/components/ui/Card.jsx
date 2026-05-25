import { cn } from '../../utils/cn'

function Card({ className = '', children, onClick, ...props }) {
  return (
    <div 
      className={cn('rounded-3xl border border-white/70 bg-white/95 shadow-[0_24px_80px_-48px_rgba(15,23,42,0.45)] backdrop-blur', className)}
      onClick={onClick}
      style={{ pointerEvents: onClick ? 'auto' : 'auto' }}
      {...props}
    >
      {children}
    </div>
  )
}

export function CardHeader({ title, description, action, className = '' }) {
  return (
    <div className={cn('flex flex-col gap-3 border-b border-slate-100 px-6 py-5 sm:flex-row sm:items-start sm:justify-between', className)}>
      <div className="space-y-1">
        <h2 className="text-lg font-semibold text-slate-950">{title}</h2>
        {description ? <p className="text-sm text-slate-500">{description}</p> : null}
      </div>
      {action}
    </div>
  )
}

export function CardContent({ className = '', children }) {
  return <div className={cn('px-6 py-5', className)}>{children}</div>
}

export { Card }
export default Card
