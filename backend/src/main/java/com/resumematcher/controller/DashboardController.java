package com.resumematcher.controller;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Minimal dashboard controller — the landing page after login.
 * This will grow in later steps when we add resume upload and analysis.
 */
@Controller
public class DashboardController {

    /**
     * Principal is injected by Spring Security automatically.
     * It holds the currently logged-in user's username.
     */
    @GetMapping("/dashboard")
    public String showDashboard(Principal principal, Model model) {
        // Pass the username to the template so we can greet the user
        model.addAttribute("username", principal.getName());
        return "dashboard"; // resolves to templates/dashboard.html
    }
}
