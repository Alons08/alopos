package com.alocode;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class SecurityPassword {
    public static void main(String[] args) {

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "alonsodev";
        String encoderPassword = encoder.encode(rawPassword);

        //para ver en consola la contraseña codificada nomás
        System.out.println("Contasena: "+encoderPassword);

    }
}