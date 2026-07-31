import { useState, useEffect } from 'react';
import { Badge } from './ui/Badge';
import useRealTimeSettlementUpdates from '../hooks/useRealTimeSettlementUpdates';

/**
 * Enhanced real-time status indicator component for settlements
 * Shows live connection status, last update time, notifications, and handles enhanced status changes
 */
const SettlementRealTimeIndicator = ({ 
  userType = 'tenant',
  settlementIds = [],
  onStatusChange,
  onTransactionUpdate,
  settlementState = null,
  enabled = true,
  className = "",
  showLastUpdate = true,
  showConnectionStatus = true,
  showNotificationCount = true,
  showStats = false,
  enableBrowserNotifications = false
}) => {
  const [notifications, setNotifications] = useState([]);
  const [dismissedNotifications, setDismissedNotifications] = useState(new Set());
  
  const internalRealtimeState = useRealTimeSettlementUpdates({
    userType,
    settlementIds,
    pollingInterval: 3000,
    enabled: enabled && !settlementState,
    enableNotifications: enableBrowserNotifications,
    onStatusChange: (change) => {
      // Create notification for status change
      const notification = {
        id: `${change.settlementId}-${Date.now()}`,
        type: 'status_change',
        settlementId: change.settlementId,
        message: `Settlement ${change.settlementId.substring(0, 8)}... status changed to ${getStatusDisplayName(change.newStatus)}`,
        status: change.newStatus,
        oldStatus: change.oldStatus,
        category: getStatusChangeCategory(change.newStatus),
        timestamp: new Date(),
        settlement: change.settlement
      };
      
      setNotifications(prev => [notification, ...prev.slice(0, 4)]); // Keep max 5 notifications
      
      // Auto-remove notification after 10 seconds unless it's important
      const autoRemoveDelay = ['COMPLETED', 'SETTLEMENT_DONE', 'PENDING_TENANT_PAYMENT', 'PENDING_OWNER_PAYMENT'].includes(change.newStatus) 
        ? 15000 : 8000;
      
      setTimeout(() => {
        setNotifications(prev => prev.filter(n => n.id !== notification.id));
      }, autoRemoveDelay);
      
      // Call external callback
      if (onStatusChange) {
        onStatusChange(change);
      }
    },
    onTransactionUpdate: (update) => {
      // Create notification for transaction updates
      const notification = {
        id: `${update.settlementId}-tx-${Date.now()}`,
        type: 'transaction_update',
        settlementId: update.settlementId,
        message: update.type === 'amount_change' 
          ? `Settlement amount updated: ₹${Math.abs(update.newAmount || 0).toLocaleString()}`
          : `Settlement transaction data updated`,
        category: 'info',
        timestamp: new Date(),
        settlement: update.settlement
      };
      
      setNotifications(prev => [notification, ...prev.slice(0, 4)]);
      
      setTimeout(() => {
        setNotifications(prev => prev.filter(n => n.id !== notification.id));
      }, 7000);
      
      // Call external callback
      if (onTransactionUpdate) {
        onTransactionUpdate(update);
      }
    },
    onError: (err, message) => {
      console.warn('Real-time updates error:', message);
      
      // Create error notification
      const notification = {
        id: `error-${Date.now()}`,
        type: 'error',
        message: 'Connection issue detected',
        category: 'error',
        timestamp: new Date()
      };
      
      setNotifications(prev => [notification, ...prev.slice(0, 4)]);
      
      setTimeout(() => {
        setNotifications(prev => prev.filter(n => n.id !== notification.id));
      }, 5000);
    }
  });

  const {
    lastUpdated,
    error,
    isPolling,
    connectionStatus,
    statusHistory,
    refresh,
    getSettlementStats,
    getPendingActionSettlements
  } = settlementState || internalRealtimeState;

  // Enhanced status display names
  const getStatusDisplayName = (status) => {
    const statusNames = {
      'PENDING_OWNER_REVIEW': 'Pending Review',
      'CALCULATION_IN_PROGRESS': 'Calculating',
      'PENDING_TENANT_PAYMENT': 'Payment Required',
      'PENDING_OWNER_PAYMENT': 'Refund Pending',
      'PAYMENT_IN_PROGRESS': 'Processing Payment',
      'SETTLEMENT_TRANSACTION_CREATED': 'Transaction Created',
      'SETTLEMENT_APPROVED': 'Approved',
      'SETTLEMENT_DONE': 'Settlement Complete',
      'COMPLETED': 'Completed',
      'CANCELLED': 'Cancelled',
      'REJECTED': 'Rejected'
    };
    
    return statusNames[status] || status;
  };

  const getStatusChangeCategory = (status) => {
    const positiveStatuses = ['COMPLETED', 'SETTLEMENT_DONE', 'SETTLEMENT_APPROVED'];
    const warningStatuses = ['PENDING_TENANT_PAYMENT', 'PENDING_OWNER_PAYMENT', 'SETTLEMENT_TRANSACTION_CREATED'];
    const errorStatuses = ['REJECTED', 'CANCELLED'];
    
    if (positiveStatuses.includes(status)) return 'success';
    if (warningStatuses.includes(status)) return 'warning';
    if (errorStatuses.includes(status)) return 'error';
    return 'info';
  };

  const formatLastUpdate = (date) => {
    if (!date) return '';
    
    const now = new Date();
    const diff = now - date;
    const seconds = Math.floor(diff / 1000);
    
    if (seconds < 60) return `${seconds}s ago`;
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
    return date.toLocaleTimeString();
  };

  const getConnectionStatusColor = () => {
    switch (connectionStatus) {
      case 'connected': return 'text-green-500';
      case 'connecting': return 'text-yellow-500';
      case 'error': return 'text-red-500';
      case 'disconnected': return 'text-gray-500';
      default: return 'text-gray-500';
    }
  };

  const getConnectionStatusText = () => {
    switch (connectionStatus) {
      case 'connected': return 'Live Updates';
      case 'connecting': return 'Connecting...';
      case 'error': return 'Connection Error';
      case 'disconnected': return 'Updates Paused';
      default: return 'Unknown Status';
    }
  };

  const getConnectionIcon = () => {
    switch (connectionStatus) {
      case 'connected': return '🟢';
      case 'connecting': return '🟡';
      case 'error': return '🔴';
      case 'disconnected': return '⚪';
      default: return '⚫';
    }
  };

  // Filter out dismissed notifications
  const activeNotifications = notifications.filter(n => !dismissedNotifications.has(n.id));
  
  // Get settlement stats if enabled
  const stats = showStats ? getSettlementStats() : null;
  const pendingActions = showStats ? getPendingActionSettlements() : [];

  const dismissNotification = (notificationId) => {
    setDismissedNotifications(prev => new Set([...prev, notificationId]));
    setNotifications(prev => prev.filter(n => n.id !== notificationId));
  };

  const dismissAllNotifications = () => {
    setNotifications([]);
    setDismissedNotifications(new Set());
  };

  return (
    <div className={`relative ${className}`}>
      {/* Main Indicator */}
      <div className="flex items-center gap-2 text-sm flex-wrap">
        {showConnectionStatus && (
          <div className="flex items-center gap-1">
            <div className={`w-2 h-2 rounded-full ${
              connectionStatus === 'connected' ? 'bg-green-500 animate-pulse' :
              connectionStatus === 'connecting' ? 'bg-yellow-500 animate-pulse' :
              connectionStatus === 'error' ? 'bg-red-500' :
              'bg-gray-500'
            }`} />
            <span className={`text-xs font-medium ${getConnectionStatusColor()}`}>
              {getConnectionStatusText()}
            </span>
          </div>
        )}
        
        {showLastUpdate && lastUpdated && (
          <span className="text-xs text-gray-500">
            Updated {formatLastUpdate(lastUpdated)}
          </span>
        )}

        {showNotificationCount && activeNotifications.length > 0 && (
          <Badge variant="destructive" className="text-xs px-1.5 py-0.5">
            {activeNotifications.length} update{activeNotifications.length > 1 ? 's' : ''}
          </Badge>
        )}

        {showStats && stats && (
          <div className="flex items-center gap-1 text-xs">
            <span className="text-gray-500">•</span>
            <span className="text-green-600 font-medium">{stats.completed} done</span>
            {stats.pending > 0 && (
              <>
                <span className="text-gray-400">•</span>
                <span className="text-yellow-600 font-medium">{stats.pending} pending</span>
              </>
            )}
            {stats.requiresAction > 0 && (
              <>
                <span className="text-gray-400">•</span>
                <span className="text-red-600 font-medium">{stats.requiresAction} action needed</span>
              </>
            )}
          </div>
        )}
        
        {error && (
          <button
            onClick={refresh}
            className="text-xs text-blue-600 hover:text-blue-800 underline"
            title="Retry connection"
          >
            Retry
          </button>
        )}

        {activeNotifications.length > 1 && (
          <button
            onClick={dismissAllNotifications}
            className="text-xs text-gray-500 hover:text-gray-700 underline"
            title="Dismiss all notifications"
          >
            Clear all
          </button>
        )}
      </div>

      {/* Enhanced Status Change Notifications */}
      {activeNotifications.length > 0 && (
        <div className="absolute top-8 right-0 z-50 space-y-2 min-w-80 max-w-96">
          {activeNotifications.map(notification => {
            const isImportant = ['COMPLETED', 'SETTLEMENT_DONE', 'PENDING_TENANT_PAYMENT', 'PENDING_OWNER_PAYMENT'].includes(notification.status);
            
            return (
              <div
                key={notification.id}
                className={`
                  p-3 rounded-lg shadow-lg border animate-fade-in-down backdrop-blur-sm
                  ${notification.category === 'success' ? 'bg-green-50/95 border-green-200 text-green-800' :
                    notification.category === 'warning' ? 'bg-yellow-50/95 border-yellow-200 text-yellow-800' :
                    notification.category === 'error' ? 'bg-red-50/95 border-red-200 text-red-800' :
                    'bg-blue-50/95 border-blue-200 text-blue-800'}
                  ${isImportant ? 'ring-2 ring-offset-1 ring-current ring-opacity-20' : ''}
                `}
              >
                <div className="flex justify-between items-start">
                  <div className="flex-1 min-w-0 pr-2">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-lg">{
                        notification.category === 'success' ? '✅' :
                        notification.category === 'warning' ? '⚠️' :
                        notification.category === 'error' ? '❌' : 'ℹ️'
                      }</span>
                      <p className="text-sm font-medium leading-tight">{notification.message}</p>
                    </div>
                    
                    {notification.type === 'status_change' && notification.oldStatus && (
                      <p className="text-xs opacity-75 mb-1">
                        Changed from {getStatusDisplayName(notification.oldStatus)}
                      </p>
                    )}
                    
                    <div className="flex items-center justify-between">
                      <p className="text-xs opacity-75">
                        {notification.timestamp.toLocaleTimeString()}
                      </p>
                      
                      {notification.settlementId && (
                        <Badge variant="secondary" className="text-xs px-1 py-0">
                          ID: {notification.settlementId.substring(0, 8)}...
                        </Badge>
                      )}
                    </div>

                    {isImportant && (
                      <div className="mt-2 pt-2 border-t border-current border-opacity-20">
                        <p className="text-xs font-medium">
                          {notification.status === 'PENDING_TENANT_PAYMENT' && userType === 'tenant' ? '💳 Payment required' :
                           notification.status === 'PENDING_OWNER_PAYMENT' && userType === 'owner' ? '💰 Refund pending' :
                           notification.status === 'COMPLETED' ? '🎉 Process complete' :
                           notification.status === 'SETTLEMENT_DONE' ? '✅ Settlement finalized' : 'Action may be required'}
                        </p>
                      </div>
                    )}
                  </div>
                  
                  <button
                    onClick={() => dismissNotification(notification.id)}
                    className="text-lg leading-none opacity-50 hover:opacity-100 transition-opacity"
                  >
                    ×
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default SettlementRealTimeIndicator;