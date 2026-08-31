import test from "node:test";
import assert from "node:assert/strict";

const tableBody = {
  children: [],
  innerHTML: "",
  appendChild(row) {
    this.children.push(row);
  },
};
const actionHeader = { style: {} };
const searchBar = { addEventListener() {}, value: "" };
const appointmentFilter = { addEventListener() {}, value: "allAppointments" };
const elements = new Map([
  ["patientTableBody", tableBody],
  ["searchBar", searchBar],
  ["appointmentFilter", appointmentFilter],
]);

globalThis.document = {
  addEventListener() {},
  createElement() {
    return {
      innerHTML: "",
      querySelector() {
        return { addEventListener() {} };
      },
    };
  },
  getElementById(id) {
    return elements.get(id);
  },
  querySelector() {
    return actionHeader;
  },
};
globalThis.window = { location: { href: "" } };

const {
  buildUpdateAppointmentQuery,
  renderAppointments,
  splitAppointmentTime,
} = await import("../../main/resources/static/js/patientAppointment.js?test");

const appointment = {
  id: 50,
  doctor: { id: 7, name: "Dr. One", specialty: "Cardiology" },
  patient: { id: 10, name: "Patient One" },
  appointmentTime: "2030-01-10T09:00:00",
  status: 0,
};

test("renders AppointmentResponse time and builds update query from contract fields", () => {
  tableBody.children = [];
  tableBody.innerHTML = "";

  renderAppointments([appointment]);

  assert.equal(tableBody.children.length, 1);
  const renderedRow = tableBody.children[0].innerHTML;
  assert.match(renderedRow, /<td>2030-01-10<\/td>/);
  assert.match(renderedRow, /<td>09:00:00<\/td>/);
  assert.doesNotMatch(renderedRow, /undefined/);

  assert.deepEqual(splitAppointmentTime(appointment.appointmentTime), {
    appointmentDate: "2030-01-10",
    appointmentTimeOnly: "09:00:00",
  });

  const query = new URLSearchParams(buildUpdateAppointmentQuery(appointment));
  assert.equal(query.get("appointmentId"), "50");
  assert.equal(query.get("patientId"), "10");
  assert.equal(query.get("doctorId"), "7");
  assert.equal(query.get("appointmentDate"), "2030-01-10");
  assert.equal(query.get("appointmentTime"), "09:00:00");
});
