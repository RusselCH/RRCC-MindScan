package com.ejemplo.rrccmindscan.service;

import com.ejemplo.rrccmindscan.model.RRCCDocumento;
import com.ejemplo.rrccmindscan.model.RRCCSeveridad;
import com.ejemplo.rrccmindscan.model.RRCCTipoCambio;
import com.ejemplo.rrccmindscan.service.RRCCAnalizadorService.RRCCDato;
import com.ejemplo.rrccmindscan.service.RRCCAnalizadorService.RRCCOracionPuntuada;
import com.ejemplo.rrccmindscan.service.RRCCAnalizadorService.RRCCTermino;
import com.ejemplo.rrccmindscan.service.RRCCComparadorService.RRCCBloqueDiff;
import com.ejemplo.rrccmindscan.service.RRCCComparadorService.RRCCFragmentoDiff;
import com.ejemplo.rrccmindscan.service.RRCCComparadorService.RRCCResultadoComparacion;
import com.ejemplo.rrccmindscan.service.RRCCRevisorService.RRCCHallazgo;
import com.ejemplo.rrccmindscan.service.RRCCRevisorService.RRCCResultadoRevision;
import com.ejemplo.rrccmindscan.service.RRCCTextoService.RRCCEstadisticas;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class RRCCReporteService {

    public static final DateTimeFormatter RRCC_FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public static final String RRCC_COLOR_TITULO = "1F3A5F";
    public static final String RRCC_COLOR_AGREGADO = "1A7F37";
    public static final String RRCC_COLOR_ELIMINADO = "CF222E";
    public static final String RRCC_COLOR_MODIFICADO = "9A6700";
    public static final String RRCC_COLOR_GRIS = "57606A";
    public static final String RRCC_FUENTE = "Calibri";

    public void rrccExportarAnalisis(RRCCDocumento rrccDoc, RRCCEstadisticas rrccEst, List<RRCCTermino> rrccTerminos,
                                     List<RRCCOracionPuntuada> rrccResumen, List<RRCCDato> rrccDatos, File rrccDestino) throws IOException {
        try (XWPFDocument rrccWord = new XWPFDocument(); OutputStream rrccSalida = new FileOutputStream(rrccDestino)) {
            rrccTitulo(rrccWord, "Informe de análisis de documento");
            rrccTexto(rrccWord, "Generado por RRCC MindScan el " + LocalDateTime.now().format(RRCC_FORMATO), RRCC_COLOR_GRIS, false, 9);

            rrccSubtitulo(rrccWord, "1. Ficha del documento");
            rrccTabla(rrccWord, new String[][]{
                    {"Nombre", rrccDoc.getRrccNombre()},
                    {"Tipo", rrccDoc.getRrccTipo().rrccEtiqueta()},
                    {"Categoría", rrccDoc.getRrccCategoria().rrccEtiqueta()},
                    {"Tamaño", rrccDoc.rrccTamanoLegible()},
                    {"Páginas", String.valueOf(rrccDoc.getRrccPaginas())},
                    {"Importado", rrccDoc.getRrccFechaImportacion().format(RRCC_FORMATO)}});

            rrccSubtitulo(rrccWord, "2. Estadísticas de lectura");
            rrccTabla(rrccWord, new String[][]{
                    {"Palabras", String.valueOf(rrccEst.rrccPalabras())},
                    {"Palabras distintas", String.valueOf(rrccEst.rrccPalabrasUnicas())},
                    {"Oraciones", String.valueOf(rrccEst.rrccOraciones())},
                    {"Párrafos", String.valueOf(rrccEst.rrccParrafos())},
                    {"Palabras por oración", String.format("%.1f", rrccEst.rrccPalabrasPorOracion())},
                    {"Índice de legibilidad (Fernández-Huerta)", String.format("%.1f - %s", rrccEst.rrccLegibilidad(), rrccEst.rrccNivel())},
                    {"Tiempo de lectura estimado", String.format("%.1f min", rrccEst.rrccMinutosLectura())}});

            rrccSubtitulo(rrccWord, "3. Palabras clave (TF-IDF)");
            StringBuilder rrccClaves = new StringBuilder();
            for (RRCCTermino rrccT : rrccTerminos) {
                if (!rrccClaves.isEmpty()) {
                    rrccClaves.append("  |  ");
                }
                rrccClaves.append(rrccT.rrccForma()).append(" (").append(rrccT.rrccFrecuencia()).append(")");
            }
            rrccTexto(rrccWord, rrccClaves.toString(), null, false, 11);

            rrccSubtitulo(rrccWord, "4. Resumen extractivo (TextRank)");
            for (RRCCOracionPuntuada rrccO : rrccResumen) {
                if (rrccO.rrccSeleccionada()) {
                    XWPFParagraph rrccP = rrccWord.createParagraph();
                    rrccP.setSpacingAfter(120);
                    rrccRun(rrccP, "- ", RRCC_COLOR_TITULO, true, 11);
                    rrccRun(rrccP, rrccO.rrccTexto(), null, false, 11);
                }
            }

            rrccSubtitulo(rrccWord, "5. Datos extraídos");
            if (rrccDatos.isEmpty()) {
                rrccTexto(rrccWord, "No se encontraron correos, teléfonos, montos, fechas ni identificadores.", RRCC_COLOR_GRIS, false, 11);
            } else {
                String[][] rrccFilas = new String[rrccDatos.size() + 1][];
                rrccFilas[0] = new String[]{"Tipo", "Valor", "Veces", "Contexto"};
                for (int rrccI = 0; rrccI < rrccDatos.size(); rrccI++) {
                    RRCCDato rrccD = rrccDatos.get(rrccI);
                    rrccFilas[rrccI + 1] = new String[]{rrccD.rrccTipo().rrccEtiqueta(), rrccD.rrccValor(),
                            String.valueOf(rrccD.rrccOcurrencias()), rrccD.rrccContexto()};
                }
                rrccTabla(rrccWord, rrccFilas);
            }
            rrccWord.write(rrccSalida);
        }
    }

    public void rrccExportarTexto(String rrccTitulo, String rrccContenido, File rrccDestino) throws IOException {
        try (XWPFDocument rrccWord = new XWPFDocument(); OutputStream rrccSalida = new FileOutputStream(rrccDestino)) {
            rrccTitulo(rrccWord, rrccTitulo);
            for (String rrccParrafo : rrccContenido.split("\\n\\s*\\n")) {
                if (!rrccParrafo.isBlank()) {
                    XWPFParagraph rrccP = rrccWord.createParagraph();
                    rrccP.setSpacingAfter(160);
                    rrccRun(rrccP, rrccParrafo.replace('\n', ' ').strip(), null, false, 11);
                }
            }
            rrccWord.write(rrccSalida);
        }
    }

    public void rrccExportarComparacion(RRCCDocumento rrccA, RRCCDocumento rrccB, RRCCResultadoComparacion rrccR,
                                        boolean rrccSoloCambios, File rrccDestino) throws IOException {
        try (XWPFDocument rrccWord = new XWPFDocument(); OutputStream rrccSalida = new FileOutputStream(rrccDestino)) {
            rrccTitulo(rrccWord, "Informe de comparación de versiones");
            rrccTexto(rrccWord, "Generado por RRCC MindScan el " + LocalDateTime.now().format(RRCC_FORMATO), RRCC_COLOR_GRIS, false, 9);
            rrccTabla(rrccWord, new String[][]{
                    {"Version original (A)", rrccA.getRrccNombre()},
                    {"Version nueva (B)", rrccB.getRrccNombre()},
                    {"Similitud global", Math.round(rrccR.rrccSimilitudGlobal() * 100) + " %"},
                    {"Similitud de vocabulario (coseno)", Math.round(rrccR.rrccSimilitudCoseno() * 100) + " %"},
                    {"Similitud de frases (Jaccard 3-gramas)", Math.round(rrccR.rrccSimilitudTejas() * 100) + " %"},
                    {"Oraciones sin cambios", String.valueOf(rrccR.rrccIguales())},
                    {"Oraciones agregadas", String.valueOf(rrccR.rrccAgregadas())},
                    {"Oraciones eliminadas", String.valueOf(rrccR.rrccEliminadas())},
                    {"Oraciones modificadas", String.valueOf(rrccR.rrccModificadas())},
                    {"Términos nuevos en B", String.join(", ", rrccR.rrccTerminosNuevos())},
                    {"Términos que ya no aparecen", String.join(", ", rrccR.rrccTerminosRetirados())}});

            rrccSubtitulo(rrccWord, "Detalle de cambios");
            XWPFParagraph rrccLeyenda = rrccWord.createParagraph();
            rrccRun(rrccLeyenda, "[+] agregado   ", RRCC_COLOR_AGREGADO, true, 9);
            rrccRun(rrccLeyenda, "[-] eliminado   ", RRCC_COLOR_ELIMINADO, true, 9);
            rrccRun(rrccLeyenda, "[~] modificado", RRCC_COLOR_MODIFICADO, true, 9);
            for (RRCCBloqueDiff rrccBloque : rrccR.rrccBloques()) {
                if (rrccSoloCambios && rrccBloque.rrccTipo() == RRCCTipoCambio.RRCC_IGUAL) {
                    continue;
                }
                XWPFParagraph rrccP = rrccWord.createParagraph();
                rrccP.setSpacingAfter(100);
                switch (rrccBloque.rrccTipo()) {
                    case RRCC_IGUAL -> rrccRun(rrccP, rrccBloque.rrccTextoB(), RRCC_COLOR_GRIS, false, 10);
                    case RRCC_AGREGADO -> {
                        rrccRun(rrccP, "[+] ", RRCC_COLOR_AGREGADO, true, 10);
                        rrccRun(rrccP, rrccBloque.rrccTextoB(), RRCC_COLOR_AGREGADO, false, 10);
                    }
                    case RRCC_ELIMINADO -> {
                        rrccRun(rrccP, "[-] ", RRCC_COLOR_ELIMINADO, true, 10);
                        XWPFRun rrccTachado = rrccRun(rrccP, rrccBloque.rrccTextoA(), RRCC_COLOR_ELIMINADO, false, 10);
                        rrccTachado.setStrikeThrough(true);
                    }
                    case RRCC_MODIFICADO -> {
                        rrccRun(rrccP, "[~] ", RRCC_COLOR_MODIFICADO, true, 10);
                        for (RRCCFragmentoDiff rrccF : rrccBloque.rrccFragmentos()) {
                            XWPFRun rrccRunF = rrccRun(rrccP, rrccF.rrccTexto() + " ",
                                    rrccF.rrccTipo() == RRCCTipoCambio.RRCC_AGREGADO ? RRCC_COLOR_AGREGADO
                                            : rrccF.rrccTipo() == RRCCTipoCambio.RRCC_ELIMINADO ? RRCC_COLOR_ELIMINADO : null,
                                    rrccF.rrccTipo() == RRCCTipoCambio.RRCC_AGREGADO, 10);
                            if (rrccF.rrccTipo() == RRCCTipoCambio.RRCC_ELIMINADO) {
                                rrccRunF.setStrikeThrough(true);
                            }
                        }
                    }
                }
            }
            rrccWord.write(rrccSalida);
        }
    }

    public void rrccExportarRevision(RRCCDocumento rrccDoc, RRCCResultadoRevision rrccR, File rrccDestino) throws IOException {
        try (Workbook rrccLibro = new XSSFWorkbook(); OutputStream rrccSalida = new FileOutputStream(rrccDestino)) {
            CellStyle rrccCabecera = rrccLibro.createCellStyle();
            Font rrccNegrita = rrccLibro.createFont();
            rrccNegrita.setBold(true);
            rrccCabecera.setFont(rrccNegrita);
            rrccCabecera.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            rrccCabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle rrccAjuste = rrccLibro.createCellStyle();
            rrccAjuste.setWrapText(true);
            rrccAjuste.setVerticalAlignment(VerticalAlignment.TOP);
            Map<RRCCSeveridad, CellStyle> rrccColores = new EnumMap<>(RRCCSeveridad.class);
            rrccColores.put(RRCCSeveridad.RRCC_ERROR, rrccRelleno(rrccLibro, IndexedColors.ROSE));
            rrccColores.put(RRCCSeveridad.RRCC_ADVERTENCIA, rrccRelleno(rrccLibro, IndexedColors.LIGHT_YELLOW));
            rrccColores.put(RRCCSeveridad.RRCC_SUGERENCIA, rrccRelleno(rrccLibro, IndexedColors.LIGHT_TURQUOISE));

            Sheet rrccResumen = rrccLibro.createSheet("Resumen");
            String[][] rrccFilas = {
                    {"Documento", rrccDoc.getRrccNombre()},
                    {"Fecha de revisión", LocalDateTime.now().format(RRCC_FORMATO)},
                    {"Puntaje de calidad (0-100)", String.valueOf(rrccR.rrccPuntaje())},
                    {"Calificación", rrccR.rrccCalificacion()},
                    {"Palabras revisadas", String.valueOf(rrccR.rrccPalabras())},
                    {"Errores", String.valueOf(rrccR.rrccErrores())},
                    {"Advertencias", String.valueOf(rrccR.rrccAdvertencias())},
                    {"Sugerencias", String.valueOf(rrccR.rrccSugerencias())}};
            Row rrccTit = rrccResumen.createRow(0);
            rrccTit.createCell(0).setCellValue("RRCC MindScan - Revisión de calidad");
            rrccTit.getCell(0).setCellStyle(rrccCabecera);
            for (int rrccI = 0; rrccI < rrccFilas.length; rrccI++) {
                Row rrccFila = rrccResumen.createRow(rrccI + 2);
                rrccFila.createCell(0).setCellValue(rrccFilas[rrccI][0]);
                rrccFila.getCell(0).setCellStyle(rrccCabecera);
                rrccFila.createCell(1).setCellValue(rrccFilas[rrccI][1]);
            }
            int rrccFilaTipo = rrccFilas.length + 3;
            Row rrccCabTipo = rrccResumen.createRow(rrccFilaTipo++);
            rrccCabTipo.createCell(0).setCellValue("Tipo de hallazgo");
            rrccCabTipo.createCell(1).setCellValue("Cantidad");
            rrccCabTipo.getCell(0).setCellStyle(rrccCabecera);
            rrccCabTipo.getCell(1).setCellStyle(rrccCabecera);
            for (var rrccE : rrccR.rrccPorTipo().entrySet()) {
                Row rrccFila = rrccResumen.createRow(rrccFilaTipo++);
                rrccFila.createCell(0).setCellValue(rrccE.getKey().rrccEtiqueta());
                rrccFila.createCell(1).setCellValue(rrccE.getValue());
            }
            rrccResumen.autoSizeColumn(0);
            rrccResumen.setColumnWidth(1, 60 * 256);

            Sheet rrccHoja = rrccLibro.createSheet("Hallazgos");
            String[] rrccCab = {"#", "Severidad", "Tipo", "Descripción", "Fragmento", "Sugerencia"};
            Row rrccFilaCab = rrccHoja.createRow(0);
            for (int rrccI = 0; rrccI < rrccCab.length; rrccI++) {
                Cell rrccC = rrccFilaCab.createCell(rrccI);
                rrccC.setCellValue(rrccCab[rrccI]);
                rrccC.setCellStyle(rrccCabecera);
            }
            int rrccN = 1;
            for (RRCCHallazgo rrccH : rrccR.rrccHallazgos()) {
                Row rrccFila = rrccHoja.createRow(rrccN);
                rrccFila.createCell(0).setCellValue(rrccN);
                Cell rrccSev = rrccFila.createCell(1);
                rrccSev.setCellValue(rrccH.rrccSeveridad().rrccEtiqueta());
                rrccSev.setCellStyle(rrccColores.get(rrccH.rrccSeveridad()));
                rrccFila.createCell(2).setCellValue(rrccH.rrccTipo().rrccEtiqueta());
                rrccFila.createCell(3).setCellValue(rrccH.rrccDescripcion());
                Cell rrccFrag = rrccFila.createCell(4);
                rrccFrag.setCellValue(rrccH.rrccFragmento());
                rrccFrag.setCellStyle(rrccAjuste);
                rrccFila.createCell(5).setCellValue(rrccH.rrccSugerencia());
                rrccN++;
            }
            rrccHoja.setColumnWidth(0, 5 * 256);
            rrccHoja.setColumnWidth(1, 13 * 256);
            rrccHoja.setColumnWidth(2, 24 * 256);
            rrccHoja.setColumnWidth(3, 55 * 256);
            rrccHoja.setColumnWidth(4, 70 * 256);
            rrccHoja.setColumnWidth(5, 36 * 256);
            rrccHoja.createFreezePane(0, 1);
            rrccLibro.write(rrccSalida);
        }
    }

    private CellStyle rrccRelleno(Workbook rrccLibro, IndexedColors rrccColor) {
        CellStyle rrccEstilo = rrccLibro.createCellStyle();
        rrccEstilo.setFillForegroundColor(rrccColor.getIndex());
        rrccEstilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return rrccEstilo;
    }

    private void rrccTitulo(XWPFDocument rrccWord, String rrccTextoTitulo) {
        XWPFParagraph rrccP = rrccWord.createParagraph();
        rrccP.setSpacingAfter(60);
        rrccRun(rrccP, rrccTextoTitulo, RRCC_COLOR_TITULO, true, 20);
    }

    private void rrccSubtitulo(XWPFDocument rrccWord, String rrccTextoSub) {
        XWPFParagraph rrccP = rrccWord.createParagraph();
        rrccP.setSpacingBefore(240);
        rrccP.setSpacingAfter(100);
        rrccRun(rrccP, rrccTextoSub, RRCC_COLOR_TITULO, true, 14);
    }

    private void rrccTexto(XWPFDocument rrccWord, String rrccContenido, String rrccColor, boolean rrccNegrita, int rrccTamano) {
        rrccRun(rrccWord.createParagraph(), rrccContenido, rrccColor, rrccNegrita, rrccTamano);
    }

    private XWPFRun rrccRun(XWPFParagraph rrccP, String rrccContenido, String rrccColor, boolean rrccNegrita, int rrccTamano) {
        XWPFRun rrccR = rrccP.createRun();
        rrccR.setText(rrccContenido == null ? "" : rrccContenido);
        rrccR.setFontFamily(RRCC_FUENTE);
        rrccR.setFontSize(rrccTamano);
        rrccR.setBold(rrccNegrita);
        if (rrccColor != null) {
            rrccR.setColor(rrccColor);
        }
        return rrccR;
    }

    private void rrccTabla(XWPFDocument rrccWord, String[][] rrccFilas) {
        int rrccColumnas = rrccFilas[0].length;
        XWPFTable rrccTabla = rrccWord.createTable(rrccFilas.length, rrccColumnas);
        rrccTabla.setWidth("100%");
        for (int rrccI = 0; rrccI < rrccFilas.length; rrccI++) {
            XWPFTableRow rrccFila = rrccTabla.getRow(rrccI);
            for (int rrccJ = 0; rrccJ < rrccColumnas; rrccJ++) {
                XWPFTableCell rrccCelda = rrccFila.getCell(rrccJ);
                XWPFParagraph rrccP = rrccCelda.getParagraphs().get(0);
                boolean rrccDestacar = rrccJ == 0 || (rrccColumnas > 2 && rrccI == 0);
                rrccRun(rrccP, rrccFilas[rrccI][rrccJ], rrccDestacar ? RRCC_COLOR_TITULO : null, rrccDestacar, 10);
                if (rrccDestacar) {
                    rrccCelda.setColor("EAF1FB");
                }
            }
        }
    }
}
