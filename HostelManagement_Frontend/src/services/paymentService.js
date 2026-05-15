import apiClient from './apiClient'

export const paymentService = {
  // Agreement Payments
  createAgreementOrder: (orderData) => apiClient.post('/api/payments/create-order', orderData),
  
  createInstallmentOrder: (orderData) => apiClient.post('/api/payments/create-installment-order', orderData),
  
  verifyAgreementPayment: (verificationData) => apiClient.post('/api/payments/verify-payment', verificationData),
  
  // Other Charges Payment Orders
  createOtherChargeOrder: (orderData) => apiClient.post('/api/payments/create-other-charge-order', orderData),
  
  createOtherChargeInstallmentOrder: (orderData) => apiClient.post('/api/payments/create-other-charge-installment-order', orderData),
  
  verifyOtherChargePayment: (verificationData) => apiClient.post('/api/payments/verify-other-charge-payment', verificationData),
  
  verifyOtherChargeInstallmentPayment: (verificationData) => apiClient.post('/api/payments/verify-other-charge-installment-payment', verificationData),
  
  // Refund Payment
  refundPayment: (refundData) => apiClient.post('/api/payments/refund', refundData),

  // Payment Status
  getPaymentStatus: (paymentId) => apiClient.get(`/api/payments/${paymentId}/status`),
  
  // Payment History
  getPaymentHistory: (params) => apiClient.get('/api/payments/history', { params }),

  // My full transaction history (sent + received) — works for Owner and Tenant
  getMyTransactionHistory: () => apiClient.get('/api/transactions/my-history'),
}

export default paymentService