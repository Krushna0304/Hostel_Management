import { useState, useEffect } from 'react'
import { Alert, Button, Card, EmptyState, LoadingScreen } from './ui'
import FormInput from './FormInput'
import FormSelect from './FormSelect'
import electricityBillService from '../services/electricityBillService'
import { hostelService } from '../services/hostelService'
import Swal from 'sweetalert2'
import FillAmountModal from './FillAmountModal'

export default function ElectricityBillCreation() {
  const [accounts, setAccounts] = useState([])
  const [hostels, setHostels] = useState([])
  const [filteredAccounts, setFilteredAccounts] = useState([])
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState('')

  // Custom modal state
  const [modalConfig, setModalConfig] = useState({ isOpen: false, type: null })

  const [formData, setFormData] = useState({
    hostelFilter: '', // Filter by hostel
    billMonth: new Date().getMonth() + 1,
    billYear: new Date().getFullYear(),
    bills: []
  })

  useEffect(() => {
    loadData()
  }, [])

  useEffect(() => {
    // Filter accounts by selected hostel
    if (formData.hostelFilter) {
      const filtered = accounts.filter(account => 
        account.hostelId === formData.hostelFilter
      )
      setFilteredAccounts(filtered)
    } else {
      setFilteredAccounts(accounts)
    }
    
    // Update bills array when accounts change
    updateBillsArray()
  }, [formData.hostelFilter, accounts])

  const loadData = async () => {
    try {
      setLoading(true)
      const [accountsData, hostelsResponse] = await Promise.all([
        electricityBillService.getOwnerAccounts(),
        hostelService.getOwnerHostels()
      ])
      
      setAccounts(accountsData)
      setHostels(hostelsResponse.data || [])
      
    } catch (err) {
      setError('Failed to load accounts. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  const updateBillsArray = () => {
    const accountsToUse = formData.hostelFilter 
      ? accounts.filter(account => account.hostelId === formData.hostelFilter)
      : accounts
      
    setFormData(prev => ({
      ...prev,
      bills: accountsToUse.map(account => {
        const existingBill = prev.bills.find(bill => bill.accountId === account.accountId)
        return {
          accountId: account.accountId,
          amount: existingBill?.amount || '',
          roomNumber: account.roomNumber,
          accountNumber: account.accountNumber
        }
      })
    }))
  }

  const handleMonthYearChange = (e) => {
    const { name, value } = e.target
    setFormData(prev => ({
      ...prev,
      [name]: name === 'billMonth' || name === 'billYear' ? parseInt(value) : value
    }))
  }

  const handleBillChange = (accountId, field, value) => {
    setFormData(prev => ({
      ...prev,
      bills: prev.bills.map(bill =>
        bill.accountId === accountId
          ? { ...bill, [field]: value }
          : bill
      )
    }))
  }

  const handleCreateBills = async () => {
    // Validate that at least one bill has an amount
    const billsWithAmount = formData.bills.filter(bill => bill.amount && parseFloat(bill.amount) > 0)
    
    if (billsWithAmount.length === 0) {
      setError('Please enter amount for at least one electricity bill.')
      return
    }

    // Confirm before creating
    const result = await Swal.fire({
      title: 'Create Electricity Bills',
      html: `
        <div style="text-align:left;font-size:15px;">
          <p style="margin-bottom:8px;"><strong>Period:</strong> ${electricityBillService.formatBillPeriod(formData.billMonth, formData.billYear)}</p>
          <p style="margin-bottom:8px;"><strong>Bills to create:</strong> ${billsWithAmount.length}</p>
          <p><strong>Total amount:</strong> ₹${billsWithAmount.reduce((sum, bill) => sum + parseFloat(bill.amount), 0).toLocaleString()}</p>
        </div>
      `,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Create Bills',
      cancelButtonText: 'Cancel',
      confirmButtonColor: '#0f172a',
      cancelButtonColor: '#94a3b8',
    })

    if (!result.isConfirmed) return

    try {
      setCreating(true)
      setError('')

      const billsToCreate = {
        billMonth: formData.billMonth,
        billYear: formData.billYear,
        bills: billsWithAmount.map(bill => ({
          accountId: bill.accountId,
          amount: parseFloat(bill.amount),
          notes: null // Removed notes as requested
        }))
      }

      await electricityBillService.createElectricityBills(billsToCreate)

      await Swal.fire({
        title: 'Bills Created! ⚡',
        text: `${billsWithAmount.length} electricity bills have been created successfully.`,
        icon: 'success',
        confirmButtonText: 'Done',
        confirmButtonColor: '#0f172a',
        timer: 3000,
        timerProgressBar: true,
      })

      // Reset form
      setFormData(prev => ({
        ...prev,
        bills: prev.bills.map(bill => ({
          ...bill,
          amount: ''
        }))
      }))

    } catch (err) {
      const errorData = err?.response?.data
      setError(errorData?.message || 'Failed to create bills. Please try again.')
    } finally {
      setCreating(false)
    }
  }

  const getTotalAmount = () => {
    return formData.bills
      .filter(bill => bill.amount && parseFloat(bill.amount) > 0)
      .reduce((sum, bill) => sum + parseFloat(bill.amount), 0)
  }

  const getBillsWithAmount = () => {
    return formData.bills.filter(bill => bill.amount && parseFloat(bill.amount) > 0).length
  }

  const getHostelName = (hostelId) => {
    const hostel = hostels.find(h => h.hostelId === hostelId)
    return hostel ? hostel.hostelName : 'All Hostels'
  }

  // Generate month options
  const monthOptions = Array.from({ length: 12 }, (_, i) => ({
    value: i + 1,
    label: new Date(2024, i).toLocaleString('default', { month: 'long' })
  }))

  // Generate year options (current year ± 2 years)
  const currentYear = new Date().getFullYear()
  const yearOptions = Array.from({ length: 5 }, (_, i) => currentYear - 2 + i)

  if (loading) {
    return <LoadingScreen />
  }

  if (accounts.length === 0) {
    return (
      <EmptyState
        title="No electricity accounts found"
        description="You need to create electricity accounts first before creating bills."
        actionLabel="Manage Accounts"
        onAction={() => window.location.href = '/owner/electricity-accounts'}
      />
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-slate-950">Create Electricity Bills</h1>
        <p className="text-sm text-slate-600 mt-1">Generate electricity bills for all accounts</p>
      </div>

      {error && <Alert tone="error">{error}</Alert>}

      {/* Filters and Billing Period */}
      <Card>
        <div className="p-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
            {/* Hostel Filter */}
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Filter by Hostel
              </label>
              <select
                name="hostelFilter"
                value={formData.hostelFilter}
                onChange={handleMonthYearChange}
                className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-100"
              >
                <option value="">All Hostels ({accounts.length} accounts)</option>
                {hostels.map(hostel => {
                  const hostelAccountCount = accounts.filter(acc => acc.hostelId === hostel.hostelId).length
                  return (
                    <option key={hostel.hostelId} value={hostel.hostelId}>
                      {hostel.hostelName} ({hostelAccountCount} accounts)
                    </option>
                  )
                })}
              </select>
            </div>

            {/* Month Selection */}
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Month *
              </label>
              <select
                name="billMonth"
                value={formData.billMonth}
                onChange={handleMonthYearChange}
                required
                className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-100"
              >
                {monthOptions.map(month => (
                  <option key={month.value} value={month.value}>
                    {month.label}
                  </option>
                ))}
              </select>
            </div>

            {/* Year Selection */}
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Year *
              </label>
              <select
                name="billYear"
                value={formData.billYear}
                onChange={handleMonthYearChange}
                required
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

          <div className="text-sm text-slate-600">
            <strong>Selected:</strong> {electricityBillService.formatBillPeriod(formData.billMonth, formData.billYear)} • 
            <strong> Hostel:</strong> {getHostelName(formData.hostelFilter)} • 
            <strong> Accounts:</strong> {filteredAccounts.length}
          </div>
        </div>
      </Card>

      {/* Bills Entry Cards */}
      <Card>
        <div className="p-6">
          <div className="flex items-center justify-between mb-6">
            <h3 className="text-lg font-semibold text-slate-950">Enter Bill Amounts</h3>
            <div className="flex items-center gap-4">
              <div className="text-sm text-slate-600">
                {getBillsWithAmount()} of {formData.bills.length} bills • Total: ₹{getTotalAmount().toLocaleString()}
              </div>
              {formData.bills.length > 0 && (
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => setModalConfig({ isOpen: true, type: 'fillAll' })}
                    className="px-3 py-1 text-xs bg-blue-100 text-blue-700 rounded-lg hover:bg-blue-200 transition-colors"
                  >
                    Fill All
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setFormData(prev => ({
                        ...prev,
                        bills: prev.bills.map(bill => ({ ...bill, amount: '' }))
                      }))
                    }}
                    className="px-3 py-1 text-xs bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors"
                  >
                    Clear All
                  </button>
                </div>
              )}
            </div>
          </div>

          {formData.bills.length === 0 ? (
            <div className="text-center py-8 text-slate-500">
              No accounts found for the selected hostel.
            </div>
          ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {/* Search/Filter for large lists */}
            {formData.bills.length > 20 && (
              <div className="col-span-full mb-4">
                <input
                  type="text"
                  placeholder="Search by room number..."
                  className="w-full max-w-md rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-100"
                  onChange={(e) => {
                    const searchTerm = e.target.value.toLowerCase()
                    const cards = document.querySelectorAll('[data-room-card]')
                    cards.forEach(card => {
                      const roomNumber = card.getAttribute('data-room-number').toLowerCase()
                      card.style.display = roomNumber.includes(searchTerm) ? 'block' : 'none'
                    })
                  }}
                />
              </div>
            )}
            
            {formData.bills.map((bill, index) => (
              <div 
                key={bill.accountId} 
                data-room-card
                data-room-number={bill.roomNumber}
                className={`border rounded-xl p-4 transition-all duration-200 ${
                  bill.amount && parseFloat(bill.amount) > 0 
                    ? 'border-green-300 bg-green-50 shadow-sm' 
                    : 'border-slate-200 hover:border-slate-300'
                }`}
              >
                <div className="mb-3">
                  <div className="flex items-center justify-between">
                    <h4 className="font-semibold text-slate-950">Room {bill.roomNumber}</h4>
                    {bill.amount && parseFloat(bill.amount) > 0 && (
                      <div className="text-xs text-green-600 font-medium">
                        ✓
                      </div>
                    )}
                  </div>
                  <p className="text-xs text-slate-600">A/C: {bill.accountNumber}</p>
                </div>

                <div>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={bill.amount || ''}
                    onChange={(e) => handleBillChange(bill.accountId, 'amount', e.target.value)}
                    onKeyDown={(e) => {
                      // Enter key moves to next input
                      if (e.key === 'Enter') {
                        e.preventDefault()
                        const inputs = document.querySelectorAll('input[type="number"]')
                        const currentIndex = Array.from(inputs).indexOf(e.target)
                        const nextInput = inputs[currentIndex + 1]
                        if (nextInput) {
                          nextInput.focus()
                          nextInput.select()
                        }
                      }
                      // Tab key behavior (default)
                      // Escape clears current input
                      if (e.key === 'Escape') {
                        e.target.value = ''
                        handleBillChange(bill.accountId, 'amount', '')
                      }
                    }}
                    placeholder="Enter amount"
                    className={`w-full rounded-lg border px-3 py-2 text-sm focus:outline-none focus:ring-2 transition-colors ${
                      bill.amount && parseFloat(bill.amount) > 0
                        ? 'border-green-300 focus:border-green-400 focus:ring-green-100 bg-white'
                        : 'border-slate-200 focus:border-sky-300 focus:ring-sky-100'
                    }`}
                  />
                </div>

                {bill.amount && parseFloat(bill.amount) > 0 && (
                  <div className="mt-2 text-xs text-green-700 font-medium text-center">
                    ₹{parseFloat(bill.amount).toLocaleString()}
                  </div>
                )}
              </div>
            ))}
          </div>
          )}

          {/* Keyboard Shortcuts Guide */}
          {formData.bills.length > 5 && (
            <div className="mt-4 p-3 bg-slate-50 border border-slate-200 rounded-lg">
              <div className="text-xs text-slate-600">
                <strong>⌨️ Keyboard Shortcuts:</strong> 
                <span className="ml-2">Enter = Next field</span> • 
                <span className="ml-1">Tab = Next field</span> • 
                <span className="ml-1">Esc = Clear field</span> • 
                <span className="ml-1">Ctrl+A = Select all text</span>
              </div>
            </div>
          )}

          {/* Quick Actions for Bulk Operations */}
          {formData.bills.length > 10 && (
            <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
              <div className="flex items-center justify-between">
                <div>
                  <h4 className="text-sm font-medium text-blue-900">Bulk Operations</h4>
                  <p className="text-xs text-blue-700 mt-1">Manage multiple rooms efficiently</p>
                </div>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => setModalConfig({ isOpen: true, type: 'setByRooms' })}
                    className="px-3 py-1 text-xs bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                  >
                    Set by Rooms
                  </button>
                  <button
                    type="button"
                    onClick={() => setModalConfig({ isOpen: true, type: 'setByRange' })}
                    className="px-3 py-1 text-xs bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                  >
                    Set by Range
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </Card>

      {/* Summary and Action */}
      {getBillsWithAmount() > 0 && (
        <Card className="border-green-200">
          <div className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-lg font-semibold text-green-900">Bills Summary</h3>
                <p className="text-sm text-green-700 mt-1">
                  {getBillsWithAmount()} bills for {electricityBillService.formatBillPeriod(formData.billMonth, formData.billYear)}
                </p>
              </div>
              <div className="text-right">
                <div className="text-2xl font-bold text-green-900">₹{getTotalAmount().toLocaleString()}</div>
                <div className="text-sm text-green-700">Total Amount</div>
              </div>
            </div>

            <div className="mt-4">
              <Button
                label={`Create ${getBillsWithAmount()} Bills`}
                onClick={handleCreateBills}
                loading={creating}
                fullWidth
              />
            </div>
          </div>
        </Card>
      )}

      {/* Custom Fill Amount Modal */}
      <FillAmountModal
        isOpen={modalConfig.isOpen}
        title={
          modalConfig.type === 'fillAll' ? 'Fill All Rooms' :
          modalConfig.type === 'setByRooms' ? 'Set Amount by Rooms' :
          modalConfig.type === 'setByRange' ? 'Set Amount by Range' : ''
        }
        subtitle={
          modalConfig.type === 'fillAll' ? 'Enter a single amount to apply to all rooms at once.' :
          modalConfig.type === 'setByRooms' ? 'Enter comma-separated room numbers and the amount to set.' :
          modalConfig.type === 'setByRange' ? 'Enter a start room, end room, and amount for the range.' : ''
        }
        fields={
          modalConfig.type === 'fillAll' ? [
            { key: 'amount', label: 'Amount (₹)', placeholder: 'e.g. 500', type: 'number' }
          ] :
          modalConfig.type === 'setByRooms' ? [
            { key: 'rooms', label: 'Room Numbers', placeholder: 'e.g. 101, 102, 103', type: 'text' },
            { key: 'amount', label: 'Amount (₹)', placeholder: 'e.g. 500', type: 'number' }
          ] :
          modalConfig.type === 'setByRange' ? [
            { key: 'start', label: 'Start Room Number', placeholder: 'e.g. 101', type: 'text' },
            { key: 'end', label: 'End Room Number', placeholder: 'e.g. 110', type: 'text' },
            { key: 'amount', label: 'Amount (₹)', placeholder: 'e.g. 500', type: 'number' }
          ] : []
        }
        onCancel={() => setModalConfig({ isOpen: false, type: null })}
        onConfirm={(values) => {
          if (modalConfig.type === 'fillAll') {
            setFormData(prev => ({
              ...prev,
              bills: prev.bills.map(bill => ({ ...bill, amount: values.amount }))
            }))
          } else if (modalConfig.type === 'setByRooms') {
            const roomNumbers = values.rooms.split(',').map(r => r.trim())
            setFormData(prev => ({
              ...prev,
              bills: prev.bills.map(bill =>
                roomNumbers.includes(bill.roomNumber)
                  ? { ...bill, amount: values.amount }
                  : bill
              )
            }))
          } else if (modalConfig.type === 'setByRange') {
            setFormData(prev => ({
              ...prev,
              bills: prev.bills.map(bill => {
                const roomNum = parseInt(bill.roomNumber)
                const startNum = parseInt(values.start)
                const endNum = parseInt(values.end)
                return (roomNum >= startNum && roomNum <= endNum)
                  ? { ...bill, amount: values.amount }
                  : bill
              })
            }))
          }
          setModalConfig({ isOpen: false, type: null })
        }}
      />
    </div>
  )
}