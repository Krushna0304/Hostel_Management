import apiClient from './apiClient'

class ElectricityBillService {
  // Electricity Account Management
  async createElectricityAccount(accountData) {
    const response = await apiClient.post('/api/electricity/accounts', accountData)
    return response.data
  }

  async getOwnerAccounts() {
    const response = await apiClient.get('/api/electricity/accounts')
    return response.data
  }

  // Electricity Bill Management
  async createElectricityBills(billsData) {
    const response = await apiClient.post('/api/electricity/bills', billsData)
    return response.data
  }

  async getOwnerBills() {
    const response = await apiClient.get('/api/electricity/bills/owner')
    return response.data
  }

  async getTenantBills() {
    const response = await apiClient.get('/api/electricity/bills/tenant')
    return response.data
  }

  async getBillDetails(billId) {
    const response = await apiClient.get(`/api/electricity/bills/${billId}`)
    return response.data
  }

  // Payment Management
  async recordPayment(paymentData) {
    const response = await apiClient.post('/api/electricity/payments', paymentData)
    return response.data
  }

  // Sends a single OTP covering all of the tenant's outstanding bills (pay all, cash)
  async sendPayAllOtp() {
    const response = await apiClient.post('/api/electricity/payments/pay-all-otp')
    return response.data
  }

  // Owner Collections
  async getOwnerCollections() {
    const response = await apiClient.get('/api/electricity/collections/owner')
    return response.data
  }

  async getTenantElectricityHistory(tenantId) {
    const response = await apiClient.get(`/api/electricity/collections/history/${tenantId}`)
    return response.data
  }

  async collectTenantElectricity(tenantId) {
    const response = await apiClient.post(`/api/electricity/collections/collect/${tenantId}`)
    return response.data
  }

  // Utility methods
  formatCurrency(amount) {
    return `₹${Number(amount).toLocaleString()}`
  }

  formatBillPeriod(month, year) {
    const monthNames = [
      'January', 'February', 'March', 'April', 'May', 'June',
      'July', 'August', 'September', 'October', 'November', 'December'
    ]
    return `${monthNames[month - 1]} ${year}`
  }

  getBillStatusColor(status) {
    switch (status) {
      case 'PENDING':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200'
      case 'PARTIAL':
        return 'bg-orange-100 text-orange-800 border-orange-200'
      case 'PAID':
      case 'COMPLETED':
        return 'bg-green-100 text-green-800 border-green-200'
      case 'OVERDUE':
        return 'bg-red-100 text-red-800 border-red-200'
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200'
    }
  }

  getBillStatusIcon(status) {
    switch (status) {
      case 'PENDING':
        return '⏳'
      case 'PARTIAL':
        return '⚡'
      case 'PAID':
      case 'COMPLETED':
        return '✅'
      case 'OVERDUE':
        return '🚨'
      default:
        return '❓'
    }
  }
}

export default new ElectricityBillService()