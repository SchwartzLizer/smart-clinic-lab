// patientRecordServices.js
import { getPatientAppointments } from "./services/patientServices.js";
import { createPatientRecordRow } from './components/patientRecordRow.js';

const tableBody = document.getElementById("patientTableBody");
const urlParams = new URLSearchParams(window.location.search);
const patientId = urlParams.get("id");
const doctorId = urlParams.get("doctorId");

document.addEventListener("DOMContentLoaded", initializePage);

async function initializePage() {
  try {
    const appointmentData = await getPatientAppointments() || [];

    // Filter by both patientId and doctorId
    const filteredAppointments = appointmentData.filter(app =>
      app.doctor?.id == doctorId && app.patient?.id == patientId);
    console.log(filteredAppointments)
    renderAppointments(filteredAppointments);
  } catch (error) {
    console.error("Error loading appointments:", error);
    alert("❌ Failed to load your appointments.");
  }
}

function renderAppointments(appointments) {
  tableBody.replaceChildren();

  if (!appointments.length) {
    const row = document.createElement("tr");
    const cell = document.createElement("td");
    cell.colSpan = 5;
    cell.className = "empty-table-cell";
    cell.textContent = "No Appointments Found";
    row.append(cell);
    tableBody.appendChild(row);
    return;
  }

  appointments.forEach(appointment => {
    const row = createPatientRecordRow(appointment);
    tableBody.appendChild(row);
  });
}
