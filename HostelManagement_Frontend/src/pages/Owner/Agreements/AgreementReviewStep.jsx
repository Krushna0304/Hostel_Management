import { useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import agreementService from "../../../services/agreementService";
import Button from "../../../components/Button";
import { Alert } from "../../../components/ui";

export default function AgreementReviewStep({ prevStep, formData }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(null);
  const [alert, setAlert] = useState(null); // { tone, message }

  const handleConfirm = async () => {
    setLoading(true);
    setError("");
    setAlert(null);
    try {
      // Prepare agreement data - using plan-based approach
      const agreementPayload = {
        userId: formData.userId,
        roomId: formData.roomId,
        planId: formData.planId,
        startDate: formData.startDate,
      };

      // For FLAT agreements, include coTenantNames and use the flat endpoint
      if (formData.agreementType === 'FLAT') {
        agreementPayload.coTenantNames = formData.coTenantNames || [];
      }

      const res = formData.agreementType === 'FLAT'
        ? await agreementService.createFlatAgreement(agreementPayload)
        : await agreementService.createRoomAgreement(agreementPayload);
      
      // Show success popup first
      setAlert({ tone: 'success', message: '✅ Agreement Created Successfully! The agreement has been generated and QR code is ready for tenant activation.' });
      
      // After 3 seconds, hide popup and show success details
      setTimeout(() => {
        setAlert(null);
        setSuccess(res.data);
      }, 3000);
    } catch (err) {
      const errorData = err?.response?.data
      
      // Check if error is field-specific validation errors (object with field names as keys)
      if (errorData && typeof errorData === 'object' && !errorData.message) {
        // It's field-specific errors - format them for display
        const errorMessages = Object.values(errorData).join(', ')
        setError(errorMessages || "Failed to create agreement.")
      } else {
        // It's a general error message
        setError(errorData?.message || "Failed to create agreement.")
      }
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return "N/A";
    return new Date(dateString).toLocaleDateString();
  };

  if (success) {
    return (
      <div className="space-y-4">
        <div className="bg-green-50 border border-green-200 rounded-lg p-6">
          <h3 className="text-xl font-bold text-green-800 mb-4">✓ Agreement Created Successfully!</h3>
          
          <div className="space-y-2 mb-4">
            <p><span className="font-semibold">Agreement ID:</span> {success.agreementId}</p>
            <p><span className="font-semibold">QR Token:</span> {success.qrToken}</p>
            <p><span className="font-semibold">Expires:</span> {formatDate(success.expiry)}</p>
          </div>

          <div className="bg-white p-4 rounded border-2 border-dashed border-gray-300 text-center">
            <p className="text-sm text-gray-600 mb-2">Share this QR code with the tenant:</p>
            <div className="bg-white p-4 inline-block rounded flex justify-center">
              <QRCodeSVG
                value={`${window.location.origin}/tenant/activate?token=${success.qrToken}`}
                size={200}
                level="H"
                includeMargin={true}
              />
            </div>
            <p className="text-xs text-gray-500 mt-2 break-all">
              Activation URL: {window.location.origin}/tenant/activate?token={success.qrToken}
            </p>
          </div>
        </div>
        
        <Button
          label="Create Another Agreement"
          onClick={() => window.location.reload()}
          fullWidth
          variant="primary"
        />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {alert ? (
        <div className="fixed top-4 right-4 z-[9999] max-w-md">
          <Alert 
            tone={alert.tone} 
            onClose={() => setAlert(null)}
            className="shadow-lg border-2"
          >
            {alert.message}
          </Alert>
        </div>
      ) : null}

      <h2 className="text-xl font-semibold mb-4">Review Agreement</h2>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 p-3 rounded">
          {error}
        </div>
      )}

      <div className="bg-gray-50 rounded-lg p-4 space-y-4 max-h-[500px] overflow-y-auto">
        {/* Basic Info */}
        <div className="grid grid-cols-2 gap-2 text-sm border-b pb-3">
          <div><span className="font-semibold">User:</span> {formData.userDisplayName || formData.userId}</div>
          <div><span className="font-semibold">Room Number:</span> {formData.roomNumber || formData.roomId}</div>
          <div className="col-span-2"><span className="font-semibold">Hostel Number:</span> {formData.hostelNumber || formData.hostelId}</div>
          <div className="col-span-2"><span className="font-semibold">Start Date:</span> {formatDate(formData.startDate)}</div>
        </div>

        {/* Co-tenants section — shown only for FLAT agreements */}
        {formData.agreementType === 'FLAT' && (
          <div className="text-sm border-b pb-3">
            <p className="font-semibold mb-1">Co-tenants:</p>
            {(formData.coTenantNames && formData.coTenantNames.length > 0) ? (
              <ul className="list-disc list-inside space-y-0.5 text-gray-700">
                {formData.coTenantNames.map((name, index) => (
                  <li key={index}>{name}</li>
                ))}
              </ul>
            ) : (
              <span className="text-gray-500">None</span>
            )}
          </div>
        )}

        {/* Complete Plan Details */}
        {formData.plan && (
          <div className="space-y-3">
            <h3 className="font-bold text-base text-blue-900">{formData.plan.planName}</h3>
            
            {/* Comprehensive Billing Breakdown */}
            <div className="bg-white border border-blue-200 rounded-xl p-4">
              <h4 className="font-semibold text-sm text-blue-900 mb-3">💳 Comprehensive Billing Breakdown</h4>
              
              {/* Calculate billing breakdown */}
              {(() => {
                const baseRent = Number(formData.plan.rentDetails?.monthlyRent) || 0
                const securityDeposit = Number(formData.plan.charges?.securityDeposit?.amount) || 0
                const oneTimeMaintenance = Number(formData.plan.charges?.maintenanceCharges?.oneTimeMaintenanceCharge?.amount) || 0
                const monthlyCleaning = Number(formData.plan.charges?.cleaningCharges?.monthlyCleaningCharge?.amount) || 0
                const monthlyMaintenance = Number(formData.plan.charges?.maintenanceCharges?.monthlyMaintenanceCharge?.amount) || 0
                const electricity = Number(formData.plan.charges?.utilityCharges?.electricity?.fixedAmount) || 0
                const water = Number(formData.plan.charges?.utilityCharges?.water?.monthlyAmount) || 0
                const deepCleaning = Number(formData.plan.charges?.cleaningCharges?.deepCleaningOnExit?.amount) || 0

                // Calculate custom charges
                const customOneTimeCharges = (formData.plan.charges?.customCharges?.oneTimeCharges || []).reduce((total, charge) => {
                  return total + (Number(charge.amount) || 0)
                }, 0)

                const customMonthlyRecurringCharges = (formData.plan.charges?.customCharges?.monthlyRecurringCharges || []).reduce((total, charge) => {
                  return total + (Number(charge.amount) || 0)
                }, 0)

                // Total calculations
                const totalOneTimeCharges = oneTimeMaintenance + customOneTimeCharges
                const recurringCharges = monthlyCleaning + monthlyMaintenance + electricity + water + customMonthlyRecurringCharges
                const monthlyTotal = baseRent + recurringCharges

                // Calculate installment details
                const totalDuration = Number(formData.plan.duration?.value) || 12
                const numberOfInstallments = Number(formData.plan.paymentModel?.installments) || 1
                const monthsPerInstallment = Math.ceil(totalDuration / numberOfInstallments)
                const installmentAmount = monthlyTotal * monthsPerInstallment
                const activationTotal = installmentAmount + securityDeposit + totalOneTimeCharges

                return (
                  <div className="space-y-3">
                    {/* Agreement Activation Amount */}
                    <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
                      <h5 className="text-xs font-semibold text-blue-900 mb-2">🚀 Agreement Activation Amount</h5>
                      <div className="space-y-1 text-xs">
                        <div className="flex justify-between">
                          <span className="text-blue-700">First Installment:</span>
                          <span className="font-semibold text-blue-900">₹{installmentAmount.toLocaleString()}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-blue-700">Refundable Deposits:</span>
                          <span className="font-semibold text-green-700">₹{securityDeposit.toLocaleString()}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-blue-700">One-time Charges:</span>
                          <span className="font-semibold text-blue-900">₹{totalOneTimeCharges.toLocaleString()}</span>
                        </div>
                        
                        {/* Show breakdown of one-time charges */}
                        {(totalOneTimeCharges > 0) && (
                          <div className="ml-3 space-y-1 text-xs text-blue-600">
                            {oneTimeMaintenance > 0 && (
                              <div className="flex justify-between">
                                <span>• Maintenance:</span>
                                <span>₹{oneTimeMaintenance.toLocaleString()}</span>
                              </div>
                            )}
                            {formData.plan.charges?.customCharges?.oneTimeCharges && formData.plan.charges.customCharges.oneTimeCharges.map((charge, index) => (
                              <div key={index} className="flex justify-between">
                                <span>• {charge.chargeName}:</span>
                                <span>₹{Number(charge.amount).toLocaleString()}</span>
                              </div>
                            ))}
                          </div>
                        )}
                        
                        <div className="border-t border-blue-300 pt-1 flex justify-between">
                          <span className="font-semibold text-blue-900">Total Activation:</span>
                          <span className="font-bold text-blue-800">₹{activationTotal.toLocaleString()}</span>
                        </div>
                      </div>
                    </div>

                    {/* Monthly Amount Breakdown */}
                    <div className="bg-green-50 border border-green-200 rounded-lg p-3">
                      <h5 className="text-xs font-semibold text-green-900 mb-2">📅 Monthly Amount Breakdown</h5>
                      <div className="space-y-1 text-xs">
                        <div className="flex justify-between">
                          <span className="text-green-700">Base Rent:</span>
                          <span className="font-semibold text-green-900">₹{baseRent.toLocaleString()}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-green-700">Recurring Charges:</span>
                          <span className="font-semibold text-green-900">₹{recurringCharges.toLocaleString()}</span>
                        </div>
                        
                        {/* Show breakdown of recurring charges */}
                        {(recurringCharges > 0) && (
                          <div className="ml-3 space-y-1 text-xs text-green-600">
                            {monthlyCleaning > 0 && (
                              <div className="flex justify-between">
                                <span>• Cleaning:</span>
                                <span>₹{monthlyCleaning.toLocaleString()}</span>
                              </div>
                            )}
                            {monthlyMaintenance > 0 && (
                              <div className="flex justify-between">
                                <span>• Maintenance:</span>
                                <span>₹{monthlyMaintenance.toLocaleString()}</span>
                              </div>
                            )}
                            {electricity > 0 && (
                              <div className="flex justify-between">
                                <span>• Electricity:</span>
                                <span>₹{electricity.toLocaleString()}</span>
                              </div>
                            )}
                            {water > 0 && (
                              <div className="flex justify-between">
                                <span>• Water:</span>
                                <span>₹{water.toLocaleString()}</span>
                              </div>
                            )}
                            {formData.plan.charges?.customCharges?.monthlyRecurringCharges && formData.plan.charges.customCharges.monthlyRecurringCharges.map((charge, index) => (
                              <div key={index} className="flex justify-between">
                                <span>• {charge.chargeName}:</span>
                                <span>₹{Number(charge.amount).toLocaleString()}</span>
                              </div>
                            ))}
                          </div>
                        )}
                        
                        <div className="border-t border-green-300 pt-1 flex justify-between">
                          <span className="font-semibold text-green-900">Total Monthly:</span>
                          <span className="font-bold text-green-800">₹{monthlyTotal.toLocaleString()}</span>
                        </div>
                      </div>
                    </div>

                    {/* Installment Details (if multiple installments) */}
                    {numberOfInstallments > 1 && (
                      <div className="bg-orange-50 border border-orange-200 rounded-lg p-3">
                        <h5 className="text-xs font-semibold text-orange-900 mb-2">💰 Installment Payment Details</h5>
                        <div className="space-y-1 text-xs">
                          <div className="flex justify-between">
                            <span className="text-orange-700">Monthly Amount:</span>
                            <span className="font-semibold text-orange-900">₹{monthlyTotal.toLocaleString()}</span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-orange-700">Months per Installment:</span>
                            <span className="font-semibold text-orange-900">{monthsPerInstallment} months</span>
                          </div>
                          <div className="border-t border-orange-300 pt-1 flex justify-between">
                            <span className="font-semibold text-orange-900">Installment Amount:</span>
                            <span className="font-bold text-orange-800">₹{installmentAmount.toLocaleString()}</span>
                          </div>
                          <div className="bg-orange-100 rounded px-2 py-1 text-xs text-orange-800">
                            <strong>Schedule:</strong> {numberOfInstallments} installments covering {totalDuration} months
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Exit Settlement */}
                    <div className="bg-slate-50 border border-slate-200 rounded-lg p-3">
                      <h5 className="text-xs font-semibold text-slate-900 mb-2">🏁 Exit Settlement Estimate</h5>
                      <div className="space-y-1 text-xs">
                        <div className="flex justify-between">
                          <span className="text-slate-700">Refundable Amount:</span>
                          <span className="font-semibold text-green-700">₹{securityDeposit.toLocaleString()}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-slate-700">Potential Deductions:</span>
                          <span className="font-semibold text-red-600">₹{deepCleaning.toLocaleString()}</span>
                        </div>
                        <div className="border-t border-slate-300 pt-1 flex justify-between">
                          <span className="font-semibold text-slate-900">Net Refund Estimate:</span>
                          <span className="font-bold text-blue-700">₹{(securityDeposit - deepCleaning).toLocaleString()}</span>
                        </div>
                      </div>
                    </div>

                    <div className="bg-amber-50 border border-amber-200 rounded px-2 py-1">
                      <p className="text-xs text-amber-700">
                        <strong>Note:</strong> Amounts shown are based on plan configuration and may vary based on actual usage.
                      </p>
                    </div>
                  </div>
                )
              })()}
            </div>

            {/* Payment Model Configuration */}
            <div className="bg-white p-3 rounded border">
              <p className="font-semibold text-sm mb-2">🔄 Payment Model Configuration</p>
              <div className="text-xs space-y-1">
                {formData.plan.paymentModel && (
                  <>
                    <div>Payment Mode: {formData.plan.paymentModel.mode}</div>
                    <div>Payment Timing: {formData.plan.paymentModel.paymentTiming}</div>
                    <div>Number of Installments: {formData.plan.paymentModel.installments}</div>
                    <div>Due Day of Month: {formData.plan.paymentModel.dueDayOfMonth}</div>
                  </>
                )}
              </div>
            </div>

            {/* Duration */}
            {formData.plan.duration && (
              <div className="bg-white p-3 rounded border">
                <p className="font-semibold text-sm mb-2">📅 Duration</p>
                <div className="text-xs">
                  {formData.plan.duration.value} {formData.plan.duration.unit}(s)
                  {formData.plan.duration.minimumStayMonths && ` (Minimum ${formData.plan.duration.minimumStayMonths} months)`}
                </div>
              </div>
            )}

            {/* Facilities */}
            {formData.plan.freeFacilities?.facilities && formData.plan.freeFacilities.facilities.length > 0 && (
              <div className="bg-white p-3 rounded border">
                <p className="font-semibold text-sm mb-2">✨ Free Facilities</p>
                <div className="flex flex-wrap gap-1">
                  {formData.plan.freeFacilities.facilities.map((f, i) => (
                    <span key={i} className="text-xs px-2 py-1 bg-green-100 text-green-800 rounded" title={f.description}>
                      {f.name}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* Late Payment Policy */}
            {formData.plan.latePaymentPolicy && (
              <div className="bg-white p-3 rounded border">
                <p className="font-semibold text-sm mb-2">⚠️ Late Payment Policy</p>
                <div className="text-xs space-y-1">
                  <div>Grace Period: {formData.plan.latePaymentPolicy.gracePeriodDays} days</div>
                  {formData.plan.latePaymentPolicy.penalty && (
                    <div>Penalty: {formData.plan.latePaymentPolicy.penalty.type} - ₹{formData.plan.latePaymentPolicy.penalty.amount}
                    {formData.plan.latePaymentPolicy.penalty.maxAmount && ` (Max: ₹${formData.plan.latePaymentPolicy.penalty.maxAmount})`}</div>
                  )}
                </div>
              </div>
            )}

            {/* Rules & Regulations */}
            {formData.plan.rulesAndRegulations?.houseRules && (
              <div className="bg-white p-3 rounded border">
                <p className="font-semibold text-sm mb-2">📋 House Rules</p>
                <div className="text-xs space-y-1">
                  <div>Smoking: {formData.plan.rulesAndRegulations.houseRules.smokingAllowed ? "Allowed" : "Not Allowed"}</div>
                  <div>Pets: {formData.plan.rulesAndRegulations.houseRules.petsAllowed ? "Allowed" : "Not Allowed"}</div>
                  {formData.plan.rulesAndRegulations.houseRules.quietHours && (
                    <div>Quiet Hours: {formData.plan.rulesAndRegulations.houseRules.quietHours.from} - {formData.plan.rulesAndRegulations.houseRules.quietHours.to}</div>
                  )}
                  {formData.plan.rulesAndRegulations.facilityUsageRules && formData.plan.rulesAndRegulations.facilityUsageRules.length > 0 && (
                    <div className="mt-2">
                      <p className="font-medium">Facility Usage:</p>
                      <ul className="list-disc list-inside">
                        {formData.plan.rulesAndRegulations.facilityUsageRules.map((rule, i) => (
                          <li key={i}>{rule}</li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Cancellation Rules */}
            {formData.plan.agreementCancellationRules?.tenantCancellation && (
              <div className="bg-white p-3 rounded border">
                <p className="font-semibold text-sm mb-2">🚪 Cancellation Policy</p>
                <div className="text-xs space-y-1">
                  <div>Tenant Cancellation: {formData.plan.agreementCancellationRules.tenantCancellation.allowed ? "Allowed" : "Not Allowed"}</div>
                  {formData.plan.agreementCancellationRules.tenantCancellation.noticePeriodDays && (
                    <div>Notice Period: {formData.plan.agreementCancellationRules.tenantCancellation.noticePeriodDays} days</div>
                  )}
                  {formData.plan.agreementCancellationRules.tenantCancellation.earlyExitPenalty && (
                    <div>Early Exit Penalty: {formData.plan.agreementCancellationRules.tenantCancellation.earlyExitPenalty.type} - {formData.plan.agreementCancellationRules.tenantCancellation.earlyExitPenalty.value}</div>
                  )}
                </div>
              </div>
            )}

            {/* Legal */}
            {formData.plan.legal && (
              <div className="bg-white p-3 rounded border">
                <p className="font-semibold text-sm mb-2">⚖️ Legal</p>
                <div className="text-xs space-y-1">
                  <div>Agreement Lock: {formData.plan.legal.agreementLock ? "Yes" : "No"}</div>
                  <div>Modification After Sign: {formData.plan.legal.modificationAllowedAfterSign ? "Allowed" : "Not Allowed"}</div>
                  {formData.plan.legal.jurisdiction && <div>Jurisdiction: {formData.plan.legal.jurisdiction}</div>}
                </div>
              </div>
            )}

            {/* Custom Fields */}
            {formData.plan.customFields && Object.keys(formData.plan.customFields).length > 0 && (
              <div className="bg-amber-50 border border-amber-200 p-3 rounded">
                <p className="font-semibold text-sm mb-2 text-amber-900">🔧 Additional Plan Details</p>
                <div className="text-xs space-y-1">
                  {Object.entries(formData.plan.customFields).map(([key, value]) => (
                    <div key={key} className="text-amber-800">
                      <span className="font-medium">{key}:</span> {String(value)}
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      <div className="flex gap-2">
        <Button
          label="Confirm & Create"
          onClick={handleConfirm}
          disabled={loading}
          fullWidth
          variant="success"
        />
        <Button
          label="Edit"
          onClick={prevStep}
          disabled={loading}
          fullWidth
          variant="secondary"
        />
      </div>
    </div>
  );
}
