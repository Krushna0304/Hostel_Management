import { useEffect, useState } from 'react'
import { roomService } from '../services/hostelService'
import { Alert, Skeleton } from './ui'
import { AlertTriangleIcon, BedIcon, UsersIcon } from './icons/AppIcons'

/**
 * Enhanced Room Dropdown Component (Task 6.3.1)
 * 
 * Features:
 * - Displays available bed count and tenant action pending counts
 * - Supports overbooking scenario display
 * - Sorts rooms by TenantActionPending count in ascending order
 * - Shows real-time availability status
 */
const EnhancedRoomDropdown = ({ 
  hostelId, 
  value, 
  onChange, 
  required = false, 
  disabled = false,
  className = '',
  showFullDetails = false,
  filterByAvailability = false 
}) => {
  const [rooms, setRooms] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    const fetchRooms = async () => {
      if (!hostelId) return
      
      setLoading(true)
      setError('')
      
      try {
        // Use enhanced room dropdown API that returns rooms sorted by pending actions
        const response = await roomService.getRoomDropdownData(hostelId)
        let roomsData = response.data?.rooms || []
        
        // Filter by availability if requested
        if (filterByAvailability) {
          roomsData = roomsData.filter(room => 
            room.available > 0 || room.status === 'AVAILABLE'
          )
        }
        
        setRooms(roomsData)
      } catch (err) {
        console.error('Failed to fetch enhanced room data:', err)
        setError('Failed to load room availability data')
      } finally {
        setLoading(false)
      }
    }

    fetchRooms()
  }, [hostelId, filterByAvailability])

  // Get display text for room option
  const getRoomDisplayText = (room) => {
    if (showFullDetails) {
      const statusText = room.status === 'OVERBOOKED' ? ' - OVERBOOKED' : ''
      const pendingText = room.pendingActions > 0 ? ` (${room.pendingActions} pending)` : ''
      return `Room ${room.roomNumber} - ${room.available}/${room.totalBeds} beds${pendingText}${statusText}`
    }
    return room.displayText || `Room ${room.roomNumber} (${room.available}/${room.totalBeds} beds)`
  }

  // Get option styling based on room status
  const getRoomOptionStyle = (room) => {
    if (room.status === 'OVERBOOKED') {
      return 'text-red-600 font-medium'
    }
    if (room.pendingActions > 0) {
      return 'text-orange-600'
    }
    if (room.available === 0) {
      return 'text-gray-500'
    }
    return 'text-gray-900'
  }

  if (loading) {
    return <Skeleton className="h-10 w-full rounded-lg" />
  }

  return (
    <div className="space-y-2">
      <select
        value={value || ''}
        onChange={(e) => onChange?.(e.target.value)}
        required={required}
        disabled={disabled || loading}
        className={`w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 disabled:bg-gray-100 disabled:cursor-not-allowed ${className}`}
      >
        <option value="">Select a room</option>
        {rooms.map((room) => (
          <option 
            key={room.roomId} 
            value={room.roomId}
            className={getRoomOptionStyle(room)}
            disabled={room.available === 0 && filterByAvailability}
          >
            {getRoomDisplayText(room)}
          </option>
        ))}
      </select>

      {error && (
        <Alert tone="error" className="text-sm">
          {error}
        </Alert>
      )}

      {/* Enhanced display information */}
      {showFullDetails && rooms.length > 0 && (
        <div className="mt-3 space-y-2">
          <div className="text-xs font-medium text-gray-600 uppercase tracking-wide">
            Room Availability Summary
          </div>
          
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 text-xs">
            <div className="flex items-center gap-2 p-2 bg-green-50 rounded-lg">
              <BedIcon className="h-4 w-4 text-green-600" />
              <span className="text-green-800">
                {rooms.filter(r => r.available > 0).length} Available
              </span>
            </div>
            
            <div className="flex items-center gap-2 p-2 bg-orange-50 rounded-lg">
              <AlertTriangleIcon className="h-4 w-4 text-orange-600" />
              <span className="text-orange-800">
                {rooms.reduce((sum, r) => sum + (r.pendingActions || 0), 0)} Pending Actions
              </span>
            </div>
            
            <div className="flex items-center gap-2 p-2 bg-red-50 rounded-lg">
              <UsersIcon className="h-4 w-4 text-red-600" />
              <span className="text-red-800">
                {rooms.filter(r => r.status === 'OVERBOOKED').length} Overbooked
              </span>
            </div>
          </div>

          {/* Sorted rooms legend */}
          <div className="text-xs text-gray-500 italic">
            Rooms are sorted by pending actions (lowest first) to prioritize availability.
          </div>
        </div>
      )}

      {/* No rooms available message */}
      {!loading && rooms.length === 0 && !error && (
        <div className="text-sm text-gray-500 italic">
          No rooms available for this hostel.
        </div>
      )}
    </div>
  )
}

export default EnhancedRoomDropdown