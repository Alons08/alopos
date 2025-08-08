package com.alocode.controller;

import com.alocode.model.Usuario;
import com.alocode.model.Rol;
import com.alocode.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequestMapping("/admin/usuarios")
public class UsuarioAdminController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public String listarUsuarios(Model model) {
        List<Usuario> usuarios = usuarioService.findAll();
        model.addAttribute("usuarios", usuarios);
        return "usuarios";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
    model.addAttribute("usuario", new Usuario());
    model.addAttribute("roles", usuarioService.getRolesSinAdmin());
    model.addAttribute("esAdmin", false);
    model.addAttribute("usuarioForm", new Usuario());
    model.addAttribute("roles", usuarioService.getRolesSinAdmin());
    model.addAttribute("esAdmin", false);
    return "usuario-form";
    }

    @PostMapping("/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario, @RequestParam(required = false) List<Long> rolesIds) {
        usuarioService.guardarUsuario(usuario, rolesIds);
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioService.findById(id);
        model.addAttribute("usuarioForm", usuario);
        model.addAttribute("roles", usuarioService.getRolesSinAdmin());
        model.addAttribute("esAdmin", usuarioService.tieneRolAdmin(usuario));
        return "usuario-form";
    }

    @PostMapping("/actualizar")
    public String actualizarUsuario(@ModelAttribute Usuario usuario, @RequestParam(required = false) List<Long> rolesIds) {
        usuarioService.actualizarUsuario(usuario, rolesIds);
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/cambiar-estado/{id}")
    public String cambiarEstado(@PathVariable Long id) {
        usuarioService.cambiarEstado(id);
        return "redirect:/admin/usuarios";
    }
}
