import { API_BASE_URL } from "../config/config.js";
const DOCTOR_API=`${API_BASE_URL}/doctor`;
async function parse(response){const body=await response.json();if(!response.ok)throw new Error(body.message||"Request failed");return body;}
export async function getDoctors(){try{return (await parse(await fetch(DOCTOR_API))).doctors||[];}catch(error){console.error(error);return [];}}
export async function deleteDoctor(id,token){try{return {success:true,message:(await parse(await fetch(`${DOCTOR_API}/${id}/${token}`,{method:"DELETE"}))).message};}catch(error){return {success:false,message:error.message};}}
export async function saveDoctor(doctor,token){try{return {success:true,message:(await parse(await fetch(`${DOCTOR_API}/${token}`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(doctor)}))).message};}catch(error){return {success:false,message:error.message};}}
export async function filterDoctors(name="all",time="all",specialty="all"){try{return await parse(await fetch(`${DOCTOR_API}/filter/${encodeURIComponent(name||"all")}/${encodeURIComponent(time||"all")}/${encodeURIComponent(specialty||"all")}`));}catch(error){console.error(error);return {doctors:[]};}}
