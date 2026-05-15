import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import FormInput from '../../components/FormInput'
import { hostelService } from '../../services/hostelService'
import { Alert, Button, Card, CardContent, CardHeader, PageHeader } from '../../components/ui'

const CreateHostel = () => {
  const navigate = useNavigate()
  const [formData, setFormData] = useState({
    hostelName: '',
    hostelAddress: '',
  })
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)
  const [apiError, setApiError] = useState('')

  const handleChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({ ...prev, [name]: value }))

    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }))
    }
  }

  const validateForm = () => {
    const newErrors = {}
    if (!formData.hostelName.trim()) newErrors.hostelName = 'Hostel name is required'
    if (!formData.hostelAddress.trim()) newErrors.hostelAddress = 'Hostel address is required'
    return newErrors
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    const newErrors = validateForm()

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors)
      return
    }

    setLoading(true)
    setApiError('')

    try {
      await hostelService.createHostel({
        hostelName: formData.hostelName,
        hostelAddress: formData.hostelAddress,
      })

      navigate('/owner/dashboard', { state: { message: 'Hostel created successfully.' } })
    } catch (error) {
      const errorData = error.response?.data

      // Priority 1: Check for message field (AlreadyExistException, NotFoundException, etc.)
      if (errorData && errorData.message) {
        setApiError(errorData.message)
        setErrors({})
      } 
      // Priority 2: Check for field-specific validation errors (object without message)
      else if (errorData && typeof errorData === 'object' && Object.keys(errorData).length > 0) {
        const fieldErrors = {}
        Object.keys(errorData).forEach((field) => {
          if (field !== 'message') {
            fieldErrors[field] = errorData[field]
          }
        })
        if (Object.keys(fieldErrors).length > 0) {
          setErrors((prev) => ({ ...prev, ...fieldErrors }))
          setApiError('')
        } else {
          setApiError('Failed to create hostel. Please try again.')
        }
      } 
      // Priority 3: Fallback to generic error
      else {
        setApiError('Failed to create hostel. Please try again.')
        setErrors({})
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="space-y-8">
      <PageHeader
        eyebrow="Property setup"
        title="Create a new hostel"
        description="Capture the property name and address first. Floors and rooms can be added immediately after creation from the workspace."
        secondaryAction={<Button label="Back to dashboard" variant="secondary" onClick={() => navigate('/owner/dashboard')} />}
      />

      <div className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
        <Card>
          <CardHeader
            title="Hostel details"
            description="Use clear naming so your portfolio remains easy to scan as more properties are added."
          />
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-5">
              {apiError ? (
                <Alert tone="error" title="Creation failed">
                  {apiError}
                </Alert>
              ) : null}

              <FormInput
                label="Hostel name"
                name="hostelName"
                type="text"
                value={formData.hostelName}
                onChange={handleChange}
                placeholder="Green Valley Hostel"
                required
                error={errors.hostelName}
                hint="Hostel names must be unique within your portfolio. Keep it short and recognizable for faster navigation."
              />

              <FormInput
                label="Hostel address"
                name="hostelAddress"
                type="text"
                value={formData.hostelAddress}
                onChange={handleChange}
                placeholder="Street address, city, state, ZIP"
                required
                error={errors.hostelAddress}
              />

              <div className="flex flex-col gap-3 pt-4 sm:flex-row">
                <Button type="submit" label="Create hostel" loading={loading} fullWidth />
                <Button
                  type="button"
                  label="Cancel"
                  onClick={() => navigate('/owner/dashboard')}
                  variant="secondary"
                  fullWidth
                />
              </div>
            </form>
          </CardContent>
        </Card>

        <div className="rounded-[1.75rem] border border-slate-200 bg-white p-6">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-600">What happens next</p>
          <div className="mt-4 space-y-4">
            {[
              'Add floors to define the building structure.',
              'Create rooms with bed capacity and availability.',
              'Use agreements to onboard tenants into active inventory.',
            ].map((item, index) => (
              <div key={item} className="flex gap-3">
                <div className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-slate-100 text-sm font-semibold text-slate-700">
                  {index + 1}
                </div>
                <p className="text-sm leading-6 text-slate-600">{item}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}

export default CreateHostel
