function element(tag, { className, id, text, type, placeholder, value, name } = {}) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (id) node.id = id;
  if (text !== undefined) node.textContent = text;
  if (type) node.type = type;
  if (placeholder) node.placeholder = placeholder;
  if (value !== undefined) node.value = value;
  if (name) node.name = name;
  return node;
}

function input(id, type, placeholder) {
  return element("input", { id, type, placeholder, className: "input-field" });
}

function button(id, text) {
  return element("button", { id, text, type: "button", className: "dashboard-btn" });
}

function selectSpecialization() {
  const select = element("select", { id: "specialization", className: "input-field select-dropdown" });
  const options = ["", "cardiologist", "dermatologist", "neurologist", "pediatrician", "orthopedic",
    "gynecologist", "psychiatrist", "dentist", "ophthalmologist", "ent", "urologist", "oncologist",
    "gastroenterologist", "general"];
  options.forEach((value) => select.append(element("option", { value, text: value || "Specialization" })));
  return select;
}

function addDoctorForm() {
  const fragment = document.createDocumentFragment();
  fragment.append(
    element("h2", { text: "Add Doctor" }),
    input("doctorName", "text", "Doctor Name"),
    selectSpecialization(),
    input("doctorEmail", "email", "Email"),
    input("doctorPassword", "password", "Password"),
    input("doctorPhone", "text", "Mobile No."),
  );
  const availability = element("div", { className: "availability-container" });
  availability.append(element("label", { className: "availabilityLabel", text: "Select Availability:" }));
  const choices = element("div", { className: "checkbox-group" });
  ["09:00-10:00", "10:00-11:00", "11:00-12:00", "12:00-13:00"].forEach((slot) => {
    const label = element("label");
    const check = element("input", { type: "checkbox", name: "availability", value: slot });
    label.append(check, document.createTextNode(` ${slot}`));
    choices.append(label);
  });
  availability.append(choices);
  fragment.append(availability, button("saveDoctorBtn", "Save"));
  return fragment;
}

function loginForm(type) {
  const fragment = document.createDocumentFragment();
  const isAdmin = type === "adminLogin";
  const isDoctor = type === "doctorLogin";
  fragment.append(
    element("h2", { text: isAdmin ? "Admin Login" : isDoctor ? "Doctor Login" : "Patient Login" }),
    input(isAdmin ? "username" : "email", "text", isAdmin ? "Username" : "Email"),
    input("password", "password", "Password"),
    button(isAdmin ? "adminLoginBtn" : isDoctor ? "doctorLoginBtn" : "loginBtn", "Login"),
  );
  return fragment;
}

function patientSignupForm() {
  const fragment = document.createDocumentFragment();
  fragment.append(
    element("h2", { text: "Patient Signup" }),
    input("name", "text", "Name"),
    input("email", "email", "Email"),
    input("password", "password", "Password"),
    input("phone", "text", "Phone"),
    input("address", "text", "Address"),
    button("signupBtn", "Signup"),
  );
  return fragment;
}

export function openModal(type) {
  const body = document.getElementById("modal-body");
  if (!body) return;
  const form = type === "addDoctor" ? addDoctorForm()
    : type === "patientSignup" ? patientSignupForm() : loginForm(type);
  body.replaceChildren(form);
  document.getElementById("modal").classList.add("is-open");
  document.getElementById("closeModal").addEventListener("click", () => {
    document.getElementById("modal").classList.remove("is-open");
  }, { once: true });
  if (type === "patientSignup") document.getElementById("signupBtn").addEventListener("click", signupPatient);
  if (type === "patientLogin") document.getElementById("loginBtn").addEventListener("click", loginPatient);
  if (type === "addDoctor") document.getElementById("saveDoctorBtn").addEventListener("click", adminAddDoctor);
  if (type === "adminLogin") document.getElementById("adminLoginBtn").addEventListener("click", adminLoginHandler);
  if (type === "doctorLogin") document.getElementById("doctorLoginBtn").addEventListener("click", doctorLoginHandler);
}
