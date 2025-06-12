package ing.gpps.dto;

import ing.gpps.notificaciones.Notificacion;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class NotificacionDTO {
    private Long id;
    private Long emisorId;
    private String emisorNombre;
    private Long destinatarioId;
    private String destinatarioNombre;
    private String mensaje;
    private String tipo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaLectura;
    private Boolean leida;
    private Boolean importante;

    public static NotificacionDTO fromEntity(Notificacion notificacion) {
        NotificacionDTO dto = new NotificacionDTO();
        dto.setId(notificacion.getId());
        dto.setEmisorId(Long.valueOf (notificacion.getEmisor() != null ? notificacion.getEmisor().getId() : null));
        dto.setEmisorNombre(notificacion.getEmisor() != null ? notificacion.getEmisor().getNombre() : null);
        dto.setDestinatarioId(Long.valueOf (notificacion.getDestinatario() != null ? notificacion.getDestinatario().getId() : null));
        dto.setDestinatarioNombre(notificacion.getDestinatario() != null ? notificacion.getDestinatario().getNombre() : null);
        dto.setMensaje(notificacion.getMensaje());
        dto.setTipo(notificacion.getTipo());
        dto.setFechaCreacion(notificacion.getFechaCreacion());
        dto.setFechaLectura(notificacion.getFechaLectura());
        dto.setLeida(notificacion.isLeida());
        dto.setImportante(notificacion.isImportante());
        return dto;
    }
} 