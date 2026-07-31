import apiClient from './apiClient';

const extensionService = {
  // Create extension request for tenant's current allotment
  async createExtensionRequest(extensionData) {
    try {
      console.log('Creating extension request with data:', extensionData);
      
      const token = localStorage.getItem('authToken');
      if (!token) {
        throw new Error('Authentication token not found. Please log in again.');
      }
      
      const response = await apiClient.post('/api/v1/allotments/extend', extensionData);
      console.log('Extension request created successfully:', response.data);
      return response.data;
    } catch (error) {
      console.error('Error creating extension request:', error);
      throw error;
    }
  },

  // Get tenant's extension requests
  async getTenantExtensionRequests() {
    try {
      const response = await apiClient.get('/api/v1/allotments/extend/tenant');
      return response.data;
    } catch (error) {
      console.error('Error getting tenant extension requests:', error);
      throw error;
    }
  },

  // Get extension requests for a specific tenant (admin/owner access)
  async getTenantExtensionRequestsById(tenantId) {
    try {
      const response = await apiClient.get(`/api/v1/allotments/extend/tenant/${tenantId}`);
      return response.data;
    } catch (error) {
      console.error('Error getting tenant extension requests by ID:', error);
      throw error;
    }
  },

  // Process extension payment
  async processExtensionPayment(requestId, paymentData) {
    try {
      console.log('Processing extension payment:', { requestId, paymentData });
      
      const response = await apiClient.post(`/api/v1/allotments/extend/${requestId}/payment`, paymentData);
      console.log('Extension payment processed successfully:', response.data);
      return response.data;
    } catch (error) {
      console.error('Error processing extension payment:', error);
      throw error;
    }
  },

  // Get extension request details
  async getExtensionRequestDetails(requestId) {
    try {
      const response = await apiClient.get(`/api/v1/allotments/extend/${requestId}`);
      return response.data;
    } catch (error) {
      console.error('Error getting extension request details:', error);
      throw error;
    }
  },

  // Get extension request status
  async getExtensionRequestStatus(requestId) {
    try {
      const response = await apiClient.get(`/api/v1/allotments/extend/${requestId}`);
      return response.data?.status || null;
    } catch (error) {
      console.error('Error getting extension request status:', error);
      throw error;
    }
  },

  // Get owner's extension requests pending approval
  async getOwnerExtensionRequests() {
    try {
      const response = await apiClient.get('/api/v1/allotments/extend/owner');
      return response.data;
    } catch (error) {
      console.error('Error getting owner extension requests:', error);
      throw error;
    }
  },

  // Get pending approval requests for owner
  async getPendingApprovalRequests() {
    try {
      const response = await apiClient.get('/api/v1/allotments/extend/pending-approvals');
      return response.data;
    } catch (error) {
      console.error('Error getting pending approval requests:', error);
      throw error;
    }
  },

  // Approve extension request (owner action)
  async approveExtensionRequest(requestId, approvalData) {
    try {
      console.log('Approving extension request:', { requestId, approvalData });

      const response = await apiClient.put(`/api/v1/allotments/extend/${requestId}/approve`, approvalData);
      console.log('Extension request approved successfully:', response.data);
      return response.data;
    } catch (error) {
      console.error('Error approving extension request:', error);
      throw error;
    }
  }
};

export default extensionService;