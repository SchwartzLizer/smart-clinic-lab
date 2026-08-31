import { openModal } from "../components/modals.js";
import { API_BASE_URL } from "../config/config.js";
import { readJson } from "./httpClient.js";

document.getElementById("adminLogin")?.addEventListener("click",()=>openModal("adminLogin"));
document.getElementById("doctorLogin")?.addEventListener("click",()=>openModal("doctorLogin"));
document.getElementById("patientLogin")?.addEventListener("click",()=>{sessionStorage.setItem("userRole","patient");window.location.href="/pages/patientDashboard.html";});

window.adminLoginHandler=async()=>{
  try {
    const data = await readJson(`${API_BASE_URL}/auth/admin/login`, { method: "POST", auth: false,
      body: JSON.stringify({username:document.getElementById("username").value,password:document.getElementById("password").value}) });
    sessionStorage.setItem("token",data.token);sessionStorage.setItem("accountId",data.accountId);sessionStorage.setItem("userRole","admin");window.location.href="/adminDashboard";
  } catch (error) { alert(error.message || "Login failed"); }
};
window.doctorLoginHandler=async()=>{
  try {
    const data = await readJson(`${API_BASE_URL}/auth/doctors/login`, { method: "POST", auth: false,
      body: JSON.stringify({email:document.getElementById("email").value,password:document.getElementById("password").value}) });
    sessionStorage.setItem("token",data.token);sessionStorage.setItem("doctorId",data.accountId);sessionStorage.setItem("accountId",data.accountId);sessionStorage.setItem("userRole","doctor");window.location.href="/doctorDashboard";
  } catch (error) { alert(error.message || "Login failed"); }
};
