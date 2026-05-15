import axios from 'axios'

// Use import.meta.env for Vite projects
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000, // 30 second timeout
  headers: {
    'Content-Type': 'application/json',
  },
})

// Add JWT token to requests
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Handle response errors
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // Handle timeout errors
    if (error.code === 'ECONNABORTED') {
      console.error('Request timeout:', error)
      error.message = 'Request timeout. Please try again.'
    }
    
    // Handle network errors
    if (error.message === 'Network Error') {
      console.error('Network error:', error)
      error.message = 'Network error. Please check your connection.'
    }
    
    if (error.response?.status === 401) {
      localStorage.removeItem('authToken')
      localStorage.removeItem('userRole')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default apiClient
