import { API_BASE_URL } from "../config/config.js";
import { apiFetch, readJson } from "./httpClient.js";

const PATIENTS_API = `${API_BASE_URL}/patients`;

export async function patientSignup(data) {
  try {
    const result = await readJson(PATIENTS_API, { method: "POST", auth: false, body: JSON.stringify(data) });
    return { success: true, message: result.message || "Signup successful" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function patientLogin(data) {
  return apiFetch(`${API_BASE_URL}/auth/patients/login`, {
    method: "POST",
    auth: false,
    body: JSON.stringify(data),
  });
}

export async function getPatientData() {
  return readJson(`${PATIENTS_API}/me`);
}

export async function getPatientAppointments() {
  const appointments = await readJson(`${API_BASE_URL}/appointments`);
  return appointments ?? [];
}

export async function filterAppointments(condition, name) {
  const params = new URLSearchParams();
  if (condition && condition !== "allAppointments" && condition !== "all") params.set("status", condition);
  if (name) params.set("patientName", name);
  try {
    return { appointments: await readJson(`${API_BASE_URL}/appointments?${params}`) };
  } catch (error) {
    console.error(error);
    return { appointments: [] };
  }
}
