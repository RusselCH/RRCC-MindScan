package com.ejemplo.rrccmindscan.ui;

import com.ejemplo.rrccmindscan.model.RRCCCategoria;
import com.ejemplo.rrccmindscan.model.RRCCDocumento;
import com.ejemplo.rrccmindscan.model.RRCCSeveridad;
import com.ejemplo.rrccmindscan.model.RRCCTipoDocumento;
import javafx.scene.control.Label;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class RRCCFormatos {

    public static final Locale RRCC_LOCALE = Locale.forLanguageTag("es-PE");
    public static final DateTimeFormatter RRCC_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", RRCC_LOCALE);

    private RRCCFormatos() {
    }

    public static String rrccNumero(long rrccValor) {
        return NumberFormat.getIntegerInstance(RRCC_LOCALE).format(rrccValor);
    }

    public static String rrccPorcentaje(double rrccFraccion) {
        return Math.round(rrccFraccion * 100) + " %";
    }

    public static String rrccMinutos(double rrccMinutos) {
        if (rrccMinutos < 1) {
            return Math.max(1, Math.round(rrccMinutos * 60)) + " s";
        }
        if (rrccMinutos < 60) {
            return Math.round(rrccMinutos) + " min";
        }
        long rrccHoras = (long) (rrccMinutos / 60);
        return rrccHoras + " h " + Math.round(rrccMinutos - rrccHoras * 60) + " min";
    }

    public static String rrccBytes(long rrccBytes) {
        if (rrccBytes < 1024) {
            return rrccBytes + " B";
        }
        if (rrccBytes < 1024 * 1024) {
            return Math.round(rrccBytes / 1024.0) + " KB";
        }
        return String.format("%.1f MB", rrccBytes / (1024.0 * 1024.0));
    }

    public static Label rrccChip(String rrccTexto, String rrccClase) {
        Label rrccChip = new Label(rrccTexto);
        rrccChip.getStyleClass().addAll("rrcc-chip", rrccClase);
        return rrccChip;
    }

    public static Label rrccChipCategoria(RRCCCategoria rrccCategoria) {
        return rrccChip(rrccCategoria.rrccEtiqueta(), rrccCategoria.rrccClaseCss());
    }

    public static Label rrccChipTipo(RRCCTipoDocumento rrccTipo) {
        return rrccChip(rrccTipo.rrccEtiqueta(), rrccTipo.rrccClaseCss());
    }

    public static Label rrccChipSeveridad(RRCCSeveridad rrccSeveridad) {
        return rrccChip(rrccSeveridad.rrccEtiqueta(), rrccSeveridad.rrccClaseCss());
    }

    public static String rrccClaseLegibilidad(double rrccLegibilidad) {
        if (rrccLegibilidad >= 60) {
            return "rrcc-bueno";
        }
        if (rrccLegibilidad >= 40) {
            return "rrcc-medio";
        }
        return "rrcc-malo";
    }

    public static String rrccClasePuntaje(int rrccPuntaje) {
        if (rrccPuntaje >= 85) {
            return "rrcc-bueno";
        }
        if (rrccPuntaje >= 60) {
            return "rrcc-medio";
        }
        return "rrcc-malo";
    }

    public static String rrccEtiquetaDocumento(RRCCDocumento rrccDoc) {
        return rrccDoc.getRrccNombre() + "   (" + rrccDoc.getRrccTipo().rrccEtiqueta() + ", "
                + rrccNumero(rrccDoc.getRrccPalabras()) + " palabras)";
    }
}
