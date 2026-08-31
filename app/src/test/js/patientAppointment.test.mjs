import test from "node:test";
import assert from "node:assert/strict";

function node(tagName = "div") {
  return {
    tagName,
    children: [],
    textContent: "",
    className: "",
    append(...children) { this.children.push(...children); },
    appendChild(child) { this.children.push(child); },
    replaceChildren(...children) { this.children = children; },
    addEventListener() {},
  };
}

const tableBody = node("tbody");
const searchBar = { addEventListener() {}, value: "" };
const appointmentFilter = { addEventListener() {}, value: "allAppointments" };
const elements = new Map([
  ["patientTableBody", tableBody],
  ["searchBar", searchBar],
  ["appointmentFilter", appointmentFilter],
]);

globalThis.document = {
  addEventListener() {},
  createElement: node,
  getElementById(id) { return elements.get(id); },
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

test("renders AppointmentResponse values as safe text nodes and builds update query", () => {
  renderAppointments([appointment]);

  assert.equal(tableBody.children.length, 1);
  const cells = tableBody.children[0].children;
  assert.equal(cells[2].textContent, "2030-01-10");
  assert.equal(cells[3].textContent, "09:00:00");
  assert.equal(cells.some((cell) => cell.textContent === "undefined"), false);

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
