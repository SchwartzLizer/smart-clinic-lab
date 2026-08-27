db = db.getSiblingDB("prescriptions");

const prescriptions = [
  { _id: ObjectId("6807dd712725f013281e7201"), patientName: "John Smith", appointmentId: 2, medication: "Paracetamol", dosage: "500mg", doctorNotes: "Take 1 tablet every 6 hours." },
  { _id: ObjectId("6807dd712725f013281e7202"), patientName: "Emily Rose", appointmentId: 3, medication: "Aspirin", dosage: "300mg", doctorNotes: "Take 1 tablet after meals." },
  { _id: ObjectId("6807dd712725f013281e7203"), patientName: "Michael Jordan", appointmentId: 4, medication: "Ibuprofen", dosage: "400mg", doctorNotes: "Take 1 tablet every 8 hours." },
  { _id: ObjectId("6807dd712725f013281e7204"), patientName: "Olivia Moon", appointmentId: 5, medication: "Antihistamine", dosage: "10mg", doctorNotes: "Take 1 tablet daily before bed." },
  { _id: ObjectId("6807dd712725f013281e7205"), patientName: "Liam King", appointmentId: 6, medication: "Vitamin C", dosage: "1000mg", doctorNotes: "Take 1 tablet daily." },
  { _id: ObjectId("6807dd712725f013281e7206"), patientName: "Sophia Lane", appointmentId: 7, medication: "Antibiotics", dosage: "500mg", doctorNotes: "Take 1 tablet every 12 hours." },
  { _id: ObjectId("6807dd712725f013281e7207"), patientName: "Noah Brooks", appointmentId: 8, medication: "Paracetamol", dosage: "500mg", doctorNotes: "Take 1 tablet every 6 hours." },
  { _id: ObjectId("6807dd712725f013281e7208"), patientName: "Ava Daniels", appointmentId: 9, medication: "Ibuprofen", dosage: "200mg", doctorNotes: "Take 1 tablet every 8 hours." },
  { _id: ObjectId("6807dd712725f013281e7209"), patientName: "William Harris", appointmentId: 10, medication: "Aspirin", dosage: "300mg", doctorNotes: "Take 1 tablet after meals." }
];

prescriptions.forEach((prescription) => {
  db.prescriptions.replaceOne(
    { _id: prescription._id },
    { ...prescription, _class: "com.project.back_end.models.Prescription" },
    { upsert: true }
  );
});

db.prescriptions.find().limit(5).pretty();
