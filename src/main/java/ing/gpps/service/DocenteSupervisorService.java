package ing.gpps.service;

import ing.gpps.entity.institucional.Entrega;
import ing.gpps.entity.institucional.Proyecto;
import ing.gpps.entity.institucional.PlanDeTrabajo;
import ing.gpps.entity.institucional.Actividad;
import ing.gpps.entity.institucional.Actividad.EstadoActividad;
import ing.gpps.entity.idClasses.ActividadId;
import ing.gpps.entity.idClasses.ProyectoId;
import ing.gpps.entity.idClasses.PlanDeTrabajoId;
import ing.gpps.entity.users.DocenteSupervisor;
import ing.gpps.repository.EntregaRepository;
import ing.gpps.repository.ProyectoRepository;
import ing.gpps.repository.ActividadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

@Service
public class DocenteSupervisorService {

    private static final Logger logger = LoggerFactory.getLogger(DocenteSupervisorService.class);

    private final ProyectoRepository proyectoRepository;
    private final EntregaRepository entregaRepository;
    private final ActividadRepository actividadRepository;

    @Value("${app.upload.path}")
    private String uploadPath;

    @Autowired
    public DocenteSupervisorService(ProyectoRepository proyectoRepository, 
                                  EntregaRepository entregaRepository,
                                  ActividadRepository actividadRepository) {
        this.proyectoRepository = proyectoRepository;
        this.entregaRepository = entregaRepository;
        this.actividadRepository = actividadRepository;
    }

    @Transactional(readOnly = true)
    public List<Proyecto> getProyectosByTutor(DocenteSupervisor tutor) {
        if (tutor == null) {
            return List.of();
        }
        return proyectoRepository.findByTutorUNRN(tutor);
    }

    @Transactional(readOnly = true)
    public Proyecto getProyectoByTituloAndCuit(String titulo, Long cuitEntidad) {
        if (titulo == null || cuitEntidad == null) {
            return null;
        }
        return proyectoRepository.findByProyectoId_TituloAndProyectoId_CuitEntidad(titulo, cuitEntidad);
    }

    @Transactional(readOnly = true)
    public List<Entrega> getEntregasByProyecto(Proyecto proyecto) {
        if (proyecto == null) {
            return List.of();
        }
        return entregaRepository.findByActividad_PlanDeTrabajo_Proyecto(proyecto);
    }

    @Transactional
    public void aprobarEntrega(Long id) {
        if (id == null) {
            return;
        }
        entregaRepository.findById(id).ifPresent(entrega -> {
            entrega.setEstado(Entrega.EstadoEntrega.APROBADO);
            entregaRepository.save(entrega);
        });
    }

    @Transactional
    public void rechazarEntrega(Long id) {
        if (id == null) {
            return;
        }
        entregaRepository.findById(id).ifPresent(entrega -> {
            entrega.setEstado(Entrega.EstadoEntrega.RECHAZADO);
            entregaRepository.save(entrega);
        });
    }

    @Transactional
    public void crearActividad(String cuitEntidad, String titulo, Actividad actividad) {
        if (cuitEntidad == null || titulo == null || actividad == null) {
            return;
        }
        Proyecto proyecto = getProyectoByTituloAndCuit(titulo, Long.parseLong(cuitEntidad));
        if (proyecto == null) {
            return;
        }
        
        PlanDeTrabajo planDeTrabajo = proyecto.getPlanDeTrabajo();
        if (planDeTrabajo == null) {
            // Crear nuevo plan de trabajo si no existe
            planDeTrabajo = new PlanDeTrabajo(1, LocalDate.now(), LocalDate.now().plusMonths(6), proyecto);
            proyecto.setPlanDeTrabajo(planDeTrabajo);
        }
        
        // Establecer el número de la actividad basado en el número actual de actividades
        int numeroActividad = planDeTrabajo.getActividades().size() + 1;
        ActividadId actividadId = new ActividadId(numeroActividad, planDeTrabajo.getPlanDeTrabajoId());
        actividad.setActividadId(actividadId);
        
        // Validar que la fecha límite no sea anterior a la fecha actual
        if (actividad.getFechaLimite() != null && actividad.getFechaLimite().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha límite no puede ser anterior a la fecha actual");
        }
        
        // Establecer el estado inicial de la actividad
        actividad.setEstado(EstadoActividad.EN_REVISION);
        
        planDeTrabajo.addActividad(actividad);
        proyectoRepository.save(proyecto);
    }

    @Transactional(readOnly = true)
    public Actividad getActividadById(int actividadId, int planNumero, Long proyectoCuit, String proyectoTitulo) {
        logger.info("Buscando actividad con parámetros: actividadId={}, planNumero={}, proyectoCuit={}, proyectoTitulo={}",
                actividadId, planNumero, proyectoCuit, proyectoTitulo);

        if (actividadId <= 0 || planNumero <= 0 || proyectoCuit == null || proyectoTitulo == null) {
            logger.warn("Parámetros inválidos para buscar actividad");
            return null;
        }
        
        try {
            ProyectoId proyectoId = new ProyectoId(proyectoTitulo, proyectoCuit);
            PlanDeTrabajoId planDeTrabajoId = new PlanDeTrabajoId(planNumero, proyectoId);
            ActividadId actividadIdObj = new ActividadId(actividadId, planDeTrabajoId);
            
            logger.info("Buscando actividad con ID compuesto: {}", actividadIdObj);
            Actividad actividad = actividadRepository.findById(actividadIdObj).orElse(null);
            
            if (actividad == null) {
                logger.warn("No se encontró la actividad con ID: {}", actividadIdObj);
            } else {
                logger.info("Actividad encontrada: {}", actividad);
            }
            
            return actividad;
        } catch (Exception e) {
            logger.error("Error al buscar actividad: {}", e.getMessage(), e);
            return null;
        }
    }

    @Transactional
    public Actividad guardarActividad(Actividad actividad) {
        return actividadRepository.save(actividad);
    }

    @Transactional
    public Entrega guardarEntrega(Entrega entrega) {
        return entregaRepository.save(entrega);
    }

    @Transactional(readOnly = true)
    public Entrega getEntregaById(Long id) {
        return entregaRepository.findById(id).orElse(null);
    }

    @Transactional
    public String guardarArchivoActividad(MultipartFile archivo, Actividad actividad) throws IOException {
        // Crear la estructura de directorios
        String proyectoPath = uploadPath + "/" + actividad.getPlanDeTrabajo().getProyecto().getProyectoId().getTitulo();
        String actividadPath = proyectoPath + "/actividades";
        Path proyectoDir = Paths.get(proyectoPath);
        Path actividadDir = Paths.get(actividadPath);

        // Crear los directorios si no existen
        Files.createDirectories(actividadDir);

        // Generar nombre único para el archivo
        String originalFilename = archivo.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = "actividad_" + actividad.getActividadId().numero() + "_" + System.currentTimeMillis() + extension;

        // Guardar el archivo
        Path filePath = actividadDir.resolve(newFilename);
        Files.copy(archivo.getInputStream(), filePath);

        // Retornar la ruta relativa del archivo
        return actividadPath + "/" + newFilename;
    }
}