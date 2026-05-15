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

// ── Room panel shown inline when a floor is expanded ──────────────────────────
const RoomPanel = ({ hostelId, hostelName, floorId, floorNumber, navigate }) => {
  const [rooms, setRooms] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // Tenant modal state
  const [selectedRoom, setSelectedRoom] = useState(null)
  const [roomTenants, setRoomTenants] = useState([])
  const [tenantLoading, setTenantLoading] = useState(false)
  const [tenantError, setTenantError] = useState('')

  useEffect(() => {
    let cancelled = false
    const fetchRooms = async () => {
      setLoading(true)
      setError('')
      try {
        const response = await roomService.getRoomsByFloor(hostelId, floorId)
        if (!cancelled) setRooms(response.data || [])
      } catch (err) {
        if (!cancelled) setError(err?.response?.data?.message || 'Failed to load rooms.')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    fetchRooms()
    return () => { cancelled = true }
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
        actionLabel="Add room"
        onAction={() =>
          navigate(`/owner/hostels/${hostelId}/floors/${floorId}/add-room`, {
            state: { hostelId, floorId, hostelName, floorNumber },
          })
        }
      />
    )
  }

  return (
    <>
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        {rooms.map((room) => {
          const isFlat = room.roomType === 'FLAT'
          const isPgRoom = room.roomType === 'PG_ROOM'
          
          // For FLAT rooms, calculate allotted tenants (total beds - available beds)
          const allottedTenants = isFlat ? (room.totalBeds - room.availableBeds) : null
          
          return (
            <button
              key={room.roomId}
              type="button"
              onClick={() => openRoom(room)}
              className="rounded-2xl border border-slate-200 bg-white p-4 text-left transition duration-200 hover:-translate-y-0.5 hover:border-sky-200 hover:bg-sky-50/60 hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500/40"
            >
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
                  </div>
                  <h4 className="mt-1 text-lg font-semibold text-slate-950">{room.roomNumber}</h4>
                </div>
                <Badge variant={room.isActive ? 'success' : 'warning'}>
                  {room.isActive ? 'Active' : 'Inactive'}
                </Badge>
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
            </button>
          )
        })}
      </div>

      {/* Tenant modal */}
      {selectedRoom ? (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-slate-950/45 px-4 py-6 sm:items-center">
          <div className="absolute inset-0" onClick={closeModal} aria-hidden="true" />
          <Card className="relative z-10 w-full max-w-2xl overflow-hidden">
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
                    
                    return (
                      <div
                        key={tenant.tenantId || `${tenant.roomId}-${tenant.tenantName}`}
                        className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4"
                      >
                        <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                          <div>
                            <div className="flex items-center gap-2">
                              <p className="text-base font-semibold text-slate-950">{tenant.tenantName || 'Unnamed tenant'}</p>
                              {isFlat && (
                                <span className="px-2 py-0.5 text-xs font-medium rounded-full bg-purple-100 text-purple-700">
                                  Primary Tenant
                                </span>
                              )}
                            </div>
                            <p className="mt-1 text-sm text-slate-500">Room {selectedRoom.roomNumber}</p>
                          </div>
                          <Badge variant="success">{tenant.roomAllotmentStatus || 'ACTIVE'}</Badge>
                        </div>
                        
                        <div className="mt-4 grid gap-3 sm:grid-cols-2">
                          <div>
                            <p className="text-xs uppercase tracking-[0.18em] text-slate-400">Floor</p>
                            <p className="mt-1 text-sm font-medium text-slate-900">Floor {floorNumber}</p>
                          </div>
                          <div>
                            <p className="text-xs uppercase tracking-[0.18em] text-slate-400">
                              {isFlat ? 'Agreement Date' : 'Allotment Date'}
                            </p>
                            <p className="mt-1 text-sm font-medium text-slate-900">
                              {tenant.allotmentDate ? new Date(tenant.allotmentDate).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '-'}
                            </p>
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
        </div>
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
  const [expandedFloorId, setExpandedFloorId] = useState(null)

  useEffect(() => {
    const fetchFloors = async () => {
      setLoading(true)
      setError('')
      try {
        const response = await floorService.getFloorsByHostel(hostelId)
        setFloors(response.data || [])
      } catch (err) {
        setError(err?.response?.data?.message || 'Failed to load floors.')
      } finally {
        setLoading(false)
      }
    }
    fetchFloors()
  }, [hostelId])

  const toggleFloor = (floorId) => {
    setExpandedFloorId((prev) => (prev === floorId ? null : floorId))
  }

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Inventory structure"
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
          <Button label="Back" variant="secondary" onClick={() => navigate('/owner/dashboard')} />
        }
      />

      {error ? <Alert tone="error">{error}</Alert> : null}

      {loading ? (
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-20 rounded-3xl" />
          ))}
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
        <Card>
          <CardHeader
            title="Floors"
            description="Click any floor to expand its rooms. Click again to collapse."
          />
          <CardContent>
            <div className="space-y-3">
              {floors.map((floor) => {
                const isOpen = expandedFloorId === floor.floorId
                return (
                  <div
                    key={floor.floorId}
                    className={`overflow-hidden rounded-3xl border transition-colors duration-200 ${
                      isOpen
                        ? 'border-sky-200 bg-sky-50/40'
                        : 'border-slate-200 bg-slate-50/80'
                    }`}
                  >
                    {/* Floor header row — click to toggle */}
                    <div className="flex w-full items-center justify-between gap-4 px-5 py-4">
                      <button
                        type="button"
                        onClick={() => toggleFloor(floor.floorId)}
                        className="flex flex-1 items-center gap-4 text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-sky-500/50 rounded-lg"
                      >
                        <div className={`rounded-2xl p-3 shadow-sm transition-colors ${isOpen ? 'bg-sky-100 text-sky-700' : 'bg-white text-slate-700'}`}>
                          <LayersIcon className="h-5 w-5" />
                        </div>
                        <div>
                          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Floor</p>
                          <h3 className="mt-0.5 text-lg font-semibold text-slate-950">
                            Floor {floor.floorNumber}
                          </h3>
                        </div>
                      </button>

                      <div className="flex items-center gap-3">
                        {/* Add room shortcut — only visible when expanded */}
                        {isOpen && (
                          <button
                            type="button"
                            onClick={() => {
                              navigate(
                                `/owner/hostels/${hostelId}/floors/${floor.floorId}/add-room`,
                                { state: { hostelId, floorId: floor.floorId, hostelName, floorNumber: floor.floorNumber } }
                              )
                            }}
                            className="rounded-xl bg-sky-600 px-3 py-1.5 text-xs font-semibold text-white shadow-sm hover:bg-sky-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500"
                          >
                            + Add room
                          </button>
                        )}
                        <button
                          type="button"
                          onClick={() => toggleFloor(floor.floorId)}
                          className="focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500/50 rounded-lg p-1"
                        >
                          {isOpen
                            ? <ChevronUp className="h-5 w-5 text-sky-500" />
                            : <ChevronDown className="h-5 w-5 text-slate-400" />
                          }
                        </button>
                      </div>
                    </div>

                    {/* Rooms panel — shown when floor is expanded */}
                    {isOpen && (
                      <div className="border-t border-sky-100 px-5 pb-5 pt-4">
                        <RoomPanel
                          hostelId={hostelId}
                          hostelName={hostelName}
                          floorId={floor.floorId}
                          floorNumber={floor.floorNumber}
                          navigate={navigate}
                        />
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}

export default Floors
