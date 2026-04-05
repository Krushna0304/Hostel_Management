import { useEffect, useState } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import Button from '../../components/Button';
import { floorService } from '../../services/hostelService';

const Floors = () => {
  const { hostelId: paramHostelId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  // Prefer hostelId from location.state if available, else from params
  const hostelId = location.state?.hostelId || paramHostelId;
  const [floors, setFloors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchFloors = async () => {
      setLoading(true);
      setError('');
      try {
        const res = await floorService.getFloorsByHostel(hostelId);
        setFloors(res.data || []);
      } catch (err) {
        const errorData = err?.response?.data
        setError(errorData?.message || 'Failed to load floors.');
      } finally {
        setLoading(false);
      }
    };
    fetchFloors();
  }, [hostelId]);

  return (
    <div className="min-h-screen bg-gray-50 p-4">
      <div className="flex justify-between items-center mb-4">
        <div className="flex gap-2 items-center">
          <Button label="Back" variant="secondary" onClick={() => navigate(-1)} />
          <h2 className="text-xl font-bold text-gray-800">Floors</h2>
        </div>
        <Button label="Add Floor" onClick={() => navigate(`/owner/hostels/${hostelId}/add-floor`, { state: { hostelId } })} />
      </div>
      {loading ? (
        <div className="text-gray-500">Loading floors...</div>
      ) : error ? (
        <div className="text-red-500">{error}</div>
      ) : floors.length === 0 ? (
        <div className="text-gray-500">No floors found.</div>
      ) : (
        <ul className="space-y-2">
          {floors.map(floor => (
            <li
              key={floor.floorId}
              className="bg-white rounded p-2 shadow-sm text-sm cursor-pointer"
              onClick={() => navigate(`/owner/hostels/${hostelId}/floors/${floor.floorId}/rooms`, { state: { hostelId, floorId: floor.floorId } })}
            >
              Floor {floor.floorNumber}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default Floors;
