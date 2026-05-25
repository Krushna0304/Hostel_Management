import { useEffect, useRef, useState } from 'react'
import { hostelService } from '../../services/hostelService'
import { ArrowLeftIcon, BuildingIcon, LayersIcon, PlusIcon } from '../../components/icons/AppIcons'
import { Alert, Badge, Button, Card, CardContent, CardHeader } from '../../components/ui'
import FormInput from '../../components/FormInput'

// ---------------------------------------------------------------------------
// Add-Hostel Modal
// ---------------------------------------------------------------------------
function AddHostelModal({ isOpen, onClose, onSuccess }) {
  const [formData, setFormData] = useState({ hostelName: '', hostelAddress: '' })
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)
  const [apiError, setApiError] = useState('')
  const firstInputRef = useRef(null)

  // Focus first input when modal opens
  useEffect(() => {
    if (isOpen) {
      setFormData({ hostelName: '', hostelAddress: '' })
      setErrors({})
      setApiError('')
      setTimeout(() => firstInputRef.current?.focus(), 80)
    }
  }, [isOpen])

  // Close on Escape key
  useEffect(() => {
    if (!isOpen) return
    const handleKey = (e) => { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', handleKey)
    return () => window.removeEventListener('keydown', handleKey)
  }, [isOpen, onClose])

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData((prev) => ({ ...prev, [name]: value }))
    if (errors[name]) setErrors((prev) => ({ ...prev, [name]: '' }))
  }

  const validate = () => {
    const errs = {}
    if (!formData.hostelName.trim()) errs.hostelName = 'Hostel name is required'
    if (!formData.hostelAddress.trim()) errs.hostelAddress = 'Hostel address is required'
    return errs
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length) { setErrors(errs); return }

    setLoading(true)
    setApiError('')
    try {
      await hostelService.createHostel({
        hostelName: formData.hostelName.trim(),
        hostelAddress: formData.hostelAddress.trim(),
      })
      onSuccess()   // parent will re-fetch + close modal
    } catch (err) {
      const data = err?.response?.data
      if (data?.message) {
        setApiError(data.message)
      } else if (data && typeof data === 'object') {
        const fieldErrs = {}
        Object.entries(data).forEach(([k, v]) => { if (k !== 'message') fieldErrs[k] = v })
        Object.keys(fieldErrs).length ? setErrors(fieldErrs) : setApiError('Failed to create hostel. Please try again.')
      } else {
        setApiError('Failed to create hostel. Please try again.')
      }
    } finally {
      setLoading(false)
    }
  }

  if (!isOpen) return null

  return (
    /* Backdrop */
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ backgroundColor: 'rgba(15,23,42,0.45)', backdropFilter: 'blur(6px)' }}
      onClick={(e) => { if (e.target === e.currentTarget) onClose() }}
      role="dialog"
      aria-modal="true"
      aria-labelledby="add-hostel-modal-title"
    >
      {/* Panel */}
      <div
        className="w-full max-w-md rounded-3xl border border-white/70 bg-white shadow-[0_32px_80px_-16px_rgba(15,23,42,0.35)]"
        style={{ animation: 'modalSlideUp 0.22s cubic-bezier(0.34,1.56,0.64,1) both' }}
      >
        {/* Modal header */}
        <div className="flex items-start justify-between border-b border-slate-100 px-6 py-5">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-slate-950 text-white">
                <BuildingIcon className="h-4 w-4" />
              </div>
              <h2 id="add-hostel-modal-title" className="text-lg font-semibold text-slate-950">
                Add new hostel
              </h2>
            </div>
            <p className="pl-10 text-sm text-slate-500">
              Capture the property name and address to get started.
            </p>
          </div>
          <button
            onClick={onClose}
            className="ml-4 mt-0.5 rounded-xl p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500/50"
            aria-label="Close modal"
          >
            {/* ✕ icon */}
            <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Modal body */}
        <form onSubmit={handleSubmit} noValidate>
          <div className="space-y-5 px-6 py-5">
            {apiError ? (
              <Alert tone="error" title="Creation failed">{apiError}</Alert>
            ) : null}

            <FormInput
              ref={firstInputRef}
              label="Hostel name"
              name="hostelName"
              type="text"
              value={formData.hostelName}
              onChange={handleChange}
              placeholder="Green Valley Hostel"
              required
              error={errors.hostelName}
              hint="Must be unique in your portfolio. Keep it short and recognizable."
            />

            <FormInput
              label="Hostel address"
              name="hostelAddress"
              type="text"
              value={formData.hostelAddress}
              onChange={handleChange}
              placeholder="Street address, city, state, ZIP"
              required
              error={errors.hostelAddress}
            />
          </div>

          {/* Modal footer */}
          <div className="flex flex-col gap-3 border-t border-slate-100 px-6 py-4 sm:flex-row sm:justify-end">
            <Button
              type="button"
              label="Cancel"
              variant="secondary"
              onClick={onClose}
              className="sm:w-auto"
              fullWidth
            />
            <Button
              type="submit"
              label="Create hostel"
              loading={loading}
              className="sm:w-auto"
              fullWidth
            />
          </div>
        </form>
      </div>

      {/* Keyframe animation injected once */}
      <style>{`
        @keyframes modalSlideUp {
          from { opacity: 0; transform: translateY(24px) scale(0.97); }
          to   { opacity: 1; transform: translateY(0)   scale(1); }
        }
      `}</style>
    </div>
  )
}

