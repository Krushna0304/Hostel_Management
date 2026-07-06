import { Badge, Button } from './ui'

export default function PlanDetailsModal({ plan, onClose }) {
  if (!plan) return null

  const formatCurrency = (amount) => {
    return `₹${amount?.toLocaleString()}`
  }

  const formatDuration = (duration) => {
    if (!duration) return 'Not specified'
    return `${duration.value} ${duration.unit}(s)`
  }

  const formatTime = (time) => {
    if (!time) return 'Not specified'
    return new Date(`2000-01-01T${time}`).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  }

  // Calculate comprehensive billing breakdown
  const calculateBillingBreakdown = () => {
    const baseRent = Number(plan.rentDetails?.monthlyRent) || 0
    const securityDeposit = Number(plan.charges?.securityDeposit?.amount) || 0
    const oneTimeMaintenance = Number(plan.charges?.maintenanceCharges?.oneTimeMaintenanceCharge?.amount) || 0
    const monthlyCleaning = Number(plan.charges?.cleaningCharges?.monthlyCleaningCharge?.amount) || 0
    const monthlyMaintenance = Number(plan.charges?.maintenanceCharges?.monthlyMaintenanceCharge?.amount) || 0
    const electricity = Number(plan.charges?.utilityCharges?.electricity?.fixedAmount) || 0
    const water = Number(plan.charges?.utilityCharges?.water?.monthlyAmount) || 0
    const deepCleaning = Number(plan.charges?.cleaningCharges?.deepCleaningOnExit?.amount) || 0

    // Custom one-time charges (fall back to top-level for backward compatibility), split by refundability
    const customOneTime = plan.charges?.customCharges?.oneTimeCharges || plan.oneTimeCharges || []
    const customRefundableOneTime = customOneTime
      .filter(c => c.refundable)
      .reduce((total, charge) => total + (Number(charge.amount) || 0), 0)
    const customNonRefundableOneTime = customOneTime
      .filter(c => !c.refundable)
      .reduce((total, charge) => total + (Number(charge.amount) || 0), 0)

    // Calculate custom monthly recurring charges
    const customMonthlyRecurringCharges = (plan.charges?.customCharges?.monthlyRecurringCharges || plan.monthlyRecurringCharges || []).reduce((total, charge) => {
      return total + (Number(charge.amount) || 0)
    }, 0)

    // Refundable deposits = security deposit + custom refundable one-time charges
    const refundableDeposits = securityDeposit + customRefundableOneTime
    // Total one-time charges = maintenance + custom non-refundable charges
    const totalOneTimeCharges = oneTimeMaintenance + customNonRefundableOneTime
    const recurringCharges = monthlyCleaning + monthlyMaintenance + electricity + water + customMonthlyRecurringCharges
    const monthlyTotal = baseRent + recurringCharges

    // Calculate installment details
    const totalDuration = Number(plan.duration?.value) || 12
    const numberOfInstallments = Number(plan.paymentModel?.installments) || 1
    const monthsPerInstallment = Math.ceil(totalDuration / numberOfInstallments)
    const installmentAmount = monthlyTotal * monthsPerInstallment

    const activationTotal = installmentAmount + refundableDeposits + totalOneTimeCharges

    return {
      activationAmount: {
        firstInstallment: installmentAmount,
        refundableDeposits: refundableDeposits,
        oneTimeCharges: totalOneTimeCharges,
        totalActivationAmount: activationTotal
      },
      monthlyAmount: {
        baseRent: baseRent,
        recurringCharges: recurringCharges,
        totalMonthlyAmount: monthlyTotal
      },
      installmentDetails: {
        installmentAmount: installmentAmount,
        monthsPerInstallment: monthsPerInstallment,
        numberOfInstallments: numberOfInstallments,
        totalDuration: totalDuration
      },
      exitSettlement: {
        estimatedRefund: refundableDeposits,
        potentialDeductions: deepCleaning,
        netRefundEstimate: refundableDeposits - deepCleaning
      }
    }
  }

  const billingBreakdown = calculateBillingBreakdown()

  const Section = ({ title, children, className = "" }) => (
    <div className={`rounded-2xl border border-slate-200 ${className}`}>
      <div className="px-4 py-3 bg-slate-50 rounded-t-2xl border-b border-slate-200">
        <h3 className="text-sm font-semibold text-slate-900">{title}</h3>
      </div>
      <div className="px-4 py-4 space-y-3">
        {children}
      </div>
    </div>
  )

  const InfoRow = ({ label, value, className = "" }) => (
    <div className={`flex justify-between items-start ${className}`}>
      <span className="text-sm text-slate-600 font-medium">{label}:</span>
      <span className="text-sm text-slate-900 font-semibold text-right">{value || 'Not specified'}</span>
    </div>
  )

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4" onClick={onClose}>
      <div
        className="relative w-full max-w-4xl max-h-[95vh] overflow-y-auto rounded-3xl bg-white shadow-2xl"
        onClick={e => e.stopPropagation()}
      >
        {/* Header */}
        <div className="sticky top-0 z-10 flex items-center justify-between bg-white px-6 py-4 border-b border-slate-100 rounded-t-3xl">
          <div>
            <div className="flex items-center gap-3">
              <h2 className="text-xl font-bold text-slate-950">{plan.planName}</h2>
              <Badge variant="success">ACTIVE</Badge>
            </div>
            <p className="text-sm text-slate-600 mt-1">{plan.planType || 'ROOM_AGREEMENT'}</p>
          </div>
          <button 
            onClick={onClose} 
            className="rounded-full p-2 text-slate-400 hover:bg-slate-100 transition"
          >
            ✕
          </button>
        </div>

        <div className="px-6 py-6 space-y-6">
          {/* Comprehensive Billing Breakdown */}
          <Section title="💳 Comprehensive Billing Breakdown" className="border-blue-200">
            <div className="space-y-4">
              {/* Activation Amount */}
              <div className="bg-white border border-slate-200 rounded-xl p-3">
                <h4 className="text-sm font-semibold text-slate-900 mb-2">🚀 Agreement Activation Amount</h4>
                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-slate-600">First Installment:</span>
                    <span className="font-semibold text-slate-900">{formatCurrency(billingBreakdown.activationAmount.firstInstallment)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-600">Refundable Deposits:</span>
                    <span className="font-semibold text-green-700">{formatCurrency(billingBreakdown.activationAmount.refundableDeposits)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-600">One-time Charges:</span>
                    <span className="font-semibold text-slate-900">{formatCurrency(billingBreakdown.activationAmount.oneTimeCharges)}</span>
                  </div>
                  
                  {/* Breakdown of non-refundable one-time charges */}
                  {(Number(plan.charges?.maintenanceCharges?.oneTimeMaintenanceCharge?.amount) > 0 ||
                    (plan.charges?.customCharges?.oneTimeCharges || plan.oneTimeCharges || []).some(c => !c.refundable)) && (
                    <div className="ml-4 space-y-1 text-xs text-slate-500">
                      {Number(plan.charges?.maintenanceCharges?.oneTimeMaintenanceCharge?.amount) > 0 && (
                        <div className="flex justify-between">
                          <span>• Maintenance:</span>
                          <span>{formatCurrency(Number(plan.charges.maintenanceCharges.oneTimeMaintenanceCharge.amount))}</span>
                        </div>
                      )}
                      {(plan.charges?.customCharges?.oneTimeCharges || plan.oneTimeCharges || []).filter(c => !c.refundable).map((charge, index) => (
                        <div key={index} className="flex justify-between">
                          <span>• {charge.chargeName}:</span>
                          <span>{formatCurrency(Number(charge.amount))}</span>
                        </div>
                      ))}
                    </div>
                  )}

                  {/* Breakdown of refundable deposits */}
                  {(plan.charges?.customCharges?.oneTimeCharges || plan.oneTimeCharges || []).some(c => c.refundable) && (
                    <div className="ml-4 space-y-1 text-xs text-slate-500">
                      {Number(plan.charges?.securityDeposit?.amount) > 0 && (
                        <div className="flex justify-between">
                          <span>• Security Deposit:</span>
                          <span>{formatCurrency(Number(plan.charges.securityDeposit.amount))}</span>
                        </div>
                      )}
                      {(plan.charges?.customCharges?.oneTimeCharges || plan.oneTimeCharges || []).filter(c => c.refundable).map((charge, index) => (
                        <div key={index} className="flex justify-between">
                          <span>• {charge.chargeName}:</span>
                          <span>{formatCurrency(Number(charge.amount))}</span>
                        </div>
                      ))}
                    </div>
                  )}

                  <div className="border-t border-slate-200 pt-2 flex justify-between">
                    <span className="font-semibold text-slate-900">Total Activation:</span>
                    <span className="font-bold text-blue-700">{formatCurrency(billingBreakdown.activationAmount.totalActivationAmount)}</span>
                  </div>
                </div>
              </div>

              {/* Monthly Amount */}
              <div className="bg-white border border-slate-200 rounded-xl p-3">
                <h4 className="text-sm font-semibold text-slate-900 mb-2">📅 Monthly Amount Breakdown</h4>
                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-slate-600">Base Rent:</span>
                    <span className="font-semibold text-slate-900">{formatCurrency(billingBreakdown.monthlyAmount.baseRent)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-600">Recurring Charges:</span>
                    <span className="font-semibold text-slate-900">{formatCurrency(billingBreakdown.monthlyAmount.recurringCharges)}</span>
                  </div>
                  
                  {/* Show breakdown of recurring charges */}
                  {(billingBreakdown.monthlyAmount.recurringCharges > 0) && (
                    <div className="ml-4 space-y-1 text-xs text-slate-500">
                      {Number(plan.charges?.cleaningCharges?.monthlyCleaningCharge?.amount) > 0 && (
                        <div className="flex justify-between">
                          <span>• Cleaning:</span>
                          <span>{formatCurrency(Number(plan.charges.cleaningCharges.monthlyCleaningCharge.amount))}</span>
                        </div>
                      )}
                      {Number(plan.charges?.maintenanceCharges?.monthlyMaintenanceCharge?.amount) > 0 && (
                        <div className="flex justify-between">
                          <span>• Maintenance:</span>
                          <span>{formatCurrency(Number(plan.charges.maintenanceCharges.monthlyMaintenanceCharge.amount))}</span>
                        </div>
                      )}
                      {Number(plan.charges?.utilityCharges?.electricity?.fixedAmount) > 0 && (
                        <div className="flex justify-between">
                          <span>• Electricity:</span>
                          <span>{formatCurrency(Number(plan.charges.utilityCharges.electricity.fixedAmount))}</span>
                        </div>
                      )}
                      {Number(plan.charges?.utilityCharges?.water?.monthlyAmount) > 0 && (
                        <div className="flex justify-between">
                          <span>• Water:</span>
                          <span>{formatCurrency(Number(plan.charges.utilityCharges.water.monthlyAmount))}</span>
                        </div>
                      )}
                      {(plan.charges?.customCharges?.monthlyRecurringCharges || plan.monthlyRecurringCharges || []).map((charge, index) => (
                        <div key={index} className="flex justify-between">
                          <span>• {charge.chargeName}:</span>
                          <span>{formatCurrency(Number(charge.amount))}</span>
                        </div>
                      ))}
                    </div>
                  )}
                  
                  <div className="border-t border-slate-200 pt-2 flex justify-between">
                    <span className="font-semibold text-slate-900">Total Monthly:</span>
                    <span className="font-bold text-green-700">{formatCurrency(billingBreakdown.monthlyAmount.totalMonthlyAmount)}</span>
                  </div>
                </div>
              </div>

              {/* Installment Amount */}
              {billingBreakdown.installmentDetails && billingBreakdown.installmentDetails.numberOfInstallments > 1 && (
                <div className="bg-white border border-orange-200 rounded-xl p-3">
                  <h4 className="text-sm font-semibold text-orange-900 mb-2">💰 Installment Payment Amount</h4>
                  <div className="space-y-2 text-sm">
                    <div className="flex justify-between">
                      <span className="text-orange-600">Monthly Amount:</span>
                      <span className="font-semibold text-orange-900">{formatCurrency(billingBreakdown.monthlyAmount.totalMonthlyAmount)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-orange-600">Months per Installment:</span>
                      <span className="font-semibold text-orange-900">{billingBreakdown.installmentDetails.monthsPerInstallment} months</span>
                    </div>
                    <div className="border-t border-orange-200 pt-2 flex justify-between">
                      <span className="font-semibold text-orange-900">Installment Amount:</span>
                      <span className="font-bold text-orange-700 text-lg">{formatCurrency(billingBreakdown.installmentDetails.installmentAmount)}</span>
                    </div>
                    <div className="bg-orange-50 rounded-lg px-2 py-1 text-xs text-orange-700">
                      <strong>Payment Schedule:</strong> {billingBreakdown.installmentDetails.numberOfInstallments} installments of {formatCurrency(billingBreakdown.installmentDetails.installmentAmount)} each, covering {billingBreakdown.installmentDetails.totalDuration} months total
                    </div>
                  </div>
                </div>
              )}

              {/* Exit Settlement */}
              <div className="bg-white border border-slate-200 rounded-xl p-3">
                <h4 className="text-sm font-semibold text-slate-900 mb-2">🏁 Exit Settlement Estimate</h4>
                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-slate-600">Refundable Amount:</span>
                    <span className="font-semibold text-green-700">{formatCurrency(billingBreakdown.exitSettlement.estimatedRefund)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-600">Potential Deductions:</span>
                    <span className="font-semibold text-red-600">{formatCurrency(billingBreakdown.exitSettlement.potentialDeductions)}</span>
                  </div>
                  <div className="border-t border-slate-200 pt-2 flex justify-between">
                    <span className="font-semibold text-slate-900">Net Refund Estimate:</span>
                    <span className="font-bold text-blue-700">{formatCurrency(billingBreakdown.exitSettlement.netRefundEstimate)}</span>
                  </div>
                </div>
              </div>

              <div className="bg-amber-50 border border-amber-200 rounded-xl px-3 py-2">
                <p className="text-xs text-amber-700">
                  <strong>Note:</strong> This breakdown shows the actual billing structure. Amounts may vary based on agreement terms and usage patterns.
                </p>
              </div>
            </div>
          </Section>

          {/* Duration & Payment Model */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <Section title="📅 Duration">
              <InfoRow label="Duration" value={formatDuration(plan.duration)} />
              <InfoRow label="Minimum Stay" value={plan.duration?.minimumStayMonths ? `${plan.duration.minimumStayMonths} months` : 'Not specified'} />
            </Section>

            <Section title="🔄 Payment Model">
              <InfoRow label="Payment Mode" value={plan.paymentModel?.mode} />
              <InfoRow label="Payment Timing" value={plan.paymentModel?.paymentTiming} />
              <InfoRow label="Installments" value={plan.paymentModel?.installments} />
              <InfoRow label="Due Day of Month" value={plan.paymentModel?.dueDayOfMonth} />
            </Section>
          </div>

          {/* Facilities */}
          {plan.freeFacilities?.facilities?.length > 0 && (
            <Section title="✨ Free Facilities">
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                {plan.freeFacilities.facilities.map((facility, index) => (
                  <div key={index} className="rounded-xl bg-green-50 border border-green-200 px-3 py-2">
                    <div className="font-medium text-green-900 text-sm">{facility.name}</div>
                    {facility.description && (
                      <div className="text-xs text-green-700 mt-1">{facility.description}</div>
                    )}
                    {facility.availability && (
                      <div className="text-xs text-green-600 mt-1">Available: {facility.availability}</div>
                    )}
                  </div>
                ))}
              </div>
            </Section>
          )}

          {/* Late Payment Policy */}
          {plan.latePaymentPolicy && (
            <Section title="⚠️ Late Payment Policy">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <InfoRow label="Grace Period" value={plan.latePaymentPolicy.gracePeriodDays ? `${plan.latePaymentPolicy.gracePeriodDays} days` : 'Not specified'} />
                <InfoRow label="Penalty Type" value={plan.latePaymentPolicy.penalty?.type} />
                <InfoRow label="Penalty Amount" value={plan.latePaymentPolicy.penalty?.amount ? formatCurrency(plan.latePaymentPolicy.penalty.amount) : 'Not specified'} />
                <InfoRow label="Maximum Penalty" value={plan.latePaymentPolicy.penalty?.maxAmount ? formatCurrency(plan.latePaymentPolicy.penalty.maxAmount) : 'Not specified'} />
              </div>
            </Section>
          )}

          {/* House Rules */}
          {plan.rulesAndRegulations && (
            <Section title="📋 House Rules & Regulations">
              <div className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <InfoRow label="Smoking Allowed" value={plan.rulesAndRegulations.houseRules?.smokingAllowed ? 'Yes' : 'No'} />
                  <InfoRow label="Pets Allowed" value={plan.rulesAndRegulations.houseRules?.petsAllowed ? 'Yes' : 'No'} />
                  <InfoRow 
                    label="Quiet Hours" 
                    value={plan.rulesAndRegulations.houseRules?.quietHours ? 
                      `${formatTime(plan.rulesAndRegulations.houseRules.quietHours.from)} - ${formatTime(plan.rulesAndRegulations.houseRules.quietHours.to)}` : 
                      'Not specified'
                    } 
                  />
                </div>
                
                {plan.rulesAndRegulations.facilityUsageRules?.length > 0 && (
                  <div>
                    <h4 className="text-sm font-semibold text-slate-700 mb-2">Facility Usage Rules:</h4>
                    <ul className="space-y-1">
                      {plan.rulesAndRegulations.facilityUsageRules.map((rule, index) => (
                        <li key={index} className="text-sm text-slate-600 bg-slate-50 rounded-lg px-3 py-2">
                          • {rule}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            </Section>
          )}

          {/* Legal Information */}
          {plan.legal && (
            <Section title="⚖️ Legal Information">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <InfoRow label="Agreement Lock" value={plan.legal.agreementLock ? 'Yes' : 'No'} />
                <InfoRow label="Modification After Sign" value={plan.legal.modificationAllowedAfterSign ? 'Allowed' : 'Not Allowed'} />
                <InfoRow label="Jurisdiction" value={plan.legal.jurisdiction} />
              </div>
            </Section>
          )}

          {/* Cancellation Policy */}
          {plan.agreementCancellationRules?.tenantCancellation && (
            <Section title="🚪 Cancellation Policy">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <InfoRow 
                  label="Tenant Cancellation Allowed" 
                  value={plan.agreementCancellationRules.tenantCancellation.allowed ? 'Yes' : 'No'} 
                />
                {plan.agreementCancellationRules.tenantCancellation.allowed && (
                  <>
                    <InfoRow 
                      label="Notice Period" 
                      value={plan.agreementCancellationRules.tenantCancellation.noticePeriodDays ? `${plan.agreementCancellationRules.tenantCancellation.noticePeriodDays} days` : 'Not specified'} 
                    />
                    {plan.agreementCancellationRules.tenantCancellation.earlyExitPenalty?.type ? (
                      <InfoRow 
                        label="Early Exit Penalty" 
                        value={
                          plan.agreementCancellationRules.tenantCancellation.earlyExitPenalty.type === 'MONTH_RENT' 
                            ? `${plan.agreementCancellationRules.tenantCancellation.earlyExitPenalty.value} Month(s) Rent` 
                            : formatCurrency(plan.agreementCancellationRules.tenantCancellation.earlyExitPenalty.value)
                        } 
                      />
                    ) : (
                      <InfoRow label="Early Exit Penalty" value="No Penalty" />
                    )}
                  </>
                )}
              </div>
            </Section>
          )}

          {/* Custom Fields */}
          {plan.customFields && Object.keys(plan.customFields).length > 0 && (
            <Section title="🔧 Custom Fields" className="border-amber-200">
              <div className="rounded-xl bg-amber-50 border border-amber-200 px-4 py-3">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  {Object.entries(plan.customFields).map(([key, value]) => (
                    <InfoRow 
                      key={key}
                      label={key}
                      value={String(value)}
                      className="text-amber-900"
                    />
                  ))}
                </div>
              </div>
            </Section>
          )}

          {/* Audit Information */}
          {plan.audit && (
            <Section title="📊 Plan Information">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <InfoRow 
                  label="Created At" 
                  value={plan.audit.createdAt ? new Date(plan.audit.createdAt).toLocaleString() : 'Not available'} 
                />
                <InfoRow 
                  label="Last Updated" 
                  value={plan.audit.updatedAt ? new Date(plan.audit.updatedAt).toLocaleString() : 'Not available'} 
                />
              </div>
            </Section>
          )}
        </div>

        {/* Footer */}
        <div className="sticky bottom-0 bg-white border-t border-slate-100 px-6 py-4 rounded-b-3xl">
          <Button 
            label="Close" 
            onClick={onClose} 
            variant="secondary"
            fullWidth
          />
        </div>
      </div>
    </div>
  )
}