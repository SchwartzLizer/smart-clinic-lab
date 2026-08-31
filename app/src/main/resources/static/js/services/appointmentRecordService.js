import { API_BASE_URL } from "../config/config.js";
import { readJson } from "./httpClient.js";

const APPOINTMENTS_API = `${API_BASE_URL}/appointments`;

export async function getAllAppointments(date, patientName) {
  const params = new URLSearchParams();
  if (date) params.set("date", date);
  if (patientName && patientName !== "all") params.set("patientName", patientName);
  return { appointments: await readJson(`${APPOINTMENTS_API}?${params}`) };
}

export async function bookAppointment(appointment) {
  try {
    const body = {
      doctorId: appointment.doctorId ?? appointment.doctor?.id,
      appointmentTime: appointment.appointmentTime,
    };
    await readJson(APPOINTMENTS_API, { method: "POST", body: JSON.stringify(body) });
    return { success: true, message: "Appointment booked" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function updateAppointment(appointment) {
  try {
    await readJson(`${APPOINTMENTS_API}/${encodeURIComponent(appointment.id)}`, {
      method: "PUT",
      body: JSON.stringify({ appointmentTime: appointment.appointmentTime }),
    });
    return { success: true, message: "Appointment updated" };
  } catch (error) {
    return { success: false, message: error.message };
  }
}

export async function cancelAppointment(id) {
  await readJson(`${APPOINTMENTS_API}/${encodeURIComponent(id)}`, { method: "DELETE" });
}
