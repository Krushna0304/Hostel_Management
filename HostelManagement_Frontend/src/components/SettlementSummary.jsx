import { Card } from './ui/Card';
import { Badge } from './ui/Badge';
import SettlementStatusBadge from './SettlementStatusBadge';
import SettlementRealTimeIndicator from './SettlementRealTimeIndicator';
import useRealTimeSettlementUpdates from '../hooks/useRealTimeSettlementUpdates';

const SettlementSummary = ({ 
  settlement, 
  showActions = false, 
  onAction, 
  showTransactionDetails = false,
  enableRealTimeUpdates = true,
  userType = 'tenant',
  onSettlementUpdate,
  showRealTimeIndicator = true,
  showEnhancedDetails = true
}) => {
  // Use real-time updates for this specific settlement
  const { 
    getSettlementById, 
    lastUpdated,
    connectionStatus,
    getSettlementHistory
  } = useRealTimeSettlementUpdates({
    enabled: enableRealTimeUpdates,
    settlementIds: settlement ? [settlement.settlementId] : [],
    userType,
    pollingInterval: 2000, // More frequent updates for individual settlement
    onStatusChange: (change) => {
      if (onSettlementUpdate) {
        onSettlementUpdate(change);
      }
    },
    onTransactionUpdate: (update) => {
      if (onSettlementUpdate) {
        onSettlementUpdate(update);
      }
    }
  });

  // Use real-time data if available, fallback to prop
  const currentSettlement = enableRealTimeUpdates 
    ? getSettlementById(settlement?.settlementId) || settlement
    : settlement;
    
  // Get status history for this settlement
  const statusHistory = enableRealTimeUpdates && settlement?.settlementId 
    ? getSettlementHistory(settlement.settlementId) 
    : [];
  const formatCurrency = (amount) => {
    return `₹${(amount || 0).toLocaleString()}`;
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  const formatDateTime = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
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

  const getSettlementTypeBadge = (type, amount) => {
    if (!type || amount === undefined || amount === null) return null;
    
    return type === 'OWNER_PAYABLE' ? (
      <Badge variant="success">
        Owner Pays: {formatCurrency(Math.abs(amount))}
      </Badge>
    ) : (
      <Badge variant="destructive">
        Tenant Pays: {formatCurrency(Math.abs(amount))}
      </Badge>
    );
  };

  // Parse transaction data if available
  const getTransactionData = () => {
    if (!currentSettlement.settlementTransactionData) return null;
    
    try {
      return typeof currentSettlement.settlementTransactionData === 'string' 
        ? JSON.parse(currentSettlement.settlementTransactionData)
        : currentSettlement.settlementTransactionData;
    } catch (error) {
      console.error('Error parsing settlement transaction data:', error);
      return null;
    }
  };

  const transactionData = getTransactionData();
  const hasTransactionData = Boolean(transactionData);

  // Check if this is an early settlement
  const isEarlySettlement = currentSettlement.earlySettlementRequested || currentSettlement.isEarlyExit || 
    (transactionData && transactionData.isEarlyExit);

  // Get enhanced settlement statuses
  const getEnhancedStatusDisplay = () => {
    const enhancedStatuses = [
      'SETTLEMENT_TRANSACTION_CREATED',
      'SETTLEMENT_APPROVED', 
      'SETTLEMENT_DONE',
      'PENDING_OWNER_REVIEW',
      'CALCULATION_IN_PROGRESS',
      'PENDING_TENANT_PAYMENT',
      'PENDING_OWNER_PAYMENT',
      'PAYMENT_IN_PROGRESS'
    ];

    if (enhancedStatuses.includes(currentSettlement.status)) {
      return <SettlementStatusBadge status={currentSettlement.status} />;
    }

    return (
      <Badge variant={
        currentSettlement.status === 'COMPLETED' ? 'success' :
        currentSettlement.status === 'CANCELLED' ? 'secondary' :
        currentSettlement.status === 'REJECTED' ? 'destructive' :
        'warning'
      }>
        {currentSettlement.status || 'Unknown'}
      </Badge>
    );
  };

  return (
    <Card className="p-4">
      <div className="space-y-4">
        {/* Header */}
        <div className="flex justify-between items-start gap-4">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2">
              <h3 className="font-semibold text-lg">
                {currentSettlement.tenantName || 'Settlement Request'}
              </h3>
              {isEarlySettlement && (
                <Badge variant="warning" className="text-xs">
                  Early Exit
                </Badge>
              )}
            </div>
            <p className="text-sm text-gray-600">
              Room: {currentSettlement.roomNumber || 'N/A'}
            </p>
            <div className="flex items-center gap-4 text-xs text-gray-500">
              <span>Requested: {formatDate(currentSettlement.createdAt)}</span>
              {currentSettlement.requestedEndDate && (
                <span>End Date: {formatDate(currentSettlement.requestedEndDate)}</span>
              )}
            </div>
            
            {/* Real-time Status Indicator */}
            {enableRealTimeUpdates && showRealTimeIndicator && (
              <div className="mt-2">
                <SettlementRealTimeIndicator
                  userType={userType}
                  settlementIds={[currentSettlement.settlementId]}
                  onStatusChange={(change) => {
                    if (onSettlementUpdate) {
                      onSettlementUpdate(change);
                    }
                  }}
                  onTransactionUpdate={(update) => {
                    if (onSettlementUpdate) {
                      onSettlementUpdate(update);
                    }
                  }}
                  className="text-xs"
                  showConnectionStatus={true}
                  showLastUpdate={true}
                  showNotificationCount={true}
                  showStats={false}
                  enableBrowserNotifications={false}
                />
              </div>
            )}

            {/* Status History - Show recent status changes */}
            {enableRealTimeUpdates && showEnhancedDetails && statusHistory.length > 1 && (
              <div className="mt-2 p-2 bg-gray-50 rounded-lg">
                <p className="text-xs font-medium text-gray-600 mb-1">Recent Changes:</p>
                <div className="space-y-1">
                  {statusHistory.slice(-3).reverse().map((change, index) => (
                    <div key={index} className="flex items-center justify-between text-xs">
                      <span className="text-gray-600">
                        {getStatusDisplayName(change.newStatus)}
                      </span>
                      <span className="text-gray-500">
                        {formatTimeAgo(change.timestamp)}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
          <div className="flex-shrink-0 flex flex-col items-end gap-2">
            {getEnhancedStatusDisplay()}
            {currentSettlement.settlementType && currentSettlement.finalSettlementAmount !== undefined && 
              getSettlementTypeBadge(currentSettlement.settlementType, currentSettlement.finalSettlementAmount)
            }
          </div>
        </div>

        {/* Transaction ID and metadata for enhanced settlements */}
        {hasTransactionData && transactionData.transactionId && (
          <div className="p-2 bg-blue-50 rounded-lg border border-blue-200">
            <div className="flex justify-between items-center text-sm">
              <span className="font-medium text-blue-800">
                Transaction: {transactionData.transactionId.substring(0, 8)}...
              </span>
              <span className="text-blue-600">
                {formatDate(transactionData.calculationDate)}
              </span>
            </div>
            {transactionData.notes && (
              <p className="text-xs text-blue-700 mt-1">{transactionData.notes}</p>
            )}
          </div>
        )}

        {/* Enhanced Financial Breakdown */}
        {showTransactionDetails && hasTransactionData ? (
          <div className="space-y-3">
            <h4 className="font-medium text-sm text-gray-700">Transaction Details</h4>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-3 text-sm">
              <div className="text-center p-2 bg-green-50 rounded">
                <p className="text-xs text-green-600 font-medium">Security Deposit</p>
                <p className="font-semibold text-green-700">
                  {formatCurrency(transactionData.securityDeposit)}
                </p>
              </div>
              {(transactionData.outstandingRent || 0) > 0 && (
                <div className="text-center p-2 bg-red-50 rounded">
                  <p className="text-xs text-red-600 font-medium">Outstanding Rent</p>
                  <p className="font-semibold text-red-700">
                    {formatCurrency(transactionData.outstandingRent)}
                  </p>
                </div>
              )}
              {(transactionData.outstandingCharges || 0) > 0 && (
                <div className="text-center p-2 bg-red-50 rounded">
                  <p className="text-xs text-red-600 font-medium">Other Charges</p>
                  <p className="font-semibold text-red-700">
                    {formatCurrency(transactionData.outstandingCharges)}
                  </p>
                </div>
              )}
              {(transactionData.damageCharges || 0) > 0 && (
                <div className="text-center p-2 bg-orange-50 rounded">
                  <p className="text-xs text-orange-600 font-medium">Damage Charges</p>
                  <p className="font-semibold text-orange-700">
                    {formatCurrency(transactionData.damageCharges)}
                  </p>
                </div>
              )}
              {(transactionData.cleaningCharges || 0) > 0 && (
                <div className="text-center p-2 bg-orange-50 rounded">
                  <p className="text-xs text-orange-600 font-medium">Cleaning Charges</p>
                  <p className="font-semibold text-orange-700">
                    {formatCurrency(transactionData.cleaningCharges)}
                  </p>
                </div>
              )}
              {(transactionData.earlyExitPenalty || 0) > 0 && (
                <div className="text-center p-2 bg-red-100 rounded">
                  <p className="text-xs text-red-600 font-medium">
                    Early Exit Penalty
                    {transactionData.earlyExitDays && ` (${transactionData.earlyExitDays} days)`}
                  </p>
                  <p className="font-semibold text-red-700">
                    {formatCurrency(transactionData.earlyExitPenalty)}
                  </p>
                </div>
              )}
              {(transactionData.otherDeductions || 0) > 0 && (
                <div className="text-center p-2 bg-orange-50 rounded">
                  <p className="text-xs text-orange-600 font-medium">Other Deductions</p>
                  <p className="font-semibold text-orange-700">
                    {formatCurrency(transactionData.otherDeductions)}
                  </p>
                </div>
              )}
            </div>
            
            {/* Total Deductions */}
            {(transactionData.totalDeductions || 0) > 0 && (
              <div className="p-2 bg-gray-50 rounded-lg border">
                <div className="flex justify-between items-center">
                  <span className="text-sm font-medium text-gray-700">Total Deductions</span>
                  <span className="font-semibold text-gray-800">
                    {formatCurrency(transactionData.totalDeductions)}
                  </span>
                </div>
              </div>
            )}
          </div>
        ) : (
          /* Original Financial Breakdown for backward compatibility */
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm">
            <div className="text-center p-2 bg-green-50 rounded">
              <p className="text-xs text-green-600 font-medium">Security Deposit</p>
              <p className="font-semibold text-green-700">
                {formatCurrency(currentSettlement.securityDeposit)}
              </p>
            </div>
            <div className="text-center p-2 bg-red-50 rounded">
              <p className="text-xs text-red-600 font-medium">Outstanding Rent</p>
              <p className="font-semibold text-red-700">
                {formatCurrency(currentSettlement.outstandingRent)}
              </p>
            </div>
            <div className="text-center p-2 bg-red-50 rounded">
              <p className="text-xs text-red-600 font-medium">Other Charges</p>
              <p className="font-semibold text-red-700">
                {formatCurrency(currentSettlement.outstandingCharges)}
              </p>
            </div>
            <div className="text-center p-2 bg-orange-50 rounded">
              <p className="text-xs text-orange-600 font-medium">Deductions</p>
              <p className="font-semibold text-orange-700">
                {formatCurrency(
                  (currentSettlement.damageCharges || 0) + 
                  (currentSettlement.cleaningCharges || 0) + 
                  (currentSettlement.otherDeductions || 0)
                )}
              </p>
            </div>
          </div>
        )}

        {/* Final Amount */}
        {currentSettlement.finalSettlementAmount !== undefined && (
          <div className="p-3 bg-blue-50 rounded-lg border border-blue-200">
            <div className="text-center">
              <p className="text-sm text-blue-600 font-medium">Final Settlement Amount</p>
              <p className="text-xl font-bold text-blue-800">
                {formatCurrency(Math.abs(currentSettlement.finalSettlementAmount))}
              </p>
              <p className="text-xs text-blue-600">
                {currentSettlement.finalSettlementAmount >= 0
                  ? 'Owner will refund this amount' 
                  : 'Tenant needs to pay this amount'
                }
              </p>
              {currentSettlement.status === 'COMPLETED' && currentSettlement.settledAt && (
                <div className="mt-2 pt-2 border-t border-blue-200">
                  <p className="text-xs text-green-600 font-medium">
                    ✓ Completed on {formatDate(currentSettlement.settledAt)}
                  </p>
                  {currentSettlement.paymentReference && (
                    <p className="text-xs text-gray-500 mt-1">
                      Ref: {currentSettlement.paymentReference}
                    </p>
                  )}
                </div>
              )}
              {currentSettlement.status === 'SETTLEMENT_DONE' && (
                <div className="mt-2 pt-2 border-t border-blue-200">
                  <p className="text-xs text-green-600 font-medium">
                    ✓ Settlement Transaction Completed
                  </p>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Enhanced Notes Section */}
        <div className="space-y-2">
          {currentSettlement.tenantNotes && (
            <div className="p-3 bg-gray-50 rounded-lg">
              <p className="text-xs font-medium text-gray-700 mb-1">Tenant Notes:</p>
              <p className="text-sm text-gray-600">{currentSettlement.tenantNotes}</p>
            </div>
          )}

          {currentSettlement.ownerNotes && (
            <div className="p-3 bg-blue-50 rounded-lg">
              <p className="text-xs font-medium text-blue-700 mb-1">Owner Notes:</p>
              <p className="text-sm text-blue-600">{currentSettlement.ownerNotes}</p>
            </div>
          )}

          {currentSettlement.damageDescription && (
            <div className="p-3 bg-red-50 rounded-lg">
              <p className="text-xs font-medium text-red-700 mb-1">Damage Description:</p>
              <p className="text-sm text-red-600">{currentSettlement.damageDescription}</p>
            </div>
          )}

          {/* Early settlement specific information */}
          {isEarlySettlement && currentSettlement.requestedEndDate && (
            <div className="p-3 bg-yellow-50 rounded-lg border border-yellow-200">
              <p className="text-xs font-medium text-yellow-700 mb-1">Early Settlement Request:</p>
              <p className="text-sm text-yellow-600">
                Requested end date: {formatDate(currentSettlement.requestedEndDate)}
              </p>
              {transactionData && transactionData.earlyExitDays && (
                <p className="text-xs text-yellow-600 mt-1">
                  {transactionData.earlyExitDays} days before original end date
                </p>
              )}
            </div>
          )}
        </div>

        {/* Transaction Timestamps */}
        {hasTransactionData && transactionData.createdAt && (
          <div className="text-xs text-gray-500 space-y-1">
            <div className="flex justify-between">
              <span>Transaction Created:</span>
              <span>{formatDateTime(transactionData.createdAt)}</span>
            </div>
            {transactionData.createdBy && (
              <div className="flex justify-between">
                <span>Created By:</span>
                <span>{transactionData.createdBy}</span>
              </div>
            )}
          </div>
        )}

        {/* Actions */}
        {showActions && onAction && (
          <div className="pt-2 border-t">
            {onAction(currentSettlement)}
          </div>
        )}
      </div>
    </Card>
  );
};

export default SettlementSummary;