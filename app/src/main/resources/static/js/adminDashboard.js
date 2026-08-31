import { getDoctors,filterDoctors,saveDoctor } from "./services/doctorServices.js";
import { createDoctorCard } from "./components/doctorCard.js";
import { openModal } from "./components/modals.js";
const content=document.getElementById("content"),search=document.getElementById("searchBar"),time=document.getElementById("timeFilter"),specialty=document.getElementById("specialtyFilter");
function render(doctors){content.replaceChildren(...doctors.map(d=>createDoctorCard(d)));}
async function load(){render(await getDoctors());}
async function filter(){render((await filterDoctors(search.value||"all",time.value,specialty.value)).doctors||[]);}
search.addEventListener("input",filter);time.addEventListener("change",filter);specialty.addEventListener("change",filter);
document.getElementById("addDoctorBtn").addEventListener("click",()=>openModal("addDoctor"));
window.adminAddDoctor=async()=>{const doctor={name:document.getElementById("doctorName").value,specialty:document.getElementById("specialization").value,email:document.getElementById("doctorEmail").value,password:document.getElementById("doctorPassword").value,phone:document.getElementById("doctorPhone").value,availableTimes:[...document.querySelectorAll('input[name="availability"]:checked')].map(x=>x.value)};const result=await saveDoctor(doctor);alert(result.message);if(result.success){document.getElementById("modal").classList.remove("is-open");load();}};
load();
