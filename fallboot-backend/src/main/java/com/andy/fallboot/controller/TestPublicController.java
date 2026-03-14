package com.andy.fallboot.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("public")
public class TestPublicController {
    @GetMapping("/test")
    public String Login() {
        return "Hello this is public";
    }
}
