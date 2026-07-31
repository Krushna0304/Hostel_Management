import { useState, useEffect } from 'react';
import { Alert, Button } from '../../components/ui';
import EmptyState from '../../components/ui/EmptyState';
import LoadingScreen from '../../components/ui/LoadingScreen';
import SettlementCalculationModal from '../../components/SettlementCalculationModal';
import SettlementTransactionModal from '../../components/SettlementTransactionModal';
import SettlementCollectionModal from '../../components/SettlementCollectionModal';
import SettlementPaymentModal from '../../components/SettlementPaymentModal';
import SettlementStatusBadge from '../../components/SettlementStatusBadge';
import SettlementSummary from '../../components/SettlementSummary';
import SettlementRealTimeIndicator from '../../components/SettlementRealTimeIndicator';
import settlementService from '../../services/settlementService';
import useRealTimeSettlementUpdates from '../../hooks/useRealTimeSettlementUpdates';
import { useSuccessPopup } from '../../hooks/useSuccessPopup';

const Settlements = () => {
  const [settlements, setSettlements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedSettlement, setSelectedSettlement] = useState(null);
  const [selectedAgreement, setSelectedAgreement] = useState(null);
  const [showCalculationModal, setShowCalculationModal] = useState(false);
  const [showTransactionModal, setShowTransactionModal] = useState(false);
  const [showCollectionModal, setShowCollectionModal] = useState(false);
  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const [confirmLeftSettlement, setConfirmLeftSettlement] = useState(null);
  const [confirmingLeft, setConfirmingLeft] = useState(false);
  const [enableRealTimeUpdates, setEnableRealTimeUpdates] = useState(true);
  const { showSuccess } = useSuccessPopup();

  // Real-time updates for all settlements
  const {
    settlements: realTimeSettlements,
    loading: realTimeLoading,
    error: realTimeError,
    lastUpdated,
    connectionStatus,
    refresh,
    isPolling
  } = useRealTimeSettlementUpdates({
    userType: 'owner',
    enabled: enableRealTimeUpdates,
    pollingInterval: 3000, // 3 seconds
    onStatusChange: (change) => {
      showSuccess(`Settlement ${change.settlementId.substring(0, 8)}... status changed to ${change.newStatus}`);
    },
    onError: (err, message) => {
      console.warn('Real-time updates error:', message);
    }
  });

  useEffect(() => {
    // Fetch initial settlements if real-time updates are disabled
    // or as a fallback when real-time updates are not available
    if (!enableRealTimeUpdates) {
      fetchSettlements();
    }
  }, [enableRealTimeUpdates]);

  // Use real-time settlements if available, fallback to manual fetch
  useEffect(() => {
    if (enableRealTimeUpdates && realTimeSettlements.length > 0) {
      setSettlements(realTimeSettlements);
      setLoading(false);
      setError('');
    } else if (!enableRealTimeUpdates || realTimeError) {
      // Fallback to manual fetching
      if (!settlements.length) {
        fetchSettlements();
      }
    }
  }, [realTimeSettlements, enableRealTimeUpdates, realTimeError]);
  
  useEffect(() => {
    if (enableRealTimeUpdates && !realTimeLoading) {
      setLoading(false);
    }
  }, [enableRealTimeUpdates, realTimeLoading]);

  const fetchSettlements = async () => {
    try {
      setLoading(true);
      setError('');
      const data = await settlementService.getOwnerSettlements();
      setSettlements(data);
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to load settlement requests.');
    } finally {
      setLoading(false);
    }
  };

  // Refresh function that works with both real-time and manual modes
  const handleRefresh = async () => {
    if (enableRealTimeUpdates) {
      await refresh();
    } else {
      await fetchSettlements();
    }
  };

  const handleViewSettlement = (settlement) => {
    setSelectedSettlement(settlement);
    setShowCalculationModal(true);
  };

  const handleCreateTransaction = (agreement) => {
    setSelectedAgreement(agreement);
    setShowTransactionModal(true);
  };

  const handleCollectPayment = (settlement) => {
    setSelectedSettlement(settlement);
    setShowCollectionModal(true);
  };

  const handleMakePayment = (settlement) => {
    setSelectedSettlement(settlement);
    setShowPaymentModal(true);
  };

  const handleOwnerConfirmLeft = async () => {
    try {
      setConfirmingLeft(true);
      await settlementService.ownerConfirmLeft(confirmLeftSettlement.allotmentId);
      showSuccess('Confirmed tenant has left. Allotment will be marked as LEFT once both parties confirm.');
      setConfirmLeftSettlement(null);
      handleRefresh();
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to confirm. Please try again.');
      setConfirmLeftSettlement(null);
    } finally {
      setConfirmingLeft(false);
    }
  };

  const handleCompletePayment = async (settlementId) => {
    try {
      const paymentReference = prompt('Enter payment reference (transaction ID, check number, etc.):');
      if (!paymentReference) return;

      await settlementService.completeSettlement(settlementId, paymentReference);
      showSuccess('Settlement marked as completed!');
      handleRefresh();
    } catch (error) {
      console.error('Error completing settlement:', error);
    }
  };

  const renderSettlementActions = (settlement) => {
    if (settlement.status === 'PENDING_OWNER_REVIEW') {
      return (
        <Button
          onClick={() => handleViewSettlement(settlement)}
          className="w-full"
        >
          Review & Calculate
        </Button>
      );
    }
    
    if (settlement.status === 'PENDING_OWNER_PAYMENT') {
      // Check if owner needs to pay (positive amount) or collect (negative amount)
      const isOwnerPaying = settlement.finalSettlementAmount > 0;
      
      return (
        <Button
          onClick={() => isOwnerPaying ? handleMakePayment(settlement) : handleCollectPayment(settlement)}
          variant="success"
          className="w-full"
        >
          {isOwnerPaying ? 'Make Payment' : 'Collect Payment'}
        </Button>
      );
    }
    
    if (settlement.status === 'PENDING_TENANT_PAYMENT') {
      // Tenant owes money, owner should collect
      return (
        <Button
          onClick={() => handleCollectPayment(settlement)}
          variant="success"
          className="w-full"
        >
          Collect Payment
        </Button>
      );
    }

    // Enhanced settlement statuses
    if (settlement.status === 'SETTLEMENT_REQUESTED') {
      return (
        <Button
          onClick={() => handleViewSettlement(settlement)}
          className="w-full"
        >
          Review Settlement Request
        </Button>
      );
    }

    if (settlement.status === 'SETTLEMENT_TRANSACTION_CREATED') {
      // Check settlement type to determine action
      if (settlement.settlementType === 'OWNER_PAYABLE' && settlement.finalSettlementAmount > 0) {
        return (
          <Button
            onClick={() => handleMakePayment(settlement)}
            variant="success"
            className="w-full"
          >
            Make Refund Payment
          </Button>
        );
      }
      
      if (settlement.settlementType === 'TENANT_PAYABLE' && settlement.finalSettlementAmount < 0) {
        return (
          <Button
            onClick={() => handleCollectPayment(settlement)}
            variant="success"
            className="w-full"
          >
            Collect Payment from Tenant
          </Button>
        );
      }

      return (
        <Button
          onClick={() => handleViewSettlement(settlement)}
          variant="outline"
          className="w-full"
        >
          View Transaction Details
        </Button>
      );
    }

    if (settlement.status === 'SETTLEMENT_APPROVED' || settlement.status === 'SETTLEMENT_DONE') {
      return (
        <div className="text-sm text-green-600 text-center py-2">
          {settlement.status === 'SETTLEMENT_DONE' 
            ? '✓ Settlement completed' 
            : 'Settlement approved - processing'
          }
        </div>
      );
    }
    
    if (settlement.allotmentStatus === 'ON_NOTICE_PERIOD' && !settlement.ownerMarkedLeft) {
      return (
        <div className="flex flex-col gap-2 w-full">
          {settlement.tenantMarkedLeft && (
            <p className="text-xs text-amber-700 font-medium text-center">
              Tenant has confirmed vacating. Waiting for your confirmation.
            </p>
          )}
          <Button
            onClick={() => setConfirmLeftSettlement(settlement)}
            variant="warning"
            className="w-full"
          >
            Confirm Tenant Left
          </Button>
          <Button
            onClick={() => handleViewSettlement(settlement)}
            variant="outline"
            className="w-full"
          >
            View Details
          </Button>
        </div>
      );
    }

    return (
      <Button
        onClick={() => handleViewSettlement(settlement)}
        variant="outline"
        className="w-full"
      >
        View Details
      </Button>
    );
  };

  // Determine loading state from both sources
  const isLoading = enableRealTimeUpdates 
    ? (realTimeLoading && settlements.length === 0) || loading
    : loading;

  // Determine error state
  const currentError = enableRealTimeUpdates 
    ? (realTimeError && !settlements.length ? realTimeError : error)
    : error;

  if (isLoading) {
    return <LoadingScreen />;
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold">Settlement Requests</h1>
          <p className="text-gray-600">Manage agreement settlement requests from tenants</p>
        </div>
        
        {/* Real-time Controls */}
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            <button
              onClick={() => setEnableRealTimeUpdates(!enableRealTimeUpdates)}
              className={`px-3 py-1 rounded text-sm font-medium transition-colors ${
                enableRealTimeUpdates 
                  ? 'bg-green-100 text-green-700 hover:bg-green-200' 
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              {enableRealTimeUpdates ? 'Real-time: ON' : 'Real-time: OFF'}
            </button>
            
            {!enableRealTimeUpdates && (
              <Button
                onClick={handleRefresh}
                size="sm"
                variant="outline"
              >
                Refresh
              </Button>
            )}
          </div>
          
          {enableRealTimeUpdates && (
            <SettlementRealTimeIndicator
              userType="owner"
              enabled={false}
              settlementState={{
                lastUpdated,
                error: realTimeError,
                isPolling,
                connectionStatus,
                statusHistory: {},
                refresh,
              }}
              onStatusChange={(change) => {
                showSuccess(`Settlement ${change.settlementId.substring(0, 8)}... status updated`);
              }}
              className="text-sm"
            />
          )}
        </div>
      </div>

      {currentError ? (
        <Alert tone="error">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <span>{currentError}</span>
            <Button label="Retry" variant="secondary" onClick={handleRefresh} />
          </div>
        </Alert>
      ) : null}

      {!currentError && settlements.length === 0 ? (
        <EmptyState
          title="No Settlement Requests"
          description="You don't have any settlement requests yet. Tenants can request settlements from their dashboard."
          icon="clipboard"
        />
      ) : (
        <div className="space-y-4">
          {settlements.map((settlement) => (
            <div key={settlement.settlementId} className="relative">
              <div className="absolute top-4 right-4 z-10">
                <SettlementStatusBadge status={settlement.status} />
              </div>
              <SettlementSummary
                settlement={settlement}
                showActions={true}
                showTransactionDetails={true}
                enableRealTimeUpdates={enableRealTimeUpdates}
                userType="owner"
                onSettlementUpdate={(change) => {
                  showSuccess(`Settlement updated: ${change.newStatus}`);
                }}
                onAction={renderSettlementActions}
              />
            </div>
          ))}
        </div>
      )}

      {showCalculationModal && (
        <SettlementCalculationModal
          isOpen={showCalculationModal}
          onClose={() => {
            setShowCalculationModal(false);
            setSelectedSettlement(null);
          }}
          settlementId={selectedSettlement?.settlementId}
          settlement={selectedSettlement}
          userType="owner"
          enableRealTimeUpdates={enableRealTimeUpdates}
          onSuccess={handleRefresh}
        />
      )}

      {showTransactionModal && (
        <SettlementTransactionModal
          isOpen={showTransactionModal}
          onClose={() => {
            setShowTransactionModal(false);
            setSelectedAgreement(null);
          }}
          agreement={selectedAgreement}
          onSuccess={handleRefresh}
        />
      )}

      {showCollectionModal && (
        <SettlementCollectionModal
          settlement={selectedSettlement}
          onClose={() => {
            setShowCollectionModal(false);
            setSelectedSettlement(null);
          }}
          onSuccess={handleRefresh}
        />
      )}

      {showPaymentModal && (
        <SettlementPaymentModal
          settlement={selectedSettlement}
          onClose={() => {
            setShowPaymentModal(false);
            setSelectedSettlement(null);
          }}
          onSuccess={handleRefresh}
        />
      )}

      {confirmLeftSettlement && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4">
          <div className="w-full max-w-sm rounded-3xl bg-white p-6 shadow-2xl">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-amber-500">Confirm vacating</p>
            <h3 className="mt-2 text-xl font-bold text-slate-950">Confirm {confirmLeftSettlement.tenantName} has left?</h3>
            <p className="mt-2 text-sm text-slate-500">
              Room: <strong>{confirmLeftSettlement.roomNumber}</strong>. This confirms the tenant has physically vacated the room.
              {!confirmLeftSettlement.tenantMarkedLeft && ' The tenant will also need to confirm before the allotment is marked as LEFT.'}
            </p>
            <div className="mt-5 flex gap-3">
              <Button
                label="Cancel"
                variant="secondary"
                fullWidth
                onClick={() => setConfirmLeftSettlement(null)}
              />
              <Button
                label="Yes, tenant has left"
                fullWidth
                loading={confirmingLeft}
                onClick={handleOwnerConfirmLeft}
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Settlements;