// Importa o componente do formulário de doação
import DonationForm from "./DonationForm.jsx";

// Secção que envolve o formulário de doação
export default function DonationSection() {
  return (
    // Wrapper da secção do formulário
    <div className="wf-form">
      {/* Formulário de doação */}
      <DonationForm />
    </div>
  );
}
