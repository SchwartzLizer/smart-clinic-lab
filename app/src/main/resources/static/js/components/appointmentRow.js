// appointmentRow.js
export function getAppointments(appointment) {
  const tr = document.createElement("tr");
  const values = [appointment.patientName, appointment.doctorName, appointment.date, appointment.time];
  values.forEach((value, index) => {
    const cell = document.createElement("td");
    if (index === 0) cell.className = "patient-id";
    cell.textContent = value || "";
    tr.append(cell);
  });
  const action = document.createElement("td");
  const image = document.createElement("img");
  image.src = "../assets/images/edit/edit.png";
  image.alt = "action";
  image.className = "prescription-btn";
  image.addEventListener("click", () => {
    window.location.href = `addPrescription.html?id=${encodeURIComponent(appointment.id ?? "")}`;
  });
  action.append(image);
  tr.append(action);

  return tr;
}
