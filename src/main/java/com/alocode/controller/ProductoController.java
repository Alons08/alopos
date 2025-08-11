
package com.alocode.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alocode.model.Producto;
import com.alocode.service.ProductoService;

import lombok.RequiredArgsConstructor;


@Controller
@RequestMapping("/productos")
@RequiredArgsConstructor
public class ProductoController {
    private final ProductoService productoService;

    @GetMapping
    public String listarProductos(@RequestParam(value = "q", required = false) String q, Model model) {
        if (q != null && !q.trim().isEmpty()) {
            model.addAttribute("productos", productoService.buscarProductosPorNombre(q));
        } else {
            model.addAttribute("productos", productoService.obtenerTodosLosProductos());
        }
        return "productos";
    }
    
    @GetMapping("/nuevo")
    public String mostrarFormularioNuevoProducto(Model model, @RequestParam(value = "id", required = false) Long id) {
        if (id != null) {
            model.addAttribute("producto", productoService.obtenerProductoPorId(id).orElse(new Producto()));
        } else {
            model.addAttribute("producto", new Producto());
        }
        return "nuevo-producto";
    }
    
    @PostMapping("/guardar")
    public String guardarProducto(@ModelAttribute Producto producto, RedirectAttributes redirectAttributes) {
        try {
            productoService.guardarProducto(producto);
            redirectAttributes.addFlashAttribute("success", "Producto guardado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/productos/nuevo";
        }
        return "redirect:/productos";
    }
    

    @PostMapping("/desactivar/{id}")
    public String desactivarProducto(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productoService.desactivarProducto(id);
            redirectAttributes.addFlashAttribute("success", "Producto desactivado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/productos";
    }
    
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditarProducto(@PathVariable Long id, Model model) {
        model.addAttribute("producto", productoService.obtenerProductoPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado")));
        return "nuevo-producto";
    }

    @PostMapping("/activar/{id}")
    public String activarProducto(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productoService.activarProducto(id);
            redirectAttributes.addFlashAttribute("success", "Producto activado exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/productos";
    }

}