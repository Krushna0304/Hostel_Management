import { Suspense, lazy } from 'react'
import { BrowserRouter as Router, Navigate, Route, Routes } from 'react-router-dom'
import ProtectedRoute from './components/ProtectedRoute'
import DashboardLayout from './layouts/DashboardLayout'
import { useAuthState } from './hooks/useAuthState'
import { LoadingScreen, ConfirmationProvider } from './components/ui'

const Login = lazy(() => import('./pages/Auth/Login'))
const Register = lazy(() => import('./pages/Auth/Register'))
const OwnerDashboard = lazy(() => import('./pages/Owner/Dashboard'))
const CreateHostel = lazy(() => import('./pages/Owner/CreateHostel'))
const Hostels = lazy(() => import('./pages/Owner/Hostels'))
const Floors = lazy(() => import('./pages/Owner/Floors'))
const CreateFloorPage = lazy(() => import('./pages/Owner/CreateFloorPage'))
const Rooms = lazy(() => import('./pages/Owner/Rooms'))
const CreateRoomPage = lazy(() => import('./pages/Owner/CreateRoomPage'))
const AgreementsLayout = lazy(() => import('./pages/Owner/Agreements/AgreementsLayout'))
const Plans = lazy(() => import('./pages/Owner/Plans'))
const PaymentSettings = lazy(() => import('./pages/Owner/PaymentSettings'))
const OwnerOtherCharges = lazy(() => import('./pages/Owner/OtherCharges'))
const OwnerElectricityBills = lazy(() => import('./pages/Owner/ElectricityBills'))
const OwnerSettlements = lazy(() => import('./pages/Owner/Settlements'))
const TenantActivatePage = lazy(() => import('./pages/Tenant/Activate/TenantActivatePage'))
const TenantDashboard = lazy(() => import('./pages/Tenant/Dashboard'))
const TenantOtherCharges = lazy(() => import('./pages/Tenant/OtherCharges'))
const TenantElectricityBills = lazy(() => import('./pages/Tenant/ElectricityBills'))
const TenantSettlements = lazy(() => import('./pages/Tenant/Settlements'))
const CollectionDashboard = lazy(() => import('./pages/Owner/CollectionDashboard'))
const PaymentMonitoring = lazy(() => import('./pages/MCP/PaymentMonitoring'))
const Profile = lazy(() => import('./pages/Profile/Profile'))
const PaymentHistory = lazy(() => import('./pages/PaymentHistory/PaymentHistory'))
const ReminderSettings = lazy(() => import('./pages/Owner/ReminderSettings'))

function App() {
  const { isAuthenticated, userRole, authenticate } = useAuthState()

  return (
    <ConfirmationProvider>
      <Router>
        <Suspense fallback={<LoadingScreen />}>
          <Routes>
            <Route path="/login" element={<Login onAuthenticated={authenticate} />} />
            <Route path="/register" element={<Register />} />
            <Route path="/tenant/activate" element={<TenantActivatePage />} />

            <Route
              path="/owner"
              element={
                <ProtectedRoute isAuthenticated={isAuthenticated} userRole={userRole} requiredRole="OWNER">
                  <DashboardLayout />
                </ProtectedRoute>
              }
            >
              <Route path="dashboard" element={<OwnerDashboard />} />
              <Route path="hostels" element={<Hostels />} />
              <Route path="hostels/create-hostel" element={<CreateHostel />} />
              <Route path="hostels/:hostelId/floors" element={<Floors />} />
              <Route path="hostels/:hostelId/add-floor" element={<CreateFloorPage />} />
              <Route path="hostels/:hostelId/floors/:floorId/rooms" element={<Rooms />} />
              <Route path="hostels/:hostelId/floors/:floorId/add-room" element={<CreateRoomPage />} />
              <Route path="agreements/*" element={<AgreementsLayout />} />
              <Route path="plans" element={<Plans />} />
              <Route path="collections" element={<CollectionDashboard />} />
              <Route path="other-charges" element={<OwnerOtherCharges />} />
              <Route path="electricity-bills" element={<OwnerElectricityBills />} />
              <Route path="settlements" element={<OwnerSettlements />} />
              <Route path="payment-settings" element={<PaymentSettings />} />
              <Route path="reminder-settings" element={<ReminderSettings />} />
              <Route path="payment-history" element={<PaymentHistory />} />
              <Route path="profile" element={<Profile />} />
            </Route>

            <Route
              path="/mcp"
              element={
                <ProtectedRoute isAuthenticated={isAuthenticated} userRole={userRole} requiredRole="MCP">
                  <DashboardLayout />
                </ProtectedRoute>
              }
            >
              <Route path="payment-monitoring" element={<PaymentMonitoring />} />
            </Route>

            <Route
              path="/tenant-portal"
              element={
                <ProtectedRoute isAuthenticated={isAuthenticated} userRole={userRole} requiredRole="TENANT">
                  <DashboardLayout />
                </ProtectedRoute>
              }
            >
              <Route path="dashboard" element={<TenantDashboard />} />
              <Route path="other-charges" element={<TenantOtherCharges />} />
              <Route path="electricity-bills" element={<TenantElectricityBills />} />
              <Route path="settlements" element={<TenantSettlements />} />
              <Route path="payment-history" element={<PaymentHistory />} />
              <Route path="profile" element={<Profile />} />
            </Route>

            <Route
              path="/"
              element={
                isAuthenticated
                  ? <Navigate to={userRole === 'TENANT' ? '/tenant-portal/dashboard' : '/owner/dashboard'} replace />
                  : <Navigate to="/login" replace />
              }
            />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Suspense>
      </Router>
    </ConfirmationProvider>
  )
}

export default App
