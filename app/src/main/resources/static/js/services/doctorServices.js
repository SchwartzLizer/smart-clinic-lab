import { API_BASE_URL } from "../config/config.js";
import { readJson } from "./httpClient.js";

const DOCTORS_API = `${API_BASE_URL}/doctors`;

export async function getDoctors() {
  const page = await readJson(`${DOCTORS_API}?size=100`);
  return page.content ?? [];
}

export async function deleteDoctor(id) {
  try {
    await readJson(`${DOCTORS_API}/${encodeURIComponent(id)}`, { method: "DELETE" });
    return { success: true, message: "Doctor deleted successfully" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function saveDoctor(doctor) {
  try {
    await readJson(DOCTORS_API, {
      method: "POST",
      body: JSON.stringify(doctor),
    });
    return { success: true, message: "Doctor added successfully" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function filterDoctors(name = "", period = "", specialty = "") {
  const params = new URLSearchParams({ size: "100" });
  if (name && name !== "all") params.set("name", name);
  if (period && period !== "all") params.set("period", period);
  if (specialty && specialty !== "all") params.set("specialty", specialty);
  try {
    const page = await readJson(`${DOCTORS_API}?${params}`);
    return { doctors: page.content ?? [] };
  } catch (error) {
    console.error(error);
    return { doctors: [] };
  }
}
