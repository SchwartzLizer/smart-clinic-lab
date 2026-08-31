// loggedPatient.js 
import { getDoctors } from './services/doctorServices.js';
import { createDoctorCard } from './components/doctorCard.js';
import { filterDoctors } from './services/doctorServices.js';
import { bookAppointment } from './services/appointmentRecordService.js';


document.addEventListener("DOMContentLoaded", () => {
  loadDoctorCards();
});

function loadDoctorCards() {
  getDoctors()
    .then(doctors => {
      const contentDiv = document.getElementById("content");
      contentDiv.replaceChildren();

      doctors.forEach(doctor => {
        const card = createDoctorCard(doctor);
        contentDiv.appendChild(card);
      });
    })
    .catch(error => {
      console.error("Failed to load doctors:", error);
    });
}

export function showBookingOverlay(e, doctor, patient) {
  const button = e.target;
  const rect = button.getBoundingClientRect();
  console.log(patient.name)
  console.log(patient)
  const ripple = document.createElement("div");
  ripple.classList.add("ripple-overlay");
  document.body.appendChild(ripple);

  setTimeout(() => ripple.classList.add("active"), 50);

  const modalApp = document.createElement("div");
  modalApp.classList.add("modalApp");

  const heading = document.createElement("h2");
  heading.textContent = "Book Appointment";
  const details = [patient.name, doctor.name, doctor.specialty, doctor.email].map((value) => {
    const input = document.createElement("input");
    input.className = "input-field";
    input.type = "text";
    input.value = value || "";
    input.disabled = true;
    return input;
  });
  const dateInput = document.createElement("input");
  dateInput.className = "input-field";
  dateInput.type = "date";
  dateInput.id = "appointment-date";
  const timeSelect = document.createElement("select");
  timeSelect.className = "input-field";
  timeSelect.id = "appointment-time";
  const placeholder = document.createElement("option");
  placeholder.value = "";
  placeholder.textContent = "Select time";
  timeSelect.append(placeholder);
  for (const time of doctor.availableTimes || []) {
    const option = document.createElement("option");
    option.value = time;
    option.textContent = time;
    timeSelect.append(option);
  }
  const confirm = document.createElement("button");
  confirm.type = "button";
  confirm.className = "confirm-booking";
  confirm.textContent = "Confirm Booking";
  modalApp.append(heading, ...details, dateInput, timeSelect, confirm);

  document.body.appendChild(modalApp);

  setTimeout(() => modalApp.classList.add("active"), 600);

  confirm.addEventListener("click", async () => {
    const date = modalApp.querySelector("#appointment-date").value;
    const time = modalApp.querySelector("#appointment-time").value;
    const startTime = time.split('-')[0];
    const appointment = {
      doctor: { id: doctor.id },
      patient: { id: patient.id },
      appointmentTime: `${date}T${startTime}:00`,
      status: 0
    };


    const { success, message } = await bookAppointment(appointment);

    if (success) {
      alert("Appointment Booked successfully");
      ripple.remove();
      modalApp.remove();
    } else {
      alert("❌ Failed to book an appointment :: " + message);
    }
  });
}



// Filter Input
document.getElementById("searchBar").addEventListener("input", filterDoctorsOnChange);
document.getElementById("filterTime").addEventListener("change", filterDoctorsOnChange);
document.getElementById("filterSpecialty").addEventListener("change", filterDoctorsOnChange);



function filterDoctorsOnChange() {
  const searchBar = document.getElementById("searchBar").value.trim();
  const filterTime = document.getElementById("filterTime").value;
  const filterSpecialty = document.getElementById("filterSpecialty").value;


  const name = searchBar.length > 0 ? searchBar : null;
  const time = filterTime.length > 0 ? filterTime : null;
  const specialty = filterSpecialty.length > 0 ? filterSpecialty : null;

  filterDoctors(name, time, specialty)
    .then(response => {
      const doctors = response.doctors;
      const contentDiv = document.getElementById("content");
      contentDiv.replaceChildren();

      if (doctors.length > 0) {
        console.log(doctors);
        doctors.forEach(doctor => {
          const card = createDoctorCard(doctor);
          contentDiv.appendChild(card);
        });
      } else {
        const empty = document.createElement("p");
        empty.textContent = "No doctors found with the given filters.";
        contentDiv.replaceChildren(empty);
        console.log("Nothing");
      }
    })
    .catch(error => {
      console.error("Failed to filter doctors:", error);
      alert("❌ An error occurred while filtering doctors.");
    });
}

export function renderDoctorCards(doctors) {
  const contentDiv = document.getElementById("content");
  contentDiv.replaceChildren();

  doctors.forEach(doctor => {
    const card = createDoctorCard(doctor);
    contentDiv.appendChild(card);
  });

}
