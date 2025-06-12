package ing.gpps.service;

import ing.gpps.dto.NotificacionDTO;
import ing.gpps.entity.users.Usuario;
import ing.gpps.notificaciones.Notificacion;
import ing.gpps.repository.NotificacionRepository;
import ing.gpps.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificacionVisualService {

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    //Crear una nueva notificación
    public NotificacionDTO crearNotificacion(Usuario emisor, Usuario destinatario, String mensaje, String tipo) {
        Notificacion notificacion = new Notificacion();
        notificacion.setEmisor(emisor);
        notificacion.setDestinatario(destinatario);
        notificacion.setUsuario(destinatario);
        notificacion.setMensaje(mensaje);
        notificacion.setTipo(tipo);
        notificacion.setFechaCreacion(LocalDateTime.now());
        notificacion.setLeida(false);

        return NotificacionDTO.fromEntity(notificacionRepository.save(notificacion));
    }

    //Obtener todas las notificaciones de un usuario ordenadas por fecha
    public List<NotificacionDTO> getNotificacionesPorUsuario(Long usuarioId) {
        return getNotificacionesPorUsuario(usuarioId, 50); // Limitar a 50 por defecto
    }

    // Obtener notificaciones de un usuario con límite
    public List<NotificacionDTO> getNotificacionesPorUsuario(Long usuarioId, int limite) {
        Pageable pageable = PageRequest.of(0, limite, Sort.by("fechaCreacion").descending());
        return notificacionRepository.findByDestinatarioIdOrderByFechaCreacionDesc(usuarioId, pageable)
                .stream()
                .map(NotificacionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    //Obtener solo las notificaciones no leídas de un usuario
    public List<NotificacionDTO> getNotificacionesNoLeidas(Long usuarioId) {
        return notificacionRepository.findByDestinatarioIdAndLeidaFalseOrderByFechaCreacionDesc(usuarioId)
                .stream()
                .map(NotificacionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    //Contar notificaciones no leídas
    public long contarNotificacionesNoLeidas(Long usuarioId) {
        return notificacionRepository.countByDestinatarioIdAndLeidaFalse(usuarioId);
    }

    //Marcar una notificación como leída
    public boolean marcarComoLeida(Long notificacionId, Long usuarioId) {
        try {
            Optional<Notificacion> notificacionOpt = notificacionRepository.findById(notificacionId);
            if (notificacionOpt.isPresent()) {
                Notificacion notificacion = notificacionOpt.get();
                // Verificar que la notificación pertenece al usuario
                if (notificacion.getDestinatario().getId().equals(usuarioId)) {
                    notificacion.setLeida(true);
                    notificacion.setFechaLectura(LocalDateTime.now());
                    notificacionRepository.save(notificacion);
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Error al marcar notificación como leída: " + e.getMessage());
        }
        return false;
    }

    //Marcar todas las notificaciones de un usuario como leídas
    public int marcarTodasComoLeidas(Long usuarioId) {
        try {
            List<Notificacion> noLeidas = notificacionRepository.findByDestinatarioIdAndLeidaFalseOrderByFechaCreacionDesc(usuarioId);
            int contador = 0;
            LocalDateTime ahora = LocalDateTime.now();

            for (Notificacion notificacion : noLeidas) {
                notificacion.setLeida(true);
                notificacion.setFechaLectura(ahora);
                notificacionRepository.save(notificacion);
                contador++;
            }

            return contador;
        } catch (Exception e) {
            System.err.println("Error al marcar todas las notificaciones como leídas: " + e.getMessage());
            return 0;
        }
    }

    //Eliminar una notificación (solo si pertenece al usuario)
    public boolean eliminarNotificacion(Long notificacionId, Long usuarioId) {
        try {
            Optional<Notificacion> notificacionOpt = notificacionRepository.findById(notificacionId);
            if (notificacionOpt.isPresent()) {
                Notificacion notificacion = notificacionOpt.get();
                // Verificar que la notificación pertenece al usuario
                if (notificacion.getDestinatario().getId().equals(usuarioId)) {
                    notificacionRepository.delete(notificacion);
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Error al eliminar notificación: " + e.getMessage());
        }
        return false;
    }

    //Eliminar notificaciones antiguas (más de X días)
    public int limpiarNotificacionesAntiguas(int diasAntiguedad) {
        try {
            LocalDateTime fechaLimite = LocalDateTime.now().minusDays(diasAntiguedad);
            List<Notificacion> antiguas = notificacionRepository.findByFechaCreacionBefore(fechaLimite);
            int contador = antiguas.size();
            notificacionRepository.deleteAll(antiguas);
            return contador;
        } catch (Exception e) {
            System.err.println("Error al limpiar notificaciones antiguas: " + e.getMessage());
            return 0;
        }
    }

    //Obtener usuario por email (para el controlador)
    public Usuario getUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    //Crear notificación para múltiples usuarios
    public void crearNotificacionMultiple(Usuario emisor, List<Usuario> destinatarios, String mensaje, String tipo) {
        for (Usuario destinatario : destinatarios) {
            crearNotificacion(emisor, destinatario, mensaje, tipo);
        }
    }

    //Obtener estadísticas de notificaciones de un usuario
    public NotificacionStats getEstadisticasUsuario(Long usuarioId) {
        long total = notificacionRepository.countByDestinatarioId(usuarioId);
        long noLeidas = contarNotificacionesNoLeidas(usuarioId);
        long leidas = total - noLeidas;

        return new NotificacionStats(total, leidas, noLeidas);
    }

    // Clase interna para estadísticas
    public static class NotificacionStats {
        private final long total;
        private final long leidas;
        private final long noLeidas;

        public NotificacionStats(long total, long leidas, long noLeidas) {
            this.total = total;
            this.leidas = leidas;
            this.noLeidas = noLeidas;
        }

        public long getTotal() { return total; }
        public long getLeidas() { return leidas; }
        public long getNoLeidas() { return noLeidas; }
    }
}
