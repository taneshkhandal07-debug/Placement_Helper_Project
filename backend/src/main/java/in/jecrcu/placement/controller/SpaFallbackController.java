package in.jecrcu.placement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to forward client-side SPA routes to the unified React application.
 * Routes such as /drives, /erp, /crt, /integration, and /spa are handled by
 * React Router once index.html is loaded in the browser.
 */
@Controller
public class SpaFallbackController {

    @GetMapping({
        "/drives",
        "/drives/**",
        "/erp",
        "/erp/**",
        "/crt",
        "/crt/**",
        "/integration",
        "/integration/**",
        "/spa",
        "/spa/**"
    })
    public String forwardSpaRoutes() {
        return "forward:/index.html";
    }
}
