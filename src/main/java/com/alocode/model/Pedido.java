package com.alocode.model;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import com.alocode.model.enums.EstadoPedido;
import com.alocode.model.enums.TipoPedido;

@Entity
@Table(name = "pedidos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date fecha;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPedido tipo;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPedido estado;
    
    @ManyToOne
    @JoinColumn(name = "mesa_id")
    private Mesa mesa;
    
    @Column(nullable = false)
    private Double total;
    
    @Column(nullable = false)
    private Double recargo;
    
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @ManyToOne
    @JoinColumn(name = "caja_id", nullable = false)
    private Caja caja;
    
    @Column(length = 500)
    private String observaciones;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaCompletado;
    
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles = new java.util.ArrayList<>();
    
    @ManyToOne
    @JoinColumn(name = "usuario_completado_id")
    private Usuario usuarioCompletado;

    public Usuario getUsuarioCompletado() {
        return usuarioCompletado;
    }

    public void setUsuarioCompletado(Usuario usuarioCompletado) {
        this.usuarioCompletado = usuarioCompletado;
    }
}