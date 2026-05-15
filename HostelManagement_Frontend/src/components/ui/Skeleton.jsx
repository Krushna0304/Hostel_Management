import { cn } from '../../utils/cn'

function Skeleton({ className = '' }) {
  return <div className={cn('animate-pulse rounded-2xl bg-slate-200/80', className)} />
}

export default Skeleton
