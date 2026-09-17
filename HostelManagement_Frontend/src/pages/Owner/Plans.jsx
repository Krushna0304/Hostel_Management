import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { planService } from '../../services/agreementService'
import { Alert, Badge, Button, Card, CardContent, CardHeader, ConfirmationModal, EmptyState, PageHeader, Skeleton } from '../../components/ui'
import PlanDetailsModal from '../../components/PlanDetailsModal'

const formatCurrency = (amount, currency = 'INR') => {
  return `₹${Number(amount || 0).toLocaleString()}${currency !== 'INR' ? ` ${currency}` : ''}`
}

const getPlanCardSummary = (plan) => {
  const charges = plan.charges || {}
  const baseRent = Number(plan.rentDetails?.monthlyRent) || 0
  const securityDeposit = Number(charges.securityDeposit?.amount) || 0
  const oneTimeMaintenance = Number(charges.maintenanceCharges?.oneTimeMaintenanceCharge?.amount) || 0
  const recurringCharges = [
    charges.cleaningCharges?.monthlyCleaningCharge?.amount,
    charges.maintenanceCharges?.monthlyMaintenanceCharge?.amount,
    charges.utilityCharges?.electricity?.fixedAmount,
    charges.utilityCharges?.water?.monthlyAmount,
  ].reduce((total, amount) => total + (Number(amount) || 0), 0)
  const customOneTime = charges.customCharges?.oneTimeCharges || plan.oneTimeCharges || []
  const refundableCustomCharges = customOneTime
    .filter((charge) => charge.refundable)
    .reduce((total, charge) => total + (Number(charge.amount) || 0), 0)
  const nonRefundableCustomCharges = customOneTime
    .filter((charge) => !charge.refundable)
    .reduce((total, charge) => total + (Number(charge.amount) || 0), 0)
  const customRecurringCharges = (charges.customCharges?.monthlyRecurringCharges || plan.monthlyRecurringCharges || [])
    .reduce((total, charge) => total + (Number(charge.amount) || 0), 0)
  const installments = Number(plan.paymentModel?.installments) || 1
  const duration = Number(plan.duration?.value) || 12
  const installmentAmount = (baseRent + recurringCharges + customRecurringCharges) * Math.ceil(duration / installments)
  const refundableAmount = securityDeposit + refundableCustomCharges

  return {
    installments,
    installmentAmount,
    refundableAmount,
    activationAmount: installmentAmount + refundableAmount + oneTimeMaintenance + nonRefundableCustomCharges,
  }
}

