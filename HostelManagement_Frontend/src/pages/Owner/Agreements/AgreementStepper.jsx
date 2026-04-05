import { useState } from "react";
import UserSelectStep from "./UserSelectStep";
import AgreementTypeStep from "./AgreementTypeStep";
import AgreementFormStep from "./AgreementFormStep";
import AgreementReviewStep from "./AgreementReviewStep";

export default function AgreementStepper() {
  const [step, setStep] = useState(0);
  const [formData, setFormData] = useState({});

  const nextStep = () => setStep((s) => s + 1);
  const prevStep = () => setStep((s) => s - 1);

  return (
    <div className="max-w-xl mx-auto bg-white p-6 rounded shadow">
      <div className="mb-6 flex justify-between">
        {["Type", "User", "Form", "Review"].map((label, idx) => (
          <div key={label} className={`flex-1 text-center ${step === idx ? "font-bold text-blue-600" : "text-gray-400"}`}>{label}</div>
        ))}
      </div>
      {step === 0 && <AgreementTypeStep nextStep={nextStep} prevStep={prevStep} setFormData={setFormData} />}
      {step === 1 && <UserSelectStep nextStep={nextStep} prevStep={prevStep} formData={formData} setFormData={setFormData} />}
      {step === 2 && <AgreementFormStep nextStep={nextStep} prevStep={prevStep} formData={formData} setFormData={setFormData} />}
      {step === 3 && <AgreementReviewStep prevStep={prevStep} formData={formData} />}
    </div>
  );
}
