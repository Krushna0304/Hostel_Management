import Button from './Button'

function EmptyState({ title, description, actionLabel, onAction, icon }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-3xl border border-dashed border-slate-200 bg-slate-50/80 px-6 py-12 text-center">
      {icon ? <div className="mb-4 rounded-2xl bg-white p-3 text-slate-700 shadow-sm">{icon}</div> : null}
      <h3 className="text-lg font-semibold text-slate-900">{title}</h3>
      <p className="mt-2 max-w-md text-sm leading-6 text-slate-500">{description}</p>
      {actionLabel && onAction ? (
        <Button className="mt-6" label={actionLabel} onClick={onAction} />
      ) : null}
    </div>
  )
}

export default EmptyState
