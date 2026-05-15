import apiClient from "./apiClient";

const agreementService = {
  createRoomAgreement: (data) => apiClient.post("/api/agreements/room", data),
  createFlatAgreement: (data) => apiClient.post("/api/agreements/flat", data),
  getAgreementByQrToken: (token) => apiClient.get(`/api/agreements/qr/${token}`),
  getQrCodeImage: (token) => apiClient.get(`/api/agreements/qr/${token}/image`),
  acceptAgreement: (agreementId, data) => apiClient.post(`/api/agreements/${agreementId}/accept`, data),
  rejectAgreement: (agreementId) => apiClient.post(`/api/agreements/${agreementId}/reject`),
  getAllAgreements: () => apiClient.get("/api/agreements"),
  getActivePlans: (planType) => apiClient.get(planType ? `/api/plans/active?planType=${planType}` : "/api/plans/active"),
  getPlanById: (planId) => apiClient.get(`/api/plans/${planId}`),
};

export const planService = {
  getMyPlans: () => apiClient.get("/api/plans/my"),
  createPlan: (data) => apiClient.post("/api/plans", data),
  deletePlan: (planId) => apiClient.delete(`/api/plans/${planId}`),
  getPaymentBreakdown: (planId) => apiClient.get(`/api/plans/${planId}/payment-breakdown`),
  getEnhancedBreakdown: (planId) => apiClient.get(`/api/plans/${planId}/enhanced-breakdown`),
  calculateInstallments: (data) => apiClient.post("/api/plans/calculate-installments", data),
};

export const tenantService = {
  getDashboard: () => apiClient.get("/tenant/dashboard"),
  getPaymentSchedule: () => apiClient.get("/tenant/payment-schedule"),
  recordPayment: (scheduleId, data) => apiClient.post(`/tenant/pay/${scheduleId}`, data),
  getMyAgreement: () => apiClient.get("/tenant/agreement"),
};

export const ownerReportService = {
  getCollectionSummary: () => apiClient.get("/owner/reports/collections"),
  getTenantInstallments: (tenantId) => apiClient.get(`/owner/reports/tenant/${tenantId}/installments`),
  getTenantPaymentHistory: (tenantId) => apiClient.get(`/owner/reports/tenant/${tenantId}/payment-history`),
  collectPayment: (scheduleId, data) => apiClient.post(`/owner/reports/collect-payment/${scheduleId}`, data),
  sendInstallmentOtp: (scheduleId) => apiClient.post(`/api/cash-payment-otp/send-installment/${scheduleId}`),
};

export default agreementService;
