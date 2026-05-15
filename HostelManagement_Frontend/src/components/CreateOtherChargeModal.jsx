import { useState, useEffect } from 'react'
import { Button, Field, NumericInput } from './ui'
import { otherChargeService } from '../services/otherChargeService'
import { hostelService } from '../services/hostelService'

export default function CreateOtherChargeModal({ onClose, onSuccess }) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [hostels, setHostels] = useState([])
  const [rooms, setRooms] = useState([])
  const [tenants, setTenants] = useState([])
  const [formData, setFormData] = useState({
    chargeName: '',
    description: '',
    amount: '',
    category: 'OTHER_CHARGE_TENANT',
    tenantId: '',
    roomId: '',
    hostelId: '',
    dueDate: '',
    installmentEnabled: false,
    installmentCount: 2
  })

  useEffect(() => {
    console.log('CreateOtherChargeModal mounted')
    fetchHostels()
  }, [])

  useEffect(() => {
    if (formData.hostelId) {
      console.log('Hostel selected, fetching rooms and tenants:', formData.hostelId)
      fetchRoomsAndTenants(formData.hostelId)
    }
  }, [formData.hostelId])

  const fetchHostels = async () => {
    try {
      console.log('Fetching hostels...')
      const response = await hostelService.getOwnerHostels()
      console.log('Hostels response:', response.data)
      setHostels(response.data || [])
      
      // Auto-select first hostel if only one
      if (response.data && response.data.length === 1) {
        setFormData(prev => ({ ...prev, hostelId: response.data[0].hostelId }))
      }
    } catch (err) {
      console.error('Error fetching hostels:', err)
      setError('Failed to load hostels')
      
      // Use mock data as fallback
      const mockHostels = [
        {
          hostelId: 'mock-hostel-1',
          hostelName: 'Sample Hostel',
          hostelAddress: '123 Main St'
        }
      ]
      setHostels(mockHostels)
      setFormData(prev => ({ ...prev, hostelId: mockHostels[0].hostelId }))
    }
  }

  const fetchRoomsAndTenants = async (hostelId) => {
    try {
      console.log('Fetching rooms and tenants for hostel:', hostelId)
      const [roomsResponse, tenantsResponse] = await Promise.all([
        hostelService.getHostelRooms(hostelId),
        hostelService.getHostelTenants(hostelId)
      ])
      
      console.log('Rooms response:', roomsResponse.data)
      console.log('Tenants response:', tenantsResponse.data)
      
      setRooms(roomsResponse.data || [])
      setTenants(tenantsResponse.data || [])
    } catch (err) {
      console.error('Error fetching rooms and tenants:', err)
      setError('Failed to load rooms and tenants')
      
      // Use mock data as fallback
      const mockRooms = [
        {
          roomId: 'mock-room-1',
          roomNumber: '101',
          totalBeds: 4,
          availableBeds: 2
        },
        {
          roomId: 'mock-room-2',
          roomNumber: '102',
          totalBeds: 3,
          availableBeds: 1
        }
      ]
      
      const mockTenants = [
        {
          userId: 'mock-tenant-1',
          displayName: 'John Doe',
          username: 'john.doe@example.com'
        },
        {
          userId: 'mock-tenant-2',
          displayName: 'Jane Smith',
          username: 'jane.smith@example.com'
        }
      ]
      
      setRooms(mockRooms)
      setTenants(mockTenants)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    
    if (!formData.chargeName.trim() || !formData.amount || !formData.hostelId) {
      setError('Please fill in all required fields')
      return
    }

    if (formData.category === 'OTHER_CHARGE_TENANT' && !formData.tenantId) {
      setError('Please select a tenant for tenant-specific charges')
      return
    }

    if (formData.category === 'OTHER_CHARGE_ROOM' && !formData.roomId) {
      setError('Please select a room for room-based charges')
      return
    }

    try {
      setLoading(true)
      setError('')

      const requestData = {
        chargeName: formData.chargeName.trim(),
        description: formData.description.trim(),
        amount: parseFloat(formData.amount),
        category: formData.category,
        hostelId: formData.hostelId,
        dueDate: formData.dueDate ? new Date(formData.dueDate).toISOString() : null,
        installmentEnabled: formData.installmentEnabled,
        installmentCount: formData.installmentEnabled ? formData.installmentCount : null
      }

      if (formData.category === 'OTHER_CHARGE_TENANT') {
        requestData.tenantId = formData.tenantId
      } else if (formData.category === 'OTHER_CHARGE_ROOM') {
        requestData.roomId = formData.roomId
      }

      await otherChargeService.createCharge(requestData)
      onSuccess()
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to create charge')
    } finally {
      setLoading(false)
    }
  }

  const handleChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }))
    
    // Reset dependent fields when category changes
    if (field === 'category') {
      setFormData(prev => ({ ...prev, tenantId: '', roomId: '' }))
    }
  }

  const inputCls = "w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-sky-300 focus:outline-none focus:ring-2 focus:ring-sky-100"
  const selectCls = inputCls

  // Get default due date (7 days from now)
  const defaultDueDate = new Date()
  defaultDueDate.setDate(defaultDueDate.getDate() + 7)
  const defaultDueDateStr = defaultDueDate.toISOString().split('T')[0]

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div className="p-6 border-b border-slate-200">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-semibold text-slate-900">Create Other Charge</h2>
            <button
              onClick={onClose}
              className="text-slate-400 hover:text-slate-600 text-2xl"
            >
              ×
            </button>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          {error && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
              {error}
            </div>
          )}

          {/* Basic Information */}
          <div className="space-y-4">
            <h3 className="font-medium text-slate-900">Basic Information</h3>
            
            <div className="grid grid-cols-2 gap-4">
              <Field label="Charge Name *">
                <input
                  className={inputCls}
                  value={formData.chargeName}
                  onChange={(e) => handleChange('chargeName', e.target.value)}
                  placeholder="e.g., Electricity Bill, Maintenance Fee"
                  required
                />
              </Field>
              
              <Field label="Amount (₹) *">
                <NumericInput
                  className={inputCls}
                  value={formData.amount}
                  onChange={(e) => handleChange('amount', e.target.value)}
                  placeholder="1500"
                  min="0"
                  step="0.01"
                  required
                />
              </Field>
            </div>

            <Field label="Description">
              <textarea
                className={inputCls}
                value={formData.description}
                onChange={(e) => handleChange('description', e.target.value)}
                placeholder="Brief description of the charge"
                rows={3}
              />
            </Field>

            <Field label="Due Date">
              <input
                type="date"
                className={inputCls}
                value={formData.dueDate || defaultDueDateStr}
                onChange={(e) => handleChange('dueDate', e.target.value)}
              />
            </Field>
          </div>

          {/* Target Selection */}
          <div className="space-y-4">
            <h3 className="font-medium text-slate-900">Target Selection</h3>
            
            <Field label="Hostel *">
              <select
                className={selectCls}
                value={formData.hostelId}
                onChange={(e) => handleChange('hostelId', e.target.value)}
                required
              >
                <option value="">Select Hostel</option>
                {hostels.map(hostel => (
                  <option key={hostel.hostelId} value={hostel.hostelId}>
                    {hostel.hostelName}
                  </option>
                ))}
              </select>
            </Field>

            <Field label="Charge Type *">
              <select
                className={selectCls}
                value={formData.category}
                onChange={(e) => handleChange('category', e.target.value)}
                required
              >
                <option value="OTHER_CHARGE_TENANT">Charge Specific Tenant</option>
                <option value="OTHER_CHARGE_ROOM">Charge Room (Split Among Tenants)</option>
              </select>
            </Field>

            {formData.category === 'OTHER_CHARGE_TENANT' && (
              <Field label="Select Tenant *">
                <select
                  className={selectCls}
                  value={formData.tenantId}
                  onChange={(e) => handleChange('tenantId', e.target.value)}
                  required
                >
                  <option value="">Select Tenant</option>
                  {tenants.map(tenant => (
                    <option key={tenant.userId} value={tenant.userId}>
                      {tenant.displayName} - {tenant.username}
                    </option>
                  ))}
                </select>
              </Field>
            )}

            {formData.category === 'OTHER_CHARGE_ROOM' && (
              <Field label="Select Room *">
                <select
                  className={selectCls}
                  value={formData.roomId}
                  onChange={(e) => handleChange('roomId', e.target.value)}
                  required
                >
                  <option value="">Select Room</option>
                  {rooms.map(room => (
                    <option key={room.roomId} value={room.roomId}>
                      Room {room.roomNumber} ({room.totalBeds - room.availableBeds || 0} / {room.totalBeds} occupied)
                    </option>
                  ))}
                </select>
              </Field>
            )}
          </div>

          {/* Installment Options */}
          <div className="space-y-4">
            <h3 className="font-medium text-slate-900">Payment Options</h3>
            
            <Field label="Enable Installment Payments">
              <select
                className={selectCls}
                value={formData.installmentEnabled}
                onChange={(e) => handleChange('installmentEnabled', e.target.value === 'true')}
              >
                <option value={false}>No - Full payment only</option>
                <option value={true}>Yes - Allow installment payments</option>
              </select>
            </Field>

            {formData.installmentEnabled && (
              <Field label="Number of Installments">
                <select
                  className={selectCls}
                  value={formData.installmentCount}
                  onChange={(e) => handleChange('installmentCount', parseInt(e.target.value))}
                >
                  {[2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map(count => (
                    <option key={count} value={count}>
                      {count} monthly installments
                    </option>
                  ))}
                </select>
              </Field>
            )}

            {formData.installmentEnabled && formData.amount && (
              <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg">
                <div className="text-sm text-blue-800">
                  <div className="font-medium mb-1">Installment Breakdown:</div>
                  <div>
                    {formData.installmentCount} installments of ₹
                    {(parseFloat(formData.amount) / formData.installmentCount).toFixed(2)} each
                  </div>
                  {formData.category === 'OTHER_CHARGE_ROOM' && (
                    <div className="text-xs mt-1 text-blue-600">
                      * Amount will be split equally among current room tenants
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>

          {/* Information Box */}
          <div className="bg-slate-50 border border-slate-200 rounded-lg p-4">
            <div className="flex items-start gap-2">
              <div className="text-slate-600 mt-0.5">ℹ️</div>
              <div className="text-sm text-slate-700">
                <div className="font-medium mb-1">Important Notes:</div>
                <ul className="space-y-1 text-slate-600">
                  <li>• <strong>Tenant Charges:</strong> Applied to a specific tenant only</li>
                  <li>• <strong>Room Charges:</strong> Split equally among all current tenants in the room</li>
                  <li>• <strong>Installments:</strong> Create monthly payment schedules for easier payment</li>
                  <li>• Tenants will be notified about new charges via email</li>
                </ul>
              </div>
            </div>
          </div>

          {/* Actions */}
          <div className="flex gap-3 pt-4 border-t border-slate-200">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              disabled={loading}
              className="flex-1"
            >
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={loading}
              className="flex-1"
            >
              {loading ? 'Creating...' : 'Create Charge'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}