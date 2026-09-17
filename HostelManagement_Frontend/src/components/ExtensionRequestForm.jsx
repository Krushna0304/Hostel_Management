import { useState, useEffect } from 'react';
import { Button } from './ui/Button';
import { InputField } from './ui/InputField';
import { Alert } from './ui/Alert';
import SelectField from './ui/SelectField';
import extensionService from '../services/extensionService';
import agreementService from '../services/agreementService';
import { useSuccessPopup } from '../hooks/useSuccessPopup';

const toExtensionPlan = (plan) => ({
  ...plan,
  planId: plan.id ?? plan.planId,
  durationMonths: plan.duration?.value ?? plan.durationMonths ?? 0,
  monthlyRent: plan.rentDetails?.monthlyRent ?? plan.monthlyRent ?? 0,
  securityDeposit: plan.charges?.securityDeposit?.amount ?? plan.securityDeposit ?? 0,
});

/**
 * Extension Request Form Component for Tenants
 * 
 * This component provides a comprehensive form for tenants to request
 * allotment extensions with real-time plan calculations and previews.
 * 
 * Features:
 * - Plan selection with real-time cost calculation
 * - Extension period preview
 * - Form validation and error handling
 * - Success notifications
 * - Mobile-responsive design
 * 
 * Props:
 * @param {Object} agreement - Current agreement details
 * @param {Function} onSuccess - Callback when extension request is successful
 * @param {Function} onCancel - Callback when form is cancelled
 * @param {boolean} showCancelButton - Whether to show cancel button (default: true)
 * @param {string} className - Additional CSS classes
 */
