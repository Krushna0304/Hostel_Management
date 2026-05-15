import Card, { CardContent } from './Card'

function StatCard({ label, value, meta, icon }) {
  return (
    <Card>
      <CardContent className="flex items-start justify-between gap-4">
        <div>
          <p className="text-sm font-medium text-slate-500">{label}</p>
          <p className="mt-3 text-3xl font-bold tracking-tight text-slate-950">{value}</p>
          {meta ? <p className="mt-2 text-sm text-slate-500">{meta}</p> : null}
        </div>
        {icon ? <div className="rounded-2xl bg-slate-100 p-3 text-slate-700">{icon}</div> : null}
      </CardContent>
    </Card>
  )
}

export default StatCard
