import { useState, useEffect } from 'react';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import EmptyState from '../../components/ui/EmptyState';
import LoadingScreen from '../../components/ui/LoadingScreen';
import { Alert } from '../../components/ui/Alert';
import SettlementRequestModal from '../../components/SettlementRequestModal';
import SettlementPaymentModal from '../../components/SettlementPaymentModal';
import SettlementRefundCollectionModal from '../../components/SettlementRefundCollectionModal';
import SettlementStatusBadge from '../../components/SettlementStatusBadge';
import SettlementSummary from '../../components/SettlementSummary';
import settlementService from '../../services/settlementService';
import agreementService from '../../services/agreementService';
import { useSuccessPopup } from '../../hooks/useSuccessPopup';

const Settlements = () => {
  const [settlements, setSettlements] = useState([]);
  const [activeAgreements, setActiveAgreements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showRequestModal, setShowRequestModal] = useState(false);
  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const [showCollectionModal, setShowCollectionModal] = useState(false);
  const [selectedAgreement, setSelectedAgreement] = useState(null);
  const [selectedSettlement, setSelectedSettlement] = useState(null);
  const { showSuccess } = useSuccessPopup();

  useEffect(() => {
    fetchData();
  }, []);

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

  const handleRequestSettlement = (agreement) => {
    setSelectedAgreement(agreement);
    setShowRequestModal(true);
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
      fetchData();
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

    return null;
  };

  if (loading) {
    return <LoadingScreen />;
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold">Agreement Settlements</h1>
          <p className="text-gray-600">Request and track agreement settlements</p>
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
                  <Button
                    onClick={() => handleRequestSettlement(agreement)}
                    size="sm"
                  >
                    Request Settlement
                  </Button>
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
                {settlement.status === 'PENDING_TENANT_PAYMENT' && (
                  <Alert type="warning">
                    <strong>Payment Required:</strong> You need to pay ₹{Math.abs(settlement.finalSettlementAmount)?.toLocaleString()} to complete the settlement.
                  </Alert>
                )}

                {settlement.status === 'PENDING_OWNER_PAYMENT' && (
                  <Alert type="info">
                    <strong>Refund Pending:</strong> Your owner will refund ₹{Math.abs(settlement.finalSettlementAmount)?.toLocaleString()} to you.
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
            fetchData();
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
            fetchData();
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
          onSuccess={fetchData}
        />
      )}
    </div>
  );
};

export default Settlements;