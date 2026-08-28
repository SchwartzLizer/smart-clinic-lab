import { API_BASE_URL } from "../config/config.js";
import { readJson } from "./httpClient.js";

const PRESCRIPTIONS_API = `${API_BASE_URL}/prescriptions`;

export async function savePrescription(prescription) {
  try {
    await readJson(PRESCRIPTIONS_API, { method: "POST", body: JSON.stringify(prescription) });
    return { success: true, message: "Prescription saved" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function getPrescription(appointmentId) {
  return readJson(`${PRESCRIPTIONS_API}/${encodeURIComponent(appointmentId)}`);
}
