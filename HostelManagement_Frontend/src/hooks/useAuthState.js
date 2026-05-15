import { useEffect, useState } from 'react'

function readAuthState() {
  const token = localStorage.getItem('authToken')
  const role = localStorage.getItem('userRole')

  return {
    isAuthenticated: Boolean(token && role),
    userRole: role,
  }
}

export function useAuthState() {
  const [authState, setAuthState] = useState(readAuthState)

  useEffect(() => {
    const syncAuth = () => setAuthState(readAuthState())
    window.addEventListener('storage', syncAuth)
    return () => window.removeEventListener('storage', syncAuth)
  }, [])

  const authenticate = (userRole) => {
    setAuthState({
      isAuthenticated: true,
      userRole,
    })
  }

  const clearAuth = () => {
    setAuthState({
      isAuthenticated: false,
      userRole: null,
    })
  }

  return {
    ...authState,
    authenticate,
    clearAuth,
  }
}
