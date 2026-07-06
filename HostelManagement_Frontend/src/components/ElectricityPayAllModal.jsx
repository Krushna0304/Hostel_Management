import { useState } from 'react'
import Swal from 'sweetalert2'
import FormInput from './FormInput'
import { Alert, Button } from './ui'
import electricityBillService from '../services/electricityBillService'

/**
 * Pays every outstanding electricity bill for the tenant. Supports online (mock) or
 * cash via a single shared OTP — one OTP authorises all the bills, and each bill is
 * settled with that same OTP.
 */
export default function ElectricityPayAllModal({ bills, onSuccess, onClose }) {
  const outstanding = bills.filter((b) => Number(b.remainingAmount) > 0)
  const total = outstanding.reduce((sum, b) => sum + Number(b.remainingAmount), 0)

  const [paymentMode, setPaymentMode] = useState('ONLINE')
  const [otp, setOtp] = useState('')
  const [otpSent, setOtpSent] = useState(false)
  const [otpMessage, setOtpMessage] = useState('')
  const [sendingOtp, setSendingOtp] = useState(false)
  const [processing, setProcessing] = useState(false)
  const [error, setError] = useState('')

  const handleSendOtp = async () => {
    try {
      setSendingOtp(true)
      setError('')
      setOtpMessage('')
      const res = await electricityBillService.sendPayAllOtp()
      setOtpSent(true)
      setOtpMessage(res?.message || 'OTP sent to owner successfully')
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to send OTP. Please try again.')
    } finally {
      setSendingOtp(false)
    }
  }

  const handlePayAll = async () => {
    if (outstanding.length === 0) return

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

    const result = await Swal.fire({
      title: 'Pay All Bills',
      html: `
        <div style="text-align:left;font-size:15px;">
          <p style="margin-bottom:8px;">You are about to pay <strong>${outstanding.length}</strong> outstanding electricity bill(s).</p>
          <p style="margin-bottom:8px;">Total amount: <strong>${electricityBillService.formatCurrency(total)}</strong></p>
          <p>Mode: <strong>${paymentMode === 'CASH' ? '💵 Cash' : '💳 Online (mock for demo)'}</strong></p>
        </div>
      `,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Yes, Pay All',
      cancelButtonText: 'Cancel',
      confirmButtonColor: '#0f172a',
      cancelButtonColor: '#94a3b8',
    })
    if (!result.isConfirmed) return

    try {
      setProcessing(true)
      setError('')
      for (const bill of outstanding) {
        const payload = {
          billId: bill.billId,
          amount: Number(bill.remainingAmount),
          paymentMode,
        }
        if (paymentMode === 'CASH') {
          payload.otp = otp.trim() // same OTP for every bill
        } else {
          payload.razorpayOrderId = 'mock_order_' + Date.now()
          payload.razorpayPaymentId = 'mock_pay_' + Date.now()
          payload.razorpaySignature = 'mock_signature'
        }
        await electricityBillService.recordPayment(payload)
      }

      await Swal.fire({
        title: 'Payment Successful! ⚡',
        html: `<p>Paid <strong>${electricityBillService.formatCurrency(total)}</strong> across ${outstanding.length} bill(s).</p>`,
        icon: 'success',
        confirmButtonText: 'Done',
        confirmButtonColor: '#0f172a',
        timer: 3000,
        timerProgressBar: true,
      })
      onSuccess()
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to pay all bills. Please try again.')
    } finally {
      setProcessing(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4" onClick={onClose}>
      <div
        className="relative w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-3xl bg-white p-6 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-start justify-between mb-5">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Electricity Payment</p>
            <h2 className="mt-1 text-2xl font-bold text-slate-950">Pay All Bills</h2>
            <p className="mt-1 text-sm text-slate-500">
              {outstanding.length} bill(s) • {electricityBillService.formatCurrency(total)}
            </p>
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

          {/* Bills list */}
          <div className="rounded-2xl bg-slate-50 border border-slate-200 p-4 space-y-2">
            {outstanding.map((b) => (
              <div key={b.billId} className="flex items-center justify-between text-sm">
                <span className="text-slate-600">{b.billPeriod} • Room {b.roomNumber}</span>
                <span className="font-semibold text-slate-900">
                  {electricityBillService.formatCurrency(b.remainingAmount)}
                </span>
              </div>
            ))}
            <div className="flex items-center justify-between border-t border-slate-200 pt-2 text-base font-semibold">
              <span>Total</span>
              <span>{electricityBillService.formatCurrency(total)}</span>
            </div>
          </div>

          {/* Payment mode */}
          <div className="grid gap-4 md:grid-cols-2">
            <button
              type="button"
              onClick={() => { setPaymentMode('ONLINE'); setOtpSent(false); setOtp(''); setOtpMessage(''); setError('') }}
              className={`rounded-3xl border p-5 text-left transition ${
                paymentMode === 'ONLINE' ? 'border-sky-300 bg-sky-50' : 'border-slate-200 bg-white'
              }`}
            >
              <p className="text-lg font-semibold text-slate-950">Online payment</p>
              <p className="mt-2 text-sm text-slate-500">Pay securely (Mock for demo)</p>
            </button>
            <button
              type="button"
              onClick={() => { setPaymentMode('CASH'); setError('') }}
              className={`rounded-3xl border p-5 text-left transition ${
                paymentMode === 'CASH' ? 'border-sky-300 bg-sky-50' : 'border-slate-200 bg-white'
              }`}
            >
              <p className="text-lg font-semibold text-slate-950">Cash payment</p>
              <p className="mt-2 text-sm text-slate-500">One OTP covers all bills</p>
            </button>
          </div>

          {/* Cash OTP flow */}
          {paymentMode === 'CASH' && (
            <div className="space-y-4">
              <div className="rounded-2xl bg-amber-50 border border-amber-200 p-4">
                <p className="text-sm font-semibold text-amber-900 mb-2">Cash Payment Process:</p>
                <ol className="text-sm text-amber-800 space-y-1 list-decimal list-inside">
                  <li>Click "Send OTP to Owner" — one OTP is generated for all bills</li>
                  <li>Hand over the total cash to the owner and collect the OTP</li>
                  <li>Enter the OTP and confirm — every bill is paid with the same OTP</li>
                </ol>
              </div>

              {!otpSent ? (
                <Button label="Send OTP to Owner" onClick={handleSendOtp} loading={sendingOtp} fullWidth variant="secondary" />
              ) : (
                <Alert tone="success" title="OTP Sent">{otpMessage}</Alert>
              )}

              {otpSent && (
                <FormInput
                  label="Enter OTP from Owner"
                  name="otp"
                  value={otp}
                  onChange={(e) => setOtp(e.target.value)}
                  placeholder="Enter 6-digit OTP"
                  required
                  maxLength={6}
                />
              )}
            </div>
          )}

          {/* Actions */}
          <div className="flex flex-col gap-3 sm:flex-row">
            <Button label="Confirm payment" onClick={handlePayAll} loading={processing} fullWidth />
            <Button label="Cancel" onClick={onClose} variant="secondary" fullWidth disabled={processing} />
          </div>
        </div>
      </div>
    </div>
  )
}
