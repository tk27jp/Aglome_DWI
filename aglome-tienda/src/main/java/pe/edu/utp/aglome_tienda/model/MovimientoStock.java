package pe.edu.utp.aglome_tienda.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Una línea del historial de stock: qué pasó, cuánto cambió y cómo quedó.
 * Nunca se edita ni se borra: es el registro para saber por qué el stock es el que es.
 */
@Entity
@Table(name = "movimiento_stock")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "variante_id", nullable = false)
    private Variante variante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimiento tipo;

    /** Con signo: negativo si el stock bajó, positivo si subió. */
    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private Integer stockAnterior;

    @Column(nullable = false)
    private Integer stockResultante;

    @Column(length = 255)
    private String motivo;

    /** Pedido que originó el movimiento (ventas y cancelaciones). Sin FK a propósito: el historial no depende del pedido. */
    private UUID pedidoId;

    /** Quién lo hizo (email), si hay una sesión. */
    @Column(length = 150)
    private String usuarioEmail;

    @Column(nullable = false)
    private LocalDateTime fecha;
}
