import { useState, useEffect } from 'react';
import { Button } from '../../components/ui/Button';
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
  const [selectedSettlement, setSelectedSettlement] = useState(null);
  const [showCalculationModal, setShowCalculationModal] = useState(false);
  const [showCollectionModal, setShowCollectionModal] = useState(false);
  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const { showSuccess } = useSuccessPopup();

  useEffect(() => {
    fetchSettlements();
  }, []);

  const fetchSettlements = async () => {
    try {
      const data = await settlementService.getOwnerSettlements();
      setSettlements(data);
    } catch (error) {
      console.error('Error fetching settlements:', error);
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

      {settlements.length === 0 ? (
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
    </div>
  );
};

export default Settlements;