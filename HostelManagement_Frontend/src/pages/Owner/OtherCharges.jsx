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
import OtherChargeHistoryModal from '../../components/OtherChargeHistoryModal'
import { OtherChargeCards } from '../../components/OtherChargeViews'

export default function OtherCharges() {
  const [charges, setCharges] = useState([])
  const [tenantRows, setTenantRows] = useState([]) // New tenant-specific rows
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [filterStatus, setFilterStatus] = useState('ALL')
  const [filterCategory, setFilterCategory] = useState('ALL')
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [selectedCharge, setSelectedCharge] = useState(null)
  const [showDetailsModal, setShowDetailsModal] = useState(false)
  const [historyTenant, setHistoryTenant] = useState(null)
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
      transformToTenantRows(response.data)
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to load charges.')
    } finally {
      setLoading(false)
    }
  }

  const transformToTenantRows = (chargesData) => {
    const tenantMap = new Map()

    chargesData.forEach(charge => {
      if (charge.category === 'OTHER_CHARGE_TENANT') {
        // For tenant-specific charges
        const key = `${charge.tenantId || charge.tenantName}-${charge.roomNumber}`
        if (!tenantMap.has(key)) {
          tenantMap.set(key, {
            tenantId: charge.tenantId,
            tenantName: charge.tenantName,
            hostelName: charge.hostelName || 'N/A',
            roomNumber: charge.roomNumber,
            charges: [],
            totalCharges: 0,
            pendingCharges: 0,
            totalAmount: 0,
            outstandingAmount: 0,
            overdueAmount: 0
          })
        }
        
        const tenantRow = tenantMap.get(key)
        tenantRow.charges.push(charge)
        tenantRow.totalCharges += 1
        tenantRow.totalAmount += charge.amount

        if (charge.paymentStatus !== 'COMPLETED') {
          tenantRow.pendingCharges += 1
          tenantRow.outstandingAmount += charge.remainingAmount || charge.amount
          
          if (charge.dueDate && new Date(charge.dueDate) < new Date()) {
            tenantRow.overdueAmount += charge.remainingAmount || charge.amount
          }
        }
      } else if (charge.category === 'OTHER_CHARGE_ROOM' && charge.roomTenants) {
        // For room-based charges, create entries for each tenant in the room
        charge.roomTenants.forEach(roomTenant => {
          const key = `${roomTenant.tenantId || roomTenant.tenantName}-${charge.roomNumber}`
          if (!tenantMap.has(key)) {
            tenantMap.set(key, {
              tenantId: roomTenant.tenantId,
              tenantName: roomTenant.tenantName,
              hostelName: charge.hostelName || 'N/A',
              roomNumber: charge.roomNumber,
              charges: [],
              totalCharges: 0,
              pendingCharges: 0,
              totalAmount: 0,
              outstandingAmount: 0,
              overdueAmount: 0
            })
          }
          
          const tenantRow = tenantMap.get(key)
          // Add room charge with tenant's split amount
          const tenantCharge = {
            ...charge,
            splitAmount: roomTenant.splitAmount,
            tenantPaymentStatus: roomTenant.paymentStatus
          }
          tenantRow.charges.push(tenantCharge)
          tenantRow.totalCharges += 1
          tenantRow.totalAmount += roomTenant.splitAmount

          if (roomTenant.paymentStatus !== 'COMPLETED') {
            tenantRow.pendingCharges += 1
            tenantRow.outstandingAmount += roomTenant.splitAmount
            
            if (charge.dueDate && new Date(charge.dueDate) < new Date()) {
              tenantRow.overdueAmount += roomTenant.splitAmount
            }
          }
        })
      }
    })

    setTenantRows(Array.from(tenantMap.values()))
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

  const handleCollect = (tenant) => {
    const charge = tenant.charges?.find((item) => {
      if (item.category === 'OTHER_CHARGE_ROOM') {
        return item.roomTenants?.some((roomTenant) => roomTenant.tenantId === tenant.tenantId && roomTenant.paymentStatus !== 'COMPLETED')
      }
      return item.paymentStatus !== 'COMPLETED'
    })

    if (charge) {
      setSelectedCharge(charge)
      setShowDetailsModal(true)
    }
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

  const getStatusBadge = (tenant) => {
    if (tenant.pendingCharges === 0) {
      return <Badge color="green">All Paid</Badge>
    }
    if (tenant.overdueAmount > 0) {
      return <Badge color="red">{tenant.pendingCharges} Overdue</Badge>
    }
    return <Badge color="yellow">{tenant.pendingCharges} Pending</Badge>
  }

  const filteredTenantRows = tenantRows.filter(tenant => {
    const matchesSearch = tenant.tenantName.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         tenant.roomNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         tenant.hostelName.toLowerCase().includes(searchQuery.toLowerCase())
    
    // For tenant filtering, we check if they have any charges matching the filters
    const hasMatchingCharges = tenant.charges.some(charge => {
      const matchesStatus = filterStatus === 'ALL' || 
                           (filterStatus === 'PENDING' && charge.paymentStatus !== 'COMPLETED') ||
                           (filterStatus === 'COMPLETED' && charge.paymentStatus === 'COMPLETED') ||
                           charge.paymentStatus === filterStatus
      
      const matchesCategory = filterCategory === 'ALL' || charge.category === filterCategory
      
      return matchesStatus && matchesCategory
    })
    
    return matchesSearch && hasMatchingCharges
  })

  const filteredCharges = charges.filter((charge) => {
    const query = searchQuery.toLowerCase()
    const matchesSearch = charge.chargeName?.toLowerCase().includes(query) ||
      charge.description?.toLowerCase().includes(query) ||
      charge.tenantName?.toLowerCase().includes(query) ||
      charge.roomNumber?.toLowerCase().includes(query) ||
      charge.hostelName?.toLowerCase().includes(query)
    const matchesStatus = filterStatus === 'ALL' ||
      (filterStatus === 'PENDING' && charge.paymentStatus !== 'COMPLETED') ||
      (filterStatus === 'COMPLETED' && charge.paymentStatus === 'COMPLETED') ||
      charge.paymentStatus === filterStatus
    return matchesSearch && matchesStatus && (filterCategory === 'ALL' || charge.category === filterCategory)
  })

  const fmt = (amount) => `₹${(amount || 0).toLocaleString()}`

  return (
    <div className="space-y-6">
      <PageHeader 
        title="Other Charges" 
        toolbar={
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
            <input
              type="search"
              placeholder="Search charges..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900 outline-none transition focus:border-sky-500 focus:ring-2 focus:ring-sky-100 sm:w-56"
            />
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              aria-label="Filter charges"
              className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm font-medium text-slate-700 outline-none transition focus:border-sky-500 focus:ring-2 focus:ring-sky-100"
            >
              <option value="ALL">Filter</option>
              <option value="PENDING">Pending</option>
              <option value="PARTIALLY_PAID">Partially paid</option>
              <option value="COMPLETED">Completed</option>
              <option value="OVERDUE">Overdue</option>
            </select>
            <select
              value={filterCategory}
              onChange={(e) => setFilterCategory(e.target.value)}
              aria-label="Filter charge category"
              className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm font-medium text-slate-700 outline-none transition focus:border-sky-500 focus:ring-2 focus:ring-sky-100"
            >
              <option value="ALL">All categories</option>
              <option value="OTHER_CHARGE_TENANT">Tenant specific</option>
              <option value="OTHER_CHARGE_ROOM">Room based</option>
            </select>
            <Button onClick={handleCreateCharge} className="bg-sky-600 hover:bg-sky-700 text-white">
              + Create Charge
            </Button>
          </div>
        }
      />

      {error && (
        <Alert type="error" message={error} onClose={() => setError('')} />
      )}

      {/* Stats Cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
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

      {/* Tenant-specific Table */}
      <Card>
        <CardHeader className="border-b border-slate-100">
          <h2 className="text-lg font-semibold text-slate-950">Other charges</h2>
          <p className="text-sm text-slate-600 mt-1">Additional charges applied to tenants and rooms with payment status.</p>
        </CardHeader>
        <CardContent>
          <OtherChargeCards
            loading={loading}
            charges={filteredCharges}
            onDetails={(charge) => { setSelectedCharge(charge); setShowDetailsModal(true) }}
            onCollect={(charge) => { setSelectedCharge(charge); setShowDetailsModal(true) }}
          />
        </CardContent>
      </Card>

      {false && <Card>
        <CardHeader className="border-b border-slate-100">
          <h2 className="text-lg font-semibold text-slate-950">
            Other charges collection
          </h2>
          <p className="text-sm text-slate-600 mt-1">
            Per-tenant other charges dues, outstanding and overdue amounts.
          </p>
        </CardHeader>
        <CardContent className="p-0">
          {loading ? (
            <div className="p-6 space-y-4">
              {Array.from({ length: 3 }).map((_, i) => (
                <Skeleton key={i} className="h-16 rounded-lg" />
              ))}
            </div>
          ) : !tenantRows.length ? (
            <EmptyState
              title="No tenants with charges"
              description="Additional charges will appear here when you create them for tenants."
            />
          ) : filteredTenantRows.length === 0 ? (
            <EmptyState
              title="No matching tenants"
              description="Try adjusting your search query or filters to find what you're looking for."
            />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-100 text-left text-xs font-semibold uppercase tracking-[0.15em] text-slate-400">
                    <th className="pb-3 pr-4 pl-6">Tenant</th>
                    <th className="pb-3 pr-4">Hostel / Room</th>
                    <th className="pb-3 pr-4">Pending charges</th>
                    <th className="pb-3 pr-4">Outstanding</th>
                    <th className="pb-3 pr-4">Overdue amount</th>
                    <th className="pb-3 text-center pr-6">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {filteredTenantRows.map((tenant) => (
                    <tr key={`${tenant.tenantId}-${tenant.roomNumber}`} className="hover:bg-slate-50">
                      <td className="py-3 pr-4 pl-6">
                        <p className="font-semibold text-slate-950">{tenant.tenantName}</p>
                      </td>
                      <td className="py-3 pr-4 text-slate-600">
                        {tenant.hostelName} · Room {tenant.roomNumber}
                      </td>
                      <td className="py-3 pr-4">
                        {tenant.pendingCharges > 0 ? (
                          <Badge variant="warning">{tenant.pendingCharges} pending</Badge>
                        ) : (
                          <Badge variant="success">All paid</Badge>
                        )}
                      </td>
                      <td className="py-3 pr-4 font-semibold text-slate-950">
                        {Number(tenant.outstandingAmount) > 0 ? fmt(tenant.outstandingAmount) : '—'}
                      </td>
                      <td className="py-3 pr-4 font-semibold text-red-600">
                        {Number(tenant.overdueAmount) > 0 ? fmt(tenant.overdueAmount) : '—'}
                      </td>
                      <td className="py-3 text-center pr-6">
                        <div className="flex items-center justify-center gap-2">
                          <Button
                            variant="secondary"
                            size="sm"
                            onClick={() => setHistoryTenant(tenant)}
                          >
                            History
                          </Button>
                          {Number(tenant.outstandingAmount) > 0 ? (
                            <Button
                              variant="primary"
                              size="sm"
                              onClick={() => handleCollect(tenant)}
                            >
                              Collect
                            </Button>
                          ) : (
                            <span className="text-sm text-slate-500">No pending</span>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>}

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

      {historyTenant && (
        <OtherChargeHistoryModal
          tenant={historyTenant}
          onClose={() => setHistoryTenant(null)}
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
