// patientRows.js
export function createPatientRow(patient, appointmentId, doctorId) {
  const tr = document.createElement("tr");
  const values = [patient.id, patient.name, patient.phone, patient.email];
  values.forEach((value, index) => {
    const cell = document.createElement("td");
    if (index === 0) cell.className = "patient-id";
    cell.textContent = value || "";
    if (index === 0) cell.addEventListener("click", () => {
    window.location.href = `/pages/patientRecord.html?id=${patient.id}&doctorId=${doctorId}`;
    });
    tr.append(cell);
  });
  const action = document.createElement("td");
  const image = document.createElement("img");
  image.src = "../assets/images/addPrescriptionIcon/addPrescription.png";
  image.alt = "add prescription";
  image.className = "prescription-btn";
  image.addEventListener("click", () => {
    window.location.href = `/pages/addPrescription.html?appointmentId=${appointmentId}&patientName=${patient.name}`;
  });
  action.append(image);
  tr.append(action);

  return tr;
}
