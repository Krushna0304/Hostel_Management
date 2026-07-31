import { useState, useEffect } from 'react';
import { Button } from './ui/Button';
import { Alert } from './ui/Alert';
import SelectField from './ui/SelectField';
import extensionService from '../services/extensionService';
import agreementService from '../services/agreementService';
import { useSuccessPopup } from '../hooks/useSuccessPopup';

const ExtensionRequestModal = ({ isOpen, onClose, agreement, onSuccess }) => {
  const [formData, setFormData] = useState({
    currentAgreementId: '',
    planId: '',
    tenantNotes: ''
  });
  const [availablePlans, setAvailablePlans] = useState([]);
  const [selectedPlan, setSelectedPlan] = useState(null);
  const [loading, setLoading] = useState(false);
  const [loadingPlans, setLoadingPlans] = useState(false);
  const [error, setError] = useState('');
  const { showSuccess } = useSuccessPopup();

  // Load available plans when modal opens
  useEffect(() => {
    if (isOpen && agreement) {
      setFormData(prev => ({
        ...prev,
        currentAgreementId: agreement.id
      }));
      loadAvailablePlans();
    }
  }, [isOpen, agreement]);

  const loadAvailablePlans = async () => {
    setLoadingPlans(true);
    try {
      // Get active plans from the API - assuming we need room/shared plans
      const response = await agreementService.getActivePlans('ROOM');
      setAvailablePlans(response.data || []);
    } catch (error) {
      console.error('Error loading plans:', error);
      setError('Failed to load available plans');
    } finally {
      setLoadingPlans(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      console.log('Submitting extension request:', formData);

      // Check if we have a valid auth token
      const token = localStorage.getItem('authToken');
      if (!token) {
        setError('Authentication token not found. Please log in again.');
        return;
      }

      const extensionData = {
        currentAgreementId: formData.currentAgreementId,
        planId: formData.planId,
        tenantNotes: formData.tenantNotes || null
      };

      const result = await extensionService.createExtensionRequest(extensionData);
      console.log('Extension request result:', result);
      
      showSuccess('Extension request submitted successfully!');
      onSuccess?.();
      onClose();
      
      // Reset form
      setFormData({
        currentAgreementId: '',
        planId: '',
        tenantNotes: ''
      });
      setSelectedPlan(null);
      
    } catch (error) {
      console.error('Extension request error:', error);
      
      let errorMessage = 'Failed to submit extension request';
      
      if (error.response?.status === 403) {
        setError('Your extension request has been submitted, but there was an authentication issue. Please refresh the page to see your request.');
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

    // If plan is selected, find the plan details
    if (name === 'planId' && value) {
      const plan = availablePlans.find(p => p.planId === value);
      setSelectedPlan(plan);
    }
  };

  if (!isOpen) return null;

  // Calculate current agreement end date
  const currentEndDate = agreement?.endDate ? new Date(agreement.endDate).toLocaleDateString() : 'N/A';
  const extensionStartDate = agreement?.endDate ? new Date(agreement.endDate) : new Date();
  
  // Calculate extension end date if plan is selected
  let extensionEndDate = null;
  if (selectedPlan && selectedPlan.durationMonths) {
    extensionEndDate = new Date(extensionStartDate);
    extensionEndDate.setMonth(extensionEndDate.getMonth() + selectedPlan.durationMonths);
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 w-full max-w-lg mx-4 max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-semibold">Request Allotment Extension</h2>
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
          <h3 className="font-medium mb-2">Current Agreement Details</h3>
          <div className="text-sm text-gray-600 space-y-1">
            <p><span className="font-medium">Room:</span> {agreement?.roomNumber || 'N/A'}</p>
            <p><span className="font-medium">Current End Date:</span> {currentEndDate}</p>
            <p><span className="font-medium">Status:</span> {agreement?.status || 'N/A'}</p>
          </div>
        </div>

        <div className="mb-4 p-4 bg-blue-50 rounded-lg border border-blue-200">
          <h4 className="font-medium text-blue-800 mb-2">Extension Information</h4>
          <ul className="text-sm text-blue-700 space-y-1">
            <li>• Extension will start from your current agreement end date</li>
            <li>• Choose a plan that fits your accommodation needs</li>
            <li>• Payment will be required after owner approval</li>
            <li>• You'll continue staying in the same room</li>
          </ul>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <SelectField
            label="Select Extension Plan"
            name="planId"
            value={formData.planId}
            onChange={handleChange}
            required
            disabled={loadingPlans}
            options={availablePlans.map(plan => ({
              value: plan.planId,
              label: `${plan.planName} - ${plan.durationMonths} months - ₹${plan.monthlyRent}/month`
            }))}
            placeholder={loadingPlans ? 'Loading plans...' : 'Select a plan'}
          />

          {selectedPlan && (
            <div className="p-4 bg-green-50 rounded-lg border border-green-200">
              <h4 className="font-medium text-green-800 mb-2">Selected Plan Details</h4>
              <div className="text-sm text-green-700 space-y-1">
                <p><span className="font-medium">Plan:</span> {selectedPlan.planName}</p>
                <p><span className="font-medium">Duration:</span> {selectedPlan.durationMonths} months</p>
                <p><span className="font-medium">Monthly Rent:</span> ₹{selectedPlan.monthlyRent}</p>
                <p><span className="font-medium">Security Deposit:</span> ₹{selectedPlan.securityDeposit}</p>
                {extensionEndDate && (
                  <>
                    <p><span className="font-medium">Extension Start:</span> {extensionStartDate.toLocaleDateString()}</p>
                    <p><span className="font-medium">Extension End:</span> {extensionEndDate.toLocaleDateString()}</p>
                  </>
                )}
              </div>
            </div>
          )}

          <div className="block space-y-2">
            <span className="flex items-center gap-1 text-sm font-medium text-slate-700">
              Additional Notes (Optional)
            </span>
            <textarea
              name="tenantNotes"
              value={formData.tenantNotes}
              onChange={handleChange}
              placeholder="Any additional information about your extension request..."
              rows={3}
              maxLength={500}
              className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 shadow-sm outline-none transition placeholder:text-slate-400 focus:border-sky-400 focus:ring-4 focus:ring-sky-100 resize-none"
            />
            <p className="text-sm text-slate-500">This information will help your owner process your request</p>
          </div>

          <div className="mb-4 p-4 bg-amber-50 rounded-lg">
            <h4 className="font-medium text-amber-800 mb-2">What happens next?</h4>
            <ul className="text-sm text-amber-700 space-y-1">
              <li>1. Your owner will review your extension request</li>
              <li>2. A new agreement will be created upon approval</li>
              <li>3. You'll need to complete payment to activate the extension</li>
              <li>4. Your accommodation will continue seamlessly</li>
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
              variant="primary"
              className="flex-1"
              disabled={!formData.planId || loadingPlans}
            >
              Submit Extension Request
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ExtensionRequestModal;