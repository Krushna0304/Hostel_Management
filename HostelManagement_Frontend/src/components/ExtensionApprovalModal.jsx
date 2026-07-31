import { useState, useEffect } from 'react'
import Swal from 'sweetalert2'
import { Button } from './ui/Button'
import { Alert } from './ui/Alert'
import { Badge } from './ui/Badge'
import extensionService from '../services/extensionService'
import { useSuccessPopup } from '../hooks/useSuccessPopup'

/**
 * ExtensionApprovalModal
 *
 * Owner-facing modal to view and approve/reject pending allotment extension
 * requests submitted by tenants.
 *
 * Props:
 *   request   - The pending extension request object
 *   onClose   - Callback to close the modal
 *   onSuccess - Callback invoked after a successful approve/reject action
 */
export default function ExtensionApprovalModal({ request, onClose, onSuccess }) {
  const [ownerNotes, setOwnerNotes] = useState('')
  const [processing, setProcessing] = useState(false)
  const [error, setError] = useState('')
  const { showSuccess } = useSuccessPopup()

  // Reset state whenever a new request is loaded
  useEffect(() => {
    if (request) {
      setOwnerNotes('')
      setError('')
    }
  }, [request])

  if (!request) return null

  const formatDate = (d) => {
    if (!d) return 'N/A'
    return new Date(d).toLocaleDateString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    })
  }

  const statusVariant = (status) => {
    switch (status) {
      case 'PENDING':
      case 'PENDING_APPROVAL':
        return 'warning'
      case 'APPROVED':
        return 'success'
      case 'REJECTED':
        return 'danger'
      default:
        return 'neutral'
    }
  }

  const handleApprove = async () => {
    const confirmed = await Swal.fire({
      title: 'Approve Extension Request?',
      html: `
        <div style="text-align:left;font-size:15px;">
          <p style="margin-bottom:8px;">Tenant: <strong>${request.tenantName || 'N/A'}</strong></p>
          <p style="margin-bottom:8px;">Room: <strong>${request.roomNumber || 'N/A'}</strong></p>
          <p style="margin-bottom:8px;">Plan: <strong>${request.planName || 'N/A'}</strong></p>
          ${ownerNotes ? `<p>Notes: <em>${ownerNotes}</em></p>` : ''}
        </div>
      `,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Yes, Approve',
      cancelButtonText: 'Cancel',
      confirmButtonColor: '#059669',
      cancelButtonColor: '#94a3b8',
    })

    if (!confirmed.isConfirmed) return

    setProcessing(true)
    setError('')
    try {
      await extensionService.approveExtensionRequest(request.id, {
        approved: true,
        ownerNotes: ownerNotes || null,
      })
      showSuccess('Extension request approved successfully!')
      onSuccess?.()
      onClose()
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          err?.response?.data ||
          'Failed to approve the extension request. Please try again.'
      )
    } finally {
      setProcessing(false)
    }
  }

  const handleReject = async () => {
    const confirmed = await Swal.fire({
      title: 'Reject Extension Request?',
      html: `
        <div style="text-align:left;font-size:15px;">
          <p style="margin-bottom:8px;">Tenant: <strong>${request.tenantName || 'N/A'}</strong></p>
          <p style="margin-bottom:8px;">Room: <strong>${request.roomNumber || 'N/A'}</strong></p>
          <p>This action will notify the tenant that their request was not approved.</p>
        </div>
      `,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Yes, Reject',
      cancelButtonText: 'Cancel',
      confirmButtonColor: '#e11d48',
      cancelButtonColor: '#94a3b8',
    })

    if (!confirmed.isConfirmed) return

    setProcessing(true)
    setError('')
    try {
      await extensionService.approveExtensionRequest(request.id, {
        approved: false,
        ownerNotes: ownerNotes || null,
      })
      showSuccess('Extension request rejected.')
      onSuccess?.()
      onClose()
    } catch (err) {
      setError(
        err?.response?.data?.message ||
          err?.response?.data ||
          'Failed to reject the extension request. Please try again.'
      )
    } finally {
      setProcessing(false)
    }
  }

  const plan = request.planSnapshot || {}

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4"
      onClick={onClose}
    >
      <div
        className="relative w-full max-w-lg max-h-[90vh] overflow-y-auto rounded-3xl bg-white shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        {/* ── Header ── */}
        <div className="sticky top-0 z-10 flex items-start justify-between bg-white px-6 py-4 border-b border-slate-100 rounded-t-3xl">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">
              Extension request
            </p>
            <h2 className="mt-1 text-xl font-bold text-slate-950">Review & Approve</h2>
          </div>
          <div className="flex items-center gap-3">
            <Badge variant={statusVariant(request.status)}>
              {(request.status || 'PENDING').replaceAll('_', ' ')}
            </Badge>
            <button
              onClick={onClose}
              disabled={processing}
              className="rounded-full p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition"
              aria-label="Close modal"
            >
              ✕
            </button>
          </div>
        </div>

        <div className="px-6 py-5 space-y-4">
          {error && (
            <Alert tone="error">{error}</Alert>
          )}

          {/* ── Tenant & Room Info ── */}
          <div className="rounded-2xl bg-blue-50 border border-blue-200 p-4">
            <p className="text-sm font-semibold text-blue-900 mb-3">👤 Tenant & Room Details</p>
            <div className="grid grid-cols-2 gap-3">
              <div className="rounded-xl bg-white border border-blue-200 px-3 py-2">
                <p className="text-xs uppercase tracking-[0.15em] text-blue-500">Tenant name</p>
                <p className="mt-1 font-semibold text-blue-900 text-sm">{request.tenantName || 'N/A'}</p>
              </div>
              <div className="rounded-xl bg-white border border-blue-200 px-3 py-2">
                <p className="text-xs uppercase tracking-[0.15em] text-blue-500">Mobile</p>
                <p className="mt-1 font-semibold text-blue-900 text-sm">{request.tenantMobileNumber || request.tenantPhone || 'N/A'}</p>
              </div>
              <div className="rounded-xl bg-white border border-blue-200 px-3 py-2">
                <p className="text-xs uppercase tracking-[0.15em] text-blue-500">Room</p>
                <p className="mt-1 font-semibold text-blue-900 text-sm">
                  {request.roomNumber
                    ? `Room ${request.roomNumber}${request.floorNumber ? `, Floor ${request.floorNumber}` : ''}`
                    : 'N/A'}
                </p>
              </div>
              <div className="rounded-xl bg-white border border-blue-200 px-3 py-2">
                <p className="text-xs uppercase tracking-[0.15em] text-blue-500">Hostel</p>
                <p className="mt-1 font-semibold text-blue-900 text-sm">{request.hostelName || 'N/A'}</p>
              </div>
            </div>
          </div>

          {/* ── Current Allotment ── */}
          <div className="rounded-2xl bg-slate-50 p-4">
            <p className="text-sm font-semibold text-slate-900 mb-3">📅 Current Allotment</p>
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div>
                <p className="text-xs text-slate-500">Allotment end date</p>
                <p className="font-semibold text-slate-900 mt-0.5">{formatDate(request.currentEndDate || request.allotmentEndDate)}</p>
              </div>
              <div>
                <p className="text-xs text-slate-500">Requested on</p>
                <p className="font-semibold text-slate-900 mt-0.5">{formatDate(request.createdAt || request.requestedAt)}</p>
              </div>
            </div>
          </div>

          {/* ── Requested Extension Plan ── */}
          <div className="rounded-2xl bg-green-50 border border-green-200 p-4">
            <p className="text-sm font-semibold text-green-900 mb-3">📋 Requested Extension Plan</p>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-green-700 font-medium">Plan name</span>
                <span className="text-green-900 font-semibold">{request.planName || plan.planName || 'N/A'}</span>
              </div>
              {(plan.duration || request.planDuration) && (
                <div className="flex justify-between">
                  <span className="text-green-700 font-medium">Duration</span>
                  <span className="text-green-900 font-semibold">
                    {plan.duration
                      ? `${plan.duration.value} ${plan.duration.unit?.toLowerCase()}${plan.duration.value > 1 ? 's' : ''}`
                      : request.planDuration}
                  </span>
                </div>
              )}
              {(plan.rentDetails?.monthlyRent || request.monthlyRent) && (
                <div className="flex justify-between">
                  <span className="text-green-700 font-medium">Monthly rent</span>
                  <span className="text-green-900 font-semibold">
                    ₹{(plan.rentDetails?.monthlyRent || request.monthlyRent || 0).toLocaleString()}
                  </span>
                </div>
              )}
              {(plan.charges?.securityDeposit?.amount || request.securityDeposit) && (
                <div className="flex justify-between">
                  <span className="text-green-700 font-medium">Security deposit</span>
                  <span className="text-green-900 font-semibold">
                    ₹{(plan.charges?.securityDeposit?.amount || request.securityDeposit || 0).toLocaleString()}
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* ── Tenant Notes ── */}
          {request.tenantNotes && (
            <div className="rounded-2xl bg-amber-50 border border-amber-200 p-4">
              <p className="text-sm font-semibold text-amber-900 mb-1">💬 Tenant notes</p>
              <p className="text-sm text-amber-800">{request.tenantNotes}</p>
            </div>
          )}

          {/* ── Owner Notes ── */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">
              Owner notes <span className="text-slate-400 font-normal">(optional)</span>
            </label>
            <textarea
              value={ownerNotes}
              onChange={(e) => setOwnerNotes(e.target.value)}
              disabled={processing}
              rows={3}
              maxLength={500}
              placeholder="Add any remarks or conditions for the tenant..."
              className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 shadow-sm outline-none transition placeholder:text-slate-400 focus:border-sky-400 focus:ring-4 focus:ring-sky-100 resize-none disabled:bg-slate-50 disabled:opacity-60"
            />
            <p className="text-xs text-slate-500 mt-1">
              These notes will be visible to the tenant after your decision.
            </p>
          </div>

          {/* ── Info box ── */}
          <div className="p-4 bg-blue-50 rounded-2xl border border-blue-200">
            <p className="text-sm font-semibold text-blue-800 mb-1">ℹ️ What happens next</p>
            <ul className="text-sm text-blue-700 space-y-1">
              <li>• Approving creates a new agreement for the extension period</li>
              <li>• The tenant will be notified and prompted to complete payment</li>
              <li>• The room will be held until payment is completed</li>
            </ul>
          </div>
        </div>

        {/* ── Footer ── */}
        <div className="sticky bottom-0 bg-white border-t border-slate-100 px-6 py-4 rounded-b-3xl">
          <div className="flex gap-3">
            <Button
              label="Cancel"
              onClick={onClose}
              variant="secondary"
              disabled={processing}
              className="flex-1"
            />
            <Button
              label="Reject"
              onClick={handleReject}
              variant="danger"
              loading={processing}
              className="flex-1"
            />
            <Button
              label="✓ Approve"
              onClick={handleApprove}
              variant="success"
              loading={processing}
              className="flex-1"
            />
          </div>
        </div>
      </div>
    </div>
  )
}
