package ing.gpps.controller;

import ing.gpps.entity.idClasses.ActividadId;
import ing.gpps.entity.idClasses.PlanDeTrabajoId;
import ing.gpps.entity.idClasses.ProyectoId;
import ing.gpps.entity.idClasses.InformeId;
import ing.gpps.entity.institucional.*;
import ing.gpps.entity.users.DocenteSupervisor;
import ing.gpps.entity.users.Usuario;
import ing.gpps.security.CustomUserDetails;
import ing.gpps.service.*;
import ing.gpps.repository.InformeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.Optional;

@Controller
@RequestMapping("/docente-supervisor")
public class DocenteSupervisorController {
    private static final Logger logger = LoggerFactory.getLogger(DocenteSupervisorController.class);

    @Value("${app.upload.path}")
    private String uploadPath;

    private final DocenteSupervisorService docenteSupervisorService;
    private final ProyectoService proyectoService;
    private final EntidadService entidadService;
    private final InformeRepository informeRepository;

    @Autowired
    public DocenteSupervisorController(DocenteSupervisorService docenteSupervisorService,
                                     ProyectoService proyectoService,
                                     EntidadService entidadService,
                                     InformeRepository informeRepository) {
        this.docenteSupervisorService = docenteSupervisorService;
        this.proyectoService = proyectoService;
        this.entidadService = entidadService;
        this.informeRepository = informeRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CustomUserDetails)) {
                logger.warn("Intento de acceso no autorizado al dashboard");
                return "redirect:/login";
            }

            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            if (!(userDetails.getUsuario() instanceof DocenteSupervisor)) {
                logger.warn("Usuario no autorizado intentando acceder al dashboard: {}", userDetails.getUsername());
                return "redirect:/login";
            }

            DocenteSupervisor tutor = (DocenteSupervisor) userDetails.getUsuario();
            List<Proyecto> proyectos = tutor != null ? docenteSupervisorService.getProyectosByTutor(tutor) : List.of();

            model.addAttribute("tutor", tutor);
            model.addAttribute("proyectos", proyectos != null ? proyectos : List.of());
            model.addAttribute("menuItems", Map.of(
                "dashboard", "Panel Principal",
                "proyectos", "Mis Proyectos",
                "estudiantes", "Estudiantes Asignados"
            ));

            return "indexDocenteSupervisor";
        } catch (Exception e) {
            logger.error("Error al cargar el dashboard: {}", e.getMessage());
            return "indexDocenteSupervisor";
        }
    }

    @GetMapping("/proyecto/{cuitEntidad}/{titulo}")
    @Transactional(readOnly = true)
    public String verProyecto(@PathVariable Long cuitEntidad,
                            @PathVariable String titulo,
                            Model model) {
        try {
            if (cuitEntidad == null || titulo == null || titulo.trim().isEmpty()) {
                logger.error("Parámetros inválidos: cuitEntidad={}, titulo={}", cuitEntidad, titulo);
                return "redirect:/error";
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CustomUserDetails)) {
                logger.warn("Intento de acceso no autorizado a proyecto: {}/{}", cuitEntidad, titulo);
                return "redirect:/login";
            }

            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            if (!(userDetails.getUsuario() instanceof DocenteSupervisor)) {
                logger.warn("Usuario no autorizado intentando acceder a proyecto: {}", userDetails.getUsername());
                return "redirect:/login";
            }

            DocenteSupervisor tutor = (DocenteSupervisor) userDetails.getUsuario();
            if (tutor == null || tutor.getId() == null) {
                logger.error("Error: Docente supervisor no encontrado o ID nulo");
                return "redirect:/error";
            }

            Proyecto proyecto = proyectoService.getProyectoByTituloAndCuit(titulo, cuitEntidad);
            if (proyecto == null) {
                logger.warn("Proyecto no encontrado: {}/{}", cuitEntidad, titulo);
                return "redirect:/docente-supervisor/dashboard";
            }

            if (proyecto.getTutorUNRN() == null || !proyecto.getTutorUNRN().getId().equals(tutor.getId())) {
                logger.warn("Intento de acceso no autorizado al proyecto {} por el tutor {}",
                          proyecto.getProyectoId(), tutor.getId());
                return "redirect:/docente-supervisor/dashboard";
            }

            PlanDeTrabajo planDeTrabajo = proyecto.getPlanDeTrabajo();
            List<Actividad> actividades = planDeTrabajo != null ? planDeTrabajo.getActividades() : List.of();
            List<Entrega> entregas = docenteSupervisorService.getEntregasByProyecto(proyecto);
            List<Informe> informes = informeRepository.findByActividad_PlanDeTrabajo_Proyecto(proyecto);
            double progreso = proyectoService.calcularProgreso(proyecto);

            model.addAttribute("progreso", progreso);
            model.addAttribute("tutor", tutor);
            model.addAttribute("proyecto", proyecto);
            model.addAttribute("planDeTrabajo", planDeTrabajo);
            model.addAttribute("actividades", actividades);
            model.addAttribute("entregas", entregas);
            model.addAttribute("informes", informes);
            model.addAttribute("menuItems", Map.of(
                "dashboard", "Panel Principal",
                "proyectos", "Mis Proyectos",
                "estudiantes", "Estudiantes Asignados"
            ));

            return "indexDocenteSupervisorProyecto";
        } catch (Exception e) {
            logger.error("Error al cargar el proyecto: {}", e.getMessage(), e);
            return "redirect:/error";
        }
    }

    @PostMapping("/proyecto/{cuit}/{titulo}/actividad/crear")
    @Transactional
    public Object crearActividad(@PathVariable Long cuit,
                               @PathVariable String titulo,
                               @RequestParam String nombre,
                               @RequestParam String descripcion,
                               @RequestParam Integer horas,
                               @RequestParam LocalDate fechaLimite,
                               @RequestParam(value = "archivo", required = false) MultipartFile archivo,
                               HttpServletRequest request) {
        try {
            logger.info("Recibida solicitud para crear actividad:");
            logger.info("  Cuit: {}", cuit);
            logger.info("  Titulo: {}", titulo);
            logger.info("  Nombre de actividad: {}", nombre);
            logger.info("  Descripción de actividad: {}", descripcion);
            logger.info("  Horas estimadas: {}", horas);
            logger.info("  Fecha Límite: {}", fechaLimite);
            logger.info("  Archivo presente: {}", archivo != null && !archivo.isEmpty());

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CustomUserDetails)) {
                logger.warn("Intento de acceso no autorizado para crear actividad");
                if (isAjax(request)) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "No autorizado"));
                } else {
                    return "redirect:/login";
                }
            }

            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            if (!(userDetails.getUsuario() instanceof DocenteSupervisor)) {
                logger.warn("Usuario no autorizado intentando crear actividad: {}", userDetails.getUsername());
                if (isAjax(request)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "No tiene permisos para crear actividades"));
                } else {
                    return "redirect:/login";
                }
            }

            DocenteSupervisor tutor = (DocenteSupervisor) userDetails.getUsuario();
            Proyecto proyecto = proyectoService.getProyectoByTituloAndCuit(titulo, cuit);

            if (proyecto == null || proyecto.getTutorUNRN() == null || !proyecto.getTutorUNRN().getId().equals(tutor.getId())) {
                logger.warn("Intento de crear actividad en proyecto no autorizado");
                if (isAjax(request)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "No tiene permisos para crear actividades en este proyecto"));
                } else {
                    return "redirect:/docente-supervisor/dashboard";
                }
            }

            PlanDeTrabajo planDeTrabajo = proyecto.getPlanDeTrabajo();
            if (planDeTrabajo == null) {
                planDeTrabajo = new PlanDeTrabajo(1, LocalDate.now(), LocalDate.now().plusMonths(6), proyecto);
                proyecto.setPlanDeTrabajo(planDeTrabajo);
            }

            int numeroActividad = planDeTrabajo.getActividades().size() + 1;
            Actividad actividadToSave = new Actividad(numeroActividad, nombre, descripcion, planDeTrabajo, horas);
            actividadToSave.setFechaLimite(fechaLimite);

            // Manejar la subida del archivo si existe
            if (archivo != null && !archivo.isEmpty()) {
                String rutaArchivo = docenteSupervisorService.guardarArchivoActividad(archivo, actividadToSave);
                actividadToSave.setRutaArchivo(rutaArchivo);
            }

            docenteSupervisorService.guardarActividad(actividadToSave);

            if (isAjax(request)) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Actividad creada exitosamente"));
            } else {
                return "redirect:/docente-supervisor/proyecto/" + cuit + "/" + titulo;
            }
        } catch (Exception e) {
            logger.error("Error al crear actividad: ", e);
            if (request != null && isAjax(request)) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error al crear la actividad: " + e.getMessage()));
            } else {
                return "redirect:/error";
            }
        }
    }

    @PostMapping("/cambiar-estado-actividad")
    @Transactional
    public ResponseEntity<?> cambiarEstadoActividad(@RequestParam int actividadId,
                                       @RequestParam int planNumero,
                                       @RequestParam String estado,
                                       @RequestParam(required = false) String comentario,
                                       @RequestParam Long proyectoCuit,
                                       @RequestParam String proyectoTitulo) {
        try {
            logger.info("Recibida solicitud para cambiar estado de actividad:");
            logger.info("  Actividad ID: {}", actividadId);
            logger.info("  Plan Número: {}", planNumero);
            logger.info("  Estado: {}", estado);
            logger.info("  Proyecto CUIT: {}", proyectoCuit);
            logger.info("  Proyecto Título: {}", proyectoTitulo);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CustomUserDetails)) {
                logger.warn("Intento de acceso no autorizado para cambiar estado de actividad");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "No autorizado"));
            }

            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            if (!(userDetails.getUsuario() instanceof DocenteSupervisor)) {
                logger.warn("Usuario no autorizado intentando cambiar estado de actividad: {}", userDetails.getUsername());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "No tiene permisos para realizar esta acción"));
            }

            DocenteSupervisor tutor = (DocenteSupervisor) userDetails.getUsuario();

            // Obtener la actividad y verificar que pertenece a un proyecto
            Actividad actividad = docenteSupervisorService.getActividadById(actividadId, planNumero, proyectoCuit, proyectoTitulo);
            if (actividad == null) {
                logger.warn("Actividad no encontrada: {}-{}-{}-{}", actividadId, planNumero, proyectoCuit, proyectoTitulo);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Actividad no encontrada"));
            }

            Proyecto proyecto = actividad.getPlanDeTrabajo().getProyecto();
            if (proyecto.getTutorUNRN() == null || !proyecto.getTutorUNRN().getId().equals(tutor.getId())) {
                logger.warn("Intento de cambiar estado de actividad en proyecto no autorizado");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "No tiene permisos para modificar esta actividad"));
            }

            // Convertir el string a enum
            Actividad.EstadoActividad estadoActividad;
            try {
                estadoActividad = Actividad.EstadoActividad.valueOf(estado);
            } catch (IllegalArgumentException e) {
                logger.error("Estado de actividad inválido: {}", estado);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Estado de actividad inválido"));
            }

            // Cambiar el estado de la actividad
            actividad.setEstado(estadoActividad);
            if (comentario != null && !comentario.trim().isEmpty()) {
                actividad.setComentarios(comentario);
            }

            docenteSupervisorService.guardarActividad(actividad);
            
            return ResponseEntity.ok(Map.of("success", true, "message", "Estado de actividad actualizado exitosamente"));
            
        } catch (Exception e) {
            logger.error("Error al cambiar estado de actividad: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error al cambiar el estado de la actividad: " + e.getMessage()));
        }
    }

    @PostMapping("/cambiar-estado-entrega")
    @Transactional
    public Object cambiarEstadoEntrega(@RequestParam Long entregaId,
                                     @RequestParam String estado,
                                     @RequestParam(required = false) String comentario,
                                     @RequestParam Long proyectoCuit,
                                     @RequestParam String proyectoTitulo,
                                     HttpServletRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CustomUserDetails)) {
                logger.warn("Intento de acceso no autorizado para cambiar estado de entrega");
                if (isAjax(request)) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "No autorizado"));
                } else {
                    return "redirect:/login";
                }
            }

            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            if (!(userDetails.getUsuario() instanceof DocenteSupervisor)) {
                logger.warn("Usuario no autorizado intentando cambiar estado de entrega: {}", userDetails.getUsername());
                if (isAjax(request)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "No tiene permisos para cambiar estado de entrega"));
                } else {
                    return "redirect:/login";
                }
            }

            DocenteSupervisor tutor = (DocenteSupervisor) userDetails.getUsuario();

            // Obtener la entrega y verificar que pertenece a un proyecto del tutor
            Entrega entrega = docenteSupervisorService.getEntregaById(entregaId);
            if (entrega == null) {
                logger.warn("Entrega no encontrada: {}", entregaId);
                if (isAjax(request)) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Entrega no encontrada"));
                } else {
                    return "redirect:/docente-supervisor/dashboard";
                }
            }

            Proyecto proyecto = entrega.getActividad().getPlanDeTrabajo().getProyecto();
            if (proyecto.getTutorUNRN() == null || !proyecto.getTutorUNRN().getId().equals(tutor.getId())) {
                logger.warn("Intento de cambiar estado de entrega en proyecto no autorizado");
                if (isAjax(request)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "No tiene permisos para modificar esta entrega"));
                } else {
                    return "redirect:/docente-supervisor/dashboard";
                }
            }

            // Convertir el string a enum
            Entrega.EstadoEntrega estadoEntrega;
            try {
                estadoEntrega = Entrega.EstadoEntrega.valueOf(estado);
            } catch (IllegalArgumentException e) {
                logger.error("Estado de entrega inválido: {}", estado);
                if (isAjax(request)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Estado de entrega inválido"));
                } else {
                    return "redirect:/error";
                }
            }

            // Cambiar el estado de la entrega
            entrega.setEstado(estadoEntrega);
            if (comentario != null && !comentario.trim().isEmpty()) {
                entrega.setComentarios(comentario);
            }

            docenteSupervisorService.guardarEntrega(entrega);

            if (isAjax(request)) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Estado de entrega actualizado exitosamente"));
            } else {
                return "redirect:/docente-supervisor/proyecto/" + proyectoCuit + "/" + proyectoTitulo;
            }
        } catch (Exception e) {
            logger.error("Error al cambiar estado de entrega: {}", e.getMessage(), e);
            if (request != null && isAjax(request)) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error al cambiar el estado de la entrega: " + e.getMessage()));
            } else {
                return "redirect:/error";
            }
        }
    }

    @GetMapping("/descargarEntrega/{id}")
    public ResponseEntity<Resource> descargarEntrega(@PathVariable int id) {
        try {
            Entrega entrega = docenteSupervisorService.getEntregaById((long) id);
            if (entrega == null || entrega.getRutaArchivo() == null) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = Paths.get(uploadPath, entrega.getRutaArchivo());
            Resource resource = new FileSystemResource(filePath.toFile());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error al descargar el archivo de la entrega {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/descargarActividad/{numero}/{planNumero}/{proyectoTitulo}/{proyectoCuit}")
    public ResponseEntity<Resource> descargarActividad(
            @PathVariable int numero,
            @PathVariable int planNumero,
            @PathVariable String proyectoTitulo,
            @PathVariable Long proyectoCuit) {
        try {
            // Obtener la actividad
            Actividad actividad = docenteSupervisorService.getActividadById(numero, planNumero, proyectoCuit, proyectoTitulo);
            
            if (actividad == null || actividad.getRutaArchivo() == null) {
                return ResponseEntity.notFound().build();
            }

            // Crear el recurso del archivo
            Path filePath = Paths.get(actividad.getRutaArchivo());
            Resource resource = new FileSystemResource(filePath);

            // Verificar que el archivo existe
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            // Configurar los headers de la respuesta
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filePath.getFileName().toString() + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error al descargar el archivo de la actividad: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/descargarInforme/{numero}/{estudianteDni}/{actividadNumero}/{planNumero}/{proyectoTitulo}/{proyectoCuit}")
    public ResponseEntity<Resource> descargarInforme(
            @PathVariable int numero,
            @PathVariable int estudianteDni,
            @PathVariable int actividadNumero,
            @PathVariable int planNumero,
            @PathVariable String proyectoTitulo,
            @PathVariable Long proyectoCuit) {
        try {
            InformeId informeId = new InformeId(numero, estudianteDni);

            Optional<Informe> informeOpt = informeRepository.findById(informeId);
            if (!informeOpt.isPresent() || informeOpt.get().getRutaArchivo() == null) {
                return ResponseEntity.notFound().build();
            }

            Informe informe = informeOpt.get();
            Path filePath = Paths.get(uploadPath, informe.getRutaArchivo());

            if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
                logger.warn("Archivo de informe no encontrado o no legible en la ruta: {}. Informe Numero: {}, EstudianteDni: {}", filePath, numero, estudianteDni);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(filePath.toFile());
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filePath.getFileName().toString() + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error al descargar el archivo del informe: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private boolean isAjax(HttpServletRequest request) {
        return request != null && "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }
}
