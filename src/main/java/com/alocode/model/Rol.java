package com.alocode.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Entity
@Table(name = "roles", uniqueConstraints = {
        @UniqueConstraint(columnNames = "nombre")
})
public class Rol {

    @Id
    @Column(name = "id_rol")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "El nombre del rol es obligatorio")
    @Size(max = 30, message = "El nombre del rol no puede exceder los 30 caracteres")
    private String nombre;

    private String descripcion;

}