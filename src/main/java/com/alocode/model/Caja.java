package com.alocode.model;


import lombok.*;
import jakarta.persistence.*;
import java.util.Date;

import com.alocode.model.enums.EstadoCaja;

@Entity
@Table(name = "caja")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Caja {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Temporal(TemporalType.DATE)
    private Date fecha;
    
    @Column(nullable = false)
    private Double montoApertura;
    
    private Double montoCierre;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCaja estado;
    
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @Temporal(TemporalType.TIME)
    private Date horaApertura;
    
    @Temporal(TemporalType.TIME)
    private Date horaCierre;
}