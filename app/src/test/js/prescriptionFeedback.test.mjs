import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const {
  buildPrescription,
  reportPrescriptionLoadError,
  submitPrescription,
} = await import("../../main/resources/static/js/addPrescription.js?test");

function field(value) {
  return { value };
}

function statusNode() {
  return { dataset: {}, hidden: true, textContent: "" };
}

test("buildPrescription keeps appointment context and entered form values", () => {
  assert.deepEqual(buildPrescription({
    patientName: field("Jane Doe"),
    medicines: field("Paracetamol"),
    dosage: field("Twice daily"),
    notes: field("After meals"),
    appointmentId: "31",
  }), {
    patientName: "Jane Doe",
    medication: "Paracetamol",
    dosage: "Twice daily",
    doctorNotes: "After meals",
    appointmentId: "31",
  });
});

test("submitPrescription exposes stable visible success feedback and prevents duplicate submit", async () => {
  const status = statusNode();
  const submitButton = { disabled: false, textContent: "Add Prescription" };

  const saved = await submitPrescription({
    prescription: { appointmentId: "31" },
    save: async () => ({ success: true, message: "Prescription saved" }),
    status,
    submitButton,
  });

  assert.equal(saved, true);
  assert.equal(status.hidden, false);
  assert.equal(status.dataset.state, "success");
  assert.equal(status.textContent, "Prescription saved successfully.");
  assert.equal(submitButton.disabled, true);
  assert.equal(submitButton.textContent, "Prescription saved");
});

test("submitPrescription makes failures visible and allows retry", async () => {
  const status = statusNode();
  const submitButton = { disabled: false, textContent: "Add Prescription" };

  const saved = await submitPrescription({
    prescription: { appointmentId: "31" },
    save: async () => ({ success: false, message: "Validation failed" }),
    status,
    submitButton,
  });

  assert.equal(saved, false);
  assert.equal(status.dataset.state, "error");
  assert.equal(status.textContent, "Failed to save prescription. Validation failed");
  assert.equal(submitButton.disabled, false);
});

test("prescription-load failures produce a bounded diagnostic", async () => {
  const originalWarn = console.warn;
  const warnings = [];
  console.warn = (...args) => warnings.push(args);

  try {
    await Promise.reject(new Error("sensitive response body must not be logged"))
      .catch(() => reportPrescriptionLoadError());
  } finally {
    console.warn = originalWarn;
  }

  assert.deepEqual(warnings, [["Unable to load existing prescription."]]);
});

test("public patient portal does not activate the role-only redirect guard", async () => {
  const source = await readFile(
    new URL("../../main/resources/static/pages/patientDashboard.html", import.meta.url),
    "utf8",
  );

  assert.doesNotMatch(source, /<body\s+data-require-role="true">/);
});

test("guest patient portal keeps a UI login path and authenticated booking feedback", async () => {
  const [headerSource, loggedPatientSource, doctorCardSource] = await Promise.all([
    readFile(new URL("../../main/resources/static/js/components/header.js", import.meta.url), "utf8"),
    readFile(new URL("../../main/resources/static/js/loggedPatient.js", import.meta.url), "utf8"),
    readFile(new URL("../../main/resources/static/js/components/doctorCard.js", import.meta.url), "utf8"),
  ]);

  assert.match(headerSource, /role==="patient"[\s\S]*login\.id="patientLogin"/);
  assert.match(loggedPatientSource, /Appointment booked successfully\./);
  assert.match(doctorCardSource, /new CustomEvent\("doctor:selected"/);
  assert.match(loggedPatientSource, /window\.addEventListener\("doctor:selected"/);
  assert.doesNotMatch(loggedPatientSource, /resumeSelectedDoctor/);
  assert.doesNotMatch(loggedPatientSource, /selectedDoctor/);
});

test("appointment modal uses a flat translucent backdrop instead of an oversized ripple", async () => {
  const source = await readFile(
    new URL("../../main/resources/static/assets/css/patientDashboard.css", import.meta.url),
    "utf8",
  );

  assert.match(source, /\.ripple-overlay\s*\{[^}]*inset:\s*0;[^}]*background-color:\s*rgba\(/s);
  assert.match(source, /\.ripple-overlay\.active\s*\{[^}]*opacity:\s*1;/s);
  assert.doesNotMatch(source, /scale\(150\)/);
});

test("prescription page uses one native form submit path with live feedback", async () => {
  const source = await readFile(
    new URL("../../main/resources/static/pages/addPrescription.html", import.meta.url),
    "utf8",
  );

  assert.match(source, /<form id="prescriptionForm">/);
  assert.match(source, /id="prescriptionStatus"[^>]*role="status"[^>]*aria-live="polite"/);
});
