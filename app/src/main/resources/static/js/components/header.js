function renderHeader(){
  const target=document.getElementById("header");if(!target)return;const role=sessionStorage.getItem("userRole");
  const header=document.createElement("header");header.className="header";
  const link=document.createElement("a");link.className="logo-link";link.href="/";
  const image=document.createElement("img");image.className="logo-img";image.src="/assets/images/logo/logo.png";image.alt="Smart Clinic";
  const title=document.createElement("strong");title.className="logo-title";title.textContent="Smart Clinic";link.append(image,title);
  const nav=document.createElement("nav");const label=document.createElement("span");label.className="role-label";label.textContent=role?role.replace("loggedPatient","Patient"):"Clinic portal";nav.append(label);
  if(role==="patient"){const login=document.createElement("button");login.id="patientLogin";login.className="text-btn";login.type="button";login.textContent="Log in";nav.append(login);}
  else if(role){const logout=document.createElement("button");logout.id="logoutBtn";logout.className="text-btn";logout.type="button";logout.textContent="Log out";logout.addEventListener("click",()=>{sessionStorage.clear();location.href="/";});nav.append(logout);}
  header.append(link,nav);target.replaceChildren(header);
}
document.readyState==="loading"?document.addEventListener("DOMContentLoaded",renderHeader):renderHeader();
