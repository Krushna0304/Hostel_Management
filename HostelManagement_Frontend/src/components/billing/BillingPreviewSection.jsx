import { useState, useEffect } from 'react'

export default function BillingPreviewSection({ form, planId = null }) {
  const [billingBreakdown, setBillingBreakdown] = useState(null)
  const [loading, setLoading] = useState(false)

  const calculateBillingPreview = () => {
    if (!form.rentDetails?.monthlyRent) return

    // Calculate preview based on form data
    const baseRent = Number(form.rentDetails.monthlyRent) || 0
    const securityDeposit = Number(form.charges?.securityDeposit?.amount) || 0
    const oneTimeMaintenance = Number(form.charges?.maintenanceCharges?.oneTimeMaintenanceCharge?.amount) || 0
    const monthlyCleaning = Number(form.charges?.cleaningCharges?.monthlyCleaningCharge?.amount) || 0
    const monthlyMaintenance = Number(form.charges?.maintenanceCharges?.monthlyMaintenanceCharge?.amount) || 0
    const electricity = Number(form.charges?.utilityCharges?.electricity?.fixedAmount) || 0
    const water = Number(form.charges?.utilityCharges?.water?.monthlyAmount) || 0
    const deepCleaning = Number(form.charges?.cleaningCharges?.deepCleaningOnExit?.amount) || 0

    // Calculate custom one-time charges
    const customOneTimeCharges = (form.charges?.customCharges?.oneTimeCharges || []).reduce((total, charge) => {
      return total + (Number(charge.amount) || 0)
    }, 0)

    // Calculate custom monthly recurring charges
    const customMonthlyRecurringCharges = (form.charges?.customCharges?.monthlyRecurringCharges || []).reduce((total, charge) => {
      return total + (Number(charge.amount) || 0)
    }, 0)

    // Total one-time charges = maintenance + custom charges
    const totalOneTimeCharges = oneTimeMaintenance + customOneTimeCharges

    const recurringCharges = monthlyCleaning + monthlyMaintenance + electricity + water + customMonthlyRecurringCharges
    const monthlyTotal = baseRent + recurringCharges

    // Calculate installment details
    const totalDuration = Number(form.duration?.value) || 12
    const numberOfInstallments = Number(form.paymentModel?.installments) || 1
    const monthsPerInstallment = Math.ceil(totalDuration / numberOfInstallments)
    const installmentAmount = monthlyTotal * monthsPerInstallment

    const activationTotal = installmentAmount + securityDeposit + totalOneTimeCharges

    const preview = {
      activationAmount: {
        firstInstallment: installmentAmount,
        refundableDeposits: securityDeposit,
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
        estimatedRefund: securityDeposit,
        potentialDeductions: deepCleaning,
        netRefundEstimate: securityDeposit - deepCleaning
      }
    }

    setBillingBreakdown(preview)
  }

  useEffect(() => {
    calculateBillingPreview()
  }, [
    form.rentDetails?.monthlyRent,
    form.charges?.securityDeposit?.amount,
    form.charges?.maintenanceCharges?.oneTimeMaintenanceCharge?.amount,
    form.charges?.cleaningCharges?.monthlyCleaningCharge?.amount,
    form.charges?.maintenanceCharges?.monthlyMaintenanceCharge?.amount,
    form.charges?.utilityCharges?.electricity?.fixedAmount,
    form.charges?.utilityCharges?.water?.monthlyAmount,
    form.charges?.cleaningCharges?.deepCleaningOnExit?.amount,
    form.charges?.customCharges?.oneTimeCharges, // Add custom one-time charges to dependencies
    form.charges?.customCharges?.monthlyRecurringCharges, // Add custom monthly recurring charges to dependencies
    form.duration?.value, // Add duration to dependencies
    form.paymentModel?.installments // Add installments to dependencies
  ])

  const formatCurrency = (amount) => `₹${amount?.toLocaleString() || 0}`

  if (!billingBreakdown) {
    return (
      <div className="rounded-2xl border border-slate-200">
        <div className="px-4 py-3 bg-slate-50 rounded-t-2xl border-b border-slate-200">
          <h3 className="text-sm font-semibold text-slate-900">💳 Billing Preview</h3>
        </div>
        <div className="px-4 py-4">
          <p className="text-sm text-slate-500">Enter monthly rent to see billing preview</p>
        </div>
      </div>
    )
  }

  return (
    <div className="rounded-2xl border border-blue-200">
      <div className="px-4 py-3 bg-blue-50 rounded-t-2xl border-b border-blue-200">
        <h3 className="text-sm font-semibold text-blue-900">💳 Billing Preview</h3>
        <p className="text-xs text-blue-600 mt-1">Real-time calculation based on your plan configuration</p>
      </div>
      <div className="px-4 py-4 space-y-4">
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
            
            {/* Show breakdown of one-time charges if there are custom charges */}
            {form.charges?.customCharges?.oneTimeCharges && form.charges.customCharges.oneTimeCharges.length > 0 && (
              <div className="ml-4 space-y-1 text-xs text-slate-500">
                {Number(form.charges?.maintenanceCharges?.oneTimeMaintenanceCharge?.amount) > 0 && (
                  <div className="flex justify-between">
                    <span>• Maintenance:</span>
                    <span>{formatCurrency(Number(form.charges.maintenanceCharges.oneTimeMaintenanceCharge.amount))}</span>
                  </div>
                )}
                {form.charges.customCharges.oneTimeCharges.map((charge, index) => (
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
            
            {/* Show breakdown of recurring charges if there are custom charges */}
            {form.charges?.customCharges?.monthlyRecurringCharges && form.charges.customCharges.monthlyRecurringCharges.length > 0 && (
              <div className="ml-4 space-y-1 text-xs text-slate-500">
                {Number(form.charges?.cleaningCharges?.monthlyCleaningCharge?.amount) > 0 && (
                  <div className="flex justify-between">
                    <span>• Cleaning:</span>
                    <span>{formatCurrency(Number(form.charges.cleaningCharges.monthlyCleaningCharge.amount))}</span>
                  </div>
                )}
                {Number(form.charges?.maintenanceCharges?.monthlyMaintenanceCharge?.amount) > 0 && (
                  <div className="flex justify-between">
                    <span>• Maintenance:</span>
                    <span>{formatCurrency(Number(form.charges.maintenanceCharges.monthlyMaintenanceCharge.amount))}</span>
                  </div>
                )}
                {Number(form.charges?.utilityCharges?.electricity?.fixedAmount) > 0 && (
                  <div className="flex justify-between">
                    <span>• Electricity:</span>
                    <span>{formatCurrency(Number(form.charges.utilityCharges.electricity.fixedAmount))}</span>
                  </div>
                )}
                {Number(form.charges?.utilityCharges?.water?.monthlyAmount) > 0 && (
                  <div className="flex justify-between">
                    <span>• Water:</span>
                    <span>{formatCurrency(Number(form.charges.utilityCharges.water.monthlyAmount))}</span>
                  </div>
                )}
                {form.charges.customCharges.monthlyRecurringCharges.map((charge, index) => (
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
        {billingBreakdown.installmentDetails && (
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
            <strong>Note:</strong> This is a preview calculation. Actual amounts may vary based on agreement terms and usage.
          </p>
        </div>
      </div>
    </div>
  )
}