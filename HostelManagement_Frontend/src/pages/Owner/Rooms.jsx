import { useEffect, useState } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import Button from '../../components/Button';
import { roomService } from '../../services/hostelService';

const Rooms = () => {
  const { hostelId: paramHostelId, floorId: paramFloorId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  // Prefer ids from location.state if available
  const hostelId = location.state?.hostelId || paramHostelId;
  const floorId = location.state?.floorId || paramFloorId;
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchRooms = async () => {
      setLoading(true);
      setError('');
      try {
        const res = await roomService.getRoomsByFloor(hostelId, floorId);
        setRooms(res.data || []);
      } catch (err) {
        const errorData = err?.response?.data
        setError(errorData?.message || 'Failed to load rooms.');
      } finally {
        setLoading(false);
      }
    };
    fetchRooms();
  }, [hostelId, floorId]);

  return (
    <div className="min-h-screen bg-gray-50 p-4">
      <div className="flex justify-between items-center mb-4">
        <div className="flex gap-2 items-center">
          <Button label="Back" variant="secondary" onClick={() => navigate(-1)} />
          <h2 className="text-xl font-bold text-gray-800">Rooms</h2>
        </div>
        <Button label="Add Room" onClick={() => navigate(`/owner/hostels/${hostelId}/floors/${floorId}/add-room`, { state: { hostelId, floorId } })} />
      </div>
      {loading ? (
        <div className="text-gray-500">Loading rooms...</div>
      ) : error ? (
        <div className="text-red-500">{error}</div>
      ) : rooms.length === 0 ? (
        <div className="text-gray-500">No rooms found.</div>
      ) : (
        <ul className="space-y-2">
          {rooms.map(room => (
            <li key={room.roomId} className="bg-white rounded p-2 shadow-sm text-sm">
              Room {room.roomNumber}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default Rooms;
