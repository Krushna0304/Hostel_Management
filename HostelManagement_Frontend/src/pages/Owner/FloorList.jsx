import React from 'react';
import Button from '../../components/Button';
import { useState } from 'react';
import CreateFloor from './CreateFloor';


const FloorList = ({
  hostel,
  floors,
  loading,
  error,
  onBack,
  onFloorClick,
}) => {
  const [showCreate, setShowCreate] = useState(false);
  return showCreate ? (
    <CreateFloor hostelId={hostel.hostelId} />
  ) : (
    <div className="mt-6 bg-gray-100 rounded-lg p-4">
      <div className="flex justify-between items-center mb-2">
        <h4 className="font-bold text-gray-700">Floors in {hostel.hostelName}</h4>
        <div className="flex gap-2">
          <Button label="Back" size="sm" variant="secondary" onClick={onBack} />
          <Button label="+ Add Floor" size="sm" onClick={() => setShowCreate(true)} />
        </div>
      </div>
      {loading ? (
        <div className="text-gray-500 text-sm">Loading floors...</div>
      ) : error ? (
        <div className="text-red-500 text-sm">{error}</div>
      ) : floors.length === 0 ? (
        <div className="text-gray-500 text-sm">No floors found.</div>
      ) : (
        <ul className="space-y-2">
          {floors.map((floor) => (
            <li
              key={floor.floorId}
              className="bg-white rounded p-2 shadow-sm text-sm cursor-pointer"
              onClick={() => onFloorClick(floor)}
            >
              Floor {floor.floorNumber}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default FloorList;
