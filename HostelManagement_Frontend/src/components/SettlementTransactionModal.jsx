import { useState, useEffect } from 'react';
import { Button } from './ui/Button';
import { InputField } from './ui/InputField';
import { Alert } from './ui/Alert';
import { Badge } from './ui/Badge';
import settlementService from '../services/settlementService';
import { useSuccessPopup } from '../hooks/useSuccessPopup';

/**
 * Settlement Transaction Creation Modal
 * 
 * This component allows owners to create settlement transactions at any time 
 * based on the current plan. It supports both manual transaction creation
 * and displays the calculated settlement details.
 */
const SettlementTransactionModal = ({
  isOpen,
  onClose,
  agreement,
  onSuccess
}) => {
  const [formData, setFormData] = useState({
    calculationDate: new Date().toISOString().split('T')[0],
    notes: ''
  });
  
  const [transactionResult, setTransactionResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState('');
  const [step, setStep] = useState('form'); // 'form' | 'preview' | 'success'

  const { showSuccess } = useSuccessPopup();

  useEffect(() => {
    if (isOpen && agreement) {
      resetForm();
    }
  }, [isOpen, agreement]);

  const resetForm = () => {
    setFormData({
      calculationDate: new Date().toISOString().split('T')[0],
      notes: ''
    });
    setTransactionResult(null);
    setError('');
    setStep('form');
  };

  const handlePreview = async () => {
    if (!agreement?.id) {
      setError('Agreement information is missing');
      return;
    }

    setLoading(true);
    setError('');

    try {
      // Create a preview calculation by calling the transaction endpoint
      const result = await settlementService.createSettlementTransaction({
        agreementId: agreement.id,
        calculationDate: formData.calculationDate,
        notes: formData.notes,
        preview: true // Add preview flag if supported by backend
      });

      setTransactionResult(result);
      setStep('preview');
    } catch (error) {
      console.error('Error creating settlement transaction preview:', error);
      setError(
        error.response?.data?.message || 
        'Failed to calculate settlement transaction'
      );
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async () => {
    if (!agreement?.id) {
      setError('Agreement information is missing');
      return;
    }

    setCreating(true);
    setError('');

    try {
      const result = await settlementService.createSettlementTransaction({
        agreementId: agreement.id,
        calculationDate: formData.calculationDate,
        notes: formData.notes
      });

      setTransactionResult(result);
      setStep('success');
      
      showSuccess('Settlement transaction created successfully!');
      
      // Auto-close after 2 seconds and call onSuccess
      setTimeout(() => {
        onSuccess?.();
        onClose();
      }, 2000);

    } catch (error) {
      console.error('Error creating settlement transaction:', error);
      setError(
        error.response?.data?.message || 
        'Failed to create settlement transaction'
      );
    } finally {
      setCreating(false);
    }
  };

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleBack = () => {
    setStep('form');
    setTransactionResult(null);
    setError('');
  };

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
      <span className="text-sm text-slate-900 font-semibold text-right break-words">
        {value || 'Not specified'}
      </span>
    </div>
  );

  if (!isOpen) return null;

  const renderFormStep = () => (
    <div className="px-6 py-6 space-y-6">
      {/* Agreement Information */}
      <Section title="📋 Agreement Details">
        <InfoRow label="Tenant" value={agreement?.tenantName || 'N/A'} />
        <InfoRow label="Room" value={agreement?.roomNumber || 'N/A'} />
        <InfoRow label="Agreement ID" value={agreement?.id || 'N/A'} />
        <InfoRow label="Status" value={agreement?.status || 'N/A'} />
        {agreement?.deposit && (
          <InfoRow 
            label="Security Deposit" 
            value={`₹${agreement.deposit.toLocaleString()}`} 
          />
        )}
      </Section>

      {/* Transaction Details Form */}
      <Section title="💰 Settlement Transaction Details">
        <InputField
          label="Calculation Date"
          name="calculationDate"
          type="date"
          value={formData.calculationDate}
          onChange={handleChange}
          max={new Date().toISOString().split('T')[0]}
          required
          hint="Date for which the settlement calculation should be performed"
        />

        <InputField
          label="Notes (Optional)"
          name="notes"
          value={formData.notes}
          onChange={handleChange}
          placeholder="Add notes about this settlement transaction..."
          multiline
          rows={3}
          maxLength={500}
          hint="Optional notes for record-keeping and communication"
        />
      </Section>

      {/* Information Box */}
      <div className="p-4 bg-blue-50 rounded-2xl border border-blue-200">
        <h4 className="font-medium text-blue-800 mb-2">📝 What will happen:</h4>
        <ul className="text-sm text-blue-700 space-y-1">
          <li>• Settlement amount will be calculated based on current plan details</li>
          <li>• Outstanding rent and charges will be included automatically</li>
          <li>• Tenant will receive immediate notification about the transaction</li>
          <li>• Transaction details will be saved for audit purposes</li>
        </ul>
      </div>
    </div>
  );

  const renderPreviewStep = () => (
    <div className="px-6 py-6 space-y-6">
      {/* Transaction Summary */}
      <Section title="📊 Settlement Transaction Summary" className="border-blue-200">
        <InfoRow label="Transaction ID" value={transactionResult?.transactionId || 'Will be generated'} />
        <InfoRow label="Agreement ID" value={transactionResult?.agreementId} />
        <InfoRow label="Calculation Date" value={formData.calculationDate} />
        <InfoRow label="Status" value={transactionResult?.status || 'SETTLEMENT_TRANSACTION_CREATED'} />
      </Section>

      {/* Financial Details */}
      <Section title="💳 Financial Breakdown">
        <div className="bg-white border border-slate-200 rounded-xl p-4">
          <div className="text-center mb-4">
            <div className={`text-2xl font-bold ${
              (transactionResult?.settlementType === 'OWNER_PAYABLE' || 
               transactionResult?.settlementAmount > 0) ? 'text-green-600' : 'text-red-600'
            }`}>
              {transactionResult?.settlementType === 'OWNER_PAYABLE' || transactionResult?.settlementAmount > 0 
                ? `₹${Math.abs(transactionResult?.settlementAmount || 0).toLocaleString()}`
                : `₹${Math.abs(transactionResult?.settlementAmount || 0).toLocaleString()}`
              }
            </div>
            <div className="text-sm text-slate-600 mt-1">
              {transactionResult?.settlementType === 'OWNER_PAYABLE' || transactionResult?.settlementAmount > 0
                ? 'Owner to pay tenant'
                : 'Tenant to pay owner'
              }
            </div>
          </div>

          {transactionResult?.calculationBreakdown && (
            <div className="space-y-2 pt-4 border-t border-slate-200">
              <h5 className="text-sm font-semibold text-slate-700 mb-3">Calculation Details:</h5>
              
              {Object.entries(transactionResult.calculationBreakdown).map(([key, value]) => (
                <div key={key} className="flex justify-between text-sm">
                  <span className="text-slate-600 capitalize">
                    {key.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase())}:
                  </span>
                  <span className="font-semibold">₹{value?.toLocaleString() || '0'}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </Section>

      {/* Notes */}
      {formData.notes && (
        <Section title="📝 Notes">
          <p className="text-sm text-slate-700 bg-slate-50 rounded-lg p-3">
            {formData.notes}
          </p>
        </Section>
      )}

      {/* Notification Info */}
      <div className="p-4 bg-green-50 rounded-2xl border border-green-200">
        <h4 className="font-medium text-green-800 mb-2">📧 Notification Status:</h4>
        <p className="text-sm text-green-700">
          {transactionResult?.notificationSent 
            ? '✓ Tenant will be notified immediately upon transaction creation'
            : '⚠ Tenant notification will be sent after transaction creation'
          }
        </p>
      </div>
    </div>
  );

  const renderSuccessStep = () => (
    <div className="px-6 py-8 text-center">
      <div className="mx-auto w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mb-6">
        <span className="text-3xl">✅</span>
      </div>
      
      <h3 className="text-xl font-bold text-slate-950 mb-2">Transaction Created Successfully!</h3>
      <p className="text-slate-600 mb-6">
        Settlement transaction has been created and the tenant has been notified.
      </p>

      {transactionResult && (
        <div className="bg-slate-50 rounded-2xl p-4 mb-6">
          <div className="space-y-2">
            <InfoRow label="Transaction ID" value={transactionResult.transactionId || transactionResult.settlementId} />
            <InfoRow label="Settlement Amount" value={`₹${Math.abs(transactionResult.settlementAmount || 0).toLocaleString()}`} />
            <InfoRow label="Settlement Type" value={transactionResult.settlementType || 'PENDING'} />
            <InfoRow label="Created At" value={new Date(transactionResult.createdAt).toLocaleString()} />
          </div>
        </div>
      )}

      <p className="text-sm text-green-600 font-medium">
        This window will close automatically in a few seconds...
      </p>
    </div>
  );

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
              <h2 className="text-xl font-bold text-slate-950">
                {step === 'form' && 'Create Settlement Transaction'}
                {step === 'preview' && 'Review Settlement Transaction'}
                {step === 'success' && 'Transaction Created'}
              </h2>
              
              {step === 'success' && (
                <Badge variant="success">Completed</Badge>
              )}
            </div>
            <p className="text-sm text-slate-600 mt-1">
              {step === 'form' && 'Create a settlement transaction based on current plan details'}
              {step === 'preview' && 'Review the transaction details before creation'}
              {step === 'success' && 'Settlement transaction has been created successfully'}
            </p>
          </div>
          <div className="flex-shrink-0 ml-4">
            <button 
              onClick={onClose} 
              className="rounded-full p-2 text-slate-400 hover:bg-slate-100 transition"
              disabled={creating}
            >
              ✕
            </button>
          </div>
        </div>

        {/* Error Alert */}
        {error && (
          <div className="px-6 pt-4">
            <Alert type="error" className="mb-4">
              {error}
            </Alert>
          </div>
        )}

        {/* Content based on step */}
        {step === 'form' && renderFormStep()}
        {step === 'preview' && renderPreviewStep()}
        {step === 'success' && renderSuccessStep()}

        {/* Footer */}
        {step !== 'success' && (
          <div className="sticky bottom-0 bg-white border-t border-slate-100 px-6 py-4 rounded-b-3xl">
            <div className="flex gap-3">
              {step === 'preview' && (
                <Button
                  label="Back"
                  onClick={handleBack}
                  variant="secondary"
                  disabled={creating}
                  className="flex-1"
                />
              )}
              
              <Button
                label="Cancel"
                onClick={onClose}
                variant="secondary"
                disabled={loading || creating}
                className="flex-1"
              />
              
              {step === 'form' && (
                <Button
                  label="Preview Transaction"
                  onClick={handlePreview}
                  loading={loading}
                  disabled={!formData.calculationDate}
                  className="flex-1"
                />
              )}
              
              {step === 'preview' && (
                <Button
                  label="Create Transaction"
                  onClick={handleCreate}
                  loading={creating}
                  className="flex-1"
                />
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default SettlementTransactionModal;