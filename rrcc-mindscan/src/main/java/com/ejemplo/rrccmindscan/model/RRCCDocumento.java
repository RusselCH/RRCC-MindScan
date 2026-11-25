package com.ejemplo.rrccmindscan.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rrcc_documento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "rrccId")
public class RRCCDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rrcc_id")
    private Long rrccId;

    @Column(name = "rrcc_nombre", nullable = false, length = 255)
    private String rrccNombre;

    @Column(name = "rrcc_ruta", length = 1024)
    private String rrccRuta;

    @Enumerated(EnumType.STRING)
    @Column(name = "rrcc_tipo", nullable = false, length = 16)
    private RRCCTipoDocumento rrccTipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "rrcc_categoria", nullable = false, length = 24)
    private RRCCCategoria rrccCategoria;

    @Column(name = "rrcc_tamano_bytes", nullable = false)
    private Long rrccTamanoBytes;

    @Column(name = "rrcc_paginas", nullable = false)
    private Integer rrccPaginas;

    @Column(name = "rrcc_palabras", nullable = false)
    private Integer rrccPalabras;

    @Column(name = "rrcc_oraciones", nullable = false)
    private Integer rrccOraciones;

    @Column(name = "rrcc_legibilidad", nullable = false)
    private Double rrccLegibilidad;

    @Column(name = "rrcc_hash", nullable = false, length = 64)
    private String rrccHash;

    @Column(name = "rrcc_fecha_importacion", nullable = false)
    private LocalDateTime rrccFechaImportacion;

    @Column(name = "rrcc_contenido", nullable = false, columnDefinition = "TEXT")
    private String rrccContenido;

    @Column(name = "rrcc_favorito")
    private Boolean rrccFavorito;

    @Column(name = "rrcc_notas", columnDefinition = "TEXT")
    private String rrccNotas;

    public boolean rrccEsFavorito() {
        return Boolean.TRUE.equals(rrccFavorito);
    }

    public String rrccTamanoLegible() {
        if (rrccTamanoBytes < 1024) {
            return rrccTamanoBytes + " B";
        }
        if (rrccTamanoBytes < 1024 * 1024) {
            return Math.round(rrccTamanoBytes / 1024.0) + " KB";
        }
        return String.format("%.1f MB", rrccTamanoBytes / (1024.0 * 1024.0));
    }
}
