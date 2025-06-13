package ing.gpps.controller;

import ing.gpps.entity.Solicitud;
import ing.gpps.entity.institucional.Convenio;
import ing.gpps.entity.institucional.Entidad;
import ing.gpps.entity.institucional.Proyecto;
import ing.gpps.entity.users.Admin;
import ing.gpps.entity.users.DireccionDeCarrera;
import ing.gpps.entity.users.DocenteSupervisor;
import ing.gpps.entity.users.Usuario;
import ing.gpps.repository.SolicitudRepository;
import ing.gpps.security.CustomUserDetails;
import ing.gpps.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/direccion")
public class DireccionCarreraController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private EntidadService entidadService;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private ProyectoService proyectoService;
    @Autowired
    private AdminEntidadService adminEntidadService;
    @Autowired
    private EstudianteService estudianteService;
    @Autowired
    private SolicitudRepository solicitudRepository;
    @Autowired
    private SolicitudService solicitudService;
    @Autowired
    private ConvenioService convenioService;
    @Autowired
    private DireccionDeCarreraService direccionDeCarreraService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // Verificar autenticación y usuario
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Intento de acceso no autorizado al dashboard");
                return "redirect:/login";
            }

            Object principal = authentication.getPrincipal();
            if (!(principal instanceof CustomUserDetails)) {
                logger.warn("Usuario no es CustomUserDetails");
                return "redirect:/login";
            }

            CustomUserDetails userDetails = (CustomUserDetails) principal;
            Usuario usuario = userDetails.getUsuario();

            if (!(usuario instanceof DireccionDeCarrera)) {
                logger.warn("Usuario no es DireccionDeCarrera");
                return "redirect:/login";
            }

            DireccionDeCarrera direccionDeCarrera = (DireccionDeCarrera) usuario;
            if (direccionDeCarrera == null || direccionDeCarrera.getId() == null) {
                logger.error("Error: Usuario no encontrado o ID nulo");
                return "redirect:/error";
            }

            model.addAttribute("direccion", direccionDeCarrera);

            List<Proyecto> proyectosActivos = proyectoService.obtenerProyectosActivos();
            model.addAttribute("proyectosActivos", proyectosActivos);

            List<Proyecto> proyectosConPostulantes = proyectoService.obtenerProyectosConPostulantes();
            Map<Proyecto, Integer> cantidadPostulantes = proyectoService.obtenerCantidadPostulantesPorProyecto(proyectosConPostulantes);
            
            model.addAttribute("proyectosConPostulantes", proyectosConPostulantes);
            model.addAttribute("cantidadPostulantes", cantidadPostulantes);

            // Obtener todos los convenios y añadirlos al modelo
            List<Convenio> convenios = convenioService.obtenerTodosLosConvenios();
            model.addAttribute("convenios", convenios);

            return "indexDireccionDeCarrera";
        } catch (Exception e) {
            logger.error("Error en dashboard: {}", e.getMessage());
            return "redirect:/error";
        }
    }

    @GetMapping("/proyecto/{cuit}/{titulo}")
    public String verDetalleProyecto(@PathVariable Long cuit, 
                                   @PathVariable String titulo, 
                                   Model model) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return "redirect:/login";
            }

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            DireccionDeCarrera direccion = (DireccionDeCarrera) userDetails.getUsuario();
            model.addAttribute("direccion", direccion);

            Proyecto proyecto = proyectoService.getProyectoByTituloAndCuit(titulo, cuit);
            if (proyecto == null) {
                logger.error("Proyecto no encontrado: CUIT={}, Título={}", cuit, titulo);
                return "redirect:/direccion/dashboard";
            }

            int horasCompletadas = proyectoService.calcularHorasCompletadas(proyecto);

            double progreso = proyectoService.calcularProgreso(proyecto);

            model.addAttribute("proyecto", proyecto);
            model.addAttribute("horasCompletadas", horasCompletadas);
            model.addAttribute("progreso", progreso);

            return "detalleProyectoDireccionDeCarrera";
        } catch (Exception e) {
            logger.error("Error al ver detalle del proyecto: {}", e.getMessage());
            return "redirect:/direccion/dashboard";
        }
    }

    @PostMapping("/proyecto/{cuit}/{titulo}/finalizar")
    public String finalizarProyecto(@PathVariable Long cuit, 
                                  @PathVariable String titulo) {
        try {
            proyectoService.finalizarPPS(titulo, cuit);

            return "redirect:/direccion/dashboard";
        } catch (Exception e) {
            logger.error("Error al finalizar el proyecto: {}", e.getMessage());
            return "redirect:/direccion/dashboard";
        }
    }

    @GetMapping("/proyecto/{cuit}/{titulo}/postulantes")
    public String verPostulantesProyecto(@PathVariable String cuit, @PathVariable String titulo, Model model) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return "redirect:/login";
            }

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            DireccionDeCarrera direccion = (DireccionDeCarrera) userDetails.getUsuario();
            model.addAttribute("direccion", direccion);

            Long cuitLong = Long.parseLong(cuit);
            Proyecto proyecto = proyectoService.getProyectoByTituloAndCuit(titulo, cuitLong);
            if (proyecto == null) {
                logger.error("Proyecto no encontrado: CUIT={}, Título={}", cuit, titulo);
                return "redirect:/direccion/dashboard";
            }

            List<Solicitud> solicitudes = solicitudService.getSolicitudesPendientesByProyecto(proyecto);
            model.addAttribute("proyecto", proyecto);
            model.addAttribute("solicitudes", solicitudes);

            return "postulantesProyectoDireccionDeCarrera";
        } catch (NumberFormatException e) {
            logger.error("Error al convertir CUIT a número: {}", cuit);
            return "redirect:/direccion/dashboard";
        } catch (Exception e) {
            logger.error("Error al obtener postulantes del proyecto: {}", e.getMessage());
            return "redirect:/direccion/dashboard";
        }
    }

    @GetMapping("/solicitud/{id}/aprobar")
    public ResponseEntity<Resource> aprobarSolicitud(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.badRequest().build();
            }

            Solicitud solicitud = solicitudService.aprobarSolicitud(id);
            Convenio convenio = convenioService.obtenerConvenioPorSolicitud(solicitud);

            if (solicitud != null && convenio != null) {
                Resource resource = convenioService.generarArchivoConvenio(convenio);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"convenio-" + convenio.getId() + ".pdf\"")
                        .body(resource);
            }
        } catch (Exception e) {
            logger.error("Error al aprobar solicitud: {}", e.getMessage());
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/solicitud/{id}/rechazar")
    public String rechazarSolicitud(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return "redirect:/login";
            }

            Solicitud solicitud = solicitudService.rechazarSolicitud(id);
            if (solicitud != null && solicitud.getProyecto() != null) {
                Proyecto proyecto = solicitud.getProyecto();
                String cuit = proyecto.getProyectoId().getCuitEntidad().toString();
                String titulo = java.net.URLEncoder.encode(proyecto.getProyectoId().getTitulo(), "UTF-8");
                logger.info("Redirigiendo a proyecto: CUIT={}, Título={}", cuit, titulo);
                return "redirect:/direccion/proyecto/" + cuit + "/" + titulo + "/postulantes";
            } else {
                logger.error("No se pudo obtener la información del proyecto para la solicitud {}", id);
            }
        } catch (Exception e) {
            logger.error("Error al rechazar solicitud: {}", e.getMessage(), e);
        }
        return "redirect:/direccion/dashboard";
    }

    @GetMapping("/convenio/{id}/descargar")
    public ResponseEntity<Resource> descargarConvenio(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.badRequest().build();
            }

            // Obtener el convenio directamente por su ID
            Convenio convenio = convenioService.obtenerConvenioPorId(id);
            if (convenio != null) {
                Resource resource = convenioService.generarArchivoConvenio(convenio);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                logger.error("Convenio no encontrado con ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error al descargar convenio: {}", e.getMessage());
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/asignarDocenteSupervisor/{cuit}/{titulo}")
    public ResponseEntity<?> asignarDocenteSupervisor(
            @PathVariable Long cuit,
            @PathVariable String titulo,
            @RequestParam String emailDocente) {

        try {
            direccionDeCarreraService.asignarDocenteSupervisor(cuit, titulo, emailDocente);
            return ResponseEntity.ok(Map.of("success", true, "message", "Docente supervisor asignado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/docentesSupervisores")
    @ResponseBody
    public ResponseEntity<List<DocenteSupervisor>> getDocentesSupervisores() {
        List<DocenteSupervisor> docentes = direccionDeCarreraService.getAllDocentesSupervisores();
        return ResponseEntity.ok(docentes);
    }

} 