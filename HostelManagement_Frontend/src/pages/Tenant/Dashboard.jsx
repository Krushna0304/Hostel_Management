import { useEffect, useState } from 'react'
import { tenantService } from '../../services/agreementService'
import {
  Alert,
  Badge,
  Button,
  Card,
  CardContent,
  CardHeader,
  EmptyState,
  PageHeader,
  Skeleton,
  StatCard,
} from '../../components/ui'
import PaymentModal from '../../components/PaymentModal'
import PaymentHistoryModal from '../../components/PaymentHistoryModal'
import SettlementRequestModal from '../../components/SettlementRequestModal'

const statusVariant = (status) => {
  switch (status) {
    case 'COMPLETED': return 'success'
    case 'OVERDUE': return 'danger'
    case 'PARTIALLY_PAID': return 'warning'
    case 'SCHEDULED': return 'neutral'
    default: return 'neutral'
  }
}

function AgreementModal({ agreement, onClose }) {
  if (!agreement) return null
  const plan = agreement.planSnapshot
  const formatDate = (d) => d ? new Date(d).toLocaleDateString() : 'N/A'

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4" onClick={onClose}>
      <div
        className="relative w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-3xl bg-white p-6 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-start justify-between mb-5">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Your agreement</p>
            <h2 className="mt-1 text-2xl font-bold text-slate-950">{agreement.type} Agreement</h2>
            <p className="mt-1 text-xs text-slate-400 break-all">ID: {agreement.id}</p>
          </div>
          <div className="flex items-center gap-3">
            <Badge variant={agreement.status === 'ACTIVE' ? 'success' : 'warning'}>
              {agreement.status?.replaceAll('_', ' ')}
            </Badge>
            <button onClick={onClose} className="rounded-full p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition">✕</button>
          </div>
        </div>

        <div className="space-y-4">
          {/* Basic dates */}
          <div className="grid grid-cols-2 gap-3">
            <div className="rounded-2xl bg-slate-50 px-4 py-3">
              <p className="text-xs uppercase tracking-[0.15em] text-slate-400">Start date</p>
              <p className="mt-1 font-semibold text-slate-950">{formatDate(agreement.startDate)}</p>
            </div>
            <div className="rounded-2xl bg-slate-50 px-4 py-3">
              <p className="text-xs uppercase tracking-[0.15em] text-slate-400">Activated</p>
              <p className="mt-1 font-semibold text-slate-950">{formatDate(agreement.activatedAt)}</p>
            </div>
          </div>

          {plan ? (
            <>
              {/* Plan name */}
              <div className="rounded-2xl bg-blue-50 border border-blue-200 px-4 py-3">
                <p className="text-xs uppercase tracking-[0.15em] text-blue-500">Plan</p>
                <p className="mt-1 text-lg font-bold text-blue-900">{plan.planName}</p>
              </div>

              {/* Comprehensive Billing Breakdown */}
              <div className="rounded-2xl bg-blue-50 border border-blue-200 p-4">
                <p className="text-sm font-semibold text-blue-900 mb-3">💳 Comprehensive Billing Breakdown</p>
                
                {(() => {
                  // Calculate billing breakdown
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

                  const customMonthlyRecurringCharges = (plan.charges?.customCharges?.monthlyRecurringCharges || plan.monthlyRecurringCharges || []).reduce((total, charge) => {
                    return total + (Number(charge.amount) || 0)
                  }, 0)

                  // Total calculations
                  const refundableDeposits = securityDeposit + customRefundableOneTime
                  const totalOneTimeCharges = oneTimeMaintenance + customNonRefundableOneTime
                  const recurringCharges = monthlyCleaning + monthlyMaintenance + electricity + water + customMonthlyRecurringCharges
                  const monthlyTotal = baseRent + recurringCharges

                  // Calculate installment details
                  const totalDuration = Number(plan.duration?.value) || 12
                  const numberOfInstallments = Number(plan.paymentModel?.installments) || 1
                  const monthsPerInstallment = Math.ceil(totalDuration / numberOfInstallments)
                  const installmentAmount = monthlyTotal * monthsPerInstallment
                  const activationTotal = installmentAmount + refundableDeposits + totalOneTimeCharges

                  return (
                    <div className="space-y-3">
                      {/* Agreement Activation Amount */}
                      <div className="bg-white border border-blue-200 rounded-xl p-3">
                        <h4 className="text-sm font-semibold text-blue-900 mb-2">🚀 Agreement Activation Amount</h4>
                        <div className="space-y-2 text-sm">
                          <div className="flex justify-between">
                            <span className="text-blue-700">First Installment:</span>
                            <span className="font-semibold text-blue-900">₹{installmentAmount.toLocaleString()}</span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-blue-700">Refundable Deposits:</span>
                            <span className="font-semibold text-green-700">₹{refundableDeposits.toLocaleString()}</span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-blue-700">One-time Charges:</span>
                            <span className="font-semibold text-blue-900">₹{totalOneTimeCharges.toLocaleString()}</span>
                          </div>

                          {/* Breakdown of non-refundable one-time charges */}
                          {(oneTimeMaintenance > 0 || customOneTime.some(c => !c.refundable)) && (
                            <div className="ml-4 space-y-1 text-xs text-blue-600">
                              {oneTimeMaintenance > 0 && (
                                <div className="flex justify-between">
                                  <span>• Maintenance:</span>
                                  <span>₹{oneTimeMaintenance.toLocaleString()}</span>
                                </div>
                              )}
                              {customOneTime.filter(c => !c.refundable).map((charge, index) => (
                                <div key={index} className="flex justify-between">
                                  <span>• {charge.chargeName}:</span>
                                  <span>₹{Number(charge.amount).toLocaleString()}</span>
                                </div>
                              ))}
                            </div>
                          )}

                          {/* Breakdown of refundable deposits */}
                          {customOneTime.some(c => c.refundable) && (
                            <div className="ml-4 space-y-1 text-xs text-blue-600">
                              {securityDeposit > 0 && (
                                <div className="flex justify-between">
                                  <span>• Security Deposit:</span>
                                  <span>₹{securityDeposit.toLocaleString()}</span>
                                </div>
                              )}
                              {customOneTime.filter(c => c.refundable).map((charge, index) => (
                                <div key={index} className="flex justify-between">
                                  <span>• {charge.chargeName}:</span>
                                  <span>₹{Number(charge.amount).toLocaleString()}</span>
                                </div>
                              ))}
                            </div>
                          )}

                          <div className="border-t border-blue-200 pt-2 flex justify-between">
                            <span className="font-semibold text-blue-900">Total Activation:</span>
                            <span className="font-bold text-blue-700">₹{activationTotal.toLocaleString()}</span>
                          </div>
                          
                          {/* Payment Status Indicator */}
                          <div className="bg-green-50 border border-green-200 rounded-lg px-3 py-2 mt-2">
                            <div className="flex items-center justify-between">
                              <span className="text-xs font-medium text-green-800">✓ Activation Payment Status:</span>
                              <span className="text-xs font-bold text-green-700">PAID</span>
                            </div>
                            <p className="text-xs text-green-600 mt-1">
                              Includes first installment payment of ₹{installmentAmount.toLocaleString()}
                            </p>
                          </div>
                        </div>
                      </div>

                      {/* Monthly Amount Breakdown */}
                      <div className="bg-white border border-green-200 rounded-xl p-3">
                        <h4 className="text-sm font-semibold text-green-900 mb-2">📅 Monthly Amount Breakdown</h4>
                        <div className="space-y-2 text-sm">
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
                            <div className="ml-4 space-y-1 text-xs text-green-600">
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
                              {(plan.charges?.customCharges?.monthlyRecurringCharges || plan.monthlyRecurringCharges || []).map((charge, index) => (
                                <div key={index} className="flex justify-between">
                                  <span>• {charge.chargeName}:</span>
                                  <span>₹{Number(charge.amount).toLocaleString()}</span>
                                </div>
                              ))}
                            </div>
                          )}
                          
                          <div className="border-t border-green-200 pt-2 flex justify-between">
                            <span className="font-semibold text-green-900">Total Monthly:</span>
                            <span className="font-bold text-green-700">₹{monthlyTotal.toLocaleString()}</span>
                          </div>
                        </div>
                      </div>

                      {/* Installment Payment Details (if multiple installments) */}
                      {numberOfInstallments > 1 && (
                        <div className="bg-white border border-orange-200 rounded-xl p-3">
                          <h4 className="text-sm font-semibold text-orange-900 mb-2">💰 Installment Payment Details</h4>
                          <div className="space-y-2 text-sm">
                            <div className="flex justify-between">
                              <span className="text-orange-700">Monthly Amount:</span>
                              <span className="font-semibold text-orange-900">₹{monthlyTotal.toLocaleString()}</span>
                            </div>
                            <div className="flex justify-between">
                              <span className="text-orange-700">Months per Installment:</span>
                              <span className="font-semibold text-orange-900">{monthsPerInstallment} months</span>
                            </div>
                            <div className="border-t border-orange-200 pt-2 flex justify-between">
                              <span className="font-semibold text-orange-900">Installment Amount:</span>
                              <span className="font-bold text-orange-700 text-lg">₹{installmentAmount.toLocaleString()}</span>
                            </div>
                            <div className="bg-orange-50 rounded-lg px-2 py-1 text-xs text-orange-700">
                              <strong>Payment Schedule:</strong> {numberOfInstallments} installments covering {totalDuration} months total
                            </div>
                            
                            {/* Installment Status Tracking */}
                            <div className="bg-slate-50 border border-slate-200 rounded-lg p-2 mt-2">
                              <h5 className="text-xs font-semibold text-slate-900 mb-2">📊 Installment Status</h5>
                              <div className="space-y-1">
                                <div className="flex items-center justify-between text-xs">
                                  <span className="text-slate-600">Installment #1:</span>
                                  <div className="flex items-center gap-2">
                                    <span className="font-semibold text-slate-900">₹{installmentAmount.toLocaleString()}</span>
                                    <span className="px-2 py-1 bg-green-100 text-green-800 rounded-full text-xs font-medium">✓ PAID</span>
                                  </div>
                                </div>
                                {Array.from({ length: numberOfInstallments - 1 }, (_, i) => (
                                  <div key={i + 2} className="flex items-center justify-between text-xs">
                                    <span className="text-slate-600">Installment #{i + 2}:</span>
                                    <div className="flex items-center gap-2">
                                      <span className="font-semibold text-slate-900">₹{installmentAmount.toLocaleString()}</span>
                                      <span className="px-2 py-1 bg-slate-100 text-slate-600 rounded-full text-xs font-medium">PENDING</span>
                                    </div>
                                  </div>
                                ))}
                              </div>
                              <div className="mt-2 pt-2 border-t border-slate-200">
                                <p className="text-xs text-slate-600">
                                  <strong>Note:</strong> First installment was paid during agreement activation
                                </p>
                              </div>
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
                            <span className="font-semibold text-green-700">₹{refundableDeposits.toLocaleString()}</span>
                          </div>
                          <div className="flex justify-between">
                            <span className="text-slate-600">Potential Deductions:</span>
                            <span className="font-semibold text-red-600">₹{deepCleaning.toLocaleString()}</span>
                          </div>
                          <div className="border-t border-slate-200 pt-2 flex justify-between">
                            <span className="font-semibold text-slate-900">Net Refund Estimate:</span>
                            <span className="font-bold text-blue-700">₹{(refundableDeposits - deepCleaning).toLocaleString()}</span>
                          </div>
                        </div>
                      </div>

                      <div className="bg-amber-50 border border-amber-200 rounded-xl px-3 py-2">
                        <p className="text-xs text-amber-700">
                          <strong>Note:</strong> This breakdown shows your actual billing structure. The first installment was included in your agreement activation payment.
                        </p>
                      </div>
                    </div>
                  )
                })()}
              </div>

              {/* Payment Model Configuration */}
              <div className="rounded-2xl bg-slate-50 p-4">
                <p className="text-sm font-semibold text-slate-900 mb-3">🔄 Payment Model Configuration</p>
                <div className="grid grid-cols-2 gap-3 text-sm">
                  {plan.paymentModel && (
                    <>
                      <div>
                        <p className="text-xs text-slate-500">Payment Mode</p>
                        <p className="font-semibold text-slate-950">{plan.paymentModel.mode}</p>
                      </div>
                      <div>
                        <p className="text-xs text-slate-500">Payment Timing</p>
                        <p className="font-semibold text-slate-950">{plan.paymentModel.paymentTiming}</p>
                      </div>
                      <div>
                        <p className="text-xs text-slate-500">Installments</p>
                        <p className="font-semibold text-slate-950">{plan.paymentModel.installments}</p>
                      </div>
                      <div>
                        <p className="text-xs text-slate-500">Due Day</p>
                        <p className="font-semibold text-slate-950">Day {plan.paymentModel.dueDayOfMonth} of month</p>
                      </div>
                    </>
                  )}
                </div>
              </div>

              {/* Duration */}
              {plan.duration && (
                <div className="rounded-2xl bg-slate-50 p-4">
                  <p className="text-sm font-semibold text-slate-900 mb-2">📅 Duration</p>
                  <p className="text-sm text-slate-700">
                    {plan.duration.value} {plan.duration.unit}(s)
                    {plan.duration.minimumStayMonths && ` · Minimum stay: ${plan.duration.minimumStayMonths} months`}
                  </p>
                </div>
              )}

              {/* Facilities */}
              {plan.freeFacilities?.facilities?.length > 0 && (
                <div className="rounded-2xl bg-slate-50 p-4">
                  <p className="text-sm font-semibold text-slate-900 mb-3">✨ Free Facilities</p>
                  <div className="flex flex-wrap gap-2">
                    {plan.freeFacilities.facilities.map((f, i) => (
                      <span key={i} className="px-3 py-1 bg-green-100 text-green-800 rounded-full text-xs font-medium" title={f.description}>
                        {f.name}{f.availability && ` · ${f.availability}`}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {/* Late Payment Policy */}
              {plan.latePaymentPolicy && (
                <div className="rounded-2xl bg-yellow-50 border border-yellow-200 p-4">
                  <p className="text-sm font-semibold text-yellow-900 mb-2">⚠️ Late Payment Policy</p>
                  <div className="text-sm text-yellow-800 space-y-1">
                    <p>Grace Period: {plan.latePaymentPolicy.gracePeriodDays} days</p>
                    {plan.latePaymentPolicy.penalty && (
                      <p>Penalty: {plan.latePaymentPolicy.penalty.type} · ₹{plan.latePaymentPolicy.penalty.amount}
                        {plan.latePaymentPolicy.penalty.maxAmount && ` (Max: ₹${plan.latePaymentPolicy.penalty.maxAmount})`}
                      </p>
                    )}
                  </div>
                </div>
              )}

              {/* House Rules */}
              {plan.rulesAndRegulations?.houseRules && (
                <div className="rounded-2xl bg-slate-50 p-4">
                  <p className="text-sm font-semibold text-slate-900 mb-3">📋 House Rules</p>
                  <div className="text-sm text-slate-700 space-y-2">
                    <div className="flex justify-between"><span>Smoking</span><span className="font-semibold">{plan.rulesAndRegulations.houseRules.smokingAllowed ? 'Allowed' : 'Not Allowed'}</span></div>
                    <div className="flex justify-between"><span>Pets</span><span className="font-semibold">{plan.rulesAndRegulations.houseRules.petsAllowed ? 'Allowed' : 'Not Allowed'}</span></div>
                    {plan.rulesAndRegulations.houseRules.quietHours && (
                      <div className="flex justify-between">
                        <span>Quiet Hours</span>
                        <span className="font-semibold">{plan.rulesAndRegulations.houseRules.quietHours.from} – {plan.rulesAndRegulations.houseRules.quietHours.to}</span>
                      </div>
                    )}
                    {plan.rulesAndRegulations.facilityUsageRules?.length > 0 && (
                      <div className="mt-2">
                        <p className="font-semibold mb-1">Facility Usage Rules:</p>
                        <ul className="list-disc list-inside space-y-1 text-xs text-slate-600">
                          {plan.rulesAndRegulations.facilityUsageRules.map((rule, i) => <li key={i}>{rule}</li>)}
                        </ul>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* Cancellation */}
              {plan.agreementCancellationRules?.tenantCancellation && (
                <div className="rounded-2xl bg-slate-50 p-4">
                  <p className="text-sm font-semibold text-slate-900 mb-2">🚪 Cancellation Policy</p>
                  <div className="text-sm text-slate-700 space-y-1">
                    <div className="flex justify-between"><span>Tenant Cancellation</span><span className="font-semibold">{plan.agreementCancellationRules.tenantCancellation.allowed ? 'Allowed' : 'Not Allowed'}</span></div>
                    {plan.agreementCancellationRules.tenantCancellation.noticePeriodDays && (
                      <div className="flex justify-between"><span>Notice Period</span><span className="font-semibold">{plan.agreementCancellationRules.tenantCancellation.noticePeriodDays} days</span></div>
                    )}
                    {plan.agreementCancellationRules.tenantCancellation.earlyExitPenalty && (
                      <div className="flex justify-between">
                        <span>Early Exit Penalty</span>
                        <span className="font-semibold">{plan.agreementCancellationRules.tenantCancellation.earlyExitPenalty.type} · {plan.agreementCancellationRules.tenantCancellation.earlyExitPenalty.value}</span>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* Legal */}
              {plan.legal && (
                <div className="rounded-2xl bg-slate-50 p-4">
                  <p className="text-sm font-semibold text-slate-900 mb-2">⚖️ Legal</p>
                  <div className="text-sm text-slate-700 space-y-1">
                    <div className="flex justify-between"><span>Agreement Lock</span><span className="font-semibold">{plan.legal.agreementLock ? 'Yes' : 'No'}</span></div>
                    <div className="flex justify-between"><span>Modification After Sign</span><span className="font-semibold">{plan.legal.modificationAllowedAfterSign ? 'Allowed' : 'Not Allowed'}</span></div>
                    {plan.legal.jurisdiction && <div className="flex justify-between"><span>Jurisdiction</span><span className="font-semibold">{plan.legal.jurisdiction}</span></div>}
                  </div>
                </div>
              )}

              {/* Custom Fields */}
              {plan.customFields && Object.keys(plan.customFields).length > 0 && (
                <div className="rounded-2xl bg-amber-50 border border-amber-200 p-4">
                  <p className="text-sm font-semibold text-amber-900 mb-3">🔧 Additional Plan Details</p>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
                    {Object.entries(plan.customFields).map(([key, value]) => (
                      <div key={key} className="flex justify-between">
                        <span className="text-amber-700 font-medium">{key}:</span>
                        <span className="text-amber-900 font-semibold">{String(value)}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </>
          ) : (
            /* Fallback legacy */
            <div className="grid grid-cols-2 gap-3">
              <div className="rounded-2xl bg-slate-50 px-4 py-3">
                <p className="text-xs uppercase tracking-[0.15em] text-slate-400">Rent</p>
                <p className="mt-1 font-semibold text-slate-950">₹{agreement.rent}</p>
              </div>
              <div className="rounded-2xl bg-slate-50 px-4 py-3">
                <p className="text-xs uppercase tracking-[0.15em] text-slate-400">Deposit</p>
                <p className="mt-1 font-semibold text-slate-950">₹{agreement.deposit || 0}</p>
              </div>
            </div>
          )}
        </div>

        <div className="mt-6">
          <Button label="Close" variant="secondary" fullWidth onClick={onClose} />
        </div>
      </div>
    </div>
  )
}

export default function TenantDashboard() {
  const [dashboard, setDashboard] = useState(null)
  const [schedule, setSchedule] = useState(null)
  const [agreement, setAgreement] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showAgreement, setShowAgreement] = useState(false)
  const [showPaymentModal, setShowPaymentModal] = useState(false)
  const [selectedPayment, setSelectedPayment] = useState(null)
  const [showHistory, setShowHistory] = useState(false)
  const [showSettlementModal, setShowSettlementModal] = useState(false)
  const [confirmingLeft, setConfirmingLeft] = useState(false)
  const [showConfirmLeft, setShowConfirmLeft] = useState(false)
  const [markingArrival, setMarkingArrival] = useState(false)
  const [showConfirmArrival, setShowConfirmArrival] = useState(false)

  useEffect(() => {
    fetchData()
  }, [])

  const fetchData = async () => {
    try {
      setLoading(true)
      setError('')
      const [dashRes, schedRes, agreementRes] = await Promise.all([
        tenantService.getDashboard(),
        tenantService.getPaymentSchedule(),
        tenantService.getMyAgreement(),
      ])
      setDashboard(dashRes.data)
      setSchedule(schedRes.data)
      setAgreement(agreementRes.data)
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to load dashboard.')
    } finally {
      setLoading(false)
    }
  }

  const handlePayClick = (scheduleId, amount, installmentNumber) => {
    setSelectedPayment({ scheduleId, amount, installmentNumber })
    setShowPaymentModal(true)
  }

  const handlePaymentSuccess = async () => {
    setShowPaymentModal(false)
    setSelectedPayment(null)
    await fetchData()
  }

  const handleMarkArrival = async () => {
    try {
      setMarkingArrival(true)
      await tenantService.markArrival(dashboard.allotmentId)
      setShowConfirmArrival(false)
      await fetchData()
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to mark arrival. Please try again.')
      setShowConfirmArrival(false)
    } finally {
      setMarkingArrival(false)
    }
  }

  const handleConfirmLeft = async () => {
    try {
      setConfirmingLeft(true)
      await tenantService.confirmLeft(dashboard.allotmentId)
      setShowConfirmLeft(false)
      await fetchData()
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to confirm vacating. Please try again.')
      setShowConfirmLeft(false)
    } finally {
      setConfirmingLeft(false)
    }
  }

  const formatDate = (d) => d ? new Date(d).toLocaleDateString() : '—'
  const formatAmount = (a) => `₹${(a ?? 0).toLocaleString()}`

  if (loading) {
    return (
      <div className="space-y-8">
        <Skeleton className="h-24 rounded-3xl" />
        <div className="grid gap-4 lg:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-40 rounded-3xl" />)}
        </div>
        <Skeleton className="h-96 rounded-3xl" />
      </div>
    )
  }

  if (error) {
    return (
      <Alert tone="error" title="Could not load dashboard">
        <div className="flex items-center justify-between gap-4">
          <span>{error}</span>
          <Button label="Retry" variant="secondary" onClick={fetchData} />
        </div>
      </Alert>
    )
  }

  const computedInstallmentAmount = (() => {
    const plan = agreement?.planSnapshot
    if (!plan) return dashboard?.installmentAmount ?? 0
    const baseRent = Number(plan.rentDetails?.monthlyRent) || 0
    const monthlyCleaning = Number(plan.charges?.cleaningCharges?.monthlyCleaningCharge?.amount) || 0
    const monthlyMaintenance = Number(plan.charges?.maintenanceCharges?.monthlyMaintenanceCharge?.amount) || 0
    const electricity = Number(plan.charges?.utilityCharges?.electricity?.fixedAmount) || 0
    const water = Number(plan.charges?.utilityCharges?.water?.monthlyAmount) || 0
    const customMonthly = (plan.charges?.customCharges?.monthlyRecurringCharges || plan.monthlyRecurringCharges || [])
      .reduce((sum, c) => sum + (Number(c.amount) || 0), 0)
    const monthlyTotal = baseRent + monthlyCleaning + monthlyMaintenance + electricity + water + customMonthly
    const totalDuration = Number(plan.duration?.value) || 12
    const numberOfInstallments = Number(plan.paymentModel?.installments) || 1
    const monthsPerInstallment = Math.ceil(totalDuration / numberOfInstallments)
    return monthlyTotal * monthsPerInstallment
  })()

  const stats = [
    {
      label: 'Installment amount',
      value: formatAmount(computedInstallmentAmount),
      meta: `Per installment · ${dashboard?.paymentFrequency ?? ''} schedule`,
      icon: null,
    },
    {
      label: 'Total paid',
      value: formatAmount(dashboard?.totalPaid),
      meta: 'Across all installments',
      icon: null,
    },
    {
      label: 'Pending amount',
      value: formatAmount(dashboard?.totalPending),
      meta: `${dashboard?.overdueCount ?? 0} overdue installment(s)`,
      icon: null,
    },
  ]

  return (
    <div className="space-y-8">
      {/* Agreement Modal */}
      {showAgreement && (
        <AgreementModal agreement={agreement} onClose={() => setShowAgreement(false)} />
      )}

      {/* Payment Modal */}
      {showPaymentModal && selectedPayment && (
        <PaymentModal
          scheduleId={selectedPayment.scheduleId}
          amount={selectedPayment.amount}
          installmentNumber={selectedPayment.installmentNumber}
          onSuccess={handlePaymentSuccess}
          onClose={() => {
            setShowPaymentModal(false)
            setSelectedPayment(null)
          }}
        />
      )}

      {/* Payment History Modal */}
      {showHistory && schedule && (
        <PaymentHistoryModal
          ledger={schedule}
          onClose={() => setShowHistory(false)}
        />
      )}

      {/* Confirm Arrival Dialog */}
      {showConfirmArrival && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4">
          <div className="w-full max-w-sm rounded-3xl bg-white p-6 shadow-2xl">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-500">Confirm arrival</p>
            <h3 className="mt-2 text-xl font-bold text-slate-950">Mark yourself as arrived?</h3>
            <p className="mt-2 text-sm text-slate-500">
              This confirms you have physically moved into your room and your agreement is now active.
            </p>
            <div className="mt-5 flex gap-3">
              <Button
                label="Cancel"
                variant="secondary"
                fullWidth
                onClick={() => setShowConfirmArrival(false)}
              />
              <Button
                label="Yes, I have arrived"
                fullWidth
                loading={markingArrival}
                onClick={handleMarkArrival}
              />
            </div>
          </div>
        </div>
      )}

      {/* Confirm Left Dialog */}
      {showConfirmLeft && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4">
          <div className="w-full max-w-sm rounded-3xl bg-white p-6 shadow-2xl">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-amber-500">Confirm vacating</p>
            <h3 className="mt-2 text-xl font-bold text-slate-950">Mark yourself as vacated?</h3>
            <p className="mt-2 text-sm text-slate-500">
              This confirms you have physically left the room. The owner will also need to confirm before the status moves to LEFT.
            </p>
            <div className="mt-5 flex gap-3">
              <Button
                label="Cancel"
                variant="secondary"
                fullWidth
                onClick={() => setShowConfirmLeft(false)}
              />
              <Button
                label="Yes, I have vacated"
                fullWidth
                loading={confirmingLeft}
                onClick={handleConfirmLeft}
              />
            </div>
          </div>
        </div>
      )}

      {/* Settlement Request Modal */}
      {showSettlementModal && agreement && (
        <SettlementRequestModal
          isOpen={showSettlementModal}
          onClose={() => setShowSettlementModal(false)}
          agreement={agreement}
          onSuccess={fetchData}
        />
      )}

      <PageHeader
        eyebrow="Tenant workspace"
        title="Your payment dashboard"
        description="Track your rent schedule, upcoming dues, and payment history in one place."
        action={
          <div className="flex gap-3">
            {schedule && (
              <Button
                label="Payment History"
                variant="secondary"
                onClick={() => setShowHistory(true)}
              />
            )}
            {agreement && agreement.status === 'ACTIVE' && (
              <Button
                label="Request Settlement"
                variant="outline"
                onClick={() => setShowSettlementModal(true)}
              />
            )}
            {dashboard?.allotmentStatus === 'UPCOMING' &&
              new Date().toLocaleDateString('en-CA') >= (dashboard.startDate?.slice(0, 10) ?? '') && (
              <Button
                label="Mark as Arrived"
                variant="success"
                onClick={() => setShowConfirmArrival(true)}
              />
            )}
            {dashboard?.allotmentStatus === 'ON_NOTICE_PERIOD' && (
              <Button
                label="Mark as Vacated"
                variant="warning"
                onClick={() => setShowConfirmLeft(true)}
              />
            )}
            {agreement ? (
              <Button label="View Agreement" variant="secondary" onClick={() => setShowAgreement(true)} />
            ) : null}
          </div>
        }
      />

      {/* Upcoming / arrival banner */}
      {dashboard?.allotmentStatus === 'UPCOMING' && (
        <div className="rounded-[1.75rem] border border-sky-200 bg-sky-50 p-5">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-600">Room allotted — not yet active</p>
          <p className="mt-1 text-sm text-sky-800">
            Your agreement starts on <strong>{new Date(dashboard.startDate).toLocaleDateString()}</strong>.
            {new Date().toLocaleDateString('en-CA') >= (dashboard.startDate?.slice(0, 10) ?? '')
              ? <> Once you physically move in, click <strong>"Mark as Arrived"</strong> above to activate your agreement.</>
              : <> The <strong>"Mark as Arrived"</strong> option will appear on or after your start date.</>}
          </p>
        </div>
      )}

      {/* On notice period banner */}
      {dashboard?.allotmentStatus === 'ON_NOTICE_PERIOD' && (
        <div className="rounded-[1.75rem] border border-amber-200 bg-amber-50 p-5">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-amber-600">Notice Period Active</p>
          <p className="mt-1 text-sm text-amber-800">
            Your settlement has been approved and payment is complete. Once you physically vacate, click <strong>"Mark as Vacated"</strong> above. The owner will also confirm, after which your allotment will be marked as LEFT.
          </p>
        </div>
      )}

      {/* Room info banner */}
      {dashboard && (
        <div className="rounded-[1.75rem] border border-slate-200 bg-slate-950 p-6 text-white">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-300">Your room</p>
          <h2 className="mt-2 text-2xl font-semibold">
            {dashboard.hostelName} · Floor {dashboard.floorNumber} · Room {dashboard.roomNumber}
          </h2>
          <p className="mt-1 text-sm text-slate-400">{dashboard.hostelAddress}</p>
          <div className="mt-4 flex flex-wrap gap-6 text-sm text-slate-300">
            <span>Allotted: {formatDate(dashboard.allotmentDate)}</span>
            <span>Agreement start: {formatDate(dashboard.startDate)}</span>
            <span>Agreement end: {formatDate(dashboard.endDate)}</span>
          </div>
        </div>
      )}

      {/* Stats */}
      <section className="grid gap-4 lg:grid-cols-3">
        {stats.map((s) => <StatCard key={s.label} {...s} />)}
      </section>

      {/* Next due */}
      {dashboard?.nextDueInstallment && (
        <Card>
          <CardHeader title="Next due installment" description="Your upcoming payment." />
          <CardContent>
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
              <div className="space-y-1">
                <p className="text-sm text-slate-500">
                  Installment #{dashboard.nextDueInstallment.installmentNumber} · Due {formatDate(dashboard.nextDueInstallment.dueDate)}
                </p>
                <p className="text-2xl font-semibold text-slate-950">
                  {formatAmount(dashboard.nextDueInstallment.amount + dashboard.nextDueInstallment.lateFeeApplied)}
                </p>
                {dashboard.nextDueInstallment.lateFeeApplied > 0 && (
                  <p className="text-sm text-red-600">
                    Includes late fee: {formatAmount(dashboard.nextDueInstallment.lateFeeApplied)}
                  </p>
                )}
              </div>
              <div className="flex items-center gap-3">
                <Badge variant={statusVariant(dashboard.nextDueInstallment.paymentStatus)}>
                  {dashboard.nextDueInstallment.paymentStatus}
                </Badge>
                <Button
                  label="Pay now"
                  onClick={() => handlePayClick(
                    dashboard.nextDueInstallment.scheduleId,
                    dashboard.nextDueInstallment.amount + dashboard.nextDueInstallment.lateFeeApplied - dashboard.nextDueInstallment.paidAmount,
                    dashboard.nextDueInstallment.installmentNumber
                  )}
                />
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Full schedule */}
      {schedule && (
        <Card>
          <CardHeader
            title="Payment schedule"
            description={`${schedule.installments?.length ?? 0} installments · ${schedule.paymentFrequency}`}
          />
          <CardContent>
            {schedule.installments?.length === 0 ? (
              <EmptyState title="No schedule yet" description="Your payment schedule will appear here once generated." />
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-slate-100 text-left text-xs font-semibold uppercase tracking-[0.15em] text-slate-400">
                      <th className="pb-3 pr-4">#</th>
                      <th className="pb-3 pr-4">Due date</th>
                      <th className="pb-3 pr-4">Amount</th>
                      <th className="pb-3 pr-4">Late fee</th>
                      <th className="pb-3 pr-4">Paid</th>
                      <th className="pb-3">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {schedule.installments.map((inst) => {
                      return (
                        <tr key={inst.scheduleId} className="py-3">
                          <td className="py-3 pr-4 font-medium text-slate-700">{inst.installmentNumber}</td>
                          <td className="py-3 pr-4 text-slate-600">{formatDate(inst.dueDate)}</td>
                          <td className="py-3 pr-4 font-semibold text-slate-950">{formatAmount(inst.amount)}</td>
                          <td className="py-3 pr-4 text-red-600">
                            {inst.lateFeeApplied > 0 ? formatAmount(inst.lateFeeApplied) : '—'}
                          </td>
                          <td className="py-3 pr-4 text-emerald-700">{formatAmount(inst.paidAmount)}</td>
                          <td className="py-3">
                            <Badge variant={statusVariant(inst.paymentStatus)}>
                              {inst.paymentStatus?.replace('_', ' ')}
                            </Badge>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  )
}
