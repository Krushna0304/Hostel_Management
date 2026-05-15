import { useEffect, useState } from 'react'
import { planService } from '../../services/agreementService'
import { Alert, Badge, Button, Card, CardContent, CardHeader, EmptyState, PageHeader, Skeleton } from '../../components/ui'
import CreatePlanModal from './CreatePlanModal'
import PlanDetailsModal from '../../components/PlanDetailsModal'

export default function Plans() {
  const [plans, setPlans] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showModal, setShowModal] = useState(false)
  const [deletingId, setDeletingId] = useState(null)
  const [selectedPlan, setSelectedPlan] = useState(null)
  const [showDetailsModal, setShowDetailsModal] = useState(false)

  useEffect(() => { fetchPlans() }, [])

  const fetchPlans = async () => {
    try {
      setLoading(true)
      setError('')
      const res = await planService.getMyPlans()
      setPlans(res.data || [])
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to load plans.')
    } finally {
      setLoading(false)
    }
  }

  const handleDelete = async (planId) => {
    if (!window.confirm('Deactivate this plan? It will no longer appear in agreement creation.')) return
    try {
      setDeletingId(planId)
      await planService.deletePlan(planId)
      setPlans(prev => prev.filter(p => p.id !== planId))
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to delete plan.')
    } finally {
      setDeletingId(null)
    }
  }

  const handleCreated = (newPlan) => {
    setPlans(prev => [newPlan, ...prev])
    setShowModal(false)
  }

  const handleViewPlan = (plan) => {
    setSelectedPlan(plan)
    setShowDetailsModal(true)
  }

  const handleCloseDetailsModal = () => {
    setShowDetailsModal(false)
    setSelectedPlan(null)
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
      {showModal && (
        <CreatePlanModal onClose={() => setShowModal(false)} onCreated={handleCreated} />
      )}

      {showDetailsModal && selectedPlan && (
        <PlanDetailsModal plan={selectedPlan} onClose={handleCloseDetailsModal} />
      )}

      <PageHeader
        eyebrow="Owner workspace"
        title="Tenant plans"
        description="Create and manage custom rent plans. Plans you create are visible only to you."
        action={<Button label="Create plan" onClick={() => setShowModal(true)} />}
      />

      {error ? <Alert tone="error">{error}</Alert> : null}

      {plans.length === 0 ? (
        <EmptyState
          title="No custom plans yet"
          description="Create your first plan to use it when setting up tenant agreements."
          actionLabel="Create plan"
          onAction={() => setShowModal(true)}
        />
      ) : (
        <div className="grid gap-4 xl:grid-cols-2">
          {plans.map((plan) => (
            <Card key={plan.id}>
              <CardHeader
                title={plan.planName}
                description={`${plan.planType || 'ROOM_AGREEMENT'}`}
                action={
                  <div className="flex items-center gap-2">
                    <Badge variant="success">ACTIVE</Badge>
                    <Button
                      label="View"
                      variant="secondary"
                      size="sm"
                      onClick={() => handleViewPlan(plan)}
                    />
                    <Button
                      label="Delete"
                      variant="danger"
                      size="sm"
                      loading={deletingId === plan.id}
                      onClick={() => handleDelete(plan.id)}
                    />
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
                  {plan.charges?.securityDeposit?.amount && (
                    <div className="rounded-2xl bg-slate-50 px-4 py-3">
                      <p className="text-xs uppercase tracking-[0.15em] text-slate-400">Security Deposit</p>
                      <p className="mt-1 font-semibold text-slate-950">₹{plan.charges.securityDeposit.amount}</p>
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
