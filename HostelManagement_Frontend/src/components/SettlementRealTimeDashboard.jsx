import { useState, useEffect } from 'react';
import { Card } from './ui/Card';
import { Badge } from './ui/Badge';
import { Button } from './ui/Button';
import SettlementRealTimeIndicator from './SettlementRealTimeIndicator';
import SettlementRealTimeNotifications from './SettlementRealTimeNotifications';
import SettlementStatusBadge from './SettlementStatusBadge';
import useRealTimeSettlementUpdates from '../hooks/useRealTimeSettlementUpdates';

/**
 * Comprehensive real-time dashboard for settlement management
 * Provides live updates, statistics, and action center
 */
const SettlementRealTimeDashboard = ({
  userType = 'tenant',
  onSettlementAction,
  showStatistics = true,
  showRecentActivity = true,
  showQuickActions = true,
  className = ""
}) => {
  const [selectedFilter, setSelectedFilter] = useState('all');
  const [showDetails, setShowDetails] = useState(false);

  const {
    settlements,
    loading,
    error,
    lastUpdated,
    connectionStatus,
    statusHistory,
    refresh,
    getSettlementStats,
    getPendingActionSettlements,
    getEnhancedSettlements,
    getCompletedSettlements,
    getSettlementsByStatus,
    getSettlementHistory
  } = useRealTimeSettlementUpdates({
    userType,
    pollingInterval: 3000,
    enabled: true,
    enableNotifications: true,
    onStatusChange: (change) => {
      console.log('Dashboard: Status changed', change);
    },
    onTransactionUpdate: (update) => {
      console.log('Dashboard: Transaction updated', update);
    },
    onError: (err, message) => {
      console.warn('Dashboard: Real-time error', message);
    }
  });

  const stats = getSettlementStats();
  const pendingActions = getPendingActionSettlements();
  const enhancedSettlements = getEnhancedSettlements();
  const completedSettlements = getCompletedSettlements();

  const formatCurrency = (amount) => {
    return `₹${(amount || 0).toLocaleString()}`;
  };

  const formatTimeAgo = (date) => {
    if (!date) return '';
    
    const now = new Date();
    const diff = now - date;
    const seconds = Math.floor(diff / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    
    if (seconds < 60) return `${seconds}s ago`;
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    return date.toLocaleDateString();
  };

  const getFilteredSettlements = () => {
    switch (selectedFilter) {
      case 'pending': return pendingActions;
      case 'enhanced': return enhancedSettlements;
      case 'completed': return completedSettlements;
      case 'in-progress': return getSettlementsByStatus('CALCULATION_IN_PROGRESS').concat(
        getSettlementsByStatus('PAYMENT_IN_PROGRESS')
      );
      default: return settlements;
    }
  };

  const getRecentActivity = () => {
    const allHistory = Object.entries(statusHistory)
      .flatMap(([settlementId, history]) => 
        history.map(change => ({ ...change, settlementId }))
      )
      .sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp))
      .slice(0, 5);
    
    return allHistory;
  };

  const handleNotificationAction = (notification, action) => {
    if (onSettlementAction) {
      onSettlementAction(notification.details?.settlement, action.action, action.data);
    }
  };

  const getStatusIcon = (status) => {
    const icons = {
      'COMPLETED': '✅',
      'SETTLEMENT_DONE': '🎉',
      'SETTLEMENT_APPROVED': '👍',
      'PENDING_TENANT_PAYMENT': '💳',
      'PENDING_OWNER_PAYMENT': '💰',
      'SETTLEMENT_TRANSACTION_CREATED': '📋',
      'PENDING_OWNER_REVIEW': '👀',
      'CALCULATION_IN_PROGRESS': '🔄',
      'PAYMENT_IN_PROGRESS': '⏳',
      'REJECTED': '❌',
      'CANCELLED': '🚫'
    };
    return icons[status] || '📄';
  };

  const getUrgencyLevel = (settlement) => {
    const urgentStatuses = ['PENDING_TENANT_PAYMENT', 'PENDING_OWNER_PAYMENT'];
    const importantStatuses = ['PENDING_OWNER_REVIEW', 'SETTLEMENT_TRANSACTION_CREATED'];
    
    if (urgentStatuses.includes(settlement.status)) return 'urgent';
    if (importantStatuses.includes(settlement.status)) return 'important';
    return 'normal';
  };

  if (loading && settlements.length === 0) {
    return (
      <div className={`p-6 ${className}`}>
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading settlement data...</p>
        </div>
      </div>
    );
  }

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Header with Real-time Status */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            Settlement Dashboard
            <span className="text-lg font-normal text-gray-500 ml-2">
              ({userType === 'tenant' ? 'Tenant' : 'Owner'} View)
            </span>
          </h1>
          <p className="text-gray-600 mt-1">Real-time settlement status and management</p>
        </div>
        
        <div className="flex items-center gap-4">
          <SettlementRealTimeNotifications
            userType={userType}
            onNotificationAction={handleNotificationAction}
            maxNotifications={20}
            autoHideDelay={12000}
          />
          
          <SettlementRealTimeIndicator
            userType={userType}
            showConnectionStatus={true}
            showLastUpdate={true}
            showNotificationCount={false}
            showStats={true}
            enableBrowserNotifications={true}
          />
        </div>
      </div>

      {/* Error Display */}
      {error && (
        <Card className="p-4 border-red-200 bg-red-50">
          <div className="flex items-center gap-3">
            <span className="text-red-600 text-xl">⚠️</span>
            <div className="flex-1">
              <p className="text-red-800 font-medium">Connection Issue</p>
              <p className="text-red-600 text-sm">{error}</p>
            </div>
            <Button
              label="Retry"
              variant="secondary"
              size="sm"
              onClick={refresh}
            />
          </div>
        </Card>
      )}

      {/* Statistics Cards */}
      {showStatistics && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <Card className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Total Settlements</p>
                <p className="text-2xl font-bold text-gray-900">{stats.total}</p>
              </div>
              <div className="text-3xl">📊</div>
            </div>
          </Card>

          <Card className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Completed</p>
                <p className="text-2xl font-bold text-green-600">{stats.completed}</p>
              </div>
              <div className="text-3xl">✅</div>
            </div>
          </Card>

          <Card className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Pending Action</p>
                <p className="text-2xl font-bold text-yellow-600">{stats.pending}</p>
              </div>
              <div className="text-3xl">⏳</div>
            </div>
          </Card>

          <Card className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">In Progress</p>
                <p className="text-2xl font-bold text-blue-600">{stats.inProgress}</p>
              </div>
              <div className="text-3xl">🔄</div>
            </div>
          </Card>
        </div>
      )}

      {/* Filters and Actions */}
      <div className="flex items-center justify-between gap-4 flex-wrap">
        <div className="flex items-center gap-2">
          <span className="text-sm font-medium text-gray-700">Filter:</span>
          {[
            { key: 'all', label: 'All Settlements', count: settlements.length },
            { key: 'pending', label: 'Need Action', count: pendingActions.length },
            { key: 'enhanced', label: 'Enhanced', count: enhancedSettlements.length },
            { key: 'in-progress', label: 'In Progress', count: stats.inProgress },
            { key: 'completed', label: 'Completed', count: completedSettlements.length }
          ].map(filter => (
            <button
              key={filter.key}
              onClick={() => setSelectedFilter(filter.key)}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                selectedFilter === filter.key
                  ? 'bg-blue-100 text-blue-800 border border-blue-200'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200 border border-transparent'
              }`}
            >
              {filter.label} ({filter.count})
            </button>
          ))}
        </div>

        <div className="flex items-center gap-2">
          <Button
            label="Refresh"
            variant="secondary"
            size="sm"
            onClick={refresh}
            loading={loading}
          />
          <button
            onClick={() => setShowDetails(!showDetails)}
            className="text-sm text-blue-600 hover:text-blue-800"
          >
            {showDetails ? 'Hide' : 'Show'} Details
          </button>
        </div>
      </div>

      {/* Settlement List */}
      <div className="grid gap-4">
        {getFilteredSettlements().length === 0 ? (
          <Card className="p-8 text-center">
            <div className="text-4xl mb-2">📭</div>
            <p className="text-gray-600">No settlements found for the selected filter</p>
          </Card>
        ) : (
          getFilteredSettlements().map(settlement => {
            const urgency = getUrgencyLevel(settlement);
            
            return (
              <Card 
                key={settlement.settlementId} 
                className={`p-4 hover:shadow-md transition-shadow ${
                  urgency === 'urgent' ? 'border-red-300 bg-red-50/30' :
                  urgency === 'important' ? 'border-yellow-300 bg-yellow-50/30' :
                  'border-gray-200'
                }`}
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex items-start gap-3 flex-1">
                    <div className="text-2xl">
                      {getStatusIcon(settlement.status)}
                    </div>
                    
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-1">
                        <h3 className="font-semibold text-gray-900">
                          {settlement.tenantName || 'Settlement Request'}
                        </h3>
                        {settlement.earlySettlementRequested && (
                          <Badge variant="warning" className="text-xs">
                            Early Exit
                          </Badge>
                        )}
                        {urgency === 'urgent' && (
                          <Badge variant="destructive" className="text-xs">
                            Urgent
                          </Badge>
                        )}
                      </div>
                      
                      <p className="text-sm text-gray-600">
                        Room: {settlement.roomNumber || 'N/A'} • 
                        ID: {settlement.settlementId.substring(0, 8)}...
                      </p>
                      
                      <div className="flex items-center gap-4 text-xs text-gray-500 mt-1">
                        <span>Created: {formatTimeAgo(new Date(settlement.createdAt))}</span>
                        {settlement.requestedEndDate && (
                          <span>End Date: {new Date(settlement.requestedEndDate).toLocaleDateString()}</span>
                        )}
                      </div>

                      {settlement.finalSettlementAmount !== undefined && (
                        <div className="mt-2">
                          <Badge variant={settlement.finalSettlementAmount >= 0 ? "success" : "destructive"}>
                            {settlement.finalSettlementAmount >= 0 ? 'Refund' : 'Payment'}: {formatCurrency(Math.abs(settlement.finalSettlementAmount))}
                          </Badge>
                        </div>
                      )}
                    </div>
                  </div>
                  
                  <div className="flex flex-col items-end gap-2">
                    <SettlementStatusBadge status={settlement.status} />
                    
                    {showQuickActions && (
                      <div className="flex gap-2">
                        {urgency === 'urgent' && (
                          <Button
                            label={
                              settlement.status === 'PENDING_TENANT_PAYMENT' && userType === 'tenant' ? 'Pay Now' :
                              settlement.status === 'PENDING_OWNER_PAYMENT' && userType === 'owner' ? 'Process Refund' :
                              'Take Action'
                            }
                            size="sm"
                            onClick={() => onSettlementAction?.(settlement, 'primary_action')}
                          />
                        )}
                        
                        <Button
                          label="Details"
                          variant="secondary"
                          size="sm"
                          onClick={() => onSettlementAction?.(settlement, 'view_details')}
                        />
                      </div>
                    )}
                  </div>
                </div>

                {/* Enhanced Details */}
                {showDetails && (
                  <div className="mt-4 pt-4 border-t border-gray-200">
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                      <div>
                        <p className="text-gray-600">Security Deposit</p>
                        <p className="font-medium text-green-600">{formatCurrency(settlement.securityDeposit)}</p>
                      </div>
                      <div>
                        <p className="text-gray-600">Outstanding Rent</p>
                        <p className="font-medium text-red-600">{formatCurrency(settlement.outstandingRent)}</p>
                      </div>
                      <div>
                        <p className="text-gray-600">Other Charges</p>
                        <p className="font-medium text-red-600">{formatCurrency(settlement.outstandingCharges)}</p>
                      </div>
                      <div>
                        <p className="text-gray-600">Deductions</p>
                        <p className="font-medium text-orange-600">
                          {formatCurrency((settlement.damageCharges || 0) + (settlement.cleaningCharges || 0) + (settlement.otherDeductions || 0))}
                        </p>
                      </div>
                    </div>
                  </div>
                )}
              </Card>
            );
          })
        )}
      </div>

      {/* Recent Activity */}
      {showRecentActivity && (
        <Card className="p-4">
          <h3 className="font-semibold text-gray-900 mb-4">Recent Activity</h3>
          <div className="space-y-3">
            {getRecentActivity().length === 0 ? (
              <p className="text-gray-500 text-sm">No recent activity</p>
            ) : (
              getRecentActivity().map((activity, index) => (
                <div key={index} className="flex items-center gap-3 p-2 bg-gray-50 rounded-lg">
                  <div className="text-lg">
                    {getStatusIcon(activity.newStatus)}
                  </div>
                  <div className="flex-1">
                    <p className="text-sm font-medium text-gray-900">
                      Settlement {activity.settlementId.substring(0, 8)}... status changed
                    </p>
                    <p className="text-xs text-gray-600">
                      From {activity.oldStatus} to {activity.newStatus} • {formatTimeAgo(activity.timestamp)}
                    </p>
                  </div>
                  <SettlementStatusBadge status={activity.newStatus} />
                </div>
              ))
            )}
          </div>
        </Card>
      )}
    </div>
  );
};

export default SettlementRealTimeDashboard;