import { useState, useEffect } from 'react';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import EmptyState from '../../components/ui/EmptyState';
import LoadingScreen from '../../components/ui/LoadingScreen';
import { Alert } from '../../components/ui/Alert';
import SettlementRequestModal from '../../components/SettlementRequestModal';
import EarlySettlementRequestModal from '../../components/EarlySettlementRequestModal';
import SettlementPaymentModal from '../../components/SettlementPaymentModal';
import SettlementRefundCollectionModal from '../../components/SettlementRefundCollectionModal';
import SettlementStatusBadge from '../../components/SettlementStatusBadge';
import SettlementSummary from '../../components/SettlementSummary';
import SettlementRealTimeIndicator from '../../components/SettlementRealTimeIndicator';
import settlementService from '../../services/settlementService';
import agreementService from '../../services/agreementService';
import useRealTimeSettlementUpdates from '../../hooks/useRealTimeSettlementUpdates';
import { useSuccessPopup } from '../../hooks/useSuccessPopup';

const Settlements = () => {
  const [settlements, setSettlements] = useState([]);
  const [activeAgreements, setActiveAgreements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showRequestModal, setShowRequestModal] = useState(false);
  const [showEarlySettlementModal, setShowEarlySettlementModal] = useState(false);
  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const [showCollectionModal, setShowCollectionModal] = useState(false);
  const [selectedAgreement, setSelectedAgreement] = useState(null);
  const [selectedSettlement, setSelectedSettlement] = useState(null);
  const [enableRealTimeUpdates, setEnableRealTimeUpdates] = useState(true);
  const { showSuccess } = useSuccessPopup();

  // Real-time updates for tenant settlements
  const {
    settlements: realTimeSettlements,
    loading: realTimeLoading,
    error: realTimeError,
    lastUpdated,
    connectionStatus,
    refresh,
    isPolling
  } = useRealTimeSettlementUpdates({
    userType: 'tenant',
    enabled: enableRealTimeUpdates,
    pollingInterval: 3000, // 3 seconds
    onStatusChange: (change) => {
      // Show status-specific notifications
      if (change.newStatus === 'SETTLEMENT_APPROVED') {
        showSuccess(`Settlement ${change.settlementId.substring(0, 8)}... has been approved!`);
      } else if (change.newStatus === 'SETTLEMENT_DONE') {
        showSuccess(`Settlement ${change.settlementId.substring(0, 8)}... has been completed!`);
      } else if (change.newStatus === 'PENDING_TENANT_PAYMENT') {
        showSuccess(`Payment required for settlement ${change.settlementId.substring(0, 8)}...`);
      } else if (change.newStatus === 'PENDING_OWNER_PAYMENT') {
        showSuccess(`Refund pending for settlement ${change.settlementId.substring(0, 8)}...`);
      } else {
        showSuccess(`Settlement ${change.settlementId.substring(0, 8)}... status changed to ${change.newStatus}`);
      }
    },
    onError: (err, message) => {
      console.warn('Real-time updates error:', message);
    }
  });

  useEffect(() => {
    // Fetch initial data if real-time updates are disabled
    if (!enableRealTimeUpdates) {
      fetchData();
    } else {
      // Still need to fetch agreements as they're not part of real-time updates
      fetchActiveAgreements();
    }
  }, [enableRealTimeUpdates]);

  // Use real-time settlements if available, fallback to manual fetch
  useEffect(() => {
    if (enableRealTimeUpdates && realTimeSettlements.length >= 0) {
      setSettlements(realTimeSettlements);
      setLoading(false);
    } else if (!enableRealTimeUpdates || realTimeError) {
      // Fallback to manual fetching
      if (!settlements.length) {
        fetchData();
      }
    }
  }, [realTimeSettlements, enableRealTimeUpdates, realTimeError]);

  const fetchActiveAgreements = async () => {
    try {
      const agreementsData = await agreementService.getTenantAgreements();
      console.log('Agreements data:', agreementsData);
      
      // Filter active agreements that don't have pending settlements
      const activeAgreementsWithoutSettlement = agreementsData.filter(agreement => 
        agreement.status === 'ACTIVE' && 
        !settlements.some(settlement => 
          settlement.agreementId === agreement.id && 
          !['COMPLETED', 'CANCELLED', 'REJECTED'].includes(settlement.status)
        )
      );
      
      console.log('Active agreements without settlement:', activeAgreementsWithoutSettlement);
      setActiveAgreements(activeAgreementsWithoutSettlement);
    } catch (agreementError) {
      console.error('Error fetching agreements:', agreementError);
    }
  };

  const fetchData = async () => {
    try {
      console.log('Fetching settlement data...');
      
      // Try to fetch both, but handle failures gracefully
      let settlementsData = [];
      let agreementsData = [];
      
      try {
        settlementsData = await settlementService.getTenantSettlements();
        console.log('Settlements data:', settlementsData);
      } catch (settlementError) {
        console.error('Error fetching settlements:', settlementError);
        // Continue with empty settlements if this fails
      }
      
      try {
        agreementsData = await agreementService.getTenantAgreements();
        console.log('Agreements data:', agreementsData);
      } catch (agreementError) {
        console.error('Error fetching agreements:', agreementError);
        // Continue with empty agreements if this fails
      }
      
      setSettlements(settlementsData);
      
      // Filter active agreements that don't have pending settlements
      const activeAgreementsWithoutSettlement = agreementsData.filter(agreement => 
        agreement.status === 'ACTIVE' && 
        !settlementsData.some(settlement => 
          settlement.agreementId === agreement.id && 
          !['COMPLETED', 'CANCELLED', 'REJECTED'].includes(settlement.status)
        )
      );
      
      console.log('Active agreements without settlement:', activeAgreementsWithoutSettlement);
      setActiveAgreements(activeAgreementsWithoutSettlement);
    } catch (error) {
      console.error('Error fetching data:', error);
    } finally {
      setLoading(false);
    }
  };

  // Refresh function that works with both real-time and manual modes
  const handleRefresh = async () => {
    if (enableRealTimeUpdates) {
      await refresh();
      await fetchActiveAgreements(); // Agreements are not real-time, so fetch manually
    } else {
      await fetchData();
    }
  };

  const handleRequestSettlement = (agreement) => {
    setSelectedAgreement(agreement);
    setShowRequestModal(true);
  };

  const handleRequestEarlySettlement = (agreement) => {
    setSelectedAgreement(agreement);
    setShowEarlySettlementModal(true);
  };

  const handleMakePayment = (settlement) => {
    setSelectedSettlement(settlement);
    setShowPaymentModal(true);
  };

  const handleCollectPayment = (settlement) => {
    setSelectedSettlement(settlement);
    setShowCollectionModal(true);
  };

  const handleCompletePayment = async (settlementId) => {
    try {
      const paymentReference = prompt('Enter payment reference (transaction ID, UPI reference, etc.):');
      if (!paymentReference) return;

      await settlementService.completeSettlement(settlementId, paymentReference);
      showSuccess('Payment confirmed! Settlement completed.');
      handleRefresh();
    } catch (error) {
      console.error('Error completing payment:', error);
    }
  };

  const renderSettlementActions = (settlement) => {
    if (settlement.status === 'PENDING_TENANT_PAYMENT') {
      // Tenant owes money to owner
      return (
        <Button
          onClick={() => handleMakePayment(settlement)}
          className="w-full"
        >
          Make Payment
        </Button>
      );
    }

    if (settlement.status === 'PENDING_OWNER_PAYMENT') {
      // Owner owes money to tenant
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

    if (settlement.status === 'COMPLETED' && settlement.settledAt) {
      return (
        <div className="text-sm text-green-600 text-center py-2">
          ✓ Settlement completed on {new Date(settlement.settledAt).toLocaleDateString()}
        </div>
      );
    }

    // Enhanced settlement statuses
    if (settlement.status === 'SETTLEMENT_DONE') {
      return (
        <div className="text-sm text-green-600 text-center py-2">
          ✓ Settlement transaction completed
        </div>
      );
    }

    if (settlement.status === 'SETTLEMENT_TRANSACTION_CREATED') {
      // Check if tenant needs to pay based on settlement type
      if (settlement.settlementType === 'TENANT_PAYABLE' && settlement.finalSettlementAmount < 0) {
        return (
          <Button
            onClick={() => handleMakePayment(settlement)}
            className="w-full"
          >
            Make Payment
          </Button>
        );
      }
      
      if (settlement.settlementType === 'OWNER_PAYABLE' && settlement.finalSettlementAmount > 0) {
        return (
          <Button
            onClick={() => handleCollectPayment(settlement)}
            variant="success"
            className="w-full"
          >
            Collect Refund
          </Button>
        );
      }
    }

    if (settlement.status === 'SETTLEMENT_APPROVED') {
      return (
        <div className="text-sm text-blue-600 text-center py-2">
          Settlement approved - waiting for final processing
        </div>
      );
    }

    return null;
  };

  // Determine loading state from both sources
  const isLoading = enableRealTimeUpdates 
    ? (realTimeLoading && settlements.length === 0) || (loading && activeAgreements.length === 0)
    : loading;

  if (isLoading) {
    return <LoadingScreen />;
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold">Agreement Settlements</h1>
          <p className="text-gray-600">Request and track agreement settlements</p>
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
              userType="tenant"
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
                // Additional actions for specific status changes
                if (change.newStatus === 'SETTLEMENT_APPROVED') {
                  fetchActiveAgreements(); // Refresh agreements as one might be completed
                }
              }}
              className="text-sm"
            />
          )}
        </div>
      </div>

      {/* Active Agreements - Settlement Request Section */}
      {activeAgreements.length > 0 && (
        <div className="space-y-4">
          <h2 className="text-lg font-semibold">Request Settlement</h2>
          <div className="grid gap-4">
            {activeAgreements.map((agreement) => (
              <Card key={agreement.id} className="p-4">
                <div className="flex justify-between items-center">
                  <div>
                    <h3 className="font-medium">
                      Room: {agreement.roomNumber || 'N/A'}
                    </h3>
                    <p className="text-sm text-gray-600">
                      Security Deposit: ₹{agreement.deposit?.toLocaleString()}
                    </p>
                    <p className="text-sm text-gray-500">
                      Active since: {new Date(agreement.startDate).toLocaleDateString()}
                    </p>
                  </div>
                  <div className="flex gap-2">
                    <Button
                      onClick={() => handleRequestSettlement(agreement)}
                      size="sm"
                      variant="secondary"
                    >
                      Normal Settlement
                    </Button>
                    <Button
                      onClick={() => handleRequestEarlySettlement(agreement)}
                      size="sm"
                      variant="danger"
                    >
                      Early Settlement
                    </Button>
                  </div>
                </div>
              </Card>
            ))}
          </div>
        </div>
      )}

      {/* Settlement Requests */}
      <div className="space-y-4">
        <h2 className="text-lg font-semibold">Settlement History</h2>
        
        {settlements.length === 0 ? (
          <EmptyState
            title="No Settlement Requests"
            description="You haven't requested any settlements yet. You can request settlement for your active agreements above."
            icon="clipboard"
          />
        ) : (
          <div className="space-y-4">
            {settlements.map((settlement) => (
              <div key={settlement.settlementId} className="space-y-4">
                {/* Status-specific alerts */}
                {(settlement.status === 'PENDING_TENANT_PAYMENT' || 
                  (settlement.status === 'SETTLEMENT_TRANSACTION_CREATED' && settlement.settlementType === 'TENANT_PAYABLE')) && (
                  <Alert type="warning">
                    <strong>Payment Required:</strong> You need to pay ₹{Math.abs(settlement.finalSettlementAmount)?.toLocaleString()} to complete the settlement.
                  </Alert>
                )}

                {(settlement.status === 'PENDING_OWNER_PAYMENT' || 
                  (settlement.status === 'SETTLEMENT_TRANSACTION_CREATED' && settlement.settlementType === 'OWNER_PAYABLE')) && (
                  <Alert type="info">
                    <strong>Refund Pending:</strong> Your owner will refund ₹{Math.abs(settlement.finalSettlementAmount)?.toLocaleString()} to you.
                  </Alert>
                )}

                {settlement.status === 'SETTLEMENT_APPROVED' && (
                  <Alert type="success">
                    <strong>Settlement Approved:</strong> Your settlement has been approved and is being processed.
                  </Alert>
                )}

                {settlement.status === 'SETTLEMENT_DONE' && (
                  <Alert type="success">
                    <strong>Settlement Completed:</strong> Your settlement transaction has been completed successfully.
                  </Alert>
                )}

                {settlement.earlySettlementRequested && settlement.status === 'SETTLEMENT_REQUESTED' && (
                  <Alert type="warning">
                    <strong>Early Settlement Requested:</strong> You have requested an early settlement. The owner will review your request.
                  </Alert>
                )}

                {settlement.status === 'REJECTED' && settlement.ownerNotes && (
                  <Alert type="error">
                    <strong>Settlement Rejected:</strong> {settlement.ownerNotes}
                  </Alert>
                )}

                <div className="relative">
                  <div className="absolute top-4 right-4 z-10">
                    <SettlementStatusBadge status={settlement.status} />
                  </div>
                  <SettlementSummary
                    settlement={settlement}
                    showActions={true}
                    showTransactionDetails={true}
                    enableRealTimeUpdates={enableRealTimeUpdates}
                    userType="tenant"
                    onSettlementUpdate={(change) => {
                      if (change.newStatus === 'SETTLEMENT_APPROVED') {
                        fetchActiveAgreements(); // Refresh agreements as one might be completed
                      }
                    }}
                    onAction={renderSettlementActions}
                  />
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Settlement Collection Modal */}
      {showCollectionModal && selectedSettlement && (
        <SettlementRefundCollectionModal
          settlement={selectedSettlement}
          onSuccess={() => {
            handleRefresh();
            setShowCollectionModal(false);
            setSelectedSettlement(null);
          }}
          onClose={() => {
            setShowCollectionModal(false);
            setSelectedSettlement(null);
          }}
        />
      )}

      {/* Settlement Payment Modal */}
      {showPaymentModal && selectedSettlement && (
        <SettlementPaymentModal
          settlement={selectedSettlement}
          onSuccess={() => {
            handleRefresh();
            setShowPaymentModal(false);
            setSelectedSettlement(null);
          }}
          onClose={() => {
            setShowPaymentModal(false);
            setSelectedSettlement(null);
          }}
        />
      )}

      {/* Settlement Request Modal */}
      {showRequestModal && (
        <SettlementRequestModal
          isOpen={showRequestModal}
          onClose={() => {
            setShowRequestModal(false);
            setSelectedAgreement(null);
          }}
          agreement={selectedAgreement}
          onSuccess={handleRefresh}
        />
      )}

      {/* Early Settlement Request Modal */}
      {showEarlySettlementModal && (
        <EarlySettlementRequestModal
          isOpen={showEarlySettlementModal}
          onClose={() => {
            setShowEarlySettlementModal(false);
            setSelectedAgreement(null);
          }}
          agreement={selectedAgreement}
          onSuccess={handleRefresh}
        />
      )}
    </div>
  );
};

export default Settlements;