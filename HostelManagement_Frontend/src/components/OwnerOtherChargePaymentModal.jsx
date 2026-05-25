import { useState } from 'react'
import Swal from 'sweetalert2'
import { Button, Alert } from './ui'
import apiClient from '../services/apiClient'

export default function OwnerOtherChargePaymentModal({ charge, installment, onClose, onSuccess }) {
  const [processing, setProcessing] = useState(false)
  const [error, setError] = useState('')

  // Calculate payment amount
  const getPaymentAmount = () => {
    if (installment) {
      return installment.remainingAmount || installment.amount
    }
    return charge.remainingAmount || charge.amount
  }

  const paymentAmount = getPaymentAmount()

  const handlePayment = async (paymentMode) => {
    const modeLabel = paymentMode === 'CASH' ? '💵 Cash' : '💳 Online'
    
    // Determine what we're collecting payment for
    const itemLabel = installment 
      ? `Installment #${installment.installmentNumber} — ${charge.chargeName}`
      : charge.chargeName
    
    const tenantLabel = charge.category === 'OTHER_CHARGE_TENANT' 
      ? charge.tenantName 
      : `Room ${charge.roomNumber} tenants`

    // SweetAlert2 confirmation
    const result = await Swal.fire({
      title: 'Confirm Collection',
      html: `
        <div style="text-align:left;font-size:15px;">
          <p style="margin-bottom:8px;">Tenant: <strong>${tenantLabel}</strong></p>
          ${installment 
            ? `<p style="margin-bottom:8px;">Installment: <strong>#${installment.installmentNumber}</strong></p>
               <p style="margin-bottom:8px;">Charge: <strong>${charge.chargeName}</strong></p>`
            : `<p style="margin-bottom:8px;">Charge: <strong>${charge.chargeName}</strong></p>`
          }
          <p style="margin-bottom:8px;">Amount: <strong>₹${paymentAmount.toLocaleString()}</strong></p>
          <p>Mode: <strong>${modeLabel}</strong></p>
        </div>
      `,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Yes, Confirm',
      cancelButtonText: 'Cancel',
      confirmButtonColor: '#0f172a',
      cancelButtonColor: '#94a3b8',
    })

    if (!result.isConfirmed) return

    try {
      setProcessing(true)
      setError('')

      if (installment) {
        // Record installment payment
        const requestData = {
          amount: paymentAmount,
          paymentMode: paymentMode,
          notes: `${paymentMode === 'CASH' ? 'Cash' : 'Online'} payment collected by owner`
        }

        // For installments, we need to determine the tenant ID
        if (charge.category === 'OTHER_CHARGE_TENANT') {
          requestData.tenantId = charge.tenantId
        } else if (charge.category === 'OTHER_CHARGE_ROOM' && charge.roomTenants && charge.roomTenants.length > 0) {
          // For room-based charges, use the first tenant or the one specified in installment
          requestData.tenantId = installment.tenantId || charge.roomTenants[0].tenantId
        }

        await apiClient.post(`/owner/other-charges/installments/${installment.installmentId}/collect-cash`, requestData)
      } else {
        // Record full charge payment
        const requestData = {
          amount: paymentAmount,
          paymentMode: paymentMode,
          notes: `${paymentMode === 'CASH' ? 'Cash' : 'Online'} payment collected by owner`
        }

        // For room-based charges, we need to specify which tenant is paying
        if (charge.category === 'OTHER_CHARGE_ROOM' && charge.roomTenants && charge.roomTenants.length > 0) {
          // For simplicity, we'll use the first tenant, but in a real scenario
          // you might want to show a dropdown to select which tenant is paying
          requestData.tenantId = charge.roomTenants[0].tenantId
        }

        await apiClient.post(`/owner/other-charges/${charge.chargeId}/collect-cash`, requestData)
      }

      // Success confirmation
      await Swal.fire({
        title: 'Payment Collected! 🎉',
        html: `<p>${installment ? `Installment <strong>#${installment.installmentNumber}</strong>` : 'Charge'} of <strong>₹${paymentAmount.toLocaleString()}</strong> collected from <strong>${tenantLabel}</strong>.</p>`,
        icon: 'success',
        confirmButtonText: 'Done',
        confirmButtonColor: '#0f172a',
        timer: 3000,
        timerProgressBar: true,
      })

      onSuccess()
    } catch (err) {
      setError(err?.response?.data?.message || err?.response?.data || 'Failed to record payment')
    } finally {
      setProcessing(false)
    }
  }

  const fmt = (amount) => `₹${(amount || 0).toLocaleString()}`

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl max-w-md w-full p-6">
        <div className="text-center mb-6">
          <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <span className="text-2xl">₹</span>
          </div>
          
          <h2 className="text-xl font-semibold text-slate-900 mb-2">Collect Payment</h2>
          
          <p className="text-sm text-slate-600 mb-1">
            Confirm payment collection from{' '}
            <span className="font-medium">
              {charge.category === 'OTHER_CHARGE_TENANT' ? charge.tenantName : `Room ${charge.roomNumber} tenants`}
            </span>
          </p>
          
          {installment && (
            <p className="text-sm text-slate-600 mb-1">
              Installment #{installment.installmentNumber} — {charge.chargeName}
            </p>
          )}
          
          {!installment && (
            <p className="text-sm text-slate-600 mb-1">{charge.chargeName}</p>
          )}
          
          <div className="text-2xl font-bold text-slate-900 mt-3">
            Amount: {fmt(paymentAmount)}
          </div>
        </div>

        {error && (
          <Alert tone="error" className="mb-4">
            {error}
          </Alert>
        )}

        <div className="space-y-3 mb-6">
          <p className="text-sm text-slate-600 text-center">How did the tenant pay?</p>
          
          <Button
            label="💵 Received Cash"
            onClick={() => handlePayment('CASH')}
            loading={processing}
            fullWidth
            variant="success"
            className="bg-green-600 hover:bg-green-700 text-white"
          />
          
          <Button
            label="💳 Received Online"
            onClick={() => handlePayment('ONLINE')}
            loading={processing}
            fullWidth
            variant="primary"
            className="bg-slate-900 hover:bg-slate-800 text-white"
          />
        </div>

        <div className="flex gap-3">
          <Button
            label="Cancel"
            onClick={onClose}
            variant="secondary"
            fullWidth
            disabled={processing}
          />
        </div>

        <p className="text-xs text-slate-500 text-center mt-4">
          This will mark the {installment ? 'installment' : 'charge'} as paid and update tenant records.
        </p>
      </div>
    </div>
  )
}