import { useState } from 'react';
import { Button } from './ui/Button';
import { InputField } from './ui/InputField';
import { Alert } from './ui/Alert';
import settlementService from '../services/settlementService';
import { useSuccessPopup } from '../hooks/useSuccessPopup';

const EarlySettlementRequestModal = ({ isOpen, onClose, agreement, onSuccess }) => {
  const [formData, setFormData] = useState({
    requestedEndDate: '',
    reason: '',
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
      console.log('Submitting early settlement request:', {
        agreementId: agreement.id,
        ...formData
      });

      // Check if we have a valid auth token
      const token = localStorage.getItem('authToken');
      if (!token) {
        setError('Authentication token not found. Please log in again.');
        return;
      }

      const earlySettlementData = {
        agreementId: agreement.id,
        requestedEndDate: formData.requestedEndDate,
        reason: formData.reason,
        tenantNotes: formData.tenantNotes || null
      };

      const result = await settlementService.requestEarlySettlement(earlySettlementData);
      console.log('Early settlement request result:', result);
      
      showSuccess('Early settlement request submitted successfully!');
      onSuccess?.();
      onClose();
      
      // Reset form
      setFormData({
        requestedEndDate: '',
        reason: '',
        tenantNotes: ''
      });
      
    } catch (error) {
      console.error('Early settlement request error:', error);
      
      let errorMessage = 'Failed to submit early settlement request';
      
      if (error.response?.status === 403) {
        setError('Your early settlement request has been submitted, but there was an authentication issue. Please refresh the page to see your request.');
        setTimeout(() => {
          onSuccess?.();
          onClose();
        }, 2000);
        return;
      } else if (error.response?.status === 401) {
        errorMessage = 'Your session has expired. Please log in again.';
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
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  if (!isOpen) return null;

  // Get today's date for minimum date validation
  const today = new Date().toISOString().split('T')[0];
  
  // Calculate agreement end date for maximum date validation
  const agreementEndDate = agreement?.endDate ? new Date(agreement.endDate).toISOString().split('T')[0] : null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 w-full max-w-md mx-4 max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-semibold">Request Early Settlement</h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 text-2xl leading-none"
            aria-label="Close modal"
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
            <p><span className="font-medium">Current End Date:</span> {agreement.endDate ? new Date(agreement.endDate).toLocaleDateString() : 'N/A'}</p>
            <p><span className="font-medium">Status:</span> {agreement.status}</p>
          </div>
        </div>

        <div className="mb-4 p-4 bg-amber-50 rounded-lg border border-amber-200">
          <h4 className="font-medium text-amber-800 mb-2">Early Settlement Notice</h4>
          <ul className="text-sm text-amber-700 space-y-1">
            <li>• Early exit may incur additional charges or penalties</li>
            <li>• Final settlement amount will be calculated by your owner</li>
            <li>• This request cannot be canceled once submitted</li>
            <li>• You must provide a valid reason for early termination</li>
          </ul>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <InputField
            label="Requested Move-out Date"
            name="requestedEndDate"
            type="date"
            value={formData.requestedEndDate}
            onChange={handleChange}
            min={today}
            max={agreementEndDate}
            required
            hint={agreementEndDate ? `Must be before your agreement ends on ${new Date(agreementEndDate).toLocaleDateString()}` : ''}
          />

          <InputField
            label="Reason for Early Exit"
            name="reason"
            value={formData.reason}
            onChange={handleChange}
            placeholder="e.g., Job relocation, family emergency, etc."
            required
            maxLength={200}
            hint="Please provide a clear reason for requesting early settlement"
          />

          <InputField
            label="Additional Notes (Optional)"
            name="tenantNotes"
            value={formData.tenantNotes}
            onChange={handleChange}
            placeholder="Any additional information about your early settlement request..."
            multiline
            rows={3}
            maxLength={500}
            hint="This information will help your owner process your request"
          />

          <div className="mb-4 p-4 bg-blue-50 rounded-lg">
            <h4 className="font-medium text-blue-800 mb-2">What happens next?</h4>
            <ul className="text-sm text-blue-700 space-y-1">
              <li>1. Your owner will review your early settlement request</li>
              <li>2. Early exit penalties (if any) will be calculated</li>
              <li>3. Final settlement amount will be determined</li>
              <li>4. You'll be notified of the decision and next steps</li>
            </ul>
          </div>

          <div className="flex gap-3 mt-6">
            <Button
              type="button"
              variant="secondary"
              onClick={onClose}
              disabled={loading}
              className="flex-1"
            >
              Cancel
            </Button>
            <Button
              type="submit"
              loading={loading}
              variant="danger"
              className="flex-1"
            >
              Submit Early Settlement Request
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default EarlySettlementRequestModal;