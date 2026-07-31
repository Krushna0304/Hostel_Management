import { useEffect, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { floorService, roomService } from '../../services/hostelService'
import {
  Alert,
  Badge,
  Button,
  Card,
  CardContent,
  CardHeader,
  EmptyState,
  PageHeader,
  Skeleton,
  CenteredModal,
} from '../../components/ui'
import { DoorIcon, LayersIcon } from '../../components/icons/AppIcons'

// Chevron icons inline to avoid extra deps
const ChevronDown = ({ className }) => (
  <svg className={className} viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
    <path fillRule="evenodd" d="M5.22 8.22a.75.75 0 0 1 1.06 0L10 11.94l3.72-3.72a.75.75 0 1 1 1.06 1.06l-4.25 4.25a.75.75 0 0 1-1.06 0L5.22 9.28a.75.75 0 0 1 0-1.06Z" clipRule="evenodd" />
  </svg>
)

const ChevronUp = ({ className }) => (
  <svg className={className} viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
    <path fillRule="evenodd" d="M14.78 11.78a.75.75 0 0 1-1.06 0L10 8.06l-3.72 3.72a.75.75 0 0 1-1.06-1.06l4.25-4.25a.75.75 0 0 1 1.06 0l4.25 4.25a.75.75 0 0 1 0 1.06Z" clipRule="evenodd" />
  </svg>
)

// ── Helper: today + offset as YYYY-MM-DD ─────────────────────────────────────
const localDateStr = (offsetDays = 0) => {
  const d = new Date()
  d.setDate(d.getDate() + offsetDays)
  return d.toLocaleDateString('en-CA') // YYYY-MM-DD
}

// ── Room panel shown inline when a floor is expanded ──────────────────────────
const RoomPanel = ({ hostelId, hostelName, floorId, floorNumber, navigate, filterOpen, onFilterClose, searchQuery }) => {
  const [rooms, setRooms] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [lastRefreshed, setLastRefreshed] = useState(null)
  const [refreshing, setRefreshing] = useState(false)
  const [isFiltered, setIsFiltered] = useState(false)

  // Filter form state
  const defaultFilters = {
    roomType: 'ALL',
    minAvailableBeds: 0,
    isActive: 'ALL',
    startDate: localDateStr(0),
    endDate: localDateStr(1),
  }
  const [filters, setFilters] = useState(defaultFilters)
  const [filterApplying, setFilterApplying] = useState(false)
  const [filterError, setFilterError] = useState('')

  // Tenant modal state
  const [selectedRoom, setSelectedRoom] = useState(null)
  const [roomTenants, setRoomTenants] = useState([])
  const [tenantLoading, setTenantLoading] = useState(false)
  const [tenantError, setTenantError] = useState('')
  const [activatingId, setActivatingId] = useState(null)

  const fetchRooms = async (isManualRefresh = false) => {
    if (isManualRefresh) {
      setRefreshing(true)
    } else {
      setLoading(true)
    }
    setError('')
    try {
      const response = await roomService.getRoomsByFloor(hostelId, floorId)
      setRooms(response.data || [])
      setLastRefreshed(new Date())
      setIsFiltered(false)
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to load rooms.')
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }

  const applyFilter = async () => {
    setFilterApplying(true)
    setFilterError('')
    try {
      const response = await roomService.filterRooms(hostelId, floorId, filters)
      setRooms(response.data || [])
      setLastRefreshed(new Date())
      setIsFiltered(true)
    } catch (err) {
      setFilterError(err?.response?.data?.message || 'Failed to apply filter.')
    } finally {
      setFilterApplying(false)
    }
  }

  const resetFilter = () => {
    setFilters(defaultFilters)
    setFilterError('')
    setIsFiltered(false)
    fetchRooms(false)
    onFilterClose()
  }

  useEffect(() => {
    let cancelled = false
    const load = async () => {
      setLoading(true)
      setError('')
      try {
        const response = await roomService.getRoomsByFloor(hostelId, floorId)
        if (!cancelled) {
          setRooms(response.data || [])
          setLastRefreshed(new Date())
          setIsFiltered(false)
        }
      } catch (err) {
        if (!cancelled) setError(err?.response?.data?.message || 'Failed to load rooms.')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    setFilters(defaultFilters)
    // 6.3.5: Auto-refresh availability every 60 seconds (only when not filtered)
    const intervalId = setInterval(() => {
      if (!cancelled && !isFiltered) fetchRooms(true)
    }, 60000)
    return () => {
      cancelled = true
      clearInterval(intervalId)
    }
  }, [hostelId, floorId])

  const openRoom = async (room) => {
    setSelectedRoom(room)
    setTenantLoading(true)
    setTenantError('')
    setRoomTenants([])
    try {
      const response = await roomService.getRoomTenants(hostelId, floorId, room.roomId)
      setRoomTenants(response.data || [])
    } catch (err) {
      setTenantError(err?.response?.data?.message || 'Failed to load tenants.')
    } finally {
      setTenantLoading(false)
    }
  }

  const closeModal = () => {
    setSelectedRoom(null)
    setRoomTenants([])
    setTenantError('')
  }

  const handleActivate = async (tenant) => {
    try {
      setActivatingId(tenant.allotmentId)
      await roomService.activateAllotment(hostelId, floorId, selectedRoom.roomId, tenant.allotmentId)
      // Refresh tenant list in place
      const response = await roomService.getRoomTenants(hostelId, floorId, selectedRoom.roomId)
      setRoomTenants(response.data || [])
    } catch (err) {
      setTenantError(err?.response?.data?.message || 'Failed to activate allotment.')
    } finally {
      setActivatingId(null)
    }
  }

  if (loading) {
    return (
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-32 rounded-2xl" />
        ))}
      </div>
    )
  }

  if (error) {
    return <Alert tone="error">{error}</Alert>
  }

  if (rooms.length === 0) {
    return (
      <EmptyState
        icon={<DoorIcon className="h-5 w-5" />}
        title="No rooms yet"
        description="Add the first room on this floor to begin managing bed inventory."
      />
    )
  }

  return (
    <>
      {/* Filter Panel */}
      {filterOpen && (
        <div className="mb-5 rounded-2xl border border-sky-200 bg-sky-50/70 p-4 shadow-sm">
          <p className="text-xs font-bold uppercase tracking-[0.18em] text-sky-700 mb-3">Filter by</p>
          {filterError && (
            <div className="mb-3 px-3 py-2 rounded-lg bg-red-50 border border-red-200 text-xs text-red-700">{filterError}</div>
          )}
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
            {/* Room Type */}
            <div className="flex flex-col gap-1">
              <label className="text-xs font-semibold text-slate-500 uppercase tracking-[0.15em]">Room Type</label>
              <select
                value={filters.roomType}
                onChange={e => setFilters(f => ({ ...f, roomType: e.target.value }))}
                className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-sky-500/40 transition"
              >
                <option value="ALL">All</option>
                <option value="FLAT">Flat</option>
                <option value="PG_ROOM">PG Room</option>
              </select>
            </div>

            {/* Min Available Beds */}
            <div className="flex flex-col gap-1">
              <label className="text-xs font-semibold text-slate-500 uppercase tracking-[0.15em]">Available Beds ≥</label>
              <input
                type="number"
                min="0"
                value={filters.minAvailableBeds}
                onChange={e => setFilters(f => ({ ...f, minAvailableBeds: parseInt(e.target.value, 10) || 0 }))}
                className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-sky-500/40 transition"
              />
            </div>

            {/* Room Status */}
            <div className="flex flex-col gap-1">
              <label className="text-xs font-semibold text-slate-500 uppercase tracking-[0.15em]">Room Status</label>
              <select
                value={filters.isActive}
                onChange={e => setFilters(f => ({ ...f, isActive: e.target.value }))}
                className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-sky-500/40 transition"
              >
                <option value="ALL">All</option>
                <option value="true">Active</option>
                <option value="false">Inactive</option>
              </select>
            </div>

            {/* Start Date */}
            <div className="flex flex-col gap-1">
              <label className="text-xs font-semibold text-slate-500 uppercase tracking-[0.15em]">Start Date</label>
              <input
                type="date"
                value={filters.startDate}
                onChange={e => setFilters(f => ({ ...f, startDate: e.target.value }))}
                className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-sky-500/40 transition"
              />
            </div>

            {/* End Date */}
            <div className="flex flex-col gap-1">
              <label className="text-xs font-semibold text-slate-500 uppercase tracking-[0.15em]">End Date</label>
              <input
                type="date"
                value={filters.endDate}
                onChange={e => setFilters(f => ({ ...f, endDate: e.target.value }))}
                className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-sky-500/40 transition"
              />
            </div>
          </div>

          {/* Action buttons */}
          <div className="mt-4 flex items-center gap-2">
            <button
              type="button"
              onClick={applyFilter}
              disabled={filterApplying}
              className="flex items-center gap-1.5 px-4 py-2 text-xs font-semibold text-white bg-sky-600 rounded-xl hover:bg-sky-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors shadow-sm"
            >
              {filterApplying ? (
                <svg className="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
              ) : (
                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2a1 1 0 01-.293.707L13 13.414V19a1 1 0 01-.553.894l-4 2A1 1 0 017 21v-7.586L3.293 6.707A1 1 0 013 6V4z" />
                </svg>
              )}
              Apply Filter
            </button>
            <button
              type="button"
              onClick={resetFilter}
              className="px-4 py-2 text-xs font-semibold text-slate-600 bg-white border border-slate-200 rounded-xl hover:bg-slate-50 transition-colors"
            >
              Reset
            </button>
          </div>
        </div>
      )}

      {/* Filtered badge */}
      {isFiltered && (
        <div className="flex items-center gap-2 mb-3">
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-semibold rounded-full bg-sky-100 text-sky-700 border border-sky-200">
            <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2a1 1 0 01-.293.707L13 13.414V19a1 1 0 01-.553.894l-4 2A1 1 0 017 21v-7.586L3.293 6.707A1 1 0 013 6V4z" />
            </svg>
            Filtered · {rooms.length} result{rooms.length !== 1 ? 's' : ''}
          </span>
          <button
            type="button"
            onClick={resetFilter}
            className="text-xs text-slate-500 hover:text-slate-800 underline"
          >
            Clear filter
          </button>
        </div>
      )}

      {/* 6.3.5: Real-time availability header with manual refresh */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          {refreshing && (
            <div className="flex items-center gap-1.5 text-xs text-sky-600">
              <svg className="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
              </svg>
              <span>Updating...</span>
            </div>
          )}
          {lastRefreshed && !refreshing && (
            <span className="text-xs text-slate-400">
              Updated {lastRefreshed.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })}
            </span>
          )}
        </div>
        <button
          type="button"
          onClick={() => fetchRooms(true)}
          disabled={refreshing || loading}
          className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-sky-700 bg-sky-50 border border-sky-200 rounded-lg hover:bg-sky-100 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          title="Refresh room availability"
        >
          <svg className={`w-3.5 h-3.5 ${refreshing ? 'animate-spin' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          Refresh
        </button>
      </div>

      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        {(() => {
          const visibleRooms = searchQuery.trim()
            ? rooms.filter(r => r.roomNumber?.toLowerCase().includes(searchQuery.trim().toLowerCase()))
            : rooms

          if (visibleRooms.length === 0 && searchQuery.trim()) {
            return (
              <div className="col-span-full py-8 text-center text-sm text-slate-400">
                No rooms match &ldquo;<span className="font-medium text-slate-600">{searchQuery}</span>&rdquo;
              </div>
            )
          }

          return visibleRooms.map((room) => {
          const isFlat = room.roomType === 'FLAT'
          const isPgRoom = room.roomType === 'PG_ROOM'
          
          // For FLAT rooms, calculate allotted tenants (total beds - available beds)
          const allottedTenants = isFlat ? (room.totalBeds - room.availableBeds) : null
          
          return (
            <button
              key={room.roomId}
              type="button"
              onClick={() => openRoom(room)}
              className="relative rounded-2xl border border-slate-200 bg-white p-4 text-left transition duration-200 hover:-translate-y-0.5 hover:border-sky-200 hover:bg-sky-50/60 hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500/40"
            >
              {/* Pending actions indicator badge */}
              {room.tenantActionPendingCount > 0 && (
                <div className="absolute -top-2 -right-2 z-10">
                  <div className="relative">
                    <div className="w-6 h-6 bg-orange-500 rounded-full flex items-center justify-center text-xs font-bold text-white border-2 border-white shadow-lg">
                      {room.tenantActionPendingCount > 99 ? '99+' : room.tenantActionPendingCount}
                    </div>
                    <div className="absolute inset-0 bg-orange-500 rounded-full animate-ping opacity-30"></div>
                  </div>
                </div>
              )}
              
              <div className="flex items-start justify-between gap-3">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Room</p>
                    <span className={`px-2 py-1 text-xs font-semibold rounded-full ${
                      isFlat 
                        ? 'bg-purple-100 text-purple-800 border border-purple-200' 
                        : 'bg-blue-100 text-blue-800 border border-blue-200'
                    }`}>
                      {isFlat ? 'Flat' : 'PG Room'}
                    </span>
                    {/* Priority badge for high pending action count */}
                    {room.tenantActionPendingCount > 5 && (
                      <span className="px-2 py-1 text-xs font-semibold rounded-full bg-red-100 text-red-800 border border-red-200">
                        Priority
                      </span>
                    )}
                  </div>
                  <h4 className="mt-1 text-lg font-semibold text-slate-950">{room.roomNumber}</h4>
                </div>
                <div className="flex flex-col items-end gap-1">
                  <Badge variant={room.isActive ? 'success' : 'warning'}>
                    {room.isActive ? 'Active' : 'Inactive'}
                  </Badge>
                  {/* Pending actions summary */}
                  {room.tenantActionPendingCount > 0 && (
                    <div className="flex items-center gap-1 px-2 py-1 bg-orange-50 border border-orange-200 rounded-full">
                      <div className="w-2 h-2 bg-orange-500 rounded-full animate-pulse"></div>
                      <span className="text-xs font-medium text-orange-700">
                        {room.tenantActionPendingCount} action{room.tenantActionPendingCount > 1 ? 's' : ''} pending
                      </span>
                    </div>
                  )}
                </div>
              </div>
              <div className="mt-3 grid grid-cols-2 gap-2">
                <div className="rounded-xl bg-slate-50 px-3 py-2">
                  <p className="text-xs uppercase tracking-[0.15em] text-slate-400">Total beds</p>
                  <p className="mt-1 text-xl font-semibold text-slate-950">{room.totalBeds ?? '-'}</p>
                </div>
                <div className="rounded-xl bg-slate-50 px-3 py-2">
                  <p className="text-xs uppercase tracking-[0.15em] text-slate-400">
                    {isFlat ? 'Allotted' : 'Available'}
                  </p>
                  <p className="mt-1 text-xl font-semibold text-slate-950">
                    {isFlat ? (allottedTenants ?? 0) : (room.availableBeds ?? '-')}
                  </p>
                </div>
              </div>
              
              {/* Enhanced bed availability display with overbooking indicator */}
              <div className="mt-3 pt-3 border-t border-slate-200">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className={`w-3 h-3 rounded-full ${
                      room.availableBeds > 0 
                        ? 'bg-green-500' 
                        : room.availableBeds === 0 
                        ? 'bg-yellow-500' 
                        : 'bg-red-500'
                    }`} />
                    <span className="text-sm font-medium text-slate-700">
                      Bed Status: {room.availableBeds > 0 ? 'Available' : room.availableBeds === 0 ? 'Full' : 'Overbooked'}
                    </span>
                  </div>
                  
                  {/* Occupancy percentage */}
                  <div className="text-sm text-slate-500">
                    {room.totalBeds > 0 ? Math.round(((room.totalBeds - room.availableBeds) / room.totalBeds) * 100) : 0}% occupied
                  </div>
                </div>
                
                {/* Enhanced overbooking warning with detailed message */}
                {room.availableBeds < 0 && (
                  <div className="mt-2 px-3 py-2 bg-red-50 border-l-4 border-red-400 rounded-r-lg">
                    <div className="flex items-start gap-2">
                      <svg className="w-5 h-5 text-red-500 flex-shrink-0 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                      </svg>
                      <div className="flex-1">
                        <div className="flex items-center justify-between">
                          <span className="text-sm font-bold text-red-800">OVERBOOKED</span>
                          <span className="text-xs text-red-600 bg-red-100 px-2 py-1 rounded-full">
                            -{Math.abs(room.availableBeds)} beds
                          </span>
                        </div>
                        <p className="text-xs text-red-700 mt-1">
                          This room has {Math.abs(room.availableBeds)} more allocation{Math.abs(room.availableBeds) > 1 ? 's' : ''} than available beds. 
                          Action required to resolve conflicts.
                        </p>
                      </div>
                    </div>
                  </div>
                )}
                
                {/* Near capacity warning */}
                {room.availableBeds === 1 && room.totalBeds > 1 && (
                  <div className="mt-2 px-3 py-2 bg-yellow-50 border-l-4 border-yellow-400 rounded-r-lg">
                    <div className="flex items-center gap-2">
                      <svg className="w-4 h-4 text-yellow-600" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                      </svg>
                      <span className="text-xs font-medium text-yellow-800">
                        Last bed available - Consider monitoring closely
                      </span>
                    </div>
                  </div>
                )}
              </div>
            </button>
          )
        })})()}
      </div>

        {/* Tenant modal — guard selectedRoom so children are not evaluated when closed */}
      {selectedRoom ? (
      <CenteredModal open onClose={closeModal}>
          <Card className="w-full overflow-hidden shadow-2xl">
            <div className="flex items-start justify-between gap-4 border-b border-slate-100 px-6 py-5">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Room tenants</p>
                  <span className={`px-2 py-1 text-xs font-semibold rounded-full ${
                    selectedRoom.roomType === 'FLAT' 
                      ? 'bg-purple-100 text-purple-800 border border-purple-200' 
                      : 'bg-blue-100 text-blue-800 border border-blue-200'
                  }`}>
                    {selectedRoom.roomType === 'FLAT' ? 'Flat' : 'PG Room'}
                  </span>
                </div>
                <h3 className="mt-2 text-xl font-semibold text-slate-950">Room {selectedRoom.roomNumber}</h3>
                <p className="mt-1 text-sm text-slate-500">{hostelName} · Floor {floorNumber}</p>
              </div>
              <Button label="Close" variant="secondary" onClick={closeModal} />
            </div>
            <div className="px-6 py-5">
              {tenantLoading ? (
                <div className="space-y-3">
                  <Skeleton className="h-16 rounded-2xl" />
                  <Skeleton className="h-16 rounded-2xl" />
                </div>
              ) : tenantError ? (
                <Alert tone="error">{tenantError}</Alert>
              ) : roomTenants.length === 0 ? (
                <EmptyState
                  title="No tenants found"
                  description="This room does not currently have any assigned tenants."
                />
              ) : (
                <div className="space-y-3">
                  {roomTenants.map((tenant) => {
                    const isFlat = tenant.agreementType === 'FLAT'
                    const hasCotenant = isFlat && tenant.coTenantNames && tenant.coTenantNames.length > 0
                    const isUpcoming = tenant.roomAllotmentStatus === 'UPCOMING'
                    const todayStr = new Date().toLocaleDateString('en-CA') // "YYYY-MM-DD" in local time
                    const startStr = tenant.allotmentDate ? tenant.allotmentDate.slice(0, 10) : null
                    const canActivate = isUpcoming && tenant.allotmentId && startStr && todayStr >= startStr

                    return (
                      <div
                        key={tenant.tenantId || `${tenant.roomId}-${tenant.tenantName}`}
                        className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4"
                      >
                        <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                          <div>
                            <div className="flex items-center gap-2 flex-wrap">
                              <p className="text-base font-semibold text-slate-950">{tenant.tenantName || 'Unnamed tenant'}</p>
                              {tenant.planId ? (
                                <button
                                  onClick={(e) => {
                                    e.stopPropagation()
                                    navigate(`/owner/plans?planId=${tenant.planId}`)
                                  }}
                                  className="text-sm font-medium text-blue-600 hover:text-blue-800 hover:underline cursor-pointer"
                                >
                                  ({tenant.planName || 'Unnamed Plan'})
                                </button>
                              ) : (
                                <span className="text-sm font-medium text-slate-600">
                                  ({tenant.planName || 'No Plan'})
                                </span>
                              )}
                              {isFlat && (
                                <span className="px-2 py-0.5 text-xs font-medium rounded-full bg-purple-100 text-purple-700">
                                  Primary Tenant
                                </span>
                              )}
                            </div>
                          </div>
                          <div className="flex items-center gap-2">
                            <Badge variant={isUpcoming ? 'warning' : 'success'}>{tenant.roomAllotmentStatus || 'ACTIVE'}</Badge>
                            {canActivate && (
                              <Button
                                label="Tenant Arrived"
                                size="sm"
                                variant="success"
                                loading={activatingId === tenant.allotmentId}
                                onClick={() => handleActivate(tenant)}
                              />
                            )}
                          </div>
                        </div>
                        
                        {/* Single line with all key information */}
                        <div className="mt-3 flex flex-wrap items-center gap-4 text-sm">
                          <div className="flex items-center gap-1">
                            <span className="text-xs uppercase tracking-[0.18em] text-slate-400">Start:</span>
                            <span className="font-medium text-slate-900">
                              {tenant.allotmentDate ? new Date(tenant.allotmentDate).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '-'}
                            </span>
                          </div>
                          
                          <div className="flex items-center gap-1">
                            <span className="text-xs uppercase tracking-[0.18em] text-slate-400">End:</span>
                            <span className="font-medium text-slate-900">
                              {tenant.agreementEndDate ? new Date(tenant.agreementEndDate).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '-'}
                            </span>
                          </div>
                          
                          <div className="flex items-center gap-1">
                            <span className="text-xs uppercase tracking-[0.18em] text-slate-400">Mobile:</span>
                            <span className="font-medium text-slate-900">{tenant.phoneNumber || '-'}</span>
                          </div>
                        </div>

                        {/* Co-tenants section for FLAT rooms */}
                        {hasCotenant && (
                          <div className="mt-4 pt-3 border-t border-slate-200">
                            <p className="text-xs uppercase tracking-[0.18em] text-slate-400 mb-2">Co-tenants</p>
                            <div className="flex flex-wrap gap-2">
                              {tenant.coTenantNames.map((coTenantName, index) => (
                                <span
                                  key={index}
                                  className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800"
                                >
                                  {coTenantName}
                                </span>
                              ))}
                            </div>
                          </div>
                        )}
                      </div>
                    )
                  })}
                </div>
              )}
            </div>
          </Card>
      </CenteredModal>
      ) : null}
    </>
  )
}

// ── Main Floors page ──────────────────────────────────────────────────────────
const Floors = () => {
  const { hostelId: paramHostelId } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const hostelId = location.state?.hostelId || paramHostelId
  const hostelName = location.state?.hostelName || `Hostel ${hostelId}`
  const hostelAddress = location.state?.hostelAddress || 'Property address unavailable'

  const [floors, setFloors] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedFloorId, setSelectedFloorId] = useState(null)
  const [filterOpen, setFilterOpen] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')

  useEffect(() => {
    const fetchFloors = async () => {
      setLoading(true)
      setError('')
      try {
        const response = await floorService.getFloorsByHostel(hostelId)
        const floorsData = response.data || []
        setFloors(floorsData)
        // Select first floor by default if no floor is selected
        if (floorsData.length > 0 && !selectedFloorId) {
          setSelectedFloorId(floorsData[0].floorId)
        }
      } catch (err) {
        setError(err?.response?.data?.message || 'Failed to load floors.')
      } finally {
        setLoading(false)
      }
    }
    fetchFloors()
  }, [hostelId])

  // Update selected floor when floors change and no floor is selected
  useEffect(() => {
    if (floors.length > 0 && !selectedFloorId) {
      setSelectedFloorId(floors[0].floorId)
    }
  }, [floors, selectedFloorId])

  const selectFloor = (floorId) => {
    setSelectedFloorId(floorId)
    setFilterOpen(false)
    setSearchQuery('')
  }

  const selectedFloor = floors.find(floor => floor.floorId === selectedFloorId)

  return (
    <div className="space-y-5">
      <PageHeader
        title={hostelName}
        description={hostelAddress}
        action={
          <Button
            label="Add floor"
            onClick={() =>
              navigate(`/owner/hostels/${hostelId}/add-floor`, {
                state: { hostelId, hostelName },
              })
            }
          />
        }
        secondaryAction={
          <Button label="Back" variant="secondary" onClick={() => navigate('/owner/hostels')} />
        }
      />

      {error ? <Alert tone="error">{error}</Alert> : null}

      {loading ? (
        <div className="space-y-0">
          {/* Floor tabs skeleton */}
          <Card>
            <CardHeader title="Floors" description="Select a floor to view its rooms." />
            <CardContent>
              <div className="flex gap-3 overflow-x-auto pb-2">
                {Array.from({ length: 3 }).map((_, i) => (
                  <Skeleton key={i} className="h-20 w-32 rounded-2xl flex-shrink-0" />
                ))}
              </div>
            </CardContent>
          </Card>
          
          {/* Rooms skeleton */}
          <Card>
            <CardHeader title="Rooms" />
            <CardContent>
              <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
                {Array.from({ length: 3 }).map((_, i) => (
                  <Skeleton key={i} className="h-32 rounded-2xl" />
                ))}
              </div>
            </CardContent>
          </Card>
        </div>
      ) : floors.length === 0 ? (
        <EmptyState
          icon={<LayersIcon className="h-5 w-5" />}
          title="No floors created yet"
          description="Add your first floor to start structuring this property and make room management easier."
          actionLabel="Create first floor"
          onAction={() =>
            navigate(`/owner/hostels/${hostelId}/add-floor`, {
              state: { hostelId, hostelName },
            })
          }
        />
      ) : (
        <div className="space-y-4">
          {/* Floors Section - Horizontal Layout */}
          <Card>
            <CardHeader
              title="Floors"
              description="Select a floor to view its rooms."
            />
            <CardContent className="pt-0 pb-5">
              <div className="flex gap-3 overflow-x-auto pb-2">
                {floors.map((floor) => {
                  const isSelected = selectedFloorId === floor.floorId
                  return (
                    <button
                      key={floor.floorId}
                      type="button"
                      onClick={() => selectFloor(floor.floorId)}
                      className={`flex-shrink-0 rounded-2xl border-2 p-4 text-left transition-all duration-200 hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500/40 ${
                        isSelected
                          ? 'border-sky-500 bg-sky-50 shadow-md'
                          : 'border-slate-200 bg-white hover:border-sky-200 hover:bg-sky-50/30'
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <div className={`rounded-xl p-2 shadow-sm transition-colors ${
                          isSelected ? 'bg-sky-100 text-sky-700' : 'bg-slate-100 text-slate-600'
                        }`}>
                          <LayersIcon className="h-4 w-4" />
                        </div>
                        <div>
                          <p className={`text-xs font-semibold uppercase tracking-[0.2em] ${
                            isSelected ? 'text-sky-600' : 'text-slate-400'
                          }`}>
                            Floor
                          </p>
                          <h3 className={`mt-0.5 text-base font-semibold ${
                            isSelected ? 'text-sky-900' : 'text-slate-950'
                          }`}>
                            Floor {floor.floorNumber}
                          </h3>
                        </div>
                      </div>
                    </button>
                  )
                })}
              </div>
            </CardContent>
          </Card>

          {/* Rooms Section */}
          {selectedFloor && (
            <Card>
              <CardHeader
                title={`Floor ${selectedFloor.floorNumber} - Rooms`}
                description={`Rooms on Floor ${selectedFloor.floorNumber}`}
                action={
                  <div className="flex items-center gap-2">
                    {/* Search bar */}
                    <div className="relative">
                      <svg
                        className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400 pointer-events-none"
                        fill="none" stroke="currentColor" viewBox="0 0 24 24"
                      >
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                          d="M21 21l-4.35-4.35M17 11A6 6 0 105 11a6 6 0 0012 0z" />
                      </svg>
                      <input
                        id="room-search-input"
                        type="text"




































































































































































































































































































































































































































































































































































































































                        
                        value={searchQuery}
                        onChange={e => setSearchQuery(e.target.value)}
                        placeholder="Search room..."
                        className="pl-8 pr-3 py-2 text-sm rounded-xl border border-slate-200 bg-white text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-sky-500/40 focus:border-sky-400 w-36 transition-all"
                      />
                      {searchQuery && (
                        <button
                          type="button"
                          onClick={() => setSearchQuery('')}
                          className="absolute right-2 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                        >
                          <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                          </svg>
                        </button>
                      )}
                    </div>
                    {/* Filter button */}
                    <button
                      type="button"
                      id="room-filter-toggle-btn"
                      onClick={() => setFilterOpen(o => !o)}
                      className={`flex items-center gap-1.5 px-3 py-2 text-sm font-semibold rounded-xl border transition-all ${
                        filterOpen
                          ? 'bg-sky-600 text-white border-sky-600 shadow-sm'
                          : 'bg-white text-slate-700 border-slate-200 hover:border-sky-300 hover:text-sky-700'
                      }`}
                      title="Toggle filter"
                    >
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2a1 1 0 01-.293.707L13 13.414V19a1 1 0 01-.553.894l-4 2A1 1 0 017 21v-7.586L3.293 6.707A1 1 0 013 6V4z" />
                      </svg>
                      Filter
                    </button>
                    <Button
                      label="Add room"
                      onClick={() => {
                        navigate(
                          `/owner/hostels/${hostelId}/floors/${selectedFloor.floorId}/add-room`,
                          { 
                            state: { 
                              hostelId, 
                              floorId: selectedFloor.floorId, 
                              hostelName, 
                              floorNumber: selectedFloor.floorNumber 
                            } 
                          }
                        )
                      }}
                    />
                  </div>
                }
              />
              <CardContent className="pt-0">
                <RoomPanel
                  hostelId={hostelId}
                  hostelName={hostelName}
                  floorId={selectedFloor.floorId}
                  floorNumber={selectedFloor.floorNumber}
                  navigate={navigate}
                  filterOpen={filterOpen}
                  onFilterClose={() => setFilterOpen(false)}
                  searchQuery={searchQuery}
                />
              </CardContent>
            </Card>
          )}
        </div>
      )}
    </div>
  )
}

export default Floors
