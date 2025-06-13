package ing.gpps.controller;

import ing.gpps.entity.users.*;
import ing.gpps.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @GetMapping("/login")
    public String login() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
                if (userDetails.getUsuario() instanceof Estudiante) {
                    logger.info("Redirigiendo estudiante a su dashboard");
                    return "redirect:/estudiante/dashboard";
                } else if (userDetails.getUsuario() instanceof DocenteSupervisor) {
                    DocenteSupervisor tutor = (DocenteSupervisor) userDetails.getUsuario();
                    if (tutor == null || tutor.getId() == null) {
                        logger.error("Error: Docente supervisor no encontrado o ID nulo");
                        return "redirect:/error";
                    }
                    logger.info("Redirigiendo docente supervisor a su dashboard");
                    return "redirect:/docente-supervisor/dashboard";
                } else if (userDetails.getUsuario() instanceof TutorExterno) {
                    logger.info("Redirigiendo tutor externo a su dashboard");
                    return "redirect:/tutor-externo/dashboard";
                } else if (userDetails.getUsuario() instanceof Admin) {
                    logger.info("Redirigiendo admin a su dashboard");
                    return "redirect:/admin/dashboard";
                } else if (userDetails.getUsuario() instanceof AdminEntidad) {
                    logger.info("Redirigiendo admin-entidad a su dashboard");
                    return "redirect:/admin-entidad/dashboard";
                }
            }
            return "login";
        } catch (Exception e) {
            logger.error("Error en el proceso de login: {}", e.getMessage());
            return "login";
        }
    }

    @GetMapping("/")
    public String index() {
        return login();
    }
}
