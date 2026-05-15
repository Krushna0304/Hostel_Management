import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import FormInput from '../../components/FormInput'
import FormSelect from '../../components/FormSelect'
import Button from '../../components/Button'
import { hostelService, floorService, roomService } from '../../services/hostelService'

const AddRoom = () => {
  const navigate = useNavigate()
  const [hostels, setHostels] = useState([])
  const [floors, setFloors] = useState([])
  const [formData, setFormData] = useState({
    hostelId: '',
    floorId: '',
    roomType: '',
    roomNumber: '',
    roomDetails: '',
    totalBeds: '',
    availableBeds: '',
    isActive: true,
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

  const handleHostelChange = async (e) => {
    const { name, value } = e.target
    setFormData((prev) => ({
      ...prev,
      [name]: value,
      floorId: '', // Reset floor when hostel changes
    }))
    setFloors([])

    if (!value) return

    try {
      const response = await floorService.getFloorsByHostel(value)
      setFloors(response.data || [])
    } catch (err) {
      console.error('Failed to fetch floors:', err)
      const errorData = err?.response?.data
      if (errorData?.message) {
        console.error('Floor fetch error:', errorData.message)
      }
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
    if (!formData.floorId) newErrors.floorId = 'Please select a floor'
    if (!formData.roomType) newErrors.roomType = 'Room type is required'
    if (!formData.roomNumber.trim()) newErrors.roomNumber = 'Room number is required'
    if (!formData.roomDetails.trim()) newErrors.roomDetails = 'Room details are required'
    if (!formData.totalBeds || formData.totalBeds <= 0) newErrors.totalBeds = 'Total beds must be greater than 0'
    if (formData.availableBeds === '' || formData.availableBeds < 0) newErrors.availableBeds = 'Available beds must be 0 or more'
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
      await roomService.createRoom(formData.hostelId, formData.floorId, {
        hostelId: formData.hostelId,
        floorId: formData.floorId,
        roomNumber: formData.roomNumber,
        roomDetails: formData.roomDetails,
        totalBeds: parseInt(formData.totalBeds),
        availableBeds: parseInt(formData.availableBeds),
        isActive: formData.isActive,
        roomType: formData.roomType,
      })

      // Success - redirect to dashboard
      navigate('/owner/dashboard', { state: { message: 'Room added successfully!' } })
    } catch (error) {
      console.error('Failed to add room:', error)
      const errorData = error.response?.data
      
      // Check if error is field-specific validation errors (object with field names as keys)
      if (errorData && typeof errorData === 'object' && !errorData.message) {
        // It's field-specific errors like {"roomNumber": "Room number is required"}
        const fieldErrors = {}
        Object.keys(errorData).forEach(field => {
          fieldErrors[field] = errorData[field]
        })
        setErrors(prev => ({ ...prev, ...fieldErrors }))
        setApiError('') // Clear general error, show field errors instead
      } else {
        // It's a general error message
        setApiError(errorData?.message || 'Failed to add room. Please try again.')
        setErrors({}) // Clear field errors
      }
    } finally {
      setLoading(false)
    }
  }

  const hostelOptions = hostels.map((hostel) => ({
    value: hostel.id,
    label: hostel.name,
  }))

  const floorOptions = floors.map((floor) => ({
    value: floor.id,
    label: `Floor ${floor.floorNumber}`,
  }))

  const roomTypeOptions = [
    { value: 'PG_ROOM', label: 'PG Room' },
    { value: 'FLAT', label: 'Flat' },
  ]

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm sticky top-0 z-10">
        <div className="max-w-2xl mx-auto px-4 py-4">
          <h1 className="text-2xl font-bold text-gray-800">Add Room</h1>
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

              {/* Select Hostel */}
              <FormSelect
                label="Select Hostel"
                name="hostelId"
                value={formData.hostelId}
                onChange={handleHostelChange}
                options={hostelOptions}
                placeholder="Choose a hostel"
                required
                error={errors.hostelId}
              />

              {/* Select Floor */}
              {formData.hostelId && (
                <FormSelect
                  label="Select Floor"
                  name="floorId"
                  value={formData.floorId}
                  onChange={handleChange}
                  options={floorOptions}
                  placeholder={floorOptions.length === 0 ? 'No floors available' : 'Choose a floor'}
                  required
                  error={errors.floorId}
                />
              )}

              {/* Room Type - Show when hostel is selected */}
              {formData.hostelId && (
                <FormSelect
                  label="Room Type"
                  name="roomType"
                  value={formData.roomType}
                  onChange={handleChange}
                  options={roomTypeOptions}
                  placeholder="Select room type"
                  required
                  error={errors.roomType}
                />
              )}

              {/* Room Details Fields - Show when hostel, floor, and room type are selected */}
              {formData.hostelId && formData.floorId && formData.roomType && (
                <>
                  {/* Room Number */}
                  <FormInput
                    label="Room Number"
                    name="roomNumber"
                    type="text"
                    value={formData.roomNumber}
                    onChange={handleChange}
                    placeholder="e.g., 101, 102"
                    required
                    error={errors.roomNumber}
                  />

                  {/* Room Details */}
                  <FormInput
                    label="Room Details"
                    name="roomDetails"
                    type="text"
                    value={formData.roomDetails}
                    onChange={handleChange}
                    placeholder="e.g., AC, attached bathroom, etc."
                    required
                    error={errors.roomDetails}
                  />

                  {/* Total Beds */}
                  <FormInput
                    label="Total Beds"
                    name="totalBeds"
                    type="number"
                    value={formData.totalBeds}
                    onChange={handleChange}
                    placeholder="e.g., 2, 3, 4"
                    required
                    error={errors.totalBeds}
                  />

                  {/* Available Beds */}
                  <FormInput
                    label="Available Beds"
                    name="availableBeds"
                    type="number"
                    value={formData.availableBeds}
                    onChange={handleChange}
                    placeholder="e.g., 2"
                    required
                    error={errors.availableBeds}
                  />

                  {/* Is Active */}
                  <div className="flex items-center mt-2 mb-4">
                    <input
                      type="checkbox"
                      id="isActive"
                      name="isActive"
                      checked={formData.isActive}
                      onChange={e => setFormData(prev => ({ ...prev, isActive: e.target.checked }))}
                      className="mr-2"
                    />
                    <label htmlFor="isActive" className="text-gray-700">Room is active</label>
                    <p className="text-sm text-gray-500 ml-2">Inactive rooms stay visible but won't appear as active inventory.</p>
                  </div>

                  {/* Action Buttons */}
                  <div className="flex gap-4 mt-8">
                    <Button
                      type="submit"
                      label={loading ? 'Adding Room...' : 'Add Room'}
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
                </>
              )}
            </form>
          )}
        </div>
      </main>
    </div>
  )
}

export default AddRoom