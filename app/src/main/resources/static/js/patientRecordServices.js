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
  tableBody.innerHTML = "";

  const actionTh = document.querySelector("#patientTable thead tr th:last-child");
  if (actionTh) {
    actionTh.style.display = "table-cell"; // Always show "Actions" column
  }

  if (!appointments.length) {
    tableBody.innerHTML = `<tr><td colspan="5" style="text-align:center;">No Appointments Found</td></tr>`;
    return;
  }

  appointments.forEach(appointment => {
    const row = createPatientRecordRow(appointment);
    tableBody.appendChild(row);
  });
}
