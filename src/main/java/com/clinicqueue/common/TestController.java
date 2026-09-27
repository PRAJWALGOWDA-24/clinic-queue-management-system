package com.clinicqueue.common;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/test/protected")
    public String protectedRoute(@AuthenticationPrincipal UserDetails userDetails) {
        return "Hello " + userDetails.getUsername() + ", your JWT works!";
    }
}