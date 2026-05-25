import { Badge, Button } from './ui'
import electricityBillService from '../services/electricityBillService'

export default function ElectricityPaymentHistory({ bill, onClose }) {
  if (!bill) return null

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString()
  }

  const getPaymentModeIcon = (mode) => {
    switch (mode) {
      case 'ONLINE':
        return '💳'
      case 'CASH':
        return '💵'
      default:
        return '💰'
    }
  }

  const getPaymentStatusColor = (status) => {
    switch (status) {
      case 'COMPLETED':
        return 'success'
      case 'PENDING':
        return 'warning'
      case 'FAILED':
        return 'error'
      default:
        return 'neutral'
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4" onClick={onClose}>
      <div
        className="relative w-full max-w-4xl max-h-[95vh] overflow-y-auto rounded-3xl bg-white shadow-2xl"
        onClick={e => e.stopPropagation()}
      >
        {/* Header */}
        <div className="sticky top-0 z-10 flex items-center justify-between bg-white px-6 py-4 border-b border-slate-100 rounded-t-3xl">
          <div>
            <h2 className="text-xl font-bold text-slate-950">
              Electricity Bill - Room {bill.roomNumber}
            </h2>
            <p className="text-sm text-slate-600 mt-1">
              {bill.billPeriod} • Account: {bill.accountNumber}
            </p>
          </div>
          <button 
            onClick={onClose} 
            className="rounded-full p-2 text-slate-400 hover:bg-slate-100 transition"
          >
            ✕
          </button>
        </div>

        <div className="px-6 py-6 space-y-6">
          {/* Bill Summary */}
          <div className="rounded-2xl border border-slate-200">
            <div className="px-4 py-3 bg-slate-50 rounded-t-2xl border-b border-slate-200">
              <h3 className="text-sm font-semibold text-slate-900">Bill Summary</h3>
            </div>
            <div className="px-4 py-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="text-center">
                  <div className="text-2xl font-bold text-slate-950">
                    {electricityBillService.formatCurrency(bill.totalAmount)}
                  </div>
                  <div className="text-sm text-slate-600">This Month Bill</div>
                </div>
                <div className="text-center">
                  <div className={`text-2xl font-bold ${
                    Number(bill.totalRemainingForRoom || bill.remainingAmount) > 0 ? 'text-red-600' : 'text-green-700'
                  }`}>
                    {electricityBillService.formatCurrency(bill.totalRemainingForRoom || bill.remainingAmount)}
                  </div>
                  <div className="text-sm text-slate-600">Total Remaining</div>
                </div>
              </div>

              <div className="mt-4 pt-4 border-t border-slate-200 grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                <div className="flex justify-between">
                  <span className="text-slate-600">Status:</span>
                  <Badge variant={getPaymentStatusColor(bill.status)}>
                    {electricityBillService.getBillStatusIcon(bill.status)} {bill.status}
                  </Badge>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-600">Tenant:</span>
                  <span className="font-medium text-slate-900">{bill.tenantName || 'No Tenant'}</span>
                </div>
                {bill.tenantPhone && (
                  <div className="flex justify-between">
                    <span className="text-slate-600">Mobile:</span>
                    <a href={`tel:${bill.tenantPhone}`} className="font-medium text-blue-600 hover:text-blue-800 transition-colors">
                      {bill.tenantPhone}
                    </a>
                  </div>
                )}
                {bill.dueDate && (
                  <div className="flex justify-between">
                    <span className="text-slate-600">Due Date:</span>
                    <span className="font-medium text-slate-900">
                      {new Date(bill.dueDate).toLocaleDateString()}
                    </span>
                  </div>
                )}
                <div className="flex justify-between">
                  <span className="text-slate-600">Created:</span>
                  <span className="font-medium text-slate-900">
                    {new Date(bill.createdAt).toLocaleDateString()}
                  </span>
                </div>
              </div>

              {bill.notes && (
                <div className="mt-4 p-3 bg-amber-50 border border-amber-200 rounded-lg">
                  <div className="text-sm font-medium text-amber-900 mb-1">Notes:</div>
                  <div className="text-sm text-amber-800">{bill.notes}</div>
                </div>
              )}
            </div>
          </div>

          {/* Payment History */}
          <div className="rounded-2xl border border-slate-200">
            <div className="px-4 py-3 bg-slate-50 rounded-t-2xl border-b border-slate-200">
              <h3 className="text-sm font-semibold text-slate-900">Payment History</h3>
            </div>
            <div className="px-4 py-4">
              {!bill.payments || bill.payments.length === 0 ? (
                <div className="text-center py-8">
                  <div className="text-slate-400 text-4xl mb-2">💳</div>
                  <div className="text-slate-600">No payments recorded yet</div>
                  <div className="text-sm text-slate-500 mt-1">
                    Payments will appear here once the tenant makes them
                  </div>
                </div>
              ) : (
                <div className="space-y-4">
                  {bill.payments.map((payment, index) => (
                    <div key={payment.paymentId} className="border border-slate-200 rounded-xl p-4">
                      <div className="flex items-start justify-between mb-3">
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="text-lg">
                              {getPaymentModeIcon(payment.paymentMode)}
                            </span>
                            <span className="font-semibold text-slate-950">
                              {electricityBillService.formatCurrency(payment.amount)}
                            </span>
                            <Badge variant={getPaymentStatusColor(payment.status)}>
                              {payment.status}
                            </Badge>
                          </div>
                          <div className="text-sm text-slate-600 mt-1">
                            Payment #{index + 1} • {payment.paymentMode}
                          </div>
                        </div>
                        <div className="text-right text-sm text-slate-600">
                          {formatDate(payment.createdAt)}
                        </div>
                      </div>

                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                        <div className="flex justify-between">
                          <span className="text-slate-600">Tenant:</span>
                          <span className="font-medium text-slate-900">{payment.tenantName}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-slate-600">Mode:</span>
                          <span className="font-medium text-slate-900">
                            {getPaymentModeIcon(payment.paymentMode)} {payment.paymentMode}
                          </span>
                        </div>
                        {payment.paymentReference && (
                          <div className="flex justify-between">
                            <span className="text-slate-600">Reference:</span>
                            <span className="font-medium text-slate-900 font-mono text-xs">
                              {payment.paymentReference}
                            </span>
                          </div>
                        )}
                        {payment.razorpayPaymentId && (
                          <div className="flex justify-between">
                            <span className="text-slate-600">Razorpay ID:</span>
                            <span className="font-medium text-slate-900 font-mono text-xs">
                              {payment.razorpayPaymentId}
                            </span>
                          </div>
                        )}
                      </div>

                      {payment.notes && (
                        <div className="mt-3 p-2 bg-slate-50 rounded-lg">
                          <div className="text-xs text-slate-600">{payment.notes}</div>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="sticky bottom-0 bg-white border-t border-slate-100 px-6 py-4 rounded-b-3xl">
          <Button 
            label="Close" 
            onClick={onClose} 
            variant="secondary"
            fullWidth
          />
        </div>
      </div>
    </div>
  )
}