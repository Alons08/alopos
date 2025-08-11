package com.alocode.security;

import com.alocode.model.Usuario;
import com.alocode.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;

@Component
public class LoginFailureListener implements ApplicationListener<AuthenticationFailureBadCredentialsEvent> {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public void onApplicationEvent(AuthenticationFailureBadCredentialsEvent event) {
        String username = (String) event.getAuthentication().getPrincipal();
        usuarioRepository.getUserByUsername(username).ifPresent(usuario -> {
            Date hoy = resetTime(new Date());
            Date ultimoIntento = usuario.getFechaUltimoIntento();
            if (ultimoIntento == null || !hoy.equals(ultimoIntento)) {
                usuario.setIntentosFallidos(1);
                usuario.setFechaUltimoIntento(hoy);
            } else {
                usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
            }
            if (usuario.getIntentosFallidos() >= 4) {
                usuario.setActivo(false);
            }
            usuarioRepository.save(usuario);
        });
    }

    private Date resetTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
