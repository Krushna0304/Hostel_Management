import { AlertTriangleIcon, BedIcon, UsersIcon, DoorIcon } from './icons/AppIcons'
import { Badge } from './ui'

/**
 * Room Availability Status Component (Task 6.3.1)
 * 
 * Displays comprehensive room availability information including:
 * - Bed count and availability
 * - Current occupancy
 * - Tenant action pending count
 * - Overbooking detection and warnings
 */
const RoomAvailabilityStatus = ({ 
  availabilityData, 
  compact = false, 
  showOverbookingWarning = true,
  className = '' 
}) => {
  if (!availabilityData) {
    return (
      <div className={`text-sm text-gray-500 ${className}`}>
        Availability data not available
      </div>
    )
  }

  const {
    totalBedCapacity = 0,
    currentOccupancy = 0,
    actualAvailableBeds = 0,
    tenantActionPendingCount = 0,
    overbookingDetected = false,
    allocationStatus = 'UNKNOWN'
  } = availabilityData

  // Get status badge variant
  const getStatusBadgeVariant = () => {
    if (overbookingDetected) return 'error'
    if (tenantActionPendingCount > 0) return 'warning'
    if (actualAvailableBeds === 0) return 'error'
    if (allocationStatus === 'AVAILABLE') return 'success'
    return 'default'
  }

  // Get status text
  const getStatusText = () => {
    if (overbookingDetected) return 'Overbooked'
    if (actualAvailableBeds === 0) return 'Full'
    if (tenantActionPendingCount > 0) return `${tenantActionPendingCount} Pending`
    return 'Available'
  }

  // Get occupancy color based on status
  const getOccupancyColor = () => {
    if (overbookingDetected) return 'text-red-600'
    if (actualAvailableBeds === 0) return 'text-orange-600'
    return 'text-slate-950'
  }

  if (compact) {
    return (
      <div className={`flex items-center gap-3 ${className}`}>
        <div className="flex items-center gap-1">
          <BedIcon className="h-4 w-4 text-slate-400" />
          <span className={`text-sm font-medium ${getOccupancyColor()}`}>
            {actualAvailableBeds}/{totalBedCapacity}
          </span>
        </div>
        
        {tenantActionPendingCount > 0 && (
          <div className="flex items-center gap-1">
            <AlertTriangleIcon className="h-4 w-4 text-orange-500" />
            <span className="text-sm text-orange-600">{tenantActionPendingCount}</span>
          </div>
        )}
        
        <Badge variant={getStatusBadgeVariant()}>
          {getStatusText()}
        </Badge>
      </div>
    )
  }

  return (
    <div className={`space-y-4 ${className}`}>
      {/* Status Header */}
      <div className="flex items-center justify-between">
        <div>
          <h4 className="text-sm font-medium text-slate-900">Room Availability</h4>
          <p className="text-xs text-slate-500 mt-1">Current occupancy and bed allocation status</p>
        </div>
        <Badge variant={getStatusBadgeVariant()}>
          {getStatusText()}
        </Badge>
      </div>

      {/* Availability Metrics */}
      <div className="grid grid-cols-3 gap-4">
        {/* Total Beds */}
        <div className="rounded-2xl bg-white px-4 py-3 shadow-sm border border-slate-100">
          <div className="flex items-center gap-2">
            <BedIcon className="h-4 w-4 text-slate-400" />
            <p className="text-xs uppercase tracking-[0.18em] text-slate-400">Total beds</p>
          </div>
          <p className="mt-2 text-xl font-semibold text-slate-950">{totalBedCapacity}</p>
        </div>

        {/* Current Occupancy */}
        <div className="rounded-2xl bg-white px-4 py-3 shadow-sm border border-slate-100">
          <div className="flex items-center gap-2">
            <UsersIcon className="h-4 w-4 text-slate-400" />
            <p className="text-xs uppercase tracking-[0.18em] text-slate-400">Occupied</p>
          </div>
          <p className={`mt-2 text-xl font-semibold ${getOccupancyColor()}`}>
            {currentOccupancy}
          </p>
        </div>

        {/* Available Beds */}
        <div className="rounded-2xl bg-white px-4 py-3 shadow-sm border border-slate-100">
          <div className="flex items-center gap-2">
            <DoorIcon className="h-4 w-4 text-slate-400" />
            <p className="text-xs uppercase tracking-[0.18em] text-slate-400">Available</p>
          </div>
          <p className={`mt-2 text-xl font-semibold ${getOccupancyColor()}`}>
            {actualAvailableBeds}
          </p>
        </div>
      </div>

      {/* Warnings and Alerts */}
      {showOverbookingWarning && (overbookingDetected || tenantActionPendingCount > 0) && (
        <div className="space-y-2">
          {overbookingDetected && (
            <div className="rounded-lg bg-red-50 border border-red-200 px-4 py-3">
              <div className="flex items-center gap-2">
                <AlertTriangleIcon className="h-5 w-5 text-red-600 flex-shrink-0" />
                <div>
                  <p className="text-sm font-medium text-red-800">Room Overbooked</p>
                  <p className="text-xs text-red-700 mt-1">
                    This room has more occupants than bed capacity. New allocations may fail.
                  </p>
                </div>
              </div>
            </div>
          )}

          {tenantActionPendingCount > 0 && !overbookingDetected && (
            <div className="rounded-lg bg-orange-50 border border-orange-200 px-4 py-3">
              <div className="flex items-center gap-2">
                <AlertTriangleIcon className="h-5 w-5 text-orange-600 flex-shrink-0" />
                <div>
                  <p className="text-sm font-medium text-orange-800">
                    {tenantActionPendingCount} Tenant Action{tenantActionPendingCount > 1 ? 's' : ''} Pending
                  </p>
                  <p className="text-xs text-orange-700 mt-1">
                    Some tenants require action confirmation before beds become available.
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Allocation Status Details */}
      <div className="rounded-lg bg-slate-50 px-4 py-3">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-slate-700">Allocation Status</p>
            <p className="text-xs text-slate-500 mt-1">
              {allocationStatus === 'AVAILABLE' && 'Room ready for new allocations'}
              {allocationStatus === 'OVERBOOKED' && 'Room exceeds capacity - allocation may fail'}
              {allocationStatus === 'AVAILABLE_PENDING_LIMITED' && 'Limited availability due to pending actions'}
              {allocationStatus === 'FULL' && 'No beds currently available'}
              {allocationStatus === 'INACTIVE' && 'Room is not active for allocations'}
              {allocationStatus === 'UNKNOWN' && 'Status could not be determined'}
            </p>
          </div>
          <Badge variant={getStatusBadgeVariant()}>
            {allocationStatus}
          </Badge>
        </div>
      </div>
    </div>
  )
}

export default RoomAvailabilityStatus