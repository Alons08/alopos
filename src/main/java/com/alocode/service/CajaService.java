package com.alocode.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alocode.model.Caja;
import com.alocode.model.Pedido;
import com.alocode.model.Usuario;
import com.alocode.model.enums.EstadoCaja;
import com.alocode.model.enums.EstadoPedido;
import com.alocode.repository.CajaRepository;
import com.alocode.repository.PedidoRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CajaService {
    private final CajaRepository cajaRepository;
    private final PedidoRepository pedidoRepository;
    
    @Transactional
    public Caja abrirCaja(Double montoApertura, Usuario usuario) {
        Date fechaActual = new Date();
        
        // Verificar si ya hay una caja abierta hoy
        if (cajaRepository.findByFechaAndEstado(fechaActual, EstadoCaja.ABIERTA).isPresent()) {
            throw new IllegalStateException("Ya hay una caja abierta hoy");
        }
        
        Caja caja = new Caja();
        caja.setFecha(fechaActual);
        caja.setMontoApertura(montoApertura);
        caja.setEstado(EstadoCaja.ABIERTA);
        caja.setUsuario(usuario);
        caja.setHoraApertura(fechaActual);
        
        return cajaRepository.save(caja);
    }
    
    @Transactional
    public Caja cerrarCaja(Long idCaja, Usuario usuario) {
        Caja caja = cajaRepository.findById(idCaja)
                .orElseThrow(() -> new IllegalArgumentException("Caja no encontrada"));
        
        if (caja.getEstado() == EstadoCaja.CERRADA) {
            throw new IllegalStateException("La caja ya está cerrada");
        }
        
        // Calcular total de ventas
        List<Pedido> pedidos = pedidoRepository.findByCajaIdAndEstado(idCaja, EstadoPedido.COMPLETADO);
        double totalVentas = pedidos.stream().mapToDouble(p -> p.getTotal() - p.getRecargo()).sum();
        double totalRecargos = pedidos.stream().mapToDouble(Pedido::getRecargo).sum();
        double totalNeto = totalVentas + totalRecargos;
        
        caja.setMontoCierre(caja.getMontoApertura() + totalNeto);
        caja.setEstado(EstadoCaja.CERRADA);
        caja.setHoraCierre(new Date());
        
        // Cancelar pedidos pendientes
        List<Pedido> pedidosPendientes = pedidoRepository.findPedidosPendientesPorCaja(idCaja);
        pedidosPendientes.forEach(p -> p.setEstado(EstadoPedido.CANCELADO));
        pedidoRepository.saveAll(pedidosPendientes);
        
        return cajaRepository.save(caja);
    }
    
    public Optional<Caja> obtenerCajaAbiertaHoy() {
        return cajaRepository.findCajaAbiertaHoy();
    }
    
    public List<Caja> obtenerCajasPorFecha(Date fecha) {
        return cajaRepository.findByFecha(fecha);
    }
}