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
} from '../../components/ui'
import { DoorIcon } from '../../components/icons/AppIcons'

const Rooms = () => {
  const { hostelId: paramHostelId, floorId: paramFloorId } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const hostelId = location.state?.hostelId || paramHostelId
  const floorId = location.state?.floorId || paramFloorId
  const floorNumber = location.state?.floorNumber || floorId
  const hostelName = location.state?.hostelName || `Hostel ${hostelId}`
  const [rooms, setRooms] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedRoom, setSelectedRoom] = useState(null)
  const [roomTenants, setRoomTenants] = useState([])
  const [tenantLoading, setTenantLoading] = useState(false)
  const [tenantError, setTenantError] = useState('')
  const [showTenantPanel, setShowTenantPanel] = useState(false)

  useEffect(() => {
    const fetchRooms = async () => {
      setLoading(true)
      setError('')
      try {
        const response = await roomService.getRoomsByFloor(hostelId, floorId)
        setRooms(response.data || [])
      } catch (err) {
        const errorData = err?.response?.data
        setError(errorData?.message || 'Failed to load rooms.')
      } finally {
        setLoading(false)
      }
    }

    fetchRooms()
  }, [hostelId, floorId])

  const closeTenantPanel = () => {
    setShowTenantPanel(false)
    setSelectedRoom(null)
    setRoomTenants([])
    setTenantError('')
    setTenantLoading(false)
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
          <CardHeader title="Rooms" description="Compact, readable cards optimized for scanning occupancy and bed availability." />
          <CardContent>
            <div className="grid gap-4 xl:grid-cols-2">
              {rooms.map((room) => (
                <button
                  key={room.roomId}
                  type="button"
                  onClick={() => handleRoomClick(room)}
                  className="rounded-3xl border border-slate-200 bg-slate-50/80 p-5 text-left transition duration-200 hover:-translate-y-0.5 hover:border-sky-200 hover:bg-sky-50/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500/40"
                >
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Room</p>
                      <h3 className="mt-2 text-xl font-semibold text-slate-950">{room.roomNumber}</h3>
                    </div>
                    <Badge variant={room.isActive ? 'success' : 'warning'}>
                      {room.isActive ? 'Active' : 'Inactive'}
                    </Badge>
                  </div>

                  <div className="mt-5 grid gap-4 sm:grid-cols-2">
                    <div className="rounded-2xl bg-white px-4 py-3 shadow-sm">
                      <p className="text-xs uppercase tracking-[0.18em] text-slate-400">Total beds</p>
                      <p className="mt-2 text-2xl font-semibold text-slate-950">{room.totalBeds ?? '-'}</p>
                    </div>
                    <div className="rounded-2xl bg-white px-4 py-3 shadow-sm">
                      <p className="text-xs uppercase tracking-[0.18em] text-slate-400">Available beds</p>
                      <p className="mt-2 text-2xl font-semibold text-slate-950">{room.availableBeds ?? '-'}</p>
                    </div>
                  </div>

                  <p className="mt-5 text-sm leading-6 text-slate-500">{room.roomDetails || 'No additional room details provided yet.'}</p>
                </button>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {showTenantPanel && selectedRoom ? (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-slate-950/45 px-4 py-6 sm:items-center">
          <div
            className="absolute inset-0"
            onClick={closeTenantPanel}
            aria-hidden="true"
          />
          <Card className="relative z-10 w-full max-w-2xl overflow-hidden">
            <div className="flex items-start justify-between gap-4 border-b border-slate-100 px-6 py-5">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Room tenants</p>
                <h3 className="mt-2 text-xl font-semibold text-slate-950">Room {selectedRoom.roomNumber}</h3>
                <p className="mt-1 text-sm text-slate-500">{hostelName} · Floor {floorNumber}</p>
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
                        <Badge variant="success">{tenant.roomAllotmentStatus || 'ACTIVE'}</Badge>
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
        </div>
      ) : null}
    </div>
  )
}

export default Rooms
