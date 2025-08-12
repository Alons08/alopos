
package com.alocode.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}