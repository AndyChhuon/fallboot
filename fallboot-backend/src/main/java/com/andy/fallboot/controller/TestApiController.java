package com.andy.fallboot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestApiController {
    @GetMapping("/test")
    public String Login() {
        return "Hello this user is authenticated";
    }
}
