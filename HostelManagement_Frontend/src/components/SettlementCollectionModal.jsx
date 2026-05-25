import { useState } from 'react'
import Swal from 'sweetalert2'
import { Button } from './ui'
import settlementService from '../services/settlementService'

export default function SettlementCollectionModal({ 
  settlement, 
  onClose, 
  onSuccess 
}) {
  const [paymentMode, setPaymentMode] = useState('CASH')
  const [processing, setProcessing] = useState(false)
  const [error, setError] = useState('')

  const handlePaymentCollection = async (mode) => {
    // SweetAlert2 confirmation before proceeding
    const result = await Swal.fire({
      title: 'Collect Payment?',
      html: `
        <div style="text-align:left;font-size:15px;">
          <p style="margin-bottom:8px;">Tenant: <strong>${settlement.tenantName}</strong></p>
          <p style="margin-bottom:8px;">Room: <strong>${settlement.roomNumber || 'N/A'}</strong></p>
          <p style="margin-bottom:8px;">Amount: <strong>₹${Math.abs(settlement.finalSettlementAmount)?.toLocaleString()}</strong></p>
          <p>Mode: <strong>${mode === 'CASH' ? '💵 Received Cash' : '💳 Received Online'}</strong></p>
        </div>
      `,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Yes, Collect',
      cancelButtonText: 'Cancel',
      confirmButtonColor: '#0f172a',
      cancelButtonColor: '#94a3b8',
    })

    if (!result.isConfirmed) return

    try {
      setProcessing(true)
      setError('')

      // Generate payment reference based on mode
      const paymentReference = mode === 'CASH' 
        ? `CASH_COLLECTED_${Date.now()}` 
        : `ONLINE_RECEIVED_${Date.now()}`

      await settlementService.completeSettlement(settlement.settlementId, paymentReference)

      await Swal.fire({
        title: 'Payment Collected! 🎉',
        html: `<p>Settlement payment of <strong>₹${Math.abs(settlement.finalSettlementAmount)?.toLocaleString()}</strong> collected from <strong>${settlement.tenantName}</strong>.</p>`,
        icon: 'success',
        confirmButtonText: 'Done',
        confirmButtonColor: '#0f172a',
        timer: 3000,
        timerProgressBar: true,
      })

      onSuccess?.()
      onClose()
    } catch (err) {
      setError(err?.response?.data?.message || 'Payment collection failed.')
    } finally {
      setProcessing(false)
    }
  }

  if (!settlement) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4" onClick={onClose}>
      <div
        className="relative w-full max-w-lg rounded-3xl bg-white p-6 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="text-center mb-6">
          <div className="mx-auto w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mb-4">
            <span className="text-2xl">💰</span>
          </div>
          <h2 className="text-2xl font-bold text-slate-950">Collect Payment</h2>
          <p className="text-slate-600 mt-1">Confirm payment collection from {settlement.tenantName}</p>
        </div>

        {error && (
          <div className="mb-4 rounded-2xl bg-red-50 border border-red-200 px-4 py-3 text-red-700">
            {error}
          </div>
        )}

        {/* Settlement Details */}
        <div className="mb-6 p-4 bg-slate-50 rounded-2xl">
          <div className="flex justify-between items-center mb-2">
            <span className="text-sm font-medium text-slate-600">Settlement Payment</span>
            <span className="text-lg font-bold text-slate-900">{settlement.roomNumber || 'N/A'}</span>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-lg font-semibold text-slate-900">Amount:</span>
            <span className="text-2xl font-bold text-blue-600">₹{Math.abs(settlement.finalSettlementAmount)?.toLocaleString()}</span>
          </div>
        </div>

        {/* Payment Method Question */}
        <div className="mb-6">
          <h3 className="text-lg font-semibold text-slate-900 mb-4 text-center">How did the tenant pay?</h3>
          
          <div className="space-y-3">
            <button
              onClick={() => handlePaymentCollection('CASH')}
              disabled={processing}
              className="w-full p-4 rounded-2xl bg-green-500 hover:bg-green-600 text-white font-semibold transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <div className="flex items-center justify-center gap-3">
                <span className="text-xl">💵</span>
                <span>Received Cash</span>
              </div>
            </button>
            
            <button
              onClick={() => handlePaymentCollection('ONLINE')}
              disabled={processing}
              className="w-full p-4 rounded-2xl bg-slate-900 hover:bg-slate-800 text-white font-semibold transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <div className="flex items-center justify-center gap-3">
                <span className="text-xl">💳</span>
                <span>Received Online</span>
              </div>
            </button>
          </div>
        </div>

        {/* Footer Note */}
        <div className="text-center">
          <p className="text-sm text-slate-500 mb-4">
            This will mark the settlement as completed and update tenant records.
          </p>
          
          <Button
            label="Cancel"
            variant="secondary"
            onClick={onClose}
            disabled={processing}
            fullWidth
          />
        </div>
      </div>
    </div>
  )
}