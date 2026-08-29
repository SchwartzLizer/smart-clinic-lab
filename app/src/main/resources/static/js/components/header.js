function renderHeader(){
  const target=document.getElementById("header");if(!target)return;const role=localStorage.getItem("userRole");
  target.innerHTML=`<header class="header"><a class="logo-link" href="/"><img class="logo-img" src="/assets/images/logo/logo.png" alt="Smart Clinic"><strong class="logo-title">Smart Clinic</strong></a><nav><span class="role-label">${role?role.replace("loggedPatient","Patient"):"Clinic portal"}</span>${role?'<button id="logoutBtn" class="text-btn">Log out</button>':""}</nav></header>`;
  document.getElementById("logoutBtn")?.addEventListener("click",()=>{localStorage.clear();location.href="/";});
}
document.readyState==="loading"?document.addEventListener("DOMContentLoaded",renderHeader):renderHeader();
