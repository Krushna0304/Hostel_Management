import { Navigate, useLocation } from 'react-router-dom'

const ProtectedRoute = ({ isAuthenticated, userRole, requiredRole, children }) => {
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  if (requiredRole && userRole !== requiredRole) {
    return <Navigate to="/login" replace />
  }

  return children
}

export default ProtectedRoute
