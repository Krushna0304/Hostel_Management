import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import FormInput from '../../components/FormInput'
import FormSelect from '../../components/FormSelect'
import authService from '../../services/authService'
import AuthLayout from '../../layouts/AuthLayout'
import { Alert, Button } from '../../components/ui'

const Login = ({ onAuthenticated }) => {
  const navigate = useNavigate()
  const location = useLocation()
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    role: 'OWNER',
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
    if (!formData.username.trim()) newErrors.username = 'Username is required'
    if (!formData.password.trim()) newErrors.password = 'Password is required'
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
      const response = await authService.login(formData.username, formData.password, formData.role)
      const { token } = response.data
      localStorage.setItem('authToken', token)

      let resolvedRole = formData.role

      try {
        const userInfoResponse = await authService.getUser(formData.username)
        resolvedRole = userInfoResponse.data.role || formData.role
      } catch (error) {
        resolvedRole = formData.role
      }

      localStorage.setItem('userRole', resolvedRole)
      onAuthenticated?.(resolvedRole)

      navigate(resolvedRole === 'OWNER' ? '/owner/dashboard' : resolvedRole === 'TENANT' ? '/tenant-portal/dashboard' : '/dashboard')
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
        setApiError(errorData?.message || 'Login failed. Please try again.')
        setErrors({})
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout
      title="Welcome back"
      subtitle="Sign in to manage your properties, rooms, and tenant agreements from one polished workspace."
      footer={
        <p className="text-center text-sm text-slate-500">
          Don&apos;t have an account?{' '}
          <Link to="/register" className="font-semibold text-sky-600 transition hover:text-sky-700">
            Create one
          </Link>
        </p>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-5">
        {location.state?.message ? <Alert tone="success">{location.state.message}</Alert> : null}
        {apiError ? <Alert tone="error">{apiError}</Alert> : null}

        <FormInput
          label="Username"
          name="username"
          type="text"
          value={formData.username}
          onChange={handleChange}
          placeholder="Enter your username"
          required
          error={errors.username}
        />

        <FormInput
          label="Password"
          name="password"
          type="password"
          value={formData.password}
          onChange={handleChange}
          placeholder="Enter your password"
          required
          error={errors.password}
        />

        <FormSelect
          label="Role"
          name="role"
          value={formData.role}
          onChange={handleChange}
          options={[
            { value: 'OWNER', label: 'Hostel owner' },
            { value: 'TENANT', label: 'Tenant' },
            { value: 'CLEANER', label: 'Cleaner' },
          ]}
          required
          error={errors.role}
        />

        <Button type="submit" label="Sign in" loading={loading} fullWidth size="lg" />
      </form>
    </AuthLayout>
  )
}

export default Login
