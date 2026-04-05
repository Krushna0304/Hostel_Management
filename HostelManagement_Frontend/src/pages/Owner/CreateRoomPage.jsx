import { useState } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import Button from '../../components/Button';
import { roomService } from '../../services/hostelService';

const CreateRoomPage = () => {
  const { hostelId: paramHostelId, floorId: paramFloorId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const hostelId = location.state?.hostelId || paramHostelId;
  const floorId = location.state?.floorId || paramFloorId;
  const [form, setForm] = useState({
    roomNumber: '',
    roomDetails: '',
    totalBeds: '',
    availableBeds: '',
    isActive: true,
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = e => {
    const { name, value, type, checked } = e.target;
    setForm(f => ({
      ...f,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleSubmit = async e => {
    e.preventDefault();
    setError('');
    if (!form.roomNumber || !form.totalBeds || !form.availableBeds) {
      setError('Room number, total beds, and available beds are required.');
      return;
    }
    setLoading(true);
    try {
      await roomService.createRoom(hostelId, floorId, {
        roomNumber: form.roomNumber,
        roomDetails: form.roomDetails,
        totalBeds: Number(form.totalBeds),
        availableBeds: Number(form.availableBeds),
        isActive: form.isActive,
      });
      navigate(`/owner/hostels/${hostelId}/floors/${floorId}/rooms`, { state: { hostelId, floorId } });
    } catch (err) {
      const errorData = err.response?.data
      
      // Check if error is field-specific validation errors (object with field names as keys)
      if (errorData && typeof errorData === 'object' && !errorData.message) {
        // It's field-specific errors - format them for display
        const errorMessages = Object.values(errorData).join(', ')
        setError(errorMessages || 'Failed to add room.')
      } else {
        // It's a general error message
        setError(errorData?.message || 'Failed to add room.')
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center">
      <div className="bg-white rounded-lg shadow p-6 w-full max-w-md">
        <div className="flex gap-2 items-center mb-6">
          <h1 className="text-2xl font-bold text-gray-800">Add Room</h1>
        </div>
        <form onSubmit={handleSubmit}>
          {error && <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded-lg text-sm">{error}</div>}
          <div className="mb-4">
            <label className="block text-gray-700 font-medium mb-2">Room Number</label>
            <input type="text" name="roomNumber" value={form.roomNumber} onChange={handleChange} className="w-full border border-gray-300 rounded px-3 py-2" required />
          </div>
          <div className="mb-4">
            <label className="block text-gray-700 font-medium mb-2">Room Details</label>
            <input type="text" name="roomDetails" value={form.roomDetails} onChange={handleChange} className="w-full border border-gray-300 rounded px-3 py-2" />
          </div>
          <div className="mb-4">
            <label className="block text-gray-700 font-medium mb-2">Total Beds</label>
            <input type="number" name="totalBeds" value={form.totalBeds} onChange={handleChange} className="w-full border border-gray-300 rounded px-3 py-2" required min={1} />
          </div>
          <div className="mb-4">
            <label className="block text-gray-700 font-medium mb-2">Available Beds</label>
            <input type="number" name="availableBeds" value={form.availableBeds} onChange={handleChange} className="w-full border border-gray-300 rounded px-3 py-2" required min={0} />
          </div>
          <div className="mb-6 flex items-center">
            <input type="checkbox" name="isActive" checked={form.isActive} onChange={handleChange} className="mr-2" />
            <label className="text-gray-700">Active</label>
          </div>
          <Button type="submit" label={loading ? 'Adding...' : 'Add Room'} fullWidth disabled={loading} />
          <Button type="button" label="Cancel" variant="secondary" fullWidth onClick={() => navigate(-1)} />
        </form>
      </div>
    </div>
  );
};

export default CreateRoomPage;
