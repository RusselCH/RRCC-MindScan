package com.ejemplo.rrccmindscan.model;

import java.util.Arrays;

public enum RRCCTipoDato {
    RRCC_CORREO("Correo"),
    RRCC_URL("Enlace web"),
    RRCC_TELEFONO("Teléfono"),
    RRCC_RUC("RUC"),
    RRCC_DNI("DNI"),
    RRCC_FECHA("Fecha"),
    RRCC_MONTO("Monto"),
    RRCC_PORCENTAJE("Porcentaje");

    private final String rrccEtiqueta;

    RRCCTipoDato(String rrccEtiqueta) {
        this.rrccEtiqueta = rrccEtiqueta;
    }

    public String rrccEtiqueta() {
        return rrccEtiqueta;
    }

    public static RRCCTipoDato rrccDesdeEtiqueta(String rrccTexto) {
        return Arrays.stream(values()).filter(rrccT -> rrccT.rrccEtiqueta.equals(rrccTexto)).findFirst().orElse(null);
    }
}
