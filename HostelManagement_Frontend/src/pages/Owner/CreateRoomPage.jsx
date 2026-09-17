import { useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { roomService } from '../../services/hostelService'
import FormInput from '../../components/FormInput'
import FormSelect from '../../components/FormSelect'
import { Alert, Button, Card, CardContent, CardHeader, PageHeader } from '../../components/ui'

const MAX_ROOMS_PER_BATCH = 100

const CreateRoomPage = () => {
  const { hostelId: paramHostelId, floorId: paramFloorId } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const hostelId = location.state?.hostelId || paramHostelId
  const floorId = location.state?.floorId || paramFloorId
  const hostelName = location.state?.hostelName || `Hostel ${hostelId}`
  const floorNumber = location.state?.floorNumber || floorId
  const [rangeStart, setRangeStart] = useState('')
  const [rangeEnd, setRangeEnd] = useState('')
  const [defaults, setDefaults] = useState({ roomType: '', totalBeds: '', isActive: true })
  const [rooms, setRooms] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [alert, setAlert] = useState(null)

  const roomTypeOptions = [
    { value: 'PG_ROOM', label: 'PG Room' },
    { value: 'FLAT', label: 'Flat' },
  ]

  const updateDefault = (event) => {
    const { name, value, type, checked } = event.target
    setDefaults((current) => ({ ...current, [name]: type === 'checkbox' ? checked : value }))
  }

  const createRange = () => {
    setError('')
    const startMatch = rangeStart.trim().match(/^(\d+)(.*)$/)
    const endMatch = rangeEnd.trim().match(/^(\d+)(.*)$/)

    if (!startMatch || !endMatch || startMatch[2] !== endMatch[2]) {
      setError('Use matching room-number formats, such as 2D to 7D or 1R to 10R.')
      return
    }

    const start = Number(startMatch[1])
    const end = Number(endMatch[1])
    if (start < 1 || end < start) {
      setError('The end room number must be the same as or higher than the start room number.')
      return
    }

    const count = end - start + 1
    if (count > MAX_ROOMS_PER_BATCH) {
      setError(`You can add up to ${MAX_ROOMS_PER_BATCH} rooms at a time. Please use a smaller range.`)
      return
    }

    const suffix = startMatch[2]
    setRooms(Array.from({ length: count }, (_, index) => ({
      id: `${start + index}${suffix}`,
      roomNumber: `${start + index}${suffix}`,
      roomType: defaults.roomType,
      totalBeds: defaults.totalBeds,
      isActive: defaults.isActive,
    })))
  }

  const updateRoom = (id, field, value) => {
    setRooms((current) => current.map((room) => (
      room.id === id ? { ...room, [field]: value } : room
    )))
  }

  const removeRoom = (id) => setRooms((current) => current.filter((room) => room.id !== id))

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setAlert(null)

    if (rooms.length === 0) {
      setError('Set a room-number range to create the room table first.')
      return
    }

    const roomNumbers = rooms.map((room) => room.roomNumber.trim())
    const duplicateRoomNumbers = roomNumbers.filter((roomNumber, index) => roomNumbers.indexOf(roomNumber) !== index)
    const invalidRoom = rooms.find((room) => !room.roomNumber.trim() || !room.roomType || !Number.isInteger(Number(room.totalBeds)) || Number(room.totalBeds) < 1)

    if (invalidRoom) {
      setError('Every row needs a room number, type, and whole total-bed count of at least 1.')
      return
    }
    if (duplicateRoomNumbers.length > 0) {
      setError(`Room number ${duplicateRoomNumbers[0]} is duplicated. Room numbers must be unique on this floor.`)
      return
    }

    setLoading(true)
    let createdRooms = []
    try {
      const existingResponse = await roomService.getRoomsByFloor(hostelId, floorId)
      const existingRoomNumbers = new Set((existingResponse.data || []).map((room) => room.roomNumber))
      const conflictingRoom = roomNumbers.find((roomNumber) => existingRoomNumbers.has(roomNumber))
      if (conflictingRoom) {
        setError(`Room ${conflictingRoom} already exists on this floor. Update the table and try again.`)
        return
      }

      for (const room of rooms) {
        await roomService.createRoom(hostelId, floorId, {
          roomNumber: room.roomNumber.trim(),
          roomDetails: '',
          roomType: room.roomType,
          totalBeds: Number(room.totalBeds),
          availableBeds: Number(room.totalBeds),
          isActive: room.isActive,
        })
        createdRooms = [...createdRooms, room.roomNumber.trim()]
      }

      setAlert({ tone: 'success', message: `✅ ${rooms.length} room${rooms.length === 1 ? '' : 's'} created successfully and ready for tenants.` })
      setTimeout(() => {
        setAlert(null)
        navigate(`/owner/hostels/${hostelId}/floors`, { state: { hostelId, floorId, hostelName, floorNumber } })
      }, 3000)
    } catch (err) {
      const errorData = err.response?.data
      const failureMessage = errorData && typeof errorData === 'object' && !errorData.message
        ? Object.values(errorData).join(', ')
        : errorData?.message
      setError(createdRooms.length > 0
        ? `Created rooms ${createdRooms.join(', ')}, but the remaining rooms could not be created. ${failureMessage || 'Please review the room list and try again.'}`
        : failureMessage || 'Failed to add rooms.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="space-y-8">
      {alert ? <div className="fixed top-4 right-4 z-[9999] max-w-md"><Alert tone={alert.tone} className="border-2 shadow-lg">{alert.message}</Alert></div> : null}
      <PageHeader
        eyebrow="Room setup"
        title={`Add rooms to floor ${floorNumber}`}
        description={`Create a complete set of rooms for ${hostelName} in one step.`}
        secondaryAction={<Button label="Back to rooms" variant="secondary" onClick={() => navigate(`/owner/hostels/${hostelId}/floors`, { state: { hostelId, floorId, hostelName, floorNumber } })} />}
      />

      <div className="max-w-5xl">
        <Card>
          <CardHeader title="Create room range" description="Set defaults, then generate an editable room table from a numbering range." />
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-6">
              {error ? <Alert tone="error">{error}</Alert> : null}
              <div className="rounded-2xl border border-sky-100 bg-sky-50/70 p-4">
                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
                  <FormInput label="Start room number" name="rangeStart" value={rangeStart} onChange={(event) => setRangeStart(event.target.value)} placeholder="2D" />
                  <FormInput label="End room number" name="rangeEnd" value={rangeEnd} onChange={(event) => setRangeEnd(event.target.value)} placeholder="7D" />
                  <FormSelect label="Default type" name="roomType" value={defaults.roomType} onChange={updateDefault} options={roomTypeOptions} placeholder="Select type" />
                  <FormInput label="Default total beds" name="totalBeds" type="number" min={1} step={1} value={defaults.totalBeds} onChange={updateDefault} placeholder="2" />
                  <label className="flex items-end gap-3 pb-3 text-sm font-medium text-slate-700"><input type="checkbox" name="isActive" checked={defaults.isActive} onChange={updateDefault} className="h-4 w-4 rounded border-slate-300 text-sky-600 focus:ring-sky-500" />Active by default</label>
                </div>
                <div className="mt-4 flex flex-col gap-3 border-t border-sky-100 pt-4 sm:flex-row sm:items-center sm:justify-between">
                  <p className="text-sm text-slate-600">Examples: <span className="font-medium">2D–7D</span> creates 2D through 7D; <span className="font-medium">1R–10R</span> creates 1R through 10R.</p>
                  <Button type="button" label="Generate rooms" onClick={createRange} />
                </div>
              </div>

              {rooms.length > 0 ? (
                <div className="overflow-x-auto rounded-2xl border border-slate-200">
                  <table className="w-full min-w-[700px] text-left text-sm">
                    <thead className="bg-slate-50 text-xs uppercase tracking-[0.12em] text-slate-500"><tr><th className="px-4 py-3 font-semibold">Room number</th><th className="px-4 py-3 font-semibold">Type</th><th className="px-4 py-3 font-semibold">Total beds</th><th className="px-4 py-3 font-semibold">Is active</th><th className="w-16 px-4 py-3"><span className="sr-only">Remove row</span></th></tr></thead>
                    <tbody className="divide-y divide-slate-100 bg-white">
                      {rooms.map((room) => (
                        <tr key={room.id}>
                          <td className="px-4 py-3"><input aria-label={`Room number for ${room.roomNumber}`} value={room.roomNumber} onChange={(event) => updateRoom(room.id, 'roomNumber', event.target.value)} className="h-10 w-full rounded-lg border border-slate-200 px-3 outline-none focus:border-sky-400 focus:ring-2 focus:ring-sky-100" /></td>
                          <td className="px-4 py-3"><select aria-label={`Type for ${room.roomNumber}`} value={room.roomType} onChange={(event) => updateRoom(room.id, 'roomType', event.target.value)} className="h-10 w-full rounded-lg border border-slate-200 bg-white px-3 outline-none focus:border-sky-400 focus:ring-2 focus:ring-sky-100"><option value="">Select type</option>{roomTypeOptions.map((type) => <option key={type.value} value={type.value}>{type.label}</option>)}</select></td>
                          <td className="px-4 py-3"><input aria-label={`Total beds for ${room.roomNumber}`} type="number" min={1} step={1} value={room.totalBeds} onChange={(event) => updateRoom(room.id, 'totalBeds', event.target.value)} className="h-10 w-20 rounded-lg border border-slate-200 px-3 outline-none focus:border-sky-400 focus:ring-2 focus:ring-sky-100" /></td>
                          <td className="px-4 py-3"><label className="inline-flex items-center gap-2 text-slate-700"><input aria-label={`Active status for ${room.roomNumber}`} type="checkbox" checked={room.isActive} onChange={(event) => updateRoom(room.id, 'isActive', event.target.checked)} className="h-4 w-4 rounded border-slate-300 text-sky-600 focus:ring-sky-500" />Active</label></td>
                          <td className="px-4 py-3 text-right"><button type="button" onClick={() => removeRoom(room.id)} className="text-sm font-medium text-rose-600 hover:text-rose-700">Remove</button></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : null}

              <div className="flex flex-col gap-3 sm:flex-row">
                <Button type="submit" label={rooms.length > 1 ? `Create ${rooms.length} rooms` : 'Create room'} loading={loading} fullWidth />
                <Button type="button" label="Cancel" variant="secondary" fullWidth onClick={() => navigate(`/owner/hostels/${hostelId}/floors`, { state: { hostelId, floorId, hostelName, floorNumber } })} />
              </div>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

export default CreateRoomPage
