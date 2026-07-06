import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import agreementService from '../../../services/agreementService'
import { Alert, Badge, Button, Card, CardContent, CardHeader, EmptyState, Skeleton } from '../../../components/ui'
import { ClipboardIcon } from '../../../components/icons/AppIcons'

function AgreementDetailModal({ agreement, onClose }) {
  if (!agreement) return null

  const formatDate = (d) => {
    if (!d) return 'N/A'
    return new Date(d).toLocaleDateString()
  }

  const plan = agreement.planSnapshot

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4" onClick={onClose}>
      <div
        className="relative w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-3xl bg-white p-6 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-start justify-between mb-5">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Agreement details</p>
            <h2 className="mt-1 text-2xl font-bold text-slate-950">{agreement.type} Agreement</h2>
            {plan && (
              <div className="mt-2 space-y-1">
                <p className="text-sm font-medium text-blue-600">{plan.planName}</p>
                {plan.duration && (
                  <p className="text-sm text-slate-600">
                    Duration: {plan.duration.value} {plan.duration.unit.toLowerCase()}{plan.duration.value > 1 ? 's' : ''}
                  </p>
                )}
              </div>
            )}
          </div>
          <div className="flex items-center gap-3">
            <Badge variant={agreement.status === 'ACTIVE' ? 'success' : agreement.status === 'PENDING_TENANT_ACTION' ? 'warning' : 'danger'}>
              {agreement.status?.replaceAll('_', ' ')}
            </Badge>
            <button
              onClick={onClose}
              className="rounded-full p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition"
            >
              ✕
            </button>
          </div>
        </div>

        <div className="space-y-4">
          {/* Tenant and Location Information */}
          <div className="rounded-2xl bg-blue-50 border border-blue-200 p-4">
            <p className="text-sm font-semibold text-blue-900 mb-3">👤 Tenant & Location Details</p>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div className="rounded-xl bg-white border border-blue-200 px-3 py-2">
                <p className="text-xs uppercase tracking-[0.15em] text-blue-500">Tenant Name</p>
                <p className="mt-1 font-semibold text-blue-900">{agreement.tenantName || 'Not available'}</p>
              </div>
              <div className="rounded-xl bg-white border border-blue-200 px-3 py-2">
                <p className="text-xs uppercase tracking-[0.15em] text-blue-500">Mobile Number</p>
                <p className="mt-1 font-semibold text-blue-900">{agreement.tenantMobileNumber || 'Not available'}</p>
              </div>
              <div className="rounded-xl bg-white border border-blue-200 px-3 py-2">
                <p className="text-xs uppercase tracking-[0.15em] text-blue-500">Hostel</p>
                <p className="mt-1 font-semibold text-blue-900">{agreement.hostelName || 'Not available'}</p>
              </div>
              <div className="rounded-xl bg-white border border-blue-200 px-3 py-2">
                <p className="text-xs uppercase tracking-[0.15em] text-blue-500">Room & Floor</p>
                <p className="mt-1 font-semibold text-blue-900">
                  {agreement.roomNumber && agreement.floorNumber 
                    ? `Room ${agreement.roomNumber}, Floor ${agreement.floorNumber}`
                    : 'Not available'
                  }
                </p>
              </div>
            </div>
          </div>
          {/* Basic Info */}
          <div className="grid grid-cols-2 gap-3">
            <div className="rounded-2xl bg-slate-50 px-4 py-3">
              <p className="text-xs uppercase tracking-[0.15em] text-slate-400">Start date</p>
              <p className="mt-1 font-semibold text-slate-950">{formatDate(agreement.startDate)}</p>
            </div>
            <div className="rounded-2xl bg-slate-50 px-4 py-3">
              <p className="text-xs uppercase tracking-[0.15em] text-slate-400">End date</p>
              <p className="mt-1 font-semibold text-slate-950">
                {(() => {
                  if (!agreement.startDate || !plan?.duration) return 'N/A'
                  
                  const startDate = new Date(agreement.startDate)
                  const duration = plan.duration
                  
                  if (duration.unit === 'MONTH') {
                    const endDate = new Date(startDate)
                    endDate.setMonth(endDate.getMonth() + duration.value)
                    return formatDate(endDate)
                  } else if (duration.unit === 'YEAR') {
                    const endDate = new Date(startDate)
                    endDate.setFullYear(endDate.getFullYear() + duration.value)
                    return formatDate(endDate)
                  }
                  return 'N/A'
                })()}
              </p>
            </div>
            <div className="rounded-2xl bg-slate-50 px-4 py-3">
              <p className="text-xs uppercase tracking-[0.15em] text-slate-400">Created</p>
              <p className="mt-1 font-semibold text-slate-950">{formatDate(agreement.createdAt)}</p>
            </div>
            {agreement.activatedAt && (
              <div className="rounded-2xl bg-slate-50 px-4 py-3">
                <p className="text-xs uppercase tracking-[0.15em] text-slate-400">Activated</p>
                <p className="mt-1 font-semibold text-slate-950">{formatDate(agreement.activatedAt)}</p>
              </div>
            )}
          </div>

          {/* Plan Details */}
          {plan ? (
            <>
              {/* Plan Name */}
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
                          <strong>Note:</strong> This breakdown shows the complete billing structure for this agreement plan.
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
                        {f.name}
                        {f.availability && ` · ${f.availability}`}
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
                      <p>
                        Penalty: {plan.latePaymentPolicy.penalty.type} · ₹{plan.latePaymentPolicy.penalty.amount}
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
                    <div className="flex justify-between">
                      <span>Smoking</span>
                      <span className="font-semibold">{plan.rulesAndRegulations.houseRules.smokingAllowed ? 'Allowed' : 'Not Allowed'}</span>
                    </div>
                    <div className="flex justify-between">
                      <span>Pets</span>
                      <span className="font-semibold">{plan.rulesAndRegulations.houseRules.petsAllowed ? 'Allowed' : 'Not Allowed'}</span>
                    </div>
                    {plan.rulesAndRegulations.houseRules.quietHours && (
                      <div className="flex justify-between">
                        <span>Quiet Hours</span>
                        <span className="font-semibold">
                          {plan.rulesAndRegulations.houseRules.quietHours.from} – {plan.rulesAndRegulations.houseRules.quietHours.to}
                        </span>
                      </div>
                    )}
                    {plan.rulesAndRegulations.facilityUsageRules?.length > 0 && (
                      <div className="mt-2">
                        <p className="font-semibold mb-1">Facility Usage Rules:</p>
                        <ul className="list-disc list-inside space-y-1 text-xs text-slate-600">
                          {plan.rulesAndRegulations.facilityUsageRules.map((rule, i) => (
                            <li key={i}>{rule}</li>
                          ))}
                        </ul>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* Cancellation Policy */}
              {plan.agreementCancellationRules?.tenantCancellation && (
                <div className="rounded-2xl bg-slate-50 p-4">
                  <p className="text-sm font-semibold text-slate-900 mb-2">🚪 Cancellation Policy</p>
                  <div className="text-sm text-slate-700 space-y-1">
                    <div className="flex justify-between">
                      <span>Tenant Cancellation</span>
                      <span className="font-semibold">{plan.agreementCancellationRules.tenantCancellation.allowed ? 'Allowed' : 'Not Allowed'}</span>
                    </div>
                    {plan.agreementCancellationRules.tenantCancellation.noticePeriodDays && (
                      <div className="flex justify-between">
                        <span>Notice Period</span>
                        <span className="font-semibold">{plan.agreementCancellationRules.tenantCancellation.noticePeriodDays} days</span>
                      </div>
                    )}
                    {plan.agreementCancellationRules.tenantCancellation.earlyExitPenalty && (
                      <div className="flex justify-between">
                        <span>Early Exit Penalty</span>
                        <span className="font-semibold">
                          {plan.agreementCancellationRules.tenantCancellation.earlyExitPenalty.type} · {plan.agreementCancellationRules.tenantCancellation.earlyExitPenalty.value}
                        </span>
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
                    <div className="flex justify-between">
                      <span>Agreement Lock</span>
                      <span className="font-semibold">{plan.legal.agreementLock ? 'Yes' : 'No'}</span>
                    </div>
                    <div className="flex justify-between">
                      <span>Modification After Sign</span>
                      <span className="font-semibold">{plan.legal.modificationAllowedAfterSign ? 'Allowed' : 'Not Allowed'}</span>
                    </div>
                    {plan.legal.jurisdiction && (
                      <div className="flex justify-between">
                        <span>Jurisdiction</span>
                        <span className="font-semibold">{plan.legal.jurisdiction}</span>
                      </div>
                    )}
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
            /* Fallback: legacy fields */
            <div className="grid grid-cols-2 gap-3">
              <div className="rounded-2xl bg-slate-50 px-4 py-3">
                <p className="text-xs uppercase tracking-[0.15em] text-slate-400">Rent</p>
                <p className="mt-1 font-semibold text-slate-950">₹{agreement.rent}</p>
              </div>
              <div className="rounded-2xl bg-slate-50 px-4 py-3">
                <p className="text-xs uppercase tracking-[0.15em] text-slate-400">Deposit</p>
                <p className="mt-1 font-semibold text-slate-950">₹{agreement.deposit || 0}</p>
              </div>
              {agreement.facilities?.length > 0 && (
                <div className="col-span-2 rounded-2xl bg-slate-50 px-4 py-3">
                  <p className="text-xs uppercase tracking-[0.15em] text-slate-400">Facilities</p>
                  <p className="mt-1 text-sm text-slate-700">{agreement.facilities.join(', ')}</p>
                </div>
              )}
            </div>
          )}

          {/* QR Info for pending agreements */}
          {agreement.status === 'PENDING_TENANT_ACTION' && (
            <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
              <p className="font-semibold">Awaiting tenant action</p>
              <p className="mt-1 break-all text-xs">
                Activation URL:{' '}
                <a
                  href={`${window.location.origin}/tenant/activate?token=${agreement.qrToken}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="underline text-amber-900 hover:text-amber-700"
                >
                  {`${window.location.origin}/tenant/activate?token=${agreement.qrToken}`}
                </a>
              </p>
              <p className="mt-1">Expires: {formatDate(agreement.qrExpiry)}</p>
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

export default function AgreementList() {
  const navigate = useNavigate()
  const [agreements, setAgreements] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedAgreement, setSelectedAgreement] = useState(null)
  const [loadingDetails, setLoadingDetails] = useState(false)

  useEffect(() => {
    fetchAgreements()
  }, [])

  const fetchAgreements = async () => {
    try {
      setLoading(true)
      setError('')
      const response = await agreementService.getAgreementCards()
      const agreementsData = response.data || []
      
      // Sort agreements by updated date in descending order (most recent first)
      const sortedAgreements = agreementsData.sort((a, b) => {
        // Use activatedAt, then createdAt as fallback for sorting
        const dateA = new Date(a.activatedAt || a.createdAt || 0)
        const dateB = new Date(b.activatedAt || b.createdAt || 0)
        return dateB - dateA // Descending order
      })
      
      setAgreements(sortedAgreements)
    } catch (err) {
      const errorData = err?.response?.data
      setError(errorData?.message || 'Failed to load agreements.')
    } finally {
      setLoading(false)
    }
  }

  const handleCardClick = async (agreementCard) => {
    try {
      setLoadingDetails(true)
      const response = await agreementService.getAgreementById(agreementCard.id)
      setSelectedAgreement(response.data)
    } catch (err) {
      console.error('Failed to load agreement details:', err)
      setError('Failed to load agreement details.')
    } finally {
      setLoadingDetails(false)
    }
  }

  const getStatusVariant = (status) => {
    switch (status) {
      case 'ACTIVE': return 'success'
      case 'PENDING_TENANT_ACTION': return 'warning'
      case 'REJECTED': return 'danger'
      default: return 'neutral'
    }
  }

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A'
    return new Date(dateString).toLocaleDateString()
  }

  if (loading) {
    return (
      <div className="grid gap-4 xl:grid-cols-2">
        {Array.from({ length: 4 }).map((_, index) => (
          <Skeleton key={index} className="h-64 rounded-3xl" />
        ))}
      </div>
    )
  }

  if (!loading && !error && agreements.length === 0) {
    return (
      <EmptyState
        icon={<ClipboardIcon className="h-5 w-5" />}
        title="No agreements yet"
        description="Create your first agreement to start a polished onboarding flow for tenants or workers."
        actionLabel="Create agreement"
        onAction={() => navigate('/owner/agreements/create')}
      />
    )
  }

  return (
    <>
      {selectedAgreement && (
        <AgreementDetailModal
          agreement={selectedAgreement}
          onClose={() => setSelectedAgreement(null)}
        />
      )}

      <div className="space-y-6">
        {error ? (
          <Alert tone="error" title="Agreement data couldn't be loaded">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <span>{error}</span>
              <Button label="Retry" variant="secondary" onClick={fetchAgreements} />
            </div>
          </Alert>
        ) : null}

        <Card>
          <CardHeader
            title="Agreement pipeline"
            description="A responsive overview of every agreement with status, rent, timing, and QR activation context."
          />
          <CardContent>
            <div className="grid gap-4 xl:grid-cols-2">
              {agreements.map((agreement) => {
                return (
                  <div
                    key={agreement.id}
                    className="cursor-pointer rounded-3xl border border-slate-200 bg-slate-50/80 p-5 transition hover:border-slate-300 hover:shadow-md"
                    onClick={() => handleCardClick(agreement)}
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div>
                        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Agreement</p>
                        <h3 className="mt-2 text-xl font-semibold text-slate-950">{agreement.type} agreement</h3>
                        <p className="mt-1 text-sm font-medium text-blue-600">{agreement.planName}</p>
                        {/* Duration Display */}
                        {agreement.planDurationValue && agreement.planDurationUnit && (
                          <p className="mt-1 text-sm text-slate-600">
                            Duration: {agreement.planDurationValue} {agreement.planDurationUnit.toLowerCase()}{agreement.planDurationValue > 1 ? 's' : ''}
                          </p>
                        )}
                      </div>
                      <Badge variant={getStatusVariant(agreement.status)}>
                        {agreement.status?.replaceAll('_', ' ')}
                      </Badge>
                    </div>

                    <div className="mt-5 grid gap-4 sm:grid-cols-2">
                      <div className="rounded-2xl bg-white px-4 py-3 shadow-sm">
                        <p className="text-xs uppercase tracking-[0.18em] text-slate-400">
                          {agreement.numberOfInstallments > 1 ? 'Installment Amount' : 'Monthly Rent'}
                        </p>
                        <p className="mt-2 text-lg font-semibold text-slate-950">
                          ₹{agreement.numberOfInstallments > 1 ? agreement.installmentAmount?.toLocaleString() : agreement.monthlyRent?.toLocaleString()}
                        </p>
                        {agreement.numberOfInstallments > 1 && (
                          <p className="text-xs text-slate-500 mt-1">
                            {agreement.numberOfInstallments} installments · {agreement.paymentTiming}
                          </p>
                        )}
                      </div>
                      <div className="rounded-2xl bg-white px-4 py-3 shadow-sm">
                        <p className="text-xs uppercase tracking-[0.18em] text-slate-400">Security Deposit</p>
                        <p className="mt-2 text-lg font-semibold text-green-700">₹{agreement.securityDeposit?.toLocaleString()}</p>
                        <p className="text-xs text-green-600 mt-1">Refundable</p>
                      </div>
                    </div>

                    <div className="mt-5 grid gap-3 text-sm text-slate-600">
                      <div className="grid grid-cols-2 gap-4">
                        <div>
                          <p><span className="font-semibold text-slate-900">Start date:</span></p>
                          <p className="text-slate-700">{formatDate(agreement.startDate)}</p>
                        </div>
                        <div>
                          <p><span className="font-semibold text-slate-900">End date:</span></p>
                          <p className="text-slate-700">
                            {(() => {
                              if (!agreement.startDate || !agreement.planDurationValue || !agreement.planDurationUnit) return 'N/A'
                              
                              const startDate = new Date(agreement.startDate)
                              
                              if (agreement.planDurationUnit === 'MONTH') {
                                const endDate = new Date(startDate)
                                endDate.setMonth(endDate.getMonth() + agreement.planDurationValue)
                                return formatDate(endDate)
                              } else if (agreement.planDurationUnit === 'YEAR') {
                                const endDate = new Date(startDate)
                                endDate.setFullYear(endDate.getFullYear() + agreement.planDurationValue)
                                return formatDate(endDate)
                              }
                              return 'N/A'
                            })()}
                          </p>
                        </div>
                      </div>
                      <div className="grid grid-cols-2 gap-4">
                        <div>
                          <p><span className="font-semibold text-slate-900">Created:</span></p>
                          <p className="text-slate-700">{formatDate(agreement.createdAt)}</p>
                        </div>
                        {agreement.activatedAt && (
                          <div>
                            <p><span className="font-semibold text-slate-900">Activated:</span></p>
                            <p className="text-slate-700">{formatDate(agreement.activatedAt)}</p>
                          </div>
                        )}
                      </div>
                    </div>

                    {agreement.status === 'PENDING_TENANT_ACTION' && (
                      <div className="mt-5 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
                        <p className="font-semibold">Awaiting tenant action</p>
                        <p className="mt-1 break-all">
                          Activation URL:{' '}
                          <a
                            href={`${window.location.origin}/tenant/activate?token=${agreement.qrToken}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="underline text-amber-900 hover:text-amber-700"
                          >
                            {`${window.location.origin}/tenant/activate?token=${agreement.qrToken}`}
                          </a>
                        </p>
                        <p className="mt-1">Expires: {formatDate(agreement.qrExpiry)}</p>
                      </div>
                    )}

                    <p className="mt-4 text-xs text-slate-400 text-right">
                      {loadingDetails ? 'Loading details...' : 'Click to view full details →'}
                    </p>
                  </div>
                )
              })}
            </div>
          </CardContent>
        </Card>
      </div>
    </>
  )
}
