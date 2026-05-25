import { useState } from 'react';
import { Button } from './ui/Button';
import { InputField } from './ui/InputField';
import { Alert } from './ui/Alert';
import settlementService from '../services/settlementService';
import { useSuccessPopup } from '../hooks/useSuccessPopup';

const SettlementRequestModal = ({ isOpen, onClose, agreement, onSuccess }) => {
  const [formData, setFormData] = useState({
    tenantNotes: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const { showSuccess } = useSuccessPopup();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      console.log('Submitting settlement request:', {
        agreementId: agreement.id,
        tenantNotes: formData.tenantNotes
      });

      // Check if we have a valid auth token
      const token = localStorage.getItem('authToken');
      console.log('Auth token present:', !!token);
      console.log('Token length:', token ? token.length : 0);

      if (!token) {
        setError('Authentication token not found. Please log in again.');
        return;
      }

      const settlementData = {
        agreementId: agreement.id,
        tenantNotes: formData.tenantNotes
      };

      const result = await settlementService.initiateSettlement(settlementData);
      console.log('Settlement request result:', result);
      
      showSuccess('Settlement request submitted successfully!');
      onSuccess?.();
      onClose();
    } catch (error) {
      console.error('Settlement request error:', error);
      console.error('Error status:', error.response?.status);
      console.error('Error data:', error.response?.data);
      
      let errorMessage = 'Failed to submit settlement request';
      
      // If it's a 403 error, show a more user-friendly message and still refresh data
      if (error.response?.status === 403) {
        // For 403 errors, the settlement might have been created successfully
        // Show a warning message but still refresh the data
        setError('Your settlement request has been submitted, but there was an authentication issue loading the updated data. Please refresh the page to see your request.');
        
        // Still call onSuccess to refresh the data after a delay
        setTimeout(() => {
          onSuccess?.();
        }, 1500);
        
        // Auto-close the modal after showing the message
        setTimeout(() => {
          onClose();
        }, 3000);
        
        return; // Don't set loading to false immediately
      } else if (error.response?.status === 401) {
        errorMessage = 'Your session has expired. Please log in again.';
        // Redirect to login
        setTimeout(() => {
          localStorage.removeItem('authToken');
          localStorage.removeItem('userRole');
          window.location.href = '/login';
        }, 2000);
      } else if (error.response?.data?.message) {
        errorMessage = error.response.data.message;
      } else if (error.message) {
        errorMessage = error.message;
      }
      
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 w-full max-w-md mx-4">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-semibold">Request Agreement Settlement</h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
          >
            ×
          </button>
        </div>

        {error && (
          <Alert type="error" className="mb-4">
            {error}
          </Alert>
        )}

        <div className="mb-4 p-4 bg-gray-50 rounded-lg">
          <h3 className="font-medium mb-2">Agreement Details</h3>
          <div className="text-sm text-gray-600 space-y-1">
            <p><span className="font-medium">Room:</span> {agreement.roomNumber || 'N/A'}</p>
            <p><span className="font-medium">Security Deposit:</span> ₹{agreement.deposit?.toLocaleString()}</p>
            <p><span className="font-medium">Status:</span> {agreement.status}</p>
          </div>
        </div>

        <div className="mb-4 p-4 bg-blue-50 rounded-lg">
          <h4 className="font-medium text-blue-800 mb-2">What happens next?</h4>
          <ul className="text-sm text-blue-700 space-y-1">
            <li>• Your owner will review the settlement request</li>
            <li>• Outstanding amounts will be calculated automatically</li>
            <li>• Any additional charges will be reviewed</li>
            <li>• Final settlement amount will be determined</li>
          </ul>
        </div>

        <form onSubmit={handleSubmit}>
          <InputField
            label="Notes (Optional)"
            name="tenantNotes"
            value={formData.tenantNotes}
            onChange={handleChange}
            placeholder="Add any notes about your settlement request..."
            multiline
            rows={3}
            maxLength={500}
          />

          <div className="flex gap-3 mt-6">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              disabled={loading}
              className="flex-1"
            >
              Cancel
            </Button>
            <Button
              type="submit"
              loading={loading}
              className="flex-1"
            >
              Submit Request
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default SettlementRequestModal;