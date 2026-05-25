import { useState, useEffect } from 'react'
import { Card } from '../../components/ui/Card'
import { Button } from '../../components/ui/Button'
import { InputField } from '../../components/ui/InputField'
import { Alert } from '../../components/ui/Alert'
import { PageHeader } from '../../components/ui/PageHeader'
import paymentSettingsService from '../../services/paymentSettingsService'

const PaymentSettings = () => {
  const [config, setConfig] = useState(null)
  const [loading, setLoading] = useState(true)
  const [testLoading, setTestLoading] = useState(false)
  const [saveLoading, setSaveLoading] = useState(false)
  const [deactivateLoading, setDeactivateLoading] = useState(false)
  
  const [credentials, setCredentials] = useState({
    keyId: '',
    keySecret: '',
  })
  
  const [alert, setAlert] = useState(null)
  const [showDeactivateConfirm, setShowDeactivateConfirm] = useState(false)

  useEffect(() => {
    loadConfiguration()
  }, [])

  const loadConfiguration = async () => {
    try {
      setLoading(true)
      const data = await paymentSettingsService.getConfiguration()
      setConfig(data)
      if (data?.razorpayKeyId) {
        setCredentials(prev => ({ ...prev, keyId: data.razorpayKeyId }))
      }
    } catch (error) {
      console.error('Failed to load configuration:', error)
      setAlert({
        type: 'error',
        message: 'Failed to load payment settings. Please try again.',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleTestConnection = async () => {
    if (!credentials.keyId || !credentials.keySecret) {
      setAlert({
        type: 'error',
        message: 'Please enter both Key ID and Key Secret',
      })
      return
    }

    try {
      setTestLoading(true)
      setAlert(null)
      const result = await paymentSettingsService.testConnection(credentials)
      
      if (result.verificationStatus === 'VERIFIED') {
        setAlert({
          type: 'success',
          message: '✅ Connection successful! Your Razorpay credentials are valid.',
        })
        setConfig(result)
      } else if (result.verificationStatus === 'FAILED') {
        setAlert({
          type: 'error',
          message: `❌ Connection failed: ${result.verificationError || 'Invalid credentials'}`,
        })
        setConfig(result)
      }
    } catch (error) {
      setAlert({
        type: 'error',
        message: error.response?.data?.message || 'Connection test failed. Please check your credentials.',
      })
    } finally {
      setTestLoading(false)
    }
  }

  const handleSaveAndActivate = async () => {
    if (!credentials.keyId || !credentials.keySecret) {
      setAlert({
        type: 'error',
        message: 'Please enter both Key ID and Key Secret',
      })
      return
    }

    try {
      setSaveLoading(true)
      setAlert(null)
      const result = await paymentSettingsService.saveAndActivate(credentials)
      
      setAlert({
        type: 'success',
        message: '🎉 Payments activated successfully! You can now accept online payments.',
      })
      setConfig(result)
      setCredentials(prev => ({ ...prev, keySecret: '' })) // Clear secret after save
    } catch (error) {
      setAlert({
        type: 'error',
        message: error.response?.data?.message || 'Failed to activate payments. Please try again.',
      })
    } finally {
      setSaveLoading(false)
    }
  }

  const handleDeactivate = async () => {
    try {
      setDeactivateLoading(true)
      setAlert(null)
      const result = await paymentSettingsService.deactivate()
      
      setAlert({
        type: 'info',
        message: 'Payments have been deactivated. You can reactivate them anytime.',
      })
      setConfig(result)
      setShowDeactivateConfirm(false)
    } catch (error) {
      setAlert({
        type: 'error',
        message: 'Failed to deactivate payments. Please try again.',
      })
    } finally {
      setDeactivateLoading(false)
    }
  }

  const getStatusBadge = () => {
    if (!config) return null

    if (config.paymentsEnabled) {
      return (
        <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-green-100 text-green-800">
          ✓ Active
        </span>
      )
    }

    if (config.mcpOverrideDisabled) {
      return (
        <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-red-100 text-red-800">
          ⚠ Disabled by Platform
        </span>
      )
    }

    if (config.verificationStatus === 'VERIFIED' && !config.isActive) {
      return (
        <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-yellow-100 text-yellow-800">
          ⏸ Inactive
        </span>
      )
    }

    if (config.verificationStatus === 'FAILED') {
      return (
        <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-red-100 text-red-800">
          ✗ Verification Failed
        </span>
      )
    }

    return (
      <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-gray-100 text-gray-800">
        ○ Not Connected
      </span>
    )
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 p-6">
        <div className="max-w-4xl mx-auto">
          <div className="animate-pulse">
            <div className="h-8 bg-gray-200 rounded w-1/4 mb-4"></div>
            <div className="h-64 bg-gray-200 rounded"></div>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-4xl mx-auto space-y-6">
        <PageHeader
          title="Payment Settings"
          subtitle="Configure your Razorpay account to accept online payments"
        />

        {alert && (
          <Alert
            type={alert.type}
            message={alert.message}
            onClose={() => setAlert(null)}
          />
        )}

        {/* Status Card */}
        <Card>
          <div className="p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold text-gray-900">Connection Status</h2>
              {getStatusBadge()}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
              <div>
                <span className="text-gray-600">Verification Status:</span>
                <span className="ml-2 font-medium text-gray-900">
                  {config?.verificationStatus || 'Not Verified'}
                </span>
              </div>
              
              {config?.lastVerifiedAt && (
                <div>
                  <span className="text-gray-600">Last Verified:</span>
                  <span className="ml-2 font-medium text-gray-900">
                    {new Date(config.lastVerifiedAt).toLocaleString()}
                  </span>
                </div>
              )}

              {config?.maskedKeyId && (
                <div>
                  <span className="text-gray-600">Key ID:</span>
                  <span className="ml-2 font-mono text-sm text-gray-900">
                    {config.maskedKeyId}
                  </span>
                </div>
              )}

              {config?.mcpOverrideDisabled && config?.mcpOverrideReason && (
                <div className="md:col-span-2">
                  <span className="text-gray-600">Platform Note:</span>
                  <span className="ml-2 font-medium text-red-600">
                    {config.mcpOverrideReason}
                  </span>
                </div>
              )}
            </div>
          </div>
        </Card>

        {/* Account Verification Card */}
        <Card>
          <div className="p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">
              Account Verification
            </h2>
            
            <div className="space-y-4">
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <h3 className="text-sm font-medium text-blue-900 mb-2">
                  📘 How to get your Razorpay API Keys
                </h3>
                <ol className="text-sm text-blue-800 space-y-1 list-decimal list-inside">
                  <li>Go to <a href="https://dashboard.razorpay.com/" target="_blank" rel="noopener noreferrer" className="underline">Razorpay Dashboard</a></li>
                  <li>Navigate to Settings → API Keys</li>
                  <li>Generate keys or copy existing ones</li>
                  <li>Use Test keys for testing, Live keys for production</li>
                </ol>
              </div>

              <InputField
                label="Razorpay Key ID"
                type="text"
                value={credentials.keyId}
                onChange={(e) => setCredentials({ ...credentials, keyId: e.target.value })}
                placeholder="rzp_test_xxxxxxxxxxxxx"
                disabled={config?.mcpOverrideDisabled}
              />

              <InputField
                label="Razorpay Key Secret"
                type="password"
                value={credentials.keySecret}
                onChange={(e) => setCredentials({ ...credentials, keySecret: e.target.value })}
                placeholder="Enter your Razorpay Key Secret"
                disabled={config?.mcpOverrideDisabled}
              />

              <div className="flex gap-3">
                <Button
                  onClick={handleTestConnection}
                  disabled={testLoading || !credentials.keyId || !credentials.keySecret || config?.mcpOverrideDisabled}
                  variant="secondary"
                  className="flex-1"
                >
                  {testLoading ? 'Testing...' : '🔍 Validate Credentials'}
                </Button>

                <Button
                  onClick={handleSaveAndActivate}
                  disabled={
                    saveLoading || 
                    !credentials.keyId || 
                    !credentials.keySecret || 
                    config?.verificationStatus !== 'VERIFIED' ||
                    config?.mcpOverrideDisabled
                  }
                  variant="primary"
                  className="flex-1"
                >
                  {saveLoading ? 'Activating...' : '✓ Save & Activate Payments'}
                </Button>
              </div>

              {config?.verificationStatus === 'VERIFIED' && !config?.isActive && (
                <p className="text-sm text-amber-600">
                  ⚠️ Credentials verified! Click "Save & Activate Payments" to enable online payments.
                </p>
              )}
            </div>
          </div>
        </Card>

        {/* Actions Card */}
        {config?.isActive && !config?.mcpOverrideDisabled && (
          <Card>
            <div className="p-6">
              <h2 className="text-xl font-semibold text-gray-900 mb-4">Actions</h2>
              
              {!showDeactivateConfirm ? (
                <Button
                  onClick={() => setShowDeactivateConfirm(true)}
                  variant="danger"
                >
                  Deactivate Payments
                </Button>
              ) : (
                <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                  <p className="text-sm text-red-800 mb-3">
                    ⚠️ Are you sure you want to deactivate payments? Tenants won't be able to pay online until you reactivate.
                  </p>
                  <div className="flex gap-3">
                    <Button
                      onClick={handleDeactivate}
                      disabled={deactivateLoading}
                      variant="danger"
                    >
                      {deactivateLoading ? 'Deactivating...' : 'Yes, Deactivate'}
                    </Button>
                    <Button
                      onClick={() => setShowDeactivateConfirm(false)}
                      variant="secondary"
                    >
                      Cancel
                    </Button>
                  </div>
                </div>
              )}
            </div>
          </Card>
        )}

        {/* Security Notice */}
        <Card>
          <div className="p-6 bg-gray-50">
            <h3 className="text-sm font-medium text-gray-900 mb-2">🔒 Security Notice</h3>
            <ul className="text-sm text-gray-600 space-y-1 list-disc list-inside">
              <li>Your credentials are encrypted and stored securely</li>
              <li>We never store your Key Secret in plain text</li>
              <li>Payments go directly to your Razorpay account</li>
              <li>You can deactivate payments anytime</li>
            </ul>
          </div>
        </Card>
      </div>
    </div>
  )
}

export default PaymentSettings
