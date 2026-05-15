function PageHeader({ eyebrow, title, subtitle, description, action, secondaryAction }) {
  return (
    <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
      <div className="space-y-2">
        {eyebrow ? <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-600">{eyebrow}</p> : null}
        <div className="space-y-1">
          <h1 className="text-3xl font-bold tracking-tight text-slate-950">{title}</h1>
          {(subtitle || description) ? <p className="max-w-2xl text-sm leading-6 text-slate-500">{subtitle || description}</p> : null}
        </div>
      </div>
      <div className="flex flex-col gap-3 sm:flex-row">
        {secondaryAction}
        {action}
      </div>
    </div>
  )
}

export { PageHeader }
export default PageHeader
