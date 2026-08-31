// patientRecordRow.js
export function createPatientRecordRow(patient) {
  const tr = document.createElement("tr");
  [patient.appointmentDate, patient.id, patient.patientId].forEach((value, index) => {
    const cell = document.createElement("td");
    if (index === 0) cell.className = "patient-id";
    cell.textContent = value || "";
    tr.append(cell);
  });
  const action = document.createElement("td");
  const image = document.createElement("img");
  image.src = "../assets/images/addPrescriptionIcon/addPrescription.png";
  image.alt = "view prescription";
  image.className = "prescription-btn";
  image.addEventListener("click", () => {
    window.location.href = `/pages/addPrescription.html?mode=view&appointmentId=${patient.id}`;
  });
  action.append(image);
  tr.append(action);

  return tr;
}
