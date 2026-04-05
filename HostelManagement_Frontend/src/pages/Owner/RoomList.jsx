import React from 'react';
import Button from '../../components/Button';

const RoomList = ({
  hostel,
  floor,
  rooms,
  loading,
  error,
  onBack,
}) => (
  <div className="mt-6 bg-gray-100 rounded-lg p-4">
    <div className="flex justify-between items-center mb-2">
      <h4 className="font-bold text-gray-700">
        Rooms in Floor {floor.floorNumber} ({hostel.hostelName})
      </h4>
      <Button label="Back" size="sm" variant="secondary" onClick={onBack} />
    </div>
    {loading ? (
      <div className="text-gray-500 text-sm">Loading rooms...</div>
    ) : error ? (
      <div className="text-red-500 text-sm">{error}</div>
    ) : rooms.length === 0 ? (
      <div className="text-gray-500 text-sm">No rooms found.</div>
    ) : (
      <ul className="space-y-2">
        {rooms.map((room) => (
          <li key={room.roomId} className="bg-white rounded p-2 shadow-sm text-sm">
            Room {room.roomNumber}
          </li>
        ))}
      </ul>
    )}
  </div>
);

export default RoomList;
