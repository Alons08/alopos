package com.alocode.model;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "productos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "El nombre es requerido")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
    private String nombre;
    
    @NotNull(message = "El precio es requerido")
    @DecimalMin(value = "0.0", message = "El precio debe ser mayor o igual a 0")
    private Double precio;
    
    @NotNull(message = "El stock es requerido")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stock;
    
    @Column(name = "stock_ocupado")
    private Integer stockOcupado = 0;
    
    @NotNull(message = "El estado activo es requerido")
    private Boolean activo = true;
}