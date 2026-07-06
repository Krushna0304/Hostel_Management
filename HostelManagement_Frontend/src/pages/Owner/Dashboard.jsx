import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import HostelList from './HostelList'
import { hostelService } from '../../services/hostelService'
import { BuildingIcon, ClipboardIcon, SparkIcon, UsersIcon } from '../../components/icons/AppIcons'
import {
  Alert,
  Button,
  EmptyState,
  PageHeader,
  Skeleton,
  StatCard,
} from '../../components/ui'

const OwnerDashboard = () => {
  const navigate = useNavigate()
  const [hostels, setHostels] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showQuickStart, setShowQuickStart] = useState(true)

  useEffect(() => {
    fetchHostels()
    // Load quick start visibility from localStorage, default to true (visible)
    const savedVisibility = localStorage.getItem('quickStartVisible')
    if (savedVisibility !== null) {
      setShowQuickStart(JSON.parse(savedVisibility))
    } else {
      // First time users see the guide by default
      setShowQuickStart(true)
      localStorage.setItem('quickStartVisible', JSON.stringify(true))
    }
  }, [])

  const toggleQuickStart = () => {
    const newVisibility = !showQuickStart
    setShowQuickStart(newVisibility)
    // Save to localStorage to persist user preference
    localStorage.setItem('quickStartVisible', JSON.stringify(newVisibility))
  }

  const fetchHostels = async () => {
    try {
      setLoading(true)
      setError('')
      const response = await hostelService.getAllHostels()
      const hostelsData = (response.data ?? []).map((hostel) => ({
        ...hostel,
        hostelId: hostel.hostelId || hostel.id || hostel._id,
      }))
      setHostels(hostelsData)
    } catch (err) {
      const errorData = err?.response?.data
      setError(errorData?.message || 'Failed to load hostels. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  const handleHostelClick = (hostel) => {
    navigate(`/owner/hostels/${hostel.hostelId}/floors`, {
      state: {
        hostelId: hostel.hostelId,
        hostelName: hostel.hostelName,
        hostelAddress: hostel.hostelAddress,
      },
    })
  }

  const stats = [
    {
      label: 'Total hostels',
      value: hostels.length,
      meta: hostels.length === 1 ? '1 active property in your workspace' : `${hostels.length} active properties in your workspace`,
      icon: <BuildingIcon className="h-5 w-5" />,
    },
    {
      label: 'Agreement workflows',
      value: hostels.length > 0 ? 'Ready' : 'Start',
      meta: 'Create new tenant onboarding agreements from the dashboard.',
      icon: <ClipboardIcon className="h-5 w-5" />,
    },
    {
      label: 'Team visibility',
      value: 'Live',
      meta: 'Shared owner workspace with cleaner, more structured navigation.',
      icon: <UsersIcon className="h-5 w-5" />,
    },
  ]

  return (
    <div className="space-y-5">
      <PageHeader
        eyebrow="Overview"
        title="Run your hostels like a modern SaaS operation"
        description="This dashboard turns your property setup into a cleaner operational workflow with quick actions, structured inventory management, and better day-to-day visibility."
        action={<Button label="Create agreement" onClick={() => navigate('/owner/agreements/create')} />}
        secondaryAction={<Button label="Add hostel" variant="secondary" onClick={() => navigate('/owner/hostels/create-hostel')} />}
      />

      {error ? (
        <Alert tone="error" title="We couldn't load your portfolio.">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <span>{error}</span>
            <Button label="Try again" variant="secondary" onClick={fetchHostels} />
          </div>
        </Alert>
      ) : null}

      <section className="grid gap-4 lg:grid-cols-3">
        {loading
          ? Array.from({ length: 3 }).map((_, index) => <Skeleton key={index} className="h-40 rounded-3xl" />)
          : stats.map((stat) => <StatCard key={stat.label} {...stat} />)}
      </section>

      <section className={`grid gap-6 ${showQuickStart ? 'xl:grid-cols-[1.6fr_0.8fr]' : 'xl:grid-cols-1'}`}>
        <div>
          {loading ? (
            <div className={`grid gap-4 ${showQuickStart ? 'lg:grid-cols-2' : 'lg:grid-cols-3'}`}>
              {Array.from({ length: showQuickStart ? 4 : 6 }).map((_, index) => (
                <Skeleton key={index} className="h-64 rounded-3xl" />
              ))}
            </div>
          ) : hostels.length > 0 ? (
            <HostelList
              hostels={hostels}
              onHostelClick={handleHostelClick}
              onCreateHostel={() => navigate('/owner/hostels/create-hostel')}
              showQuickStart={showQuickStart}
            />
          ) : (
            <EmptyState
              icon={<SparkIcon className="h-5 w-5" />}
              title="No hostels yet"
              description="Start by creating your first hostel. From there you can add floors, define room inventory, and move into agreement creation."
              actionLabel="Create your first hostel"
              onAction={() => navigate('/owner/hostels/create-hostel')}
            />
          )}
        </div>

        {/* Quick Start Guide with toggle - only render when visible */}
        {showQuickStart && (
          <div className="relative">
            {/* Toggle Button */}
            <button
              onClick={toggleQuickStart}
              className="absolute -top-2 -right-2 z-10 flex h-8 w-8 items-center justify-center rounded-full bg-slate-800 text-white shadow-lg transition-all hover:bg-slate-700 hover:scale-105"
              title="Hide Quick Start Guide"
            >
              <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>

            {/* Quick Start Guide Content */}
            <div className="rounded-[1.75rem] border border-slate-200 bg-slate-950 p-6 text-white surface-glow">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-300">Quick start</p>
              <h2 className="mt-3 text-2xl font-semibold tracking-tight">A cleaner property workflow</h2>
              <div className="mt-6 space-y-4">
                {[
                  'Create a hostel to establish the property record.',
                  'Add floors to organize the physical structure clearly.',
                  'Create rooms with capacity and availability details.',
                  'Launch agreements when inventory is ready for onboarding.',
                ].map((step, index) => (
                  <div key={step} className="flex gap-3">
                    <div className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-white/10 text-sm font-semibold">
                      {index + 1}
                    </div>
                    <p className="text-sm leading-6 text-slate-300">{step}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </section>

      {/* Show Quick Start Guide Button - only when hidden */}
      {!showQuickStart && (
        <div className="fixed bottom-6 right-6 z-50">
          <button
            onClick={toggleQuickStart}
            className="flex h-12 w-12 items-center justify-center rounded-full bg-slate-950 text-white shadow-lg transition-all hover:bg-slate-800 hover:scale-105"
            title="Show Quick Start Guide"
          >
            <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </button>
        </div>
      )}
    </div>
  )
}

export default OwnerDashboard
