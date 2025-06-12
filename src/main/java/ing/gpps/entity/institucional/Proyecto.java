package ing.gpps.entity.institucional;

import ing.gpps.entity.Solicitud;
import ing.gpps.entity.idClasses.ProyectoId;
import ing.gpps.entity.users.DocenteSupervisor;
import ing.gpps.entity.users.Estudiante;
import ing.gpps.entity.users.TutorExterno;
import ing.gpps.entity.users.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Proyecto {

    @EmbeddedId
    private ProyectoId proyectoId;

    @ManyToOne
    @MapsId("cuitEntidad")
    @JoinColumn(name = "cuit")
    private Entidad entidad;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne
    @JoinColumn(name = "fk_nombre_area")
    private Area area;

    @OneToOne(mappedBy = "proyecto", cascade = CascadeType.ALL, orphanRemoval = true)
    private PlanDeTrabajo planDeTrabajo;

    @OneToOne
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;

    @ManyToOne
    @JoinColumn(name = "tutor_unrn_id")
    private DocenteSupervisor tutorUNRN;

    @ManyToOne
    @JoinColumn(name = "tutor_externo_id")
    private TutorExterno tutorExterno;

    @OneToMany(mappedBy = "proyecto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Solicitud> solicitudes = new ArrayList<>();

    @ElementCollection
    private List<String> objetivos = new ArrayList<>();

    @Column(name = "progreso")
    private int progreso;

    @Enumerated(EnumType.STRING)
    private EstadoProyecto estado;

    public Proyecto(String titulo, String descripcion, Estudiante estudiante,
                    DocenteSupervisor tutorUNRN, TutorExterno tutorExterno, Entidad entidad) {
        this.descripcion = descripcion;
        this.estudiante = estudiante;
        if (estudiante != null) {
            estudiante.setProyecto(this);
        }
        this.tutorUNRN = tutorUNRN;
        this.tutorExterno = tutorExterno;
        this.entidad = entidad;
        this.progreso = 0;
        this.proyectoId = new ProyectoId(titulo, entidad.getCuit());
        this.estado = EstadoProyecto.EN_CURSO;
    }
    public Proyecto(String titulo, String descripcion,
                    DocenteSupervisor tutorUNRN, TutorExterno tutorExterno, Entidad entidad) {
        this.descripcion = descripcion;
        this.tutorUNRN = tutorUNRN;
        this.tutorExterno = tutorExterno;
        this.entidad = entidad;
        this.progreso = 0;
        this.proyectoId = new ProyectoId(titulo, entidad.getCuit());
        this.estado = EstadoProyecto.EN_ESPERA;
    }

    public void addObjetivo(String objetivo) {
        this.objetivos.add(objetivo);
    }

    public ProyectoId proyectoId() {
        return proyectoId;
    }

    public Entidad entidad() {
        return entidad;
    }

    public Area area() {
        return area;
    }

    public String getTitulo() {
        return proyectoId != null ? proyectoId.titulo() : null;
    }

    public TutorExterno getTutorExterno() {
        return tutorExterno;
    }

    public void setTutorExterno(TutorExterno tutorExterno) {
        this.tutorExterno = tutorExterno;
    }

    public void asignarEstudiante(Estudiante e) {
        if (this.estudiante != null) {
            this.estudiante.setProyecto(null);
        }
        this.estudiante = e;
        if (e != null) {
            e.setProyecto(this);
            this.estudiante = e;
        }
        estado = EstadoProyecto.EN_CURSO;
    }

    public void removerEstudiante() {
        if (this.estudiante != null) {
            Estudiante e = this.estudiante;
            this.estudiante = null;
            e.setProyecto(null);
        }
    }

    public void setPlanDeTrabajo(PlanDeTrabajo planDeTrabajo) {
        this.planDeTrabajo = planDeTrabajo;
        if (planDeTrabajo != null && planDeTrabajo.getProyecto() != this) {
            planDeTrabajo.setProyecto(this);
        }
    }

    public void setEntidad(Entidad entidad) {
        if (this.entidad != null) {
            this.entidad.removeProyecto(this);
        }
        this.entidad = entidad;
//        if (entidad != null) {
//            entidad.addProyecto(this);
//        }
    }

    public enum EstadoProyecto {
        EN_ESPERA("En espera"),
        EN_CURSO("En curso"),
        FINALIZADO("Finalizado");

        private final String label;

        EstadoProyecto(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public boolean isActivo() {
            return this == EN_CURSO;
        }
    }
}