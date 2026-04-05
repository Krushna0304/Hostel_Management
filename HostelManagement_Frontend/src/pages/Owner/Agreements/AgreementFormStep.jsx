import { useState, useEffect } from "react";
import FormInput from "../../../components/FormInput";
import Button from "../../../components/Button";
import { hostelService, floorService, roomService } from "../../../services/hostelService";
import agreementService from "../../../services/agreementService";

export default function AgreementFormStep({ nextStep, prevStep, formData, setFormData }) {
  const [hostels, setHostels] = useState([]);
  const [floors, setFloors] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [plans, setPlans] = useState([]);
  const [selectedHostelId, setSelectedHostelId] = useState(formData.hostelId || "");
  const [selectedFloorId, setSelectedFloorId] = useState(formData.floorId || "");
  const [selectedPlan, setSelectedPlan] = useState(null);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState({});
  
  const [agreementData, setAgreementData] = useState({
    roomId: formData.roomId || "",
    planId: formData.planId || "",
    startDate: formData.startDate || "",
  });

  useEffect(() => {
    fetchHostels();
    fetchPlans();
  }, []);

  useEffect(() => {
    if (selectedHostelId) {
      fetchFloors(selectedHostelId);
      setFloors([]);
      setRooms([]);
      setSelectedFloorId("");
      setAgreementData(prev => ({ ...prev, roomId: "" }));
    }
  }, [selectedHostelId]);

  useEffect(() => {
    if (selectedHostelId && selectedFloorId) {
      fetchRooms(selectedHostelId, selectedFloorId);
      setAgreementData(prev => ({ ...prev, roomId: "" }));
    }
  }, [selectedHostelId, selectedFloorId]);

  useEffect(() => {
    if (agreementData.planId) {
      const plan = plans.find(p => p.id === agreementData.planId);
      setSelectedPlan(plan || null);
    }
  }, [agreementData.planId, plans]);

  const fetchHostels = async () => {
    try {
      const res = await hostelService.getAllHostels();
      setHostels(res.data || []);
    } catch (err) {
      console.error("Failed to fetch hostels", err);
    }
  };

  const fetchFloors = async (hostelId) => {
    try {
      const res = await floorService.getFloorsByHostel(hostelId);
      setFloors(res.data || []);
    } catch (err) {
      console.error("Failed to fetch floors", err);
    }
  };

  const fetchRooms = async (hostelId, floorId) => {
    try {
      const res = await roomService.getActiveRoomsByFloor(hostelId, floorId);
      setRooms(res.data || []);
    } catch (err) {
      console.error("Failed to fetch rooms", err);
    }
  };

  const fetchPlans = async () => {
    try {
      const res = await agreementService.getActivePlans();
      setPlans(res.data || []);
    } catch (err) {
      console.error("Failed to fetch plans", err);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setAgreementData((prev) => ({
      ...prev,
      [name]: value,
    }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: "" }));
    }
  };

  const validate = () => {
    const newErrors = {};
    if (!agreementData.roomId) newErrors.roomId = "Room is required";
    if (!agreementData.planId) newErrors.planId = "Plan is required";
    if (!agreementData.startDate) newErrors.startDate = "Start date is required";
    return newErrors;
  };

  const handleNext = () => {
    const validationErrors = validate();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    const selectedHostel = hostels.find(h => h.hostelId === selectedHostelId);
    const selectedRoom = rooms.find(r => r.roomId === agreementData.roomId);

    setFormData((prev) => ({
      ...prev,
      ...agreementData,
      hostelId: selectedHostelId,
      floorId: selectedFloorId,
      plan: selectedPlan,
      hostelNumber: selectedHostel ? (selectedHostel.hostelNumber || selectedHostel.hostelName || selectedHostel.name) : undefined,
      roomNumber: selectedRoom ? selectedRoom.roomNumber : undefined,
    }));
    nextStep();
  };

  return (
    <div className="space-y-4 max-h-[600px] overflow-y-auto">
      <h2 className="text-xl font-semibold mb-4">Agreement Details</h2>
      
      {/* Hostel Selection */}
      <div className="space-y-2">
        <label className="block text-sm font-medium text-gray-700">
          Select Hostel <span className="text-red-500">*</span>
        </label>
        <select
          value={selectedHostelId}
          onChange={(e) => setSelectedHostelId(e.target.value)}
          className="w-full px-4 py-2 border rounded-lg"
        >
          <option value="">Select a hostel</option>
          {hostels.map((hostel) => (
            <option key={hostel.hostelId} value={hostel.hostelId}>
              {hostel.hostelName}
            </option>
          ))}
        </select>
      </div>

      {/* Floor Selection */}
      {selectedHostelId && (
        <div className="space-y-2">
          <label className="block text-sm font-medium text-gray-700">
            Select Floor <span className="text-red-500">*</span>
          </label>
          <select
            value={selectedFloorId}
            onChange={(e) => setSelectedFloorId(e.target.value)}
            className="w-full px-4 py-2 border rounded-lg"
          >
            <option value="">Select a floor</option>
            {floors.map((floor) => (
              <option key={floor.floorId} value={floor.floorId}>
                Floor {floor.floorNumber}
              </option>
            ))}
          </select>
        </div>
      )}

      {/* Room Selection */}
      {selectedHostelId && selectedFloorId && (
        <div className="space-y-2">
          <label className="block text-sm font-medium text-gray-700">
            Select Room <span className="text-red-500">*</span>
          </label>
          <select
            name="roomId"
            value={agreementData.roomId}
            onChange={handleChange}
            className="w-full px-4 py-2 border rounded-lg"
          >
            <option value="">Select a room</option>
            {rooms.map((room) => (
              <option key={room.roomId} value={room.roomId}>
                Room {room.roomNumber} ({room.availableBeds} beds available)
              </option>
            ))}
          </select>
          {errors.roomId && <p className="text-red-500 text-sm">{errors.roomId}</p>}
        </div>
      )}

      {/* Plan Selection */}
      <div className="space-y-2">
        <label className="block text-sm font-medium text-gray-700">
          Select Agreement Plan <span className="text-red-500">*</span>
        </label>
        <select
          name="planId"
          value={agreementData.planId}
          onChange={handleChange}
          className="w-full px-4 py-2 border rounded-lg"
        >
          <option value="">Select a plan</option>
          {plans.map((plan) => (
            <option key={plan.id} value={plan.id}>
              {plan.planName} - ₹{plan.rentDetails?.monthlyRent}/month
            </option>
          ))}
        </select>
        {errors.planId && <p className="text-red-500 text-sm">{errors.planId}</p>}
      </div>

      {/* Plan Details Display */}
      {selectedPlan && (
        <div className="mt-4 p-4 bg-blue-50 rounded-lg border border-blue-200">
          <h3 className="font-semibold text-lg mb-3 text-blue-900">{selectedPlan.planName}</h3>
          
          <div className="space-y-2 text-sm">
            <div>
              <span className="font-medium">Monthly Rent:</span> ₹{selectedPlan.rentDetails?.monthlyRent} {selectedPlan.rentDetails?.currency}
            </div>
            {selectedPlan.charges?.securityDeposit && (
              <div>
                <span className="font-medium">Security Deposit:</span> ₹{selectedPlan.charges.securityDeposit.amount} 
                {selectedPlan.charges.securityDeposit.refundable && " (Refundable)"}
              </div>
            )}
            {selectedPlan.duration && (
              <div>
                <span className="font-medium">Duration:</span> {selectedPlan.duration.value} {selectedPlan.duration.unit}(s)
                {selectedPlan.duration.minimumStayMonths && ` (Minimum ${selectedPlan.duration.minimumStayMonths} months)`}
              </div>
            )}
            
            {selectedPlan.freeFacilities?.facilities && selectedPlan.freeFacilities.facilities.length > 0 && (
              <div className="mt-3">
                <span className="font-medium block mb-2">Free Facilities Included:</span>
                <div className="flex flex-wrap gap-2">
                  {selectedPlan.freeFacilities.facilities.map((facility, index) => (
                    <span
                      key={index}
                      className="px-2 py-1 bg-green-100 text-green-800 rounded text-xs"
                      title={facility.description}
                    >
                      {facility.name}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      <FormInput
        label="Start Date"
        name="startDate"
        type="date"
        value={agreementData.startDate}
        onChange={handleChange}
        required
        error={errors.startDate}
      />

      <div className="flex gap-2">
        <Button
          label="Review"
          onClick={handleNext}
          fullWidth
          variant="primary"
        />
        <Button
          label="Cancel"
          onClick={prevStep}
          fullWidth
          variant="secondary"
        />
      </div>
    </div>
  );
}
