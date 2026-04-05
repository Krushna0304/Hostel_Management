import { Navigate } from 'react-router-dom'

const ProtectedRoute = ({ isAuthenticated, userRole, requiredRole, children }) => {
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (requiredRole && userRole !== requiredRole) {
    return <Navigate to="/login" replace />
  }

  return children
}

export default ProtectedRoute
