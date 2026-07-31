import { useState, useEffect } from 'react';
import { Badge } from './ui/Badge';
import { Button } from './ui/Button';
import useRealTimeSettlementUpdates from '../hooks/useRealTimeSettlementUpdates';

/**
 * Dedicated component for managing real-time settlement notifications
 * Displays a notification center with detailed settlement updates
 */
const SettlementRealTimeNotifications = ({
  userType = 'tenant',
  settlementIds = [],
  onNotificationAction,
  className = "",
  maxNotifications = 10,
  autoHideDelay = 15000,
  showNotificationCenter = true
}) => {
  const [notifications, setNotifications] = useState([]);
  const [isExpanded, setIsExpanded] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);

  const {
    connectionStatus,
    lastUpdated,
    getSettlementStats,
    getPendingActionSettlements,
    refresh
  } = useRealTimeSettlementUpdates({
    userType,
    settlementIds,
    pollingInterval: 3000,
    enabled: true,
    enableNotifications: true, // Enable browser notifications
    onStatusChange: (change) => {
      const notification = createStatusChangeNotification(change);
      addNotification(notification);
    },
    onTransactionUpdate: (update) => {
      const notification = createTransactionUpdateNotification(update);
      addNotification(notification);
    },
    onError: (err, message) => {
      const notification = createErrorNotification(message);
      addNotification(notification);
    }
  });

  const createStatusChangeNotification = (change) => ({
    id: `status-${change.settlementId}-${Date.now()}`,
    type: 'status_change',
    category: getStatusCategory(change.newStatus),
    title: 'Settlement Status Update',
    message: `Settlement ${change.settlementId.substring(0, 8)}... status changed to ${getStatusDisplayName(change.newStatus)}`,
    details: {
      settlementId: change.settlementId,
      oldStatus: change.oldStatus,
      newStatus: change.newStatus,
      settlement: change.settlement
    },
    timestamp: new Date(),
    isRead: false,
    priority: getStatusPriority(change.newStatus),
    actions: getStatusActions(change.newStatus, change.settlement, userType)
  });

  const createTransactionUpdateNotification = (update) => ({
    id: `transaction-${update.settlementId}-${Date.now()}`,
    type: 'transaction_update',
    category: 'info',
    title: 'Transaction Update',
    message: update.type === 'amount_change' 
      ? `Settlement amount updated: ₹${Math.abs(update.newAmount || 0).toLocaleString()}`
      : 'Settlement transaction data has been updated',
    details: {
      settlementId: update.settlementId,
      updateType: update.type,
      oldAmount: update.oldAmount,
      newAmount: update.newAmount,
      settlement: update.settlement
    },
    timestamp: new Date(),
    isRead: false,
    priority: 'normal'
  });

  const createErrorNotification = (message) => ({
    id: `error-${Date.now()}`,
    type: 'error',
    category: 'error',
    title: 'Connection Issue',
    message: message || 'Unable to fetch settlement updates',
    timestamp: new Date(),
    isRead: false,
    priority: 'low',
    actions: [{
      label: 'Retry',
      action: 'retry',
      variant: 'secondary'
    }]
  });

  const addNotification = (notification) => {
    setNotifications(prev => {
      const updated = [notification, ...prev].slice(0, maxNotifications);
      return updated;
    });

    setUnreadCount(prev => prev + 1);

    // Auto-hide notification based on priority
    const hideDelay = notification.priority === 'high' ? autoHideDelay * 2 : 
                     notification.priority === 'low' ? autoHideDelay / 2 : autoHideDelay;

    setTimeout(() => {
      setNotifications(prev => prev.filter(n => n.id !== notification.id));
    }, hideDelay);
  };

  const getStatusCategory = (status) => {
    const categories = {
      'COMPLETED': 'success',
      'SETTLEMENT_DONE': 'success',
      'SETTLEMENT_APPROVED': 'success',
      'PENDING_TENANT_PAYMENT': 'warning',
      'PENDING_OWNER_PAYMENT': 'warning',
      'SETTLEMENT_TRANSACTION_CREATED': 'info',
      'REJECTED': 'error',
      'CANCELLED': 'error'
    };
    return categories[status] || 'info';
  };

  const getStatusDisplayName = (status) => {
    const names = {
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
    return names[status] || status;
  };

  const getStatusPriority = (status) => {
    const highPriority = ['PENDING_TENANT_PAYMENT', 'PENDING_OWNER_PAYMENT', 'COMPLETED'];
    const normalPriority = ['SETTLEMENT_TRANSACTION_CREATED', 'SETTLEMENT_APPROVED', 'SETTLEMENT_DONE'];
    
    if (highPriority.includes(status)) return 'high';
    if (normalPriority.includes(status)) return 'normal';
    return 'low';
  };

  const getStatusActions = (status, settlement, userType) => {
    const actions = [];
    
    if (status === 'PENDING_TENANT_PAYMENT' && userType === 'tenant') {
      actions.push({
        label: 'Make Payment',
        action: 'make_payment',
        variant: 'primary',
        data: { settlementId: settlement?.settlementId }
      });
    }
    
    if (status === 'PENDING_OWNER_PAYMENT' && userType === 'owner') {
      actions.push({
        label: 'Process Refund',
        action: 'process_refund',
        variant: 'primary',
        data: { settlementId: settlement?.settlementId }
      });
    }
    
    if (status === 'PENDING_OWNER_REVIEW' && userType === 'owner') {
      actions.push({
        label: 'Review Settlement',
        action: 'review_settlement',
        variant: 'primary',
        data: { settlementId: settlement?.settlementId }
      });
    }

    // Always add view details action
    actions.push({
      label: 'View Details',
      action: 'view_details',
      variant: 'secondary',
      data: { settlementId: settlement?.settlementId }
    });

    return actions;
  };

  const handleNotificationAction = (notification, action) => {
    if (action.action === 'retry') {
      refresh();
    } else if (onNotificationAction) {
      onNotificationAction(notification, action);
    }
  };

  const markAsRead = (notificationId) => {
    setNotifications(prev => 
      prev.map(n => n.id === notificationId ? { ...n, isRead: true } : n)
    );
    setUnreadCount(prev => Math.max(0, prev - 1));
  };

  const markAllAsRead = () => {
    setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
    setUnreadCount(0);
  };

  const clearNotification = (notificationId) => {
    setNotifications(prev => {
      const notification = prev.find(n => n.id === notificationId);
      if (notification && !notification.isRead) {
        setUnreadCount(prevCount => Math.max(0, prevCount - 1));
      }
      return prev.filter(n => n.id !== notificationId);
    });
  };

  const clearAllNotifications = () => {
    setNotifications([]);
    setUnreadCount(0);
  };

  const formatTimeAgo = (date) => {
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

  const getNotificationIcon = (category, type) => {
    if (type === 'status_change') {
      return category === 'success' ? '✅' : 
             category === 'warning' ? '⚠️' : 
             category === 'error' ? '❌' : '📋';
    }
    if (type === 'transaction_update') return '💰';
    if (type === 'error') return '🔴';
    return 'ℹ️';
  };

  if (!showNotificationCenter) return null;

  return (
    <div className={`relative ${className}`}>
      {/* Notification Toggle Button */}
      <button
        onClick={() => setIsExpanded(!isExpanded)}
        className="relative p-2 rounded-lg hover:bg-gray-100 transition-colors"
        title="Settlement Notifications"
      >
        <div className="w-6 h-6 text-gray-600">
          🔔
        </div>
        {unreadCount > 0 && (
          <Badge 
            variant="destructive" 
            className="absolute -top-1 -right-1 text-xs min-w-[1.5rem] h-6 px-1 rounded-full flex items-center justify-center"
          >
            {unreadCount > 99 ? '99+' : unreadCount}
          </Badge>
        )}
      </button>

      {/* Notification Panel */}
      {isExpanded && (
        <div className="absolute top-12 right-0 z-50 w-96 max-h-[600px] bg-white border border-gray-200 rounded-lg shadow-xl overflow-hidden">
          {/* Header */}
          <div className="p-4 border-b border-gray-200 bg-gray-50">
            <div className="flex items-center justify-between">
              <h3 className="font-semibold text-gray-900">
                Settlement Notifications
              </h3>
              <div className="flex items-center gap-2">
                {unreadCount > 0 && (
                  <button
                    onClick={markAllAsRead}
                    className="text-xs text-blue-600 hover:text-blue-800"
                  >
                    Mark all read
                  </button>
                )}
                <button
                  onClick={clearAllNotifications}
                  className="text-xs text-gray-500 hover:text-gray-700"
                >
                  Clear all
                </button>
                <button
                  onClick={() => setIsExpanded(false)}
                  className="text-gray-500 hover:text-gray-700"
                >
                  ✕
                </button>
              </div>
            </div>

            {/* Connection Status */}
            <div className="flex items-center gap-2 mt-2 text-sm">
              <div className={`w-2 h-2 rounded-full ${
                connectionStatus === 'connected' ? 'bg-green-500' :
                connectionStatus === 'connecting' ? 'bg-yellow-500' :
                connectionStatus === 'error' ? 'bg-red-500' : 'bg-gray-500'
              }`} />
              <span className="text-gray-600">
                {connectionStatus === 'connected' ? 'Live updates active' :
                 connectionStatus === 'connecting' ? 'Connecting...' :
                 connectionStatus === 'error' ? 'Connection error' : 'Disconnected'}
              </span>
              {lastUpdated && (
                <span className="text-gray-500">
                  • Updated {formatTimeAgo(lastUpdated)}
                </span>
              )}
            </div>
          </div>

          {/* Notifications List */}
          <div className="overflow-y-auto max-h-[400px]">
            {notifications.length === 0 ? (
              <div className="p-8 text-center text-gray-500">
                <div className="text-4xl mb-2">🔕</div>
                <p className="text-sm">No recent notifications</p>
              </div>
            ) : (
              <div className="divide-y divide-gray-200">
                {notifications.map(notification => (
                  <div
                    key={notification.id}
                    className={`p-4 hover:bg-gray-50 transition-colors ${
                      !notification.isRead ? 'bg-blue-50/30 border-l-4 border-l-blue-500' : ''
                    }`}
                  >
                    <div className="flex items-start gap-3">
                      <div className="text-xl flex-shrink-0">
                        {getNotificationIcon(notification.category, notification.type)}
                      </div>
                      
                      <div className="flex-1 min-w-0">
                        <div className="flex items-start justify-between gap-2">
                          <div className="flex-1">
                            <h4 className="text-sm font-medium text-gray-900">
                              {notification.title}
                            </h4>
                            <p className="text-sm text-gray-600 mt-1">
                              {notification.message}
                            </p>
                            
                            {notification.details?.settlementId && (
                              <div className="mt-2">
                                <Badge variant="secondary" className="text-xs">
                                  Settlement: {notification.details.settlementId.substring(0, 8)}...
                                </Badge>
                              </div>
                            )}
                            
                            <div className="flex items-center justify-between mt-2">
                              <span className="text-xs text-gray-500">
                                {formatTimeAgo(notification.timestamp)}
                              </span>
                              
                              {notification.priority === 'high' && (
                                <Badge variant="destructive" className="text-xs">
                                  Important
                                </Badge>
                              )}
                            </div>
                          </div>
                          
                          <button
                            onClick={() => clearNotification(notification.id)}
                            className="text-gray-400 hover:text-gray-600 p-1"
                          >
                            ✕
                          </button>
                        </div>
                        
                        {/* Action Buttons */}
                        {notification.actions && notification.actions.length > 0 && (
                          <div className="flex gap-2 mt-3">
                            {notification.actions.map((action, index) => (
                              <Button
                                key={index}
                                label={action.label}
                                variant={action.variant}
                                size="sm"
                                onClick={() => {
                                  handleNotificationAction(notification, action);
                                  if (!notification.isRead) {
                                    markAsRead(notification.id);
                                  }
                                }}
                              />
                            ))}
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default SettlementRealTimeNotifications;