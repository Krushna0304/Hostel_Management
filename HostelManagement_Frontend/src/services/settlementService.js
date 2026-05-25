import apiClient from './apiClient';

const settlementService = {
  // Initiate settlement request with improved error handling
  async initiateSettlement(settlementData) {
    try {
      console.log('Initiating settlement request with data:', settlementData);
      
      // Check authentication before making the request
      const token = localStorage.getItem('authToken');
      if (!token) {
        throw new Error('Authentication token not found. Please log in again.');
      }
      
      const response = await apiClient.post('/api/settlements/request', settlementData);
      console.log('Settlement request successful:', response.data);
      return response.data;
    } catch (error) {
      console.error('Error initiating settlement:', error);
      
      // Handle specific error cases
      if (error.response?.status === 403) {
        // Check if the settlement was actually created despite the 403 error
        console.log('Got 403 error, but settlement might have been created');
        
        // Wait a moment and try to fetch settlements to see if it was created
        try {
          await new Promise(resolve => setTimeout(resolve, 1000));
          const settlements = await this.getTenantSettlements();
          
          // Check if a settlement with this agreement ID was just created
          const recentSettlement = settlements.find(s => 
            s.agreementId === settlementData.agreementId && 
            new Date(s.createdAt) > new Date(Date.now() - 30000) // Created in last 30 seconds
          );
          
          if (recentSettlement) {
            console.log('Settlement was actually created successfully:', recentSettlement);
            return recentSettlement;
          }
        } catch (fetchError) {
          console.error('Error checking if settlement was created:', fetchError);
        }
      }
      
      throw error;
    }
  },

  // Get settlement calculation
  async getSettlementCalculation(settlementId) {
    try {
      const response = await apiClient.get(`/api/settlements/${settlementId}/calculate`);
      return response.data;
    } catch (error) {
      console.error('Error getting settlement calculation:', error);
      throw error;
    }
  },

  // Approve settlement (owner)
  async approveSettlement(settlementId, approvalData) {
    try {
      const response = await apiClient.post(`/api/settlements/${settlementId}/approve`, approvalData);
      return response.data;
    } catch (error) {
      console.error('Error approving settlement:', error);
      throw error;
    }
  },

  // Complete settlement
  async completeSettlement(settlementId, paymentReference) {
    try {
      const response = await apiClient.post(`/api/settlements/${settlementId}/complete`, {
        paymentReference
      });
      return response.data;
    } catch (error) {
      console.error('Error completing settlement:', error);
      throw error;
    }
  },

  // Get owner settlements
  async getOwnerSettlements() {
    try {
      const response = await apiClient.get('/api/settlements/owner');
      return response.data;
    } catch (error) {
      console.error('Error getting owner settlements:', error);
      throw error;
    }
  },

  // Get tenant settlements with retry logic for 403 errors
  async getTenantSettlements(retryCount = 0) {
    try {
      const response = await apiClient.get('/api/settlements/tenant');
      return response.data;
    } catch (error) {
      console.error('Error getting tenant settlements:', error);
      
      // If it's a 403 error and we haven't retried yet, try once more after a short delay
      if (error.response?.status === 403 && retryCount < 2) {
        console.log(`Retrying tenant settlements request (attempt ${retryCount + 1})...`);
        await new Promise(resolve => setTimeout(resolve, 1000 * (retryCount + 1)));
        return this.getTenantSettlements(retryCount + 1);
      }
      
      // If still failing, return empty array to prevent UI crashes
      if (error.response?.status === 403) {
        console.warn('Unable to fetch tenant settlements due to authentication issues, returning empty array');
        return [];
      }
      
      throw error;
    }
  },

  // Check if a settlement was recently created for a specific agreement
  async checkRecentSettlement(agreementId, maxAgeMinutes = 5) {
    try {
      const settlements = await this.getTenantSettlements();
      const cutoffTime = new Date(Date.now() - (maxAgeMinutes * 60 * 1000));
      
      return settlements.find(settlement => 
        settlement.agreementId === agreementId && 
        new Date(settlement.createdAt) > cutoffTime
      );
    } catch (error) {
      console.error('Error checking recent settlement:', error);
      return null;
    }
  }
};

export default settlementService;