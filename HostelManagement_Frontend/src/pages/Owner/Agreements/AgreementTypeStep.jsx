import Button from "../../../components/Button";

export default function AgreementTypeStep({ nextStep, prevStep, setFormData }) {
  const handleTypeSelect = (type) => {
    setFormData((prev) => ({ ...prev, agreementType: type }));
    nextStep();
  };

  const handleCancel = () => {
    // Navigate back to agreements list
    window.location.href = '/owner/agreements';
  };

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-semibold mb-4">Select Agreement Type</h2>
      <Button
        label="Room Agreement"
        onClick={() => handleTypeSelect("ROOM")}
        fullWidth
        variant="primary"
      />
      <Button
        label="Worker Agreement"
        onClick={() => handleTypeSelect("WORKER")}
        fullWidth
        variant="secondary"
      />
      <Button
        label="Cancel"
        onClick={handleCancel}
        fullWidth
        variant="secondary"
      />
    </div>
  );
}
