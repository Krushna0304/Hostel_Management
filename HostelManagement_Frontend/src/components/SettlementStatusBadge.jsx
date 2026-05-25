import { Badge } from './ui/Badge';

const SettlementStatusBadge = ({ status }) => {
  const getStatusConfig = (status) => {
    const statusConfig = {
      PENDING_OWNER_REVIEW: { 
        variant: 'warning', 
        label: 'Pending Review',
        description: 'Waiting for owner to review and approve'
      },
      CALCULATION_IN_PROGRESS: { 
        variant: 'secondary', 
        label: 'Calculating',
        description: 'Settlement amount being calculated'
      },
      PENDING_TENANT_PAYMENT: { 
        variant: 'destructive', 
        label: 'Payment Required',
        description: 'Tenant needs to make payment'
      },
      PENDING_OWNER_PAYMENT: { 
        variant: 'warning', 
        label: 'Refund Pending',
        description: 'Owner needs to process refund'
      },
      PAYMENT_IN_PROGRESS: { 
        variant: 'secondary', 
        label: 'Processing',
        description: 'Payment is being processed'
      },
      COMPLETED: { 
        variant: 'success', 
        label: 'Completed',
        description: 'Settlement successfully completed'
      },
      CANCELLED: { 
        variant: 'secondary', 
        label: 'Cancelled',
        description: 'Settlement was cancelled'
      },
      REJECTED: { 
        variant: 'destructive', 
        label: 'Rejected',
        description: 'Settlement request was rejected'
      }
    };

    return statusConfig[status] || { 
      variant: 'secondary', 
      label: status,
      description: 'Unknown status'
    };
  };

  const config = getStatusConfig(status);
  
  return (
    <Badge 
      variant={config.variant}
      title={config.description}
    >
      {config.label}
    </Badge>
  );
};

export default SettlementStatusBadge;