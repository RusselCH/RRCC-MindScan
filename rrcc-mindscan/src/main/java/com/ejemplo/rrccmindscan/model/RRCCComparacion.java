package com.ejemplo.rrccmindscan.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rrcc_comparacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "rrccId")
public class RRCCComparacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rrcc_id")
    private Long rrccId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rrcc_documento_a_id", nullable = false)
    private RRCCDocumento rrccDocumentoA;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rrcc_documento_b_id", nullable = false)
    private RRCCDocumento rrccDocumentoB;

    @Column(name = "rrcc_fecha", nullable = false)
    private LocalDateTime rrccFecha;

    @Column(name = "rrcc_similitud", nullable = false)
    private Double rrccSimilitud;

    @Column(name = "rrcc_iguales", nullable = false)
    private Integer rrccIguales;

    @Column(name = "rrcc_agregadas", nullable = false)
    private Integer rrccAgregadas;

    @Column(name = "rrcc_eliminadas", nullable = false)
    private Integer rrccEliminadas;

    @Column(name = "rrcc_modificadas", nullable = false)
    private Integer rrccModificadas;
}
