import { useState } from 'react'
import { ownerReportService } from '../services/agreementService'
import { Button } from './ui'

export default function QuickPaymentConfirmation({ tenant, nextInstallment, onClose, onSuccess }) {
  const [processing, setProcessing] = useState(false)
  const [error, setError] = useState('')

  const handlePaymentConfirm = async (paymentMode) => {
    if (!nextInstallment) return

    try {
      setProcessing(true)
      setError('')

      const paymentData = {
        amount: nextInstallment.amount + nextInstallment.lateFeeApplied - nextInstallment.paidAmount,
        paymentMode: paymentMode,
        // For cash payments, we'll skip OTP for owner collections (trusted source)
        otp: paymentMode === 'CASH' ? 'OWNER_COLLECTION' : null,
      }

      await ownerReportService.collectPayment(nextInstallment.scheduleId, paymentData)
      
      onSuccess?.()
      onClose()
    } catch (err) {
      setError(err?.response?.data?.message || 'Payment collection failed.')
    } finally {
      setProcessing(false)
    }
  }

  const formatCurrency = (amount) => `₹${amount?.toLocaleString()}`

  if (!tenant || !nextInstallment) return null

  const totalAmount = nextInstallment.amount + nextInstallment.lateFeeApplied - nextInstallment.paidAmount

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4" onClick={onClose}>
      <div
        className="relative w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="text-center mb-6">
          <div className="mx-auto w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mb-4">
            <svg className="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1" />
            </svg>
          </div>
          <h2 className="text-xl font-bold text-slate-950 mb-2">Collect Payment</h2>
          <p className="text-sm text-slate-600">
            Confirm payment collection from <strong>{tenant.tenantName}</strong>
          </p>
        </div>

        {/* Payment Details */}
        <div className="bg-slate-50 rounded-2xl p-4 mb-6">
          <div className="flex justify-between items-center mb-2">
            <span className="text-sm text-slate-600">Installment #{nextInstallment.installmentNumber}</span>
            <span className="text-sm text-slate-500">
              {tenant.hostelName} · Room {tenant.roomNumber}
            </span>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-lg font-bold text-slate-900">Amount:</span>
            <span className="text-xl font-bold text-blue-600">{formatCurrency(totalAmount)}</span>
          </div>
          {nextInstallment.lateFeeApplied > 0 && (
            <div className="flex justify-between items-center mt-1">
              <span className="text-xs text-red-600">Includes late fee:</span>
              <span className="text-xs text-red-600">{formatCurrency(nextInstallment.lateFeeApplied)}</span>
            </div>
          )}
        </div>

        {error && (
          <div className="mb-4 rounded-2xl bg-red-50 border border-red-200 px-4 py-3 text-red-700 text-sm">
            {error}
          </div>
        )}

        {/* Action Buttons */}
        <div className="space-y-3">
          <p className="text-sm text-slate-600 text-center mb-4">
            How did the tenant pay?
          </p>
          
          <Button
            label={processing ? "Processing..." : "💵 Received Cash"}
            onClick={() => handlePaymentConfirm('CASH')}
            disabled={processing}
            fullWidth
            variant="success"
          />
          
          <Button
            label={processing ? "Processing..." : "💳 Received Online"}
            onClick={() => handlePaymentConfirm('ONLINE')}
            disabled={processing}
            fullWidth
            variant="primary"
          />
          
          <Button
            label="Cancel"
            onClick={onClose}
            disabled={processing}
            fullWidth
            variant="secondary"
          />
        </div>

        {/* Info Note */}
        <div className="mt-4 text-xs text-slate-500 text-center">
          <p>This will mark the installment as paid and update tenant records.</p>
        </div>
      </div>
    </div>
  )
}