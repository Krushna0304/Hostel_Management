import { useEffect, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { roomService } from '../../services/hostelService'
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
import { DoorIcon, AlertTriangleIcon, UsersIcon, BedIcon } from '../../components/icons/AppIcons'

const Rooms = () => {
  const { hostelId: paramHostelId, floorId: paramFloorId } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const hostelId = location.state?.hostelId || paramHostelId
  const floorId = location.state?.floorId || paramFloorId
  const floorNumber = location.state?.floorNumber || floorId
  const hostelName = location.state?.hostelName || `Hostel ${hostelId}`
  const [rooms, setRooms] = useState([])
  const [enhancedRooms, setEnhancedRooms] = useState([]) // Enhanced room data with overbooking info
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedRoom, setSelectedRoom] = useState(null)
  const [roomTenants, setRoomTenants] = useState([])
  const [tenantLoading, setTenantLoading] = useState(false)
  const [tenantError, setTenantError] = useState('')
  const [showTenantPanel, setShowTenantPanel] = useState(false)
  const [availabilityData, setAvailabilityData] = useState({}) // Store availability data for rooms

  useEffect(() => {
    const fetchRoomsWithAvailability = async () => {
      setLoading(true)
      setError('')
      try {
        // Fetch basic room data
        const roomsResponse = await roomService.getRoomsByFloor(hostelId, floorId)
        const basicRooms = roomsResponse.data || []
        setRooms(basicRooms)

        // For enhanced room management (Task 6.3.1), try to get enhanced availability data
        try {
          const enhancedResponse = await roomService.getRoomsSortedByPendingActions(hostelId)
          const enhancedRoomData = enhancedResponse.data?.rooms || []
          
          // Filter enhanced data to only rooms on this floor and merge with basic room data
          const floorRooms = enhancedRoomData.filter(room => 
            basicRooms.some(basicRoom => basicRoom.roomId === room.roomId)
          )
          
          // Create availability data map for quick lookup
          const availabilityMap = {}
          await Promise.all(floorRooms.map(async (room) => {
            try {
              const availabilityResponse = await roomService.getRoomAvailability(room.roomId)
              availabilityMap[room.roomId] = availabilityResponse.data
            } catch (err) {
              console.warn(`Failed to get availability for room ${room.roomId}:`, err)
              // Fallback to basic room data
              availabilityMap[room.roomId] = {
                roomId: room.roomId,
                roomNumber: room.roomNumber,
                totalBedCapacity: room.totalBeds || room.totalBedCapacity,
                currentOccupancy: room.activeAllotments || 0,
                tenantActionPendingCount: room.tenantActionPendingCount || 0,
                actualAvailableBeds: room.actualAvailableBeds || room.availableBeds,
                overbookingDetected: room.overbookingDetected || false,
                allocationStatus: room.allocationStatus || 'UNKNOWN'
              }
            }
          }))

          setAvailabilityData(availabilityMap)
          setEnhancedRooms(floorRooms)
        } catch (enhancedError) {
          console.warn('Enhanced room availability not available, using basic room data:', enhancedError)
          setEnhancedRooms([])
          setAvailabilityData({})
        }
      } catch (err) {
        const errorData = err?.response?.data
        setError(errorData?.message || 'Failed to load rooms.')
      } finally {
        setLoading(false)
      }
    }

    fetchRoomsWithAvailability()
  }, [hostelId, floorId])

  const closeTenantPanel = () => {
    setShowTenantPanel(false)
    setSelectedRoom(null)
    setRoomTenants([])
    setTenantError('')
    setTenantLoading(false)
  }

  // Helper function to get enhanced room data or fallback to basic room data
  const getRoomDisplayData = (room) => {
    const availability = availabilityData[room.roomId]
    if (availability) {
      return {
        ...room,
        totalBeds: availability.totalBedCapacity || room.totalBeds,
        availableBeds: availability.actualAvailableBeds,
        currentOccupancy: availability.currentOccupancy,
        tenantActionPendingCount: availability.tenantActionPendingCount || 0,
        overbookingDetected: availability.overbookingDetected || false,
        allocationStatus: availability.allocationStatus,
        upcomingDepartures: availability.upcomingDepartures || []
      }
    }
    return {
      ...room,
      currentOccupancy: (room.totalBeds || 0) - (room.availableBeds || 0),
      tenantActionPendingCount: 0,
      overbookingDetected: false,
      allocationStatus: room.isActive ? 'AVAILABLE' : 'INACTIVE',
      upcomingDepartures: []
    }
  }

  // Enhanced room card status badge
  const getRoomStatusBadge = (roomData) => {
    if (!roomData.isActive) {
      return <Badge variant="warning">Inactive</Badge>
    }
    
    if (roomData.overbookingDetected) {
      return <Badge variant="error">Overbooked</Badge>
    }
    
    if (roomData.tenantActionPendingCount > 0) {
      return <Badge variant="warning">{roomData.tenantActionPendingCount} Pending</Badge>
    }
    
    if (roomData.availableBeds === 0) {
      return <Badge variant="error">Full</Badge>
    }
    
    return <Badge variant="success">Available</Badge>
  }

  // Get occupancy status color
  const getOccupancyColor = (roomData) => {
    if (roomData.overbookingDetected) return 'text-red-600'
    if (roomData.availableBeds === 0) return 'text-orange-600'
    return 'text-slate-950'
  }

  const handleRoomClick = async (room) => {
    setSelectedRoom(room)
    setShowTenantPanel(true)
    setTenantLoading(true)
    setTenantError('')
    setRoomTenants([])

    try {
      const response = await roomService.getRoomTenants(hostelId, floorId, room.roomId)
      setRoomTenants(response.data || [])
    } catch (err) {
      const errorData = err?.response?.data
      setTenantError(errorData?.message || 'Failed to load tenants for this room.')
    } finally {
      setTenantLoading(false)
    }
  }

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Room inventory"
        title={`${hostelName} · Floor ${floorNumber}`}
        description="Track room capacity, availability, and active status in one structured list."
        action={
          <Button
            label="Add room"
            onClick={() =>
              navigate(`/owner/hostels/${hostelId}/floors/${floorId}/add-room`, {
                state: { hostelId, floorId, hostelName, floorNumber },
              })
            }
          />
        }
        secondaryAction={
          <Button
            label="Back to floors"
            variant="secondary"
            onClick={() => navigate(`/owner/hostels/${hostelId}/floors`, { state: { hostelId, hostelName } })}
          />
        }
      />

      {error ? <Alert tone="error">{error}</Alert> : null}

      {loading ? (
        <div className="grid gap-4 xl:grid-cols-2">
          {Array.from({ length: 4 }).map((_, index) => (
            <Skeleton key={index} className="h-48 rounded-3xl" />
          ))}
        </div>
      ) : rooms.length === 0 ? (
        <EmptyState
          icon={<DoorIcon className="h-5 w-5" />}
          title="No rooms created yet"
          description="Add the first room on this floor to begin managing bed inventory and availability."
          actionLabel="Create first room"
          onAction={() =>
            navigate(`/owner/hostels/${hostelId}/floors/${floorId}/add-room`, {
              state: { hostelId, floorId, hostelName, floorNumber },
            })
          }
        />
      ) : (
        <Card>
          <CardHeader 
            title="Rooms" 
            description="Enhanced room availability with bed counts, occupancy status, and overbooking detection." 
          />
          <CardContent>
            <div className="grid gap-4 xl:grid-cols-2">
              {rooms.map((room) => {
                const roomData = getRoomDisplayData(room)
                return (
                  <button
                    key={room.roomId}
                    type="button"
                    onClick={() => handleRoomClick(room)}
                    className={`rounded-3xl border p-5 text-left transition duration-200 hover:-translate-y-0.5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500/40 ${
                      roomData.overbookingDetected 
                        ? 'border-red-200 bg-red-50/80 hover:border-red-300 hover:bg-red-100/80' 
                        : roomData.tenantActionPendingCount > 0
                        ? 'border-orange-200 bg-orange-50/80 hover:border-orange-300 hover:bg-orange-100/80'
                        : 'border-slate-200 bg-slate-50/80 hover:border-sky-200 hover:bg-sky-50/80'
                    }`}
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div>
                        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Room</p>
                        <h3 className="mt-2 text-xl font-semibold text-slate-950">{room.roomNumber}</h3>
                      </div>
                      <div className="flex flex-col items-end gap-2">
                        {getRoomStatusBadge(roomData)}
                        {roomData.overbookingDetected && (
                          <div className="flex items-center gap-1 text-xs text-red-600">
                            <AlertTriangleIcon className="h-3 w-3" />
                            <span>Overbooked</span>
                          </div>
                        )}
                      </div>
                    </div>

                    <div className="mt-5 grid gap-4 sm:grid-cols-3">
                      <div className="rounded-2xl bg-white px-4 py-3 shadow-sm">
                        <div className="flex items-center gap-2">
                          <BedIcon className="h-4 w-4 text-slate-400" />
                          <p className="text-xs uppercase tracking-[0.18em] text-slate-400">Total beds</p>
                        </div>
                        <p className="mt-2 text-2xl font-semibold text-slate-950">{roomData.totalBeds ?? '-'}</p>
                      </div>
                      
                      <div className="rounded-2xl bg-white px-4 py-3 shadow-sm">
                        <div className="flex items-center gap-2">
                          <UsersIcon className="h-4 w-4 text-slate-400" />
                          <p className="text-xs uppercase tracking-[0.18em] text-slate-400">Occupied</p>
                        </div>
                        <p className={`mt-2 text-2xl font-semibold ${getOccupancyColor(roomData)}`}>
                          {roomData.currentOccupancy ?? '-'}
                        </p>
                      </div>
                      
                      <div className="rounded-2xl bg-white px-4 py-3 shadow-sm">
                        <div className="flex items-center gap-2">
                          <DoorIcon className="h-4 w-4 text-slate-400" />
                          <p className="text-xs uppercase tracking-[0.18em] text-slate-400">Available</p>
                        </div>
                        <p className={`mt-2 text-2xl font-semibold ${getOccupancyColor(roomData)}`}>
                          {roomData.availableBeds ?? '-'}
                        </p>
                      </div>
                    </div>

                    {/* Additional info for enhanced rooms */}
                    {roomData.tenantActionPendingCount > 0 && (
                      <div className="mt-4 rounded-2xl bg-orange-100 px-4 py-3">
                        <div className="flex items-center gap-2">
                          <AlertTriangleIcon className="h-4 w-4 text-orange-600" />
                          <p className="text-sm font-medium text-orange-800">
                            {roomData.tenantActionPendingCount} tenant{roomData.tenantActionPendingCount > 1 ? 's' : ''} pending action
                          </p>
                        </div>
                      </div>
                    )}

                    <p className="mt-5 text-sm leading-6 text-slate-500">
                      {room.roomDetails || 'No additional room details provided yet.'}
                    </p>
                  </button>
                )
              })}
            </div>
          </CardContent>
        </Card>
      )}

      {showTenantPanel && selectedRoom ? (
      <CenteredModal open onClose={closeTenantPanel}>
          <Card className="w-full overflow-hidden shadow-2xl">
            <div className="flex items-start justify-between gap-4 border-b border-slate-100 px-6 py-5">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Room tenants</p>
                <h3 className="mt-2 text-xl font-semibold text-slate-950">Room {selectedRoom.roomNumber}</h3>
                <p className="mt-1 text-sm text-slate-500">{hostelName} · Floor {floorNumber}</p>
                
                {/* Enhanced room info */}
                {availabilityData[selectedRoom.roomId] && (
                  <div className="mt-4 flex flex-wrap gap-4 text-sm">
                    <div className="flex items-center gap-2">
                      <BedIcon className="h-4 w-4 text-slate-400" />
                      <span className="text-slate-600">
                        {availabilityData[selectedRoom.roomId].actualAvailableBeds}/{availabilityData[selectedRoom.roomId].totalBedCapacity} beds available
                      </span>
                    </div>
                    {availabilityData[selectedRoom.roomId].tenantActionPendingCount > 0 && (
                      <div className="flex items-center gap-2">
                        <AlertTriangleIcon className="h-4 w-4 text-orange-500" />
                        <span className="text-orange-600">
                          {availabilityData[selectedRoom.roomId].tenantActionPendingCount} pending actions
                        </span>
                      </div>
                    )}
                    {availabilityData[selectedRoom.roomId].overbookingDetected && (
                      <div className="flex items-center gap-2">
                        <AlertTriangleIcon className="h-4 w-4 text-red-500" />
                        <span className="text-red-600">Overbooked</span>
                      </div>
                    )}
                  </div>
                )}
              </div>
              <Button label="Close" variant="secondary" onClick={closeTenantPanel} />
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
                  {roomTenants.map((tenant) => (
                    <div key={tenant.tenantId || `${tenant.roomId}-${tenant.tenantName}`} className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
                      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                        <div>
                          <p className="text-base font-semibold text-slate-950">{tenant.tenantName || 'Unnamed tenant'}</p>
                          <p className="mt-1 text-sm text-slate-500">Room {selectedRoom.roomNumber}</p>
                        </div>
                        <Badge variant={
                          tenant.roomAllotmentStatus === 'ALLOTMENT_ACTION_PENDING' ? 'warning' : 
                          tenant.roomAllotmentStatus === 'ACTIVE' ? 'success' : 'default'
                        }>
                          {tenant.roomAllotmentStatus || 'ACTIVE'}
                        </Badge>
                      </div>

                      <div className="mt-4 grid gap-3 sm:grid-cols-2">
                        <div>
                          <p className="text-xs uppercase tracking-[0.18em] text-slate-400">Floor</p>
                          <p className="mt-1 text-sm font-medium text-slate-900">Floor {selectedRoom.floorNumber ?? floorNumber ?? '-'}</p>
                        </div>
                        <div>
                          <p className="text-xs uppercase tracking-[0.18em] text-slate-400">Allotment Date</p>
                          <p className="mt-1 text-sm font-medium text-slate-900">
                            {tenant.allotmentDate ? new Date(tenant.allotmentDate).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '-'}
                          </p>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </Card>
      </CenteredModal>
      ) : null}
    </div>
  )
}

export default Rooms
