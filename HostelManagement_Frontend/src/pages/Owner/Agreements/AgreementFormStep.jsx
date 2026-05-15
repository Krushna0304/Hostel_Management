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

  // Co-tenant state
  const [coTenantInput, setCoTenantInput] = useState("");
  const [coTenantError, setCoTenantError] = useState("");

  const [agreementData, setAgreementData] = useState({
    roomId: formData.roomId || "",
    planId: formData.planId || "",
    startDate: formData.startDate || "",
    coTenantNames: formData.coTenantNames || [],
  });

  const isFlat = formData.agreementType === "FLAT";
  const isRoom = formData.agreementType === "ROOM";

  // Derive the roomType param for fetching
  const getRoomTypeParam = () => {
    if (isFlat) return "FLAT";
    if (isRoom) return "PG_ROOM";
    return undefined;
  };

  // Derive the planType param for fetching
  const getPlanTypeParam = () => {
    if (isFlat) return "FLAT";
    if (isRoom) return "PG_ROOM";
    return undefined;
  };

  useEffect(() => {
    fetchHostels();
    fetchPlans();
  }, []);

  // Re-fetch plans when agreementType changes
  useEffect(() => {
    fetchPlans();
  }, [formData.agreementType]);

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
      const roomType = getRoomTypeParam();
      const res = await roomService.getActiveRoomsByFloor(hostelId, floorId, roomType);
      setRooms(res.data || []);
    } catch (err) {
      console.error("Failed to fetch rooms", err);
    }
  };

  const fetchPlans = async () => {
    try {
      const planType = getPlanTypeParam();
      const res = await agreementService.getActivePlans(planType);
      setPlans(res.data || []);
    } catch (err) {
      console.error("Failed to fetch plans", err);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    
    // If room is changing, clear co-tenants since the bed limit might be different
    if (name === 'roomId' && isFlat && value !== agreementData.roomId) {
      setAgreementData((prev) => ({
        ...prev,
        [name]: value,
        coTenantNames: [], // Clear co-tenants when room changes
      }));
    } else {
      setAgreementData((prev) => ({
        ...prev,
        [name]: value,
      }));
    }
    
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: "" }));
    }
  };

  // Co-tenant handlers
  const handleAddCoTenant = () => {
    const trimmed = coTenantInput.trim();
    if (!trimmed) return;
    if (trimmed.length > 100) {
      setCoTenantError("Co-tenant name must not exceed 100 characters");
      return;
    }

    // Get the selected room to check available beds
    const selectedRoom = rooms.find(r => r.roomId === agreementData.roomId);
    const maxCoTenants = selectedRoom ? selectedRoom.availableBeds - 1 : 5; // Default to 5 if no room selected

    if (agreementData.coTenantNames.length >= maxCoTenants) {
      setCoTenantError(`Maximum ${maxCoTenants} co-tenants allowed for this room (${selectedRoom?.availableBeds} beds available, 1 for primary tenant)`);
      return;
    }

    setAgreementData(prev => ({
      ...prev,
      coTenantNames: [...prev.coTenantNames, trimmed],
    }));
    setCoTenantInput("");
    setCoTenantError("");
  };

  const handleRemoveCoTenant = (index) => {
    setAgreementData(prev => ({
      ...prev,
      coTenantNames: prev.coTenantNames.filter((_, i) => i !== index),
    }));
  };

  const handleCoTenantInputChange = (e) => {
    const val = e.target.value;
    setCoTenantInput(val);
    if (val.length > 100) {
      setCoTenantError("Co-tenant name must not exceed 100 characters");
    } else {
      setCoTenantError("");
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

    const update = {
      ...agreementData,
      hostelId: selectedHostelId,
      floorId: selectedFloorId,
      plan: selectedPlan,
      hostelNumber: selectedHostel ? (selectedHostel.hostelNumber || selectedHostel.hostelName || selectedHostel.name) : undefined,
      roomNumber: selectedRoom ? selectedRoom.roomNumber : undefined,
    };

    if (isFlat) {
      update.coTenantNames = agreementData.coTenantNames;
    }

    setFormData((prev) => ({
      ...prev,
      ...update,
    }));
    nextStep();
  };

  // Empty-state messages
  const plansEmptyMessage = isFlat
    ? "No Flat plans available. Please create a Flat plan first."
    : isRoom
    ? "No PG Room plans available. Please create a PG Room plan first."
    : null;

  const roomsEmptyMessage = isFlat
    ? "No Flat rooms available on this floor."
    : isRoom
    ? "No PG Rooms available on this floor."
    : null;

  const plansLoaded = plans.length === 0 && plansEmptyMessage;
  const roomsLoaded = rooms.length === 0 && roomsEmptyMessage && selectedHostelId && selectedFloorId;

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
          {roomsLoaded ? (
            <p className="text-sm text-amber-600 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">
              {roomsEmptyMessage}
            </p>
          ) : (
            <select
              name="roomId"
              value={agreementData.roomId}
              onChange={handleChange}
              disabled={roomsLoaded}
              className="w-full px-4 py-2 border rounded-lg disabled:bg-gray-100 disabled:cursor-not-allowed"
            >
              <option value="">Select a room</option>
              {rooms.map((room) => (
                <option key={room.roomId} value={room.roomId}>
                  Room {room.roomNumber} ({room.availableBeds} beds available)
                </option>
              ))}
            </select>
          )}
          {errors.roomId && <p className="text-red-500 text-sm">{errors.roomId}</p>}
        </div>
      )}

      {/* Plan Selection */}
      <div className="space-y-2">
        <label className="block text-sm font-medium text-gray-700">
          Select Agreement Plan <span className="text-red-500">*</span>
        </label>
        {plansLoaded ? (
          <p className="text-sm text-amber-600 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">
            {plansEmptyMessage}
          </p>
        ) : (
          <select
            name="planId"
            value={agreementData.planId}
            onChange={handleChange}
            disabled={plansLoaded}
            className="w-full px-4 py-2 border rounded-lg disabled:bg-gray-100 disabled:cursor-not-allowed"
          >
            <option value="">Select a plan</option>
            {plans.map((plan) => (
              <option key={plan.id} value={plan.id}>
                {plan.planName} - ₹{plan.rentDetails?.monthlyRent}/month
              </option>
            ))}
          </select>
        )}
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

      {/* Co-tenant Names Section — visible only for FLAT agreements */}
      {isFlat && (
        <div className="space-y-3 border border-gray-200 rounded-lg p-4 bg-gray-50">
          <h3 className="text-sm font-semibold text-gray-700">Co-tenants</h3>

          {/* Input row */}
          <div className="flex gap-2 items-start">
            <div className="flex-1">
              <input
                type="text"
                value={coTenantInput}
                onChange={handleCoTenantInputChange}
                placeholder="Enter co-tenant name"
                className="w-full px-3 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-300"
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    handleAddCoTenant();
                  }
                }}
              />
              {coTenantError && (
                <p className="text-red-500 text-xs mt-1">{coTenantError}</p>
              )}
            </div>
            <button
              type="button"
              onClick={handleAddCoTenant}
              disabled={(() => {
                const selectedRoom = rooms.find(r => r.roomId === agreementData.roomId);
                const maxCoTenants = selectedRoom ? selectedRoom.availableBeds - 1 : 5;
                return agreementData.coTenantNames.length >= maxCoTenants || !coTenantInput.trim();
              })()}
              className="px-3 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed whitespace-nowrap"
            >
              Add co-tenant
            </button>
          </div>

          {/* Co-tenant list */}
          {agreementData.coTenantNames.length > 0 && (
            <ul className="space-y-1">
              {agreementData.coTenantNames.map((name, index) => (
                <li
                  key={index}
                  className="flex items-center justify-between bg-white border border-gray-200 rounded-lg px-3 py-2 text-sm"
                >
                  <span>{name}</span>
                  <button
                    type="button"
                    onClick={() => handleRemoveCoTenant(index)}
                    className="text-gray-400 hover:text-red-500 ml-2 font-bold leading-none"
                    aria-label={`Remove ${name}`}
                  >
                    ✕
                  </button>
                </li>
              ))}
            </ul>
          )}

          {/* Dynamic help text based on selected room */}
          {(() => {
            const selectedRoom = rooms.find(r => r.roomId === agreementData.roomId);
            if (selectedRoom) {
              const maxCoTenants = selectedRoom.availableBeds - 1;
              const currentCount = agreementData.coTenantNames.length;
              return (
                <div className="text-xs">
                  {currentCount === 0 ? (
                    <p className="text-gray-400">
                      No co-tenants added yet. You can add up to {maxCoTenants} co-tenants for this room 
                      ({selectedRoom.availableBeds} beds available, 1 reserved for primary tenant).
                    </p>
                  ) : (
                    <p className="text-gray-600">
                      {currentCount} of {maxCoTenants} co-tenants added 
                      ({selectedRoom.availableBeds} beds available, 1 for primary tenant).
                    </p>
                  )}
                </div>
              );
            } else {
              return (
                <p className="text-xs text-gray-400">
                  Select a room first to see the maximum number of co-tenants allowed.
                </p>
              );
            }
          })()}
        </div>
      )}

      <FormInput
        label="Start Date"
        name="startDate"
        type="date"
        value={agreementData.startDate}
        onChange={handleChange}
        min={new Date().toISOString().split('T')[0]}
        required
        error={errors.startDate}
      />

      <div className="flex gap-2">
        <Button
          label="Review"
          onClick={handleNext}
          fullWidth
          variant="primary"
          disabled={plansLoaded}
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
