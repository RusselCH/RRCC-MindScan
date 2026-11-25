package com.ejemplo.rrccmindscan.model;

public enum RRCCSeveridad {
    RRCC_ERROR("Error", "rrcc-sev-error", 8),
    RRCC_ADVERTENCIA("Advertencia", "rrcc-sev-advertencia", 3),
    RRCC_SUGERENCIA("Sugerencia", "rrcc-sev-sugerencia", 1);

    private final String rrccEtiqueta;
    private final String rrccClaseCss;
    private final int rrccPeso;

    RRCCSeveridad(String rrccEtiqueta, String rrccClaseCss, int rrccPeso) {
        this.rrccEtiqueta = rrccEtiqueta;
        this.rrccClaseCss = rrccClaseCss;
        this.rrccPeso = rrccPeso;
    }

    public String rrccEtiqueta() {
        return rrccEtiqueta;
    }

    public String rrccClaseCss() {
        return rrccClaseCss;
    }

    public int rrccPeso() {
        return rrccPeso;
    }
}
