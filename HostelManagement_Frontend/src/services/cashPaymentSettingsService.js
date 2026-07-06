import apiClient from './apiClient'

/**
 * Owner settings for which payment flows can be paid in cash via OTP.
 */
class CashPaymentSettingsService {
  async getSettings() {
    const response = await apiClient.get('/api/cash-payment-otp/settings')
    return response.data
  }

  async updateSetting(method, allowed) {
    const response = await apiClient.put('/api/cash-payment-otp/settings', { method, allowed })
    return response.data
  }
}

export default new CashPaymentSettingsService()
