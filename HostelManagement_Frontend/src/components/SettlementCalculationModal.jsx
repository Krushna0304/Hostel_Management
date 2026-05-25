import { useState, useEffect } from 'react';
import { Button } from './ui/Button';
import { InputField } from './ui/InputField';
import { Alert } from './ui/Alert';
import { Badge } from './ui/Badge';
import settlementService from '../services/settlementService';
import { useSuccessPopup } from '../hooks/useSuccessPopup';

const SettlementCalculationModal = ({
  isOpen,
  onClose,
  settlementId,
  settlement, // Add settlement object as prop
  onSuccess
}) => {
  const [calculation, setCalculation] = useState(null);

  const [formData, setFormData] = useState({
    damageCharges: 0,
    cleaningCharges: 0,
    otherDeductions: 0,
    ownerNotes: '',
    damageDescription: '',
    approved: true
  });

  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const { showSuccess } = useSuccessPopup();

  useEffect(() => {
    if (isOpen && settlementId) {
      fetchCalculation();
    }
  }, [isOpen, settlementId]);

  const fetchCalculation = async () => {
    setLoading(true);
    setError('');

    try {
      const data =
        await settlementService.getSettlementCalculation(
          settlementId
        );

      setCalculation(data);

      // Pre-fill form with existing data
      setFormData({
        damageCharges: data.damageCharges || 0,
        cleaningCharges: data.cleaningCharges || 0,
        otherDeductions: data.otherDeductions || 0,
        ownerNotes: data.ownerNotes || '',
        damageDescription: data.damageDescription || '',
        approved: true
      });
    } catch (error) {
      setError('Failed to load settlement calculation');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Prevent submit if already completed
    if (isCompleted) return;

    setSubmitting(true);
    setError('');

    try {
      await settlementService.approveSettlement(
        settlementId,
        formData
      );

      showSuccess(
        formData.approved
          ? 'Settlement approved successfully!'
          : 'Settlement rejected'
      );

      onSuccess?.();
      onClose();
    } catch (error) {
      setError(
        error.response?.data?.message ||
          'Failed to process settlement'
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleChange = (e) => {
    const { name, value, type } = e.target;

    setFormData({
      ...formData,
      [name]:
        type === 'number'
          ? parseFloat(value) || 0
          : value
    });
  };

  const calculateFinalAmount = () => {
    if (!calculation) return 0;

    const totalDeductions =
      calculation.outstandingRent +
      calculation.outstandingCharges +
      parseFloat(formData.damageCharges) +
      parseFloat(formData.cleaningCharges) +
      parseFloat(formData.otherDeductions);

    return calculation.securityDeposit - totalDeductions;
  };

  const finalAmount = calculateFinalAmount();

  const settlementType =
    finalAmount >= 0
      ? 'OWNER_PAYABLE'
      : 'TENANT_PAYABLE';

  // READ ONLY FLAG - Check both calculation status and settlement prop status
  const isCompleted =
    calculation?.status === 'COMPLETED' || 
    settlement?.status === 'COMPLETED' ||
    calculation?.settledAt ||
    settlement?.settledAt;

  // Section component for consistent styling
  const Section = ({ title, children, className = "" }) => (
    <div className={`rounded-2xl border border-slate-200 ${className}`}>
      <div className="px-4 py-3 bg-slate-50 rounded-t-2xl border-b border-slate-200">
        <h3 className="text-sm font-semibold text-slate-900">{title}</h3>
      </div>
      <div className="px-4 py-4 space-y-3">
        {children}
      </div>
    </div>
  );

  const InfoRow = ({ label, value, className = "" }) => (
    <div className={`flex justify-between items-start gap-4 ${className}`}>
      <span className="text-sm text-slate-600 font-medium flex-shrink-0">{label}:</span>
      <span className="text-sm text-slate-900 font-semibold text-right break-words">{value || 'Not specified'}</span>
    </div>
  );

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4" onClick={onClose}>
      <div
        className="relative w-full max-w-4xl max-h-[95vh] overflow-y-auto rounded-3xl bg-white shadow-2xl"
        onClick={e => e.stopPropagation()}
      >
        {/* Header */}
        <div className="sticky top-0 z-10 flex items-start justify-between bg-white px-6 py-4 border-b border-slate-100 rounded-t-3xl">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-3 flex-wrap">
              <h2 className="text-xl font-bold text-slate-950">Settlement Calculation</h2>
              {isCompleted && (
                <Badge variant="success">Completed</Badge>
              )}
            </div>
            <p className="text-sm text-slate-600 mt-1">Agreement settlement details and calculation</p>
          </div>
          <div className="flex-shrink-0 ml-4">
            <button 
              onClick={onClose} 
              className="rounded-full p-2 text-slate-400 hover:bg-slate-100 transition"
            >
              ✕
            </button>
          </div>
        </div>

        {/* Error */}
        {error && (
          <div className="px-6 pt-4">
            <Alert type="error" className="mb-4">
              {error}
            </Alert>
          </div>
        )}

        {/* Loader */}
        {loading ? (
          <div className="flex justify-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          </div>
        ) : calculation ? (
          <form onSubmit={handleSubmit}>
            <div className="px-6 py-6 space-y-6">

              {/* Agreement Info */}
              <Section title="📋 Agreement Details">
                <InfoRow label="Tenant" value={calculation.tenantName} />
                <InfoRow label="Room" value={calculation.roomNumber} />
              </Section>

              {/* Financial Breakdown */}
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Left Column - Existing Amounts */}
                <Section title="💰 Existing Financial Summary">
                  <div className="flex justify-between items-start gap-4">
                    <span className="text-sm text-slate-600 font-medium flex-shrink-0">Security Deposit:</span>
                    <span className="text-sm font-semibold text-right text-green-600">
                      +₹{calculation.securityDeposit?.toLocaleString()}
                    </span>
                  </div>
                  <div className="flex justify-between items-start gap-4">
                    <span className="text-sm text-slate-600 font-medium flex-shrink-0">Outstanding Rent:</span>
                    <span className="text-sm font-semibold text-right text-red-600">
                      -₹{calculation.outstandingRent?.toLocaleString()}
                    </span>
                  </div>
                  <div className="flex justify-between items-start gap-4">
                    <span className="text-sm text-slate-600 font-medium flex-shrink-0">Outstanding Charges:</span>
                    <span className="text-sm font-semibold text-right text-red-600">
                      -₹{calculation.outstandingCharges?.toLocaleString()}
                    </span>
                  </div>

                  {/* Outstanding Items Details */}
                  {calculation.outstandingItems?.length > 0 && (
                    <div className="mt-4">
                      <h4 className="text-sm font-semibold text-slate-700 mb-2">Outstanding Items:</h4>
                      <div className="max-h-32 overflow-y-auto space-y-1">
                        {calculation.outstandingItems.map((item, index) => (
                          <div key={index} className="text-sm p-2 bg-red-50 rounded-lg border border-red-200">
                            <div className="flex justify-between">
                              <span className="text-slate-700">{item.description}</span>
                              <Badge variant={item.status === 'OVERDUE' ? 'destructive' : 'secondary'}>
                                {item.status}
                              </Badge>
                            </div>
                            <div className="text-xs text-slate-600 mt-1">
                              ₹{item.amount?.toLocaleString()} - Due: {item.dueDate}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </Section>

                {/* Right Column - Additional Charges */}
                <Section title="⚡ Additional Charges">
                  <InputField
                    label="Damage Charges"
                    name="damageCharges"
                    type="number"
                    min="0"
                    step="0.01"
                    value={formData.damageCharges}
                    onChange={handleChange}
                    placeholder="0.00"
                    disabled={isCompleted}
                  />

                  <InputField
                    label="Cleaning Charges"
                    name="cleaningCharges"
                    type="number"
                    min="0"
                    step="0.01"
                    value={formData.cleaningCharges}
                    onChange={handleChange}
                    placeholder="0.00"
                    disabled={isCompleted}
                  />

                  <InputField
                    label="Other Deductions"
                    name="otherDeductions"
                    type="number"
                    min="0"
                    step="0.01"
                    value={formData.otherDeductions}
                    onChange={handleChange}
                    placeholder="0.00"
                    disabled={isCompleted}
                  />
                </Section>
              </div>

              {/* Final Settlement */}
              <Section title="🎯 Final Settlement" className="border-blue-200">
                <div className="bg-white border border-slate-200 rounded-xl p-4">
                  {settlementType === 'OWNER_PAYABLE' ? (
                    <div className="text-center">
                      <div className="text-green-600">
                        <span className="text-lg font-bold">
                          Owner to pay tenant: ₹{Math.abs(finalAmount).toLocaleString()}
                        </span>
                        <p className="text-sm text-slate-600 mt-1">
                          Security deposit exceeds total deductions
                        </p>
                      </div>
                    </div>
                  ) : (
                    <div className="text-center">
                      <div className="text-red-600">
                        <span className="text-lg font-bold">
                          Tenant to pay owner: ₹{Math.abs(finalAmount).toLocaleString()}
                        </span>
                        <p className="text-sm text-slate-600 mt-1">
                          Total deductions exceed security deposit
                        </p>
                      </div>
                    </div>
                  )}

                  {isCompleted && (calculation?.settledAt || settlement?.settledAt) && (
                    <div className="mt-4 pt-4 border-t border-slate-200">
                      <p className="text-sm text-green-600 font-medium text-center">
                        ✓ Settlement completed on{' '}
                        {new Date(calculation?.settledAt || settlement?.settledAt).toLocaleDateString()}
                      </p>
                      {(calculation?.paymentReference || settlement?.paymentReference) && (
                        <p className="text-xs text-slate-600 mt-1 text-center">
                          Payment Reference: {calculation?.paymentReference || settlement?.paymentReference}
                        </p>
                      )}
                    </div>
                  )}
                </div>
              </Section>

              {/* Notes */}
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <Section title="📝 Owner Notes">
                  <InputField
                    label="Owner Notes"
                    name="ownerNotes"
                    value={formData.ownerNotes}
                    onChange={handleChange}
                    placeholder="Add notes about the settlement..."
                    multiline
                    rows={3}
                    maxLength={500}
                    disabled={isCompleted}
                  />
                </Section>

                {formData.damageCharges > 0 && (
                  <Section title="🔧 Damage Description">
                    <InputField
                      label="Damage Description"
                      name="damageDescription"
                      value={formData.damageDescription}
                      onChange={handleChange}
                      placeholder="Describe the damages..."
                      multiline
                      rows={3}
                      maxLength={1000}
                      disabled={isCompleted}
                    />
                  </Section>
                )}
              </div>

              {/* Tenant Notes */}
              {calculation.tenantNotes && (
                <Section title="💬 Tenant Notes">
                  <p className="text-sm text-slate-700 bg-slate-50 rounded-lg p-3">
                    {calculation.tenantNotes}
                  </p>
                </Section>
              )}
            </div>

          </form>
        ) : null}

        {/* Footer */}
        <div className="sticky bottom-0 bg-white border-t border-slate-100 px-6 py-4 rounded-b-3xl">
          {isCompleted ? (
            <Button 
              label="Close" 
              onClick={onClose} 
              variant="secondary"
              fullWidth
            />
          ) : (
            <div className="flex gap-3">
              <Button
                label="Cancel"
                onClick={onClose}
                variant="secondary"
                disabled={submitting}
                className="flex-1"
              />
              <Button
                label="Reject Settlement"
                onClick={() => setFormData({ ...formData, approved: false })}
                variant="destructive"
                disabled={submitting}
                className="flex-1"
              />
              <Button
                label="Approve Settlement"
                onClick={handleSubmit}
                loading={submitting}
                className="flex-1"
              />
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default SettlementCalculationModal;