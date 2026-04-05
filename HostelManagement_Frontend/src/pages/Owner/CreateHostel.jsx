import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import FormInput from '../../components/FormInput'
import Button from '../../components/Button'
import { hostelService } from '../../services/hostelService'

const CreateHostel = () => {
  const navigate = useNavigate()
  const [formData, setFormData] = useState({
    hostelName: '',
    hostelAddress: '',
  })
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)
  const [apiError, setApiError] = useState('')

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
    if (!formData.hostelName.trim()) newErrors.hostelName = 'Hostel name is required'
    if (!formData.hostelAddress.trim()) newErrors.hostelAddress = 'Hostel address is required'
    return newErrors
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const newErrors = validateForm()

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors)
      return
    }

    setLoading(true)
    setApiError('')

    try {
      // TODO: Verify endpoint and response structure from backend
      await hostelService.createHostel({
        hostelName: formData.hostelName,
        hostelAddress: formData.hostelAddress,
      })

      // Success - redirect to dashboard
      navigate('/owner/dashboard', { state: { message: 'Hostel created successfully!' } })
    } catch (error) {
      console.error('Failed to create hostel:', error)
      const errorData = error.response?.data
      
      // Check if error is field-specific validation errors (object with field names as keys)
      if (errorData && typeof errorData === 'object' && !errorData.message) {
        // It's field-specific errors like {"hostelName": "Hostel name is required"}
        const fieldErrors = {}
        Object.keys(errorData).forEach(field => {
          fieldErrors[field] = errorData[field]
        })
        setErrors(prev => ({ ...prev, ...fieldErrors }))
        setApiError('') // Clear general error, show field errors instead
      } else {
        // It's a general error message
        setApiError(errorData?.message || 'Failed to create hostel. Please try again.')
        setErrors({}) // Clear field errors
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm sticky top-0 z-10">
        <div className="max-w-2xl mx-auto px-4 py-4">
          <h1 className="text-2xl font-bold text-gray-800">Create New Hostel</h1>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-2xl mx-auto px-4 py-8">
        <div className="bg-white rounded-lg shadow p-6">
          <form onSubmit={handleSubmit}>
            {/* API Error */}
            {apiError && (
              <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded-lg text-sm">
                {apiError}
              </div>
            )}

            {/* Hostel Name */}
            <FormInput
              label="Hostel Name"
              name="hostelName"
              type="text"
              value={formData.hostelName}
              onChange={handleChange}
              placeholder="e.g., Green Valley Hostel"
              required
              error={errors.hostelName}
            />

            {/* Hostel Address */}
            <FormInput
              label="Hostel Address"
              name="hostelAddress"
              type="text"
              value={formData.hostelAddress}
              onChange={handleChange}
              placeholder="Street address, City, State, ZIP"
              required
              error={errors.hostelAddress}
            />

            {/* Action Buttons */}
            <div className="flex gap-4 mt-8">
              <Button
                type="submit"
                label={loading ? 'Creating...' : 'Create Hostel'}
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

          {/* TODO: Add hostel structure guide or hints */}
        </div>
      </main>
    </div>
  )
}

export default CreateHostel