// ---------------------------------------------------------------------------
// HostelList  (main export)
// ---------------------------------------------------------------------------
const HostelList = ({ hostels, onHostelClick, onCreateHostel, showQuickStart = true }) => {
  const [modalOpen, setModalOpen] = useState(false)

  const handleSuccess = () => {
    setModalOpen(false)
    // Re-fetch the hostel list by calling the parent refresh callback if provided,
    // otherwise fall back to the navigate-to-create-hostel route.
    if (typeof onCreateHostel === 'function') onCreateHostel('__refresh__')
  }

  return (
    <>
      <Card>
        <CardHeader
          title="Property portfolio"
          description="A clean view of every hostel in your workspace with quick access to floors and room inventory."
        />
        <CardContent>
          <div className={`grid gap-4 ${showQuickStart ? 'lg:grid-cols-2' : 'lg:grid-cols-3'}`}>
            {hostels.map((hostel, index) => (
              <button
                key={hostel.hostelId}
                type="button"
                onClick={() => onHostelClick(hostel)}
                className="group rounded-3xl border border-slate-200 bg-slate-50/90 p-5 text-left transition hover:-translate-y-0.5 hover:border-sky-200 hover:bg-white hover:shadow-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500/50"
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="rounded-2xl bg-white p-3 text-slate-700 shadow-sm">
                    <BuildingIcon className="h-5 w-5" />
                  </div>
                  <Badge variant="info">Hostel {index + 1}</Badge>
                </div>

                <div className="mt-5 space-y-2">
                  <h3 className="text-lg font-semibold text-slate-950">{hostel.hostelName}</h3>
                  <p className="text-sm leading-6 text-slate-500">{hostel.hostelAddress}</p>
                </div>

                <div className="mt-5 flex items-center justify-between border-t border-slate-200 pt-4">
                  <div className="flex items-center gap-2 text-sm text-slate-500">
                    <LayersIcon className="h-4 w-4" />
                    <span>Open floors and room inventory</span>
                  </div>
                  <span className="flex items-center gap-2 text-sm font-semibold text-slate-900">
                    View
                    <ArrowLeftIcon className="h-4 w-4 rotate-180 transition group-hover:translate-x-0.5" />
                  </span>
                </div>
              </button>
            ))}
          </div>
        </CardContent>
      </Card>

      <AddHostelModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        onSuccess={handleSuccess}
      />
    </>
  )
}

export default HostelList
