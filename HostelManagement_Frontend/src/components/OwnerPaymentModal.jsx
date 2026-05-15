import { useState, useEffect } from 'react'
import { ownerReportService } from '../services/agreementService'
import { Button, Badge } from './ui'

export default function OwnerPaymentModal({ tenant, onClose, onSuccess }) {
  const [installments, setInstallments] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedInstallment, setSelectedInstallment] = useState(null)
  const [paymentAmount, setPaymentAmount] = useState('')
  const [paymentMode, setPaymentMode] = useState('CASH')
  const [otp, setOtp] = useState('')
  const [processing, setProcessing] = useState(false)
  const [otpSent, setOtpSent] = useState(false)
  const [sendingOtp, setSendingOtp] = useState(false)

  useEffect(() => {
    if (tenant?.tenantId) {
      fetchInstallments()
    }
  }, [tenant])

  const fetchInstallments = async () => {
    try {
      setLoading(true)
      setError('')
      console.log('Fetching installments for tenant:', tenant.tenantId)
      const res = await ownerReportService.getTenantInstallments(tenant.tenantId)
      console.log('Installments response:', res.data)
      setInstallments(res.data)
    } catch (err) {
      console.error('Error fetching installments:', err)
      setError(err?.response?.data?.message || err?.response?.data || 'Failed to load installments.')
    } finally {
      setLoading(false)
    }
  }

  const handleInstallmentSelect = (installment) => {
    setSelectedInstallment(installment)
    const totalDue = installment.amount + installment.lateFeeApplied - installment.paidAmount
    setPaymentAmount(totalDue.toString())
  }

  const handleSendOtp = async () => {
    if (!selectedInstallment) return

    try {
      setSendingOtp(true)
      setError('')
      
      await ownerReportService.sendInstallmentOtp(selectedInstallment.scheduleId)
      setOtpSent(true)
    } catch (err) {
      setError('Failed to send OTP. Please try again.')
    } finally {
      setSendingOtp(false)
    }
  }

  const handlePaymentSubmit = async (e) => {
    e.preventDefault()
    if (!selectedInstallment || !paymentAmount) return

    try {
      setProcessing(true)
      setError('')

      const paymentData = {
        amount: parseInt(paymentAmount),
        paymentMode: paymentMode,
        otp: paymentMode === 'CASH' ? otp : null,
      }

      await ownerReportService.collectPayment(selectedInstallment.scheduleId, paymentData)
      
      onSuccess?.()
      onClose()
    } catch (err) {
      setError(err?.response?.data?.message || 'Payment collection failed.')
    } finally {
      setProcessing(false)
    }
  }

  const formatCurrency = (amount) => `₹${amount?.toLocaleString()}`
  const formatDate = (date) => date ? new Date(date).toLocaleDateString() : 'N/A'

  const statusVariant = (status) => {
    switch (status) {
      case 'COMPLETED': return 'success'
      case 'OVERDUE': return 'danger'
      case 'PARTIALLY_PAID': return 'warning'
      case 'SCHEDULED': return 'neutral'
      default: return 'neutral'
    }
  }

  if (!tenant) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4" onClick={onClose}>
      <div
        className="relative w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-3xl bg-white p-6 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-start justify-between mb-5">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">Collect Payment</p>
            <h2 className="mt-1 text-2xl font-bold text-slate-950">{tenant.tenantName}</h2>
            <p className="mt-1 text-sm text-slate-600">{tenant.hostelName} · Room {tenant.roomNumber}</p>
          </div>
          <button 
            onClick={onClose} 
            className="rounded-full p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition"
          >
            ✕
          </button>
        </div>

        {error && (
          <div className="mb-4 rounded-2xl bg-red-50 border border-red-200 px-4 py-3 text-red-700">
            {error}
          </div>
        )}

        {loading ? (
          <div className="flex items-center justify-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          </div>
        ) : (
          <div className="space-y-6">
            {/* Installments List */}
            <div>
              <h3 className="text-lg font-semibold text-slate-900 mb-3">Pending Installments</h3>
              {installments.length === 0 ? (
                <div className="text-center py-8 text-slate-500">
                  <p>No pending installments found.</p>
                </div>
              ) : (
                <div className="space-y-2">
                  {installments.map((installment) => {
                    const totalDue = installment.amount + installment.lateFeeApplied - installment.paidAmount
                    const isSelected = selectedInstallment?.scheduleId === installment.scheduleId
                    const isCompleted = installment.paymentStatus === 'COMPLETED'
                    
                    return (
                      <div
                        key={installment.scheduleId}
                        className={`rounded-2xl border p-4 transition ${
                          isCompleted 
                            ? 'border-green-200 bg-green-50 opacity-60' 
                            : isSelected 
                              ? 'border-blue-300 bg-blue-50 cursor-pointer' 
                              : 'border-slate-200 hover:border-slate-300 cursor-pointer'
                        }`}
                        onClick={() => !isCompleted && handleInstallmentSelect(installment)}
                      >
                        <div className="flex items-center justify-between">
                          <div>
                            <div className="flex items-center gap-3">
                              <span className="font-semibold text-slate-900">
                                Installment #{installment.installmentNumber}
                              </span>
                              <Badge variant={statusVariant(installment.paymentStatus)}>
                                {installment.paymentStatus?.replace('_', ' ')}
                              </Badge>
                              {isCompleted && (
                                <span className="text-xs text-green-600">(Already Paid)</span>
                              )}
                            </div>
                            <p className="text-sm text-slate-600 mt-1">
                              Due: {formatDate(installment.dueDate)}
                            </p>
                          </div>
                          <div className="text-right">
                            <p className="font-bold text-slate-900">
                              {isCompleted ? formatCurrency(installment.paidAmount) : formatCurrency(totalDue)}
                            </p>
                            {installment.lateFeeApplied > 0 && !isCompleted && (
                              <p className="text-sm text-red-600">
                                +{formatCurrency(installment.lateFeeApplied)} late fee
                              </p>
                            )}
                          </div>
                        </div>
                      </div>
                    )
                  })}
                </div>
              )}
            </div>

            {/* Payment Form */}
            {selectedInstallment && (
              <form onSubmit={handlePaymentSubmit} className="space-y-4">
                <div className="rounded-2xl bg-slate-50 p-4">
                  <h4 className="font-semibold text-slate-900 mb-3">Payment Details</h4>
                  
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-2">
                        Payment Amount
                      </label>
                      <input
                        type="number"
                        value={paymentAmount}
                        onChange={(e) => setPaymentAmount(e.target.value)}
                        className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-blue-300 focus:outline-none focus:ring-4 focus:ring-blue-100"
                        required
                        min="1"
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-slate-700 mb-2">
                        Payment Mode
                      </label>
                      <select
                        value={paymentMode}
                        onChange={(e) => setPaymentMode(e.target.value)}
                        className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-blue-300 focus:outline-none focus:ring-4 focus:ring-blue-100"
                      >
                        <option value="CASH">Cash</option>
                        <option value="ONLINE">Online</option>
                      </select>
                    </div>
                  </div>

                  {paymentMode === 'CASH' && (
                    <div className="mt-4 space-y-3">
                      <div className="flex items-center gap-3">
                        <label className="block text-sm font-medium text-slate-700">
                          OTP (Required for cash payments)
                        </label>
                        <Button
                          type="button"
                          label={sendingOtp ? "Sending..." : otpSent ? "OTP Sent" : "Send OTP"}
                          variant="secondary"
                          size="sm"
                          onClick={handleSendOtp}
                          disabled={sendingOtp || !selectedInstallment}
                        />
                      </div>
                      <input
                        type="text"
                        value={otp}
                        onChange={(e) => setOtp(e.target.value)}
                        className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-blue-300 focus:outline-none focus:ring-4 focus:ring-blue-100"
                        placeholder="Enter OTP"
                        required
                      />
                      {otpSent && (
                        <p className="text-sm text-green-600">
                          ✓ OTP sent successfully. Please enter the OTP above.
                        </p>
                      )}
                    </div>
                  )}
                </div>

                <div className="flex gap-3">
                  <Button
                    type="submit"
                    label={processing ? "Processing..." : "Collect Payment"}
                    disabled={processing || !selectedInstallment || !paymentAmount}
                    fullWidth
                  />
                  <Button
                    type="button"
                    label="Cancel"
                    variant="secondary"
                    onClick={onClose}
                    disabled={processing}
                    fullWidth
                  />
                </div>
              </form>
            )}
          </div>
        )}
      </div>
    </div>
  )
}