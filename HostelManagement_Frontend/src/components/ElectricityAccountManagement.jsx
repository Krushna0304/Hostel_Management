import { useState, useEffect } from 'react'
import { Alert, Button, Card, EmptyState, LoadingScreen } from './ui'
import FormInput from './FormInput'
import FormSelect from './FormSelect'
import electricityBillService from '../services/electricityBillService'
import { hostelService } from '../services/hostelService'
import Swal from 'sweetalert2'

export default function ElectricityAccountManagement() {
  const [accounts, setAccounts] = useState([])
  const [hostels, setHostels] = useState([])
  const [rooms, setRooms] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadingRooms, setLoadingRooms] = useState(false)
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState('')
  const [hostelFilter, setHostelFilter] = useState('')

  const [formData, setFormData] = useState({
    hostelId: '',
    roomId: '',
    accountNumber: ''
  })

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      setLoading(true)
      const [accountsData, hostelsResponse] = await Promise.all([
        electricityBillService.getOwnerAccounts(),
        hostelService.getOwnerHostels()
      ])
      
      console.log('Hostels response:', hostelsResponse)
      console.log('Hostels data:', hostelsResponse.data)
      
      setAccounts(accountsData)
      setHostels(hostelsResponse.data || [])
      
      // Auto-select first hostel if only one exists
      if (hostelsResponse.data && hostelsResponse.data.length === 1) {
        setFormData(prev => ({ ...prev, hostelId: hostelsResponse.data[0].hostelId }))
        loadRooms(hostelsResponse.data[0].hostelId)
      }
    } catch (err) {
      console.error('Error loading data:', err)
      setError('Failed to load data. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  const loadRooms = async (hostelId) => {
    if (!hostelId) {
      setRooms([])
      return
    }

    try {
      setLoadingRooms(true)
      const response = await hostelService.getHostelRooms(hostelId)
      
      console.log('Rooms response for hostel', hostelId, ':', response)
      console.log('Rooms data:', response.data)
      
      setRooms(response.data || [])
    } catch (err) {
      console.error('Error loading rooms:', err)
      setError('Failed to load rooms. Please try again.')
      setRooms([])
    } finally {
      setLoadingRooms(false)
    }
  }

  const handleInputChange = (e) => {
    const { name, value } = e.target
    setFormData(prev => ({
      ...prev,
      [name]: value
    }))

    // Load rooms when hostel is selected
    if (name === 'hostelId') {
      setFormData(prev => ({ ...prev, roomId: '' })) // Reset room selection
      loadRooms(value)
    }
  }

  const handleCreateAccount = async (e) => {
    e.preventDefault()
    
    if (!formData.hostelId || !formData.roomId || !formData.accountNumber.trim()) {
      setError('Please fill in all required fields.')
      return
    }

    try {
      setCreating(true)
      setError('')
      
      await electricityBillService.createElectricityAccount({
        roomId: formData.roomId,
        accountNumber: formData.accountNumber
      })
      
      await Swal.fire({
        title: 'Account Created! ⚡',
        text: 'Electricity account has been created successfully.',
        icon: 'success',
        confirmButtonText: 'Done',
        confirmButtonColor: '#0f172a',
        timer: 3000,
        timerProgressBar: true,
      })

      setFormData({ hostelId: '', roomId: '', accountNumber: '' })
      setRooms([])
      setShowCreateForm(false)
      await loadData()
    } catch (err) {
      const errorData = err?.response?.data
      setError(errorData?.message || 'Failed to create account. Please try again.')
    } finally {
      setCreating(false)
    }
  }

  // Get available rooms (rooms without electricity accounts)
  const getAvailableRooms = () => {
    const accountRoomIds = accounts.map(account => account.roomId)
    return rooms.filter(room => !accountRoomIds.includes(room.roomId))
  }

  // Get hostel name by ID
  const getHostelName = (hostelId) => {
    const hostel = hostels.find(h => h.hostelId === hostelId)
    return hostel ? hostel.hostelName : 'Unknown Hostel'
  }

  // Filter accounts by hostel
  const getFilteredAccounts = () => {
    if (!hostelFilter) return accounts
    return accounts.filter(account => account.hostelId === hostelFilter)
  }

  if (loading) {
    return <LoadingScreen />
  }

  const filteredAccounts = getFilteredAccounts()

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-950">Electricity Account Management</h1>
          <p className="text-sm text-slate-600 mt-1">Create and manage electricity accounts for your rooms</p>
        </div>
        <Button
          label="Create Account"
          onClick={() => setShowCreateForm(true)}
          disabled={hostels.length === 0}
        />
      </div>

      {error && <Alert tone="error">{error}</Alert>}

      {/* Hostel Filter */}
      {hostels.length > 1 && (
        <div className="max-w-xs">
          <label className="block text-sm font-medium text-slate-700 mb-1">Filter by Hostel</label>
          <select
            value={hostelFilter}
            onChange={(e) => setHostelFilter(e.target.value)}
            className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-100"
          >
            <option value="">All Hostels ({accounts.length} accounts)</option>
            {hostels.map(hostel => {
              const count = accounts.filter(a => a.hostelId === hostel.hostelId).length
              return (
                <option key={hostel.hostelId} value={hostel.hostelId}>
                  {hostel.hostelName} ({count} accounts)
                </option>
              )
            })}
          </select>
        </div>
      )}

      {/* Create Account Form */}
      {showCreateForm && (
        <Card className="border-blue-200">
          <div className="p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-slate-950">Create New Electricity Account</h3>
              <button
                onClick={() => {
                  setShowCreateForm(false)
                  setFormData({ hostelId: '', roomId: '', accountNumber: '' })
                  setRooms([])
                  setError('')
                }}
                className="text-slate-400 hover:text-slate-600"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateAccount} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Select Hostel *
                  </label>
                  <select
                    name="hostelId"
                    value={formData.hostelId}
                    onChange={handleInputChange}
                    required
                    className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-100"
                  >
                    <option value="">Choose a hostel</option>
                    {hostels.map(hostel => {
                      console.log('Rendering hostel:', hostel)
                      return (
                        <option key={hostel.hostelId} value={hostel.hostelId}>
                          {hostel.hostelName}
                        </option>
                      )
                    })}
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Select Room *
                  </label>
                  <select
                    name="roomId"
                    value={formData.roomId}
                    onChange={handleInputChange}
                    required
                    disabled={!formData.hostelId || loadingRooms}
                    className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-100 disabled:bg-slate-100 disabled:cursor-not-allowed"
                  >
                    <option value="">
                      {!formData.hostelId 
                        ? "Select hostel first" 
                        : loadingRooms 
                          ? "Loading rooms..." 
                          : "Choose a room"
                      }
                    </option>
                    {getAvailableRooms().map(room => (
                      <option key={room.roomId} value={room.roomId}>
                        Room {room.roomNumber} - Floor {room.floorNumber}
                      </option>
                    ))}
                  </select>
                </div>

                <FormInput
                  label="Account Number"
                  name="accountNumber"
                  value={formData.accountNumber}
                  onChange={handleInputChange}
                  placeholder="Enter electricity account number"
                  required
                />
              </div>

              {formData.hostelId && getAvailableRooms().length === 0 && !loadingRooms && (
                <Alert tone="info">
                  All rooms in this hostel already have electricity accounts.
                </Alert>
              )}

              <div className="flex gap-3">
                <Button
                  type="submit"
                  label="Create Account"
                  loading={creating}
                  disabled={!formData.hostelId || !formData.roomId || getAvailableRooms().length === 0}
                />
                <Button
                  type="button"
                  label="Cancel"
                  variant="secondary"
                  onClick={() => {
                    setShowCreateForm(false)
                    setFormData({ hostelId: '', roomId: '', accountNumber: '' })
                    setRooms([])
                    setError('')
                  }}
                />
              </div>
            </form>
          </div>
        </Card>
      )}

      {/* Accounts List */}
      {filteredAccounts.length === 0 ? (
        <EmptyState
          title={hostelFilter ? "No accounts in this hostel" : "No electricity accounts found"}
          description={hostelFilter ? "Try selecting a different hostel or create a new account." : "Create electricity accounts for your rooms to start managing electricity bills."}
          actionLabel="Create Account"
          onAction={() => setShowCreateForm(true)}
          disabled={hostels.length === 0}
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredAccounts.map(account => (
            <Card key={account.accountId} className="hover:shadow-lg transition-shadow">
              <div className="p-6">
                <div className="flex items-start justify-between mb-4">
                  <div>
                    <h3 className="text-lg font-semibold text-slate-950">Room {account.roomNumber}</h3>
                    <p className="text-sm text-slate-600">Account: {account.accountNumber}</p>
                    <p className="text-xs text-slate-500 mt-1">
                      Hostel: {getHostelName(account.hostelId)}
                    </p>
                  </div>
                  <div className={`px-2 py-1 rounded-full text-xs font-medium ${
                    account.isActive 
                      ? 'bg-green-100 text-green-800' 
                      : 'bg-gray-100 text-gray-800'
                  }`}>
                    {account.isActive ? 'Active' : 'Inactive'}
                  </div>
                </div>

                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-slate-600">Created:</span>
                    <span className="text-slate-900">
                      {new Date(account.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-600">Last Updated:</span>
                    <span className="text-slate-900">
                      {new Date(account.updatedAt).toLocaleDateString()}
                    </span>
                  </div>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* Info Message */}
      {hostels.length === 0 && (
        <Alert tone="info">
          You need to create hostels first before creating electricity accounts.
        </Alert>
      )}
    </div>
  )
}