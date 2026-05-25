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
                          <Badge variant="success">{tenant.roomAllotmentStatus || 'ACTIVE'}</Badge>
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
                              {isFlat ? (tenant.agreementEndDate ? new Date(tenant.agreementEndDate).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : 'Ongoing') : (tenant.agreementEndDate ? new Date(tenant.agreementEndDate).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : 'Ongoing')}
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
  const [selectedFloorId, setSelectedFloorId] = useState(null)

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
  }

  const selectedFloor = floors.find(floor => floor.floorId === selectedFloorId)

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
          <Button label="Back" variant="secondary" onClick={() => navigate('/owner/hostels')} />
        }
      />

      {error ? <Alert tone="error">{error}</Alert> : null}

      {loading ? (
        <div className="space-y-6">
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
        <div className="space-y-6">
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
                }
              />
              <CardContent className="pt-0">
                <RoomPanel
                  hostelId={hostelId}
                  hostelName={hostelName}
                  floorId={selectedFloor.floorId}
                  floorNumber={selectedFloor.floorNumber}
                  navigate={navigate}
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
