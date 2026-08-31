function renderFooter(){const target=document.getElementById("footer");if(!target)return;const footer=document.createElement("footer");footer.className="footer";footer.textContent="Portfolio demo — synthetic data only; not for clinical use; no SLA.";target.replaceChildren(footer);}
document.readyState==="loading"?document.addEventListener("DOMContentLoaded",renderFooter):renderFooter();
