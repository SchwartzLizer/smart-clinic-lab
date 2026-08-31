// patientAppointment.js
import { getPatientAppointments, getPatientData, filterAppointments } from "./services/patientServices.js";

const tableBody = document.getElementById("patientTableBody");
let allAppointments = [];
let filteredAppointments = [];
let patientId = null;

document.addEventListener("DOMContentLoaded", initializePage);

async function initializePage() {
  try {
    const patient = await getPatientData();
    if (!patient) throw new Error("Failed to fetch patient details");

    patientId = Number(patient.id);

    const appointmentData = await getPatientAppointments() || [];
    allAppointments = appointmentData;

    renderAppointments(allAppointments);
  } catch (error) {
    console.error("Error loading appointments:", error);
    alert("❌ Failed to load your appointments.");
  }
}

export function splitAppointmentTime(appointmentTime) {
  const [appointmentDate, appointmentTimeOnly] = appointmentTime.split("T");
  return { appointmentDate, appointmentTimeOnly };
}

export function renderAppointments(appointments) {
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
    const { appointmentDate, appointmentTimeOnly } = splitAppointmentTime(appointment.appointmentTime);
    const tr = document.createElement("tr");
    [appointment.patient?.name || "You", appointment.doctor?.name || "", appointmentDate, appointmentTimeOnly]
      .forEach((value) => {
        const cell = document.createElement("td");
        cell.textContent = value;
        tr.append(cell);
      });
    const action = document.createElement("td");

    if (appointment.status == 0) {
      const actionBtn = document.createElement("img");
      actionBtn.src = "../assets/images/edit/edit.png";
      actionBtn.alt = "Edit";
      actionBtn.className = "prescription-btn";
      actionBtn.addEventListener("click", () => redirectToUpdatePage(appointment));
      action.append(actionBtn);
    } else {
      action.textContent = "-";
    }
    tr.append(action);
    tableBody.appendChild(tr);
  });
}

export function buildUpdateAppointmentQuery(appointment) {
  const { appointmentDate, appointmentTimeOnly } = splitAppointmentTime(appointment.appointmentTime);
  return new URLSearchParams({
    appointmentId: appointment.id,
    patientId: appointment.patient?.id,
    patientName: appointment.patient?.name || "You",
    doctorName: appointment.doctor?.name || "",
    doctorId: appointment.doctor?.id,
    appointmentDate,
    appointmentTime: appointmentTimeOnly,
  }).toString();
}

function redirectToUpdatePage(appointment) {
  const queryString = buildUpdateAppointmentQuery(appointment);
  // Redirect to the update page with the query string
  setTimeout(() => {
    window.location.href = `/pages/updateAppointment.html?${queryString}`;
  }, 100);
}


// Search and Filter Listeners
document.getElementById("searchBar").addEventListener("input", handleFilterChange);
document.getElementById("appointmentFilter").addEventListener("change", handleFilterChange);

async function handleFilterChange() {
  const searchBarValue = document.getElementById("searchBar").value.trim();
  const filterValue = document.getElementById("appointmentFilter").value;

  const name = searchBarValue || null;
  const condition = filterValue === "allAppointments" ? null : filterValue || null;

  try {
    const response = await filterAppointments(condition, name);
    const appointments = response?.appointments || [];
    filteredAppointments = appointments;

    renderAppointments(filteredAppointments);
  } catch (error) {
    console.error("Failed to filter appointments:", error);
    alert("❌ An error occurred while filtering appointments.");
  }
}

