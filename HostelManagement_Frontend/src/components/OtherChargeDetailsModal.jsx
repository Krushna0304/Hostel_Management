import { useState } from 'react'
import { Button, Badge } from './ui'
import { otherChargeService } from '../services/otherChargeService'

export default function OtherChargeDetailsModal({ charge, onClose, onUpdate }) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [collectingPayment, setCollectingPayment] = useState(false)

  // Room (split) charges carry per-tenant shares with their own status.
  const isSplitCharge =
    charge.category === 'OTHER_CHARGE_ROOM' &&
    !charge.installmentEnabled &&
    Array.isArray(charge.roomTenants) &&
    charge.roomTenants.some((t) => t.paymentStatus)

  const handleCollectCashPayment = async (installmentId = null) => {
    const amount = installmentId ?
      charge.installments.find(i => i.installmentId === installmentId)?.remainingAmount :
      charge.remainingAmount

    if (!amount || amount <= 0) {
      setError('No amount to collect')
      return
    }

    try {
      setCollectingPayment(true)
      setError('')

      if (installmentId) {
        await otherChargeService.recordInstallmentCashPayment(installmentId, {
          amount: amount,
          notes: 'Cash payment collected by owner'
        })
      } else {
        await otherChargeService.recordCashPayment(charge.chargeId, {
          amount: amount,
          notes: 'Cash payment collected by owner'
        })
      }

      onUpdate()
      onClose()
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to record payment')
    } finally {
      setCollectingPayment(false)
    }
  }

  // Collect a single tenant's share of a room (split) charge.
  const handleCollectShare = async (tenant) => {
    try {
      setCollectingPayment(true)
      setError('')
      await otherChargeService.recordCashPayment(charge.chargeId, {
        tenantId: tenant.tenantId,
        amount: tenant.splitAmount,
        notes: 'Cash payment collected by owner'
      })
      onUpdate()
      onClose()
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to record payment')
    } finally {
      setCollectingPayment(false)
    }
  }

  const handleSendReminder = async () => {
    try {
      setLoading(true)
      setError('')
      
      await otherChargeService.sendPaymentReminder(charge.chargeId)
      // Show success message
      alert('Payment reminder sent successfully!')
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to send reminder')
    } finally {
      setLoading(false)
    }
  }

  const getStatusBadge = (status) => {
    const statusConfig = {
      PENDING: { color: 'yellow', text: 'Pending' },
      PARTIALLY_PAID: { color: 'blue', text: 'Partially Paid' },
      COMPLETED: { color: 'green', text: 'Completed' },
      OVERDUE: { color: 'red', text: 'Overdue' },
      CANCELLED: { color: 'gray', text: 'Cancelled' }
    }
    
    const config = statusConfig[status] || statusConfig.PENDING
    return <Badge color={config.color}>{config.text}</Badge>
  }

  const fmt = (amount) => `₹${(amount || 0).toLocaleString()}`

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl max-w-4xl w-full max-h-[90vh] overflow-y-auto">
        <div className="p-6 border-b border-slate-200">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-slate-900">{charge.chargeName}</h2>
              <div className="flex items-center gap-2 mt-1">
                {getStatusBadge(charge.paymentStatus)}
                <Badge color={charge.category === 'OTHER_CHARGE_TENANT' ? 'blue' : 'purple'}>
                  {charge.category === 'OTHER_CHARGE_TENANT' ? 'Tenant Specific' : 'Room Based'}
                </Badge>
                {charge.installmentEnabled && (
                  <Badge color="blue">Installments</Badge>
                )}
              </div>
            </div>
            <button
              onClick={onClose}
              className="text-slate-400 hover:text-slate-600 text-2xl"
            >
              ×
            </button>
          </div>
        </div>

        <div className="p-6 space-y-6">
          {error && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
              {error}
            </div>
          )}

          {/* Charge Details */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-4">
              <h3 className="font-medium text-slate-900">Charge Information</h3>
              
              <div className="space-y-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-slate-600">Amount:</span>
                  <span className="font-medium">{fmt(charge.amount)}</span>
                </div>
                
                {charge.paidAmount > 0 && (
                  <div className="flex justify-between text-green-600">
                    <span>Paid Amount:</span>
                    <span className="font-medium">{fmt(charge.paidAmount)}</span>
                  </div>
                )}
                
                {charge.paymentStatus !== 'COMPLETED' && (
                  <div className="flex justify-between text-orange-600">
                    <span>Remaining:</span>
                    <span className="font-medium">{fmt(charge.remainingAmount)}</span>
                  </div>
                )}
                
                <div className="flex justify-between">
                  <span className="text-slate-600">Due Date:</span>
                  <span className={`font-medium ${
                    charge.dueDate && new Date(charge.dueDate) < new Date() && charge.paymentStatus !== 'COMPLETED' ? 
                    'text-red-600' : 'text-slate-900'
                  }`}>
                    {charge.dueDate ? new Date(charge.dueDate).toLocaleDateString() : 'Not set'}
                  </span>
                </div>
                
                <div className="flex justify-between">
                  <span className="text-slate-600">Created:</span>
                  <span className="font-medium">{new Date(charge.createdAt).toLocaleDateString()}</span>
                </div>
              </div>

              {charge.description && (
                <div>
                  <h4 className="font-medium text-slate-900 mb-2">Description</h4>
                  <p className="text-sm text-slate-600">{charge.description}</p>
                </div>
              )}
            </div>

            <div className="space-y-4">
              <h3 className="font-medium text-slate-900">Target Information</h3>
              
              <div className="space-y-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-slate-600">Owner:</span>
                  <span className="font-medium">{charge.ownerName}</span>
                </div>
                
                <div className="flex justify-between">
                  <span className="text-slate-600">Hostel:</span>
                  <span className="font-medium">{charge.hostelName}</span>
                </div>
                
                {charge.category === 'OTHER_CHARGE_TENANT' && (
                  <div className="flex justify-between">
                    <span className="text-slate-600">Tenant:</span>
                    <span className="font-medium">{charge.tenantName}</span>
                  </div>
                )}
                
                {charge.category === 'OTHER_CHARGE_ROOM' && (
                  <>
                    <div className="flex justify-between">
                      <span className="text-slate-600">Room:</span>
                      <span className="font-medium">Room {charge.roomNumber}</span>
                    </div>
                    
                    {charge.roomTenants && charge.roomTenants.length > 0 && !isSplitCharge && (
                      <div>
                        <span className="text-slate-600">Current Tenants ({charge.roomTenants.length}):</span>
                        <div className="mt-1 space-y-1">
                          {charge.roomTenants.map((tenant, idx) => (
                            <div key={idx} className="flex justify-between text-xs">
                              <span>{tenant.tenantName}</span>
                              <span className="text-slate-500">{fmt(tenant.splitAmount)}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </>
                )}
              </div>
            </div>
          </div>

          {/* Installments Section */}
          {charge.installmentEnabled && charge.installments && (
            <div className="space-y-4">
              <h3 className="font-medium text-slate-900">Installment Details</h3>
              
              <div className="bg-slate-50 rounded-lg p-4">
                <div className="flex justify-between items-center mb-4">
                  <span className="text-sm text-slate-600">
                    Progress: {charge.installments.filter(i => i.paymentStatus === 'COMPLETED').length} / {charge.installments.length} completed
                  </span>
                  <span className="text-sm font-medium">
                    {fmt(charge.installmentAmount)} per installment
                  </span>
                </div>
                
                <div className="space-y-3">
                  {charge.installments.map((installment, idx) => (
                    <div key={idx} className="flex items-center justify-between p-3 bg-white rounded border">
                      <div className="flex items-center gap-3">
                        <div className={`w-3 h-3 rounded-full ${
                          installment.paymentStatus === 'COMPLETED' ? 'bg-green-500' :
                          installment.paymentStatus === 'PARTIALLY_PAID' ? 'bg-yellow-500' :
                          installment.isOverdue ? 'bg-red-500' : 'bg-slate-300'
                        }`} />
                        <div>
                          <div className="text-sm font-medium">
                            Installment {installment.installmentNumber}
                          </div>
                          <div className="text-xs text-slate-600">
                            Due: {new Date(installment.dueDate).toLocaleDateString()}
                          </div>
                        </div>
                      </div>
                      
                      <div className="flex items-center gap-3">
                        <div className="text-right">
                          <div className="text-sm font-medium">{fmt(installment.amount)}</div>
                          {installment.paymentStatus !== 'COMPLETED' && (
                            <div className="text-xs text-orange-600">
                              Remaining: {fmt(installment.remainingAmount)}
                            </div>
                          )}
                        </div>
                        
                        {installment.paymentStatus !== 'COMPLETED' && (
                          <Button
                            size="sm"
                            onClick={() => handleCollectCashPayment(installment.installmentId)}
                            disabled={collectingPayment}
                            className={installment.isOverdue ? 'bg-red-600 hover:bg-red-700' : ''}
                          >
                            {collectingPayment ? 'Processing...' : 'Collect Cash'}
                          </Button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* Per-tenant shares (room split charge) */}
          {isSplitCharge && (
            <div className="space-y-4">
              <h3 className="font-medium text-slate-900">Tenant Shares</h3>

              <div className="bg-slate-50 rounded-lg p-4">
                <div className="flex justify-between items-center mb-4">
                  <span className="text-sm text-slate-600">
                    Paid: {charge.roomTenants.filter(t => t.paymentStatus === 'COMPLETED').length} / {charge.roomTenants.length}
                  </span>
                  <span className="text-sm font-medium">{fmt(charge.amount)} total</span>
                </div>

                <div className="space-y-3">
                  {charge.roomTenants.map((tenant, idx) => {
                    const paid = tenant.paymentStatus === 'COMPLETED'
                    return (
                      <div key={idx} className="flex items-center justify-between p-3 bg-white rounded border">
                        <div className="flex items-center gap-3">
                          <div className={`w-3 h-3 rounded-full ${paid ? 'bg-green-500' : 'bg-slate-300'}`} />
                          <div>
                            <div className="text-sm font-medium">{tenant.tenantName}</div>
                            <div className="text-xs text-slate-600">
                              {paid
                                ? `Paid${tenant.paidAt ? ' on ' + new Date(tenant.paidAt).toLocaleDateString() : ''}`
                                : 'Pending'}
                            </div>
                          </div>
                        </div>

                        <div className="flex items-center gap-3">
                          <div className="text-sm font-medium">{fmt(tenant.splitAmount)}</div>
                          {!paid && (
                            <Button
                              size="sm"
                              onClick={() => handleCollectShare(tenant)}
                              disabled={collectingPayment}
                            >
                              {collectingPayment ? 'Processing...' : 'Collect Cash'}
                            </Button>
                          )}
                        </div>
                      </div>
                    )
                  })}
                </div>
              </div>
            </div>
          )}

          {/* Actions */}
          <div className="flex gap-3 pt-4 border-t border-slate-200">
            <Button
              variant="outline"
              onClick={onClose}
              className="flex-1"
            >
              Close
            </Button>
            
            {charge.paymentStatus !== 'COMPLETED' && (
              <>
                <Button
                  variant="outline"
                  onClick={handleSendReminder}
                  disabled={loading}
                >
                  {loading ? 'Sending...' : 'Send Reminder'}
                </Button>
                
                {!charge.installmentEnabled && !isSplitCharge && (
                  <Button
                    onClick={() => handleCollectCashPayment()}
                    disabled={collectingPayment}
                  >
                    {collectingPayment ? 'Processing...' : `Collect ${fmt(charge.remainingAmount)}`}
                  </Button>
                )}
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}