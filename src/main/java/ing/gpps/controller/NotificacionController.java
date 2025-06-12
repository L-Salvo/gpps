package ing.gpps.controller;

import ing.gpps.dto.NotificacionDTO;
import ing.gpps.entity.users.Usuario;
import ing.gpps.notificaciones.Notificacion;

import ing.gpps.service.NotificacionVisualService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    @Autowired
    private NotificacionVisualService notificacionService;

    //Obtener todas las notificaciones del usuario actual
    @GetMapping("/usuario")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getNotificacionesUsuario(){
        try {
            Usuario usuario = getUsuarioActual();

            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Usuario no autenticado o no encontrado."));
            }

            List<NotificacionDTO> notificaciones = notificacionService.getNotificacionesPorUsuario(Long.valueOf(usuario.getId()));
            long noLeidas = notificacionService.contarNotificacionesNoLeidas(Long.valueOf(usuario.getId()));

            Map<String, Object> response = new HashMap<>();
            response.put("notificaciones", notificaciones);
            response.put("noLeidas", noLeidas);
            response.put("total", notificaciones.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log the exception for debugging purposes
            System.err.println("Error en getNotificacionesUsuario: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("message", "Error interno del servidor al obtener notificaciones."));
        }
    }
    //Marcar una notificación como leída
    @PostMapping("/{id}/marcar-leida")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> marcarComoLeida(@PathVariable Long id) {
        try {
            Usuario usuario = getUsuarioActual();
            if (usuario == null) {
                return ResponseEntity.badRequest().build();
            }

            boolean marcada = notificacionService.marcarComoLeida(id, Long.valueOf (usuario.getId()));

            Map<String, Object> response = new HashMap<>();
            response.put("success", marcada);
            response.put("message", marcada ? "Notificación marcada como leída" : "Error al marcar notificación");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    //Marcar todas las notificaciones como leídas
    @PostMapping("/marcar-todas-leidas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> marcarTodasComoLeidas() {
        try {
            Usuario usuario = getUsuarioActual();
            if (usuario == null) {
                return ResponseEntity.badRequest().build();
            }

            int marcadas = notificacionService.marcarTodasComoLeidas(Long.valueOf (usuario.getId()));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("marcadas", marcadas);
            response.put("message", marcadas + " notificaciones marcadas como leídas");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    //Obtener el conteo de notificaciones no leídas
    @GetMapping("/no-leidas/count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getConteoNoLeidas() {
        try {
            Usuario usuario = getUsuarioActual();
            if (usuario == null) {
                return ResponseEntity.badRequest().build();
            }

            long noLeidas = notificacionService.contarNotificacionesNoLeidas(Long.valueOf (usuario.getId()));

            Map<String, Object> response = new HashMap<>();
            response.put("count", noLeidas);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    //Eliminar una notificación
    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> eliminarNotificacion(@PathVariable Long id) {
        try {
            Usuario usuario = getUsuarioActual();
            if (usuario == null) {
                return ResponseEntity.badRequest().build();
            }

            boolean eliminada = notificacionService.eliminarNotificacion(id, Long.valueOf (usuario.getId()));

            Map<String, Object> response = new HashMap<>();
            response.put("success", eliminada);
            response.put("message", eliminada ? "Notificación eliminada" : "Error al eliminar notificación");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    //Obtener el usuario actualmente autenticado
    private Usuario getUsuarioActual() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                // Aquí deberías implementar la lógica para obtener el usuario
                // basándote en el sistema de autenticación que estés usando
                // Por ejemplo, si usas Spring Security con UserDetails:
                return notificacionService.getUsuarioPorEmail(authentication.getName());
            }
        } catch (Exception e) {
            System.err.println("Error al obtener usuario actual: " + e.getMessage());
        }
        return null;
    }
}
