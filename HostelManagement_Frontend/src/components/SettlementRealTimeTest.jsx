import { useState } from 'react';
import { Card } from './ui/Card';
import { Button } from './ui/Button';
import SettlementRealTimeIndicator from './SettlementRealTimeIndicator';
import useRealTimeSettlementUpdates from '../hooks/useRealTimeSettlementUpdates';

/**
 * Test component for demonstrating real-time settlement updates
 * This component can be used for testing and development
 */
const SettlementRealTimeTest = () => {
  const [userType, setUserType] = useState('tenant');
  const [enabled, setEnabled] = useState(true);
  const [logs, setLogs] = useState([]);

  const addLog = (message, type = 'info') => {
    const log = {
      id: Date.now(),
      timestamp: new Date().toLocaleTimeString(),
      message,
      type
    };
    setLogs(prev => [log, ...prev.slice(0, 19)]); // Keep last 20 logs
  };

  const {
    settlements,
    loading,
    error,
    lastUpdated,
    isPolling,
    refresh,
    startPolling,
    stopPolling
  } = useRealTimeSettlementUpdates({
    pollingInterval: 2000, // 2 seconds for testing
    enabled,
    userType,
    onStatusChange: (change) => {
      addLog(
        `Status changed: ${change.settlementId.substring(0, 8)}... 
         from ${change.oldStatus} to ${change.newStatus}`,
        'success'
      );
    },
    onError: (err, message) => {
      addLog(`Error: ${message}`, 'error');
    }
  });

  const togglePolling = () => {
    if (isPolling) {
      stopPolling();
      addLog('Polling stopped', 'warning');
    } else {
      startPolling();
      addLog('Polling started', 'info');
    }
  };

  const handleRefresh = () => {
    refresh();
    addLog('Manual refresh triggered', 'info');
  };

  const clearLogs = () => {
    setLogs([]);
  };

  const getLogTypeStyle = (type) => {
    const styles = {
      info: 'text-blue-600 bg-blue-50 border-blue-200',
      success: 'text-green-600 bg-green-50 border-green-200', 
      warning: 'text-yellow-600 bg-yellow-50 border-yellow-200',
      error: 'text-red-600 bg-red-50 border-red-200'
    };
    return styles[type] || styles.info;
  };

  return (
    <div className="max-w-6xl mx-auto p-6 space-y-6">
      {/* Header */}
      <div className="text-center">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          Real-Time Settlement Updates Test
        </h1>
        <p className="text-gray-600">
          Test component for validating real-time settlement functionality
        </p>
      </div>

      {/* Controls */}
      <Card className="p-6">
        <h2 className="text-xl font-semibold mb-4">Controls</h2>
        
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* User Type Toggle */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              User Type
            </label>
            <select
              value={userType}
              onChange={(e) => {
                setUserType(e.target.value);
                addLog(`Switched to ${e.target.value} mode`, 'info');
              }}
              className="w-full p-2 border border-gray-300 rounded-lg"
            >
              <option value="tenant">Tenant</option>
              <option value="owner">Owner</option>
            </select>
          </div>

          {/* Enable/Disable Toggle */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Real-Time Updates
            </label>
            <button
              onClick={() => {
                setEnabled(!enabled);
                addLog(`Real-time updates ${!enabled ? 'enabled' : 'disabled'}`, 'info');
              }}
              className={`w-full p-2 rounded-lg font-medium ${
                enabled 
                  ? 'bg-green-100 text-green-700 border border-green-200' 
                  : 'bg-gray-100 text-gray-700 border border-gray-200'
              }`}
            >
              {enabled ? 'Enabled' : 'Disabled'}
            </button>
          </div>

          {/* Polling Controls */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Polling Control
            </label>
            <Button
              label={isPolling ? 'Stop Polling' : 'Start Polling'}
              onClick={togglePolling}
              variant={isPolling ? 'destructive' : 'primary'}
              fullWidth
            />
          </div>

          {/* Manual Refresh */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Manual Actions
            </label>
            <Button
              label="Refresh Now"
              onClick={handleRefresh}
              loading={loading}
              variant="secondary"
              fullWidth
            />
          </div>
        </div>
      </Card>

      {/* Status Display */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Real-Time Indicator */}
        <Card className="p-6">
          <h2 className="text-xl font-semibold mb-4">Real-Time Status</h2>
          
          <div className="space-y-4">
            <SettlementRealTimeIndicator
              userType={userType}
              onStatusChange={(change) => {
                addLog(
                  `Indicator detected change: ${change.settlementId.substring(0, 8)}...`,
                  'success'
                );
              }}
              className="p-3 bg-gray-50 rounded-lg"
            />

            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <span className="font-medium text-gray-600">Status:</span>
                <div className="mt-1">
                  {error ? (
                    <span className="text-red-600">Error</span>
                  ) : isPolling ? (
                    <span className="text-green-600">Polling Active</span>
                  ) : (
                    <span className="text-gray-600">Polling Stopped</span>
                  )}
                </div>
              </div>
              
              <div>
                <span className="font-medium text-gray-600">Last Update:</span>
                <div className="mt-1 text-gray-800">
                  {lastUpdated ? lastUpdated.toLocaleTimeString() : 'Never'}
                </div>
              </div>

              <div>
                <span className="font-medium text-gray-600">Settlements:</span>
                <div className="mt-1 text-gray-800">
                  {settlements.length} found
                </div>
              </div>

              <div>
                <span className="font-medium text-gray-600">User Type:</span>
                <div className="mt-1 text-gray-800 capitalize">
                  {userType}
                </div>
              </div>
            </div>

            {error && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-lg">
                <p className="text-red-700 text-sm font-medium">Error:</p>
                <p className="text-red-600 text-sm">{error}</p>
              </div>
            )}
          </div>
        </Card>

        {/* Settlements List */}
        <Card className="p-6">
          <h2 className="text-xl font-semibold mb-4">Settlements ({settlements.length})</h2>
          
          <div className="space-y-3 max-h-64 overflow-y-auto">
            {loading && settlements.length === 0 ? (
              <div className="text-center py-4">
                <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600 mx-auto"></div>
                <p className="text-sm text-gray-600 mt-2">Loading settlements...</p>
              </div>
            ) : settlements.length === 0 ? (
              <div className="text-center py-4 text-gray-500">
                <p className="text-sm">No settlements found</p>
                <p className="text-xs mt-1">
                  {userType === 'tenant' ? 'No settlement requests' : 'No settlements to review'}
                </p>
              </div>
            ) : (
              settlements.map(settlement => (
                <div
                  key={settlement.settlementId}
                  className="p-3 bg-gray-50 rounded-lg border"
                >
                  <div className="flex justify-between items-start">
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-sm truncate">
                        {settlement.tenantName || 'Unknown Tenant'}
                      </p>
                      <p className="text-xs text-gray-600">
                        ID: {settlement.settlementId.substring(0, 8)}...
                      </p>
                    </div>
                    <div className="flex-shrink-0 ml-2">
                      <span className={`
                        inline-block px-2 py-1 text-xs rounded-full
                        ${settlement.status === 'COMPLETED' 
                          ? 'bg-green-100 text-green-700'
                          : settlement.status === 'PENDING_OWNER_REVIEW'
                          ? 'bg-yellow-100 text-yellow-700'  
                          : 'bg-blue-100 text-blue-700'
                        }
                      `}>
                        {settlement.status}
                      </span>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </Card>
      </div>

      {/* Activity Log */}
      <Card className="p-6">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-semibold">Activity Log ({logs.length})</h2>
          <Button
            label="Clear Log"
            onClick={clearLogs}
            variant="secondary"
            size="sm"
          />
        </div>
        
        <div className="space-y-2 max-h-64 overflow-y-auto">
          {logs.length === 0 ? (
            <div className="text-center py-4 text-gray-500">
              <p className="text-sm">No activity yet</p>
              <p className="text-xs mt-1">Events will appear here as they happen</p>
            </div>
          ) : (
            logs.map(log => (
              <div
                key={log.id}
                className={`p-2 rounded-lg border text-sm ${getLogTypeStyle(log.type)}`}
              >
                <div className="flex justify-between items-start">
                  <span className="flex-1">{log.message}</span>
                  <span className="text-xs opacity-75 ml-2">{log.timestamp}</span>
                </div>
              </div>
            ))
          )}
        </div>
      </Card>

      {/* Instructions */}
      <Card className="p-6 bg-blue-50 border-blue-200">
        <h2 className="text-xl font-semibold text-blue-900 mb-3">
          Testing Instructions
        </h2>
        <div className="text-blue-800 text-sm space-y-2">
          <p>• <strong>Switch User Types:</strong> Toggle between tenant and owner to see different settlements</p>
          <p>• <strong>Monitor Status Changes:</strong> Changes will appear in the activity log automatically</p>
          <p>• <strong>Test Polling:</strong> Stop/start polling to see connection status changes</p>
          <p>• <strong>Manual Refresh:</strong> Use the refresh button to trigger immediate data fetch</p>
          <p>• <strong>Watch Indicators:</strong> The real-time indicator shows connection status and last update time</p>
        </div>
      </Card>
    </div>
  );
};

export default SettlementRealTimeTest;