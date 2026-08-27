package com.project.back_end.mvc;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.project.back_end.services.Service;
@Controller
public class DashboardController {
    private final Service service;
    public DashboardController(Service service){this.service=service;}
    @GetMapping("/adminDashboard/{token}") public String adminDashboard(@PathVariable String token){return service.validateToken(token,"admin")==null?"admin/adminDashboard":"redirect:/";}
    @GetMapping("/doctorDashboard/{token}") public String doctorDashboard(@PathVariable String token){return service.validateToken(token,"doctor")==null?"doctor/doctorDashboard":"redirect:/";}
}
