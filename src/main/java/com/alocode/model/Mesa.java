package com.alocode.model;


import lombok.*;

import com.alocode.model.enums.EstadoMesa;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "mesas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Mesa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "El número es requerido")
    @Min(value = 1, message = "El número debe ser mayor a 0")
    @Column(unique = true, nullable = false)
    private Integer numero;
    
    @NotNull(message = "La capacidad es requerida")
    @Min(value = 1, message = "La capacidad debe ser mayor a 0")
    private Integer capacidad;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoMesa estado;
    
    @Size(max = 100, message = "La ubicación no puede exceder los 100 caracteres")
    private String ubicacion;
}