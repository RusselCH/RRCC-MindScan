package com.ejemplo.rrccmindscan.model;

public enum RRCCTipoCambio {
    RRCC_IGUAL("Sin cambios", "rrcc-diff-igual"),
    RRCC_AGREGADO("Agregado", "rrcc-diff-agregado"),
    RRCC_ELIMINADO("Eliminado", "rrcc-diff-eliminado"),
    RRCC_MODIFICADO("Modificado", "rrcc-diff-modificado");

    private final String rrccEtiqueta;
    private final String rrccClaseCss;

    RRCCTipoCambio(String rrccEtiqueta, String rrccClaseCss) {
        this.rrccEtiqueta = rrccEtiqueta;
        this.rrccClaseCss = rrccClaseCss;
    }

    public String rrccEtiqueta() {
        return rrccEtiqueta;
    }

    public String rrccClaseCss() {
        return rrccClaseCss;
    }
}
