import { SparkIcon } from '../components/icons/AppIcons'

function AuthLayout({ title, subtitle, children, footer }) {
  return (
    <div className="h-screen overflow-hidden bg-[radial-gradient(circle_at_top_left,_rgba(14,165,233,0.14),_transparent_30%),radial-gradient(circle_at_bottom_right,_rgba(15,23,42,0.08),_transparent_35%),linear-gradient(180deg,#f8fafc_0%,#eef2ff_100%)]">
      <div className="mx-auto grid h-full max-w-7xl items-stretch gap-8 px-4 py-4 lg:grid-cols-[1.1fr_0.9fr] lg:px-8">
        <div className="hidden rounded-[2rem] border border-white/70 bg-slate-950 p-8 text-white shadow-[0_40px_120px_-60px_rgba(15,23,42,0.8)] lg:block">
          <div className="flex h-full flex-col justify-between">
            <div className="space-y-4">
              <div className="inline-flex items-center gap-3 rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm">
                <SparkIcon className="h-4 w-4" />
                <span>Modern hostel operations platform</span>
              </div>
              <div className="space-y-4">
                <h1 className="max-w-xl text-5xl font-bold leading-tight">
                  Bring your hostels, rooms, agreements, and onboarding into one polished workspace.
                </h1>
                <p className="max-w-lg text-base leading-7 text-slate-300">
                  Built for owners who want real operational clarity: cleaner setup flows, better visibility, and a product experience that feels ready for scale.
                </p>
              </div>
            </div>
            <div className="grid gap-4 sm:grid-cols-3">
              {[
                ['Fast setup', 'Create properties and assign inventory in minutes.'],
                ['Clear workflows', 'Guide teams through agreements and move-ins.'],
                ['Owner visibility', 'Track occupancy and growth from one dashboard.'],
              ].map(([heading, body]) => (
                <div key={heading} className="rounded-2xl border border-white/10 bg-white/5 p-4">
                  <p className="font-semibold">{heading}</p>
                  <p className="mt-2 text-sm leading-6 text-slate-300">{body}</p>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="mx-auto flex w-full max-w-xl items-start overflow-y-auto py-2">
          <div className="w-full rounded-[2rem] border border-white/80 bg-white/90 p-5 shadow-[0_30px_90px_-50px_rgba(15,23,42,0.45)] backdrop-blur">
            <div className="mb-4 space-y-1.5">
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-sky-600">Hostel Hub</p>
              <h2 className="text-3xl font-bold tracking-tight text-slate-950">{title}</h2>
              <p className="text-sm leading-6 text-slate-500">{subtitle}</p>
            </div>
            {children}
            {footer ? <div className="mt-2 border-t border-slate-100 pt-2">{footer}</div> : null}
          </div>
        </div>
      </div>
    </div>
  )
}

export default AuthLayout
