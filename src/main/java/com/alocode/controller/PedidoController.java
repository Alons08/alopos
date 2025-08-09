package com.alocode.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alocode.model.DetallePedido;
import com.alocode.model.Pedido;
import com.alocode.model.Producto;
import com.alocode.model.Usuario;
import com.alocode.model.enums.EstadoPedido;
import com.alocode.model.enums.TipoPedido;
import com.alocode.service.*;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;

import java.util.List;

@Controller
@RequestMapping("/pedidos")
public class PedidoController {
    private final PedidoService pedidoService;
    private final ProductoService productoService;
    private final MesaService mesaService;
    private final CajaService cajaService;
    
    public PedidoController(PedidoService pedidoService, ProductoService productoService, 
                          MesaService mesaService, CajaService cajaService) {
        this.pedidoService = pedidoService;
        this.productoService = productoService;
        this.mesaService = mesaService;
        this.cajaService = cajaService;
    }
    
    @GetMapping
    public String listarPedidos(Model model) {
        List<Pedido> pedidos = pedidoService.obtenerPedidosPendientes();
        model.addAttribute("pedidos", pedidos);
        return "pedidos";
    }
    
    @GetMapping("/nuevo")
    public String mostrarFormularioNuevoPedido(Model model) {
        boolean cajaAbierta = cajaService.obtenerCajaAbiertaHoy().isPresent();
        model.addAttribute("cajaAbierta", cajaAbierta);
        model.addAttribute("pedido", new Pedido());
        model.addAttribute("tiposPedido", TipoPedido.values());
        model.addAttribute("mesasDisponibles", mesaService.obtenerMesasDisponibles());
        model.addAttribute("productos", productoService.obtenerProductosActivos());
        return "nuevo-pedido";
    }
    
    @PostMapping("/guardar")
    public String guardarPedido(@ModelAttribute Pedido pedido,
                               @RequestParam List<Long> productos,
                               @RequestParam List<Integer> cantidades,
                               @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        System.out.println("--- [LOG] Guardar Pedido ---");
        System.out.println("Pedido recibido: " + pedido);
        System.out.println("Productos: " + productos);
        System.out.println("Cantidades: " + cantidades);
        Usuario usuario = null;
        if (userDetails instanceof MyUserDetails myUserDetails) {
            usuario = myUserDetails.getUsuario();
        }
        System.out.println("Usuario: " + (usuario != null ? usuario.getUsername() : "null"));
        try {
            // Validar caja abierta
            if (!cajaService.obtenerCajaAbiertaHoy().isPresent()) {
                System.out.println("[LOG] No hay caja abierta hoy");
                redirectAttributes.addFlashAttribute("error", "No hay caja abierta hoy");
                return "redirect:/pedidos/nuevo";
            }

            // Crear detalles del pedido
            for (int i = 0; i < productos.size(); i++) {
                Producto producto = productoService.obtenerProductoPorId(productos.get(i))
                        .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

                DetallePedido detalle = new DetallePedido();
                detalle.setProducto(producto);
                detalle.setCantidad(cantidades.get(i));
                detalle.setPrecioUnitario(producto.getPrecio());
                detalle.setSubtotal(producto.getPrecio() * cantidades.get(i));
                pedido.getDetalles().add(detalle);
                System.out.println("[LOG] Detalle agregado: Producto=" + producto.getNombre() + ", Cantidad=" + cantidades.get(i));
            }

            if (usuario == null) {
                throw new IllegalStateException("No se pudo obtener el usuario autenticado");
            }
            pedidoService.crearPedido(pedido, pedido.getDetalles(), usuario);
            System.out.println("[LOG] Pedido guardado correctamente");
            redirectAttributes.addFlashAttribute("success", "Pedido creado exitosamente");
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/pedidos/nuevo";
        }
        return "redirect:/pedidos";
    }
    
