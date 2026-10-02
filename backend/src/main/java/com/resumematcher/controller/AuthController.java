package com.resumematcher.controller;

import com.resumematcher.entity.User;
import com.resumematcher.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handles login and registration pages.
 *
 * @Controller (not @RestController) because we return Thymeleaf view names,
 * not raw JSON.  Spring resolves "login" → templates/login.html.
 */
@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ── Login ──────────────────────────────────────────────────────

    /**
     * Shows the login form.
     * Spring Security handles the POST /login automatically —
     * we only provide the GET to render the page.
     */
    @GetMapping("/login")
    public String showLoginPage() {
        return "login"; // resolves to templates/login.html
    }

    // ── Registration ───────────────────────────────────────────────

    /** Shows the registration form. */
    @GetMapping("/register")
    public String showRegisterPage() {
        return "register"; // resolves to templates/register.html
    }

    /**
     * Processes the registration form submission.
     *
     * Flow:
     *   1. Check if username already exists → show error if so.
     *   2. Hash the raw password with BCrypt before saving.
     *   3. Assign the default role "USER" (not "ROLE_USER" — Spring adds the prefix).
     *   4. Save to MySQL and redirect to the login page.
     */
    @PostMapping("/register")
    public String registerUser(@RequestParam String username,
                               @RequestParam String password,
                               Model model) {

        // Guard: prevent duplicate usernames
        if (userRepository.findByUsername(username).isPresent()) {
            model.addAttribute("error", "Username already taken.");
            return "register"; // re-show the form with the error message
        }

        // Create and save the new user
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password)); // NEVER store plain text
        newUser.setRole("USER"); // default role for self-registered users

        userRepository.save(newUser);

        // Redirect to login so the user can sign in with their new account.
        // "redirect:" sends an HTTP 302 — this avoids form re-submission on refresh.
        return "redirect:/login?registered";
    }
}
