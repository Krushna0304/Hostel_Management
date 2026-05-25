import { useState, useEffect } from 'react'
import Swal from 'sweetalert2'
import apiClient from '../services/apiClient'
import FormInput from './FormInput'
import { Alert, Button } from './ui'
import { loadRazorpay, preloadRazorpay } from '../utils/razorpayLoader'

export default function PaymentModal({ 
  scheduleId, 
  amount, 
  installmentNumber,
  onSuccess, 
  onClose 
}) {
  const [paymentMode, setPaymentMode] = useState('ONLINE')
  const [otp, setOtp] = useState('')
  const [otpSent, setOtpSent] = useState(false)
  const [otpMessage, setOtpMessage] = useState('')
  const [sendingOtp, setSendingOtp] = useState(false)
  const [processing, setProcessing] = useState(false)
  const [error, setError] = useState('')

  // Preload Razorpay when payment modal opens
  useEffect(() => {
    preloadRazorpay()
  }, [])

  const handleSendOtp = async () => {
    try {
      setSendingOtp(true)
      setError('')
      setOtpMessage('')
      
      const response = await apiClient.post(`/api/cash-payment-otp/send-installment/${scheduleId}`)
      setOtpSent(true)
      setOtpMessage(response.data.message || 'OTP sent to owner successfully')
    } catch (err) {
      const errorData = err?.response?.data
      setError(errorData?.message || 'Failed to send OTP. Please try again.')
    } finally {
      setSendingOtp(false)
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
    }

    // SweetAlert2 confirmation before proceeding
    const result = await Swal.fire({
      title: 'Confirm Payment',
      html: `
        <div style="text-align:left;font-size:15px;">
          <p style="margin-bottom:8px;"><strong>Installment #${installmentNumber}</strong></p>
          <p style="margin-bottom:8px;">Amount: <strong>₹${amount.toLocaleString()}</strong></p>
          <p>Mode: <strong>${paymentMode === 'CASH' ? '💵 Cash' : '💳 Online (Razorpay)'}</strong></p>
        </div>
      `,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Yes, Proceed',
      cancelButtonText: 'Cancel',
      confirmButtonColor: '#0f172a',
      cancelButtonColor: '#94a3b8',
      borderRadius: '16px',
    })

    if (!result.isConfirmed) return

    if (paymentMode === 'CASH') {
      await confirmPayment({ paymentMode: 'CASH', otp })
    } else {
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
      const orderRes = await apiClient.post('/api/payments/create-installment-order', {
        scheduleId,
        amount,
        currency: 'INR',
      })

      const { orderId, providerKey, amount: orderAmount, currency, provider } = orderRes.data

      // Step 3: If mock provider, skip checkout UI and confirm directly
      if (provider === 'mock') {
        await confirmPayment({
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
        amount: orderAmount,
        currency,
        name: 'Hostel Management',
        description: `Installment #${installmentNumber} Payment`,
        order_id: orderId,
        handler: async (response) => {
          // Step 5: Verify payment on backend then record payment
          await confirmPayment({
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
      const errorData = err?.response?.data
      if (err.message && err.message.includes('Razorpay')) {
        setError('Failed to load payment system. Please check your internet connection and try again.')
      } else {
        setError(errorData?.message || 'Failed to initiate payment. Please try again.')
      }
    }
  }

  const confirmPayment = async (paymentData) => {
    try {
      setProcessing(true)
      setError('')
      
      await apiClient.post(`/tenant/pay/${scheduleId}`, {
        amount,
        paymentMode: paymentData.paymentMode,
        otp: paymentData.otp,
        razorpayOrderId: paymentData.razorpayOrderId,
        razorpayPaymentId: paymentData.razorpayPaymentId,
        razorpaySignature: paymentData.razorpaySignature,
      })

      await Swal.fire({
        title: 'Payment Successful! 🎉',
        html: `<p>Installment <strong>#${installmentNumber}</strong> of <strong>₹${amount.toLocaleString()}</strong> has been paid successfully.</p>`,
        icon: 'success',
        confirmButtonText: 'Done',
        confirmButtonColor: '#0f172a',
        timer: 3000,
        timerProgressBar: true,
      })

      onSuccess()
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

  return (
    <div 
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4" 
      onClick={onClose}
    >
      <div
        className="relative w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-3xl bg-white p-6 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-start justify-between mb-5">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Payment</p>
            <h2 className="mt-1 text-2xl font-bold text-slate-950">Complete installment payment</h2>
            <p className="mt-1 text-sm text-slate-500">Installment #{installmentNumber}</p>
          </div>
          <button 
            onClick={onClose} 
            className="rounded-full p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition"
          >
            ✕
          </button>
        </div>

        <div className="space-y-6">
          {error && <Alert tone="error">{error}</Alert>}

          {/* Payment Mode Selection */}
          <div className="grid gap-4 md:grid-cols-2">
            <button
              type="button"
              onClick={() => {
                setPaymentMode('ONLINE')
                setOtpSent(false)
                setOtp('')
                setOtpMessage('')
                setError('')
              }}
              className={`rounded-3xl border p-5 text-left transition ${
                paymentMode === 'ONLINE' ? 'border-sky-300 bg-sky-50' : 'border-slate-200 bg-white'
              }`}
            >
              <p className="text-lg font-semibold text-slate-950">Online payment</p>
              <p className="mt-2 text-sm text-slate-500">Pay securely using Razorpay</p>
            </button>
            <button
              type="button"
              onClick={() => {
                setPaymentMode('CASH')
                setOtpSent(false)
                setOtp('')
                setOtpMessage('')
                setError('')
              }}
              className={`rounded-3xl border p-5 text-left transition ${
                paymentMode === 'CASH' ? 'border-sky-300 bg-sky-50' : 'border-slate-200 bg-white'
              }`}
            >
              <p className="text-lg font-semibold text-slate-950">Cash payment</p>
              <p className="mt-2 text-sm text-slate-500">Use OTP verification for cash payment</p>
            </button>
          </div>

          {/* Cash Payment OTP Flow */}
          {paymentMode === 'CASH' && (
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
          )}

          {/* Payment Summary */}
          <div className="rounded-3xl bg-slate-950 p-5 text-white">
            <p className="text-sm font-semibold text-slate-300">Payment summary</p>
            <div className="mt-4 space-y-3 text-sm">
              <div className="flex justify-between">
                <span>Installment #{installmentNumber}</span>
                <span>₹{amount.toLocaleString()}</span>
              </div>
              <div className="flex justify-between border-t border-white/10 pt-3 text-base font-semibold">
                <span>Total amount</span>
                <span>₹{amount.toLocaleString()}</span>
              </div>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex flex-col gap-3 sm:flex-row">
            <Button 
              label="Confirm payment" 
              onClick={handlePayment} 
              loading={processing} 
              fullWidth 
            />
            <Button 
              label="Cancel" 
              onClick={onClose} 
              variant="secondary" 
              fullWidth 
              disabled={processing}
            />
          </div>
        </div>
      </div>
    </div>
  )
}
