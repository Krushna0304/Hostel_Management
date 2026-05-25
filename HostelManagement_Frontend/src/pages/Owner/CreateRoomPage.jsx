import { useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { roomService } from '../../services/hostelService'
import FormInput from '../../components/FormInput'
import FormSelect from '../../components/FormSelect'
import { Alert, Button, Card, CardContent, CardHeader, PageHeader } from '../../components/ui'

const CreateRoomPage = () => {
  const { hostelId: paramHostelId, floorId: paramFloorId } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const hostelId = location.state?.hostelId || paramHostelId
  const floorId = location.state?.floorId || paramFloorId
  const hostelName = location.state?.hostelName || `Hostel ${hostelId}`
  const floorNumber = location.state?.floorNumber || floorId
  const [form, setForm] = useState({
    roomNumber: '',
    roomDetails: '',
    roomType: '',
    totalBeds: '',
    availableBeds: '',
    isActive: true,
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [alert, setAlert] = useState(null) // { tone, message }

  // Room type options matching the backend enum
  const roomTypeOptions = [
    { value: 'PG_ROOM', label: 'PG Room' },
    { value: 'FLAT', label: 'Flat' },
  ]

  const handleChange = (event) => {
    const { name, value, type, checked } = event.target
    setForm((current) => ({
      ...current,
      [name]: type === 'checkbox' ? checked : value,
    }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setAlert(null)

    if (!form.roomNumber || !form.roomType || !form.totalBeds || !form.availableBeds) {
      setError('Room number, room type, total beds, and available beds are required.')
      return
    }

    if (Number(form.availableBeds) > Number(form.totalBeds)) {
      setError('Available beds cannot be greater than total beds.')
      return
    }

    setLoading(true)
    try {
      await roomService.createRoom(hostelId, floorId, {
        roomNumber: form.roomNumber,
        roomDetails: form.roomDetails,
        roomType: form.roomType,
        totalBeds: Number(form.totalBeds),
        availableBeds: Number(form.availableBeds),
        isActive: form.isActive,
      })

      // Show success popup
      setAlert({ tone: 'success', message: '✅ Room Added Successfully! Your room has been created and is ready for tenants.' })
      
      // Auto-hide success message and navigate after 3 seconds
      setTimeout(() => {
        setAlert(null)
        navigate(`/owner/hostels/${hostelId}/floors`, {
          state: { hostelId, floorId, hostelName, floorNumber },
        })
      }, 3000)
    } catch (err) {
      const errorData = err.response?.data
      if (errorData && typeof errorData === 'object' && !errorData.message) {
        setError(Object.values(errorData).join(', ') || 'Failed to add room.')
      } else {
        setError(errorData?.message || 'Failed to add room.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="space-y-8">
      {alert ? (
        <div className="fixed top-4 right-4 z-[9999] max-w-md">
          <Alert 
            tone={alert.tone} 
            onClose={() => setAlert(null)}
            className="shadow-lg border-2"
          >
            {alert.message}
          </Alert>
        </div>
      ) : null}

      <PageHeader
        eyebrow="Room setup"
        title={`Add a room to floor ${floorNumber}`}
        description={`Create a structured room record for ${hostelName}, including beds, availability, and operational status.`}
        secondaryAction={
          <Button
            label="Back to rooms"
            variant="secondary"
            onClick={() =>
              navigate(`/owner/hostels/${hostelId}/floors`, {
                state: { hostelId, floorId, hostelName, floorNumber },
              })
            }
          />
        }
      />

      <div className="max-w-3xl">
        <Card>
          <CardHeader title="Room details" description="Use clear identifiers and accurate bed counts to keep occupancy reporting reliable." />
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-5">
              {error ? <Alert tone="error">{error}</Alert> : null}

              <div className="grid gap-5 md:grid-cols-2">
                <FormInput
                  label="Room number"
                  name="roomNumber"
                  value={form.roomNumber}
                  onChange={handleChange}
                  placeholder="A-101"
                  required
                />
                <FormSelect
                  label="Room type"
                  name="roomType"
                  value={form.roomType}
                  onChange={handleChange}
                  options={roomTypeOptions}
                  placeholder="Select room type"
                  required
                />
                <FormInput
                  label="Room details"
                  name="roomDetails"
                  value={form.roomDetails}
                  onChange={handleChange}
                  placeholder="Near staircase, dual occupancy"
                />
                <FormInput
                  label="Total beds"
                  name="totalBeds"
                  type="number"
                  value={form.totalBeds}
                  onChange={handleChange}
                  required
                  min={1}
                />
                <FormInput
                  label="Available beds"
                  name="availableBeds"
                  type="number"
                  value={form.availableBeds}
                  onChange={handleChange}
                  required
                  min={0}
                />
              </div>

              <label className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                <input
                  type="checkbox"
                  name="isActive"
                  checked={form.isActive}
                  onChange={handleChange}
                  className="h-4 w-4 rounded border-slate-300 text-sky-600 focus:ring-sky-500"
                />
                <div>
                  <p className="text-sm font-medium text-slate-900">Room is active</p>
                  <p className="text-sm text-slate-500">Inactive rooms stay visible but won’t appear as active inventory.</p>
                </div>
              </label>

              <div className="flex flex-col gap-3 pt-4 sm:flex-row">
                <Button type="submit" label="Add room" loading={loading} fullWidth />
                <Button
                  type="button"
                  label="Cancel"
                  variant="secondary"
                  fullWidth
                  onClick={() =>
                    navigate(`/owner/hostels/${hostelId}/floors`, {
                      state: { hostelId, floorId, hostelName, floorNumber },
                    })
                  }
                />
              </div>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

export default CreateRoomPage
