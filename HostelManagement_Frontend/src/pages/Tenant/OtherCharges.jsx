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
import PaymentModal from '../../components/PaymentModal'
import OtherChargePaymentModal from '../../components/OtherChargePaymentModal'

export default function TenantOtherCharges() {
  const [charges, setCharges] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [filterStatus, setFilterStatus] = useState('ALL')
  const [selectedCharge, setSelectedCharge] = useState(null)
  const [showPaymentModal, setShowPaymentModal] = useState(false)
  const [selectedInstallment, setSelectedInstallment] = useState(null)
  const [stats, setStats] = useState({
    totalCharges: 0,
    totalAmount: 0,
    pendingAmount: 0,
    paidAmount: 0,
    overdueCount: 0
  })

  useEffect(() => {
    fetchCharges()
  }, [])

  const fetchCharges = async () => {
    try {
      setLoading(true)
      setError('')
      const response = await otherChargeService.getTenantCharges()
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
      
      // For room-based charges, calculate tenant's share
      const tenantAmount = charge.category === 'OTHER_CHARGE_ROOM' && charge.roomTenants ? 
        charge.amount / charge.roomTenants.length : charge.amount
      
      acc.totalAmount += tenantAmount
      
      if (charge.paymentStatus === 'COMPLETED') {
        acc.paidAmount += tenantAmount
      } else {
        const remaining = charge.remainingAmount || tenantAmount
        acc.pendingAmount += remaining
        
        if (charge.dueDate && new Date(charge.dueDate) < new Date() && charge.paymentStatus !== 'COMPLETED') {
          acc.overdueCount += 1
        }
      }
      
      return acc
    }, {
      totalCharges: 0,
      totalAmount: 0,
      pendingAmount: 0,
      paidAmount: 0,
      overdueCount: 0
    })
    
    setStats(stats)
  }

  const handlePayCharge = (charge) => {
    setSelectedCharge(charge)
    setSelectedInstallment(null)
    setShowPaymentModal(true)
  }

  const handlePayInstallment = (charge, installment) => {
    setSelectedCharge(charge)
    setSelectedInstallment(installment)
    setShowPaymentModal(true)
  }

  const handlePaymentSuccess = () => {
    setShowPaymentModal(false)
    setSelectedCharge(null)
    setSelectedInstallment(null)
    fetchCharges()
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
      OTHER_CHARGE_TENANT: { color: 'blue', text: 'Personal Charge' },
      OTHER_CHARGE_ROOM: { color: 'purple', text: 'Room Charge (Split)' }
    }
    
    const config = categoryConfig[category] || { color: 'gray', text: category }
    return <Badge color={config.color}>{config.text}</Badge>
  }

  const getTenantAmount = (charge) => {
    if (charge.category === 'OTHER_CHARGE_ROOM' && charge.roomTenants) {
      return charge.amount / charge.roomTenants.length
    }
    return charge.amount
  }

  const getTenantRemainingAmount = (charge) => {
    if (charge.category === 'OTHER_CHARGE_ROOM' && charge.roomTenants) {
      const tenantShare = charge.amount / charge.roomTenants.length
      const paidShare = (charge.paidAmount || 0) / charge.roomTenants.length
      return tenantShare - paidShare
    }
    return charge.remainingAmount || charge.amount
  }

  const filteredCharges = charges.filter(charge => {
    const matchesSearch = charge.chargeName.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         charge.description?.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         charge.ownerName?.toLowerCase().includes(searchQuery.toLowerCase())
    
    const matchesStatus = filterStatus === 'ALL' || charge.paymentStatus === filterStatus
    
    return matchesSearch && matchesStatus
  })

  const fmt = (amount) => `₹${(amount || 0).toLocaleString()}`

  if (loading) {
    return (
      <div className="space-y-6">
        <PageHeader title="My Other Charges" />
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
        title="My Other Charges" 
        subtitle="View and pay additional charges assigned to you"
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
          title="Paid"
          value={fmt(stats.paidAmount)}
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
                placeholder="Search charges..."
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
          </div>
        </CardContent>
      </Card>

      {/* Charges List */}
      {filteredCharges.length === 0 ? (
        <EmptyState
          title="No charges found"
          description={charges.length === 0 ? 
            "You don't have any additional charges at the moment." : 
            "No charges match your current filters."
          }
        />
      ) : (
        <div className="grid gap-4">
          {filteredCharges.map((charge) => {
            const tenantAmount = getTenantAmount(charge)
            const tenantRemaining = getTenantRemainingAmount(charge)
            
            return (
              <Card key={charge.chargeId} className="hover:shadow-md transition-shadow">
                <CardContent className="p-6">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-2">
                        <h3 className="font-semibold text-slate-900">{charge.chargeName}</h3>
                        {getStatusBadge(charge.paymentStatus)}
                        {getCategoryBadge(charge.category)}
                        {charge.installmentEnabled && (
                          <Badge color="blue">Installments Available</Badge>
                        )}
                      </div>
                      
                      {charge.description && (
                        <p className="text-sm text-slate-600 mb-3">{charge.description}</p>
                      )}
                      
                      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm mb-4">
                        <div>
                          <span className="text-slate-500">Your Amount:</span>
                          <div className="font-medium">{fmt(tenantAmount)}</div>
                        </div>
                        
                        {charge.paymentStatus !== 'COMPLETED' && (
                          <div>
                            <span className="text-slate-500">Remaining:</span>
                            <div className="font-medium text-orange-600">{fmt(tenantRemaining)}</div>
                          </div>
                        )}
                        
                        <div>
                          <span className="text-slate-500">Owner:</span>
                          <div className="font-medium">{charge.ownerName}</div>
                        </div>
                        
                        {charge.dueDate && (
                          <div>
                            <span className="text-slate-500">Due Date:</span>
                            <div className={`font-medium ${
                              new Date(charge.dueDate) < new Date() && charge.paymentStatus !== 'COMPLETED' ? 
                              'text-red-600' : 'text-slate-900'
                            }`}>
                              {new Date(charge.dueDate).toLocaleDateString()}
                            </div>
                          </div>
                        )}
                      </div>

                      {/* Room charge split info */}
                      {charge.category === 'OTHER_CHARGE_ROOM' && charge.roomTenants && (
                        <div className="mb-4 p-3 bg-purple-50 rounded-lg">
                          <div className="text-sm text-purple-800">
                            <div className="font-medium mb-1">Room Charge Split</div>
                            <div>Total: {fmt(charge.amount)} ÷ {charge.roomTenants.length} tenants = {fmt(tenantAmount)} per person</div>
                            <div className="text-xs mt-1">
                              Room {charge.roomNumber} • Shared with: {charge.roomTenants.filter(t => t.tenantId !== 'current').map(t => t.tenantName).join(', ')}
                            </div>
                          </div>
                        </div>
                      )}
                      
                      {/* Installments */}
                      {charge.installmentEnabled && charge.installments && (
                        <div className="mb-4 p-3 bg-slate-50 rounded-lg">
                          <div className="text-sm text-slate-600 mb-3">
                            <div className="flex justify-between items-center">
                              <span>Installment Progress:</span>
                              <span>{charge.installments.filter(i => i.paymentStatus === 'COMPLETED').length} / {charge.installments.length} completed</span>
                            </div>
                          </div>
                          
                          <div className="space-y-2">
                            {charge.installments.slice(0, 3).map((installment, idx) => (
                              <div key={idx} className="flex items-center justify-between p-2 bg-white rounded border">
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
                                      onClick={() => handlePayInstallment(charge, installment)}
                                      className={installment.isOverdue ? 'bg-red-600 hover:bg-red-700' : ''}
                                    >
                                      Pay Now
                                    </Button>
                                  )}
                                </div>
                              </div>
                            ))}
                            
                            {charge.installments.length > 3 && (
                              <div className="text-xs text-slate-500 text-center">
                                ... and {charge.installments.length - 3} more installments
                              </div>
                            )}
                          </div>
                        </div>
                      )}
                    </div>
                    
                    {/* Payment Actions */}
                    {charge.paymentStatus !== 'COMPLETED' && (
                      <div className="flex flex-col gap-2 ml-4">
                        <Button
                          onClick={() => handlePayCharge(charge)}
                          className={charge.paymentStatus === 'OVERDUE' ? 'bg-red-600 hover:bg-red-700' : ''}
                        >
                          Pay Full Amount
                        </Button>
                        
                        {charge.installmentEnabled && (
                          <Button
                            variant="outline"
                            onClick={() => {
                              const nextInstallment = charge.installments?.find(i => i.paymentStatus !== 'COMPLETED')
                              if (nextInstallment) {
                                handlePayInstallment(charge, nextInstallment)
                              }
                            }}
                          >
                            Pay Next Installment
                          </Button>
                        )}
                      </div>
                    )}
                  </div>
                </CardContent>
              </Card>
            )
          })}
        </div>
      )}

      {/* Payment Modal */}
      {showPaymentModal && selectedCharge && (
        <OtherChargePaymentModal
          charge={selectedCharge}
          installment={selectedInstallment}
          onClose={() => setShowPaymentModal(false)}
          onSuccess={handlePaymentSuccess}
        />
      )}
    </div>
  )
}