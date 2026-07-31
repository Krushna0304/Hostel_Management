import { useState, useEffect, useRef, useCallback } from 'react';
import settlementService from '../services/settlementService';

/**
 * Enhanced hook for real-time settlement status updates
 * Provides automatic polling for settlement data with enhanced status tracking
 * 
 * @param {Object} options - Configuration options
 * @param {number} options.pollingInterval - Polling interval in milliseconds (default: 3000)
 * @param {boolean} options.enabled - Whether polling is enabled (default: true) 
 * @param {Array} options.settlementIds - Specific settlement IDs to track
 * @param {string} options.userType - 'tenant' or 'owner' for different API calls
 * @param {Function} options.onStatusChange - Callback when status changes
 * @param {Function} options.onTransactionUpdate - Callback when transaction data changes
 * @param {Function} options.onError - Error callback
 * @param {boolean} options.enableNotifications - Enable browser notifications (default: false)
 * 
 * @returns {Object} - Enhanced return object with additional helpers
 */
const useRealTimeSettlementUpdates = (options = {}) => {
  const {
    pollingInterval = 3000, // 3 seconds default
    enabled = true,
    settlementIds = [],
    userType = 'tenant',
    onStatusChange,
    onTransactionUpdate,
    onError,
    enableNotifications = false
  } = options;

  const [settlements, setSettlements] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [isPolling, setIsPolling] = useState(false);
  const [connectionStatus, setConnectionStatus] = useState('connecting');
  const [statusHistory, setStatusHistory] = useState({});

  // Use refs to store the latest values for callbacks
  const settlementsRef = useRef(settlements);
  const intervalRef = useRef(null);
  const isMountedRef = useRef(true);
  const notificationPermissionRef = useRef(null);
  const onStatusChangeRef = useRef(onStatusChange);
  const onTransactionUpdateRef = useRef(onTransactionUpdate);
  const onErrorRef = useRef(onError);

  // Update refs when settlements change
  useEffect(() => {
    settlementsRef.current = settlements;
  }, [settlements]);

  useEffect(() => {
    onStatusChangeRef.current = onStatusChange;
  }, [onStatusChange]);

  useEffect(() => {
    onTransactionUpdateRef.current = onTransactionUpdate;
  }, [onTransactionUpdate]);

  useEffect(() => {
    onErrorRef.current = onError;
  }, [onError]);

  // Request notification permission if enabled
  useEffect(() => {
    if (enableNotifications && 'Notification' in window) {
      if (Notification.permission === 'default') {
        Notification.requestPermission().then(permission => {
          notificationPermissionRef.current = permission;
        });
      } else {
        notificationPermissionRef.current = Notification.permission;
      }
    }
  }, [enableNotifications]);

  // Show browser notification for status changes
  const showNotification = useCallback((title, body, options = {}) => {
    if (!enableNotifications || !('Notification' in window)) return;
    
    if (notificationPermissionRef.current === 'granted') {
      try {
        new Notification(title, {
          body,
          icon: '/favicon.ico', // You can customize this
          tag: 'settlement-update',
          ...options
        });
      } catch (error) {
        console.warn('Failed to show notification:', error);
      }
    }
  }, [enableNotifications]);

  // Enhanced status change detection with transaction tracking
  const detectChanges = useCallback((newSettlements, oldSettlements) => {
    const changes = {
      statusChanges: [],
      transactionUpdates: [],
      newSettlements: []
    };

    newSettlements.forEach(newSettlement => {
      const oldSettlement = oldSettlements.find(
        s => s.settlementId === newSettlement.settlementId
      );

      if (!oldSettlement) {
        // New settlement detected
        changes.newSettlements.push(newSettlement);
        return;
      }

      // Status change detection
      if (oldSettlement.status !== newSettlement.status) {
        const statusChange = {
          settlementId: newSettlement.settlementId,
          oldStatus: oldSettlement.status,
          newStatus: newSettlement.status,
          settlement: newSettlement,
          timestamp: new Date()
        };
        
        changes.statusChanges.push(statusChange);

        // Update status history
        setStatusHistory(prev => ({
          ...prev,
          [newSettlement.settlementId]: [
            ...(prev[newSettlement.settlementId] || []),
            statusChange
          ].slice(-5) // Keep last 5 status changes
        }));

        // Show notification for important status changes
        const notificationStatuses = [
          'SETTLEMENT_TRANSACTION_CREATED',
          'SETTLEMENT_APPROVED',
          'SETTLEMENT_DONE',
          'COMPLETED',
          'PENDING_TENANT_PAYMENT',
          'PENDING_OWNER_PAYMENT'
        ];

        if (notificationStatuses.includes(newSettlement.status)) {
          const messages = {
            SETTLEMENT_TRANSACTION_CREATED: 'Settlement transaction has been created',
            SETTLEMENT_APPROVED: 'Settlement has been approved by owner',
            SETTLEMENT_DONE: 'Settlement transaction completed successfully',
            COMPLETED: 'Settlement process completed',
            PENDING_TENANT_PAYMENT: 'Payment required from tenant',
            PENDING_OWNER_PAYMENT: 'Refund pending from owner'
          };

          showNotification(
            'Settlement Update',
            messages[newSettlement.status] || `Status changed to ${newSettlement.status}`,
            { tag: `settlement-${newSettlement.settlementId}` }
          );
        }
      }

      // Transaction data change detection
      const oldTransactionData = oldSettlement.settlementTransactionData;
      const newTransactionData = newSettlement.settlementTransactionData;
      
      if (JSON.stringify(oldTransactionData) !== JSON.stringify(newTransactionData)) {
        changes.transactionUpdates.push({
          settlementId: newSettlement.settlementId,
          oldTransactionData,
          newTransactionData,
          settlement: newSettlement,
          timestamp: new Date()
        });
      }

      // Amount change detection (for enhanced settlements)
      if (oldSettlement.finalSettlementAmount !== newSettlement.finalSettlementAmount) {
        changes.transactionUpdates.push({
          settlementId: newSettlement.settlementId,
          type: 'amount_change',
          oldAmount: oldSettlement.finalSettlementAmount,
          newAmount: newSettlement.finalSettlementAmount,
          settlement: newSettlement,
          timestamp: new Date()
        });
      }
    });

    return changes;
  }, [showNotification]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      isMountedRef.current = false;
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, []);

  // Enhanced fetch settlements with better error handling and status tracking
  const fetchSettlements = useCallback(async (showLoading = false) => {
    try {
      if (showLoading && isMountedRef.current) {
        setLoading(true);
      }

      setConnectionStatus('connected');

      const data = userType === 'owner' 
        ? await settlementService.getOwnerSettlements()
        : await settlementService.getTenantSettlements();

      if (!isMountedRef.current) return;

      // Filter by specific settlement IDs if provided
      const filteredData = settlementIds.length > 0
        ? data.filter(settlement => settlementIds.includes(settlement.settlementId))
        : data;

      // Detect all types of changes
      const changes = detectChanges(filteredData, settlementsRef.current);

      // Fire callbacks for status changes
      if (onStatusChangeRef.current && changes.statusChanges.length > 0) {
        changes.statusChanges.forEach(change => onStatusChangeRef.current(change));
      }

      // Fire callbacks for transaction updates
      if (onTransactionUpdateRef.current && changes.transactionUpdates.length > 0) {
        changes.transactionUpdates.forEach(update => onTransactionUpdateRef.current(update));
      }

      setSettlements(filteredData);
      setLastUpdated(new Date());
      setError(null);

    } catch (err) {
      console.error('Error fetching settlements for real-time updates:', err);
      
      if (!isMountedRef.current) return;
      
      setConnectionStatus('error');
      
      // Only set error if it's not a 403 (handled gracefully by service)
      if (err.response?.status !== 403) {
        const errorMessage = err.response?.data?.message || 'Failed to fetch settlement updates';
        setError(errorMessage);
        
        if (onErrorRef.current) {
          onErrorRef.current(err, errorMessage);
        }
      }
    } finally {
      if (isMountedRef.current && showLoading) {
        setLoading(false);
      }
    }
  }, [userType, settlementIds, detectChanges]);

  // Start polling with enhanced connection monitoring
  const startPolling = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
    }

    setConnectionStatus('connecting');

    // Fetch immediately
    fetchSettlements(true);

    // Set up polling interval
    intervalRef.current = setInterval(() => {
      fetchSettlements(false);
    }, pollingInterval);

    setIsPolling(true);
  }, [fetchSettlements, pollingInterval]);

  // Stop polling with status update
  const stopPolling = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
    setConnectionStatus('disconnected');
    setIsPolling(false);
  }, []);

  // Manual refresh
  const refresh = useCallback(() => {
    return fetchSettlements(true);
  }, [fetchSettlements]);

  // Setup polling when enabled changes
  useEffect(() => {
    if (enabled && !isPolling) {
      startPolling();
    } else if (!enabled && isPolling) {
      stopPolling();
    }
  }, [enabled, isPolling, startPolling, stopPolling]);

  // Cleanup interval on unmount or when polling stops
  useEffect(() => {
    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, []);

  return {
    settlements,
    loading,
    error,
    lastUpdated,
    isPolling,
    connectionStatus,
    statusHistory,
    refresh,
    startPolling,
    stopPolling,
    
    // Enhanced helper methods
    getSettlementById: useCallback((settlementId) => {
      return settlements.find(s => s.settlementId === settlementId);
    }, [settlements]),
    
    getSettlementsByStatus: useCallback((status) => {
      return settlements.filter(s => s.status === status);
    }, [settlements]),

    getSettlementHistory: useCallback((settlementId) => {
      return statusHistory[settlementId] || [];
    }, [statusHistory]),

    // Enhanced settlement status helpers
    getEnhancedSettlements: useCallback(() => {
      return settlements.filter(s => 
        s.settlementTransactionData || 
        ['SETTLEMENT_TRANSACTION_CREATED', 'SETTLEMENT_APPROVED', 'SETTLEMENT_DONE'].includes(s.status)
      );
    }, [settlements]),

    getPendingActionSettlements: useCallback(() => {
      const userPendingStatuses = userType === 'tenant' 
        ? ['PENDING_TENANT_PAYMENT', 'PAYMENT_IN_PROGRESS']
        : ['PENDING_OWNER_REVIEW', 'CALCULATION_IN_PROGRESS', 'PENDING_OWNER_PAYMENT'];
      
      return settlements.filter(s => userPendingStatuses.includes(s.status));
    }, [settlements, userType]),

    getCompletedSettlements: useCallback(() => {
      return settlements.filter(s => 
        ['COMPLETED', 'SETTLEMENT_DONE'].includes(s.status)
      );
    }, [settlements]),

    // Real-time statistics
    getSettlementStats: useCallback(() => {
      const stats = {
        total: settlements.length,
        completed: 0,
        pending: 0,
        inProgress: 0,
        requiresAction: 0
      };

      settlements.forEach(settlement => {
        if (['COMPLETED', 'SETTLEMENT_DONE'].includes(settlement.status)) {
          stats.completed++;
        } else if (['PENDING_OWNER_REVIEW', 'PENDING_TENANT_PAYMENT', 'PENDING_OWNER_PAYMENT'].includes(settlement.status)) {
          stats.pending++;
        } else if (['CALCULATION_IN_PROGRESS', 'PAYMENT_IN_PROGRESS'].includes(settlement.status)) {
          stats.inProgress++;
        } else {
          stats.requiresAction++;
        }
      });

      return stats;
    }, [settlements])
  };
};

export default useRealTimeSettlementUpdates;