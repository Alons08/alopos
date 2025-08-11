package com.alocode.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.alocode.service.impl.UserDetailsServiceImpl;

@Configuration //esta anotación registra Beans en el contenedor de Spring Boot
@EnableWebSecurity //habilitar la seguridad en la app y poder usar la BD sin problemas
public class WebSecurityConfig {

    //SE USA EN EL FILTER CHAIN
    @Autowired
    private CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

    //Para tener al usuario en la fabrica de Spring
    @Bean
    public UserDetailsService userDetailsService(){
        return new UserDetailsServiceImpl();
    }

    //Para encriptar la contraseña del ususario
    @Bean
    public BCryptPasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    /*Haremos una implementacion del AuthenticationProvider que se utiliza
        comunmente para autenticar usuarios en BD. Además, es el responsable
         de verificar las credenciales del usuario y autenticar el usuario*/
    @Bean
    public DaoAuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService());
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    //Luego de lo de arriba tenemos que registrarlo en el manejador de autenticacion
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity httpSecurity) throws Exception {
        AuthenticationManagerBuilder authMB = httpSecurity.getSharedObject(AuthenticationManagerBuilder.class);
        //cuando se realiza una solicitud el administrador de autenticacion usara este provider
        authMB.authenticationProvider(authenticationProvider());
        return authMB.build();
    }

    //CREAMOS un FILTRO (define las reglas de autorizacion para las solicitudes HTTP)
    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                // Se definen las reglas de acceso a las rutas
                .authorizeHttpRequests(auth -> auth
                        //rutas públicas
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()

                        //rutas de pedidos y reportes
                        .requestMatchers("/pedidos/**", "/reportes/diario").hasAnyAuthority("SECRETARIA", "ADMIN")

                        //rutas de administración
                        .requestMatchers("/caja/**", "/productos/**", "/mesas/**", "/reportes/semanal", "/reportes/mensual").hasAuthority("ADMIN")
                        
                        // gestión de usuarios solo para ADMIN
                        .requestMatchers("/admin/usuarios/**").hasAuthority("ADMIN")


                        //todas las demás rutas requieren autenticación
                        .anyRequest().authenticated()
                )
                // Configura el manejo del inicio de sesión en la aplicación.
                .formLogin(form -> form
                        .loginPage("/login") /*ENNN "WebMvcConfigurer" vinculo el endpoint a un HTML*/
                        .failureHandler(customAuthenticationFailureHandler) //NUEVOOOOOOOOOOO
                        .defaultSuccessUrl("/home", true) //redirigir al home después del login
                        .permitAll() //permite el acceso al cierre de sesión para todos.
                )
                // Limita a una sola sesión activa por usuario
                .sessionManagement(session -> session
                    .maximumSessions(1)
                    .maxSessionsPreventsLogin(false) // Si es true, bloquea el nuevo login; si es false, cierra la sesión anterior
                )
                // Configura el manejo del cierre de sesión en la aplicación.
                .logout(logout -> logout
                        .logoutUrl("/logout")  //define la URL para realizar el cierre de sesión
                        .logoutSuccessUrl("/login?logout")  //redirige a login con parámetro de logout
                        .invalidateHttpSession(true)  //invalida la sesión
                        .deleteCookies("JSESSIONID")  //elimina cookies de sesión
                        .permitAll() //permite el acceso al cierre de sesión para todos.
                )
                // Configura el manejo de excepciones relacionadas con el acceso denegado
                .exceptionHandling(e -> e
                        .accessDeniedPage("/403") /*ENNN "WebMvcConfigurer" vinculo el endpoint a un HTML*/
                );

        return httpSecurity.build();
    }

}
