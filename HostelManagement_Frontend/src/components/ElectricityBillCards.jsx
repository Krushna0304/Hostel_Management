import { useState, useEffect } from 'react'
import { Alert, Card, EmptyState, LoadingScreen } from './ui'
import electricityBillService from '../services/electricityBillService'
import { hostelService } from '../services/hostelService'
import ElectricityPaymentHistory from './ElectricityPaymentHistory'

export default function ElectricityBillCards() {
  const [bills, setBills] = useState([])
  const [hostels, setHostels] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedBill, setSelectedBill] = useState(null)
  const [showPaymentHistory, setShowPaymentHistory] = useState(false)

  // Filters — default to current month/year
  const [hostelFilter, setHostelFilter] = useState('')
  const [monthFilter, setMonthFilter] = useState(new Date().getMonth() + 1)
  const [yearFilter, setYearFilter] = useState(new Date().getFullYear())

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      setLoading(true)
      const [billsData, hostelsResponse] = await Promise.all([
        electricityBillService.getOwnerBills(),
        hostelService.getOwnerHostels()
      ])
      setBills(billsData)
      setHostels(hostelsResponse.data || [])
    } catch (err) {
      setError('Failed to load bills. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  const handleViewPaymentHistory = async (bill) => {
    try {
      const billDetails = await electricityBillService.getBillDetails(bill.billId)
      setSelectedBill(billDetails)
      setShowPaymentHistory(true)
    } catch (err) {
      setError('Failed to load payment history. Please try again.')
    }
  }

  // Apply filters
  const getFilteredBills = () => {
    return bills.filter(bill => {
      // Hostel filter
      if (hostelFilter && bill.hostelId !== hostelFilter) return false
      // Month filter
      if (monthFilter && bill.billMonth !== monthFilter) return false
      // Year filter
      if (yearFilter && bill.billYear !== yearFilter) return false
      return true
    })
  }

  const groupBillsByPeriod = (filteredBills) => {
    const grouped = {}
    filteredBills.forEach(bill => {
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

  // Month options
  const monthOptions = Array.from({ length: 12 }, (_, i) => ({
    value: i + 1,
    label: new Date(2024, i).toLocaleString('default', { month: 'long' })
  }))

  // Year options (current year ± 2)
  const currentYear = new Date().getFullYear()
  const yearOptions = Array.from({ length: 5 }, (_, i) => currentYear - 2 + i)

  if (loading) {
    return <LoadingScreen />
  }

  if (bills.length === 0) {
    return (
      <EmptyState
        title="No electricity bills found"
        description="Create electricity bills to start tracking payments and usage."
        actionLabel="Create Bills"
        onAction={() => window.location.href = '/owner/electricity-bills/create'}
      />
    )
  }

  const filteredBills = getFilteredBills()
  const groupedBills = groupBillsByPeriod(filteredBills)

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-slate-950">Electricity Bills</h1>
        <p className="text-sm text-slate-600 mt-1">Manage electricity bills and track payments</p>
      </div>

      {error && <Alert tone="error">{error}</Alert>}

      {/* Filters */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Hostel Filter */}
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">Filter by Hostel</label>
          <select
            value={hostelFilter}
            onChange={(e) => setHostelFilter(e.target.value)}
            className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-100"
          >
            <option value="">All Hostels</option>
            {hostels.map(hostel => (
              <option key={hostel.hostelId} value={hostel.hostelId}>
                {hostel.hostelName}
              </option>
            ))}
          </select>
        </div>

        {/* Month Filter */}
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">Month</label>
          <select
            value={monthFilter}
            onChange={(e) => setMonthFilter(parseInt(e.target.value))}
            className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-100"
          >
            {monthOptions.map(month => (
              <option key={month.value} value={month.value}>
                {month.label}
              </option>
            ))}
          </select>
        </div>

        {/* Year Filter */}
        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">Year</label>
          <select
            value={yearFilter}
            onChange={(e) => setYearFilter(parseInt(e.target.value))}
            className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-100"
          >
            {yearOptions.map(year => (
              <option key={year} value={year}>
                {year}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Bills by Period */}
      {filteredBills.length === 0 ? (
        <div className="text-center py-12 text-slate-500">
          <div className="text-4xl mb-3">📭</div>
          <div className="font-medium">No bills found for the selected filters</div>
          <div className="text-sm mt-1">Try changing the month, year, or hostel filter</div>
        </div>
      ) : (
        groupedBills.map((group, groupIndex) => (
          <div key={groupIndex} className="space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-slate-950">{group.period}</h2>
              <div className="text-sm text-slate-600">
                {group.bills.length} bills • Total: {electricityBillService.formatCurrency(
                  group.bills.reduce((sum, bill) => sum + Number(bill.totalAmount), 0)
                )}
              </div>
            </div>

            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-3">
              {group.bills.map(bill => (
                <Card 
                  key={bill.billId} 
                  className="hover:shadow-lg transition-shadow cursor-pointer"
                  onClick={() => handleViewPaymentHistory(bill)}
                >
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

                    {/* Tenant Info */}
                    <div className="mb-2 text-xs text-slate-600 truncate">
                      {bill.tenantName || 'No Tenant'}
                    </div>

                    {/* Amount */}
                    <div className="flex items-baseline justify-between text-xs">
                      <span className="text-slate-500">Amount:</span>
                      <span className="font-semibold text-slate-900">
                        {electricityBillService.formatCurrency(bill.totalAmount)}
                      </span>
                    </div>

                    {/* Due Date */}
                    {bill.dueDate && (
                      <div className="mt-2 pt-2 border-t border-slate-100">
                        <div className="text-[10px] text-slate-400">
                          Due: {new Date(bill.dueDate).toLocaleDateString()}
                        </div>
                      </div>
                    )}
                  </div>
                </Card>
              ))}
            </div>
          </div>
        ))
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