import apiClient from './apiClient'

// AuthService updated to match backend API
export const authService = {
  // User login
  login: (username, password, role) => {
    // Backend expects: { username, password, role } at /users/login (POST)
    return apiClient.post('/users/login', { username, password, role })
  },

  // User registration (create user)
  register: (userData) => {
    // Backend expects: { displayName, username, password, phoneNumber, role, isActive }
    // displayName can be constructed from firstName + lastName if needed
    const {
      displayName,
      firstName,
      lastName,
      phoneNumber,
      username,
      password,
      role,
      isActive,
    } = userData
    // Use displayName from userData if present, else construct from firstName + lastName
    const finalDisplayName = displayName || `${firstName || ''} ${lastName || ''}`.trim()
    return apiClient.post('/users', {
      displayName: finalDisplayName,
      username,
      password,
      phoneNumber,
      role,
      isActive: isActive !== undefined ? isActive : true,
    })
  },

  // Logout
  logout: () => {
    localStorage.removeItem('authToken')
    localStorage.removeItem('userRole')
  },

  // Get user by username
  getUser: (username) => {
    return apiClient.get(`/users/${username}`)
  },

  // Get user by username and role
  getUserByUsernameAndRole: (username, roleName) => {
    return apiClient.get(`/users/${username}/role/${roleName}`)
  },

  // Update user
  updateUser: (username, userData) => {
    // userData should match CreateUserRequest fields
    return apiClient.put(`/users/${username}`, userData)
  },

  // Delete user
  deleteUser: (username) => {
    return apiClient.delete(`/users/${username}`)
  },
}

export default authService
