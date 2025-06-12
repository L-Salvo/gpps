package ing.gpps.entity.users;

//imports de notificaciones
import ing.gpps.notificaciones.NotificacionesService;
import ing.gpps.notificaciones.Notificar;
import ing.gpps.service.EmailService;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import ing.gpps.entity.institucional.Entidad;
import ing.gpps.entity.institucional.Proyecto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Entity
@NoArgsConstructor
@Getter
@Setter
@DiscriminatorValue("TUTOR_EXTERNO")
public class TutorExterno extends Usuario implements Notificar {
    //servicios inyectados para notificaciones
    @Transient
    @Autowired
    private NotificacionesService notificacionesService;

    @Transient
    @Autowired
    private EmailService emailService;

    @ManyToOne
    @JoinColumn(name = "cuit_entidad", referencedColumnName = "cuit")
    private Entidad entidad;

    @OneToMany(mappedBy = "tutorExterno")
    private List<Proyecto> proyectos;

    public Long getTelefono() {
        return super.getNumTelefono();
    }

    public TutorExterno(String nombre, String apellido, String email, String password, Long numTelefono) {
        super(nombre, apellido, email, password, numTelefono);
    }

    @Override
    public String getRol() {
        return "TUTOR_EXTERNO";
    }

    @Override
    public void notificarAutomatico (String mensaje) {
        // Procesar la notificación por email
        procesarNotificacion(mensaje);
    }
    private void procesarNotificacion (String mensaje) {
        try {
            String destinatario = this.getEmail ();
            String asunto = "Notificación para el Tutor " + this.getNombre ();
            String cuerpo = mensaje;
            emailService.enviarCorreo (destinatario, asunto, cuerpo);
            System.out.println ("Procesando notificación: " + mensaje);
        } catch (Exception e) {
            System.err.println ("Error al enviar la notificación por correoal estudiante " + this.getNombre () + e.getMessage ());
        }
    }



}
