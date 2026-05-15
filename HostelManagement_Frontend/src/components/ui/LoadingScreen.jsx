function LoadingScreen({ title = 'Loading workspace...' }) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-[radial-gradient(circle_at_top,_rgba(14,165,233,0.12),_transparent_35%),linear-gradient(180deg,#f8fafc_0%,#eef2ff_100%)] px-4">
      <div className="rounded-3xl border border-white/80 bg-white/90 px-8 py-6 shadow-xl backdrop-blur">
        <div className="flex items-center gap-3">
          <div className="h-3 w-3 animate-pulse rounded-full bg-sky-500" />
          <p className="text-sm font-medium text-slate-700">{title}</p>
        </div>
      </div>
    </div>
  )
}

export default LoadingScreen
