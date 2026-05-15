import { useState, useEffect } from 'react'
import { Card } from '../../components/ui/Card'
import { Button } from '../../components/ui/Button'
import { Alert } from '../../components/ui/Alert'
import { PageHeader } from '../../components/ui/PageHeader'
import { Badge } from '../../components/ui/Badge'
import mcpPaymentService from '../../services/mcpPaymentService'

const PaymentMonitoring = () => {
  const [statistics, setStatistics] = useState(null)
  const [configurations, setConfigurations] = useState([])
  const [loading, setLoading] = useState(true)
  const [alert, setAlert] = useState(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [filterStatus, setFilterStatus] = useState('all')
  const [selectedOwner, setSelectedOwner] = useState(null)
  const [overrideModal, setOverrideModal] = useState(null)
  const [overrideReason, setOverrideReason] = useState('')
  const [actionLoading, setActionLoading] = useState(false)

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      setLoading(true)
      const [stats, configs] = await Promise.all([
        mcpPaymentService.getStatistics(),
        mcpPaymentService.getAllConfigurations(),
      ])
      setStatistics(stats)
      setConfigurations(configs)
    } catch (error) {
      console.error('Failed to load data:', error)
      setAlert({
        type: 'error',
        message: 'Failed to load payment monitoring data',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleOverride = async (ownerId, disabled) => {
    if (!overrideReason.trim()) {
      setAlert({
        type: 'error',
        message: 'Please provide a reason for this action',
      })
      return
    }

    try {
      setActionLoading(true)
      await mcpPaymentService.mcpOverride(ownerId, {
        disabled,
        reason: overrideReason,
      })
      
      setAlert({
        type: 'success',
        message: `Payments ${disabled ? 'disabled' : 'enabled'} successfully`,
      })
      setOverrideModal(null)
      setOverrideReason('')
      await loadData()
    } catch (error) {
      setAlert({
        type: 'error',
        message: 'Failed to update payment status',
      })
    } finally {
      setActionLoading(false)
    }
  }

  const handleForceReverify = async (ownerId) => {
    if (!confirm('Force re-verification? The owner will need to verify their credentials again.')) {
      return
    }

    try {
      setActionLoading(true)
      await mcpPaymentService.forceReVerification(ownerId)
      
      setAlert({
        type: 'success',
        message: 'Re-verification forced successfully',
      })
      await loadData()
    } catch (error) {
      setAlert({
        type: 'error',
        message: 'Failed to force re-verification',
      })
    } finally {
      setActionLoading(false)
    }
  }

  const getStatusBadge = (config) => {
    if (config.paymentsEnabled) {
      return <Badge variant="success">Active</Badge>
    }
    if (config.mcpOverrideDisabled) {
      return <Badge variant="danger">MCP Disabled</Badge>
    }
    if (config.verificationStatus === 'VERIFIED' && !config.isActive) {
      return <Badge variant="warning">Inactive</Badge>
    }
    if (config.verificationStatus === 'FAILED') {
      return <Badge variant="danger">Failed</Badge>
    }
    return <Badge variant="secondary">Not Connected</Badge>
  }

  const filteredConfigurations = configurations.filter((config) => {
    const matchesSearch = 
      config.ownerEmail?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      config.ownerName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      config.razorpayKeyId?.toLowerCase().includes(searchTerm.toLowerCase())

    const matchesFilter =
      filterStatus === 'all' ||
      (filterStatus === 'active' && config.paymentsEnabled) ||
      (filterStatus === 'inactive' && !config.paymentsEnabled) ||
      (filterStatus === 'verified' && config.verificationStatus === 'VERIFIED') ||
      (filterStatus === 'failed' && config.verificationStatus === 'FAILED') ||
      (filterStatus === 'mcp-disabled' && config.mcpOverrideDisabled)

    return matchesSearch && matchesFilter
  })

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 p-6">
        <div className="max-w-7xl mx-auto">
          <div className="animate-pulse space-y-4">
            <div className="h-8 bg-gray-200 rounded w-1/4"></div>
            <div className="grid grid-cols-4 gap-4">
              {[1, 2, 3, 4].map((i) => (
                <div key={i} className="h-24 bg-gray-200 rounded"></div>
              ))}
            </div>
            <div className="h-96 bg-gray-200 rounded"></div>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-7xl mx-auto space-y-6">
        <PageHeader
          title="Payment Monitoring"
          subtitle="Monitor and control owner Razorpay integrations"
        />

        {alert && (
          <Alert
            type={alert.type}
            message={alert.message}
            onClose={() => setAlert(null)}
          />
        )}

        {/* Statistics Cards */}
        {statistics && (
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <Card>
              <div className="p-6">
                <div className="text-sm text-gray-600 mb-1">Total Owners</div>
                <div className="text-3xl font-bold text-gray-900">{statistics.totalOwners}</div>
              </div>
            </Card>
            <Card>
              <div className="p-6">
                <div className="text-sm text-gray-600 mb-1">Verified</div>
                <div className="text-3xl font-bold text-green-600">{statistics.verifiedOwners}</div>
              </div>
            </Card>
            <Card>
              <div className="p-6">
                <div className="text-sm text-gray-600 mb-1">Active</div>
                <div className="text-3xl font-bold text-blue-600">{statistics.activeOwners}</div>
              </div>
            </Card>
            <Card>
              <div className="p-6">
                <div className="text-sm text-gray-600 mb-1">MCP Disabled</div>
                <div className="text-3xl font-bold text-red-600">{statistics.mcpDisabledOwners}</div>
              </div>
            </Card>
          </div>
        )}

        {/* Filters */}
        <Card>
          <div className="p-4">
            <div className="flex flex-col md:flex-row gap-4">
              <div className="flex-1">
                <input
                  type="text"
                  placeholder="Search by owner name, email, or key ID..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
              <div>
                <select
                  value={filterStatus}
                  onChange={(e) => setFilterStatus(e.target.value)}
                  className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="all">All Status</option>
                  <option value="active">Active</option>
                  <option value="inactive">Inactive</option>
                  <option value="verified">Verified</option>
                  <option value="failed">Failed</option>
                  <option value="mcp-disabled">MCP Disabled</option>
                </select>
              </div>
            </div>
          </div>
        </Card>

        {/* Configurations Table */}
        <Card>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Owner
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Key ID
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Last Verified
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {filteredConfigurations.length === 0 ? (
                  <tr>
                    <td colSpan="5" className="px-6 py-8 text-center text-gray-500">
                      No configurations found
                    </td>
                  </tr>
                ) : (
                  filteredConfigurations.map((config) => (
                    <tr key={config.configId} className="hover:bg-gray-50">
                      <td className="px-6 py-4">
                        <div className="text-sm font-medium text-gray-900">
                          {config.ownerName || 'Unknown'}
                        </div>
                        <div className="text-sm text-gray-500">
                          {config.ownerEmail || 'No email'}
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <div className="text-sm font-mono text-gray-900">
                          {config.maskedKeyId || 'Not set'}
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        {getStatusBadge(config)}
                        {config.mcpOverrideDisabled && (
                          <div className="text-xs text-red-600 mt-1">
                            {config.mcpOverrideReason}
                          </div>
                        )}
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-500">
                        {config.lastVerifiedAt
                          ? new Date(config.lastVerifiedAt).toLocaleString()
                          : 'Never'}
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex gap-2">
                          {config.paymentsEnabled ? (
                            <Button
                              onClick={() => setOverrideModal({ config, action: 'disable' })}
                              variant="danger"
                              size="sm"
                              disabled={actionLoading}
                            >
                              Disable
                            </Button>
                          ) : config.mcpOverrideDisabled ? (
                            <Button
                              onClick={() => setOverrideModal({ config, action: 'enable' })}
                              variant="success"
                              size="sm"
                              disabled={actionLoading}
                            >
                              Enable
                            </Button>
                          ) : null}
                          
                          {config.verificationStatus === 'VERIFIED' && (
                            <Button
                              onClick={() => handleForceReverify(config.ownerId)}
                              variant="secondary"
                              size="sm"
                              disabled={actionLoading}
                            >
                              Re-verify
                            </Button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </Card>

        {/* Override Modal */}
        {overrideModal && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <Card className="max-w-md w-full mx-4">
              <div className="p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">
                  {overrideModal.action === 'disable' ? 'Disable' : 'Enable'} Payments
                </h3>
                
                <p className="text-sm text-gray-600 mb-4">
                  Owner: <strong>{overrideModal.config.ownerEmail}</strong>
                </p>

                <div className="mb-4">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Reason (required)
                  </label>
                  <textarea
                    value={overrideReason}
                    onChange={(e) => setOverrideReason(e.target.value)}
                    placeholder="Enter reason for this action..."
                    rows="3"
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>

                <div className="flex gap-3">
                  <Button
                    onClick={() =>
                      handleOverride(
                        overrideModal.config.ownerId,
                        overrideModal.action === 'disable'
                      )
                    }
                    disabled={actionLoading || !overrideReason.trim()}
                    variant={overrideModal.action === 'disable' ? 'danger' : 'success'}
                    className="flex-1"
                  >
                    {actionLoading ? 'Processing...' : 'Confirm'}
                  </Button>
                  <Button
                    onClick={() => {
                      setOverrideModal(null)
                      setOverrideReason('')
                    }}
                    variant="secondary"
                    className="flex-1"
                  >
                    Cancel
                  </Button>
                </div>
              </div>
            </Card>
          </div>
        )}
      </div>
    </div>
  )
}

export default PaymentMonitoring
