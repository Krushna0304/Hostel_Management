import apiClient from './apiClient'

export const otherChargeService = {
  // Owner endpoints
  createCharge: (chargeData) => apiClient.post('/api/other-charges', chargeData),
  
  getOwnerCharges: () => apiClient.get('/owner/other-charges'),
  
  getOwnerChargesPaginated: (params) => apiClient.get('/api/other-charges/owner/paginated', { params }),
  
  updateCharge: (chargeId, chargeData) => apiClient.put(`/api/other-charges/${chargeId}`, chargeData),
  
  deleteCharge: (chargeId) => apiClient.delete(`/api/other-charges/${chargeId}`),
  
  getChargeById: (chargeId) => apiClient.get(`/api/other-charges/${chargeId}`),

  // Tenant endpoints
  getTenantCharges: () => apiClient.get('/tenant/other-charges'),
  
  getTenantChargeDetails: (chargeId) => apiClient.get(`/tenant/other-charges/${chargeId}`),

  // Payment endpoints
  createChargePaymentOrder: (orderData) => apiClient.post('/api/payments/create-other-charge-order', orderData),
  
  createInstallmentPaymentOrder: (orderData) => apiClient.post('/api/payments/create-other-charge-installment-order', orderData),
  
  verifyChargePayment: (verificationData) => apiClient.post('/api/payments/verify-other-charge-payment', verificationData),
  
  verifyInstallmentPayment: (verificationData) => apiClient.post('/api/payments/verify-other-charge-installment-payment', verificationData),

  // Collection endpoints (for owners)
  recordCashPayment: (chargeId, paymentData) => apiClient.post(`/owner/other-charges/${chargeId}/collect-cash`, paymentData),
  
  recordInstallmentCashPayment: (installmentId, paymentData) => apiClient.post(`/owner/other-charges/installments/${installmentId}/collect-cash`, paymentData),

  // Dashboard/Summary endpoints
  getOwnerSummary: () => apiClient.get('/owner/other-charges/summary'),
  
  getTenantSummary: () => apiClient.get('/tenant/other-charges/summary'),

  // Installment management
  getChargeInstallments: (chargeId) => apiClient.get(`/api/other-charges/${chargeId}/installments`),
  
  getInstallmentDetails: (installmentId) => apiClient.get(`/api/other-charges/installments/${installmentId}`),

  // Bulk operations
  bulkCreateCharges: (chargesData) => apiClient.post('/api/other-charges/bulk', chargesData),
  
  bulkUpdateCharges: (updateData) => apiClient.put('/api/other-charges/bulk', updateData),

  // Reports
  getChargeReport: (params) => apiClient.get('/owner/other-charges/report', { params }),
  
  exportChargeReport: (params) => apiClient.get('/owner/other-charges/export', { 
    params, 
    responseType: 'blob' 
  }),

  // Notifications
  sendPaymentReminder: (chargeId) => apiClient.post(`/api/other-charges/${chargeId}/remind`),
  
  sendBulkReminders: (chargeIds) => apiClient.post('/api/other-charges/bulk-remind', { chargeIds }),

  // Search and filters
  searchCharges: (query, filters) => apiClient.get('/api/other-charges/search', { 
    params: { query, ...filters } 
  }),

  // Statistics
  getChargeStatistics: (hostelId, dateRange) => apiClient.get('/api/other-charges/statistics', {
    params: { hostelId, ...dateRange }
  })
}

export default otherChargeService