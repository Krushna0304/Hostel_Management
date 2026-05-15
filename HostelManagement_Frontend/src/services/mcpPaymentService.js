import apiClient from './apiClient'

/**
 * Service for MCP payment monitoring and control
 */
const mcpPaymentService = {
  /**
   * Get all owner Razorpay configurations
   */
  getAllConfigurations: async () => {
    const response = await apiClient.get('/mcp/payment-monitoring/configurations')
    return response.data
  },

  /**
   * Get configuration for specific owner
   */
  getOwnerConfiguration: async (ownerId) => {
    const response = await apiClient.get(`/mcp/payment-monitoring/configurations/${ownerId}`)
    return response.data
  },

  /**
   * Enable/Disable payments for an owner (MCP override)
   */
  mcpOverride: async (ownerId, overrideData) => {
    const response = await apiClient.post(
      `/mcp/payment-monitoring/configurations/${ownerId}/override`,
      overrideData
    )
    return response.data
  },

  /**
   * Force re-verification for an owner
   */
  forceReVerification: async (ownerId) => {
    const response = await apiClient.post(
      `/mcp/payment-monitoring/configurations/${ownerId}/force-reverify`
    )
    return response.data
  },

  /**
   * Get MCP dashboard statistics
   */
  getStatistics: async () => {
    const response = await apiClient.get('/mcp/payment-monitoring/statistics')
    return response.data
  },
}

export default mcpPaymentService
