import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import FormInput from '../../components/FormInput'
import Button from '../../components/Button'
import authService from '../../services/authService'

const Login = ({ setIsAuthenticated, setUserRole }) => {
  const navigate = useNavigate()
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    role: 'OWNER',
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
    // Clear error for this field
    if (errors[name]) {
      setErrors((prev) => ({
        ...prev,
        [name]: '',
      }))
    }
  }

  const validateForm = () => {
    const newErrors = {}
    if (!formData.username.trim()) newErrors.username = 'Username is required'
    if (!formData.password.trim()) newErrors.password = 'Password is required'
    if (!formData.role) newErrors.role = 'Role is required'
    return newErrors
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    const newErrors = validateForm();
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }
    try {
      // Backend returns only JWT token
      const response = await authService.login(formData.username, formData.password, formData.role);
      const { token } = response.data;
      localStorage.setItem('authToken', token);
      setIsAuthenticated(true);

      // Fetch user info to get role
      try {
        const userInfoResponse = await authService.getUser(formData.username);
        const userRole = userInfoResponse.data.role;
        localStorage.setItem('userRole', userRole);
        setUserRole(userRole);
        // Redirect based on role
        navigate(userRole === 'OWNER' ? '/owner/dashboard' : '/dashboard');
      } catch (userInfoError) {
        // Fallback: redirect to dashboard if user info fetch fails
        navigate('/dashboard');
      }
    } catch (error) {
      console.error('Login failed:', error);
      const errorData = error.response?.data
      
      // Check if error is field-specific validation errors (object with field names as keys)
      if (errorData && typeof errorData === 'object' && !errorData.message) {
        // It's field-specific errors like {"username": "Username is required"}
        const fieldErrors = {}
        Object.keys(errorData).forEach(field => {
          fieldErrors[field] = errorData[field]
        })
        setErrors(prev => ({ ...prev, ...fieldErrors }))
        setApiError('') // Clear general error, show field errors instead
      } else {
        // It's a general error message
        setApiError(errorData?.message || 'Login failed. Please try again.')
        setErrors({}) // Clear field errors
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-lg p-8">
        {/* Header */}
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-gray-800 mb-2">Hostel Hub</h1>
          <p className="text-gray-600">Hostel Management System</p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit}>
          {/* API Error */}
          {apiError && (
            <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded-lg text-sm">
              {apiError}
            </div>
          )}

          {/* Username */}
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

          {/* Password */}
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

          {/* Role Selection */}
          <div className="mb-4">
            <label htmlFor="role" className="block text-sm font-medium text-gray-700 mb-1">Role</label>
            <select
              id="role"
              name="role"
              value={formData.role}
              onChange={handleChange}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            >
              <option value="OWNER">Hostel Owner</option>
              <option value="TENANT">Tenant</option>
              <option value="CLEANER">Cleaner</option>
            </select>
            {errors.role && <div className="text-red-600 text-xs mt-1">{errors.role}</div>}
          </div>

          {/* Login Button */}
          <Button
            type="submit"
            label={loading ? 'Logging in...' : 'Login'}
            disabled={loading}
            fullWidth
            className="mb-4"
          />
        </form>

        {/* Register Link */}
        <div className="text-center">
          <p className="text-gray-600">
            Don't have an account?{' '}
            <Link to="/register" className="text-blue-500 font-medium hover:text-blue-600">
              Register here
            </Link>
          </p>
        </div>

        {/* Footer */}
        <div className="mt-6 pt-6 border-t border-gray-200">
          <p className="text-xs text-gray-500 text-center">
            This is a secure platform. Please keep your credentials confidential.
          </p>
        </div>
      </div>
    </div>
  )
}

export default Login
