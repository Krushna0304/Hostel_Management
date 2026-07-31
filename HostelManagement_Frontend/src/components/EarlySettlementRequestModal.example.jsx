import { useState } from 'react';
import { Button } from './ui/Button';
import EarlySettlementRequestModal from './EarlySettlementRequestModal';

/**
 * Example usage of EarlySettlementRequestModal component
 * 
 * This example demonstrates how to integrate the early settlement request
 * functionality into your tenant-facing components.
 */
const EarlySettlementExample = () => {
  const [showModal, setShowModal] = useState(false);

  // Example agreement data - replace with actual agreement from your state
  const sampleAgreement = {
    id: 'agreement-123',
    roomNumber: 'A-101',
    endDate: '2024-12-31',
    status: 'ACTIVE',
    deposit: 5000
  };

  const handleSuccess = () => {
    // Refresh your settlements data here
    console.log('Early settlement request submitted successfully');
    // You might want to:
    // - Refresh the settlements list
    // - Update the UI state
    // - Navigate to settlements page
  };

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-4">Early Settlement Example</h1>
      
      <div className="mb-6 p-4 bg-gray-50 rounded-lg">
        <h3 className="font-medium mb-2">Current Agreement</h3>
        <p><strong>Room:</strong> {sampleAgreement.roomNumber}</p>
        <p><strong>End Date:</strong> {new Date(sampleAgreement.endDate).toLocaleDateString()}</p>
        <p><strong>Status:</strong> {sampleAgreement.status}</p>
      </div>

      <Button
        onClick={() => setShowModal(true)}
        variant="danger"
        className="mb-4"
      >
        Request Early Settlement
      </Button>

      <div className="text-sm text-gray-600 space-y-2">
        <p><strong>When to use Early Settlement:</strong></p>
        <ul className="list-disc ml-5 space-y-1">
          <li>You need to terminate your agreement before the end date</li>
          <li>Job relocation or personal emergencies</li>
          <li>You understand there may be penalties involved</li>
        </ul>
        
        <p className="mt-4"><strong>API Requirements:</strong></p>
        <ul className="list-disc ml-5 space-y-1">
          <li>Endpoint: POST /api/v1/settlements/early-settlement</li>
          <li>Authentication required (Bearer token)</li>
          <li>Request body: agreementId, requestedEndDate, reason, tenantNotes</li>
        </ul>
      </div>

      <EarlySettlementRequestModal
        isOpen={showModal}
        onClose={() => setShowModal(false)}
        agreement={sampleAgreement}
        onSuccess={handleSuccess}
      />
    </div>
  );
};

export default EarlySettlementExample;