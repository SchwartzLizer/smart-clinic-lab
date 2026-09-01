import { savePrescription, getPrescription } from "./services/prescriptionServices.js";

export function setPrescriptionStatus(status, message, state) {
  status.textContent = message;
  status.dataset.state = state;
  status.hidden = false;
}

export function buildPrescription({ patientName, medicines, dosage, notes, appointmentId }) {
  return {
    patientName: patientName.value,
    medication: medicines.value,
    dosage: dosage.value,
    doctorNotes: notes.value,
    appointmentId,
  };
}

export async function submitPrescription({ prescription, save, status, submitButton }) {
  submitButton.disabled = true;
  setPrescriptionStatus(status, "Saving prescription…", "pending");

  const { success, message } = await save(prescription);
  if (success) {
    setPrescriptionStatus(status, "Prescription saved successfully.", "success");
    submitButton.textContent = "Prescription saved";
    return true;
  }

  submitButton.disabled = false;
  setPrescriptionStatus(status, `Failed to save prescription. ${message}`, "error");
  return false;
}

function initializePage() {
  const form = document.getElementById("prescriptionForm");
  const savePrescriptionBtn = document.getElementById("savePrescription");
  const patientNameInput = document.getElementById("patientName");
  const medicinesInput = document.getElementById("medicines");
  const dosageInput = document.getElementById("dosage");
  const notesInput = document.getElementById("notes");
  const heading = document.getElementById("heading");
  const status = document.getElementById("prescriptionStatus");
  const urlParams = new URLSearchParams(window.location.search);
  const appointmentId = urlParams.get("appointmentId");
  const mode = urlParams.get("mode");
  const patientName = urlParams.get("patientName");

  if (!form || !savePrescriptionBtn || !patientNameInput || !medicinesInput || !dosageInput || !notesInput || !status) return;

  heading.textContent = mode === "view" ? "View Prescription" : "Add Prescription";
  if (patientName) patientNameInput.value = patientName;

  if (appointmentId) {
    getPrescription(appointmentId)
      .then((existingPrescription) => {
        if (!existingPrescription) return;
        patientNameInput.value = existingPrescription.patientName || "You";
        medicinesInput.value = existingPrescription.medication || "";
        dosageInput.value = existingPrescription.dosage || "";
        notesInput.value = existingPrescription.doctorNotes || "";
      })
      .catch(() => {});
  }

  if (mode === "view") {
    patientNameInput.disabled = true;
    medicinesInput.disabled = true;
    dosageInput.disabled = true;
    notesInput.disabled = true;
    savePrescriptionBtn.classList.add("is-hidden");
  }

  document.getElementById("cancelPrescription")?.addEventListener("click", () => selectRole("doctor"));
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    await submitPrescription({
      prescription: buildPrescription({
        patientName: patientNameInput,
        medicines: medicinesInput,
        dosage: dosageInput,
        notes: notesInput,
        appointmentId,
      }),
      save: savePrescription,
      status,
      submitButton: savePrescriptionBtn,
    });
  });
}

if (globalThis.document) {
  document.addEventListener("DOMContentLoaded", initializePage);
}
