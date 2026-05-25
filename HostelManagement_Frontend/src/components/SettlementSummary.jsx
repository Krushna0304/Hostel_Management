import { Card } from './ui/Card';
import { Badge } from './ui/Badge';

const SettlementSummary = ({ settlement, showActions = false, onAction }) => {
  const formatCurrency = (amount) => {
    return `₹${(amount || 0).toLocaleString()}`;
  };

  const getSettlementTypeBadge = (type, amount) => {
    if (!type || !amount) return null;
    
    return type === 'OWNER_PAYABLE' ? (
      <Badge variant="success">
        Owner Pays: {formatCurrency(amount)}
      </Badge>
    ) : (
      <Badge variant="warning">
        Tenant Pays: {formatCurrency(amount)}
      </Badge>
    );
  };

  return (
    <Card className="p-4">
      <div className="space-y-4">
        {/* Header */}
        <div className="flex justify-between items-start gap-4">
          <div className="flex-1 min-w-0">
            <h3 className="font-semibold text-lg">
              {settlement.tenantName || 'Settlement Request'}
            </h3>
            <p className="text-sm text-gray-600">
              Room: {settlement.roomNumber || 'N/A'}
            </p>
            <p className="text-xs text-gray-500">
              Requested: {new Date(settlement.createdAt).toLocaleDateString()}
            </p>
          </div>
          <div className="flex-shrink-0 mt-8">
            {settlement.settlementType && settlement.finalSettlementAmount && 
              getSettlementTypeBadge(settlement.settlementType, settlement.finalSettlementAmount)
            }
          </div>
        </div>

        {/* Financial Breakdown */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm">
          <div className="text-center p-2 bg-green-50 rounded">
            <p className="text-xs text-green-600 font-medium">Security Deposit</p>
            <p className="font-semibold text-green-700">
              {formatCurrency(settlement.securityDeposit)}
            </p>
          </div>
          <div className="text-center p-2 bg-red-50 rounded">
            <p className="text-xs text-red-600 font-medium">Outstanding Rent</p>
            <p className="font-semibold text-red-700">
              {formatCurrency(settlement.outstandingRent)}
            </p>
          </div>
          <div className="text-center p-2 bg-red-50 rounded">
            <p className="text-xs text-red-600 font-medium">Other Charges</p>
            <p className="font-semibold text-red-700">
              {formatCurrency(settlement.outstandingCharges)}
            </p>
          </div>
          <div className="text-center p-2 bg-orange-50 rounded">
            <p className="text-xs text-orange-600 font-medium">Deductions</p>
            <p className="font-semibold text-orange-700">
              {formatCurrency(
                (settlement.damageCharges || 0) + 
                (settlement.cleaningCharges || 0) + 
                (settlement.otherDeductions || 0)
              )}
            </p>
          </div>
        </div>

        {/* Final Amount */}
        {settlement.finalSettlementAmount !== undefined && (
          <div className="p-3 bg-blue-50 rounded-lg border border-blue-200">
            <div className="text-center">
              <p className="text-sm text-blue-600 font-medium">Final Settlement Amount</p>
              <p className="text-xl font-bold text-blue-800">
                {formatCurrency(Math.abs(settlement.finalSettlementAmount))}
              </p>
              <p className="text-xs text-blue-600">
                {settlement.finalSettlementAmount >= 0
                  ? 'Owner will refund this amount' 
                  : 'Tenant needs to pay this amount'
                }
              </p>
              {settlement.status === 'COMPLETED' && settlement.settledAt && (
                <div className="mt-2 pt-2 border-t border-blue-200">
                  <p className="text-xs text-green-600 font-medium">
                    ✓ Completed on {new Date(settlement.settledAt).toLocaleDateString()}
                  </p>
                  {settlement.paymentReference && (
                    <p className="text-xs text-gray-500 mt-1">
                      Ref: {settlement.paymentReference}
                    </p>
                  )}
                </div>
              )}
            </div>
          </div>
        )}

        {/* Notes */}
        {settlement.tenantNotes && (
          <div className="p-3 bg-gray-50 rounded-lg">
            <p className="text-xs font-medium text-gray-700 mb-1">Tenant Notes:</p>
            <p className="text-sm text-gray-600">{settlement.tenantNotes}</p>
          </div>
        )}

        {settlement.ownerNotes && (
          <div className="p-3 bg-gray-50 rounded-lg">
            <p className="text-xs font-medium text-gray-700 mb-1">Owner Notes:</p>
            <p className="text-sm text-gray-600">{settlement.ownerNotes}</p>
          </div>
        )}

        {settlement.damageDescription && (
          <div className="p-3 bg-red-50 rounded-lg">
            <p className="text-xs font-medium text-red-700 mb-1">Damage Description:</p>
            <p className="text-sm text-red-600">{settlement.damageDescription}</p>
          </div>
        )}

        {/* Actions */}
        {showActions && onAction && (
          <div className="pt-2 border-t">
            {onAction(settlement)}
          </div>
        )}
      </div>
    </Card>
  );
};

export default SettlementSummary;