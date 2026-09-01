import { deleteDoctor } from "../services/doctorServices.js";
export function createDoctorCard(doctor,onDeleted=()=>{}){
  const card=document.createElement("article");card.className="doctor-card";
  const avatar=document.createElement("div");avatar.className="doctor-avatar";avatar.textContent=(doctor.name||"?").charAt(0);
  const details=document.createElement("div");const specialty=document.createElement("p");specialty.className="specialty";specialty.textContent=doctor.specialty||"";
  const name=document.createElement("h3");name.textContent=doctor.name||"";const email=document.createElement("p");email.textContent=doctor.email||"";const phone=document.createElement("p");phone.textContent=doctor.phone||"";
  const slots=document.createElement("div");slots.className="slot-list";const available=doctor.availableTimes||[];for(const slot of available.length?available:["No slots listed"]){const item=document.createElement("span");item.textContent=slot;slots.append(item);}details.append(specialty,name,email,phone,slots);
  const role=sessionStorage.getItem("userRole");const actions=document.createElement("div");actions.className="card-actions";
  if(role==="admin"){const button=document.createElement("button");button.type="button";button.textContent="Delete";button.className="danger-btn";button.addEventListener("click",async()=>{const result=await deleteDoctor(doctor.id);alert(result.message);if(result.success){card.remove();onDeleted();}});actions.append(button);}
  else{const button=document.createElement("button");button.type="button";button.textContent="Book now";button.className="primary-btn";button.addEventListener("click",()=>{if(role==="loggedPatient"){window.dispatchEvent(new CustomEvent("doctor:selected",{detail:doctor}));return;}alert("Please log in as a patient to book.");});actions.append(button);}
  card.append(avatar,details,actions);
  return card;
}
