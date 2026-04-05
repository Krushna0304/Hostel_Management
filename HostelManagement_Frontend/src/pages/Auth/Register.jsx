import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import FormInput from '../../components/FormInput'
import FormSelect from '../../components/FormSelect'
import Button from '../../components/Button'
import authService from '../../services/authService'

const Register = () => {
  const navigate = useNavigate()
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    phoneNumber: '',
    username: '',
    password: '',
    confirmPassword: '',
    role: '',
  })
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)
  const [apiError, setApiError] = useState('')

  const roleOptions = [
    { value: 'OWNER', label: 'Hostel Owner' },
    { value: 'TENANT', label: 'Tenant' },
    { value: 'CLEANER', label: 'Cleaner' },
  ]

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
    if (!formData.firstName.trim()) newErrors.firstName = 'First name is required'
    if (!formData.lastName.trim()) newErrors.lastName = 'Last name is required'
    if (!formData.phoneNumber.trim()) newErrors.phoneNumber = 'Phone number is required'
    if (!formData.username.trim()) newErrors.username = 'Username is required'
    if (!formData.password.trim()) newErrors.password = 'Password is required'
    if (formData.password !== formData.confirmPassword) newErrors.confirmPassword = 'Passwords do not match'
    if (!formData.role) newErrors.role = 'Role is required'
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
      // TODO: Update register endpoint based on actual backend
      const registerData = {
        firstName: formData.firstName,
        lastName: formData.lastName,
        phoneNumber: formData.phoneNumber,
        username: formData.username,
        password: formData.password,
        role: formData.role,
        isActive: true, // Always active when registering from login page
      }

      await authService.register(registerData)
      
      // Redirect to login on successful registration
      navigate('/login', { state: { message: 'Registration successful! Please login.' } })
    } catch (error) {
      console.error('Registration failed:', error)
      const errorData = error.response?.data
      
      // Check if error is field-specific validation errors (object with field names as keys)
      if (errorData && typeof errorData === 'object' && !errorData.message) {
        // It's field-specific errors like {"phoneNumber": "Phone number must be..."}
        const fieldErrors = {}
        Object.keys(errorData).forEach(field => {
          fieldErrors[field] = errorData[field]
        })
        setErrors(prev => ({ ...prev, ...fieldErrors }))
        setApiError('') // Clear general error, show field errors instead
      } else {
        // It's a general error message
        setApiError(errorData?.message || 'Registration failed. Please try again.')
        setErrors({}) // Clear field errors
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center p-4 py-8">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-lg p-8">
        {/* Header */}
        <div className="text-center mb-6">
          <h1 className="text-3xl font-bold text-gray-800 mb-2">Create Account</h1>
          <p className="text-gray-600 text-sm">Join Hostel Hub</p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit}>
          {/* API Error */}
          {apiError && (
            <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded-lg text-sm">
              {apiError}
            </div>
          )}

          {/* First Name */}
          <FormInput
            label="First Name"
            name="firstName"
            type="text"
            value={formData.firstName}
            onChange={handleChange}
            placeholder="John"
            required
            error={errors.firstName}
          />

          {/* Last Name */}
          <FormInput
            label="Last Name"
            name="lastName"
            type="text"
            value={formData.lastName}
            onChange={handleChange}
            placeholder="Doe"
            required
            error={errors.lastName}
          />

          {/* Phone Number */}
          <FormInput
            label="Phone Number"
            name="phoneNumber"
            type="tel"
            value={formData.phoneNumber}
            onChange={handleChange}
            placeholder="+91 98765 43210"
            required
            error={errors.phoneNumber}
          />

          {/* Username */}
          <FormInput
            label="Username"
            name="username"
            type="text"
            value={formData.username}
            onChange={handleChange}
            placeholder="john_doe"
            required
            error={errors.username}
          />

          {/* Role */}
          <FormSelect
            label="Role"
            name="role"
            value={formData.role}
            onChange={handleChange}
            options={roleOptions}
            placeholder="Select your role"
            required
            error={errors.role}
          />

          {/* Password */}
          <FormInput
            label="Password"
            name="password"
            type="password"
            value={formData.password}
            onChange={handleChange}
            placeholder="••••••••"
            required
            error={errors.password}
          />

          {/* Confirm Password */}
          <FormInput
            label="Confirm Password"
            name="confirmPassword"
            type="password"
            value={formData.confirmPassword}
            onChange={handleChange}
            placeholder="••••••••"
            required
            error={errors.confirmPassword}
          />

          {/* Register Button */}
          <Button
            type="submit"
            label={loading ? 'Registering...' : 'Register'}
            disabled={loading}
            fullWidth
            className="mb-4"
          />
        </form>

        {/* Login Link */}
        <div className="text-center">
          <p className="text-gray-600 text-sm">
            Already have an account?{' '}
            <Link to="/login" className="text-blue-500 font-medium hover:text-blue-600">
              Login here
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}

export default Register
