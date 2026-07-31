import { useState } from 'react'
import Swal from 'sweetalert2'
import { Button, Alert } from './ui'
import extensionService from '../services/extensionService'

export default function ExtensionPaymentModal({ extensionRequest, onClose, onSuccess }) {
  const [paymentMethod, setPaymentMethod] = useState('ONLINE')
  const [paymentReference, setPaymentReference] = useState('')
  const [transactionId, setTransactionId] = useState('')
  const [processing, setProcessing] = useState(false)
  const [error, setError] = useState('')

  const paymentAmount = extensionRequest?.totalAmount ?? extensionRequest?.paymentAmount ?? 0

  const handlePayment = async () => {
    if (!paymentReference.trim()) {
      setError('Please enter a payment reference.')
      return
    }

    const result = await Swal.fire({
      title: 'Confirm Payment',
      html: `
        <div style="text-align:left;font-size:15px;">
          <p style="margin-bottom:8px;"><strong>Extension Payment</strong></p>
          <p style="margin-bottom:8px;">Amount: <strong>₹${Number(paymentAmount).toLocaleString()}</strong></p>
          <p style="margin-bottom:8px;">Reference: <strong>${paymentReference}</strong></p>
          <p>Mode: <strong>${paymentMethod === 'CASH' ? '💵 Cash' : '💳 Online'}</strong></p>
        </div>
      `,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Yes, Proceed',
      cancelButtonText: 'Cancel',
      confirmButtonColor: '#0f172a',
      cancelButtonColor: '#94a3b8',
    })

    if (!result.isConfirmed) return

    await confirmPayment()
  }

  const confirmPayment = async () => {
    try {
      setProcessing(true)
      setError('')

      const paymentData = {
        paymentAmount: paymentAmount,
        paymentReference: paymentReference.trim(),
        paymentMethod: paymentMethod,
        transactionId: transactionId.trim() || null,
      }

      await extensionService.processExtensionPayment(extensionRequest.requestId, paymentData)

      await Swal.fire({
        title: 'Payment Successful! 🎉',
        html: `<p>Extension payment of <strong>₹${Number(paymentAmount).toLocaleString()}</strong> has been completed. Your new accommodation period is now active.</p>`,
        icon: 'success',
        confirmButtonText: 'Done',
        confirmButtonColor: '#0f172a',
        timer: 3000,
        timerProgressBar: true,
      })

      onSuccess?.()
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
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">PAYMENT</p>
            <h2 className="mt-1 text-2xl font-bold text-slate-950">Complete Extension Payment</h2>
            <p className="mt-1 text-sm text-slate-500">Activate your approved allotment extension</p>
          </div>
          <button
            onClick={onClose}
            className="rounded-full p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition"
            aria-label="Close modal"
          >
            ✕
          </button>
        </div>

        <div className="space-y-6">
          {error && <Alert tone="error">{error}</Alert>}

          {/* Extension Details */}
          <div className="rounded-2xl bg-slate-50 border border-slate-200 p-4 space-y-2">
            <p className="text-sm font-semibold text-slate-700">Extension Details</p>
            <div className="text-sm text-slate-600 space-y-1">
              {extensionRequest?.roomNumber && (
                <div className="flex justify-between">
                  <span>Room</span>
                  <span className="font-medium text-slate-900">{extensionRequest.roomNumber}</span>
                </div>
              )}
              {extensionRequest?.tenantName && (
                <div className="flex justify-between">
                  <span>Tenant</span>
                  <span className="font-medium text-slate-900">{extensionRequest.tenantName}</span>
                </div>
              )}
              {extensionRequest?.planName && (
                <div className="flex justify-between">
                  <span>Plan</span>
                  <span className="font-medium text-slate-900">{extensionRequest.planName}</span>
                </div>
              )}
              {extensionRequest?.extensionPeriod && (
                <div className="flex justify-between">
                  <span>Extension Period</span>
                  <span className="font-medium text-slate-900">{extensionRequest.extensionPeriod}</span>
                </div>
              )}
            </div>
          </div>

          {/* Payment Mode Selection */}
          <div className="grid gap-4 md:grid-cols-2">
            <button
              type="button"
              onClick={() => {
                setPaymentMethod('ONLINE')
                setError('')
              }}
              className={`rounded-3xl border p-5 text-left transition ${
                paymentMethod === 'ONLINE' ? 'border-sky-300 bg-sky-50' : 'border-slate-200 bg-white'
              }`}
            >
              <p className="text-lg font-semibold text-slate-950">Online payment</p>
              <p className="mt-2 text-sm text-slate-500">Bank transfer, UPI, or card</p>
            </button>
            <button
              type="button"
              onClick={() => {
                setPaymentMethod('CASH')
                setError('')
              }}
              className={`rounded-3xl border p-5 text-left transition ${
                paymentMethod === 'CASH' ? 'border-sky-300 bg-sky-50' : 'border-slate-200 bg-white'
              }`}
            >
              <p className="text-lg font-semibold text-slate-950">Cash payment</p>
              <p className="mt-2 text-sm text-slate-500">Pay in person to the owner</p>
            </button>
          </div>

          {/* Payment Reference */}
          <div className="space-y-2">
            <label htmlFor="paymentReference" className="block text-sm font-medium text-slate-700">
              Payment Reference <span className="text-red-500">*</span>
            </label>
            <input
              id="paymentReference"
              type="text"
              value={paymentReference}
              onChange={(e) => setPaymentReference(e.target.value)}
              placeholder={
                paymentMethod === 'CASH'
                  ? 'e.g., Receipt number or cash transaction note'
                  : 'e.g., UTR number, transaction ID, or reference code'
              }
              className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 shadow-sm outline-none transition placeholder:text-slate-400 focus:border-sky-400 focus:ring-4 focus:ring-sky-100"
            />
          </div>

          {/* Transaction ID (optional) */}
          <div className="space-y-2">
            <label htmlFor="transactionId" className="block text-sm font-medium text-slate-700">
              Transaction ID <span className="text-slate-400 font-normal">(Optional)</span>
            </label>
            <input
              id="transactionId"
              type="text"
              value={transactionId}
              onChange={(e) => setTransactionId(e.target.value)}
              placeholder="e.g., Razorpay / UPI transaction ID"
              className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 shadow-sm outline-none transition placeholder:text-slate-400 focus:border-sky-400 focus:ring-4 focus:ring-sky-100"
            />
          </div>

          {/* Payment Summary */}
          <div className="rounded-3xl bg-slate-950 p-5 text-white">
            <p className="text-sm font-semibold text-slate-300">Payment summary</p>
            <div className="mt-4 space-y-3 text-sm">
              <div className="flex justify-between">
                <span>Extension Payment</span>
                <span>₹{Number(paymentAmount).toLocaleString()}</span>
              </div>
              <div className="flex justify-between border-t border-white/10 pt-3 text-base font-semibold">
                <span>Total amount</span>
                <span>₹{Number(paymentAmount).toLocaleString()}</span>
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
