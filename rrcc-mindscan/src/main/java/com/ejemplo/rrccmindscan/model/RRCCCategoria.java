package com.ejemplo.rrccmindscan.model;

import java.util.Arrays;

public enum RRCCCategoria {
    RRCC_ACADEMICO("Académico", "rrcc-cat-academico", new String[]{
            "estudiante", "curso", "docente", "universidad", "silabo", "evaluacion", "investigacion", "tesis",
            "aprendizaje", "academico", "semestre", "practicas", "facultad", "asignatura", "calificacion"}),
    RRCC_LEGAL("Legal", "rrcc-cat-legal", new String[]{
            "contrato", "clausula", "partes", "obligaciones", "ley", "articulo", "reglamento", "penalidad",
            "resolucion", "firma", "derecho", "vigencia", "incumplimiento", "jurisdiccion", "estipula"}),
    RRCC_TECNICO("Técnico", "rrcc-cat-tecnico", new String[]{
            "servidor", "red", "sistema", "software", "configuracion", "equipo", "protocolo", "datos",
            "conexion", "hardware", "instalacion", "version", "router", "switch", "mantenimiento"}),
    RRCC_ADMINISTRATIVO("Administrativo", "rrcc-cat-administrativo", new String[]{
            "acta", "reunion", "acuerdo", "comite", "oficio", "solicitud", "memorando", "area", "gestion",
            "responsable", "informe", "jefatura", "asistentes", "agenda", "coordinacion"}),
    RRCC_FINANCIERO("Financiero", "rrcc-cat-financiero", new String[]{
            "presupuesto", "monto", "soles", "pago", "costo", "gasto", "inversion", "factura", "importe",
            "financiero", "tributario", "igv", "ingresos", "egresos", "balance"}),
    RRCC_GENERAL("General", "rrcc-cat-general", new String[]{});

    private final String rrccEtiqueta;
    private final String rrccClaseCss;
    private final String[] rrccPalabrasClave;

    RRCCCategoria(String rrccEtiqueta, String rrccClaseCss, String[] rrccPalabrasClave) {
        this.rrccEtiqueta = rrccEtiqueta;
        this.rrccClaseCss = rrccClaseCss;
        this.rrccPalabrasClave = rrccPalabrasClave;
    }

    public String rrccEtiqueta() {
        return rrccEtiqueta;
    }

    public String rrccClaseCss() {
        return rrccClaseCss;
    }

    public String[] rrccPalabrasClave() {
        return rrccPalabrasClave.clone();
    }

    public static RRCCCategoria rrccDesdeEtiqueta(String rrccTexto) {
        return Arrays.stream(values()).filter(rrccC -> rrccC.rrccEtiqueta.equals(rrccTexto)).findFirst().orElse(null);
    }
}
