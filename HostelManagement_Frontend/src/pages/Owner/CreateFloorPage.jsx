import { useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { floorService } from '../../services/hostelService'
import FormInput from '../../components/FormInput'
import { Alert, Button, Card, CardContent, CardHeader, PageHeader } from '../../components/ui'

const CreateFloorPage = () => {
  const { hostelId } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const hostelName = location.state?.hostelName || `Hostel ${hostelId}`
  const [startFloorNumber, setStartFloorNumber] = useState('')
  const [endFloorNumber, setEndFloorNumber] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [alert, setAlert] = useState(null) // { tone, message }

  const startFloor = Number(startFloorNumber)
  const endFloor = Number(endFloorNumber)
  const hasCompleteRange = startFloorNumber !== '' && endFloorNumber !== ''
  const isValidRange = hasCompleteRange
    && Number.isInteger(startFloor)
    && Number.isInteger(endFloor)
    && startFloor > 0
    && endFloor >= startFloor
  const floorCount = isValidRange ? endFloor - startFloor + 1 : 0

  const getRequestedFloors = () => Array.from(
    { length: floorCount },
    (_, index) => startFloor + index,
  )

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setAlert(null)

    if (!hasCompleteRange || !Number.isInteger(startFloor) || !Number.isInteger(endFloor) || startFloor <= 0 || endFloor <= 0) {
      setError('Enter whole floor numbers greater than 0.')
      return
    }

    if (endFloor < startFloor) {
      setError('End floor number must be the same as or higher than the start floor number.')
      return
    }

    if (floorCount > 100) {
      setError('You can create up to 100 floors at a time. Please use a smaller range.')
      return
    }

    setLoading(true)
    let createdFloors = []
    try {
      const requestedFloors = getRequestedFloors()
      const floorsResponse = await floorService.getFloorsByHostel(hostelId)
      const existingFloorNumbers = new Set(
        (floorsResponse.data || []).map((floor) => Number(floor.floorNumber)),
      )
      const duplicateFloors = requestedFloors.filter((floorNumber) => existingFloorNumbers.has(floorNumber))

      if (duplicateFloors.length > 0) {
        setError(`Floor${duplicateFloors.length > 1 ? 's' : ''} ${duplicateFloors.join(', ')} already exist${duplicateFloors.length === 1 ? 's' : ''}. Choose a range with unique floor numbers.`)
        return
      }

      for (const floorNumber of requestedFloors) {
        await floorService.createFloor(hostelId, { floorNumber })
        createdFloors = [...createdFloors, floorNumber]
      }
      
      const floorLabel = floorCount === 1 ? `Floor ${startFloor}` : `Floors ${startFloor}–${endFloor}`
      setAlert({
        tone: 'success',
        message: `✅ ${floorLabel} ${floorCount === 1 ? 'has' : 'have'} been created and ${floorCount === 1 ? 'is' : 'are'} ready for rooms.`,
      })
      
      // Auto-hide success message and navigate after 3 seconds
      setTimeout(() => {
        setAlert(null)
        navigate(`/owner/hostels/${hostelId}/floors`, { state: { hostelId, hostelName } })
      }, 3000)
    } catch (err) {
      const errorData = err.response?.data
      const failureMessage = errorData && typeof errorData === 'object' && !errorData.message
        ? Object.values(errorData).join(', ')
        : errorData?.message

      if (createdFloors.length > 0) {
        setError(`Created floors ${createdFloors.join(', ')}, but the remaining floors could not be created. ${failureMessage || 'Please review the floor list and try again.'}`)
        return
      }

      if (errorData && typeof errorData === 'object' && !errorData.message) {
        setError(Object.values(errorData).join(', ') || 'Failed to add floor.')
      } else {
        setError(errorData?.message || 'Failed to add floor.')
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
            onClose={() => {
              setAlert(null)
              navigate(`/owner/hostels/${hostelId}/floors`, { state: { hostelId, hostelName } })
            }}
            className="shadow-lg border-2"
          >
            {alert.message}
          </Alert>
        </div>
      ) : null}

      <PageHeader
        eyebrow="Property setup"
        title={`Add floors to ${hostelName}`}
        description="Create one floor or a complete sequential range for faster property setup."
        secondaryAction={<Button label="Back to floors" variant="secondary" onClick={() => navigate(`/owner/hostels/${hostelId}/floors`, { state: { hostelId, hostelName } })} />}
      />

      <div className="max-w-2xl">
        <Card>
          <CardHeader title="Floor range" description="Create one floor or every floor in an inclusive range. Each floor becomes a container for rooms and bed inventory." />
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-5">
              {error ? <Alert tone="error">{error}</Alert> : null}

              <div className="grid gap-4 sm:grid-cols-2">
                <FormInput
                  label="Start floor number"
                  name="startFloorNumber"
                  type="number"
                  value={startFloorNumber}
                  onChange={(event) => setStartFloorNumber(event.target.value)}
                  min={1}
                  step={1}
                  required
                  placeholder="2"
                />
                <FormInput
                  label="End floor number"
                  name="endFloorNumber"
                  type="number"
                  value={endFloorNumber}
                  onChange={(event) => setEndFloorNumber(event.target.value)}
                  min={1}
                  step={1}
                  required
                  placeholder="5"
                />
              </div>
              <div className="rounded-xl border border-sky-100 bg-sky-50 px-4 py-3 text-sm text-slate-700">
                {isValidRange
                  ? <>This will create <span className="font-semibold">{floorCount} floor{floorCount === 1 ? '' : 's'}</span>: {getRequestedFloors().join(', ')}.</>
                  : 'For example, entering 2 to 5 creates floors 2, 3, 4, and 5.'}
              </div>
              <p className="text-sm text-gray-600">
                Floor numbers must be unique within this hostel. You can create up to 100 floors at once.
              </p>

              <div className="flex flex-col gap-3 sm:flex-row">
                <Button type="submit" label={floorCount > 1 ? `Create ${floorCount} floors` : 'Create floor'} loading={loading} fullWidth />
                <Button
                  type="button"
                  label="Cancel"
                  variant="secondary"
                  fullWidth
                  onClick={() => navigate(`/owner/hostels/${hostelId}/floors`, { state: { hostelId, hostelName } })}
                />
              </div>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

export default CreateFloorPage
