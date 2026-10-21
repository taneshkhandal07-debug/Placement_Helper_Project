package in.jecrcu.placement.controller;

import in.jecrcu.placement.config.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API authentication endpoints for the React frontend.
 *
 * These endpoints live under /api/auth/** which is already CSRF-exempt
 * in SecurityConfig. They reuse the existing Spring Security session
 * mechanism — the same HttpSession that Thymeleaf pages rely on.
 *
 * The existing Thymeleaf form login at POST /login is not changed.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthRestController {

    private final AuthenticationManager authenticationManager;

    // ---------------------------------------------------------------
    // GET /api/auth/me
    // Returns the current authenticated user's public profile.
    // Returns 401 if the session has no authenticated principal.
    // ---------------------------------------------------------------
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("UNKNOWN");

        return ResponseEntity.ok(Map.of(
                "id",              userDetails.getId(),
                "email",           userDetails.getEmail(),
                "fullName",        userDetails.getFullName(),
                "role",            role,
                "branch",          userDetails.getBranch()          != null ? userDetails.getBranch()          : "",
                "graduationYear",  userDetails.getGraduationYear()  != null ? userDetails.getGraduationYear()  : 0,
                "emailVerified",   userDetails.isEmailVerified(),
                "isMentor",        userDetails.isMentor()
        ));
    }

    // ---------------------------------------------------------------
    // POST /api/auth/login
    // Accepts JSON { "email": "...", "password": "..." }.
    // On success: creates a Spring Security session and returns user info.
    // On failure: returns 401.
    //
    // CSRF is not required here because /api/** is already excluded
    // from CSRF checking in SecurityConfig.
    // ---------------------------------------------------------------
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> credentials,
            HttpServletRequest request) {

        String email    = credentials != null ? credentials.get("email")    : null;
        String password = credentials != null ? credentials.get("password") : null;

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email and password are required"));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email.trim(), password));

            // Store the authenticated context in the HTTP session so subsequent
            // requests from the same browser session are recognised as authenticated.
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            HttpSession session = request.getSession(true);
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    context);

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String role = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("UNKNOWN");

            log.info("React login successful for: {}", email);

            return ResponseEntity.ok(Map.of(
                    "id",          userDetails.getId(),
                    "email",       userDetails.getEmail(),
                    "fullName",    userDetails.getFullName(),
                    "role",        role,
                    "redirectUrl", resolveRedirectUrl(role)
            ));

        } catch (BadCredentialsException e) {
            log.warn("Failed React login attempt for email: {}", email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        } catch (AuthenticationException e) {
            log.warn("Auth error for {}: {}", email, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }

    // ---------------------------------------------------------------
    // POST /api/auth/logout
    // Clears the Spring Security context and invalidates the session.
    // ---------------------------------------------------------------
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------
    private String resolveRedirectUrl(String role) {
        return switch (role) {
            case "ROLE_ADMIN"   -> "/admin";
            case "ROLE_ALUMNI"  -> "/alumni/dashboard";
            case "ROLE_FACULTY" -> "/erp";
            default             -> "/dashboard";
        };
    }
}
