package ing.gpps.repository;

import ing.gpps.entity.Solicitud;
import ing.gpps.entity.institucional.Proyecto;
import ing.gpps.entity.idClasses.ProyectoId;
import ing.gpps.entity.users.DocenteSupervisor;
import ing.gpps.entity.users.Estudiante;
import ing.gpps.entity.users.TutorExterno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProyectoRepository extends JpaRepository<Proyecto, ProyectoId> {
    List<Proyecto> findByTutorUNRN(DocenteSupervisor tutor);
    Proyecto findByProyectoId_TituloAndProyectoId_CuitEntidad(String titulo, Long cuitEntidad);
    List<Proyecto> findByTutorExterno(TutorExterno tutorExterno);

    @Query("SELECT p FROM Proyecto p WHERE p.estudiante.id = :estudianteId")
    List<Proyecto> findByEstudiante(@Param("estudianteId") Long estudianteId);

    boolean existsByProyectoId_Titulo(String titulo);
    Optional<Proyecto> findByProyectoId_Titulo(String titulo);

    List<Proyecto> findByEstudiante(Estudiante estudiante);

    Proyecto findByProyectoIdCuitEntidad(Long cuit);
    //Optional<Proyecto> findByEstudianteAndId(Estudiante estudiante, ProyectoId proyectoId);

    @Query("""
            SELECT p FROM Proyecto p
            WHERE p.entidad.cuit = :cuitEntidad
            AND p.planDeTrabajo IS NULL
            """)
    List<Proyecto> findWithoutPlanDeTrabajoByEntidad(@Param("cuitEntidad") Long cuitEntidad);

    Optional<Proyecto> findByProyectoId(ProyectoId id);

    List<Proyecto> findAllByProyectoIdCuitEntidad(Long cuitEntidad);

    List<Proyecto> findByEstado(Proyecto.EstadoProyecto estado);
    @Query("SELECT DISTINCT p FROM Proyecto p JOIN Solicitud s ON s.proyecto = p WHERE s.solicitante = :estudiante AND s.estado = :estado")
    List<Proyecto> findBySolicitudes_EstudianteAndSolicitudes_Estado(@Param("estudiante") Estudiante estudiante, @Param("estado") Solicitud.EstadoSolicitud estado);
}
