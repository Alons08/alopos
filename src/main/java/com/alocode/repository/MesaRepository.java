package com.alocode.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alocode.model.Mesa;
import com.alocode.model.enums.EstadoMesa;

import java.util.List;
import java.util.Optional;

@Repository
public interface MesaRepository extends JpaRepository<Mesa, Long> {

    List<Mesa> findAllByOrderByIdAsc();
    List<Mesa> findByEstado(EstadoMesa estado);
    
    @Query("SELECT m FROM Mesa m WHERE m.estado = 'DISPONIBLE' ORDER BY m.numero")
    List<Mesa> findMesasDisponibles();
    
    Optional<Mesa> findByNumero(Integer numero);

    @Query("SELECT m FROM Mesa m WHERE (:q IS NULL OR CAST(m.numero AS string) LIKE %:q% OR LOWER(m.estado) LIKE LOWER(CONCAT('%', :q, '%')) ) ORDER BY m.numero")
    List<Mesa> buscarPorNumeroOEstado(@Param("q") String q);

}