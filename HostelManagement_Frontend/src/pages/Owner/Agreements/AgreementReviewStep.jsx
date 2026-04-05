import { useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import agreementService from "../../../services/agreementService";
import Button from "../../../components/Button";

export default function AgreementReviewStep({ prevStep, formData }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(null);

  const handleConfirm = async () => {
    setLoading(true);
    setError("");
    try {
      // Prepare agreement data - using plan-based approach
      const agreementPayload = {
        userId: formData.userId,
        roomId: formData.roomId,
        planId: formData.planId,
        startDate: formData.startDate,
      };

      const res = await agreementService.createRoomAgreement(agreementPayload);
      setSuccess(res.data);
    } catch (err) {
      const errorData = err?.response?.data
      
      // Check if error is field-specific validation errors (object with field names as keys)
      if (errorData && typeof errorData === 'object' && !errorData.message) {
        // It's field-specific errors - format them for display
        const errorMessages = Object.values(errorData).join(', ')
        setError(errorMessages || "Failed to create agreement.")
      } else {
        // It's a general error message
        setError(errorData?.message || "Failed to create agreement.")
      }
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return "N/A";
    return new Date(dateString).toLocaleDateString();
  };

  if (success) {
    return (
      <div className="space-y-4">
        <div className="bg-green-50 border border-green-200 rounded-lg p-6">
          <h3 className="text-xl font-bold text-green-800 mb-4">✓ Agreement Created Successfully!</h3>
          
          <div className="space-y-2 mb-4">
            <p><span className="font-semibold">Agreement ID:</span> {success.agreementId}</p>
            <p><span className="font-semibold">QR Token:</span> {success.qrToken}</p>
            <p><span className="font-semibold">Expires:</span> {formatDate(success.expiry)}</p>
          </div>

          <div className="bg-white p-4 rounded border-2 border-dashed border-gray-300 text-center">
            <p className="text-sm text-gray-600 mb-2">Share this QR code with the tenant:</p>
            <div className="bg-white p-4 inline-block rounded flex justify-center">
              <QRCodeSVG
                value={`${window.location.origin}/tenant/activate?token=${success.qrToken}`}
                size={200}
                level="H"
                includeMargin={true}
              />
            </div>
            <p className="text-xs text-gray-500 mt-2 break-all">
              Activation URL: {window.location.origin}/tenant/activate?token={success.qrToken}
            </p>
          </div>
        </div>
        
        <Button
          label="Create Another Agreement"
          onClick={() => window.location.reload()}
          fullWidth
          variant="primary"
        />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-semibold mb-4">Review Agreement</h2>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 p-3 rounded">
          {error}
        </div>
      )}

      <div className="bg-gray-50 rounded-lg p-4 space-y-3">
        <div className="grid grid-cols-2 gap-2 text-sm">
          <div><span className="font-semibold">User:</span> {formData.userDisplayName || formData.userId}</div>
          <div><span className="font-semibold">Room Number:</span> {formData.roomNumber || formData.roomId}</div>
          <div><span className="font-semibold">Hostel Number:</span> {formData.hostelNumber || formData.hostelId}</div>
          {formData.plan && (
            <>
              <div className="col-span-2"><span className="font-semibold">Plan:</span> {formData.plan.planName}</div>
              <div><span className="font-semibold">Monthly Rent:</span> ₹{formData.plan.rentDetails?.monthlyRent} {formData.plan.rentDetails?.currency}</div>
              {formData.plan.charges?.securityDeposit && (
                <div><span className="font-semibold">Security Deposit:</span> ₹{formData.plan.charges.securityDeposit.amount} 
                {formData.plan.charges.securityDeposit.refundable && " (Refundable)"}</div>
              )}
              {formData.plan.duration && (
                <div className="col-span-2">
                  <span className="font-semibold">Duration:</span> {formData.plan.duration.value} {formData.plan.duration.unit}(s)
                  {formData.plan.duration.minimumStayMonths && ` (Minimum ${formData.plan.duration.minimumStayMonths} months)`}
                </div>
              )}
              {formData.plan.freeFacilities?.facilities && formData.plan.freeFacilities.facilities.length > 0 && (
                <div className="col-span-2">
                  <span className="font-semibold">Free Facilities:</span>{" "}
                  {formData.plan.freeFacilities.facilities.map(f => f.name).join(", ")}
                </div>
              )}
            </>
          )}
          <div><span className="font-semibold">Start Date:</span> {formatDate(formData.startDate)}</div>
        </div>
      </div>

      <div className="flex gap-2">
        <Button
          label="Confirm & Create"
          onClick={handleConfirm}
          disabled={loading}
          fullWidth
          variant="success"
        />
        <Button
          label="Edit"
          onClick={prevStep}
          disabled={loading}
          fullWidth
          variant="secondary"
        />
      </div>
    </div>
  );
}
