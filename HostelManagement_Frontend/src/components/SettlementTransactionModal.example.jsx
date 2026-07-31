/**
 * Example Usage of SettlementTransactionModal
 * 
 * This file demonstrates how to integrate and use the SettlementTransactionModal
 * component in a parent component or page.
 */

import { useState } from 'react';
import SettlementTransactionModal from './SettlementTransactionModal';
import { Button } from './ui/Button';

const ExampleSettlementTransactionUsage = () => {
  const [showModal, setShowModal] = useState(false);
  const [selectedAgreement, setSelectedAgreement] = useState(null);

  // Example agreement data structure
  const mockAgreement = {
    id: 'agreement-123',
    tenantName: 'John Doe',
    roomNumber: 'A-101',
    status: 'ACTIVE',
    deposit: 5000,
    // Add other relevant agreement fields as needed
  };

  const handleCreateTransaction = (agreement) => {
    setSelectedAgreement(agreement);
    setShowModal(true);
  };

  const handleModalClose = () => {
    setShowModal(false);
    setSelectedAgreement(null);
  };

  const handleTransactionSuccess = () => {
    // Called when transaction is successfully created
    console.log('Settlement transaction created successfully');
    
    // Refresh your data, update state, show notifications, etc.
    // For example:
    // - Refresh settlements list
    // - Update agreement status
    // - Show success message
    // - Navigate to settlements page
    
    // The modal will close automatically after success
  };

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-6">Settlement Transaction Management</h1>
      
      {/* Example: Button to trigger modal */}
      <div className="space-y-4">
        <div className="p-4 border rounded-lg">
          <h3 className="font-semibold">Agreement: {mockAgreement.id}</h3>
          <p className="text-sm text-gray-600">
            Tenant: {mockAgreement.tenantName} | Room: {mockAgreement.roomNumber}
          </p>
          <Button
            label="Create Settlement Transaction"
            onClick={() => handleCreateTransaction(mockAgreement)}
            className="mt-2"
          />
        </div>
      </div>

      {/* Settlement Transaction Modal */}
      <SettlementTransactionModal
        isOpen={showModal}
        onClose={handleModalClose}
        agreement={selectedAgreement}
        onSuccess={handleTransactionSuccess}
      />
    </div>
  );
};

// Integration in Owner Dashboard or Settlements Page
const OwnerSettlementsPageExample = () => {
  const [showTransactionModal, setShowTransactionModal] = useState(false);
  const [selectedAgreement, setSelectedAgreement] = useState(null);
  const [agreements, setAgreements] = useState([]);

  const createSettlementTransaction = (agreement) => {
    setSelectedAgreement(agreement);
    setShowTransactionModal(true);
  };

  const refreshAgreements = async () => {
    // Refresh agreements list from API
    try {
      // const updatedAgreements = await agreementService.getOwnerAgreements();
      // setAgreements(updatedAgreements);
      console.log('Refreshing agreements after transaction creation');
    } catch (error) {
      console.error('Failed to refresh agreements:', error);
    }
  };

  return (
    <div>
      {/* Your existing settlements/agreements UI */}
      <div className="agreements-list">
        {agreements.map(agreement => (
          <div key={agreement.id} className="agreement-card">
            {/* Agreement details */}
            <Button
              label="Create Transaction"
              onClick={() => createSettlementTransaction(agreement)}
            />
          </div>
        ))}
      </div>

      {/* Modal Integration */}
      <SettlementTransactionModal
        isOpen={showTransactionModal}
        onClose={() => {
          setShowTransactionModal(false);
          setSelectedAgreement(null);
        }}
        agreement={selectedAgreement}
        onSuccess={() => {
          refreshAgreements();
          setShowTransactionModal(false);
          setSelectedAgreement(null);
        }}
      />
    </div>
  );
};

// Props interface for TypeScript (if using TypeScript)
const propTypes = {
  // Required props
  isOpen: 'boolean', // Controls modal visibility
  onClose: 'function', // Called when modal should be closed
  agreement: 'object', // Agreement object with required fields
  onSuccess: 'function', // Called when transaction is created successfully

  // Agreement object structure
  // {
  //   id: string, // Agreement ID (required)
  //   tenantName: string, // Tenant display name
  //   roomNumber: string, // Room number/identifier
  //   status: string, // Agreement status
  //   deposit: number, // Security deposit amount
  //   // ... other agreement fields
  // }
};

// CSS classes used (for custom styling if needed)
const stylingClasses = {
  modal: 'fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4',
  modalContent: 'relative w-full max-w-4xl max-h-[95vh] overflow-y-auto rounded-3xl bg-white shadow-2xl',
  header: 'sticky top-0 z-10 flex items-start justify-between bg-white px-6 py-4 border-b border-slate-100 rounded-t-3xl',
  footer: 'sticky bottom-0 bg-white border-t border-slate-100 px-6 py-4 rounded-b-3xl',
  section: 'rounded-2xl border border-slate-200',
  sectionHeader: 'px-4 py-3 bg-slate-50 rounded-t-2xl border-b border-slate-200',
  sectionContent: 'px-4 py-4 space-y-3'
};

export default ExampleSettlementTransactionUsage;
export { OwnerSettlementsPageExample, propTypes, stylingClasses };