package ing.gpps.service;

import ing.gpps.entity.idClasses.ProyectoId;
import ing.gpps.entity.institucional.Proyecto;
import ing.gpps.entity.users.DocenteSupervisor;
import ing.gpps.entity.users.Estudiante;
import ing.gpps.repository.DocenteSupervisorRepository;
import ing.gpps.repository.ProyectoRepository;
import ing.gpps.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DireccionDeCarreraService {

    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;
    private final GenerarConvenioService generarConvenioService;
    private final DocenteSupervisorRepository docenteSupervisorRepository;

    public DireccionDeCarreraService(ProyectoRepository proyectoRepository, UsuarioRepository usuarioRepository, GenerarConvenioService generarConvenioService, DocenteSupervisorRepository docenteSupervisorRepository){
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
        this.generarConvenioService = generarConvenioService;
        this.docenteSupervisorRepository = docenteSupervisorRepository;
    }

    @Transactional
    public void asignarDocenteSupervisor(Long cuitEntidad, String titulo, String emailDocente) {
        // Buscar el proyecto por su ID compuesto
        ProyectoId proyectoId = new ProyectoId(titulo, cuitEntidad);
        Optional<Proyecto> proyectoOpt = proyectoRepository.findById(proyectoId);

        // Buscar el docente supervisor por su ID
        Optional<DocenteSupervisor> docenteOpt = docenteSupervisorRepository.findByEmail(emailDocente);

        // Verificar que ambos existan
        if (proyectoOpt.isPresent() && docenteOpt.isPresent()) {
            Proyecto proyecto = proyectoOpt.get();
            DocenteSupervisor docente = docenteOpt.get();

            // Asignar el docente al proyecto
            proyecto.setTutorUNRN(docente);

            // Guardar el proyecto actualizado
            proyectoRepository.save(proyecto);
        } else {
            throw new RuntimeException("No se pudo asignar el docente supervisor al proyecto. Verifique los datos.");
        }
    }

    @Transactional(readOnly = true)
    public List<DocenteSupervisor> getAllDocentesSupervisores() {
        return docenteSupervisorRepository.findAll();
    }

    public void aprobarEstudiantePPS(Proyecto p, Estudiante e) {

//        p.asignarEstudiante(e);
//        e.asignarProyecto(p);
//        p.setEstado(Proyecto.EstadoProyecto.EN_CURSO);
//        p.setProgreso(0);
//        p.setFechaInicio(java.time.LocalDate.now());
//        p.setFechaFinEstimada(java.time.LocalDate.now().plusMonths(6));
//
//        proyectoRepository.save(p);
//
//        // Enviar notificación al estudiante
//        generarConvenioService.generar(p, e);




    }



}
