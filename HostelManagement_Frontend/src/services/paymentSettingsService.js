import apiClient from './apiClient'

/**
 * Service for managing owner Razorpay payment settings
 */
const paymentSettingsService = {
  /**
   * Get current Razorpay configuration
   */
  getConfiguration: async () => {
    const response = await apiClient.get('/owner/payment-settings')
    return response.data
  },

  /**
   * Test Razorpay connection with provided credentials
   */
  testConnection: async (credentials) => {
    const response = await apiClient.post('/owner/payment-settings/test-connection', credentials)
    return response.data
  },

  /**
   * Save and activate Razorpay payments
   */
  saveAndActivate: async (credentials) => {
    const response = await apiClient.post('/owner/payment-settings/save-and-activate', credentials)
    return response.data
  },

  /**
   * Deactivate payments
   */
  deactivate: async () => {
    const response = await apiClient.post('/owner/payment-settings/deactivate')
    return response.data
  },

  /**
   * Check if payments are enabled
   */
  getStatus: async () => {
    const response = await apiClient.get('/owner/payment-settings/status')
    return response.data
  },
}

export default paymentSettingsService
