import { useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  CardContent,
  CardHeader,
  InputField,
  PageHeader,
  Skeleton,
} from '../../components/ui'
import { profileService, getUsernameFromToken } from '../../services/profileService'

// ── helpers ──────────────────────────────────────────────────────────────────

function formatDate(dateStr) {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleDateString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

const EMPTY_FORM = {
  displayName: '',
  phoneNumber: '',
  newPassword: '',
  confirmPassword: '',
}

// ── component ─────────────────────────────────────────────────────────────────

export default function Profile() {
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [fetchError, setFetchError] = useState('')

  const [form, setForm] = useState(EMPTY_FORM)
  const [errors, setErrors] = useState({})
  const [saving, setSaving] = useState(false)
  const [alert, setAlert] = useState(null) // { tone, message }

  const username = getUsernameFromToken()

  // ── fetch profile ──────────────────────────────────────────────────────────
  useEffect(() => {
    if (!username) {
      setFetchError('Unable to identify the current user. Please log in again.')
      setLoading(false)
      return
    }
    fetchProfile()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [username])

  const fetchProfile = async () => {
    try {
      setLoading(true)
      setFetchError('')
      const res = await profileService.getProfile(username)
      const data = res.data
      setProfile(data)
      setForm({
        displayName: data.displayName || '',
        phoneNumber: data.phoneNumber || '',
        newPassword: '',
        confirmPassword: '',
      })
    } catch (err) {
      setFetchError(err?.response?.data?.message || 'Failed to load profile. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  // ── form handling ──────────────────────────────────────────────────────────
  const handleChange = (e) => {
    const { name, value } = e.target
    setForm((prev) => ({ ...prev, [name]: value }))
    if (errors[name]) setErrors((prev) => ({ ...prev, [name]: '' }))
  }

  const validate = () => {
    const errs = {}

    if (!form.displayName.trim()) errs.displayName = 'Display name is required.'
    if (form.displayName.length > 100) errs.displayName = 'Display name must not exceed 100 characters.'

    if (!form.phoneNumber.trim()) errs.phoneNumber = 'Phone number is required.'
    else if (!/^[6-9]\d{9}$/.test(form.phoneNumber))
      errs.phoneNumber = 'Enter a valid 10-digit Indian mobile number.'

    if (form.newPassword) {
      if (form.newPassword.length < 8 || form.newPassword.length > 20)
        errs.newPassword = 'Password must be 8–20 characters.'
      else if (!/^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[@#$%^&+=!]).*$/.test(form.newPassword))
        errs.newPassword =
          'Password must include uppercase, lowercase, a number, and a special character (@#$%^&+=!).'

      if (form.newPassword !== form.confirmPassword)
        errs.confirmPassword = 'Passwords do not match.'
    }

    return errs
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setAlert(null)

    const errs = validate()
    if (Object.keys(errs).length > 0) {
      setErrors(errs)
      return
    }

    setSaving(true)
    try {
      const payload = {
        displayName: form.displayName.trim(),
        phoneNumber: form.phoneNumber.trim(),
      }
      if (form.newPassword) payload.newPassword = form.newPassword

      const res = await profileService.updateProfile(username, payload)
      setProfile(res.data)
      setForm((prev) => ({ ...prev, newPassword: '', confirmPassword: '' }))
      setAlert({ tone: 'success', message: '✅ Profile updated successfully! Your changes have been saved.' })
      
      // Auto-hide success message after 5 seconds
      setTimeout(() => setAlert(null), 5000)
    } catch (err) {
      console.error('Profile update error:', err)
      
      // Handle specific error cases
      let errorMessage = 'Failed to update profile. Please try again.'
      
      if (err?.response?.status === 400) {
        const errorData = err.response.data
        if (typeof errorData === 'string') {
          errorMessage = errorData
        } else if (errorData?.message) {
          errorMessage = errorData.message
        } else if (errorData?.errors) {
          // Handle validation errors
          const validationErrors = Object.values(errorData.errors).join(', ')
          errorMessage = `Validation failed: ${validationErrors}`
        }
      } else if (err?.response?.status === 409) {
        errorMessage = '📱 Phone number already exists. Please use a different phone number.'
      } else if (err?.response?.status === 404) {
        errorMessage = '👤 User not found. Please log in again.'
      } else if (err?.response?.status === 401) {
        errorMessage = '🔒 Session expired. Please log in again.'
        // Redirect to login after a short delay
        setTimeout(() => {
          localStorage.removeItem('authToken')
          localStorage.removeItem('userRole')
          window.location.href = '/login'
        }, 2000)
      } else if (err?.code === 'ERR_NETWORK' || err?.message === 'Network Error') {
        errorMessage = '🌐 Network error. Please check your internet connection and try again.'
      } else if (err?.response?.data?.message) {
        errorMessage = err.response.data.message
      }
      
      setAlert({ tone: 'error', message: errorMessage })
    } finally {
      setSaving(false)
    }
  }

  // ── render ─────────────────────────────────────────────────────────────────
  return (
    <div className="space-y-8">
      {/* <PageHeader
        eyebrow="Account"
        title="My Profile"
        description="Update your display name, phone number, or change your password."
      /> */}

      {fetchError ? (
        <Alert tone="error" title="Could not load profile">
          {fetchError}
        </Alert>
      ) : null}

      {alert ? (
        <div className="fixed top-4 right-4 z-50 max-w-md">
          <Alert 
            tone={alert.tone} 
            onClose={() => setAlert(null)}
            className="shadow-lg border-2"
          >
            {alert.message}
          </Alert>
        </div>
      ) : null}

      <div className="grid gap-6 lg:grid-cols-[1fr_1.6fr]">
        {/* ── Account summary card ── */}
        <Card>
          <CardHeader title="Account summary" description="Your current account details." />
          <CardContent>
            {loading ? (
              <div className="space-y-4">
                <Skeleton className="h-6 w-3/4 rounded-xl" />
                <Skeleton className="h-5 w-1/2 rounded-xl" />
                <Skeleton className="h-5 w-2/3 rounded-xl" />
                <Skeleton className="h-5 w-1/3 rounded-xl" />
              </div>
            ) : profile ? (
              <div className="space-y-5">
                {/* Avatar placeholder */}
                <div className="flex items-center gap-4">
                  <div className="flex h-16 w-16 items-center justify-center rounded-3xl bg-slate-950 text-2xl font-bold text-white">
                    {profile.displayName?.charAt(0)?.toUpperCase() || '?'}
                  </div>
                  <div>
                    <p className="text-lg font-semibold text-slate-950">{profile.displayName}</p>
                    <p className="text-sm text-slate-500">@{profile.username}</p>
                  </div>
                </div>

                <div className="space-y-3 rounded-2xl border border-slate-100 bg-slate-50 p-4">
                  <InfoRow label="Username" value={profile.username} />
                  <InfoRow label="Phone" value={profile.phoneNumber} />
                  <InfoRow label="Role" value={profile.role} />
                  <InfoRow
                    label="Status"
                    value={
                      <span
                        className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                          profile.active
                            ? 'bg-emerald-100 text-emerald-800'
                            : 'bg-rose-100 text-rose-800'
                        }`}
                      >
                        {profile.active ? 'Active' : 'Inactive'}
                      </span>
                    }
                  />
                </div>
              </div>
            ) : null}
          </CardContent>
        </Card>

        {/* ── Edit form card ── */}
        <Card>
          {/* <CardHeader
            title="Edit profile"
            description="Changes are saved immediately. Password update is optional."
          /> */}
          <CardContent>
            {loading ? (
              <div className="space-y-4">
                <Skeleton className="h-12 rounded-2xl" />
                <Skeleton className="h-12 rounded-2xl" />
                <Skeleton className="h-12 rounded-2xl" />
                <Skeleton className="h-12 rounded-2xl" />
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="space-y-2" noValidate>
                {/* Read-only username */}
                <InputField
                  label="Username"
                  name="username"
                  value={profile?.username || ''}
                  disabled
                  // hint="Username cannot be changed."
                />

                <InputField
                  label="Display name"
                  name="displayName"
                  value={form.displayName}
                  onChange={handleChange}
                  placeholder="Your full name"
                  required
                  error={errors.displayName}
                />

                <InputField
                  label="Phone number"
                  name="phoneNumber"
                  type="tel"
                  value={form.phoneNumber}
                  onChange={handleChange}
                  placeholder="10-digit mobile number"
                  required
                  error={errors.phoneNumber}
                />

                {/* Password section */}
                <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 space-y-4">
                  <p className="text-sm font-semibold text-slate-700">
                    Change password{' '}
                    <span className="font-normal text-slate-400">(leave blank to keep current)</span>
                  </p>

                  <InputField
                    label="New password"
                    name="newPassword"
                    type="password"
                    value={form.newPassword}
                    onChange={handleChange}
                    placeholder="Min 8 chars, upper + lower + digit + special"
                    error={errors.newPassword}
                    autoComplete="new-password"
                  />

                  <InputField
                    label="Confirm new password"
                    name="confirmPassword"
                    type="password"
                    value={form.confirmPassword}
                    onChange={handleChange}
                    placeholder="Re-enter new password"
                    error={errors.confirmPassword}
                    autoComplete="new-password"
                  />
                </div>

                <Button
                  type="submit"
                  label={saving ? 'Saving changes...' : 'Save changes'}
                  loading={saving}
                  disabled={saving}
                  fullWidth
                  className={saving ? 'cursor-not-allowed' : ''}
                />
              </form>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

// ── small helper ──────────────────────────────────────────────────────────────
function InfoRow({ label, value }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <span className="text-sm text-slate-500">{label}</span>
      <span className="text-sm font-medium text-slate-900 text-right">{value ?? '—'}</span>
    </div>
  )
}
