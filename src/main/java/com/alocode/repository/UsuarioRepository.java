package com.alocode.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alocode.model.Usuario;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);
    
    // Query personalizada para buscar un usuario por su username
    @Query("SELECT u FROM Usuario u WHERE u.username = :username")
    Optional<Usuario> getUserByUsername(@Param("username") String username);

    boolean existsByUsername(String username);

}