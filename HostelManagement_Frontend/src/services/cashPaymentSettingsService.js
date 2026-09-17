import apiClient from './apiClient'

/**
 * Owner settings for which payment flows can be paid in cash via OTP.
 */
class CashPaymentSettingsService {
  async getSettings() {
    const response = await apiClient.get('/api/cash-payment-otp/settings')
    const data = response.data
    // Ensure we always return an array
    if (Array.isArray(data)) {
      return data
    }
    if (data && typeof data === 'object') {
      // If it's an object with a data property that's an array
      if (Array.isArray(data.data)) {
        return data.data
      }
      // If it's a single object, wrap it in an array
      if (!Array.isArray(data)) {
        return [data]
      }
    }
    // Default to empty array
    return []
  }

  async updateSetting(method, allowed) {
    const response = await apiClient.put('/api/cash-payment-otp/settings', { method, allowed })
    return response.data
  }
}

export default new CashPaymentSettingsService()
