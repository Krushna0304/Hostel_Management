import apiClient from "./apiClient";

const agreementService = {
  createRoomAgreement: (data) => apiClient.post("/api/agreements/room", data),
  getAgreementByQrToken: (token) => apiClient.get(`/api/agreements/qr/${token}`),
  getQrCodeImage: (token) => apiClient.get(`/api/agreements/qr/${token}/image`),
  acceptAgreement: (agreementId, data) => apiClient.post(`/api/agreements/${agreementId}/accept`, data),
  rejectAgreement: (agreementId) => apiClient.post(`/api/agreements/${agreementId}/reject`),
  getAllAgreements: () => apiClient.get("/api/agreements"),
  getActivePlans: () => apiClient.get("/api/plans/active"),
  getPlanById: (planId) => apiClient.get(`/api/plans/${planId}`),
};

export default agreementService;
