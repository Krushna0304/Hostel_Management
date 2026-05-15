import { useMemo, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import authService from '../services/authService'
import { ownerNavigation, tenantNavigation } from '../constants/navigation'
import { useAuthState } from '../hooks/useAuthState'
import {
  ArrowLeftIcon,
  BellIcon,
  BuildingIcon,
  ClipboardIcon,
  CloseIcon,
  CreditCardIcon,
  DocumentIcon,
  HistoryIcon,
  HomeIcon,
  LogOutIcon,
  MenuIcon,
  ProfileIcon,
  ReceiptIcon,
  SparkIcon,
  UsersIcon,
} from '../components/icons/AppIcons'
import { cn } from '../utils/cn'

const iconMap = {
  home: HomeIcon,
  building: BuildingIcon,
  clipboard: ClipboardIcon,
  users: UsersIcon,
  document: DocumentIcon,
  'credit-card': CreditCardIcon,
  receipt: ReceiptIcon,
  profile: ProfileIcon,
  history: HistoryIcon,
  bell: BellIcon,
}

const titleMap = {
  '/owner/dashboard': {
    title: 'Operations Dashboard',
    description: 'Track hostels, occupancy structure, and agreement activity from one place.',
  },
  '/owner/create-hostel': {
    title: 'Create Hostel',
    description: 'Set up a new property with a clear, structured intake flow.',
  },
  '/owner/agreements': {
    title: 'Agreements',
    description: 'Create and monitor tenant onboarding agreements with better visibility.',
  },
  '/owner/collections': {
    title: 'Rent Collections',
    description: 'Track rent collection, overdue payments, and tenant payment status.',
  },
  '/owner/other-charges': {
    title: 'Other Charges',
    description: 'Manage additional charges and billing for tenants.',
  },
  '/owner/plans': {
    title: 'Tenant Plans',
    description: 'Create and manage custom rent plans visible only to you.',
  },
  '/owner/payment-settings': {
    title: 'Payment Settings',
    description: 'Configure your Razorpay account to accept online payments.',
  },
  '/owner/profile': {
    title: 'My Profile',
    description: 'Update your display name, phone number, or change your password.',
  },
  '/owner/payment-history': {
    title: 'Payment History',
    description: 'All transactions received and sent across your properties.',
  },
  '/owner/reminder-settings': {
    title: 'Reminder Settings',
    description: 'Control automated SMS reminders sent to tenants for dues and new charges.',
  },
  '/tenant-portal/dashboard': {
    title: 'My Dashboard',
    description: 'Your payment schedule, upcoming dues, and payment history.',
  },
  '/tenant-portal/other-charges': {
    title: 'My Other Charges',
    description: 'View and pay additional charges assigned to you.',
  },
  '/tenant-portal/profile': {
    title: 'My Profile',
    description: 'Update your display name, phone number, or change your password.',
  },
  '/tenant-portal/payment-history': {
    title: 'Payment History',
    description: 'All your rent payments and transactions in one place.',
  },
}

function DashboardLayout() {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false)
  const location = useLocation()
  const { userRole } = useAuthState()

  const navigation = userRole === 'TENANT' ? tenantNavigation : ownerNavigation
  const workspaceLabel = userRole === 'TENANT' ? 'Tenant Portal' : 'Owner Control Center'
  const roleLabel = userRole ?? 'USER'

  const headerCopy = useMemo(() => {
    const entry = Object.entries(titleMap).find(([path]) => location.pathname.startsWith(path))
    return entry?.[1] || {
      title: 'Workspace',
      description: 'Manage your hostel operations.',
    }
  }, [location.pathname])

  const handleLogout = () => {
    authService.logout()
    window.location.href = '/login'
  }

  return (
    <>
      {/* Root: locked to viewport — nothing overflows the document body */}
      <div className="flex h-screen overflow-hidden bg-[radial-gradient(circle_at_top,_rgba(14,165,233,0.10),_transparent_28%),linear-gradient(180deg,#f8fafc_0%,#f1f5f9_100%)] text-slate-900">

        {/* ── Sidebar ── fixed height, independent scroll for nav items ── */}
        <aside
          className={cn(
            'fixed inset-y-0 left-0 z-40 flex h-screen w-80 flex-col border-r border-white/70 bg-slate-950/95 text-slate-100 shadow-2xl backdrop-blur transition-transform duration-300 lg:static lg:translate-x-0',
            isSidebarOpen ? 'translate-x-0' : '-translate-x-full',
          )}
        >
          {/* Top: logo + workspace card — never scrolls */}
          <div className="shrink-0 p-6">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="rounded-2xl bg-white/10 p-2">
                  <SparkIcon className="h-5 w-5" />
                </div>
                <div>
                  <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Hostel Hub</p>
                  <p className="text-sm font-semibold">{workspaceLabel}</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setIsSidebarOpen(false)}
                className="rounded-xl p-2 text-slate-400 transition hover:bg-white/5 hover:text-white lg:hidden"
                aria-label="Close navigation"
              >
                <CloseIcon />
              </button>
            </div>

            <div className="mt-6 rounded-3xl border border-white/10 bg-white/5 p-4">
              <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Workspace</p>
              <p className="mt-2 text-lg font-semibold text-white">Production-style property operations</p>
              <p className="mt-2 text-sm leading-6 text-slate-300">
                Move between property setup, inventory, and agreement creation without losing context.
              </p>
            </div>
          </div>

          {/* Middle: nav links — scrolls independently */}
          <nav className="sidebar-scroll flex-1 overflow-y-auto px-6 pb-2">
            <div className="space-y-2">
              {navigation.map((item) => {
                const Icon = iconMap[item.icon]
                return (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    onClick={() => setIsSidebarOpen(false)}
                    className={({ isActive }) =>
                      cn(
                        'group flex items-center gap-3 rounded-2xl px-4 py-3 transition',
                        isActive
                          ? 'bg-white text-slate-950 shadow-lg'
                          : 'text-slate-300 hover:bg-white/5 hover:text-white',
                      )
                    }
                  >
                    {({ isActive }) => (
                      <>
                        <div className={cn('rounded-xl p-2', isActive ? 'bg-slate-100 text-slate-900' : 'bg-white/5 text-slate-300')}>
                          <Icon className="h-4 w-4" />
                        </div>
                        <div>
                          <p className="text-sm font-semibold">{item.label}</p>
                          <p className={cn('text-xs', isActive ? 'text-slate-500' : 'text-slate-400')}>{item.description}</p>
                        </div>
                      </>
                    )}
                  </NavLink>
                )
              })}
            </div>
          </nav>

          {/* Bottom: sign out — always pinned */}
          <div className="shrink-0 p-6 pt-2">
            <button
              type="button"
              onClick={handleLogout}
              className="flex w-full items-center gap-3 rounded-2xl border border-white/10 px-4 py-3 text-sm font-semibold text-slate-300 transition hover:border-white/20 hover:bg-white/5 hover:text-white"
            >
              <LogOutIcon className="h-4 w-4" />
              Sign out
            </button>
          </div>
        </aside>

        {/* ── Right column: header (sticky) + scrollable main ── */}
        <div className="flex h-screen min-w-0 flex-1 flex-col">
          <header className="shrink-0 border-b border-white/70 bg-white/75 backdrop-blur">
            <div className="flex items-center justify-between gap-4 px-4 py-4 sm:px-6 lg:px-10">
              <div className="flex items-center gap-3">
                <button
                  type="button"
                  onClick={() => setIsSidebarOpen(true)}
                  className="rounded-2xl border border-slate-200 bg-white p-3 text-slate-600 shadow-sm transition hover:bg-slate-50 lg:hidden"
                  aria-label="Open navigation"
                >
                  <MenuIcon />
                </button>
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-600">Owner workspace</p>
                  <h1 className="text-xl font-semibold tracking-tight text-slate-950">{headerCopy.title}</h1>
                  <p className="hidden text-sm text-slate-500 sm:block">{headerCopy.description}</p>
                </div>
              </div>

              <div className="hidden items-center gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-2.5 shadow-sm sm:flex">
                <ArrowLeftIcon className="h-4 w-4 text-slate-400" />
                <div className="text-right">
                  <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Role</p>
                  <p className="text-sm font-semibold text-slate-700">{roleLabel}</p>
                </div>
              </div>
            </div>
          </header>

          {/* Page content — only this area scrolls */}
          <main className="flex-1 overflow-y-auto px-4 py-6 sm:px-6 lg:px-10">
            <div className="mx-auto max-w-7xl">
              <Outlet />
            </div>
          </main>
        </div>
      </div>

      {/* Mobile overlay */}
      {isSidebarOpen && (
        <button
          type="button"
          className="fixed inset-0 z-30 bg-slate-950/40 lg:hidden"
          onClick={() => setIsSidebarOpen(false)}
          aria-label="Close navigation overlay"
        />
      )}
    </>
  )
}

export default DashboardLayout
