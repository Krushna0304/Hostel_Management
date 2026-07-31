import apiClient from './apiClient';

/**
 * Room Allocation Service
 * Handles room availability, allocation, and overbooking management
 * Covers task 7.2.1 - API endpoint connections for room allocation
 */
const roomAllocationService = {
  /**
   * Get room availability with bed counts and overbooking support
   * GET /api/v1/rooms/{roomId}/availability
   */
  async getRoomAvailability(roomId) {
    try {
      const response = await apiClient.get(`/api/v1/rooms/${roomId}/availability`);
      return response.data;
    } catch (error) {
      console.error(`Error getting room availability for room ${roomId}:`, error);
      throw error;
    }
  },

  /**
   * Allocate room with overbooking support (first-come-first-served)
   * POST /api/v1/rooms/{roomId}/allocate
   */
  async allocateRoom(roomId, allocationData) {
    try {
      console.log(`Allocating room ${roomId} with data:`, allocationData);
      const response = await apiClient.post(`/api/v1/rooms/${roomId}/allocate`, allocationData);
      console.log('Room allocation successful:', response.data);
      return response.data;
    } catch (error) {
      console.error(`Error allocating room ${roomId}:`, error);
      // Provide user-friendly error messages for specific error codes
      if (error.response?.status === 409) {
        const errorData = error.response.data;
        const enhancedError = new Error(
          errorData?.message || 'Room capacity exceeded. No beds available.'
        );
        enhancedError.response = error.response;
        enhancedError.isOverbookingError = true;
        enhancedError.suggestedAlternatives = errorData?.suggestedAlternatives || [];
        throw enhancedError;
      }
      throw error;
    }
  },

  /**
   * Search available rooms sorted by TenantActionPending count (ascending)
   * GET /api/v1/rooms/search/available?hostelId={hostelId}
   */
  async searchAvailableRooms(hostelId, filters = {}) {
    try {
      const params = new URLSearchParams();
      if (hostelId) params.append('hostelId', hostelId);
      if (filters.minAvailableBeds !== undefined) params.append('minAvailableBeds', filters.minAvailableBeds);
      if (filters.roomType) params.append('roomType', filters.roomType);

      const query = params.toString() ? `?${params.toString()}` : '';
      const response = await apiClient.get(`/api/v1/rooms/search/available${query}`);
      return response.data;
    } catch (error) {
      console.error('Error searching available rooms:', error);
      throw error;
    }
  },

  /**
   * Get room dropdown data with pending action counts
   * Sorted by TenantActionPending count in ascending order
   * GET /api/v1/rooms/dropdown?hostelId={hostelId}
   */
  async getRoomDropdownData(hostelId) {
    try {
      const response = await apiClient.get(`/api/v1/rooms/dropdown?hostelId=${hostelId}`);
      return response.data;
    } catch (error) {
      console.error(`Error getting room dropdown data for hostel ${hostelId}:`, error);
      throw error;
    }
  },

  /**
   * Get sorted rooms by pending actions count (ascending)
   * GET /api/v1/rooms/sorted/by-pending-actions?hostelId={hostelId}
   */
  async getRoomsSortedByPendingActions(hostelId) {
    try {
      const response = await apiClient.get(`/api/v1/rooms/sorted/by-pending-actions?hostelId=${hostelId}`);
      return response.data;
    } catch (error) {
      console.error(`Error getting sorted rooms for hostel ${hostelId}:`, error);
      throw error;
    }
  },

  /**
   * Check if room has overbooking based on availability data
   * @param {Object} availabilityData - Room availability response from getRoomAvailability
   * @returns {boolean}
   */
  isRoomOverbooked(availabilityData) {
    return availabilityData?.overbookingDetected === true ||
      availabilityData?.allocationStatus === 'OVERBOOKED';
  },

  /**
   * Get user-friendly error message for allocation errors
   * @param {Error} error
   * @returns {string}
   */
  getAllocationErrorMessage(error) {
    if (error.isOverbookingError) {
      return 'Room not available. This room has reached its maximum capacity. Please select a different room.';
    }
    if (error.response?.status === 403) {
      return 'You do not have permission to allocate this room.';
    }
    if (error.response?.status === 404) {
      return 'Room not found. It may have been removed or deactivated.';
    }
    return error.response?.data?.message || error.message || 'Failed to allocate room. Please try again.';
  },
};

export default roomAllocationService;
