package com.nexur.nexur.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/landing")
    public String landing() {
        return "home";
    }
}