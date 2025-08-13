package com.alocode.controller;

import com.alocode.model.Usuario;
import com.alocode.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final UsuarioRepository usuarioRepository;

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "username", required = false) String username,
                            Model model, HttpServletRequest request) {
        String mensaje = null;
        String advertencia = null;
        if (error != null && username != null) {
            Optional<Usuario> usuarioOpt = usuarioRepository.getUserByUsername(username);
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                if (!Boolean.TRUE.equals(usuario.getActivo())) {
                    mensaje = "El usuario está bloqueado. Contacte al administrador.";
                } else {
                    mensaje = "Contraseña incorrecta."; //contraseña incorrecta. /* Usuario o contraseña incorrectos. */
                    if (usuario.getIntentosFallidos() == 3) {
                        advertencia = "¡Atención! Si falla un intento más, su usuario será bloqueado";
                    }
                }
            } else {
                mensaje = "//El usuario no existe."; //El usuario no existe.
            }
        }
        model.addAttribute("mensaje", mensaje);
        model.addAttribute("advertencia", advertencia);
        model.addAttribute("username", username);
        return "login";
    }
}
