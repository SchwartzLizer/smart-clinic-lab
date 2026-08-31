import { savePrescription, getPrescription } from "./services/prescriptionServices.js";

document.addEventListener('DOMContentLoaded', async () => {
  const savePrescriptionBtn = document.getElementById("savePrescription");
  const patientNameInput = document.getElementById("patientName");
  const medicinesInput = document.getElementById("medicines");
  const dosageInput = document.getElementById("dosage");
  const notesInput = document.getElementById("notes");
  const heading = document.getElementById("heading")


  const urlParams = new URLSearchParams(window.location.search);
  const appointmentId = urlParams.get("appointmentId");
  const mode = urlParams.get("mode");
  const patientName = urlParams.get("patientName")

  if (heading) {
    if (mode === "view") {
      heading.innerHTML = `View <span>Prescription</span>`;
    } else {
      heading.innerHTML = `Add <span>Prescription</span>`;
    }
  }


  // Pre-fill patient name
  if (patientNameInput && patientName) {
    patientNameInput.value = patientName;
  }

  // Fetch and pre-fill existing prescription if it exists
  if (appointmentId) {
    try {
      const response = await getPrescription(appointmentId);
      console.log("getPrescription :: ", response);

      // Now, check if the prescription exists in the response and access it from the array
      if (response) {
        const existingPrescription = response;
        patientNameInput.value = existingPrescription.patientName || "You";
        medicinesInput.value = existingPrescription.medication || "";
        dosageInput.value = existingPrescription.dosage || "";
        notesInput.value = existingPrescription.doctorNotes || "";
      }

    } catch (error) {
      console.warn("No existing prescription found or failed to load:", error);
    }
  }
  if (mode === 'view') {
    // Make fields read-only
    patientNameInput.disabled = true;
    medicinesInput.disabled = true;
    dosageInput.disabled = true;
    notesInput.disabled = true;
    savePrescriptionBtn.style.display = "none";  // Hide the save button
  }
  // Save prescription on button click
  savePrescriptionBtn.addEventListener('click', async (e) => {
    e.preventDefault();

    const prescription = {
      patientName: patientNameInput.value,
      medication: medicinesInput.value,
      dosage: dosageInput.value,
      doctorNotes: notesInput.value,
      appointmentId
    };

    const { success, message } = await savePrescription(prescription);

    if (success) {
      alert("✅ Prescription saved successfully.");
      window.location.href = "/doctorDashboard";
    } else {
      alert("❌ Failed to save prescription. " + message);
    }
  });
});
