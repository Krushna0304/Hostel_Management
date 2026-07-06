import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { planService } from '../../services/agreementService'
import { Alert, Badge, Button, Card, CardContent, CardHeader, ConfirmationModal, EmptyState, PageHeader, Skeleton } from '../../components/ui'
import PlanDetailsModal from '../../components/PlanDetailsModal'

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

  if (loading) {
    return (
      <div className="space-y-4">
        {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-48 rounded-3xl" />)}
      </div>
    )
  }

  return (
    <div className="space-y-8">
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
        eyebrow="Owner workspace"
        title="Tenant plans"
        description="Create and manage custom rent plans. Plans you create are visible only to you."
        action={<Button label="Create plan" onClick={() => navigate('/owner/plans/create')} />}
      />

      {error ? <Alert tone="error">{error}</Alert> : null}

      {plans.length === 0 ? (
        <EmptyState
          title="No custom plans yet"
          description="Create your first plan to use it when setting up tenant agreements."
          actionLabel="Create plan"
          onAction={() => navigate('/owner/plans/create')}
        />
      ) : (
        <div className="grid gap-4 xl:grid-cols-2">
          {plans.map((plan) => (
            <Card 
              key={plan.id} 
              className="cursor-pointer hover:shadow-lg transition-shadow duration-200"
              onClick={(e) => {
                console.log('Card clicked!', e, plan);
                handleViewPlan(plan);
              }}
            >
              <CardHeader
                title={plan.planName}
                description={`${plan.planType || 'ROOM_AGREEMENT'}`}
                action={
                  <div className="flex items-center gap-2">
                    <Badge variant={plan.inUseFlag === 1 ? 'warning' : 'success'}>
                      {plan.inUseFlag === 1 ? 'IN USE' : 'NEW'}
                    </Badge>
                    <Badge variant={plan.isActive ? 'success' : 'neutral'}>
                      {plan.isActive ? 'ACTIVE' : 'INACTIVE'}
                    </Badge>
                    
                    {/* Edit and Delete buttons only for plans not in use */}
                    {plan.inUseFlag === 0 && (
                      <>
                        <Button
                          label="Edit"
                          variant="secondary"
                          size="sm"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleEditPlan(plan);
                          }}
                        />
                        <Button
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
                        label="Deactivate"
                        variant="warning"
                        size="sm"
                        loading={deactivatingId === plan.id}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDeactivate(plan.id);
                        }}
                      />
                    ) : (
                      <Button
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
                }
              />
              <CardContent>
                <div className="grid grid-cols-2 gap-3 text-sm">
                  {plan.rentDetails?.monthlyRent && (
                    <div className="rounded-2xl bg-slate-50 px-4 py-3">
                      <p className="text-xs uppercase tracking-[0.15em] text-slate-400">Base Rent</p>
                      <p className="mt-1 font-semibold text-slate-950">₹{plan.rentDetails.monthlyRent} {plan.rentDetails.currency}</p>
                    </div>
                  )}
                  {plan.charges?.securityDeposit !== undefined && (
                    <div className="rounded-2xl bg-slate-50 px-4 py-3">
                      <p className="text-xs uppercase tracking-[0.15em] text-slate-400">Security Deposit</p>
                      <p className="mt-1 font-semibold text-slate-950">₹{plan.charges.securityDeposit.amount || 0}</p>
                    </div>
                  )}
                  {plan.duration && (
                    <div className="rounded-2xl bg-slate-50 px-4 py-3">
                      <p className="text-xs uppercase tracking-[0.15em] text-slate-400">Duration</p>
                      <p className="mt-1 font-semibold text-slate-950">{plan.duration.value} {plan.duration.unit}(s)</p>
                    </div>
                  )}
                  {plan.paymentModel?.mode && (
                    <div className="rounded-2xl bg-slate-50 px-4 py-3">
                      <p className="text-xs uppercase tracking-[0.15em] text-slate-400">Payment Mode</p>
                      <p className="mt-1 font-semibold text-slate-950">{plan.paymentModel.mode}</p>
                    </div>
                  )}
                </div>

                {/* Monthly Charges Summary */}
                {(plan.charges?.cleaningCharges?.monthlyCleaningCharge?.amount > 0 || 
                  plan.charges?.maintenanceCharges?.monthlyMaintenanceCharge?.amount > 0 ||
                  plan.charges?.utilityCharges?.electricity?.fixedAmount > 0 ||
                  plan.charges?.utilityCharges?.water?.monthlyAmount > 0) && (
                  <div className="mt-3">
                    <p className="text-xs font-semibold uppercase tracking-[0.15em] text-slate-400 mb-2">Monthly Charges</p>
                    <div className="flex flex-wrap gap-2">
                      {plan.charges.cleaningCharges?.monthlyCleaningCharge?.amount > 0 && (
                        <span className="px-2 py-1 bg-blue-100 text-blue-800 rounded-full text-xs">
                          Cleaning: ₹{plan.charges.cleaningCharges.monthlyCleaningCharge.amount}
                        </span>
                      )}
                      {plan.charges.maintenanceCharges?.monthlyMaintenanceCharge?.amount > 0 && (
                        <span className="px-2 py-1 bg-blue-100 text-blue-800 rounded-full text-xs">
                          Maintenance: ₹{plan.charges.maintenanceCharges.monthlyMaintenanceCharge.amount}
                        </span>
                      )}
                      {plan.charges.utilityCharges?.electricity?.fixedAmount > 0 && (
                        <span className="px-2 py-1 bg-blue-100 text-blue-800 rounded-full text-xs">
                          Electricity: ₹{plan.charges.utilityCharges.electricity.fixedAmount}
                        </span>
                      )}
                      {plan.charges.utilityCharges?.water?.monthlyAmount > 0 && (
                        <span className="px-2 py-1 bg-blue-100 text-blue-800 rounded-full text-xs">
                          Water: ₹{plan.charges.utilityCharges.water.monthlyAmount}
                        </span>
                      )}
                    </div>
                  </div>
                )}

                {plan.freeFacilities?.facilities?.length > 0 && (
                  <div className="mt-3">
                    <p className="text-xs font-semibold uppercase tracking-[0.15em] text-slate-400 mb-2">Facilities</p>
                    <div className="flex flex-wrap gap-2">
                      {plan.freeFacilities.facilities.map((f, i) => (
                        <span key={i} className="px-2 py-1 bg-green-100 text-green-800 rounded-full text-xs">{f.name}</span>
                      ))}
                    </div>
                  </div>
                )}

                {plan.customFields && Object.keys(plan.customFields).length > 0 && (
                  <div className="mt-3 rounded-2xl bg-amber-50 border border-amber-200 px-4 py-3">
                    <p className="text-xs font-semibold uppercase tracking-[0.15em] text-amber-700 mb-2">Custom Fields</p>
                    <div className="grid grid-cols-2 gap-2 text-sm">
                      {Object.entries(plan.customFields).map(([key, value]) => (
                        <div key={key}>
                          <span className="text-slate-500">{key}: </span>
                          <span className="font-medium text-slate-900">{String(value)}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}
