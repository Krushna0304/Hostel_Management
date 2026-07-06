import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import FormInput from '../../components/FormInput'
import FormSelect from '../../components/FormSelect'
import authService from '../../services/authService'
import AuthLayout from '../../layouts/AuthLayout'
import { Alert, Button } from '../../components/ui'

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
    { value: 'OWNER', label: 'Hostel owner' },
    { value: 'TENANT', label: 'Tenant' },
    { value: 'CLEANER', label: 'Cleaner' },
  ]

  const handleChange = (event) => {
    const { name, value } = event.target
    setFormData((prev) => ({ ...prev, [name]: value }))
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }))
    }
  }

  const validateForm = () => {
    const newErrors = {}
    if (!formData.firstName.trim()) newErrors.firstName = 'First name is required'
    if (!formData.lastName.trim()) newErrors.lastName = 'Last name is required'
    if (!formData.phoneNumber.trim()) newErrors.phoneNumber = 'Phone number is required'
    else if (formData.phoneNumber.length !== 10) newErrors.phoneNumber = 'Phone number must be exactly 10 digits'
    if (!formData.username.trim()) newErrors.username = 'Username is required'
    if (!formData.password.trim()) newErrors.password = 'Password is required'
    if (formData.password.length < 8) newErrors.password = 'Password must be at least 8 characters'
    if (formData.password !== formData.confirmPassword) newErrors.confirmPassword = 'Passwords do not match'
    if (!formData.role) newErrors.role = 'Role is required'
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
      await authService.register({
        firstName: formData.firstName,
        lastName: formData.lastName,
        phoneNumber: formData.phoneNumber,
        username: formData.username,
        password: formData.password,
        role: formData.role,
        isActive: true,
      })

      navigate('/login', { state: { message: 'Registration successful. Please sign in.' } })
    } catch (error) {
      const errorData = error.response?.data

      if (errorData && typeof errorData === 'object' && !errorData.message) {
        const fieldErrors = {}
        Object.keys(errorData).forEach((field) => {
          fieldErrors[field] = errorData[field]
        })
        setErrors((prev) => ({ ...prev, ...fieldErrors }))
        setApiError('')
      } else {
        setApiError(errorData?.message || 'Registration failed. Please try again.')
        setErrors({})
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout
      title="Create your account"
      subtitle="Set up a secure account and step into a cleaner workflow for hostel operations."
      footer={
        <p className="text-center text-sm text-slate-500">
          Already have an account?{' '}
          <Link to="/login" className="font-semibold text-sky-600 transition hover:text-sky-700">
            Sign in
          </Link>
        </p>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-3">
        {apiError ? <Alert tone="error">{apiError}</Alert> : null}

        <div className="grid gap-3 sm:grid-cols-2">
          <FormInput
            label="First name"
            name="firstName"
            type="text"
            value={formData.firstName}
            onChange={handleChange}
            placeholder="John"
            required
            error={errors.firstName}
          />

          <FormInput
            label="Last name"
            name="lastName"
            type="text"
            value={formData.lastName}
            onChange={handleChange}
            placeholder="Doe"
            required
            error={errors.lastName}
          />
        </div>

        <FormInput
          label="Phone number"
          name="phoneNumber"
          type="tel"
          inputMode="numeric"
          value={formData.phoneNumber}
          onChange={(e) => {
            const digits = e.target.value.replace(/\D/g, '').slice(0, 10)
            handleChange({ target: { name: 'phoneNumber', value: digits } })
          }}
          placeholder="9876543210"
          maxLength={10}
          required
          error={errors.phoneNumber}
        />

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

        <div className="grid gap-3 sm:grid-cols-2">
          <FormInput
            label="Password"
            name="password"
            type="password"
            value={formData.password}
            onChange={handleChange}
            placeholder="Minimum 8 characters"
            required
            error={errors.password}
          />

          <FormInput
            label="Confirm password"
            name="confirmPassword"
            type="password"
            value={formData.confirmPassword}
            onChange={handleChange}
            placeholder="Repeat your password"
            required
            error={errors.confirmPassword}
          />
        </div>

        <Button type="submit" label="Create account" loading={loading} fullWidth size="lg" />
      </form>
    </AuthLayout>
  )
}

export default Register
