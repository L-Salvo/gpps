package ing.gpps.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.draw.LineSeparator;
import ing.gpps.entity.institucional.Actividad;
import ing.gpps.entity.Solicitud;
import ing.gpps.entity.institucional.Convenio;
import ing.gpps.repository.ConvenioRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ConvenioService {

    private final ConvenioRepository convenioRepository;
    private static final String CARPETA_CONVENIOS = "convenios";
    private static final float HEADER_HEIGHT = 100;

    private Font titleFont;
    private Font subtitleFont;
    private Font normalFont;
    private Font boldFont;

    private static final Logger logger = LoggerFactory.getLogger(ConvenioService.class);

    public ConvenioService(ConvenioRepository convenioRepository) {
        this.convenioRepository = convenioRepository;
        
        // Inicializar fuentes con la fuente por defecto
        try {
            titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
            subtitleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
            normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
            boldFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        } catch (Exception e) {
            throw new RuntimeException("Error al inicializar fuentes del PDF: " + e.getMessage(), e);
        }
    }

    private class HeaderFooterPageEvent extends PdfPageEventHelper {
        private Image logoUNRN;

        public HeaderFooterPageEvent() {
            try {
                // Intentar cargar la imagen desde una URL
                String imageUrl = "https://hebbkx1anhila5yf.public.blob.vercel-storage.com/image-So2CHeud6JF1xVNp1LlSIuwt3sgVUg.png";
                logger.info("Intentando cargar imagen desde URL: {}", imageUrl);
                
                // Crear una URL y obtener la imagen
                java.net.URL url = new java.net.URL(imageUrl);
                java.io.InputStream is = url.openStream();
                byte[] imageBytes = is.readAllBytes();
                is.close();
                
                // Crear la imagen desde los bytes
                logoUNRN = Image.getInstance(imageBytes);
                logoUNRN.scaleToFit(70, 70);
                logger.info("Imagen cargada exitosamente");
            } catch (Exception e) {
                logger.error("Error al cargar el logo UNRN: " + e.getMessage(), e);
                // Crear una imagen de respaldo o placeholder
                try {
                    logoUNRN = Image.getInstance(createPlaceholderImage());
                    logoUNRN.scaleToFit(70, 70);
                } catch (Exception ex) {
                    logger.error("Error al crear imagen de respaldo: " + ex.getMessage(), ex);
                }
            }
        }

        private byte[] createPlaceholderImage() throws IOException {
            // Crear una imagen simple de 100x100 píxeles
            java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(100, 100, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g2d = image.createGraphics();
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillRect(0, 0, 100, 100);
            g2d.setColor(java.awt.Color.BLACK);
            g2d.drawRect(0, 0, 99, 99);
            g2d.drawString("UNRN", 30, 50);
            g2d.dispose();

            // Convertir a bytes
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                if (logoUNRN != null) {
                    // Posicionar el logo en la esquina superior izquierda
                    logoUNRN.setAbsolutePosition(36, document.getPageSize().getHeight() - 70);
                    writer.getDirectContent().addImage(logoUNRN);
                    logger.debug("Logo agregado exitosamente en la página");
                } else {
                    logger.error("El logo es null en onEndPage");
                }
            } catch (Exception e) {
                logger.error("Error al agregar el logo en la cabecera: " + e.getMessage(), e);
            }
        }
    }

    public Resource generarArchivoConvenio(Convenio convenio) {
        try {
            // Crear la carpeta si no existe
            Path carpetaPath = Paths.get(CARPETA_CONVENIOS).toAbsolutePath();
            if (!Files.exists(carpetaPath)) {
                Files.createDirectories(carpetaPath);
            }
            
            // Verificar permisos de escritura
            if (!Files.isWritable(carpetaPath)) {
                throw new RuntimeException("No hay permisos de escritura en la carpeta de convenios: " + carpetaPath);
            }

            String nombreArchivo = carpetaPath.resolve("convenio-" + convenio.getId() + ".pdf").toString();
            Path filePath = Paths.get(nombreArchivo);
            
            // Verificar si el archivo ya existe
            if (Files.exists(filePath)) {
                return new UrlResource(filePath.toUri());
            }

            // Crear el documento PDF
            Document document = new Document(PageSize.A4, 36, 36, 90, 36);
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(nombreArchivo));
            
            // Agregar el evento de página para el logo
            writer.setPageEvent(new HeaderFooterPageEvent());
            
            document.open();

            // Título del acta
            Paragraph docTitle = new Paragraph("ACTA ACUERDO\nSOBRE INSTANCIAS DE PRÁCTICAS PROFESIONALES SUPERVISADAS", titleFont);
            docTitle.setAlignment(Element.ALIGN_CENTER);
            docTitle.setSpacingBefore(10);
            document.add(docTitle);

            // Introducción
            Paragraph intro = new Paragraph();
            intro.add(new Chunk("Entre la UNIVERSIDAD NACIONAL DE RÍO NEGRO, representada en este acto por su ENTIDAD ", normalFont));
            intro.add(new Chunk(convenio.getEntidad().getNombre(), boldFont));
            intro.add(new Chunk(", y la/el estudiante de la carrera de Licenciatura en Sistemas, ", normalFont));
            intro.add(new Chunk(convenio.getEstudiante().getNombre()+ " " +convenio.getEstudiante().getApellido(), boldFont));
            intro.add(new Chunk(", DNI "+convenio.getEstudiante().getDni(), boldFont));
            intro.add(new Chunk(", en adelante la/el ESTUDIANTE/PRACTICANTE, convienen celebrar el presente acuerdo, conforme lo establecido por la RESOLUCIÓN CDEyVE SEDE ATLÁNTICA N° 011/2021 y con sujeción a las siguientes cláusulas y condiciones:", normalFont));
            intro.setSpacingBefore(10);
            intro.setSpacingAfter(15);
            document.add(intro);

            // Cláusulas
            document.add(new Paragraph("PRIMERA:", boldFont));
            document.add(new Paragraph("El presente tiene como objetivo la implementación del Sistema de Instancias de Prácticas Profesionales Supervisadas \"PPS\", a los fines de que la/el ESTUDIANTE/PRACTICANTE de la carrera Licenciatura en Sistemas, realice actividades de carácter formativo relacionadas con la propuesta curricular de la materia Práctica Profesional Supervisada, de acuerdo al Programa de actividades formativas propuesto en el Anexo I.", normalFont));
            document.add(new Paragraph("\nSEGUNDA:", boldFont));
            document.add(new Paragraph("La situación de/el la/el estudiante practicante no generará ninguna relación laboral entre la/el estudiante y la UNRN, ni tampoco demandará retribución económica alguna a favor del estudiante practicante.", normalFont));
            document.add(new Paragraph("\nTERCERA:", boldFont));
            Paragraph terceraClausula = new Paragraph();
            terceraClausula.add(new Chunk("La Secretaría de Docencia y Vida Estudiantil, designa como tutor/a a ", normalFont));
            terceraClausula.add(new Chunk(convenio.getTutorExterno().getNombre() + " " + convenio.getTutorExterno().getApellido(), boldFont));
            terceraClausula.add(new Chunk(", quien cuenta con experiencia específica y capacidad para planificar, implementar y evaluar propuestas formativas. Asimismo se designa Docente Supervisor a ", normalFont));
            terceraClausula.add(new Chunk(convenio.getDocenteSupervisor().getNombre() + " " + convenio.getDocenteSupervisor().getApellido(), boldFont));
            terceraClausula.add(new Chunk(", quién evaluará el cumplimiento de los aspectos formativos de las tareas de la ESTUDIANTE/PRACTICANTE. Ambos deberán elaborar un plan de trabajo que determine el proceso educativo del estudiante practicante para alcanzar los objetivos propuestos.", normalFont));
            document.add(terceraClausula);
            document.add(new Paragraph("\nCUARTA:", boldFont));
            document.add(new Paragraph("La duración de la práctica tendrá un plazo que no podrá superar la cantidad de 200 (doscientas) horas. Finalizada la misma la/el ESTUDIANTE/PRACTICANTE deberá efectuar un informe final escrito y una instancia de presentación oral del mismo a la que se invitará a concurrir al Tutor que ha supervisado el trabajo de campo. El informe final será evaluado por el Profesor asignado como Docente Supervisor.", normalFont));
            document.add(new Paragraph("\nQUINTA:", boldFont));
            document.add(new Paragraph("Las Instancias de Prácticas Profesionales Supervisadas podrán llevarse a cabo en las instalaciones de la UNRN o en el lugar que ésta disponga según el tipo de labor a desarrollar. Dichos ámbitos reunirán las condiciones de higiene y seguridad dispuesta por la Ley 19587 – Ley de Higiene y Seguridad del Trabajo y sus normas reglamentarias. Con previo acuerdo de ambas partes, las actividades también podrán desarrollarse en el Laboratorio de Informática Aplicada de la Sede Atlántica.", normalFont));
            document.add(new Paragraph("\nSEXTA:", boldFont));
            document.add(new Paragraph("La/El ESTUDIANTE/PRACTICANTE deberá ajustarse a las normas y reglamentos internos de la UNRN que le será comunicada y notificada en el comienzo de las actividades. El horario a asignar no podrá superponerse con el dictado de las horas de clases de la carrera.", normalFont));
            document.add(new Paragraph("\nSÉPTIMA:", boldFont));
            document.add(new Paragraph("La UNRN será la encargada de gestionar la cobertura de los seguros necesarios para la/el ESTUDIANTE/PRACTICANTE que formará parte en las Instancias de Prácticas Profesionales Supervisadas.", normalFont));

            // Firma
            Paragraph firma = new Paragraph("\nEn prueba de conformidad y ratificación de las cláusulas que anteceden, las PARTES firman el presente instrumento, como documento único no editable, a un sólo efecto, conforme surja del detalle de la firma digital inserta y la firma ológrafa de la/el ESTUDIANTE validada por su remisión a la SEDE ATLÁNTICA mediante el correo electrónico denunciado.", normalFont);
            firma.setSpacingBefore(20);
            document.add(firma);

            // Anexo
            document.newPage();
            Paragraph anexoTitle = new Paragraph("ANEXO: PROGRAMA DE ACTIVIDADES FORMATIVAS PROPUESTO", subtitleFont);
            anexoTitle.setAlignment(Element.ALIGN_CENTER);
            anexoTitle.setSpacingAfter(20);
            document.add(anexoTitle);

            Paragraph alumnoInfo = new Paragraph();
            alumnoInfo.add(new Chunk("Alumno: ", boldFont));
            alumnoInfo.add(new Chunk(convenio.getEstudiante().getNombre() + " " + convenio.getEstudiante().getApellido()+"               ", normalFont));
            alumnoInfo.add(new Chunk("Legajo: ", boldFont));
            alumnoInfo.add(new Chunk(String.valueOf(convenio.getEstudiante().getLegajo()), normalFont));
            alumnoInfo.setAlignment(Element.ALIGN_CENTER);

            Paragraph objetivosProyecto = new Paragraph();

            objetivosProyecto.add(new Chunk("\n\nTitulo del Proyecto: ", boldFont));
            objetivosProyecto.add(new Chunk(convenio.getProyecto().getTitulo(), normalFont));

            objetivosProyecto.add(new Chunk("\n\nBreve Descripción del Proyecto: ", boldFont));
            objetivosProyecto.add(new Chunk(convenio.getProyecto().getDescripcion(), normalFont));

            if (convenio.getProyecto() != null) {
                List<String> objetivos = convenio.getProyecto().getObjetivos();
                if (objetivos != null && !objetivos.isEmpty()) {
                    objetivosProyecto.add(new Chunk("\n\nObjetivos del Proyecto:\n", boldFont));
                    for (String objetivo : objetivos) {
                        objetivosProyecto.add(new Chunk("\n  • " + objetivo, normalFont));
                    }
                }
            }

            document.add(alumnoInfo);
            document.add(objetivosProyecto);

            // Tabla de Plan de Trabajo
            if (convenio.getProyecto().getPlanDeTrabajo() != null) {
                List<Actividad> actividades = convenio.getProyecto().getPlanDeTrabajo().getActividades();
                if (actividades != null && !actividades.isEmpty()) {
                    document.add(new Paragraph("\nPlan de Trabajo:", boldFont));

                    PdfPTable table = new PdfPTable(2); // 2 columnas
                    table.setWidthPercentage(100); // Ancho de la tabla al 100%
                    table.setSpacingBefore(10f); // Espacio antes de la tabla
                    table.setSpacingAfter(10f); // Espacio después de la tabla

                    // Establecer anchos relativos de las columnas (Actividad: 80%, Horas: 20%)
                    float[] columnWidths = {0.80f, 0.20f};
                    table.setWidths(columnWidths);

                    // Encabezados de la tabla
                    PdfPCell cell1 = new PdfPCell(new Phrase(" Actividad", boldFont));
                    cell1.setHorizontalAlignment(Element.ALIGN_LEFT);
                    cell1.setBackgroundColor(new BaseColor(236, 236, 236));
                    table.addCell(cell1);

                    PdfPCell cell2 = new PdfPCell(new Phrase(" Horas", boldFont));
                    cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell2.setBackgroundColor(new BaseColor(236, 236, 236));
                    table.addCell(cell2);

                    int totalHoras = 0;
                    for (Actividad actividad : actividades) {
                        table.addCell(new Phrase(" "+actividad.getNombre(), normalFont));
                        PdfPCell horasCell = new PdfPCell(new Phrase(String.valueOf(actividad.getHoras()), normalFont));
                        horasCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        table.addCell(horasCell);
                        totalHoras += actividad.getHoras();
                    }

                    // Fila Total
                    PdfPCell totalTextCell = new PdfPCell(new Phrase(" TOTAL", boldFont));
                    totalTextCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                    table.addCell(totalTextCell);

                    PdfPCell totalHoursCell = new PdfPCell(new Phrase(String.valueOf(totalHoras), boldFont));
                    totalHoursCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(totalHoursCell);
                    
                    document.add(table);
                }
            }
            
            document.close();

            // Convertir el archivo a un recurso descargable
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("No se pudo leer el archivo generado: " + filePath);
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el archivo del convenio: " + e.getMessage(), e);
        }
    }

    public Convenio obtenerConvenioPorSolicitud(Solicitud solicitud) {
        if (solicitud == null || solicitud.getProyecto() == null || solicitud.getSolicitante() == null) {
            throw new IllegalArgumentException("Solicitud, proyecto o solicitante no pueden ser nulos");
        }

        return convenioRepository.findByProyecto(solicitud.getProyecto())
                .orElseThrow(() -> new RuntimeException("No se encontró un convenio para el proyecto: " + solicitud.getProyecto().getTitulo()));
    }

    public List<Convenio> obtenerTodosLosConvenios() {
        return convenioRepository.findAll();
    }

    public Convenio obtenerConvenioPorId(Long id) {
        return convenioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el convenio con ID: " + id));
    }
}