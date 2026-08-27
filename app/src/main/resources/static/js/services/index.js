import { openModal } from "../components/modals.js";
import { API_BASE_URL } from "../config/config.js";

document.getElementById("adminLogin")?.addEventListener("click",()=>openModal("adminLogin"));
document.getElementById("doctorLogin")?.addEventListener("click",()=>openModal("doctorLogin"));
document.getElementById("patientLogin")?.addEventListener("click",()=>{localStorage.setItem("userRole","patient");window.location.href="/pages/patientDashboard.html";});

window.adminLoginHandler=async()=>{
  const response=await fetch(`${API_BASE_URL}/admin`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({username:document.getElementById("username").value,password:document.getElementById("password").value})});
  const data=await response.json();if(!response.ok)return alert(data.message||"Login failed");
  localStorage.setItem("token",data.token);localStorage.setItem("userRole","admin");window.location.href=`/adminDashboard/${data.token}`;
};
window.doctorLoginHandler=async()=>{
  const response=await fetch(`${API_BASE_URL}/doctor/login`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({identifier:document.getElementById("email").value,password:document.getElementById("password").value})});
  const data=await response.json();if(!response.ok)return alert(data.message||"Login failed");
  localStorage.setItem("token",data.token);localStorage.setItem("doctorId",data.id);localStorage.setItem("userRole","doctor");window.location.href=`/doctorDashboard/${data.token}`;
};
