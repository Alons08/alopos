package com.alocode.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.alocode.model.Mesa;
import com.alocode.repository.MesaRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MesaService {
    private final MesaRepository mesaRepository;
    
    public List<Mesa> obtenerTodasLasMesas() {
        return mesaRepository.findAll();
    }

    public List<Mesa> buscarMesas(String q) {
        if (q == null || q.trim().isEmpty()) {
            return mesaRepository.findAll();
        }
        return mesaRepository.buscarPorNumeroOEstado(q.trim());
    }
    
    public List<Mesa> obtenerMesasDisponibles() {
        return mesaRepository.findMesasDisponibles();
    }
    
    public Mesa guardarMesa(Mesa mesa) {
        // Validar que el número de mesa sea único
        Optional<Mesa> existente = mesaRepository.findByNumero(mesa.getNumero());
        if (existente.isPresent() && (mesa.getId() == null || !existente.get().getId().equals(mesa.getId()))) {
            throw new IllegalArgumentException("Ya existe una mesa con el número " + mesa.getNumero());
        }
        return mesaRepository.save(mesa);
    }
    
    public void eliminarMesa(Long id) {
        mesaRepository.deleteById(id);
    }
    
    public Optional<Mesa> obtenerMesaPorNumero(Integer numero) {
        return mesaRepository.findByNumero(numero);
    }

    public Optional<Mesa> obtenerMesaPorId(Long id) {
        return mesaRepository.findById(id);
    }

        public void desactivarMesa(Long id) {
        Optional<Mesa> mesaOpt = mesaRepository.findById(id);
        if (mesaOpt.isPresent()) {
            Mesa mesa = mesaOpt.get();
            mesa.setEstado(com.alocode.model.enums.EstadoMesa.INACTIVA);
            mesaRepository.save(mesa);
        }
    }

    public void activarMesa(Long id) {
        Optional<Mesa> mesaOpt = mesaRepository.findById(id);
        if (mesaOpt.isPresent()) {
            Mesa mesa = mesaOpt.get();
            mesa.setEstado(com.alocode.model.enums.EstadoMesa.DISPONIBLE);
            mesaRepository.save(mesa);
        }
    }
    
}