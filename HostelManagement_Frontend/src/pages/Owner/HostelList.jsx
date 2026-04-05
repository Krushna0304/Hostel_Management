import React from 'react';

const HostelList = ({ hostels, onHostelClick }) => (
  <div className="bg-white rounded-lg shadow">
    <div className="px-4 py-6 border-b border-gray-200">
      <h2 className="text-xl font-bold text-gray-800">Your Hostels</h2>
    </div>
    <div className="divide-y divide-gray-200">
      {hostels.map((hostel) => (
        <div
          key={hostel.hostelId}
          className="px-4 py-4 hover:bg-gray-50 cursor-pointer"
          onClick={() => onHostelClick(hostel)}
        >
          <h3 className="font-semibold text-gray-800 text-lg truncate">{hostel.hostelName}</h3>
          <p className="text-sm text-gray-600 break-words">{hostel.hostelAddress}</p>
          {hostel.ownerName && (
            <p className="text-xs text-gray-500">Owner: {hostel.ownerName}</p>
          )}
        </div>
      ))}
    </div>
  </div>
);

export default HostelList;
