package com.ejemplo.rrccmindscan.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rrcc_revision")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "rrccId")
public class RRCCRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rrcc_id")
    private Long rrccId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rrcc_documento_id", nullable = false)
    private RRCCDocumento rrccDocumento;

    @Column(name = "rrcc_fecha", nullable = false)
    private LocalDateTime rrccFecha;

    @Column(name = "rrcc_puntaje", nullable = false)
    private Integer rrccPuntaje;

    @Column(name = "rrcc_errores", nullable = false)
    private Integer rrccErrores;

    @Column(name = "rrcc_advertencias", nullable = false)
    private Integer rrccAdvertencias;

    @Column(name = "rrcc_sugerencias", nullable = false)
    private Integer rrccSugerencias;
}
