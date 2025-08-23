package com.alocode.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alocode.model.Producto;
import com.alocode.repository.ProductoRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductoService {
    private final ProductoRepository productoRepository;
    
    public List<Producto> obtenerTodosLosProductos() {
        return productoRepository.findAllByOrderByIdAsc();
    }
    
    public List<Producto> buscarProductosPorNombre(String nombre) {
        return productoRepository.buscarPorNombre(nombre);
    }
    
    public Optional<Producto> obtenerProductoPorId(Long id) {
        return productoRepository.findById(id);
    }
    
    public Producto guardarProducto(Producto producto) {
        // Si es producto derivado, validar que tenga producto base y factor de conversión
        if (producto.getProductoBase() != null) {
            if (producto.getFactorConversion() == null) {
                throw new IllegalArgumentException("Los productos derivados deben tener un factor de conversión");
            }
            // No permitir stock propio en productos derivados
            producto.setStock(0.0);
            producto.setStockOcupado(0.0);
        }
        return productoRepository.save(producto);
    }
    
    public void eliminarProducto(Long id) {
        productoRepository.deleteById(id);
    }

    public void desactivarProducto(Long id) {
        Optional<Producto> productoOpt = productoRepository.findById(id);
        if (productoOpt.isPresent()) {
            Producto producto = productoOpt.get();
            producto.setActivo(false);
            productoRepository.save(producto);
        }
    }
    
    public List<Producto> obtenerProductosActivos() {
        return productoRepository.findByActivoTrue();
    }

    public void activarProducto(Long id) {
        Optional<Producto> productoOpt = productoRepository.findById(id);
        if (productoOpt.isPresent()) {
            Producto producto = productoOpt.get();
            producto.setActivo(true);
            productoRepository.save(producto);
        }
    }
    
    public List<Producto> obtenerProductosBase() {
        return productoRepository.findByEsProductoBaseTrueAndActivoTrue();
    }
    
    public List<Producto> obtenerProductosDerivados() {
        return productoRepository.findByProductoBaseIsNotNullAndActivoTrue();
    }
    
    public boolean verificarStockDisponible(Producto producto, Integer cantidad) {
        if (producto.getProductoBase() != null) {
            // Producto derivado - verificar stock del producto base
            Producto productoBase = producto.getProductoBase();
            double cantidadBaseNecesaria = cantidad * producto.getFactorConversion();
            return (productoBase.getStock() - productoBase.getStockOcupado()) >= cantidadBaseNecesaria;
        } else {
            // Producto normal - verificar su propio stock
            return (producto.getStock() - producto.getStockOcupado()) >= cantidad;
        }
    }
    
    @Transactional
    public void reservarStock(Producto producto, Integer cantidad) {
        if (producto.getProductoBase() != null) {
            // Producto derivado - reservar stock del producto base
            Producto productoBase = producto.getProductoBase();
            double cantidadBaseReservar = cantidad * producto.getFactorConversion();
            if ((productoBase.getStock() - productoBase.getStockOcupado()) < cantidadBaseReservar) {
                throw new IllegalStateException("Stock insuficiente del producto base: " + productoBase.getNombre());
            }
            productoBase.setStockOcupado(productoBase.getStockOcupado() + cantidadBaseReservar);
            productoRepository.save(productoBase);
        } else {
            // Producto normal - reservar su propio stock
            if ((producto.getStock() - producto.getStockOcupado()) < cantidad) {
                throw new IllegalStateException("Stock insuficiente para el producto: " + producto.getNombre());
            }
            producto.setStockOcupado(producto.getStockOcupado() + cantidad);
            productoRepository.save(producto);
        }
    }
    
    @Transactional
    public void liberarStockReservado(Producto producto, Integer cantidad) {
        if (producto.getProductoBase() != null) {
            // Producto derivado - liberar stock del producto base
            Producto productoBase = producto.getProductoBase();
            double cantidadBaseLiberar = cantidad * producto.getFactorConversion();
            productoBase.setStockOcupado(Math.max(0.0, productoBase.getStockOcupado() - cantidadBaseLiberar));
            productoRepository.save(productoBase);
        } else {
            // Producto normal - liberar su propio stock
            producto.setStockOcupado(Math.max(0.0, producto.getStockOcupado() - cantidad));
            productoRepository.save(producto);
        }
    }
    
    @Transactional
    public void consumirStock(Producto producto, Integer cantidad) {
        if (producto.getProductoBase() != null) {
            // Producto derivado - consumir stock del producto base
            Producto productoBase = producto.getProductoBase();
            double cantidadBaseConsumir = cantidad * producto.getFactorConversion();
            // Liberar primero el stock reservado
            productoBase.setStockOcupado(Math.max(0.0, productoBase.getStockOcupado() - cantidadBaseConsumir));
            // Reducir el stock real
            productoBase.setStock(Math.max(0.0, productoBase.getStock() - cantidadBaseConsumir));
            productoRepository.save(productoBase);
        } else {
            // Producto normal - consumir su propio stock
            producto.setStockOcupado(Math.max(0.0, producto.getStockOcupado() - cantidad));
            producto.setStock(Math.max(0.0, producto.getStock() - cantidad));
            productoRepository.save(producto);
        }
    }
}