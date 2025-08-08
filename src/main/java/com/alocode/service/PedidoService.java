package com.alocode.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alocode.model.Caja;
import com.alocode.model.DetallePedido;
import com.alocode.model.Mesa;
import com.alocode.model.Pedido;
import com.alocode.model.Producto;
import com.alocode.model.Usuario;
import com.alocode.model.enums.EstadoMesa;
import com.alocode.model.enums.EstadoPedido;
import com.alocode.model.enums.TipoPedido;
import com.alocode.repository.CajaRepository;
import com.alocode.repository.MesaRepository;
import com.alocode.repository.PedidoRepository;
import com.alocode.repository.ProductoRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final MesaRepository mesaRepository;
    private final CajaRepository cajaRepository;

    @Transactional
    public Pedido crearPedido(Pedido pedido, List<DetallePedido> detalles, Usuario usuario) {
        // Validar caja abierta
        Caja caja = cajaRepository.findCajaAbiertaHoy()
                .orElseThrow(() -> new IllegalStateException("No hay caja abierta hoy"));

        // Validar y procesar detalles
        for (DetallePedido detalle : detalles) {
            Producto producto = productoRepository.findById(detalle.getProducto().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

            if (producto.getStock() - producto.getStockOcupado() < detalle.getCantidad()) {
                throw new IllegalStateException("Stock insuficiente para el producto: " + producto.getNombre());
            }

            // Reservar stock
            producto.setStockOcupado(producto.getStockOcupado() + detalle.getCantidad());
            productoRepository.save(producto);

            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(producto.getPrecio() * detalle.getCantidad());
            detalle.setPedido(pedido);
        }

        // Calcular total
        double subtotal = detalles.stream().mapToDouble(DetallePedido::getSubtotal).sum();
        pedido.setTotal(subtotal + pedido.getRecargo());

        // Asignar caja y usuario
        pedido.setCaja(caja);
        pedido.setUsuario(usuario);
        pedido.setFecha(new Date());
        pedido.setEstado(EstadoPedido.PENDIENTE);

        // Si es pedido en mesa, actualizar estado de la mesa
        if (pedido.getTipo() == TipoPedido.MESA && pedido.getMesa() != null) {
            Mesa mesa = mesaRepository.findById(pedido.getMesa().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Mesa no encontrada"));
            mesa.setEstado(EstadoMesa.OCUPADA);
            mesaRepository.save(mesa);
        }

        pedido.setDetalles(detalles);
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido actualizarEstadoPedido(Long idPedido, EstadoPedido nuevoEstado, Usuario usuario) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));

        // Validar transición de estado
        if (pedido.getEstado() != EstadoPedido.PENDIENTE &&
                pedido.getEstado() != EstadoPedido.PREPARANDO &&
                pedido.getEstado() != EstadoPedido.LISTO) {
            throw new IllegalStateException("No se puede cambiar el estado de un pedido completado o cancelado");
        }

        // Procesar cambio de estado
        if (nuevoEstado == EstadoPedido.COMPLETADO) {
            // Liberar stock ocupado y descontar stock real
            for (DetallePedido detalle : pedido.getDetalles()) {
                Producto producto = detalle.getProducto();
                producto.setStock(producto.getStock() - detalle.getCantidad());
                producto.setStockOcupado(producto.getStockOcupado() - detalle.getCantidad());
                productoRepository.save(producto);
            }

            // Si es pedido en mesa, liberar mesa
            if (pedido.getTipo() == TipoPedido.MESA && pedido.getMesa() != null) {
                Mesa mesa = pedido.getMesa();
                mesa.setEstado(EstadoMesa.DISPONIBLE);
                mesaRepository.save(mesa);
            }

            pedido.setFechaCompletado(new Date());
            pedido.setUsuarioCompletado(usuario);
        } else if (nuevoEstado == EstadoPedido.CANCELADO) {
            // Solo liberar stock ocupado
            for (DetallePedido detalle : pedido.getDetalles()) {
                Producto producto = detalle.getProducto();
                producto.setStockOcupado(producto.getStockOcupado() - detalle.getCantidad());
                productoRepository.save(producto);
            }

            // Si es pedido en mesa, liberar mesa
            if (pedido.getTipo() == TipoPedido.MESA && pedido.getMesa() != null) {
                Mesa mesa = pedido.getMesa();
                mesa.setEstado(EstadoMesa.DISPONIBLE);
                mesaRepository.save(mesa);
            }
        }

        pedido.setEstado(nuevoEstado);
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public void cancelarPedidosPendientesDeDiasAnteriores() {
        List<Pedido> pedidos = pedidoRepository.findPedidosPendientesDeDiasAnteriores();
        pedidos.forEach(p -> {
            p.setEstado(EstadoPedido.CANCELADO);
            // Liberar stock ocupado
            p.getDetalles().forEach(d -> {
                Producto producto = d.getProducto();
                producto.setStockOcupado(producto.getStockOcupado() - d.getCantidad());
                productoRepository.save(producto);
            });
        });
        pedidoRepository.saveAll(pedidos);
    }

    public List<Pedido> obtenerPedidosPendientes() {
        return pedidoRepository.findByEstadoIn(List.of(
                EstadoPedido.PENDIENTE,
                EstadoPedido.PREPARANDO,
                EstadoPedido.LISTO));
    }

    public List<Pedido> obtenerPedidosCompletados() {
        return pedidoRepository.findByEstadoIn(List.of(EstadoPedido.COMPLETADO));
    }

    public Optional<Pedido> obtenerPedidoPorId(Long id) {
        return pedidoRepository.findById(id);
    }

    @Transactional
    public void actualizarDetallesPedido(Pedido pedido, List<Long> productos, List<Integer> cantidades,
            Usuario usuario) {
        if (pedido.getEstado() == EstadoPedido.COMPLETADO) {
            throw new IllegalStateException("No se puede editar un pedido COMPLETADO");
        }
        // Liberar stock ocupado de los productos actuales
        for (DetallePedido detalle : pedido.getDetalles()) {
            Producto producto = detalle.getProducto();
            producto.setStockOcupado(producto.getStockOcupado() - detalle.getCantidad());
            productoRepository.save(producto);
        }
        pedido.getDetalles().clear();
        // Agregar los nuevos detalles
        for (int i = 0; i < productos.size(); i++) {
            Producto producto = productoRepository.findById(productos.get(i))
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
            if (producto.getStock() - producto.getStockOcupado() < cantidades.get(i)) {
                throw new IllegalStateException("Stock insuficiente para el producto: " + producto.getNombre());
            }
            producto.setStockOcupado(producto.getStockOcupado() + cantidades.get(i));
            productoRepository.save(producto);
            DetallePedido detalle = new DetallePedido();
            detalle.setProducto(producto);
            detalle.setCantidad(cantidades.get(i));
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(producto.getPrecio() * cantidades.get(i));
            detalle.setPedido(pedido);
            pedido.getDetalles().add(detalle);
        }
        // Recalcular total
        double subtotal = pedido.getDetalles().stream().mapToDouble(DetallePedido::getSubtotal).sum();
        pedido.setTotal(subtotal + pedido.getRecargo());
        pedidoRepository.save(pedido);
    }

    public List<Pedido> obtenerPedidosPorCajaYEstado(Long cajaId, EstadoPedido estado) {
        return pedidoRepository.findByCajaIdAndEstado(cajaId, estado);
    }
}