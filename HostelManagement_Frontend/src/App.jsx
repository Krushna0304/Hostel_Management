import { useState, useEffect } from 'react'
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'

import ProtectedRoute from './components/ProtectedRoute'

// Auth
import Login from './pages/Auth/Login'
import Register from './pages/Auth/Register'

// Owner Pages
import OwnerDashboard from './pages/Owner/Dashboard'
import CreateHostel from './pages/Owner/CreateHostel'
import Floors from './pages/Owner/Floors'
import CreateFloorPage from './pages/Owner/CreateFloorPage'
import Rooms from './pages/Owner/Rooms'
import CreateRoomPage from './pages/Owner/CreateRoomPage'

import './App.css'

// Agreement Pages
import AgreementsLayout from './pages/Owner/Agreements/AgreementsLayout'
import TenantActivatePage from './pages/Tenant/Activate/TenantActivatePage'

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [userRole, setUserRole] = useState(null)

  useEffect(() => {
    const token = localStorage.getItem('authToken')
    const role = localStorage.getItem('userRole')

    if (token && role) {
      setIsAuthenticated(true)
      setUserRole(role)
    }
  }, [])

  return (
    <Router>
      <Routes>

        {/* Agreement Owner Flow */}
        <Route
          path="/owner/agreements/*"
          element={
            <ProtectedRoute isAuthenticated={isAuthenticated} userRole={userRole} requiredRole="OWNER">
              <AgreementsLayout />
            </ProtectedRoute>
          }
        />
        
        {/* Redirect /owner/agreements to /owner/agreements/ */}
        <Route
          path="/owner/agreements"
          element={
            <ProtectedRoute isAuthenticated={isAuthenticated} userRole={userRole} requiredRole="OWNER">
              <AgreementsLayout />
            </ProtectedRoute>
          }
        />

        {/* Tenant QR Activation (Public) */}
        <Route path="/tenant/activate" element={<TenantActivatePage />} />
        {/* ===================== */}
        {/* Public Routes */}
        {/* ===================== */}
        <Route
          path="/login"
          element={<Login setIsAuthenticated={setIsAuthenticated} setUserRole={setUserRole} />}
        />
        <Route path="/register" element={<Register />} />

        {/* ===================== */}
        {/* Owner Routes */}
        {/* ===================== */}

        <Route
          path="/owner/dashboard"
          element={
            <ProtectedRoute isAuthenticated={isAuthenticated} userRole={userRole} requiredRole="OWNER">
              <OwnerDashboard />
            </ProtectedRoute>
          }
        />

        <Route
          path="/owner/create-hostel"
          element={
            <ProtectedRoute isAuthenticated={isAuthenticated} userRole={userRole} requiredRole="OWNER">
              <CreateHostel />
            </ProtectedRoute>
          }
        />

        {/* Floors */}
        <Route
          path="/owner/hostels/:hostelId/floors"
          element={
            <ProtectedRoute isAuthenticated={isAuthenticated} userRole={userRole} requiredRole="OWNER">
              <Floors />
            </ProtectedRoute>
          }
        />

        <Route
          path="/owner/hostels/:hostelId/add-floor"
          element={
            <ProtectedRoute isAuthenticated={isAuthenticated} userRole={userRole} requiredRole="OWNER">
              <CreateFloorPage />
            </ProtectedRoute>
          }
        />

        {/* Rooms */}
        <Route
          path="/owner/hostels/:hostelId/floors/:floorId/rooms"
          element={
            <ProtectedRoute isAuthenticated={isAuthenticated} userRole={userRole} requiredRole="OWNER">
              <Rooms />
            </ProtectedRoute>
          }
        />

        <Route
          path="/owner/hostels/:hostelId/floors/:floorId/add-room"
          element={
            <ProtectedRoute isAuthenticated={isAuthenticated} userRole={userRole} requiredRole="OWNER">
              <CreateRoomPage />
            </ProtectedRoute>
          }
        />

        {/* ===================== */}
        {/* Redirects */}
        {/* ===================== */}
        <Route
          path="/"
          element={isAuthenticated ? <Navigate to="/owner/dashboard" /> : <Navigate to="/login" />}
        />
        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </Router>
  )
}

export default App
