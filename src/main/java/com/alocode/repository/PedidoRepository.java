package com.alocode.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alocode.model.Pedido;
import com.alocode.model.enums.EstadoPedido;

import java.util.Date;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    
    List<Pedido> findByEstadoIn(List<EstadoPedido> estados);
    
    @Query("SELECT p FROM Pedido p WHERE p.estado = 'PENDIENTE' AND DATE(p.fecha) < CURRENT_DATE")
    List<Pedido> findPedidosPendientesDeDiasAnteriores();
    
    @Query("SELECT p FROM Pedido p WHERE p.caja.id = :cajaId AND p.estado = 'PENDIENTE'")
    List<Pedido> findPedidosPendientesPorCaja(@Param("cajaId") Long cajaId);
    
    @Query("SELECT p FROM Pedido p WHERE p.estado = 'COMPLETADO' AND DATE(p.fechaCompletado) BETWEEN :inicio AND :fin")
    List<Pedido> findPedidosCompletadosEntreFechas(@Param("inicio") Date inicio, @Param("fin") Date fin);
    
    List<Pedido> findByCajaIdAndEstado(Long cajaId, EstadoPedido estado);
    
    List<Pedido> findByEstadoAndFechaCompletadoBetween(EstadoPedido estado, java.time.LocalDateTime inicio, java.time.LocalDateTime fin);
}