const ExtensionRequestForm = ({ 
  agreement, 
  onSuccess, 
  onCancel, 
  showCancelButton = true,
  className = ''
}) => {
  const [formData, setFormData] = useState({
    currentAgreementId: '',
    planId: '',
    tenantNotes: ''
  });
  const [availablePlans, setAvailablePlans] = useState([]);
  const [selectedPlan, setSelectedPlan] = useState(null);
  const [extensionCalculation, setExtensionCalculation] = useState(null);
  const [loading, setLoading] = useState(false);
  const [loadingPlans, setLoadingPlans] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const { showSuccess } = useSuccessPopup();

  // Initialize form with agreement data
  useEffect(() => {
    if (agreement) {
      setFormData(prev => ({
        ...prev,
        currentAgreementId: agreement.id
      }));
      loadAvailablePlans();
    }
  }, [agreement]);

  // Calculate extension details when plan is selected
  useEffect(() => {
    if (selectedPlan && agreement) {
      calculateExtensionDetails();
    } else {
      setExtensionCalculation(null);
    }
  }, [selectedPlan, agreement]);

  /**
   * Load available plans for room type
   */
  const loadAvailablePlans = async () => {
    setLoadingPlans(true);
    setError('');
    
    try {
      // Agreements and plans use the same canonical room type.
      const planType = agreement?.type || 'PG_ROOM';
      const response = await agreementService.getActivePlans(planType);
      
      const plans = (response.data || []).map(toExtensionPlan);
      setAvailablePlans(plans);
      
      if (plans.length === 0) {
        setError('No extension plans are currently available for your room type.');
      }
    } catch (error) {
      console.error('Error loading plans:', error);
      setError('Failed to load available plans. Please try again.');
    } finally {
      setLoadingPlans(false);
    }
  };

  /**
   * Calculate extension period and cost details
   */
  const calculateExtensionDetails = () => {
    if (!selectedPlan || !agreement) return;

    try {
      const currentEndDate = new Date(agreement.endDate);
      const extensionStartDate = new Date(currentEndDate);
      const extensionEndDate = new Date(extensionStartDate);
      extensionEndDate.setMonth(extensionEndDate.getMonth() + (selectedPlan.durationMonths || 12));

      // Calculate costs (this is a preview - actual calculation happens on backend)
      const monthlyRent = Number(selectedPlan.monthlyRent) || 0;
      const securityDeposit = Number(selectedPlan.securityDeposit) || 0;
      const activationAmount = monthlyRent; // First month rent
      const totalAmount = activationAmount + securityDeposit;

      setExtensionCalculation({
        startDate: extensionStartDate,
        endDate: extensionEndDate,
        durationMonths: selectedPlan.durationMonths || 12,
        monthlyRent: monthlyRent,
        securityDeposit: securityDeposit,
        activationAmount: activationAmount,
        estimatedTotal: totalAmount
      });
    } catch (error) {
      console.error('Error calculating extension details:', error);
      setError('Failed to calculate extension details.');
    }
  };

  /**
   * Handle form input changes
   */
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));

    // Update selected plan when plan is changed
    if (name === 'planId' && value) {
      const plan = availablePlans.find(p => p.planId === value);
      setSelectedPlan(plan);
    } else if (name === 'planId' && !value) {
      setSelectedPlan(null);
    }
  };

  /**
   * Handle form submission
   */
  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');

    try {
      // Validate required fields
      if (!formData.currentAgreementId || !formData.planId) {
        setError('Please select a plan for your extension.');
        return;
      }

      // Check authentication
      const token = localStorage.getItem('authToken');
      if (!token) {
        setError('Authentication token not found. Please log in again.');
        return;
      }

      const extensionData = {
        currentAgreementId: formData.currentAgreementId,
        planId: formData.planId,
        tenantNotes: formData.tenantNotes.trim() || null
      };

      console.log('Submitting extension request:', extensionData);

      const result = await extensionService.createExtensionRequest(extensionData);
      console.log('Extension request result:', result);
      
      // Show success message
      const successMessage = `Extension request submitted successfully! Request ID: ${result.requestId}`;
      setSuccess(successMessage);
      showSuccess('Extension request submitted successfully!');
      
      // Call success callback after a brief delay to show success message
      setTimeout(() => {
        onSuccess?.(result);
      }, 1500);
      
      // Reset form
      resetForm();
      
    } catch (error) {
      console.error('Extension request error:', error);
      
      // Handle specific error scenarios
      if (error.response?.status === 403) {
        setError('Your extension request has been submitted, but there was an authentication issue. Please refresh the page to see your request.');
        setTimeout(() => {
          onSuccess?.();
        }, 2000);
        return;
      } else if (error.response?.status === 401) {
        setError('Your session has expired. Please log in again.');
        setTimeout(() => {
          localStorage.removeItem('authToken');
          localStorage.removeItem('userRole');
          window.location.href = '/login';
        }, 2000);
        return;
      }
      
      // Extract error message
      let errorMessage = 'Failed to submit extension request';
      if (error.response?.data?.message) {
        errorMessage = error.response.data.message;
      } else if (error.message) {
        errorMessage = error.message;
      }
      
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Reset form to initial state
   */
  const resetForm = () => {
    setFormData({
      currentAgreementId: agreement?.id || '',
      planId: '',
      tenantNotes: ''
    });
    setSelectedPlan(null);
    setExtensionCalculation(null);
  };

  /**
   * Format date for display
   */
  const formatDate = (date) => {
    return date ? new Date(date).toLocaleDateString() : 'N/A';
  };

  if (!agreement) {
    return (
      <div className={`p-6 ${className}`}>
        <Alert type="warning">
          No active agreement found. Please ensure you have an active agreement to request an extension.
        </Alert>
      </div>
    );
  }

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Header Section */}
      <div>
        <h2 className="text-xl font-semibold text-slate-900 mb-2">
          Request Allotment Extension
        </h2>
        <p className="text-sm text-slate-600">
          Extend your current accommodation with a seamless transition to a new agreement.
        </p>
      </div>

      {/* Success Message */}
      {success && (
        <Alert type="success" className="mb-4">
          {success}
        </Alert>
      )}

      {/* Error Message */}
      {error && (
        <Alert type="error" className="mb-4">
          {error}
        </Alert>
      )}

      {/* Current Agreement Info */}
      <div className="p-4 bg-slate-50 rounded-lg border border-slate-200">
        <h3 className="font-medium text-slate-900 mb-3">Current Agreement Details</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
          <div>
            <span className="font-medium text-slate-700">Room:</span>
            <span className="ml-2 text-slate-900">{agreement.roomNumber || 'N/A'}</span>
          </div>
          <div>
            <span className="font-medium text-slate-700">Current End Date:</span>
            <span className="ml-2 text-slate-900">{formatDate(agreement.endDate)}</span>
          </div>
          <div>
            <span className="font-medium text-slate-700">Status:</span>
            <span className="ml-2 text-slate-900">{agreement.status || 'N/A'}</span>
          </div>
          <div>
            <span className="font-medium text-slate-700">Agreement Type:</span>
            <span className="ml-2 text-slate-900">{agreement.type || 'PG_ROOM'}</span>
          </div>
        </div>
      </div>

      {/* Extension Information */}
      <div className="p-4 bg-blue-50 rounded-lg border border-blue-200">
        <h4 className="font-medium text-blue-800 mb-2">📋 Extension Information</h4>
        <ul className="text-sm text-blue-700 space-y-1">
          <li>• Extension will start automatically from your current agreement end date</li>
          <li>• You'll continue staying in the same room without any interruption</li>
          <li>• Choose a plan that matches your accommodation needs</li>
          <li>• Payment will be required after owner approval</li>
          <li>• New agreement will be created upon successful payment</li>
        </ul>
      </div>

      {/* Extension Request Form */}
      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Plan Selection */}
        <SelectField
          label="Select Extension Plan"
          name="planId"
          value={formData.planId}
          onChange={handleChange}
          required
          disabled={loadingPlans || loading}
          options={availablePlans.map(plan => ({
            value: plan.planId,
            label: `${plan.planName} - ${plan.durationMonths || 12} months - ₹${Number(plan.monthlyRent || 0).toLocaleString()}/month`
          }))}
          placeholder={loadingPlans ? 'Loading available plans...' : 'Select a plan for your extension'}
          error={!loadingPlans && availablePlans.length === 0 ? 'No plans available' : ''}
        />

        {/* Selected Plan Details */}
        {selectedPlan && (
          <div className="p-4 bg-green-50 rounded-lg border border-green-200">
            <h4 className="font-medium text-green-800 mb-3">✅ Selected Plan Details</h4>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm text-green-700">
              <div>
                <span className="font-medium">Plan Name:</span>
                <span className="ml-2">{selectedPlan.planName}</span>
              </div>
              <div>
                <span className="font-medium">Duration:</span>
                <span className="ml-2">{selectedPlan.durationMonths || 12} months</span>
              </div>
              <div>
                <span className="font-medium">Monthly Rent:</span>
                <span className="ml-2">₹{Number(selectedPlan.monthlyRent || 0).toLocaleString()}</span>
              </div>
              <div>
                <span className="font-medium">Security Deposit:</span>
                <span className="ml-2">₹{Number(selectedPlan.securityDeposit || 0).toLocaleString()}</span>
              </div>
            </div>
          </div>
        )}

        {/* Extension Period Preview */}
        {extensionCalculation && (
          <div className="p-4 bg-purple-50 rounded-lg border border-purple-200">
            <h4 className="font-medium text-purple-800 mb-3">📅 Extension Period Preview</h4>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between items-center text-purple-700">
                <span className="font-medium">Extension Start:</span>
                <span>{formatDate(extensionCalculation.startDate)}</span>
              </div>
              <div className="flex justify-between items-center text-purple-700">
                <span className="font-medium">Extension End:</span>
                <span>{formatDate(extensionCalculation.endDate)}</span>
              </div>
              <div className="flex justify-between items-center text-purple-700">
                <span className="font-medium">Total Duration:</span>
                <span>{extensionCalculation.durationMonths} months</span>
              </div>
              <hr className="border-purple-200" />
              <div className="flex justify-between items-center text-purple-800 font-semibold">
                <span>Estimated Activation Amount:</span>
                <span>₹{Number(extensionCalculation.activationAmount).toLocaleString()}</span>
              </div>
              <p className="text-xs text-purple-600 mt-2">
                * Final amounts will be calculated by the system after owner approval
              </p>
            </div>
          </div>
        )}

        {/* Additional Notes */}
        <div className="space-y-2">
          <label className="flex items-center gap-1 text-sm font-medium text-slate-700">
            Additional Notes
            <span className="text-slate-500">(Optional)</span>
          </label>
          <textarea
            name="tenantNotes"
            value={formData.tenantNotes}
            onChange={handleChange}
            placeholder="Any additional information about your extension request..."
            rows={3}
            maxLength={500}
            disabled={loading}
            className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 shadow-sm outline-none transition placeholder:text-slate-400 focus:border-sky-400 focus:ring-4 focus:ring-sky-100 resize-none disabled:opacity-60 disabled:cursor-not-allowed"
          />
          <div className="flex justify-between text-sm text-slate-500">
            <span>This information will help your owner process your request</span>
            <span>{formData.tenantNotes.length}/500</span>
          </div>
        </div>

        {/* Process Information */}
        <div className="p-4 bg-amber-50 rounded-lg border border-amber-200">
          <h4 className="font-medium text-amber-800 mb-2">🔄 What happens next?</h4>
          <ol className="text-sm text-amber-700 space-y-1 list-decimal list-inside">
            <li>Your owner will review your extension request</li>
            <li>A new agreement will be created upon approval</li>
            <li>You'll receive a notification to complete payment</li>
            <li>Your accommodation will continue seamlessly after payment</li>
            <li>You can track the status in your dashboard</li>
          </ol>
        </div>

        {/* Form Actions */}
        <div className="flex gap-3 pt-4">
          {showCancelButton && onCancel && (
            <Button
              type="button"
              variant="secondary"
              onClick={onCancel}
              disabled={loading}
              className="flex-1"
            >
              Cancel
            </Button>
          )}
          <Button
            type="submit"
            loading={loading}
            variant="primary"
            className="flex-1"
            disabled={!formData.planId || loadingPlans || loading}
          >
            {loading ? 'Submitting Request...' : 'Submit Extension Request'}
          </Button>
        </div>
      </form>

      {/* Help Information */}
      <div className="text-xs text-slate-500 space-y-1">
        <p>💡 <strong>Tip:</strong> Extension requests are typically processed within 24-48 hours.</p>
        <p>📞 <strong>Need help?</strong> Contact your property owner if you have questions about available plans.</p>
      </div>
    </div>
  );
};

export default ExtensionRequestForm;
