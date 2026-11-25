package com.ejemplo.rrccmindscan.model;

import java.util.Locale;

public enum RRCCTipoDocumento {
    RRCC_PDF("PDF", "rrcc-tipo-pdf"),
    RRCC_WORD("Word", "rrcc-tipo-word"),
    RRCC_TEXTO("Texto", "rrcc-tipo-texto");

    private final String rrccEtiqueta;
    private final String rrccClaseCss;

    RRCCTipoDocumento(String rrccEtiqueta, String rrccClaseCss) {
        this.rrccEtiqueta = rrccEtiqueta;
        this.rrccClaseCss = rrccClaseCss;
    }

    public String rrccEtiqueta() {
        return rrccEtiqueta;
    }

    public String rrccClaseCss() {
        return rrccClaseCss;
    }

    public static RRCCTipoDocumento rrccDesdeNombre(String rrccNombre) {
        String rrccMinusculas = rrccNombre.toLowerCase(Locale.ROOT);
        if (rrccMinusculas.endsWith(".pdf")) {
            return RRCC_PDF;
        }
        if (rrccMinusculas.endsWith(".docx")) {
            return RRCC_WORD;
        }
        if (rrccMinusculas.endsWith(".txt") || rrccMinusculas.endsWith(".md") || rrccMinusculas.endsWith(".csv")) {
            return RRCC_TEXTO;
        }
        return null;
    }
}
