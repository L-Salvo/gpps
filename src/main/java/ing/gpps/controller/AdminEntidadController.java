package ing.gpps.controller;

import ing.gpps.entity.institucional.Actividad;
import ing.gpps.entity.institucional.PlanDeTrabajo;
import ing.gpps.entity.institucional.Proyecto;
import ing.gpps.entity.users.AdminEntidad;
import ing.gpps.entity.users.TutorExterno;
import ing.gpps.entity.users.Usuario;
import ing.gpps.security.CustomUserDetails;
import ing.gpps.service.AdminEntidadService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin-entidad")
@SessionAttributes({"planForm"})
public class AdminEntidadController {

    @Autowired
    private AdminEntidadService adminEntidadService;

    @GetMapping("/dashboard")
    public String dashboard(Model model,
                            @RequestParam(value = "modal", required = false) String modal,
                            @RequestParam(value = "mensaje", required = false) String mensaje,
                            @RequestParam(value = "error", required = false) String error) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                throw new RuntimeException("Usuario no autenticado");
            }
            Object principal = authentication.getPrincipal();

            if (!(principal instanceof CustomUserDetails)) {
                throw new RuntimeException("Usuario no es CustomUserDetails");
            }

            CustomUserDetails userDetails = (CustomUserDetails) principal;
            Usuario usuario = userDetails.getUsuario();

            if (!(usuario instanceof AdminEntidad)) {
                throw new RuntimeException("Usuario no es un administrador de entidad");
            }

            AdminEntidad adminEntidad = (AdminEntidad) usuario;

            // Cargar todos los datos necesarios
            cargarDatosCompletos(model, adminEntidad);

            // Manejar mensajes de redirect
            if (mensaje != null) {
                model.addAttribute("mensaje", mensaje);
            }
            if (error != null) {
                model.addAttribute("error", error);
            }

            // Manejar apertura de modales
            if ("plan".equals(modal)) {
                model.addAttribute("abrirModalPlan", true);
            } else if ("actividad".equals(modal)) {
                model.addAttribute("abrirModalActividad", true);
                model.addAttribute("abrirModalPlan", true);
            }

            return "indexAdminEntidad";
        } catch (Exception e) {
            System.err.println("Error en dashboard: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/login";
    }

    //MODIFICAR LA CARGA DE ACTIVIDADES: REEMPLAZAR "ADJUNTA ARCHIVO" POR FECHA LIMITE

    @ModelAttribute("planForm")
    public PlanForm planForm() {
        return new PlanForm();
    }

    @PostMapping("/guardarTutor")
    public String guardarTutor(@ModelAttribute("tutor") @Valid TutorExterno tutor,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Datos inválidos del tutor.");
            return "redirect:/admin-entidad/dashboard";
        }

        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Usuario usuario = userDetails.getUsuario();
        AdminEntidad admin = (AdminEntidad) usuario;
        adminEntidadService.altaTutor(tutor.getNombre(), tutor.getApellido(), tutor.getEmail(),
                tutor.getPassword(), tutor.getTelefono(), admin.getEntidad().getCuit());

        redirectAttributes.addFlashAttribute("mensaje", "Tutor externo registrado exitosamente.");
        return "redirect:/admin-entidad/dashboard";
    }

    @PostMapping("/guardarProyecto")
    public String guardarProyecto(@ModelAttribute("proyecto") @Valid Proyecto proyecto,
                                  BindingResult result,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Datos inválidos del proyecto.");
            return "redirect:/admin-entidad/dashboard";
        }

        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Usuario usuario = userDetails.getUsuario();
        AdminEntidad admin = (AdminEntidad) usuario;
        proyecto.setEntidad(admin.getEntidad()); //la línea que lanza la excepcion.

        adminEntidadService.proponerProyectos(proyecto);

        redirectAttributes.addFlashAttribute("mensaje", "Proyecto agregado correctamente.");
        return "redirect:/admin-entidad/dashboard";
    }

    @GetMapping("/nuevoPlan")
    public String iniciarCargaPlan(RedirectAttributes redirectAttributes) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Usuario usuario = userDetails.getUsuario();
        AdminEntidad adminEntidad = (AdminEntidad) usuario;

        try {
            List<Proyecto> proyectos = adminEntidadService.obtenerProyectosSinPlan(adminEntidad.getEntidad().getCuit());
            if (proyectos == null || proyectos.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No hay proyectos sin plan de trabajo para esta entidad.");
                return "redirect:/admin-entidad/dashboard";
            }
        } catch (EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "No hay proyectos sin plan de trabajo para esta entidad.");
            return "redirect:/admin-entidad/dashboard";
        } catch (Exception e) {
            System.err.println("Error al obtener proyectos sin plan: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al cargar proyectos. Intente nuevamente.");
            return "redirect:/admin-entidad/dashboard";
        }

        redirectAttributes.addAttribute("modal", "plan");
        return "redirect:/admin-entidad/dashboard";
    }

    @GetMapping("/eliminarTutor")
    public String eliminarTutor(@RequestParam Long cuitTutor,
                                RedirectAttributes redirectAttributes) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Usuario usuario = userDetails.getUsuario();
            AdminEntidad admin = (AdminEntidad) usuario;

            adminEntidadService.eliminarTutor(cuitTutor, admin.getEntidad().getCuit());
            redirectAttributes.addFlashAttribute("mensaje", "Tutor eliminado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el tutor: " + e.getMessage());
        }
        return "redirect:/admin-entidad/dashboard";
    }

    @GetMapping("/eliminarProyecto")
    public String eliminarProyecto(@RequestParam String tituloProyecto,
                                   @RequestParam Long cuitEntidad,
                                   RedirectAttributes redirectAttributes) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Usuario usuario = userDetails.getUsuario();
            AdminEntidad admin = (AdminEntidad) usuario;

            // Seguridad: verificar que el cuit recibido coincide con el del admin autenticado
            if (!admin.getEntidad().getCuit().equals(cuitEntidad)) {
                redirectAttributes.addFlashAttribute("error", "No puede eliminar proyectos de otra entidad.");
                return "redirect:/admin-entidad/dashboard";
            }

            adminEntidadService.eliminarProyecto(tituloProyecto, cuitEntidad);
            redirectAttributes.addFlashAttribute("mensaje", "Proyecto eliminado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el proyecto: " + e.getMessage());
        }
        return "redirect:/admin-entidad/dashboard";
    }

    @GetMapping("/eliminarPlanDeTrabajo")
    public String eliminarPlanDeTrabajo(@RequestParam Long cuitEntidad,
                                        @RequestParam int numeroPlan,
                                        @RequestParam String tituloProyecto,
                                        RedirectAttributes redirectAttributes) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Usuario usuario = userDetails.getUsuario();
            AdminEntidad admin = (AdminEntidad) usuario;

            // Seguridad: verificar que el cuit recibido coincide con el del admin autenticado
            if (!admin.getEntidad().getCuit().equals(cuitEntidad)) {
                redirectAttributes.addFlashAttribute("error", "No puede eliminar planes de otra entidad.");
                return "redirect:/admin-entidad/dashboard";
            }

            adminEntidadService.eliminarPlanDeTrabajo(numeroPlan, tituloProyecto, cuitEntidad);
            redirectAttributes.addFlashAttribute("mensaje", "Plan de trabajo eliminado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el plan de trabajo: " + e.getMessage());
        }
        return "redirect:/admin-entidad/dashboard";
    }


    @PostMapping("/agregarActividad")
    public String agregarActividad(@RequestParam String nombre,
                                   @RequestParam String descripcion,
                                   @RequestParam int cantidadHoras,
                                   @RequestParam String fechaLimite,
                                   @ModelAttribute("planForm") PlanForm planForm,
                                   RedirectAttributes redirectAttributes) {
        try {
            Actividad actividad = new Actividad();
            actividad.setNombre(nombre);
            actividad.setDescripcion(descripcion);
            actividad.setCantidadHoras(cantidadHoras);
            actividad.setFechaLimite(LocalDate.parse(fechaLimite));

            planForm.agregarActividad(actividad);

            redirectAttributes.addFlashAttribute("mensaje",
                    "Actividad agregada al plan. Total actividades: " + planForm.getActividades().size());
            redirectAttributes.addAttribute("modal", "plan");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al agregar actividad: " + e.getMessage());
        }

        return "redirect:/admin-entidad/dashboard";
    }


    @PostMapping("/guardarPlan")
    public String guardarPlan(@ModelAttribute("planForm") PlanForm planForm,
                              @RequestParam String numeroPlan,
                              @RequestParam String fechaInicio,
                              @RequestParam String fechaFin,
                              @RequestParam String tituloProyecto,
                              RedirectAttributes redirectAttributes,
                              SessionStatus sessionStatus) {
        try {
            // Obtener el usuario autenticado para el CUIT
            CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            Usuario usuario = userDetails.getUsuario();
            AdminEntidad admin = (AdminEntidad) usuario;
            Long cuitEntidad = admin.getEntidad().getCuit();

            // Convertir fechas de String a LocalDate
            LocalDate inicio = LocalDate.parse(fechaInicio);
            LocalDate fin = LocalDate.parse(fechaFin);

            // Convertir numeroPlan de String a int
            int numeroPlaneInt = Integer.parseInt(numeroPlan);

            // Llamar al método correcto con los parámetros adecuados
            adminEntidadService.generarPlanDeTrabajo(
                    planForm.getActividades(),  // List<Actividad>
                    numeroPlaneInt,             // int
                    inicio,                     // LocalDate
                    fin,                        // LocalDate
                    tituloProyecto,            // String
                    cuitEntidad                // Long
            );

            sessionStatus.setComplete();
            redirectAttributes.addFlashAttribute("mensaje", "Plan de trabajo guardado exitosamente");

        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("error", "Número de plan inválido");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar plan: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/admin-entidad/dashboard";
    }

    @PostMapping("/eliminarActividad")
    public String eliminarActividad(@RequestParam int indice,
                                    @ModelAttribute("planForm") PlanForm planForm,
                                    RedirectAttributes redirectAttributes) {
        if (indice >= 0 && indice < planForm.getActividades().size()) {
            planForm.getActividades().remove(indice);
            redirectAttributes.addFlashAttribute("mensaje", "Actividad eliminada del plan.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Índice de actividad inválido.");
        }

        redirectAttributes.addAttribute("modal", "plan");
        return "redirect:/admin-entidad/dashboard";
    }

    @PostMapping("/modificarActividad")
    public String modificarActividad(@RequestParam Long cuitEntidad,
                                     @RequestParam int cantidadHoras,
                                     @RequestParam String nombre,
                                     @RequestParam String descripcion,
                                     @RequestParam String fechaLimite,
                                     @ModelAttribute("actividad") Actividad actividad,
                                     RedirectAttributes redirectAttributes) {
        try {
            adminEntidadService.modificarActividades(actividad, cuitEntidad, cantidadHoras, nombre, descripcion, LocalDate.parse(fechaLimite));
            redirectAttributes.addFlashAttribute("mensaje", "Actividad modificada correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al modificar la actividad: " + e.getMessage());
        }
        return "redirect:/admin-entidad/dashboard";
    }

    private void cargarDatosCompletos(Model model, AdminEntidad adminEntidad) {
        model.addAttribute("admin", adminEntidad);
        model.addAttribute("tutor", new TutorExterno());
        model.addAttribute("proyecto", new Proyecto());
        model.addAttribute("actividad", new Actividad());
        model.addAttribute("areas", adminEntidadService.obtenerTodasLasAreas());

        List<TutorExterno> tutores = adminEntidadService.obtenerTutoresDeEntidad(adminEntidad.getEntidad().getCuit());
        model.addAttribute("tutores", tutores);

        List<Proyecto> proyectos = adminEntidadService.obtenerProyectosPorCuit(adminEntidad.getEntidad().getCuit());
        model.addAttribute("proyectos", proyectos);

        // ✅ USAR TU MÉTODO EXISTENTE
        try {
            List<PlanDeTrabajo> planesDeTrabajo = adminEntidadService.ObtenerPlanPorEntidad(adminEntidad.getEntidad().getCuit());
            model.addAttribute("planesDeTrabajo", planesDeTrabajo != null ? planesDeTrabajo : new ArrayList<>());
            System.out.println("✅ Planes cargados: " + (planesDeTrabajo != null ? planesDeTrabajo.size() : 0));
        } catch (Exception e) {
            System.err.println("❌ Error al cargar planes: " + e.getMessage());
            model.addAttribute("planesDeTrabajo", new ArrayList<>());
        }

        try {
            List<Proyecto> proyectosSinPlan = adminEntidadService.obtenerProyectosSinPlan(adminEntidad.getEntidad().getCuit());
            model.addAttribute("proyectosSinPlan", proyectosSinPlan != null ? proyectosSinPlan : new ArrayList<>());
        } catch (EntityNotFoundException e) {
            System.out.println("No hay proyectos sin plan para la entidad: " + adminEntidad.getEntidad().getCuit());
            model.addAttribute("proyectosSinPlan", new ArrayList<>());
        } catch (Exception e) {
            System.err.println("Error al cargar proyectos sin plan: " + e.getMessage());
            model.addAttribute("proyectosSinPlan", new ArrayList<>());
        }

        model.addAttribute("estados", Proyecto.EstadoProyecto.values());

        if (!model.containsAttribute("planForm")) {
            model.addAttribute("planForm", new PlanForm());
        }
    }

    public static class PlanForm implements Serializable {
        private static final long serialVersionUID = 1L;

        private List<Actividad> actividades = new ArrayList<>();
        private int numeroPlan;
        private String fechaInicio;
        private String fechaFin;
        private String tituloProyecto;

        public PlanForm() {
            this.actividades = new ArrayList<>();
        }

        public List<Actividad> getActividades() {
            if (actividades == null) {
                actividades = new ArrayList<>();
            }
            System.out.println("PlanForm - Devolviendo " + actividades.size() + " actividades");
            return actividades;
        }

        public int getNumeroPlan() {
            return numeroPlan;
        }

        public String getFechaInicio() {
            return fechaInicio;
        }

        public String getFechaFin() {
            return fechaFin;
        }

        public String getTituloProyecto() {
            return tituloProyecto;
        }

        public void setActividades(List<Actividad> actividades) {
            System.out.println("PlanForm - Estableciendo " + (actividades != null ? actividades.size() : 0) + " actividades");
            this.actividades = actividades != null ? actividades : new ArrayList<>();
        }

        public void setNumeroPlan(int numeroPlan) {
            this.numeroPlan = numeroPlan;
        }

        public void setFechaInicio(String fechaInicio) {
            this.fechaInicio = fechaInicio;
        }

        public void setFechaFin(String fechaFin) {
            this.fechaFin = fechaFin;
        }

        public void setTituloProyecto(String tituloProyecto) {
            this.tituloProyecto = tituloProyecto;
        }

        public void agregarActividad(Actividad actividad) {
            if (this.actividades == null) {
                this.actividades = new ArrayList<>();
            }
            this.actividades.add(actividad);
            System.out.println("Actividad agregada. Total: " + this.actividades.size());
        }

        public boolean tieneActividades() {
            return actividades != null && !actividades.isEmpty();
        }

        public int getTotalHoras() {
            return actividades != null ?
                    actividades.stream().mapToInt(Actividad::getCantidadHoras).sum() : 0;
        }

        public void limpiar() {
            this.actividades.clear();
            this.numeroPlan = 0;
            this.fechaInicio = null;
            this.fechaFin = null;
            this.tituloProyecto = null;
        }
    }
}


