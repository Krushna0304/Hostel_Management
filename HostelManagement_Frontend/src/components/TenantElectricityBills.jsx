import { useState, useEffect } from 'react'
import { Alert, Card, EmptyState, LoadingScreen, Button } from './ui'
import electricityBillService from '../services/electricityBillService'
import ElectricityPaymentModal from './ElectricityPaymentModal'
import ElectricityPaymentHistory from './ElectricityPaymentHistory'
import ElectricityPayAllModal from './ElectricityPayAllModal'

export default function TenantElectricityBills() {
  const [bills, setBills] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedBill, setSelectedBill] = useState(null)
  const [showPaymentModal, setShowPaymentModal] = useState(false)
  const [showPaymentHistory, setShowPaymentHistory] = useState(false)
  const [showPayAll, setShowPayAll] = useState(false)

  useEffect(() => {
    loadBills()
  }, [])

  const loadBills = async () => {
    try {
      setLoading(true)
      const billsData = await electricityBillService.getTenantBills()
      setBills(billsData)
    } catch (err) {
      setError('Failed to load bills. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  const handlePayBill = async (bill) => {
    try {
      const billDetails = await electricityBillService.getBillDetails(bill.billId)
      setSelectedBill(billDetails)
      setShowPaymentModal(true)
    } catch (err) {
      setError('Failed to load bill details. Please try again.')
    }
  }

  const handleViewHistory = async (bill) => {
    try {
      const billDetails = await electricityBillService.getBillDetails(bill.billId)
      setSelectedBill(billDetails)
      setShowPaymentHistory(true)
    } catch (err) {
      setError('Failed to load payment history. Please try again.')
    }
  }

  const handlePaymentSuccess = () => {
    setShowPaymentModal(false)
    setSelectedBill(null)
    loadBills() // Refresh bills after payment
  }

  const getOutstandingBills = () => bills.filter(bill => Number(bill.remainingAmount) > 0)

  const handlePayAllSuccess = () => {
    setShowPayAll(false)
    loadBills()
  }

  const getTotalStats = () => {
    const totalAmount = bills.reduce((sum, bill) => sum + Number(bill.totalAmount), 0)
    const paidAmount = bills.reduce((sum, bill) => sum + Number(bill.paidAmount), 0)
    const remainingAmount = bills.reduce((sum, bill) => sum + Number(bill.remainingAmount), 0)

    return { totalAmount, paidAmount, remainingAmount }
  }

  // Sort bills newest first by period
  const getSortedBills = () =>
    [...bills].sort((a, b) => {
      const keyA = `${a.billYear}-${a.billMonth.toString().padStart(2, '0')}`
      const keyB = `${b.billYear}-${b.billMonth.toString().padStart(2, '0')}`
      return keyB.localeCompare(keyA)
    })

  if (loading) {
    return <LoadingScreen />
  }

  if (bills.length === 0) {
    return (
      <EmptyState
        title="No electricity bills found"
        description="Your electricity bills will appear here once they are created by the owner."
        icon="⚡"
      />
    )
  }

  const stats = getTotalStats()
  const sortedBills = getSortedBills()
  const outstandingCount = getOutstandingBills().length

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-950">My Electricity Bills</h1>
          <p className="text-sm text-slate-600 mt-1">View and pay your electricity bills</p>
        </div>
        {outstandingCount > 0 && (
          <Button
            label={`Pay All (${electricityBillService.formatCurrency(stats.remainingAmount)})`}
            onClick={() => setShowPayAll(true)}
          />
        )}
      </div>

      {error && <Alert tone="error">{error}</Alert>}

      {/* Summary Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <div className="p-6 text-center">
            <div className="text-2xl font-bold text-slate-950">
              {electricityBillService.formatCurrency(stats.totalAmount)}
            </div>
            <div className="text-sm text-slate-600">Total Billed</div>
          </div>
        </Card>
        <Card>
          <div className="p-6 text-center">
            <div className="text-2xl font-bold text-green-700">
              {electricityBillService.formatCurrency(stats.paidAmount)}
            </div>
            <div className="text-sm text-slate-600">Total Paid</div>
          </div>
        </Card>
        <Card>
          <div className="p-6 text-center">
            <div className={`text-2xl font-bold ${
              stats.remainingAmount > 0 ? 'text-red-600' : 'text-green-700'
            }`}>
              {electricityBillService.formatCurrency(stats.remainingAmount)}
            </div>
            <div className="text-sm text-slate-600">Outstanding</div>
          </div>
        </Card>
      </div>

      {/* Bills Table */}
      <Card>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-left text-xs uppercase tracking-wider text-slate-500">
                <th className="px-4 py-3 font-medium">Period</th>
                <th className="px-4 py-3 font-medium">Room</th>
                <th className="px-4 py-3 font-medium">Account</th>
                <th className="px-4 py-3 font-medium text-right">Amount</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium">Due Date</th>
                <th className="px-4 py-3 font-medium text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {sortedBills.map(bill => (
                <tr key={bill.billId} className="border-b border-slate-100 last:border-0 hover:bg-slate-50">
                  <td className="px-4 py-3 font-medium text-slate-900">{bill.billPeriod}</td>
                  <td className="px-4 py-3 text-slate-700">Room {bill.roomNumber}</td>
                  <td className="px-4 py-3 text-slate-500">{bill.accountNumber}</td>
                  <td className="px-4 py-3 text-right font-semibold text-slate-900">
                    {electricityBillService.formatCurrency(bill.totalAmount)}
                  </td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded-full text-[11px] font-medium border whitespace-nowrap ${
                      electricityBillService.getBillStatusColor(bill.status)
                    }`}>
                      {electricityBillService.getBillStatusIcon(bill.status)} {bill.status}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-slate-600">
                    {bill.dueDate ? new Date(bill.dueDate).toLocaleDateString() : '-'}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center justify-end gap-2">
                      {Number(bill.remainingAmount) > 0 && (
                        <Button
                          label={`Pay ${electricityBillService.formatCurrency(bill.remainingAmount)}`}
                          onClick={() => handlePayBill(bill)}
                          size="sm"
                        />
                      )}
                      <Button
                        label="View History"
                        onClick={() => handleViewHistory(bill)}
                        variant="secondary"
                        size="sm"
                      />
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Payment Modal */}
      {showPaymentModal && selectedBill && (
        <ElectricityPaymentModal
          bill={selectedBill}
          onSuccess={handlePaymentSuccess}
          onClose={() => {
            setShowPaymentModal(false)
            setSelectedBill(null)
          }}
        />
      )}

      {/* Payment History Modal */}
      {showPaymentHistory && selectedBill && (
        <ElectricityPaymentHistory
          bill={selectedBill}
          onClose={() => {
            setShowPaymentHistory(false)
            setSelectedBill(null)
          }}
        />
      )}

      {/* Pay All Modal */}
      {showPayAll && (
        <ElectricityPayAllModal
          bills={bills}
          onSuccess={handlePayAllSuccess}
          onClose={() => setShowPayAll(false)}
        />
      )}
    </div>
  )
}