export default function Plans() {
  const navigate = useNavigate()
  const [plans, setPlans] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [deletingId, setDeletingId] = useState(null)
  const [activatingId, setActivatingId] = useState(null)
  const [deactivatingId, setDeactivatingId] = useState(null)
  const [selectedPlan, setSelectedPlan] = useState(null)
  const [showDetailsModal, setShowDetailsModal] = useState(false)
  const [searchParams, setSearchParams] = useSearchParams()
  const [searchQuery, setSearchQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  
  // Confirmation modal states
  const [showConfirmModal, setShowConfirmModal] = useState(false)
  const [confirmConfig, setConfirmConfig] = useState({
    message: '',
    onConfirm: null,
    variant: 'default',
    confirmText: 'OK',
    cancelText: 'Cancel'
  })

  useEffect(() => { 
    let isMounted = true
    
    const loadPlans = async () => {
      if (isMounted) {
        await fetchPlans()
      }
    }
    
    loadPlans()
    
    return () => {
      isMounted = false
    }
  }, [])

  // Handle URL parameters to auto-open plan details
  useEffect(() => {
    const planId = searchParams.get('planId')
    if (planId && plans.length > 0) {
      const plan = plans.find(p => p.id === planId)
      if (plan) {
        handleViewPlan(plan)
        // Remove the planId from URL after opening the modal
        setSearchParams({})
      }
    }
  }, [plans, searchParams, setSearchParams])

  const fetchPlans = async () => {
    try {
      setLoading(true)
      setError('')
      const res = await planService.getMyPlans()
      const plansData = res.data || []
      
      // Sort plans by updated date in descending order (most recent first)
      const sortedPlans = plansData.sort((a, b) => {
        const dateA = new Date(a.audit?.updatedAt || a.audit?.createdAt || 0)
        const dateB = new Date(b.audit?.updatedAt || b.audit?.createdAt || 0)
        return dateB - dateA // Descending order
      })
      
      setPlans(sortedPlans)
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to load plans.')
    } finally {
      setLoading(false)
    }
  }

  // Helper function to show confirmation dialog
  const showConfirmation = (message, onConfirm, options = {}) => {
    setConfirmConfig({
      message,
      onConfirm,
      variant: options.variant || 'default',
      confirmText: options.confirmText || 'OK',
      cancelText: options.cancelText || 'Cancel'
    })
    setShowConfirmModal(true)
  }

  const handleConfirmAction = () => {
    if (confirmConfig.onConfirm) {
      confirmConfig.onConfirm()
    }
    setShowConfirmModal(false)
    setConfirmConfig({ message: '', onConfirm: null, variant: 'default', confirmText: 'OK', cancelText: 'Cancel' })
  }

  const handleCancelAction = () => {
    setShowConfirmModal(false)
    setConfirmConfig({ message: '', onConfirm: null, variant: 'default', confirmText: 'OK', cancelText: 'Cancel' })
  }

  const handleDelete = async (planId) => {
    const plan = plans.find(p => p.id === planId);
    if (plan && plan.inUseFlag === 1) {
      setError('Cannot delete plan that is currently in use by agreements.');
      return;
    }
    
    showConfirmation(
      'Delete this plan? It will be permanently removed and cannot be recovered.',
      async () => {
        try {
          setDeletingId(planId)
          await planService.deletePlan(planId)
          setPlans(prev => prev.filter(p => p.id !== planId))
        } catch (err) {
          setError(err?.response?.data?.message || 'Failed to delete plan.')
        } finally {
          setDeletingId(null)
        }
      },
      { variant: 'danger', confirmText: 'Delete', cancelText: 'Cancel' }
    )
  }

  const handleActivate = async (planId) => {
    try {
      setActivatingId(planId)
      await planService.activatePlan(planId)
      setPlans(prev => prev.map(p => p.id === planId ? { ...p, isActive: true } : p))
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to activate plan.')
    } finally {
      setActivatingId(null)
    }
  }

  const handleDeactivate = async (planId) => {
    const plan = plans.find(p => p.id === planId);
    if (plan && plan.inUseFlag === 1) {
      setError('Cannot deactivate plan that is currently in use by agreements.');
      return;
    }
    
    showConfirmation(
      'Deactivate this plan? It will no longer appear in agreement creation.',
      async () => {
        try {
          setDeactivatingId(planId)
          await planService.deactivatePlan(planId)
          setPlans(prev => prev.map(p => p.id === planId ? { ...p, isActive: false } : p))
        } catch (err) {
          setError(err?.response?.data?.message || 'Failed to deactivate plan.')
        } finally {
          setDeactivatingId(null)
        }
      },
      { variant: 'warning', confirmText: 'Deactivate', cancelText: 'Cancel' }
    )
  }

  const handleViewPlan = (plan) => {
    console.log('handleViewPlan called with plan:', plan)
    console.log('Setting selectedPlan to:', plan)
    console.log('Setting showDetailsModal to true')
    setSelectedPlan(plan)
    setShowDetailsModal(true)
    console.log('State should be updated now')
  }

  const handleCloseDetailsModal = () => {
    setShowDetailsModal(false)
    setSelectedPlan(null)
  }

  const handleEditPlan = (plan) => {
    navigate('/owner/plans/edit', { state: { plan } })
  }

  const visiblePlans = plans.filter((plan) => {
    const matchesSearch = !searchQuery.trim() || plan.planName?.toLowerCase().includes(searchQuery.trim().toLowerCase())
    const matchesStatus = statusFilter === 'ALL' ||
      (statusFilter === 'ACTIVE' && plan.isActive) ||
      (statusFilter === 'INACTIVE' && !plan.isActive) ||
      (statusFilter === 'IN_USE' && plan.inUseFlag === 1)
    return matchesSearch && matchesStatus
  })

  if (loading) {
    return (
      <div className="space-y-4">
        {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-48 rounded-3xl" />)}
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {showDetailsModal && selectedPlan && (
        <PlanDetailsModal plan={selectedPlan} onClose={handleCloseDetailsModal} />
      )}

      <ConfirmationModal
        isOpen={showConfirmModal}
        onClose={handleCancelAction}
        onConfirm={handleConfirmAction}
        message={confirmConfig.message}
        variant={confirmConfig.variant}
        confirmText={confirmConfig.confirmText}
        cancelText={confirmConfig.cancelText}
      />

      <PageHeader
        title="Tenant Plans"
        toolbar={
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
            <input
              type="search"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              placeholder="Search plans..."
              className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-900 outline-none transition focus:border-sky-500 focus:ring-2 focus:ring-sky-100 sm:w-56"
            />
            <select
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value)}
              aria-label="Filter plans"
              className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm font-medium text-slate-700 outline-none transition focus:border-sky-500 focus:ring-2 focus:ring-sky-100"
            >
              <option value="ALL">Filter</option>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
              <option value="IN_USE">In use</option>
            </select>
            <Button label="Create plan" onClick={() => navigate('/owner/plans/create')} />
          </div>
        }
      />

      {error ? <Alert tone="error">{error}</Alert> : null}

      {plans.length === 0 ? (
        <EmptyState
          title="No custom plans yet"
          description="Create your first plan to use it when setting up tenant agreements."
          actionLabel="Create plan"
          onAction={() => navigate('/owner/plans/create')}
        />
      ) : visiblePlans.length === 0 ? (
        <EmptyState
          title="No matching plans"
          description="Try a different search term or filter to find a plan."
        />
      ) : (
        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
          {visiblePlans.map((plan) => (
            <Card
              key={plan.id}
              className="cursor-pointer rounded-2xl border-sky-200 bg-sky-50/60 shadow-none transition hover:border-sky-300 hover:shadow-md"
              onClick={(e) => {
                console.log('Card clicked!', e, plan);
                handleViewPlan(plan);
              }}
            >
              <CardHeader
                title={plan.planName}
                className="grid grid-cols-2 gap-1 px-4 py-4"
                action={
                  <div className="flex flex-wrap items-center justify-end gap-1.5">
                    <Badge variant={plan.inUseFlag === 1 ? 'warning' : 'success'}>
                      {plan.inUseFlag === 1 ? 'IN USE' : 'NEW'}
                    </Badge>
                    <Badge variant={plan.isActive ? 'success' : 'neutral'}>
                      {plan.isActive ? 'ACTIVE' : 'INACTIVE'}
                    </Badge>
                  </div>
                }
              />
              <CardContent className="px-4 py-3 pt-0">
                {(() => {
                  const summary = getPlanCardSummary(plan)
                  const currency = plan.rentDetails?.currency || 'INR'

                  return (
                    <>

                    <div className="grid grid-cols-2 gap-x-3 gap-y-2 border-t border-sky-100 pt-3 pb-3 text-xs">
                      <PlanCardValue label="Duration" value={plan.duration ? `${plan.duration.value} ${plan.duration.unit}(s)` : 'Not specified'} />
                      <PlanCardValue label="Plan type" value={plan.planType || 'ROOM_AGREEMENT'} />
                      <PlanCardValue label="Installments" value={`${summary.installments} installment${summary.installments === 1 ? '' : 's'}`} />
                      <PlanCardValue label="Installment amount" value={formatCurrency(summary.installmentAmount, currency)} emphasis="text-sky-700" />
                      <PlanCardValue label="Activation amount" value={formatCurrency(summary.activationAmount, currency)} />
                      <PlanCardValue label="Refundable amount" value={formatCurrency(summary.refundableAmount, currency)} emphasis="text-emerald-700" />
                    </div>

                    <div className="flex w-full gap-3 border-t border-sky-100 pt-3 pb-0 text-xs">

                    {/* Edit and Delete buttons only for plans not in use */}
                    {plan.inUseFlag === 0 && (
                      <>
                        <Button
                          className="flex-1"
                          label="Edit"
                          variant="secondary"
                          size="sm"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleEditPlan(plan);
                          }}
                        />
                        <Button
                          className="flex-1"
                          label="Delete"
                          variant="danger"
                          size="sm"
                          loading={deletingId === plan.id}
                          onClick={(e) => {
                            e.stopPropagation();
                            handleDelete(plan.id);
                          }}
                        />
                      </>
                    )}
                    
                    {/* Activate/Deactivate buttons for all plans */}
                    {plan.isActive ? (
                      <Button
                        className="flex-1"
                        label="Deactivate"
                        variant="secondary"
                        size="sm"
                        loading={deactivatingId === plan.id}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDeactivate(plan.id);
                        }}
                      />
                    ) : (
                      <Button
                        className="flex-1"
                        label="Activate"
                        variant="success"
                        size="sm"
                        loading={activatingId === plan.id}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleActivate(plan.id);
                        }}
                      />
                    )}
                    </div>
                    </>
                  )
                })()}
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}

function PlanCardValue({ label, value, emphasis = 'text-slate-950' }) {
  return (
    <div className="min-w-0">
      <p className="font-semibold text-slate-500">{label}</p>
      <p className={`mt-0.5 truncate font-semibold ${emphasis}`} title={value}>{value}</p>
    </div>
  )
}
