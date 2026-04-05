import { useState, useEffect } from 'react';
import { hostelService } from '../../services/hostelService';
import HostelList from './HostelList';
import { useNavigate } from 'react-router-dom';
import Button from '../../components/Button';

const OwnerDashboard = () => {
  const navigate = useNavigate()

  const [hostels, setHostels] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchHostels()
  }, [])

  const fetchHostels = async () => {
    try {
      setLoading(true);
      setError('');
      const response = await hostelService.getAllHostels();
      // Map backend response to always include hostelId
      const hostelsData = (response.data ?? []).map(h => ({
        ...h,
        hostelId: h.hostelId || h.id || h._id // fallback if backend uses a different key
      }));
      setHostels(hostelsData);
    } catch (err) {
      console.error(err);
      const errorData = err?.response?.data
      setError(errorData?.message || 'Failed to load hostels. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  const handleHostelClick = (hostel) => {
    navigate(`/owner/hostels/${hostel.hostelId}/floors`);
  };

  return (
    <div className="min-h-screen bg-gray-50 p-4">
      <h1 className="text-2xl font-bold mb-6 text-gray-800">Owner Dashboard</h1>
      {error && (
        <div className="mb-4 p-4 bg-red-100 border border-red-400 text-red-700 rounded-lg">
          {error}
        </div>
      )}
      <div className="mb-8">
        <Button
          label="+ Create Hostel"
          onClick={() => navigate('/owner/create-hostel')}
          fullWidth
        />
          <div className="mt-4">
            <Button
              label="+ Create Agreement"
              onClick={() => navigate('/owner/agreements')}
              fullWidth
              variant="primary"
            />
          </div>
      </div>
      {loading ? (
        <div className="text-gray-500">Loading hostels...</div>
      ) : (
        <HostelList hostels={hostels} onHostelClick={handleHostelClick} />
      )}
    </div>
  );
}

export default OwnerDashboard
