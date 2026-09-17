import Card, { CardContent } from './Card'

function StatCard({ label, title, value, meta, icon }) {
  return (
    <Card>
      <CardContent className="flex items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="text-sm font-medium text-slate-500">{label || title}</p>
          <p className="mt-3 whitespace-nowrap text-2xl font-bold tracking-tight text-slate-950 sm:text-3xl">{value}</p>
          {meta ? <p className="mt-2 text-sm text-slate-500">{meta}</p> : null}
        </div>
        {icon ? <div className="rounded-2xl bg-slate-100 p-3 text-slate-700">{icon}</div> : null}
      </CardContent>
    </Card>
  )
}

export default StatCard
