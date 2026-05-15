import { useEffect, useState } from 'react'
import { 
  Alert, 
  Badge, 
  Button, 
  Card, 
  CardContent, 
  CardHeader, 
  EmptyState, 
  PageHeader,
  Skeleton,
  StatCard
} from '../../components/ui'
import { otherChargeService } from '../../services/otherChargeService'
import CreateOtherChargeModal from '../../components/CreateOtherChargeModal'
import OtherChargeDetailsModal from '../../components/OtherChargeDetailsModal'

export default function OtherCharges() {
  const [charges, setCharges] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [filterStatus, setFilterStatus] = useState('ALL')
  const [filterCategory, setFilterCategory] = useState('ALL')
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [selectedCharge, setSelectedCharge] = useState(null)
  const [showDetailsModal, setShowDetailsModal] = useState(false)
  const [stats, setStats] = useState({
    totalCharges: 0,
    totalAmount: 0,
    pendingAmount: 0,
    collectedAmount: 0,
    overdueCount: 0
  })

  useEffect(() => {
    fetchCharges()
  }, [])

  const fetchCharges = async () => {
    try {
      setLoading(true)
      setError('')
      const response = await otherChargeService.getOwnerCharges()
      setCharges(response.data)
      calculateStats(response.data)
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to load charges.')
    } finally {
      setLoading(false)
    }
  }

  const calculateStats = (chargesData) => {
    const stats = chargesData.reduce((acc, charge) => {
      acc.totalCharges += 1
      acc.totalAmount += charge.amount
      
      if (charge.paymentStatus === 'COMPLETED') {
        acc.collectedAmount += charge.amount
      } else {
        acc.pendingAmount += charge.remainingAmount || charge.amount
        
        if (charge.dueDate && new Date(charge.dueDate) < new Date() && charge.paymentStatus !== 'COMPLETED') {
          acc.overdueCount += 1
        }
      }
      
      return acc
    }, {
      totalCharges: 0,
      totalAmount: 0,
      pendingAmount: 0,
      collectedAmount: 0,
      overdueCount: 0
    })
    
    setStats(stats)
  }

  const handleCreateCharge = () => {
    setShowCreateModal(true)
  }

  const handleChargeCreated = () => {
    setShowCreateModal(false)
    fetchCharges()
  }

  const handleViewDetails = (charge) => {
    setSelectedCharge(charge)
    setShowDetailsModal(true)
  }

  const handleDeleteCharge = async (chargeId) => {
    if (!confirm('Are you sure you want to delete this charge?')) return
    
    try {
      await otherChargeService.deleteCharge(chargeId)
      fetchCharges()
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to delete charge.')
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

  const getCategoryBadge = (category) => {
    const categoryConfig = {
      OTHER_CHARGE_TENANT: { color: 'blue', text: 'Tenant Specific' },
      OTHER_CHARGE_ROOM: { color: 'purple', text: 'Room Based' }
    }
    
    const config = categoryConfig[category] || { color: 'gray', text: category }
    return <Badge color={config.color}>{config.text}</Badge>
  }

  const filteredCharges = charges.filter(charge => {
    const matchesSearch = charge.chargeName.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         charge.description?.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         charge.tenantName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         charge.roomNumber?.toLowerCase().includes(searchQuery.toLowerCase())
    
    const matchesStatus = filterStatus === 'ALL' || charge.paymentStatus === filterStatus
    const matchesCategory = filterCategory === 'ALL' || charge.category === filterCategory
    
    return matchesSearch && matchesStatus && matchesCategory
  })

  const fmt = (amount) => `₹${(amount || 0).toLocaleString()}`

  if (loading) {
    return (
      <div className="space-y-6">
        <PageHeader title="Other Charges" />
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {[...Array(4)].map((_, i) => (
            <Skeleton key={i} className="h-24" />
          ))}
        </div>
        <Skeleton className="h-96" />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <PageHeader 
        title="Other Charges" 
        subtitle="Manage additional charges for tenants and rooms"
        action={
          <Button onClick={handleCreateCharge} className="bg-sky-600 hover:bg-sky-700 text-white">
            + Create New Charge
          </Button>
        }
      />

      {error && (
        <Alert type="error" message={error} onClose={() => setError('')} />
      )}

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
        <StatCard
          title="Total Charges"
          value={stats.totalCharges}
          icon="📋"
        />
        <StatCard
          title="Total Amount"
          value={fmt(stats.totalAmount)}
          icon="💰"
        />
        <StatCard
          title="Collected"
          value={fmt(stats.collectedAmount)}
          icon="✅"
          trend="positive"
        />
        <StatCard
          title="Pending"
          value={fmt(stats.pendingAmount)}
          icon="⏳"
          trend="neutral"
        />
        <StatCard
          title="Overdue"
          value={stats.overdueCount}
          icon="⚠️"
          trend={stats.overdueCount > 0 ? "negative" : "neutral"}
        />
      </div>

      {/* Filters */}
      <Card>
        <CardContent className="p-4">
          <div className="flex flex-col md:flex-row gap-4">
            <div className="flex-1">
              <input
                type="text"
                placeholder="Search charges, tenants, rooms..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-sky-500"
              />
            </div>
            
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-sky-500"
            >
              <option value="ALL">All Status</option>
              <option value="PENDING">Pending</option>
              <option value="PARTIALLY_PAID">Partially Paid</option>
              <option value="COMPLETED">Completed</option>
              <option value="OVERDUE">Overdue</option>
            </select>
            
            <select
              value={filterCategory}
              onChange={(e) => setFilterCategory(e.target.value)}
              className="px-3 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-sky-500"
            >
              <option value="ALL">All Categories</option>
              <option value="OTHER_CHARGE_TENANT">Tenant Specific</option>
              <option value="OTHER_CHARGE_ROOM">Room Based</option>
            </select>
          </div>
        </CardContent>
      </Card>

      {/* Charges List */}
      {filteredCharges.length === 0 ? (
        <EmptyState
          title="No charges found"
          description={charges.length === 0 ? 
            "Create your first other charge to get started. You can charge specific tenants or entire rooms." : 
            "No charges match your current filters."
          }
          action={charges.length === 0 ? (
            <Button onClick={handleCreateCharge} className="bg-sky-600 hover:bg-sky-700 text-white">
              + Create Your First Charge
            </Button>
          ) : null}
        />
      ) : (
        <div className="grid gap-4">
          {filteredCharges.map((charge) => (
            <Card key={charge.chargeId} className="hover:shadow-md transition-shadow">
              <CardContent className="p-6">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <h3 className="font-semibold text-slate-900">{charge.chargeName}</h3>
                      {getStatusBadge(charge.paymentStatus)}
                      {getCategoryBadge(charge.category)}
                      {charge.installmentEnabled && (
                        <Badge color="blue">Installments</Badge>
                      )}
                    </div>
                    
                    {charge.description && (
                      <p className="text-sm text-slate-600 mb-3">{charge.description}</p>
                    )}
                    
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                      <div>
                        <span className="text-slate-500">Amount:</span>
                        <div className="font-medium">{fmt(charge.amount)}</div>
                      </div>
                      
                      {charge.paymentStatus !== 'COMPLETED' && (
                        <div>
                          <span className="text-slate-500">Remaining:</span>
                          <div className="font-medium text-orange-600">{fmt(charge.remainingAmount)}</div>
                        </div>
                      )}
                      
                      <div>
                        <span className="text-slate-500">Target:</span>
                        <div className="font-medium">
                          {charge.category === 'OTHER_CHARGE_TENANT' ? (
                            <span>👤 {charge.tenantName}</span>
                          ) : (
                            <span>🏠 Room {charge.roomNumber} ({charge.roomTenants?.length || 0} tenants)</span>
                          )}
                        </div>
                      </div>
                      
                      {charge.dueDate && (
                        <div>
                          <span className="text-slate-500">Due Date:</span>
                          <div className="font-medium">
                            {new Date(charge.dueDate).toLocaleDateString()}
                          </div>
                        </div>
                      )}
                    </div>
                    
                    {charge.installmentEnabled && charge.installments && (
                      <div className="mt-3 p-3 bg-slate-50 rounded-lg">
                        <div className="text-sm text-slate-600 mb-2">
                          Installments: {charge.installments.filter(i => i.paymentStatus === 'COMPLETED').length} / {charge.installments.length} completed
                        </div>
                        <div className="flex gap-1">
                          {charge.installments.map((installment, idx) => (
                            <div
                              key={idx}
                              className={`w-4 h-2 rounded-full ${
                                installment.paymentStatus === 'COMPLETED' ? 'bg-green-500' :
                                installment.paymentStatus === 'PARTIALLY_PAID' ? 'bg-yellow-500' :
                                installment.isOverdue ? 'bg-red-500' : 'bg-slate-300'
                              }`}
                              title={`Installment ${installment.installmentNumber}: ${installment.paymentStatus}`}
                            />
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                  
                  <div className="flex gap-2 ml-4">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleViewDetails(charge)}
                    >
                      View Details
                    </Button>
                    
                    {charge.paymentStatus !== 'COMPLETED' && (
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleDeleteCharge(charge.chargeId)}
                        className="text-red-600 hover:text-red-700 hover:bg-red-50"
                      >
                        Delete
                      </Button>
                    )}
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Modals */}
      {showCreateModal && (
        <CreateOtherChargeModal
          onClose={() => setShowCreateModal(false)}
          onSuccess={handleChargeCreated}
        />
      )}
      
      {showDetailsModal && selectedCharge && (
        <OtherChargeDetailsModal
          charge={selectedCharge}
          onClose={() => setShowDetailsModal(false)}
          onUpdate={fetchCharges}
        />
      )}

      {/* Floating Action Button for Mobile */}
      <div className="fixed bottom-6 right-6 lg:hidden">
        <Button
          onClick={handleCreateCharge}
          className="w-14 h-14 rounded-full bg-sky-600 hover:bg-sky-700 text-white shadow-lg hover:shadow-xl transition-all duration-200"
          title="Create New Charge"
        >
          <span className="text-2xl">+</span>
        </Button>
      </div>
    </div>
  )
}