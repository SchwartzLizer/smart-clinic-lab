import { bangkokDate } from "./clinicDate.js";
import { getAllAppointments } from "./services/appointmentRecordService.js";

const body = document.getElementById("patientTableBody");
const empty = document.getElementById("emptyState");
const search = document.getElementById("searchBar");
const picker = document.getElementById("datePicker");

picker.value = bangkokDate();

function cell(value) {
  const element = document.createElement("td");
  element.textContent = value ?? "";
  return element;
}

function appointmentRow(appointment) {
  const row = document.createElement("tr");
  row.append(
    cell(appointment.patient?.id),
    cell(appointment.patient?.name),
    cell(appointment.appointmentTime),
    cell(appointment.status === 0 ? "Scheduled" : "Completed"),
  );
  const action = document.createElement("td");
  const button = document.createElement("button");
  button.type = "button";
  button.className = "primary-btn";
  button.textContent = "Add";
  button.addEventListener("click", () => {
    const query = new URLSearchParams({
      appointmentId: String(appointment.id ?? ""),
      patientName: appointment.patient?.name ?? "",
    });
    window.location.href = `/pages/addPrescription.html?${query}`;
  });
  action.append(button);
  row.append(action);
  return row;
}

async function load() {
  const data = await getAllAppointments(picker.value, search.value || "all");
  const rows = data.appointments || [];
  body.replaceChildren(...rows.map(appointmentRow));
  empty.hidden = rows.length > 0;
}

document.getElementById("todayButton").addEventListener("click", () => {
  picker.value = bangkokDate();
  load();
});
picker.addEventListener("change", load);
search.addEventListener("input", load);
load();
