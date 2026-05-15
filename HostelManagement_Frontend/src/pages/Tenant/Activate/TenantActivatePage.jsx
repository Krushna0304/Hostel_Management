import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import agreementService from '../../../services/agreementService'
import apiClient from '../../../services/apiClient'
import FormInput from '../../../components/FormInput'
import { loadRazorpay, preloadRazorpay } from '../../../utils/razorpayLoader'
import {
  Alert,
  Badge,
  Button,
  Card,
  CardContent,
  CardHeader,
  LoadingScreen,
} from '../../../components/ui'

export default function TenantActivatePage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const navigate = useNavigate()
  const [agreement, setAgreement] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [step, setStep] = useState('view')
  const [paymentMode, setPaymentMode] = useState('ONLINE')
  const [otp, setOtp] = useState('')
  const [otpSent, setOtpSent] = useState(false)
  const [otpMessage, setOtpMessage] = useState('')
  const [sendingOtp, setSendingOtp] = useState(false)
  const [processing, setProcessing] = useState(false)
  const [resetToken, setResetToken] = useState('')
  const [passwordData, setPasswordData] = useState({
    password: '',
    confirmPassword: '',
  })

  useEffect(() => {
    fetchAgreement()
    // Preload Razorpay since this is a payment page
    preloadRazorpay()
  }, [token])

  const fetchAgreement = async () => {
    try {
      setLoading(true)
      setError('')
      const response = await agreementService.getAgreementByQrToken(token)
      setAgreement(response.data)
    } catch (err) {
      const errorData = err?.response?.data
      if (errorData && typeof errorData === 'object' && !errorData.message) {
        setError(Object.values(errorData).join(', ') || 'Invalid or expired QR token.')
      } else {
        setError(errorData?.message || 'Invalid or expired QR token.')
      }
    } finally {
      setLoading(false)
    }
  }

  const handleReject = async () => {
    if (!window.confirm('Are you sure you want to reject this agreement?')) {
      return
    }

    try {
      setProcessing(true)
      await agreementService.rejectAgreement(agreement.id)
      navigate('/')
    } catch (err) {
      const errorData = err?.response?.data
      if (errorData && typeof errorData === 'object' && !errorData.message) {
        setError(Object.values(errorData).join(', ') || 'Failed to reject agreement.')
      } else {
        setError(errorData?.message || 'Failed to reject agreement.')
      }
    } finally {
      setProcessing(false)
    }
  }

  const handlePayment = async () => {
    if (paymentMode === 'CASH') {
      if (!otpSent) {
        setError('Please request OTP first by clicking "Send OTP to Owner".')
        return
      }
      if (!otp.trim()) {
        setError('Please enter the OTP provided by the owner.')
        return
      }
      // Cash payment — confirm directly
      await confirmAgreement({ paymentMode: 'CASH', otp })
    } else {
      // Online payment — open Razorpay checkout first
      await handleRazorpayCheckout()
    }
  }

  const handleRazorpayCheckout = async () => {
    try {
      setProcessing(true)
      setError('')

      // Step 1: Load Razorpay script dynamically
      await loadRazorpay()

      // Step 2: Create order on backend
      const orderRes = await apiClient.post('/api/payments/create-order', {
        agreementId: agreement.id,
        amount: agreement.planSnapshot?.rentDetails?.monthlyRent
          || agreement.rent
          || 0,
        currency: 'INR',
      })

      const { orderId, providerKey, amount, currency, provider } = orderRes.data

      // Step 3: If mock provider, skip checkout UI and confirm directly
      if (provider === 'mock') {
        await confirmAgreement({
          paymentMode: 'ONLINE',
          razorpayOrderId: orderId,
          razorpayPaymentId: 'mock_pay_' + Date.now(),
          razorpaySignature: 'mock_signature',
        })
        return
      }

      // Step 4: Open Razorpay checkout
      const options = {
        key: providerKey,
        amount,
        currency,
        name: 'Hostel Management',
        description: 'Agreement Activation Payment',
        order_id: orderId,
        handler: async (response) => {
          // Step 5: Verify payment on backend then activate agreement
          await confirmAgreement({
            paymentMode: 'ONLINE',
            razorpayOrderId: response.razorpay_order_id,
            razorpayPaymentId: response.razorpay_payment_id,
            razorpaySignature: response.razorpay_signature,
          })
        },
        prefill: { name: '', email: '', contact: '' },
        theme: { color: '#0f172a' },
        modal: {
          ondismiss: () => {
            setProcessing(false)
            setError('Payment cancelled. Please try again.')
          },
        },
      }

      const rzp = new window.Razorpay(options)
      rzp.on('payment.failed', (response) => {
        setProcessing(false)
        setError('Payment failed: ' + response.error.description)
      })
      rzp.open()

    } catch (err) {
      setProcessing(false)
      if (err.message && err.message.includes('Razorpay')) {
        setError('Failed to load payment system. Please check your internet connection and try again.')
      } else {
        const errorData = err?.response?.data
        setError(errorData?.message || 'Failed to initiate payment. Please try again.')
      }
    }
  }

  const confirmAgreement = async (paymentData) => {
    try {
      setProcessing(true)
      setError('')
      const response = await agreementService.acceptAgreement(agreement.id, paymentData)
      if (response.data?.passwordResetToken) {
        setResetToken(response.data.passwordResetToken)
      }
      setStep('password')
    } catch (err) {
      const errorData = err?.response?.data
      if (errorData && typeof errorData === 'object' && !errorData.message) {
        setError(Object.values(errorData).join(', ') || 'Payment failed. Please try again.')
      } else {
        setError(errorData?.message || 'Payment failed. Please try again.')
      }
    } finally {
      setProcessing(false)
    }
  }

  const handleSendOtp = async () => {
    try {
      setSendingOtp(true)
      setError('')
      setOtpMessage('')
      
      const response = await apiClient.post(`/api/cash-payment-otp/send/${agreement.id}`)
      setOtpSent(true)
      setOtpMessage(response.data.message || 'OTP sent to owner successfully')
    } catch (err) {
      const errorData = err?.response?.data
      setError(errorData?.message || 'Failed to send OTP. Please try again.')
    } finally {
      setSendingOtp(false)
    }
  }

  const handleSetPassword = async () => {
    if (passwordData.password !== passwordData.confirmPassword) {
      setError('Passwords do not match.')
      return
    }
    if (passwordData.password.length < 8) {
      setError('Password must be at least 8 characters.')
      return
    }

    if (!resetToken) {
      setError('Password reset token is missing. Please contact support.')
      return
    }

    try {
      setProcessing(true)
      setError('')
      
      // Use apiClient to ensure correct base URL (localhost:8080)
      await apiClient.post('/api/password-reset/reset', {
        token: resetToken,
        newPassword: passwordData.password
      })
      
      navigate('/login', { state: { message: 'Account activated successfully. Please sign in.' } })
    } catch (err) {
      const errorData = err.response?.data
      setError(errorData?.error || errorData?.message || 'Failed to set password. Please try again.')
    } finally {
      setProcessing(false)
    }
  }

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A'
    return new Date(dateString).toLocaleDateString()
  }

  const totalAmount =
    parseFloat(agreement?.rent || 0) +
    parseFloat(agreement?.deposit || 0) +
    parseFloat(agreement?.cleaningCharges || 0) +
    parseFloat(agreement?.maintenanceCharges || 0)

  if (loading) {
    return <LoadingScreen title="Loading agreement..." />
  }

  if (error && !agreement) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
        <div className="w-full max-w-lg">
          <Card>
            <CardContent className="space-y-4 text-center">
              <h1 className="text-2xl font-semibold text-slate-950">Agreement unavailable</h1>
              <Alert tone="error">{error}</Alert>
              <Button label="Go to home" onClick={() => navigate('/')} />
            </CardContent>
          </Card>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-[radial-gradient(circle_at_top,_rgba(14,165,233,0.12),_transparent_30%),linear-gradient(180deg,#f8fafc_0%,#eef2ff_100%)] px-4 py-10">
      <div className="mx-auto max-w-4xl space-y-6">
        <div className="space-y-2 text-center">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-sky-600">Tenant activation</p>
          <h1 className="text-3xl font-bold tracking-tight text-slate-950">Review and activate your agreement</h1>
          <p className="text-sm leading-6 text-slate-500">A guided activation flow with clear payment and account setup steps.</p>
        </div>

        <Card>
          <CardHeader
            title={
              step === 'view'
                ? 'Agreement details'
                : step === 'payment'
                  ? 'Complete payment'
                  : 'Set your password'
            }
            description="Review the agreement, confirm payment, and finish account setup."
          />
          <CardContent className="space-y-6">
            {error ? <Alert tone="error">{error}</Alert> : null}

            {step === 'view' ? (
              <>
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-slate-500">Agreement type</p>
                    <p className="text-xl font-semibold text-slate-950">{agreement.type}</p>
                  </div>
                  <Badge variant={agreement.status === 'PENDING_TENANT_ACTION' ? 'warning' : 'success'}>
                    {agreement.status?.replaceAll('_', ' ')}
                  </Badge>
                </div>

                {/* Complete Plan Details from planSnapshot */}
                {agreement.planSnapshot ? (
                  <div className="space-y-4 max-h-[500px] overflow-y-auto">
                    <div className="rounded-2xl bg-blue-50 border border-blue-200 p-4">
                      <h3 className="font-bold text-lg text-blue-900">{agreement.planSnapshot.planName}</h3>
                      <p className="text-xs text-blue-700 mt-1">Plan ID: {agreement.planSnapshot.id}</p>
                    </div>

                    {/* Comprehensive Billing Breakdown */}
                    <div className="rounded-2xl bg-blue-50 border border-blue-200 p-4">
                      <h4 className="text-sm font-semibold text-blue-900 mb-3">💳 Comprehensive Billing Breakdown</h4>
                      
                      {/* Calculate billing breakdown */}
                      {(() => {
                        const baseRent = Number(agreement.planSnapshot.rentDetails?.monthlyRent) || 0
                        const securityDeposit = Number(agreement.planSnapshot.charges?.securityDeposit?.amount) || 0
                        const oneTimeMaintenance = Number(agreement.planSnapshot.charges?.maintenanceCharges?.oneTimeMaintenanceCharge?.amount) || 0
                        const monthlyCleaning = Number(agreement.planSnapshot.charges?.cleaningCharges?.monthlyCleaningCharge?.amount) || 0
                        const monthlyMaintenance = Number(agreement.planSnapshot.charges?.maintenanceCharges?.monthlyMaintenanceCharge?.amount) || 0
                        const electricity = Number(agreement.planSnapshot.charges?.utilityCharges?.electricity?.fixedAmount) || 0
                        const water = Number(agreement.planSnapshot.charges?.utilityCharges?.water?.monthlyAmount) || 0
                        const deepCleaning = Number(agreement.planSnapshot.charges?.cleaningCharges?.deepCleaningOnExit?.amount) || 0

                        // Calculate custom charges
                        const customOneTimeCharges = (agreement.planSnapshot.charges?.customCharges?.oneTimeCharges || []).reduce((total, charge) => {
                          return total + (Number(charge.amount) || 0)
                        }, 0)

                        const customMonthlyRecurringCharges = (agreement.planSnapshot.charges?.customCharges?.monthlyRecurringCharges || []).reduce((total, charge) => {
                          return total + (Number(charge.amount) || 0)
                        }, 0)

                        // Total calculations
                        const totalOneTimeCharges = oneTimeMaintenance + customOneTimeCharges
                        const recurringCharges = monthlyCleaning + monthlyMaintenance + electricity + water + customMonthlyRecurringCharges
                        const monthlyTotal = baseRent + recurringCharges

                        // Calculate installment details
                        const totalDuration = Number(agreement.planSnapshot.duration?.value) || 12
                        const numberOfInstallments = Number(agreement.planSnapshot.paymentModel?.installments) || 1
                        const monthsPerInstallment = Math.ceil(totalDuration / numberOfInstallments)
                        const installmentAmount = monthlyTotal * monthsPerInstallment
                        const activationTotal = installmentAmount + securityDeposit + totalOneTimeCharges

                        return (
                          <div className="space-y-3">
                            {/* Agreement Activation Amount */}
                            <div className="bg-white border border-blue-200 rounded-xl p-3">
                              <h5 className="text-sm font-semibold text-blue-900 mb-2">🚀 Agreement Activation Amount</h5>
                              <div className="space-y-2 text-sm">
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
                                    {agreement.planSnapshot.charges?.customCharges?.oneTimeCharges && agreement.planSnapshot.charges.customCharges.oneTimeCharges.map((charge, index) => (
                                      <div key={index} className="flex justify-between">
                                        <span>• {charge.chargeName}:</span>
                                        <span>₹{Number(charge.amount).toLocaleString()}</span>
                                      </div>
                                    ))}
                                  </div>
                                )}
                                
                                <div className="border-t border-blue-300 pt-2 flex justify-between">
                                  <span className="font-semibold text-blue-900">Total Activation:</span>
                                  <span className="font-bold text-blue-800 text-lg">₹{activationTotal.toLocaleString()}</span>
                                </div>
                              </div>
                            </div>

                            {/* Monthly Amount Breakdown */}
                            <div className="bg-white border border-green-200 rounded-xl p-3">
                              <h5 className="text-sm font-semibold text-green-900 mb-2">📅 Monthly Amount Breakdown</h5>
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
                                    {agreement.planSnapshot.charges?.customCharges?.monthlyRecurringCharges && agreement.planSnapshot.charges.customCharges.monthlyRecurringCharges.map((charge, index) => (
                                      <div key={index} className="flex justify-between">
                                        <span>• {charge.chargeName}:</span>
                                        <span>₹{Number(charge.amount).toLocaleString()}</span>
                                      </div>
                                    ))}
                                  </div>
                                )}
                                
                                <div className="border-t border-green-300 pt-2 flex justify-between">
                                  <span className="font-semibold text-green-900">Total Monthly:</span>
                                  <span className="font-bold text-green-800 text-lg">₹{monthlyTotal.toLocaleString()}</span>
                                </div>
                              </div>
                            </div>

                            {/* Installment Details (if multiple installments) */}
                            {numberOfInstallments > 1 && (
                              <div className="bg-white border border-orange-200 rounded-xl p-3">
                                <h5 className="text-sm font-semibold text-orange-900 mb-2">💰 Installment Payment Details</h5>
                                <div className="space-y-2 text-sm">
                                  <div className="flex justify-between">
                                    <span className="text-orange-700">Monthly Amount:</span>
                                    <span className="font-semibold text-orange-900">₹{monthlyTotal.toLocaleString()}</span>
                                  </div>
                                  <div className="flex justify-between">
                                    <span className="text-orange-700">Months per Installment:</span>
                                    <span className="font-semibold text-orange-900">{monthsPerInstallment} months</span>
                                  </div>
                                  <div className="border-t border-orange-300 pt-2 flex justify-between">
                                    <span className="font-semibold text-orange-900">Installment Amount:</span>
                                    <span className="font-bold text-orange-800 text-lg">₹{installmentAmount.toLocaleString()}</span>
                                  </div>
                                  <div className="bg-orange-50 rounded-lg px-2 py-1 text-xs text-orange-800">
                                    <strong>Schedule:</strong> {numberOfInstallments} installments covering {totalDuration} months
                                  </div>
                                </div>
                              </div>
                            )}

                            {/* Exit Settlement */}
                            <div className="bg-white border border-slate-200 rounded-xl p-3">
                              <h5 className="text-sm font-semibold text-slate-900 mb-2">🏁 Exit Settlement Estimate</h5>
                              <div className="space-y-2 text-sm">
                                <div className="flex justify-between">
                                  <span className="text-slate-700">Refundable Amount:</span>
                                  <span className="font-semibold text-green-700">₹{securityDeposit.toLocaleString()}</span>
                                </div>
                                <div className="flex justify-between">
                                  <span className="text-slate-700">Potential Deductions:</span>
                                  <span className="font-semibold text-red-600">₹{deepCleaning.toLocaleString()}</span>
                                </div>
                                <div className="border-t border-slate-300 pt-2 flex justify-between">
                                  <span className="font-semibold text-slate-900">Net Refund Estimate:</span>
                                  <span className="font-bold text-blue-700">₹{(securityDeposit - deepCleaning).toLocaleString()}</span>
                                </div>
                              </div>
                            </div>

                            <div className="bg-amber-50 border border-amber-200 rounded px-2 py-1">
                              <p className="text-xs text-amber-700">
                                <strong>Note:</strong> This breakdown shows your exact payment obligations. The activation amount is due upon agreement acceptance.
                              </p>
                            </div>
                          </div>
                        )
                      })()}
                    </div>

                    {/* Payment Model Configuration */}
                    <div className="rounded-2xl bg-slate-50 p-4">
                      <p className="text-sm font-semibold text-slate-900 mb-3">🔄 Payment Model Configuration</p>
                      <div className="grid gap-2 text-sm">
                        {agreement.planSnapshot.paymentModel && (
                          <>
                            <div className="flex justify-between">
                              <span className="text-slate-600">Payment Mode:</span>
                              <span className="font-semibold">{agreement.planSnapshot.paymentModel.mode}</span>
                            </div>
                            <div className="flex justify-between">
                              <span className="text-slate-600">Payment Timing:</span>
                              <span className="font-semibold">{agreement.planSnapshot.paymentModel.paymentTiming}</span>
                            </div>
                            <div className="flex justify-between">
                              <span className="text-slate-600">Number of Installments:</span>
                              <span className="font-semibold">{agreement.planSnapshot.paymentModel.installments}</span>
                            </div>
                            <div className="flex justify-between">
                              <span className="text-slate-600">Due Day of Month:</span>
                              <span className="font-semibold">Day {agreement.planSnapshot.paymentModel.dueDayOfMonth}</span>
                            </div>
                          </>
                        )}
                      </div>
                    </div>

                    {/* Duration */}
                    {agreement.planSnapshot.duration && (
                      <div className="rounded-2xl bg-slate-50 p-4">
                        <p className="text-sm font-semibold text-slate-900 mb-2">📅 Agreement Duration</p>
                        <p className="text-sm text-slate-700">
                          {agreement.planSnapshot.duration.value} {agreement.planSnapshot.duration.unit}(s)
                          {agreement.planSnapshot.duration.minimumStayMonths && 
                            ` (Minimum stay: ${agreement.planSnapshot.duration.minimumStayMonths} months)`}
                        </p>
                      </div>
                    )}

                    {/* Facilities */}
                    {agreement.planSnapshot.freeFacilities?.facilities && agreement.planSnapshot.freeFacilities.facilities.length > 0 && (
                      <div className="rounded-2xl bg-slate-50 p-4">
                        <p className="text-sm font-semibold text-slate-900 mb-3">✨ Free Facilities Included</p>
                        <div className="flex flex-wrap gap-2">
                          {agreement.planSnapshot.freeFacilities.facilities.map((facility, i) => (
                            <div key={i} className="px-3 py-2 bg-green-100 text-green-800 rounded-lg text-xs" title={facility.description}>
                              <p className="font-semibold">{facility.name}</p>
                              {facility.availability && <p className="text-[10px]">{facility.availability}</p>}
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Late Payment Policy */}
                    {agreement.planSnapshot.latePaymentPolicy && (
                      <div className="rounded-2xl bg-yellow-50 border border-yellow-200 p-4">
                        <p className="text-sm font-semibold text-yellow-900 mb-2">⚠️ Late Payment Policy</p>
                        <div className="text-sm text-yellow-800 space-y-1">
                          <p>Grace Period: {agreement.planSnapshot.latePaymentPolicy.gracePeriodDays} days</p>
                          {agreement.planSnapshot.latePaymentPolicy.penalty && (
                            <p>Penalty: {agreement.planSnapshot.latePaymentPolicy.penalty.type} - ₹{agreement.planSnapshot.latePaymentPolicy.penalty.amount}
                            {agreement.planSnapshot.latePaymentPolicy.penalty.maxAmount && 
                              ` (Maximum: ₹${agreement.planSnapshot.latePaymentPolicy.penalty.maxAmount})`}</p>
                          )}
                        </div>
                      </div>
                    )}

                    {/* House Rules */}
                    {agreement.planSnapshot.rulesAndRegulations?.houseRules && (
                      <div className="rounded-2xl bg-slate-50 p-4">
                        <p className="text-sm font-semibold text-slate-900 mb-3">📋 House Rules</p>
                        <div className="text-sm text-slate-700 space-y-2">
                          <div className="flex justify-between">
                            <span>Smoking:</span>
                            <span className="font-semibold">{agreement.planSnapshot.rulesAndRegulations.houseRules.smokingAllowed ? "Allowed" : "Not Allowed"}</span>
                          </div>
                          <div className="flex justify-between">
                            <span>Pets:</span>
                            <span className="font-semibold">{agreement.planSnapshot.rulesAndRegulations.houseRules.petsAllowed ? "Allowed" : "Not Allowed"}</span>
                          </div>
                          {agreement.planSnapshot.rulesAndRegulations.houseRules.quietHours && (
                            <div className="flex justify-between">
                              <span>Quiet Hours:</span>
                              <span className="font-semibold">
                                {agreement.planSnapshot.rulesAndRegulations.houseRules.quietHours.from} - {agreement.planSnapshot.rulesAndRegulations.houseRules.quietHours.to}
                              </span>
                            </div>
                          )}
                          {agreement.planSnapshot.rulesAndRegulations.facilityUsageRules && 
                           agreement.planSnapshot.rulesAndRegulations.facilityUsageRules.length > 0 && (
                            <div className="mt-3">
                              <p className="font-semibold mb-2">Facility Usage Rules:</p>
                              <ul className="list-disc list-inside space-y-1 text-xs">
                                {agreement.planSnapshot.rulesAndRegulations.facilityUsageRules.map((rule, i) => (
                                  <li key={i}>{rule}</li>
                                ))}
                              </ul>
                            </div>
                          )}
                        </div>
                      </div>
                    )}

                    {/* Cancellation Policy */}
                    {agreement.planSnapshot.agreementCancellationRules?.tenantCancellation && (
                      <div className="rounded-2xl bg-slate-50 p-4">
                        <p className="text-sm font-semibold text-slate-900 mb-2">🚪 Cancellation Policy</p>
                        <div className="text-sm text-slate-700 space-y-1">
                          <p>Tenant Cancellation: {agreement.planSnapshot.agreementCancellationRules.tenantCancellation.allowed ? "Allowed" : "Not Allowed"}</p>
                          {agreement.planSnapshot.agreementCancellationRules.tenantCancellation.noticePeriodDays && (
                            <p>Notice Period: {agreement.planSnapshot.agreementCancellationRules.tenantCancellation.noticePeriodDays} days</p>
                          )}
                          {agreement.planSnapshot.agreementCancellationRules.tenantCancellation.earlyExitPenalty && (
                            <p>Early Exit Penalty: {agreement.planSnapshot.agreementCancellationRules.tenantCancellation.earlyExitPenalty.type} - {agreement.planSnapshot.agreementCancellationRules.tenantCancellation.earlyExitPenalty.value}</p>
                          )}
                        </div>
                      </div>
                    )}

                    {/* Legal */}
                    {agreement.planSnapshot.legal && (
                      <div className="rounded-2xl bg-slate-50 p-4">
                        <p className="text-sm font-semibold text-slate-900 mb-2">⚖️ Legal Terms</p>
                        <div className="text-sm text-slate-700 space-y-1">
                          <p>Agreement Lock: {agreement.planSnapshot.legal.agreementLock ? "Yes" : "No"}</p>
                          <p>Modification After Sign: {agreement.planSnapshot.legal.modificationAllowedAfterSign ? "Allowed" : "Not Allowed"}</p>
                          {agreement.planSnapshot.legal.jurisdiction && <p>Jurisdiction: {agreement.planSnapshot.legal.jurisdiction}</p>}
                        </div>
                      </div>
                    )}

                    {/* Custom Fields */}
                    {agreement.planSnapshot.customFields && Object.keys(agreement.planSnapshot.customFields).length > 0 && (
                      <div className="rounded-2xl bg-amber-50 border border-amber-200 p-4">
                        <p className="text-sm font-semibold text-amber-900 mb-3">🔧 Additional Plan Details</p>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
                          {Object.entries(agreement.planSnapshot.customFields).map(([key, value]) => (
                            <div key={key} className="flex justify-between">
                              <span className="text-amber-700 font-medium">{key}:</span>
                              <span className="text-amber-900 font-semibold">{String(value)}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Start Date */}
                    <div className="rounded-2xl bg-slate-50 p-4">
                      <p className="text-sm font-semibold text-slate-900 mb-2">📆 Agreement Start Date</p>
                      <p className="text-lg font-bold text-slate-950">{formatDate(agreement.startDate)}</p>
                    </div>
                  </div>
                ) : (
                  /* Fallback to legacy fields if planSnapshot is not available */
                  <div className="grid gap-4 md:grid-cols-2">
                    {[
                      ['Rent', `₹${agreement.rent}`],
                      ['Deposit', `₹${agreement.deposit || 0}`],
                      ['Cleaning charges', `₹${agreement.cleaningCharges}`],
                      ['Maintenance charges', `₹${agreement.maintenanceCharges}`],
                      ['Parking', agreement.parkingAllowed ? 'Allowed' : 'Not allowed'],
                      ['Start date', formatDate(agreement.startDate)],
                    ].map(([label, value]) => (
                      <div key={label} className="rounded-2xl bg-slate-50 px-4 py-3">
                        <p className="text-xs uppercase tracking-[0.18em] text-slate-400">{label}</p>
                        <p className="mt-2 text-lg font-semibold text-slate-950">{value}</p>
                      </div>
                    ))}
                    <div className="col-span-2 rounded-2xl border border-slate-200 px-4 py-4">
                      <p className="text-sm font-semibold text-slate-900">Facilities</p>
                      <p className="mt-2 text-sm leading-6 text-slate-500">
                        {agreement.facilities?.length > 0 ? agreement.facilities.join(', ') : 'No facilities specified.'}
                      </p>
                    </div>
                  </div>
                )}

                <div className="flex flex-col gap-3 sm:flex-row">
                  <Button
                    label="Accept and continue"
                    onClick={() => setStep('payment')}
                    disabled={processing || agreement?.status !== 'PENDING_TENANT_ACTION'}
                    fullWidth
                  />
                  <Button
                    label="Reject agreement"
                    onClick={handleReject}
                    disabled={processing || agreement?.status !== 'PENDING_TENANT_ACTION'}
                    fullWidth
                    variant="danger"
                  />
                </div>
              </>
            ) : null}

            {step === 'payment' ? (
              <>
                <div className="grid gap-4 md:grid-cols-2">
                  <button
                    type="button"
                    onClick={() => {
                      setPaymentMode('ONLINE')
                      setOtpSent(false)
                      setOtp('')
                      setOtpMessage('')
                    }}
                    className={`rounded-3xl border p-5 text-left transition ${
                      paymentMode === 'ONLINE' ? 'border-sky-300 bg-sky-50' : 'border-slate-200 bg-white'
                    }`}
                  >
                    <p className="text-lg font-semibold text-slate-950">Online payment</p>
                    <p className="mt-2 text-sm text-slate-500">Continue digitally with the default activation flow.</p>
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setPaymentMode('CASH')
                      setOtpSent(false)
                      setOtp('')
                      setOtpMessage('')
                    }}
                    className={`rounded-3xl border p-5 text-left transition ${
                      paymentMode === 'CASH' ? 'border-sky-300 bg-sky-50' : 'border-slate-200 bg-white'
                    }`}
                  >
                    <p className="text-lg font-semibold text-slate-950">Cash payment</p>
                    <p className="mt-2 text-sm text-slate-500">Use OTP verification for an in-person payment handoff.</p>
                  </button>
                </div>

                {paymentMode === 'CASH' ? (
                  <div className="space-y-4">
                    <div className="rounded-2xl bg-amber-50 border border-amber-200 p-4">
                      <p className="text-sm font-semibold text-amber-900 mb-2">Cash Payment Process:</p>
                      <ol className="text-sm text-amber-800 space-y-1 list-decimal list-inside">
                        <li>Click "Send OTP to Owner" button below</li>
                        <li>Owner will receive OTP on their registered mobile number</li>
                        <li>Collect the OTP from the owner after handing over cash</li>
                        <li>Enter the OTP and confirm payment</li>
                      </ol>
                    </div>

                    {!otpSent ? (
                      <Button
                        label="Send OTP to Owner"
                        onClick={handleSendOtp}
                        loading={sendingOtp}
                        fullWidth
                        variant="secondary"
                      />
                    ) : (
                      <Alert tone="success" title="OTP Sent">
                        {otpMessage}
                      </Alert>
                    )}

                    {otpSent && (
                      <FormInput
                        label="Enter OTP from Owner"
                        name="otp"
                        value={otp}
                        onChange={(event) => setOtp(event.target.value)}
                        placeholder="Enter 6-digit OTP"
                        required
                        maxLength={6}
                      />
                    )}
                  </div>
                ) : null}

                <div className="rounded-3xl bg-slate-950 p-5 text-white">
                  <p className="text-sm font-semibold text-slate-300">Payment summary</p>
                  <div className="mt-4 space-y-3 text-sm">
                    {agreement.planSnapshot ? (
                      // Use comprehensive billing breakdown for planSnapshot
                      (() => {
                        const baseRent = Number(agreement.planSnapshot.rentDetails?.monthlyRent) || 0
                        const securityDeposit = Number(agreement.planSnapshot.charges?.securityDeposit?.amount) || 0
                        const oneTimeMaintenance = Number(agreement.planSnapshot.charges?.maintenanceCharges?.oneTimeMaintenanceCharge?.amount) || 0
                        const monthlyCleaning = Number(agreement.planSnapshot.charges?.cleaningCharges?.monthlyCleaningCharge?.amount) || 0
                        const monthlyMaintenance = Number(agreement.planSnapshot.charges?.maintenanceCharges?.monthlyMaintenanceCharge?.amount) || 0
                        const electricity = Number(agreement.planSnapshot.charges?.utilityCharges?.electricity?.fixedAmount) || 0
                        const water = Number(agreement.planSnapshot.charges?.utilityCharges?.water?.monthlyAmount) || 0

                        // Calculate custom charges
                        const customOneTimeCharges = (agreement.planSnapshot.charges?.customCharges?.oneTimeCharges || []).reduce((total, charge) => {
                          return total + (Number(charge.amount) || 0)
                        }, 0)

                        const customMonthlyRecurringCharges = (agreement.planSnapshot.charges?.customCharges?.monthlyRecurringCharges || []).reduce((total, charge) => {
                          return total + (Number(charge.amount) || 0)
                        }, 0)

                        // Total calculations
                        const totalOneTimeCharges = oneTimeMaintenance + customOneTimeCharges
                        const recurringCharges = monthlyCleaning + monthlyMaintenance + electricity + water + customMonthlyRecurringCharges
                        const monthlyTotal = baseRent + recurringCharges

                        // Calculate installment details
                        const totalDuration = Number(agreement.planSnapshot.duration?.value) || 12
                        const numberOfInstallments = Number(agreement.planSnapshot.paymentModel?.installments) || 1
                        const monthsPerInstallment = Math.ceil(totalDuration / numberOfInstallments)
                        const installmentAmount = monthlyTotal * monthsPerInstallment
                        const activationTotal = installmentAmount + securityDeposit + totalOneTimeCharges

                        return (
                          <>
                            <div className="flex justify-between">
                              <span>First Installment</span>
                              <span>₹{installmentAmount.toLocaleString()}</span>
                            </div>
                            <div className="flex justify-between">
                              <span>Refundable Deposits</span>
                              <span>₹{securityDeposit.toLocaleString()}</span>
                            </div>
                            <div className="flex justify-between">
                              <span>One-time Charges</span>
                              <span>₹{totalOneTimeCharges.toLocaleString()}</span>
                            </div>
                            <div className="flex justify-between border-t border-white/10 pt-3 text-base font-semibold">
                              <span>Agreement Activation Total</span>
                              <span>₹{activationTotal.toLocaleString()}</span>
                            </div>
                          </>
                        )
                      })()
                    ) : (
                      // Fallback to legacy fields if planSnapshot is not available
                      <>
                        <div className="flex justify-between">
                          <span>Rent</span>
                          <span>₹{agreement.rent}</span>
                        </div>
                        <div className="flex justify-between">
                          <span>Deposit</span>
                          <span>₹{agreement.deposit || 0}</span>
                        </div>
                        <div className="flex justify-between">
                          <span>Cleaning charges</span>
                          <span>₹{agreement.cleaningCharges}</span>
                        </div>
                        <div className="flex justify-between">
                          <span>Maintenance</span>
                          <span>₹{agreement.maintenanceCharges}</span>
                        </div>
                        <div className="flex justify-between border-t border-white/10 pt-3 text-base font-semibold">
                          <span>Total</span>
                          <span>₹{totalAmount}</span>
                        </div>
                      </>
                    )}
                  </div>
                </div>

                <div className="flex flex-col gap-3 sm:flex-row">
                  <Button label="Confirm payment" onClick={handlePayment} loading={processing} fullWidth />
                  <Button label="Back" onClick={() => setStep('view')} variant="secondary" fullWidth />
                </div>
              </>
            ) : null}

            {step === 'password' ? (
              <>
                <Alert tone="success" title="Payment complete">
                  Set a secure password to finish activation and prepare your account for sign-in.
                </Alert>

                <div className="grid gap-5 sm:grid-cols-2">
                  <FormInput
                    label="Password"
                    name="password"
                    type="password"
                    value={passwordData.password}
                    onChange={(event) => setPasswordData((prev) => ({ ...prev, password: event.target.value }))}
                    required
                  />

                  <FormInput
                    label="Confirm password"
                    name="confirmPassword"
                    type="password"
                    value={passwordData.confirmPassword}
                    onChange={(event) => setPasswordData((prev) => ({ ...prev, confirmPassword: event.target.value }))}
                    required
                  />
                </div>

                <Button label="Finish activation" onClick={handleSetPassword} loading={processing} fullWidth />
              </>
            ) : null}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
