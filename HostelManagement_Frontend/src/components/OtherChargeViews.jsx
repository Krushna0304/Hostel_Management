import { Badge, Button, EmptyState, Skeleton } from './ui'

export function buildOtherChargeTenantRows(charges) {
  const tenantMap = new Map()
  const add = (tenant, charge, amount, status) => {
    const key = `${tenant.tenantId || tenant.tenantName}-${charge.roomNumber}`
    const row = tenantMap.get(key) || {
      tenantId: tenant.tenantId,
      tenantName: tenant.tenantName,
      hostelName: charge.hostelName || 'N/A',
      roomNumber: charge.roomNumber,
      charges: [], pendingCharges: 0, outstandingAmount: 0, overdueAmount: 0,
    }
    row.charges.push(charge)
    if (status !== 'COMPLETED') {
      row.pendingCharges += 1
      row.outstandingAmount += amount
      if (charge.dueDate && new Date(charge.dueDate) < new Date()) row.overdueAmount += amount
    }
    tenantMap.set(key, row)
  }

  charges.forEach((charge) => {
    if (charge.category === 'OTHER_CHARGE_TENANT') {
      add({ tenantId: charge.tenantId, tenantName: charge.tenantName }, charge, charge.remainingAmount ?? charge.amount, charge.paymentStatus)
    } else if (charge.category === 'OTHER_CHARGE_ROOM') {
      charge.roomTenants?.forEach((tenant) => add(tenant, charge, tenant.splitAmount, tenant.paymentStatus))
    }
  })
  return Array.from(tenantMap.values())
}

export function OtherChargeTenantTable({ loading, rows, onHistory, onCollect }) {
  const fmt = (amount) => `₹${Number(amount || 0).toLocaleString()}`
  if (loading) return <div className="p-6 space-y-4">{Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-16 rounded-lg" />)}</div>
  if (!rows.length) return <EmptyState title="No tenants with charges" description="Additional charges will appear here when you create them for tenants." />

  return <div className="overflow-x-auto"><table className="w-full text-sm">
    <thead><tr className="border-b border-slate-100 text-left text-xs font-semibold uppercase tracking-[0.15em] text-slate-400">
      <th className="pb-3 pr-4 pl-6">Tenant</th><th className="pb-3 pr-4">Hostel / Room</th><th className="pb-3 pr-4">Pending charges</th><th className="pb-3 pr-4">Outstanding</th><th className="pb-3 pr-4">Overdue amount</th><th className="pb-3 text-center pr-6">Actions</th>
    </tr></thead>
    <tbody className="divide-y divide-slate-100">{rows.map((tenant) => <tr key={`${tenant.tenantId}-${tenant.roomNumber}`} className="hover:bg-slate-50">
      <td className="py-3 pr-4 pl-6"><p className="font-semibold text-slate-950">{tenant.tenantName}</p></td>
      <td className="py-3 pr-4 text-slate-600">{tenant.hostelName} · Room {tenant.roomNumber}</td>
      <td className="py-3 pr-4">{tenant.pendingCharges > 0 ? <Badge variant="warning">{tenant.pendingCharges} pending</Badge> : <Badge variant="success">All paid</Badge>}</td>
      <td className="py-3 pr-4 font-semibold text-slate-950">{tenant.outstandingAmount > 0 ? fmt(tenant.outstandingAmount) : '—'}</td>
      <td className="py-3 pr-4 font-semibold text-red-600">{tenant.overdueAmount > 0 ? fmt(tenant.overdueAmount) : '—'}</td>
      <td className="py-3 text-center pr-6"><div className="flex items-center justify-center gap-2"><Button variant="secondary" size="sm" onClick={() => onHistory(tenant)}>History</Button>{tenant.outstandingAmount > 0 ? <Button variant="primary" size="sm" onClick={() => onCollect(tenant)}>Collect</Button> : <span className="text-sm text-slate-500">No pending</span>}</div></td>
    </tr>)}</tbody>
  </table></div>
}

export function OtherChargeCards({ loading, charges, onDetails, onCollect }) {
  const fmt = (amount) => `₹${Number(amount || 0).toLocaleString()}`
  if (loading) return <div className="space-y-4">{Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-32 rounded-lg" />)}</div>
  if (!charges.length) return <EmptyState title="No other charges" description="Additional charges will appear here when you create them." />
  return (
    <div className="space-y-4">
      {charges.map((charge) => (
        <div key={charge.chargeId} className="rounded-lg border border-slate-200 p-4 transition-shadow hover:shadow-sm">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
            <div className="min-w-0 flex-1">
              <div className="mb-2 flex flex-wrap items-center gap-2">
                <h3 className="min-w-0 break-words font-semibold text-slate-900">{charge.chargeName}</h3>
                <Badge variant={charge.paymentStatus === 'COMPLETED' ? 'success' : charge.paymentStatus === 'OVERDUE' ? 'danger' : charge.paymentStatus === 'PARTIALLY_PAID' ? 'warning' : 'neutral'}>{charge.paymentStatus === 'COMPLETED' ? 'Paid' : charge.paymentStatus === 'OVERDUE' ? 'Overdue' : charge.paymentStatus === 'PARTIALLY_PAID' ? 'Partial' : 'Pending'}</Badge>
                <Badge variant={charge.category === 'OTHER_CHARGE_TENANT' ? 'primary' : 'secondary'}>{charge.category === 'OTHER_CHARGE_TENANT' ? 'Tenant' : 'Room'}</Badge>
                {charge.installmentEnabled && <Badge variant="info">Installments</Badge>}
              </div>
              {charge.description && <p className="mb-3 break-words text-sm text-slate-600">{charge.description}</p>}
              <div className="grid grid-cols-2 gap-4 text-sm md:grid-cols-4">
                <div><span className="text-slate-500">Amount:</span><div className="font-medium">{fmt(charge.amount)}</div></div>
                {charge.paymentStatus !== 'COMPLETED' && <div><span className="text-slate-500">Remaining:</span><div className="font-medium text-orange-600">{fmt(charge.remainingAmount)}</div></div>}
                <div className="min-w-0"><span className="text-slate-500">Target:</span><div className="break-words font-medium">{charge.category === 'OTHER_CHARGE_TENANT' ? `👤 ${charge.tenantName}` : `🏠 Room ${charge.roomNumber} (${charge.roomTenants?.length || 0} tenants)`}</div></div>
                {charge.dueDate && <div><span className="text-slate-500">Due Date:</span><div className="font-medium">{new Date(charge.dueDate).toLocaleDateString()}</div></div>}
              </div>
            </div>
            <div className="flex w-full flex-col gap-2 sm:w-auto sm:flex-row lg:shrink-0">
              <Button label="View Details" variant="secondary" size="sm" className="w-full sm:w-auto" onClick={() => onDetails(charge)} />
              {charge.paymentStatus !== 'COMPLETED' && <Button label="Collect Payment" variant="primary" size="sm" className="w-full sm:w-auto" onClick={() => onCollect(charge)} />}
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}
