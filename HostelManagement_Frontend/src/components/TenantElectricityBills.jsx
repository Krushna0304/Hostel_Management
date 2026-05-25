import { useState, useEffect } from 'react'
import { Alert, Card, EmptyState, LoadingScreen, Button } from './ui'
import electricityBillService from '../services/electricityBillService'
import ElectricityPaymentModal from './ElectricityPaymentModal'
import ElectricityPaymentHistory from './ElectricityPaymentHistory'

export default function TenantElectricityBills() {
  const [bills, setBills] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedBill, setSelectedBill] = useState(null)
  const [showPaymentModal, setShowPaymentModal] = useState(false)
  const [showPaymentHistory, setShowPaymentHistory] = useState(false)

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

  const groupBillsByPeriod = () => {
    const grouped = {}
    bills.forEach(bill => {
      const key = `${bill.billYear}-${bill.billMonth.toString().padStart(2, '0')}`
      if (!grouped[key]) {
        grouped[key] = {
          period: bill.billPeriod,
          bills: []
        }
      }
      grouped[key].bills.push(bill)
    })
    
    // Sort by period (newest first)
    return Object.entries(grouped)
      .sort(([a], [b]) => b.localeCompare(a))
      .map(([key, data]) => data)
  }

  const getTotalStats = () => {
    const totalAmount = bills.reduce((sum, bill) => sum + Number(bill.totalAmount), 0)
    const paidAmount = bills.reduce((sum, bill) => sum + Number(bill.paidAmount), 0)
    const remainingAmount = bills.reduce((sum, bill) => sum + Number(bill.remainingAmount), 0)
    
    return { totalAmount, paidAmount, remainingAmount }
  }

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

  const groupedBills = groupBillsByPeriod()
  const stats = getTotalStats()

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-slate-950">My Electricity Bills</h1>
        <p className="text-sm text-slate-600 mt-1">View and pay your electricity bills</p>
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

      {/* Bills by Period */}
      {groupedBills.map((group, groupIndex) => (
        <div key={groupIndex} className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-slate-950">{group.period}</h2>
            <div className="text-sm text-slate-600">
              {group.bills.length} bills
            </div>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-3">
            {group.bills.map(bill => (
              <Card key={bill.billId} className="hover:shadow-lg transition-shadow">
                <div className="p-3">
                  {/* Header */}
                  <div className="flex items-start justify-between mb-2">
                    <div className="min-w-0 flex-1 mr-2">
                      <h3 className="text-sm font-semibold text-slate-950 truncate">Room {bill.roomNumber}</h3>
                      <p className="text-xs text-slate-500 truncate">Acc: {bill.accountNumber}</p>
                    </div>
                    <div className={`px-1.5 py-0.5 rounded-full text-[10px] font-medium border whitespace-nowrap ${
                      electricityBillService.getBillStatusColor(bill.status)
                    }`}>
                      {electricityBillService.getBillStatusIcon(bill.status)} {bill.status}
                    </div>
                  </div>

                  {/* Amount */}
                  <div className="flex items-baseline justify-between text-xs mb-2">
                    <span className="text-slate-500">Amount:</span>
                    <span className="font-semibold text-slate-900">
                      {electricityBillService.formatCurrency(bill.totalAmount)}
                    </span>
                  </div>

                  {/* Due Date */}
                  {bill.dueDate && (
                    <div className="mb-2 text-[10px] text-amber-700">
                      Due: {new Date(bill.dueDate).toLocaleDateString()}
                    </div>
                  )}

                  {/* Action Buttons */}
                  <div className="space-y-1.5 pt-2 border-t border-slate-100">
                    {Number(bill.remainingAmount) > 0 && (
                      <Button
                        label={`Pay ${electricityBillService.formatCurrency(bill.remainingAmount)}`}
                        onClick={() => handlePayBill(bill)}
                        fullWidth
                        size="sm"
                      />
                    )}
                    <Button
                      label="View History"
                      onClick={() => handleViewHistory(bill)}
                      variant="secondary"
                      fullWidth
                      size="sm"
                    />
                  </div>
                </div>
              </Card>
            ))}
          </div>
        </div>
      ))}

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
    </div>
  )
}