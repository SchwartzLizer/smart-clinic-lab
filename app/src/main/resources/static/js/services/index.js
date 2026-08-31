import { openModal } from "../components/modals.js";
import { API_BASE_URL } from "../config/config.js";
import { readJson } from "./httpClient.js";

document.getElementById("adminLogin")?.addEventListener("click",()=>openModal("adminLogin"));
document.getElementById("doctorLogin")?.addEventListener("click",()=>openModal("doctorLogin"));
document.getElementById("patientLogin")?.addEventListener("click",()=>{localStorage.setItem("userRole","patient");window.location.href="/pages/patientDashboard.html";});

window.adminLoginHandler=async()=>{
  try {
    const data = await readJson(`${API_BASE_URL}/auth/admin/login`, { method: "POST", auth: false,
      body: JSON.stringify({username:document.getElementById("username").value,password:document.getElementById("password").value}) });
    localStorage.setItem("token",data.token);localStorage.setItem("accountId",data.accountId);localStorage.setItem("userRole","admin");window.location.href="/adminDashboard";
  } catch (error) { alert(error.message || "Login failed"); }
};
window.doctorLoginHandler=async()=>{
  try {
    const data = await readJson(`${API_BASE_URL}/auth/doctors/login`, { method: "POST", auth: false,
      body: JSON.stringify({email:document.getElementById("email").value,password:document.getElementById("password").value}) });
    localStorage.setItem("token",data.token);localStorage.setItem("doctorId",data.accountId);localStorage.setItem("accountId",data.accountId);localStorage.setItem("userRole","doctor");window.location.href="/doctorDashboard";
  } catch (error) { alert(error.message || "Login failed"); }
};
