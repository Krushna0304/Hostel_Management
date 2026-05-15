import { useEffect, useState } from 'react'
import { ownerReportService } from '../../services/agreementService'
import { otherChargeService } from '../../services/otherChargeService'
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
  StatCard,
} from '../../components/ui'
import QuickPaymentConfirmation from '../../components/QuickPaymentConfirmation'
import PaymentHistoryModal from '../../components/PaymentHistoryModal'

export default function CollectionDashboard() {
  const [data, setData] = useState(null)
  const [otherCharges, setOtherCharges] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [activeTab, setActiveTab] = useState('installments') // 'installments' or 'other-charges'
  const [showQuickPayment, setShowQuickPayment] = useState(false)
  const [selectedTenant, setSelectedTenant] = useState(null)
  const [nextInstallment, setNextInstallment] = useState(null)
  const [loadingInstallment, setLoadingInstallment] = useState(false)
  const [showHistory, setShowHistory] = useState(false)
  const [historyTenant, setHistoryTenant] = useState(null)

  useEffect(() => {
    fetchData()
    fetchOtherCharges()
  }, [])

  const fetchData = async () => {
    try {
      setLoading(true)
      setError('')
      const res = await ownerReportService.getCollectionSummary()
      setData(res.data)
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to load collection data.')
    } finally {
      setLoading(false)
    }
  }

  const fetchOtherCharges = async () => {
    try {
      const res = await otherChargeService.getOwnerCharges()
      setOtherCharges(res.data)
    } catch (err) {
      console.error('Failed to load other charges:', err)
    }
  }

  const handleCollectNextInstallment = async (tenant) => {
    try {
      setLoadingInstallment(true)
      setError('')
      
      // Debug: Check if token exists
      const token = localStorage.getItem('authToken')
      console.log('Auth token exists:', !!token)
      console.log('Making request to:', `/owner/reports/tenant/${tenant.tenantId}/installments`)
      
      // Get the next due installment for this tenant
      const res = await ownerReportService.getTenantInstallments(tenant.tenantId)
      const installments = res.data
      
      // Find the next due installment (SCHEDULED or OVERDUE)
      const nextDue = installments.find(inst => 
        inst.paymentStatus === 'SCHEDULED' || 
        inst.paymentStatus === 'OVERDUE' || 
        inst.paymentStatus === 'PARTIALLY_PAID'
      )
      
      if (nextDue) {
        setSelectedTenant(tenant)
        setNextInstallment(nextDue)
        setShowQuickPayment(true)
      } else {
        setError('No pending installments found for this tenant.')
      }
    } catch (err) {
      console.error('Error details:', err)
      console.error('Response status:', err?.response?.status)
      console.error('Response data:', err?.response?.data)
      setError('Failed to load installment details: ' + (err?.response?.data?.message || err.message))
    } finally {
      setLoadingInstallment(false)
    }
  }

  const handlePaymentSuccess = () => {
    setShowQuickPayment(false)
    setSelectedTenant(null)
    setNextInstallment(null)
    fetchData() // Refresh the data
    fetchOtherCharges() // Refresh other charges
  }

  const fmt = (a) => `₹${(a ?? 0).toLocaleString()}`

  // Calculate other charges stats
  const otherChargesStats = otherCharges.reduce((acc, charge) => {
    acc.totalAmount += charge.amount
    if (charge.paymentStatus === 'COMPLETED') {
      acc.collectedAmount += charge.amount
    } else {
      acc.pendingAmount += charge.remainingAmount || charge.amount
      if (charge.dueDate && new Date(charge.dueDate) < new Date()) {
        acc.overdueAmount += charge.remainingAmount || charge.amount
      }
    }
    return acc
  }, { totalAmount: 0, collectedAmount: 0, pendingAmount: 0, overdueAmount: 0 })

  // Filter tenants based on search query
  const filteredTenants = data?.tenants?.filter((tenant) => {
    const query = searchQuery.toLowerCase()
    return (
      tenant.tenantName?.toLowerCase().includes(query) ||
      tenant.hostelName?.toLowerCase().includes(query) ||
      tenant.roomNumber?.toLowerCase().includes(query) ||
      tenant.tenantId?.toLowerCase().includes(query)
    )
  }) || []

  // Filter other charges based on search query
  const filteredOtherCharges = otherCharges.filter((charge) => {
    const query = searchQuery.toLowerCase()
    return (
      charge.chargeName?.toLowerCase().includes(query) ||
      charge.description?.toLowerCase().includes(query) ||
      charge.tenantName?.toLowerCase().includes(query) ||
      charge.roomNumber?.toLowerCase().includes(query)
    )
  })

  if (loading) {
    return (
      <div className="space-y-8">
        <div className="grid gap-4 lg:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-40 rounded-3xl" />)}
        </div>
        <Skeleton className="h-96 rounded-3xl" />
      </div>
    )
  }

  const stats = [
    { label: 'Total collected', value: fmt(data?.totalCollected), meta: 'All time across all tenants' },
    { label: 'Pending', value: fmt(data?.totalPending), meta: 'Upcoming scheduled payments' },
    { label: 'Overdue', value: fmt(data?.totalOverdue), meta: `${data?.overdueTenantsCount ?? 0} tenant(s) overdue` },
    { label: 'Active tenants', value: data?.activeTenants ?? 0, meta: 'Currently allotted tenants' },
  ]

  return (
    <div className="space-y-8">
      {/* Quick Payment Confirmation Modal */}
      {showQuickPayment && (
        <QuickPaymentConfirmation
          tenant={selectedTenant}
          nextInstallment={nextInstallment}
          onClose={() => {
            setShowQuickPayment(false)
            setSelectedTenant(null)
            setNextInstallment(null)
          }}
          onSuccess={handlePaymentSuccess}
        />
      )}

      {/* Payment History Modal */}
      {showHistory && historyTenant && (
        <PaymentHistoryModal
          tenantName={historyTenant.tenantName}
          fetchLedger={async () => {
            const res = await ownerReportService.getTenantPaymentHistory(historyTenant.tenantId)
            return res.data
          }}
          onClose={() => {
            setShowHistory(false)
            setHistoryTenant(null)
          }}
        />
      )}

      <PageHeader
        eyebrow="Collections"
        title="Rent collection dashboard"
        description="Track rent collection, overdue payments, and tenant payment status across all your properties."
        action={<Button label="Refresh" variant="secondary" onClick={fetchData} />}
      />

      {error ? <Alert tone="error">{error}</Alert> : null}

      <section className="grid gap-4 lg:grid-cols-4">
        {loading
          ? Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-40 rounded-3xl" />)
          : stats.map((s) => <StatCard key={s.label} {...s} />)}
      </section>

      {/* Tabs */}
      <div className="flex space-x-1 bg-slate-100 p-1 rounded-lg">
        <button
          onClick={() => setActiveTab('installments')}
          className={`flex-1 px-4 py-2 text-sm font-medium rounded-md transition-colors ${
            activeTab === 'installments'
              ? 'bg-white text-slate-900 shadow-sm'
              : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          Installment Collections ({filteredTenants.length})
        </button>
        <button
          onClick={() => setActiveTab('other-charges')}
          className={`flex-1 px-4 py-2 text-sm font-medium rounded-md transition-colors ${
            activeTab === 'other-charges'
              ? 'bg-white text-slate-900 shadow-sm'
              : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          Other Charges ({filteredOtherCharges.length})
        </button>
      </div>

      <Card>
        <CardHeader
          title={activeTab === 'installments' ? "Tenant collection status" : "Other Charges Collection"}
          description={activeTab === 'installments' ? 
            "Per-tenant breakdown of rent collection, pending installments, and overdue amounts." :
            "Additional charges applied to tenants and rooms with payment status."
          }
        />
        <CardContent>
          {/* Search Filter */}
          <div className="mb-6">
            <div className="relative">
              <input
                type="text"
                placeholder={activeTab === 'installments' ? 
                  "Search by tenant name, hostel, room, or ID..." :
                  "Search by charge name, tenant, room..."
                }
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 pl-11 text-sm text-slate-950 placeholder-slate-400 transition focus:border-sky-300 focus:outline-none focus:ring-4 focus:ring-sky-100"
              />
              <svg
                className="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                />
              </svg>
              {searchQuery && (
                <button
                  onClick={() => setSearchQuery('')}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                >
                  <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              )}
            </div>
            {searchQuery && (
              <p className="mt-2 text-xs text-slate-500">
                {activeTab === 'installments' ? 
                  `Found ${filteredTenants.length} of ${data?.tenants?.length || 0} tenants` :
                  `Found ${filteredOtherCharges.length} of ${otherCharges.length} charges`
                }
              </p>
            )}
          </div>

          {activeTab === 'installments' ? (
            // Existing installments table
            !data?.tenants?.length ? (
              <EmptyState
                title="No active tenants"
                description="Tenant payment data will appear here once agreements are activated."
              />
            ) : filteredTenants.length === 0 ? (
              <EmptyState
                title="No matching tenants"
                description="Try adjusting your search query to find what you're looking for."
              />
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-100 text-left text-xs font-semibold uppercase tracking-[0.15em] text-slate-400">
                    <th className="pb-3 pr-4">Tenant</th>
                    <th className="pb-3 pr-4">Hostel / Room</th>
                    <th className="pb-3 pr-4">Monthly rent</th>
                    <th className="pb-3 pr-4">Pending installments</th>
                    <th className="pb-3 pr-4">Overdue</th>
                    <th className="pb-3 pr-4">Overdue amount</th>
                    <th className="pb-3 text-center">Actions</th>
                  </tr>                </thead>
                <tbody className="divide-y divide-slate-100">
                  {filteredTenants.map((t) => (
                    <tr key={t.tenantId}>
                      <td className="py-3 pr-4">
                        <p className="font-semibold text-slate-950">{t.tenantName}</p>
                        <p className="text-xs text-slate-400">{t.tenantId.slice(0, 8)}…</p>
                      </td>
                      <td className="py-3 pr-4 text-slate-600">
                        {t.hostelName} · Room {t.roomNumber}
                      </td>
                      <td className="py-3 pr-4 font-semibold text-slate-950">{fmt(t.installmentAmount)}</td>
                      <td className="py-3 pr-4 text-slate-700">{t.pendingInstallments ?? '—'}</td>
                      <td className="py-3 pr-4">
                        {t.overdueInstallments > 0 ? (
                          <Badge variant="danger">{t.overdueInstallments} overdue</Badge>
                        ) : (
                          <Badge variant="success">On track</Badge>
                        )}
                      </td>
                      <td className="py-3 font-semibold text-red-600">
                        {t.totalOverdueAmount > 0 ? fmt(t.totalOverdueAmount) : '—'}
                      </td>
                      <td className="py-3 text-center">
                        <div className="flex items-center justify-center gap-2">
                          <Button
                            label="History"
                            variant="secondary"
                            size="sm"
                            onClick={() => {
                              setHistoryTenant(t)
                              setShowHistory(true)
                            }}
                          />
                          {t.pendingInstallments === 0 ? (
                            <span className="text-sm text-slate-500">No pending</span>
                          ) : (
                            <Button
                              label={loadingInstallment ? "Loading..." : "Collect"}
                              variant="primary"
                              size="sm"
                              onClick={() => handleCollectNextInstallment(t)}
                              disabled={loadingInstallment}
                            />
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            )
          ) : (
            // Other Charges Section
            !otherCharges.length ? (
              <EmptyState
                title="No other charges"
                description="Additional charges will appear here when you create them."
              />
            ) : filteredOtherCharges.length === 0 ? (
              <EmptyState
                title="No matching charges"
                description="Try adjusting your search query to find what you're looking for."
              />
            ) : (
              <div className="space-y-4">
                {filteredOtherCharges.map((charge) => (
                  <div key={charge.chargeId} className="border border-slate-200 rounded-lg p-4 hover:shadow-sm transition-shadow">
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center gap-3 mb-2">
                          <h3 className="font-semibold text-slate-900">{charge.chargeName}</h3>
                          <Badge variant={
                            charge.paymentStatus === 'COMPLETED' ? 'success' :
                            charge.paymentStatus === 'OVERDUE' ? 'danger' :
                            charge.paymentStatus === 'PARTIALLY_PAID' ? 'warning' : 'neutral'
                          }>
                            {charge.paymentStatus === 'COMPLETED' ? 'Paid' :
                             charge.paymentStatus === 'OVERDUE' ? 'Overdue' :
                             charge.paymentStatus === 'PARTIALLY_PAID' ? 'Partial' : 'Pending'}
                          </Badge>
                          <Badge variant={charge.category === 'OTHER_CHARGE_TENANT' ? 'primary' : 'secondary'}>
                            {charge.category === 'OTHER_CHARGE_TENANT' ? 'Tenant' : 'Room'}
                          </Badge>
                          {charge.installmentEnabled && (
                            <Badge variant="info">Installments</Badge>
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
                              <div className={`font-medium ${
                                new Date(charge.dueDate) < new Date() && charge.paymentStatus !== 'COMPLETED' ? 
                                'text-red-600' : 'text-slate-900'
                              }`}>
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
                          label="View Details"
                          variant="secondary"
                          size="sm"
                          onClick={() => {
                            // TODO: Open charge details modal
                            console.log('View charge details:', charge.chargeId)
                          }}
                        />
                        
                        {charge.paymentStatus !== 'COMPLETED' && (
                          <Button
                            label="Collect Payment"
                            variant="primary"
                            size="sm"
                            onClick={() => {
                              // TODO: Open payment collection modal
                              console.log('Collect payment for charge:', charge.chargeId)
                            }}
                          />
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )
          )}
        </CardContent>
      </Card>
    </div>
  )
}
