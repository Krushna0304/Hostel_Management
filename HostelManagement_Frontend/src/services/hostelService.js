import apiClient from './apiClient'

// TODO: Update endpoints based on actual backend API paths

export const hostelService = {
  // Create a new hostel
  createHostel: (hostelData) => {
    // TODO: Verify endpoint and fields from backend
    // Expected fields: name, address, city, state, zip, totalFloors, ownerName
    return apiClient.post('/hostels', hostelData)
  },

  // Get all hostels (for owner)
  getAllHostels: () => {
    // TODO: Verify endpoint
    return apiClient.get('/hostels')
  },

  // Get single hostel by ID
  getHostelById: (hostelId) => {
    // TODO: Verify endpoint
    return apiClient.get(`/hostels/${hostelId}`)
  },

  // Update hostel
  updateHostel: (hostelId, hostelData) => {
    // TODO: Verify endpoint
    return apiClient.put(`/hostels/${hostelId}`, hostelData)
  },

  // Delete hostel
  deleteHostel: (hostelId) => {
    // TODO: Verify endpoint
    return apiClient.delete(`/hostels/${hostelId}`)
  },
}

export const floorService = {
  // Create a new floor
  createFloor: (hostelId, floorData) => {
    // TODO: Verify endpoint and fields from backend
    // Expected fields: floorNumber, totalRooms
    return apiClient.post(`/hostels/${hostelId}/floors`, floorData)
  },

  // Get all floors for a hostel
  getFloorsByHostel: (hostelId) => {
    // TODO: Verify endpoint
    return apiClient.get(`/hostels/${hostelId}/floors`)
  },

  // Get single floor
  getFloorById: (hostelId, floorId) => {
    // TODO: Verify endpoint
    return apiClient.get(`/hostels/${hostelId}/floors/${floorId}`)
  },

  // Update floor
  updateFloor: (hostelId, floorId, floorData) => {
    // TODO: Verify endpoint
    return apiClient.put(`/hostels/${hostelId}/floors/${floorId}`, floorData)
  },

  // Delete floor
  deleteFloor: (hostelId, floorId) => {
    // TODO: Verify endpoint
    return apiClient.delete(`/hostels/${hostelId}/floors/${floorId}`)
  },
}

export const roomService = {
  // Create a new room
  createRoom: (hostelId, floorId, roomData) => {
    // Backend expects: /hostels/{hostelId}/{floorId}/rooms
    return apiClient.post(`/hostels/${hostelId}/${floorId}/rooms`, roomData);
  },

  // Get all rooms for a floor
  getRoomsByFloor: (hostelId, floorId) => {
    // Backend expects: /hostels/{hostelId}/{floorId}/rooms
    return apiClient.get(`/hostels/${hostelId}/${floorId}/rooms`);
  },

  // Get only active rooms for a floor
  getActiveRoomsByFloor: (hostelId, floorId) => {
    // Backend expects: /hostels/{hostelId}/{floorId}/rooms/active
    return apiClient.get(`/hostels/${hostelId}/${floorId}/rooms/active`);
  },

  // Get, update, delete room by id (if needed)
  getRoomById: (hostelId, roomId) => apiClient.get(`/hostels/${hostelId}/rooms/${roomId}`),
  updateRoom: (hostelId, roomId, roomData) => apiClient.put(`/hostels/${hostelId}/rooms/${roomId}`, roomData),
  deleteRoom: (hostelId, roomId) => apiClient.delete(`/hostels/${hostelId}/rooms/${roomId}`),
};

export default { hostelService, floorService, roomService }
