import apiClient from './apiClient'

/**
 * Decode the JWT payload without verifying the signature.
 * Returns null if the token is missing or malformed.
 */
export function getUsernameFromToken() {
  try {
    const token = localStorage.getItem('authToken')
    if (!token) return null
    const payload = JSON.parse(atob(token.split('.')[1]))
    // Spring Security sets the subject to the username
    return payload.sub || null
  } catch {
    return null
  }
}

export const profileService = {
  /** Fetch the current user's profile */
  getProfile: (username) => apiClient.get(`/users/${username}`),

  /** Update displayName, phoneNumber, and optionally password */
  updateProfile: (username, data) => apiClient.patch(`/users/${username}/profile`, data),

  /** Fetch current user's profile and return it (resolves with response.data) */
  getCurrentProfile: async () => {
    const username = getUsernameFromToken()
    if (!username) throw new Error('Not authenticated')
    const res = await apiClient.get(`/users/${username}`)
    return res.data
  },
}

export default profileService
