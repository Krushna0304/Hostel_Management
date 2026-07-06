import { useState, useEffect } from 'react';
import { Alert, Button } from '../../components/ui';
import EmptyState from '../../components/ui/EmptyState';
import LoadingScreen from '../../components/ui/LoadingScreen';
import SettlementCalculationModal from '../../components/SettlementCalculationModal';
import SettlementCollectionModal from '../../components/SettlementCollectionModal';
import SettlementPaymentModal from '../../components/SettlementPaymentModal';
import SettlementStatusBadge from '../../components/SettlementStatusBadge';
import SettlementSummary from '../../components/SettlementSummary';
import settlementService from '../../services/settlementService';
import { useSuccessPopup } from '../../hooks/useSuccessPopup';

const Settlements = () => {
  const [settlements, setSettlements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedSettlement, setSelectedSettlement] = useState(null);
  const [showCalculationModal, setShowCalculationModal] = useState(false);
  const [showCollectionModal, setShowCollectionModal] = useState(false);
  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const [confirmLeftSettlement, setConfirmLeftSettlement] = useState(null);
  const [confirmingLeft, setConfirmingLeft] = useState(false);
  const { showSuccess } = useSuccessPopup();

  useEffect(() => {
    fetchSettlements();
  }, []);

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

  const handleViewSettlement = (settlement) => {
    setSelectedSettlement(settlement);
    setShowCalculationModal(true);
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
      fetchSettlements();
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
      fetchSettlements();
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

  if (loading) {
    return <LoadingScreen />;
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold">Settlement Requests</h1>
          <p className="text-gray-600">Manage agreement settlement requests from tenants</p>
        </div>
      </div>

      {error ? (
        <Alert tone="error">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <span>{error}</span>
            <Button label="Retry" variant="secondary" onClick={fetchSettlements} />
          </div>
        </Alert>
      ) : null}

      {!error && settlements.length === 0 ? (
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
          onSuccess={fetchSettlements}
        />
      )}

      {showCollectionModal && (
        <SettlementCollectionModal
          settlement={selectedSettlement}
          onClose={() => {
            setShowCollectionModal(false);
            setSelectedSettlement(null);
          }}
          onSuccess={fetchSettlements}
        />
      )}

      {showPaymentModal && (
        <SettlementPaymentModal
          settlement={selectedSettlement}
          onClose={() => {
            setShowPaymentModal(false);
            setSelectedSettlement(null);
          }}
          onSuccess={fetchSettlements}
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