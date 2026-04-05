import { useState, useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import FormInput from '../../components/FormInput'
import FormSelect from '../../components/FormSelect'
import Button from '../../components/Button'
import { hostelService, floorService } from '../../services/hostelService'

const AddFloor = () => {
  const navigate = useNavigate()
  const location = useLocation()
  // Prefer state over query param for hostelId
  const defaultHostelId = location.state?.hostelId || ''
  const [hostels, setHostels] = useState([])
  const [formData, setFormData] = useState({
    hostelId: defaultHostelId,
    floorNumber: '',
  })
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)
  const [fetchLoading, setFetchLoading] = useState(true)
  const [apiError, setApiError] = useState('')

  useEffect(() => {
    fetchHostels()
  }, [])

  const fetchHostels = async () => {
    try {
      setFetchLoading(true)
      // TODO: Verify endpoint returns correct data structure
      const response = await hostelService.getAllHostels()
      setHostels(response.data || [])
    } catch (err) {
      console.error('Failed to fetch hostels:', err)
      const errorData = err?.response?.data
      setApiError(errorData?.message || 'Failed to load hostels. Please try again.')
    } finally {
      setFetchLoading(false)
    }
  }

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }))
    if (errors[name]) {
      setErrors((prev) => ({
        ...prev,
        [name]: '',
      }))
    }
  }

  const validateForm = () => {
    const newErrors = {}
    if (!formData.hostelId) newErrors.hostelId = 'Please select a hostel'
    if (!formData.floorNumber || formData.floorNumber <= 0) newErrors.floorNumber = 'Floor number must be greater than 0'
    return newErrors
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const newErrors = validateForm()

    if (!formData.hostelId) {
      setApiError('Hostel is not selected. Please go back and select a hostel.');
      return;
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors)
      return
    }

    setLoading(true)
    setApiError('')

    try {
      await floorService.createFloor(formData.hostelId, {
        hostelId: formData.hostelId,
        floorNumber: parseInt(formData.floorNumber),
      })

      // Success - redirect to dashboard
      navigate('/owner/dashboard', { state: { message: 'Floor added successfully!' } })
    } catch (error) {
      console.error('Failed to add floor:', error)
      const errorData = error.response?.data
      
      // Check if error is field-specific validation errors (object with field names as keys)
      if (errorData && typeof errorData === 'object' && !errorData.message) {
        // It's field-specific errors like {"floorNumber": "Floor number is required"}
        const fieldErrors = {}
        Object.keys(errorData).forEach(field => {
          fieldErrors[field] = errorData[field]
        })
        setErrors(prev => ({ ...prev, ...fieldErrors }))
        setApiError('') // Clear general error, show field errors instead
      } else {
        // It's a general error message
        setApiError(errorData?.message || 'Failed to add floor. Please try again.')
        setErrors({}) // Clear field errors
      }
    } finally {
      setLoading(false)
    }
  }

  const hostelOptions = hostels.map((hostel) => ({
    value: hostel.hostelId,
    label: hostel.hostelName,
  }))

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm sticky top-0 z-10">
        <div className="max-w-2xl mx-auto px-4 py-4">
          <h1 className="text-2xl font-bold text-gray-800">Add Floor</h1>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-2xl mx-auto px-4 py-8">
        <div className="bg-white rounded-lg shadow p-6">
          {fetchLoading ? (
            <div className="text-center py-8 text-gray-600">Loading hostels...</div>
          ) : hostels.length === 0 ? (
            <div className="text-center py-8">
              <p className="text-gray-600 mb-4">No hostels available. Please create a hostel first.</p>
              <Button label="Create Hostel" onClick={() => navigate('/owner/create-hostel')} />
            </div>
          ) : (
            <form onSubmit={handleSubmit}>
              {/* API Error */}
              {apiError && (
                <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded-lg text-sm">
                  {apiError}
                </div>
              )}

              {/* Select Hostel (only if not preselected) */}
              {!defaultHostelId && (
                <FormSelect
                  label="Select Hostel"
                  name="hostelId"
                  value={formData.hostelId}
                  onChange={handleChange}
                  options={hostelOptions}
                  placeholder="Choose a hostel"
                  required
                  error={errors.hostelId}
                />
              )}

              {/* Floor Number */}
              <FormInput
                label="Floor Number"
                name="floorNumber"
                type="number"
                value={formData.floorNumber}
                onChange={handleChange}
                placeholder="e.g., 1, 2, 3"
                required
                error={errors.floorNumber}
              />

              {/* ...existing code... */}

              {/* Action Buttons */}
              <div className="flex gap-4 mt-8">
                <Button
                  type="submit"
                  label={loading ? 'Adding Floor...' : 'Add Floor'}
                  disabled={loading}
                  fullWidth
                />
                <Button
                  type="button"
                  label="Cancel"
                  onClick={() => navigate('/owner/dashboard')}
                  variant="secondary"
                  fullWidth
                />
              </div>
            </form>
          )}

          {/* TODO: Add floor details summary or guidelines */}
        </div>
      </main>
    </div>
  )
}

export default AddFloor
