import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Button from '../../components/Button';
import { floorService } from '../../services/hostelService';

const CreateFloorPage = () => {
  const { hostelId } = useParams();
  const navigate = useNavigate();
  const [floorNumber, setFloorNumber] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!floorNumber || isNaN(floorNumber) || Number(floorNumber) <= 0) {
      setError('Please enter a valid floor number.');
      return;
    }
    setLoading(true);
    try {
      await floorService.createFloor(hostelId, { floorNumber: Number(floorNumber) });
      navigate(`/owner/hostels/${hostelId}/floors`);
    } catch (err) {
      const errorData = err.response?.data
      
      // Check if error is field-specific validation errors (object with field names as keys)
      if (errorData && typeof errorData === 'object' && !errorData.message) {
        // It's field-specific errors - format them for display
        const errorMessages = Object.values(errorData).join(', ')
        setError(errorMessages || 'Failed to add floor.')
      } else {
        // It's a general error message
        setError(errorData?.message || 'Failed to add floor.')
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center">
      <div className="bg-white rounded-lg shadow p-6 w-full max-w-md">
        <div className="flex gap-2 items-center mb-6">
          <h1 className="text-2xl font-bold text-gray-800">Add Floor</h1>
        </div>
        <form onSubmit={handleSubmit}>
          {error && <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded-lg text-sm">{error}</div>}
          <div className="mb-6">
            <label className="block text-gray-700 font-medium mb-2">Floor Number</label>
            <input
              type="number"
              className="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={floorNumber}
              onChange={e => setFloorNumber(e.target.value)}
              min={1}
              required
            />
          </div>
          <Button type="submit" label={loading ? 'Adding...' : 'Add Floor'} fullWidth disabled={loading} />
          <Button type="button" label="Cancel" variant="secondary" fullWidth onClick={() => navigate(`/owner/hostels/${hostelId}/floors`)} />
        </form>
      </div>
    </div>
  );
};

export default CreateFloorPage;
