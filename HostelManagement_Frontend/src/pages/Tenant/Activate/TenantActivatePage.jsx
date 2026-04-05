import { useState, useEffect } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import agreementService from "../../../services/agreementService";
import FormInput from "../../../components/FormInput";
import Button from "../../../components/Button";

export default function TenantActivatePage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");
  const navigate = useNavigate();
  const [agreement, setAgreement] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [step, setStep] = useState("view"); // 'view', 'payment', 'password'
  const [paymentMode, setPaymentMode] = useState("ONLINE");
  const [otp, setOtp] = useState("");
  const [processing, setProcessing] = useState(false);
  const [passwordData, setPasswordData] = useState({
    password: "",
    confirmPassword: "",
  });

  useEffect(() => {
    fetchAgreement();
  }, [token]);

  const fetchAgreement = async () => {
    try {
      setLoading(true);
      setError("");
      const res = await agreementService.getAgreementByQrToken(token);
      setAgreement(res.data);
    } catch (err) {
      const errorData = err?.response?.data
      
      // Check if error is field-specific validation errors (object with field names as keys)
      if (errorData && typeof errorData === 'object' && !errorData.message) {
        // It's field-specific errors - format them for display
        const errorMessages = Object.values(errorData).join(', ')
        setError(errorMessages || "Invalid or expired QR token")
      } else {
        // It's a general error message
        setError(errorData?.message || "Invalid or expired QR token")
      }
    } finally {
      setLoading(false);
    }
  };

  const handleAccept = () => {
    setStep("payment");
  };

  const handleReject = async () => {
    if (!window.confirm("Are you sure you want to reject this agreement?")) {
      return;
    }
    try {
      setProcessing(true);
      await agreementService.rejectAgreement(agreement.id);
      alert("Agreement rejected successfully");
      navigate("/");
    } catch (err) {
      const errorData = err?.response?.data
      
      // Check if error is field-specific validation errors (object with field names as keys)
      if (errorData && typeof errorData === 'object' && !errorData.message) {
        // It's field-specific errors - format them for display
        const errorMessages = Object.values(errorData).join(', ')
        setError(errorMessages || "Failed to reject agreement")
      } else {
        // It's a general error message
        setError(errorData?.message || "Failed to reject agreement")
      }
    } finally {
      setProcessing(false);
    }
  };

  const handlePayment = async () => {
    if (paymentMode === "CASH" && !otp.trim()) {
      setError("Please enter OTP for cash payment");
      return;
    }
    try {
      setProcessing(true);
      setError("");
      await agreementService.acceptAgreement(agreement.id, {
        paymentMode,
        otp: paymentMode === "CASH" ? otp : null,
      });
      setStep("password");
    } catch (err) {
      const errorData = err?.response?.data
      
      // Check if error is field-specific validation errors (object with field names as keys)
      if (errorData && typeof errorData === 'object' && !errorData.message) {
        // It's field-specific errors - format them for display
        const errorMessages = Object.values(errorData).join(', ')
        setError(errorMessages || "Payment failed. Please try again.")
      } else {
        // It's a general error message
        setError(errorData?.message || "Payment failed. Please try again.")
      }
    } finally {
      setProcessing(false);
    }
  };

  const handleSetPassword = async () => {
    if (passwordData.password !== passwordData.confirmPassword) {
      setError("Passwords do not match");
      return;
    }
    if (passwordData.password.length < 8) {
      setError("Password must be at least 8 characters");
      return;
    }
    // TODO: Call password reset API
    alert("Password set successfully! You can now login.");
    navigate("/login");
  };

  const formatDate = (dateString) => {
    if (!dateString) return "N/A";
    return new Date(dateString).toLocaleDateString();
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="text-gray-600">Loading agreement...</div>
        </div>
      </div>
    );
  }

  if (error && !agreement) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-lg shadow-lg p-6 max-w-md w-full text-center">
          <h2 className="text-xl font-bold text-red-600 mb-2">Error</h2>
          <p className="text-gray-700 mb-4">{error}</p>
          <Button label="Go to Home" onClick={() => navigate("/")} variant="primary" />
        </div>
      </div>
    );
  }

  if (step === "password") {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-lg shadow-lg p-6 max-w-md w-full">
          <h2 className="text-2xl font-bold mb-4">Set Your Password</h2>
          {error && <div className="text-red-600 text-sm mb-4">{error}</div>}
          
          <FormInput
            label="Password"
            name="password"
            type="password"
            value={passwordData.password}
            onChange={(e) => setPasswordData({ ...passwordData, password: e.target.value })}
            required
          />
          
          <FormInput
            label="Confirm Password"
            name="confirmPassword"
            type="password"
            value={passwordData.confirmPassword}
            onChange={(e) => setPasswordData({ ...passwordData, confirmPassword: e.target.value })}
            required
          />
          
          <Button
            label="Set Password"
            onClick={handleSetPassword}
            fullWidth
            variant="success"
            className="mt-4"
          />
        </div>
      </div>
    );
  }

  if (step === "payment") {
    return (
      <div className="min-h-screen bg-gray-50 p-4">
        <div className="max-w-2xl mx-auto bg-white rounded-lg shadow-lg p-6">
          <h2 className="text-2xl font-bold mb-4">Payment</h2>
          {error && <div className="text-red-600 text-sm mb-4">{error}</div>}
          
          <div className="space-y-4 mb-6">
            <div>
              <label className="block text-sm font-medium mb-2">Payment Mode</label>
              <div className="space-y-2">
                <label className="flex items-center">
                  <input
                    type="radio"
                    value="ONLINE"
                    checked={paymentMode === "ONLINE"}
                    onChange={(e) => setPaymentMode(e.target.value)}
                    className="mr-2"
                  />
                  Online Payment
                </label>
                <label className="flex items-center">
                  <input
                    type="radio"
                    value="CASH"
                    checked={paymentMode === "CASH"}
                    onChange={(e) => setPaymentMode(e.target.value)}
                    className="mr-2"
                  />
                  Cash Payment
                </label>
              </div>
            </div>
            
            {paymentMode === "CASH" && (
              <FormInput
                label="OTP (for cash verification)"
                name="otp"
                value={otp}
                onChange={(e) => setOtp(e.target.value)}
                placeholder="Enter OTP"
                required
              />
            )}
            
            <div className="bg-gray-50 p-4 rounded">
              <p className="font-semibold mb-2">Payment Summary:</p>
              <div className="space-y-1 text-sm">
                <div className="flex justify-between">
                  <span>Rent:</span>
                  <span>₹{agreement.rent}</span>
                </div>
                {agreement.deposit && (
                  <div className="flex justify-between">
                    <span>Deposit:</span>
                    <span>₹{agreement.deposit}</span>
                  </div>
                )}
                <div className="flex justify-between">
                  <span>Cleaning Charges:</span>
                  <span>₹{agreement.cleaningCharges}</span>
                </div>
                <div className="flex justify-between">
                  <span>Maintenance:</span>
                  <span>₹{agreement.maintenanceCharges}</span>
                </div>
                <div className="flex justify-between font-bold border-t pt-2 mt-2">
                  <span>Total:</span>
                  <span>
                    ₹
                    {parseFloat(agreement.rent) +
                      (agreement.deposit ? parseFloat(agreement.deposit) : 0) +
                      parseFloat(agreement.cleaningCharges) +
                      parseFloat(agreement.maintenanceCharges)}
                  </span>
                </div>
              </div>
            </div>
          </div>
          
          <div className="flex gap-2">
            <Button
              label="Pay Now"
              onClick={handlePayment}
              disabled={processing}
              fullWidth
              variant="success"
            />
            <Button
              label="Back"
              onClick={() => setStep("view")}
              disabled={processing}
              variant="secondary"
            />
          </div>
        </div>
      </div>
    );
  }

  // View agreement step
  return (
    <div className="min-h-screen bg-gray-50 p-4">
      <div className="max-w-2xl mx-auto bg-white rounded-lg shadow-lg p-6">
        <h1 className="text-2xl font-bold mb-6">Agreement Details</h1>
        
        {error && <div className="text-red-600 text-sm mb-4">{error}</div>}
        
        {agreement && (
          <div className="space-y-4 mb-6">
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <span className="font-semibold">Type:</span> {agreement.type}
              </div>
              <div>
                <span className="font-semibold">Status:</span> {agreement.status}
              </div>
              <div>
                <span className="font-semibold">Rent:</span> ₹{agreement.rent}
              </div>
              {agreement.deposit && (
                <div>
                  <span className="font-semibold">Deposit:</span> ₹{agreement.deposit}
                </div>
              )}
              <div>
                <span className="font-semibold">Cleaning Charges:</span> ₹{agreement.cleaningCharges}
              </div>
              <div>
                <span className="font-semibold">Maintenance:</span> ₹{agreement.maintenanceCharges}
              </div>
              <div className="col-span-2">
                <span className="font-semibold">Light Bill Policy:</span> {agreement.lightBillPolicy}
              </div>
              <div className="col-span-2">
                <span className="font-semibold">Facilities:</span>{" "}
                {agreement.facilities?.length > 0 ? agreement.facilities.join(", ") : "None"}
              </div>
              <div>
                <span className="font-semibold">Parking:</span> {agreement.parkingAllowed ? "Yes" : "No"}
              </div>
              <div>
                <span className="font-semibold">Start Date:</span> {formatDate(agreement.startDate)}
              </div>
            </div>
          </div>
        )}
        
        <div className="flex gap-2">
          <Button
            label="Accept & Pay"
            onClick={handleAccept}
            disabled={processing || agreement?.status !== "PENDING_TENANT_ACTION"}
            fullWidth
            variant="success"
          />
          <Button
            label="Reject"
            onClick={handleReject}
            disabled={processing || agreement?.status !== "PENDING_TENANT_ACTION"}
            fullWidth
            variant="danger"
          />
        </div>
      </div>
    </div>
  );
}
