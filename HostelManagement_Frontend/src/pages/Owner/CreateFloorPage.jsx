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
  const [floorNumber, setFloorNumber] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [alert, setAlert] = useState(null) // { tone, message }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setAlert(null)

    if (!floorNumber || Number.isNaN(Number(floorNumber)) || Number(floorNumber) <= 0) {
      setError('Please enter a valid floor number.')
      return
    }

    setLoading(true)
    try {
      await floorService.createFloor(hostelId, { floorNumber: Number(floorNumber) })
      
      // Show success popup
      setAlert({ tone: 'success', message: '✅ Floor Added Successfully! Your floor has been created and is ready for rooms.' })
      
      // Auto-hide success message and navigate after 3 seconds
      setTimeout(() => {
        setAlert(null)
        navigate(`/owner/hostels/${hostelId}/floors`, { state: { hostelId, hostelName } })
      }, 3000)
    } catch (err) {
      const errorData = err.response?.data
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
        title={`Add a floor to ${hostelName}`}
        description="Keep the structure sequential and easy to scan so room management scales cleanly."
        secondaryAction={<Button label="Back to floors" variant="secondary" onClick={() => navigate(`/owner/hostels/${hostelId}/floors`, { state: { hostelId, hostelName } })} />}
      />

      <div className="max-w-2xl">
        <Card>
          <CardHeader title="Floor details" description="Each floor becomes a container for rooms and bed inventory." />
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-5">
              {error ? <Alert tone="error">{error}</Alert> : null}

              <FormInput
                label="Floor number"
                name="floorNumber"
                type="number"
                value={floorNumber}
                onChange={(event) => setFloorNumber(event.target.value)}
                min={1}
                required
                placeholder="1"
              />
              <p className="text-sm text-gray-600">
                Floor numbers must be unique within this hostel. Each floor can contain multiple rooms.
              </p>

              <div className="flex flex-col gap-3 sm:flex-row">
                <Button type="submit" label="Add floor" loading={loading} fullWidth />
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
