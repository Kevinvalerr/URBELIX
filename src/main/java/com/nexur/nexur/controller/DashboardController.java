package com.nexur.nexur.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String mostrarDashboard(Model model) {
        model.addAttribute("titulo", "Dashboard");
        model.addAttribute("currentPath", "/dashboard");
        return "dashboard/dashboard";
    }
    
}
