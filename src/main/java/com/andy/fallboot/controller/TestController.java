package com.andy.fallboot.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1")
public class TestController {
    @GetMapping("/public/test")
    public String test() {
        return "Hello this is a public test";
    }

    @GetMapping("/private/test")
    public String test2(@AuthenticationPrincipal Jwt jwt) {
        return String.format("Hello this user is authenticated with id: %s", jwt.getSubject());
    }
}
