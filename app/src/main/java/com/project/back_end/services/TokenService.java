package com.project.back_end.services;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
@Component
public class TokenService {
    private final AdminRepository admins; private final DoctorRepository doctors; private final PatientRepository patients;
    @Value("${jwt.secret}") private String secret;
    public TokenService(AdminRepository admins,DoctorRepository doctors,PatientRepository patients){this.admins=admins;this.doctors=doctors;this.patients=patients;}
    public SecretKey getSigningKey(){return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));}
    public String generateToken(String email){Date now=new Date();return Jwts.builder().subject(email).issuedAt(now).expiration(new Date(now.getTime()+7L*24*60*60*1000)).signWith(getSigningKey()).compact();}
    public String extractIdentifier(String token){return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload().getSubject();}
    public String extractEmail(String token){return extractIdentifier(token);}
    public boolean validateToken(String token,String user){try{String id=extractIdentifier(token);return switch(user.toLowerCase()){case "admin"->admins.findByUsername(id)!=null;case "doctor"->doctors.findByEmail(id)!=null;case "patient"->patients.findByEmail(id)!=null;default->false;};}catch(Exception ignored){return false;}}
}
