package com.ejemplo.rrccmindscan.model;

public enum RRCCTipoHallazgo {
    RRCC_CONTRADICCION("Posible contradicción", RRCCSeveridad.RRCC_ERROR),
    RRCC_CIFRA_INCONSISTENTE("Cifras inconsistentes", RRCCSeveridad.RRCC_ERROR),
    RRCC_FECHA_INVALIDA("Fecha inválida", RRCCSeveridad.RRCC_ERROR),
    RRCC_PALABRA_REPETIDA("Palabra repetida", RRCCSeveridad.RRCC_ERROR),
    RRCC_SIGNOS_DESBALANCEADOS("Signos sin cerrar", RRCCSeveridad.RRCC_ADVERTENCIA),
    RRCC_PARRAFO_DUPLICADO("Párrafo duplicado", RRCCSeveridad.RRCC_ADVERTENCIA),
    RRCC_ORACION_LARGA("Oración muy larga", RRCCSeveridad.RRCC_ADVERTENCIA),
    RRCC_MAYUSCULA("Falta mayúscula inicial", RRCCSeveridad.RRCC_SUGERENCIA),
    RRCC_PUNTUACION("Espacio antes de signo", RRCCSeveridad.RRCC_SUGERENCIA),
    RRCC_ESPACIOS("Espacios múltiples", RRCCSeveridad.RRCC_SUGERENCIA);

    private final String rrccEtiqueta;
    private final RRCCSeveridad rrccSeveridad;

    RRCCTipoHallazgo(String rrccEtiqueta, RRCCSeveridad rrccSeveridad) {
        this.rrccEtiqueta = rrccEtiqueta;
        this.rrccSeveridad = rrccSeveridad;
    }

    public String rrccEtiqueta() {
        return rrccEtiqueta;
    }

    public RRCCSeveridad rrccSeveridad() {
        return rrccSeveridad;
    }
}
