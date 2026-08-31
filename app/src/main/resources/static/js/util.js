// util.js
  function setRole(role) {
    sessionStorage.setItem("userRole", role);
  }
  
  function getRole() {
    return sessionStorage.getItem("userRole");
  }
  
  function clearRole() {
    sessionStorage.removeItem("userRole");
  }
  
