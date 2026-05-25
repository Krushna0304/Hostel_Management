import { useState } from 'react'
import Swal from 'sweetalert2'
import { Button } from './ui'
import settlementService from '../services/settlementService'

export default function SettlementRefundCollectionModal({ 
  settlement, 
  onClose, 
  onSuccess 
}) {
  const [processing, setProcessing] = useState(false)
  const [error, setError] = useState('')

  const handleRefundCollection = async (mode) => {
    // SweetAlert2 confirmation before proceeding
    const result = await Swal.fire({
      title: 'Confirm Refund Collection?',
      html: `
        <div style="text-align:left;font-size:15px;">
          <p style="margin-bottom:8px;">Owner: <strong>${settlement.ownerName || 'Owner'}</strong></p>
          <p style="margin-bottom:8px;">Room: <strong>${settlement.roomNumber || 'N/A'}</strong></p>
          <p style="margin-bottom:8px;">Refund Amount: <strong>₹${Math.abs(settlement.finalSettlementAmount)?.toLocaleString()}</strong></p>
          <p>Mode: <strong>${mode === 'CASH' ? '💵 Received Cash' : '💳 Received Online'}</strong></p>
        </div>
      `,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Yes, Received',
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
        ? `REFUND_CASH_RECEIVED_${Date.now()}` 
        : `REFUND_ONLINE_RECEIVED_${Date.now()}`

      await settlementService.completeSettlement(settlement.settlementId, paymentReference)

      await Swal.fire({
        title: 'Refund Collected! 🎉',
        html: `<p>Refund of <strong>₹${Math.abs(settlement.finalSettlementAmount)?.toLocaleString()}</strong> received from owner.</p>`,
        icon: 'success',
        confirmButtonText: 'Done',
        confirmButtonColor: '#0f172a',
        timer: 3000,
        timerProgressBar: true,
      })

      onSuccess?.()
      onClose()
    } catch (err) {
      setError(err?.response?.data?.message || 'Refund collection confirmation failed.')
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
          <div className="mx-auto w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mb-4">
            <span className="text-2xl">💰</span>
          </div>
          <h2 className="text-2xl font-bold text-slate-950">Collect Refund</h2>
          <p className="text-slate-600 mt-1">Confirm refund collection from owner</p>
        </div>

        {error && (
          <div className="mb-4 rounded-2xl bg-red-50 border border-red-200 px-4 py-3 text-red-700">
            {error}
          </div>
        )}

        {/* Settlement Details */}
        <div className="mb-6 p-4 bg-slate-50 rounded-2xl">
          <div className="flex justify-between items-center mb-2">
            <span className="text-sm font-medium text-slate-600">Settlement Refund</span>
            <span className="text-lg font-bold text-slate-900">{settlement.roomNumber || 'N/A'}</span>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-lg font-semibold text-slate-900">Refund Amount:</span>
            <span className="text-2xl font-bold text-green-600">₹{Math.abs(settlement.finalSettlementAmount)?.toLocaleString()}</span>
          </div>
        </div>

        {/* Payment Method Question */}
        <div className="mb-6">
          <h3 className="text-lg font-semibold text-slate-900 mb-4 text-center">How did you receive the refund?</h3>
          
          <div className="space-y-3">
            <button
              onClick={() => handleRefundCollection('CASH')}
              disabled={processing}
              className="w-full p-4 rounded-2xl bg-green-500 hover:bg-green-600 text-white font-semibold transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <div className="flex items-center justify-center gap-3">
                <span className="text-xl">💵</span>
                <span>Received Cash</span>
              </div>
            </button>
            
            <button
              onClick={() => handleRefundCollection('ONLINE')}
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
            This will mark the settlement as completed and close the agreement.
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