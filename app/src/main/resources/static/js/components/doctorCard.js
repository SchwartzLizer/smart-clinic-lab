import { deleteDoctor } from "../services/doctorServices.js";
export function createDoctorCard(doctor,onDeleted=()=>{}){
  const card=document.createElement("article");card.className="doctor-card";
  const slots=(doctor.availableTimes||[]).map(slot=>`<span>${slot}</span>`).join("")||"<span>No slots listed</span>";
  card.innerHTML=`<div class="doctor-avatar">${doctor.name.charAt(0)}</div><div><p class="specialty">${doctor.specialty}</p><h3>${doctor.name}</h3><p>${doctor.email}</p><p>${doctor.phone}</p><div class="slot-list">${slots}</div></div><div class="card-actions"></div>`;
  const role=localStorage.getItem("userRole");const actions=card.querySelector(".card-actions");
  if(role==="admin"){const button=document.createElement("button");button.textContent="Delete";button.className="danger-btn";button.onclick=async()=>{const result=await deleteDoctor(doctor.id);alert(result.message);if(result.success){card.remove();onDeleted();}};actions.append(button);}
  else{const button=document.createElement("button");button.textContent="Book now";button.className="primary-btn";button.onclick=()=>{localStorage.setItem("selectedDoctor",JSON.stringify(doctor));if(role==="loggedPatient")window.location.href="/pages/loggedPatientDashboard.html";else alert("Please log in as a patient to book.");};actions.append(button);}
  return card;
}