    @PostMapping("/{id}/estado")
    public String cambiarEstadoPedido(@PathVariable Long id,
                                     @RequestParam EstadoPedido estado,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {
        Usuario usuario = null;
        if (userDetails instanceof MyUserDetails myUserDetails) {
            usuario = myUserDetails.getUsuario();
        }
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error", "No se pudo obtener el usuario autenticado");
            return "redirect:/pedidos";
        }
        try {
            pedidoService.actualizarEstadoPedido(id, estado, usuario);
            redirectAttributes.addFlashAttribute("success", "Estado del pedido actualizado");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pedidos";
    }
    
    @GetMapping("/{id}/detalle")
    public String verDetallePedido(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return pedidoService.obtenerPedidoPorId(id)
                .map(pedido -> {
                    model.addAttribute("pedido", pedido);
                    return "detalle-pedido";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Pedido no encontrado");
                    return "redirect:/pedidos";
                });
    }

       @GetMapping("/{id}/editar")
    public String mostrarFormularioEditarPedido(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return pedidoService.obtenerPedidoPorId(id)
                .map(pedido -> {
                    if (pedido.getEstado() == EstadoPedido.COMPLETADO) {
                        redirectAttributes.addFlashAttribute("error", "No se puede editar un pedido COMPLETADO");
                        return "redirect:/pedidos";
                    }
                    model.addAttribute("pedido", pedido);
                    model.addAttribute("productos", productoService.obtenerProductosActivos());
                    return "editar-pedido";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Pedido no encontrado");
                    return "redirect:/pedidos";
                });
    }

    @PostMapping("/{id}/actualizar")
    public String actualizarPedido(@PathVariable Long id,
                                   @RequestParam List<Long> productos,
                                   @RequestParam List<Integer> cantidades,
                                   @RequestParam(required = false, defaultValue = "0.0") Double recargo,
                                   @RequestParam(required = false) String observaciones,
                                   @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        Usuario usuario = null;
        if (userDetails instanceof MyUserDetails myUserDetails) {
            usuario = myUserDetails.getUsuario();
        }
        try {
            Pedido pedido = pedidoService.obtenerPedidoPorId(id).orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
            if (pedido.getEstado() == EstadoPedido.COMPLETADO) {
                redirectAttributes.addFlashAttribute("error", "No se puede editar un pedido COMPLETADO");
                return "redirect:/pedidos";
            }
            pedido.setRecargo(recargo != null ? recargo : 0.0);
            pedido.setObservaciones(observaciones);
            pedidoService.actualizarDetallesPedido(pedido, productos, cantidades, usuario);
            redirectAttributes.addFlashAttribute("success", "Pedido actualizado correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/pedidos/{id}/editar";
        }
        return "redirect:/pedidos";
    }

    @GetMapping("/{id}/comprobante")
    public void generarComprobante(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Pedido pedido = pedidoService.obtenerPedidoPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=comprobante_pedido_" + id + ".pdf");

        // Tamaño boleta típico: 80mm x 200mm (en puntos: 1 pulgada = 72 puntos)
        float width = 226.77f; // 80mm
        float height = 566.93f; // 200mm
        Document document = new Document(new Rectangle(width, height), 10, 10, 10, 10);
        try {
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            // --- DATOS DE EMPRESA FICTICIOS ---
            Font fontTitle = new Font(Font.HELVETICA, 11, Font.BOLD);
            Font fontNormal = new Font(Font.HELVETICA, 7, Font.NORMAL);
            Font fontBold = new Font(Font.HELVETICA, 7, Font.BOLD);
            Paragraph nombreEmpresa = new Paragraph("ALOPOS S.A.C.", fontTitle);
            nombreEmpresa.setAlignment(Element.ALIGN_CENTER);
            document.add(nombreEmpresa);
            Paragraph ruc = new Paragraph("RUC: 12345678901", fontNormal);
            ruc.setAlignment(Element.ALIGN_CENTER);
            document.add(ruc);
            Paragraph direccion = new Paragraph("Av. Ficticia 123 - Lima, Perú", fontNormal);
            direccion.setAlignment(Element.ALIGN_CENTER);
            document.add(direccion);
            document.add(new Paragraph("----------------------------------------------------------------------------------------", fontNormal));
            Paragraph tipoDoc = new Paragraph("BOLETA DE VENTA ELECTRÓNICA", fontBold);
            tipoDoc.setAlignment(Element.ALIGN_CENTER);
            document.add(tipoDoc);
            document.add(new Paragraph("N°: B001-" + String.format("%06d", pedido.getId()), fontBold));
            document.add(new Paragraph("----------------------------------------------------------------------------------------", fontNormal));

            // --- DATOS DE FECHA Y ATENCIÓN ---
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            document.add(new Paragraph("Fecha y Hora: " + sdf.format(pedido.getFecha()), fontNormal));
            if (pedido.getMesa() != null) {
                document.add(new Paragraph("Mesa: " + pedido.getMesa().getNumero(), fontNormal));
            }
            document.add(new Paragraph("Atiende: " + (pedido.getUsuario() != null ? pedido.getUsuario().getNombre() : "-"), fontNormal));
            document.add(new Paragraph("----------------------------------------------------------------------------------------", fontNormal));

            // --- TABLA DE PRODUCTOS ---
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2, 8, 3, 4});
            table.addCell(new Phrase("Cant", fontBold));
            table.addCell(new Phrase("Descripción", fontBold));
            table.addCell(new Phrase("P.U.", fontBold));
            table.addCell(new Phrase("Importe", fontBold));
            double subtotal = 0.0;
            for (var det : pedido.getDetalles()) {
                table.addCell(new Phrase(String.valueOf(det.getCantidad()), fontNormal));
                table.addCell(new Phrase(det.getProducto().getNombre(), fontNormal));
                table.addCell(new Phrase(String.format("%.2f", det.getPrecioUnitario()), fontNormal));
                table.addCell(new Phrase(String.format("%.2f", det.getSubtotal()), fontNormal));
                subtotal += det.getSubtotal();
            }
            document.add(table);
            document.add(new Paragraph("----------------------------------------------------------------------------------------", fontNormal));

            // --- TOTALES ---
            double igv = subtotal * 0.18;
            double total = subtotal + pedido.getRecargo();
            document.add(new Paragraph(String.format("SUBTOTAL:   S/ %.2f", subtotal), fontNormal));
            document.add(new Paragraph(String.format("IGV (18%%):     S/ %.2f", igv), fontNormal));
            document.add(new Paragraph(String.format("RECARGO:    S/ %.2f", pedido.getRecargo()), fontNormal));
            document.add(new Paragraph(String.format("TOTAL:          S/ %.2f", pedido.getTotal()), fontBold));
            document.add(new Paragraph("----------------------------------------------------------------------------------------", fontNormal));

            // --- OBSERVACIONES ---
            if (pedido.getObservaciones() != null && !pedido.getObservaciones().isEmpty()) {
                document.add(new Paragraph("Obs: " + pedido.getObservaciones(), fontNormal));
            }

            document.add(new Paragraph(" "));
            Paragraph gracias = new Paragraph("¡Gracias por su compra!", fontNormal);
            gracias.setAlignment(Element.ALIGN_CENTER);
            document.add(gracias);
        } finally {
            document.close();
        }
    }


}