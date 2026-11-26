package com.ejemplo.rrccmindscan.controller;

import com.ejemplo.rrccmindscan.config.RRCCAnalisisPropiedades;
import com.ejemplo.rrccmindscan.model.RRCCCategoria;
import com.ejemplo.rrccmindscan.model.RRCCDocumento;
import com.ejemplo.rrccmindscan.model.RRCCRevision;
import com.ejemplo.rrccmindscan.model.RRCCTipoDocumento;
import com.ejemplo.rrccmindscan.service.RRCCDocumentoService;
import com.ejemplo.rrccmindscan.service.RRCCDocumentoService.RRCCResumenBiblioteca;
import com.ejemplo.rrccmindscan.service.RRCCExtraccionService;
import com.ejemplo.rrccmindscan.service.RRCCExtraccionService.RRCCEvento;
import com.ejemplo.rrccmindscan.service.RRCCHistorialService;
import com.ejemplo.rrccmindscan.ui.*;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
@RequiredArgsConstructor
public class RRCCPanelController implements RRCCVista {

    public static final int RRCC_MAX_AGENDA = 7;
    public static final int RRCC_MAX_MATRIZ = 12;
    public static final double RRCC_CELDA = 62;
    public static final double RRCC_UMBRAL_VERSION = 0.6;
    public static final DateTimeFormatter RRCC_DIA = DateTimeFormatter.ofPattern("d MMM", RRCCFormatos.RRCC_LOCALE);
    public static final DateTimeFormatter RRCC_DIA_LARGO = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", RRCCFormatos.RRCC_LOCALE);

    private final RRCCDocumentoService rrccDocumentos;
    private final RRCCHistorialService rrccHistorial;
    private final RRCCExtraccionService rrccExtraccion;
    private final RRCCImportador rrccImportador;
    private final RRCCContexto rrccContexto;
    private final RRCCMainController rrccMain;
    private final RRCCLector rrccLector;
    private final RRCCAnalisisPropiedades rrccPropiedades;

    @FXML private Label rrccLblSubtitulo;
    @FXML private Label rrccLblDocumentos;
    @FXML private Label rrccLblDetalleDocumentos;
    @FXML private Label rrccLblLectura;
    @FXML private Label rrccLblDetalleLectura;
    @FXML private Label rrccLblCalidad;
    @FXML private Label rrccLblDetalleCalidad;
    @FXML private Label rrccLblPlazo;
    @FXML private Label rrccLblDetallePlazo;
    @FXML private Label rrccLblResumenAgenda;
    @FXML private VBox rrccCajaAgenda;
    @FXML private VBox rrccCajaCategorias;
    @FXML private VBox rrccCajaFormatos;
    @FXML private GridPane rrccGridEstado;
    @FXML private Label rrccLblLecturaMatriz;
    @FXML private VBox rrccCajaMatriz;
    @FXML private VBox rrccZonaArrastre;

    private record RRCCPlazo(RRCCDocumento rrccDocumento, RRCCEvento rrccEvento) {
    }

    @Override
    public void rrccIniciar() {
        rrccImportador.rrccHabilitarArrastre(rrccZonaArrastre, rrccR -> rrccDespuesDeImportar());
    }

    @Override
    public void rrccRefrescar() {
        List<RRCCDocumento> rrccLista = rrccDocumentos.rrccListar();
        RRCCResumenBiblioteca rrccResumen = rrccDocumentos.rrccResumen(rrccLista);
        Map<Long, RRCCRevision> rrccUltimas = new HashMap<>();
        for (RRCCRevision rrccR : rrccHistorial.rrccRevisiones()) {
            rrccUltimas.putIfAbsent(rrccR.getRrccDocumento().getRrccId(), rrccR);
        }
        rrccLblSubtitulo.setText("Resumen de tu biblioteca al " + LocalDate.now().format(RRCC_DIA_LARGO)
                + ". Todo se calcula con algoritmos clásicos de texto, sin inteligencia artificial.");
        rrccPintarIndicadores(rrccResumen, rrccUltimas, rrccLista);
        rrccPintarComposicion(rrccResumen);
        rrccCajaAgenda.getChildren().setAll(new Label("Leyendo fechas de los documentos..."));
        rrccGridEstado.getChildren().clear();
        RRCCTareas.rrccEjecutar(() -> rrccPlazos(rrccLista), rrccPlazos -> {
            rrccPintarAgenda(rrccPlazos);
            rrccPintarEstado(rrccLista, rrccUltimas, rrccPlazos);
        }, rrccMensaje -> rrccCajaAgenda.getChildren().setAll(new Label(rrccMensaje)));
        List<RRCCDocumento> rrccParaMatriz = rrccLista.stream().limit(RRCC_MAX_MATRIZ).toList();
        rrccCajaMatriz.getChildren().setAll(new Label("Calculando similitudes..."));
        RRCCTareas.rrccEjecutar(() -> rrccDocumentos.rrccMatrizSimilitud(rrccParaMatriz),
                rrccMatriz -> rrccPintarMatriz(rrccParaMatriz, rrccMatriz),
                rrccMensaje -> rrccCajaMatriz.getChildren().setAll(new Label(rrccMensaje)));
    }

    private List<RRCCPlazo> rrccPlazos(List<RRCCDocumento> rrccLista) {
        List<RRCCPlazo> rrccPlazos = new ArrayList<>();
        for (RRCCDocumento rrccD : rrccLista) {
            for (RRCCEvento rrccE : rrccExtraccion.rrccCronologia(rrccD.getRrccContenido())) {
                rrccPlazos.add(new RRCCPlazo(rrccD, rrccE));
            }
        }
        rrccPlazos.sort(Comparator.comparing((RRCCPlazo rrccP) -> rrccP.rrccEvento().rrccFecha()));
        return rrccPlazos;
    }

    private void rrccPintarIndicadores(RRCCResumenBiblioteca rrccResumen, Map<Long, RRCCRevision> rrccUltimas, List<RRCCDocumento> rrccLista) {
        rrccLblDocumentos.setText(RRCCFormatos.rrccNumero(rrccResumen.rrccDocumentos()));
        long rrccFavoritos = rrccLista.stream().filter(RRCCDocumento::rrccEsFavorito).count();
        rrccLblDetalleDocumentos.setText(RRCCFormatos.rrccNumero(rrccResumen.rrccPaginas()) + " páginas  ·  "
                + RRCCFormatos.rrccNumero(rrccResumen.rrccPalabras()) + " palabras" + (rrccFavoritos > 0 ? "  ·  " + rrccFavoritos + " ★" : ""));
        double rrccMinutos = rrccResumen.rrccPalabras() / (double) rrccPropiedades.rrccPalabrasPorMinuto();
        double rrccConResumen = rrccMinutos * rrccPropiedades.rrccProporcionResumen();
        rrccLblLectura.setText(RRCCFormatos.rrccMinutos(rrccMinutos));
        rrccLblDetalleLectura.setText("Con los resúmenes automáticos: " + RRCCFormatos.rrccMinutos(rrccConResumen)
                + " (ahorras " + RRCCFormatos.rrccMinutos(rrccMinutos - rrccConResumen) + ")");
        if (rrccUltimas.isEmpty()) {
            rrccLblCalidad.setText("-");
            rrccLblDetalleCalidad.setText("Aún no revisas documentos");
        } else {
            double rrccPromedio = rrccUltimas.values().stream().mapToInt(RRCCRevision::getRrccPuntaje).average().orElse(0);
            int rrccErrores = rrccUltimas.values().stream().mapToInt(RRCCRevision::getRrccErrores).sum();
            rrccLblCalidad.setText(Math.round(rrccPromedio) + " / 100");
            rrccLblDetalleCalidad.setText(rrccUltimas.size() + " de " + rrccLista.size() + " revisados  ·  " + rrccErrores + " errores por corregir");
        }
        rrccLblPlazo.setText("-");
        rrccLblDetallePlazo.setText("Buscando fechas...");
    }

    private void rrccPintarComposicion(RRCCResumenBiblioteca rrccResumen) {
        rrccCajaCategorias.getChildren().clear();
        int rrccTotal = Math.max(1, rrccResumen.rrccDocumentos());
        rrccResumen.rrccPorCategoria().entrySet().stream()
                .sorted(Map.Entry.<RRCCCategoria, Integer>comparingByValue().reversed())
                .forEach(rrccE -> rrccCajaCategorias.getChildren().add(rrccFilaBarra(RRCCFormatos.rrccChipCategoria(rrccE.getKey()),
                        rrccE.getValue() / (double) rrccTotal, rrccE.getValue() + (rrccE.getValue() == 1 ? " documento" : " documentos"),
                        rrccE.getKey().rrccClaseCss())));
        rrccCajaFormatos.getChildren().clear();
        for (Map.Entry<RRCCTipoDocumento, Integer> rrccE : rrccResumen.rrccPorTipo().entrySet()) {
            rrccCajaFormatos.getChildren().add(rrccFilaBarra(RRCCFormatos.rrccChipTipo(rrccE.getKey()),
                    rrccE.getValue() / (double) rrccTotal, String.valueOf(rrccE.getValue()), "rrcc-barra-tinta"));
        }
        if (rrccResumen.rrccDocumentos() == 0) {
            rrccCajaCategorias.getChildren().add(new Label("Sin documentos"));
        }
    }

    private HBox rrccFilaBarra(Label rrccChip, double rrccFraccion, String rrccTexto, String rrccClase) {
        rrccChip.setMinWidth(110);
        HBox rrccMedidor = RRCCGraficos.rrccMedidor(rrccFraccion, rrccClase, rrccTexto);
        HBox.setHgrow(rrccMedidor, Priority.ALWAYS);
        HBox rrccFila = new HBox(10, rrccChip, rrccMedidor);
        rrccFila.setAlignment(Pos.CENTER_LEFT);
        return rrccFila;
    }

    private void rrccPintarAgenda(List<RRCCPlazo> rrccPlazos) {
        LocalDate rrccHoy = LocalDate.now();
        List<RRCCPlazo> rrccFuturos = rrccPlazos.stream().filter(rrccP -> !rrccP.rrccEvento().rrccFecha().isBefore(rrccHoy)).toList();
        long rrccPasados = rrccPlazos.size() - rrccFuturos.size();
        rrccLblResumenAgenda.setText(rrccFuturos.size() + " por venir  ·  " + rrccPasados + " pasados");
        rrccCajaAgenda.getChildren().clear();
        if (rrccFuturos.isEmpty()) {
            rrccLblPlazo.setText("Ninguno");
            rrccLblDetallePlazo.setText("No hay fechas futuras en los documentos");
            rrccCajaAgenda.getChildren().add(RRCCGraficos.rrccVacio("Sin plazos pendientes",
                    "Ningún documento menciona fechas posteriores a hoy."));
            return;
        }
        RRCCPlazo rrccPrimero = rrccFuturos.get(0);
        long rrccDias = ChronoUnit.DAYS.between(rrccHoy, rrccPrimero.rrccEvento().rrccFecha());
        rrccLblPlazo.setText(rrccPrimero.rrccEvento().rrccFecha().format(RRCC_DIA));
        rrccLblDetallePlazo.setText((rrccDias == 0 ? "Hoy" : "En " + rrccDias + (rrccDias == 1 ? " día" : " días"))
                + "  ·  " + rrccPrimero.rrccDocumento().getRrccNombre());
        for (RRCCPlazo rrccP : rrccFuturos.stream().limit(RRCC_MAX_AGENDA).toList()) {
            LocalDate rrccF = rrccP.rrccEvento().rrccFecha();
            long rrccFaltan = ChronoUnit.DAYS.between(rrccHoy, rrccF);
            Label rrccDia = new Label(String.valueOf(rrccF.getDayOfMonth()));
            rrccDia.getStyleClass().add("rrcc-agenda-dia");
            Label rrccMes = new Label(rrccF.format(DateTimeFormatter.ofPattern("MMM yyyy", RRCCFormatos.RRCC_LOCALE)).toUpperCase());
            rrccMes.getStyleClass().add("rrcc-agenda-mes");
            VBox rrccCalendario = new VBox(0, rrccDia, rrccMes);
            rrccCalendario.setAlignment(Pos.CENTER);
            rrccCalendario.setMinWidth(70);
            rrccCalendario.getStyleClass().add("rrcc-agenda-calendario");
            Label rrccDoc = new Label(rrccP.rrccDocumento().getRrccNombre());
            rrccDoc.getStyleClass().add("rrcc-fila-titulo");
            Label rrccOracion = new Label(rrccP.rrccEvento().rrccOracion());
            rrccOracion.setWrapText(true);
            rrccOracion.getStyleClass().add("rrcc-texto-suave");
            VBox rrccTextos = new VBox(3, rrccDoc, rrccOracion);
            HBox.setHgrow(rrccTextos, Priority.ALWAYS);
            String rrccEstado = rrccFaltan <= 7 ? "rrcc-evento-proximo" : "rrcc-evento-futuro";
            Label rrccCuando = RRCCFormatos.rrccChip(rrccFaltan == 0 ? "Hoy" : "En " + rrccFaltan + (rrccFaltan == 1 ? " día" : " días"), rrccEstado);
            rrccCuando.setMinWidth(Region.USE_PREF_SIZE);
            HBox rrccFila = new HBox(14, rrccCalendario, rrccTextos, rrccCuando);
            rrccFila.setAlignment(Pos.CENTER_LEFT);
            rrccFila.getStyleClass().addAll("rrcc-fila", "rrcc-fila-clic");
            rrccFila.setOnMouseClicked(rrccE -> rrccLector.rrccAbrir(rrccP.rrccDocumento(), rrccP.rrccEvento().rrccPosicion(),
                    rrccP.rrccEvento().rrccTextoFecha().length(), "Plazo del " + rrccF.format(RRCC_DIA_LARGO)));
            rrccCajaAgenda.getChildren().add(rrccFila);
        }
    }

    private void rrccPintarEstado(List<RRCCDocumento> rrccLista, Map<Long, RRCCRevision> rrccUltimas, List<RRCCPlazo> rrccPlazos) {
        rrccGridEstado.getChildren().clear();
        String[] rrccCabeceras = {"Documento", "Categoría", "Legibilidad", "Calidad", "Próximo plazo", ""};
        for (int rrccC = 0; rrccC < rrccCabeceras.length; rrccC++) {
            Label rrccCab = new Label(rrccCabeceras[rrccC].toUpperCase());
            rrccCab.getStyleClass().add("rrcc-estado-cabecera");
            rrccGridEstado.add(rrccCab, rrccC, 0);
        }
        if (rrccLista.isEmpty()) {
            rrccGridEstado.add(new Label("Importa documentos para ver su estado."), 0, 1, 6, 1);
            return;
        }
        LocalDate rrccHoy = LocalDate.now();
        int rrccFila = 1;
        for (RRCCDocumento rrccD : rrccLista) {
            Label rrccNombre = new Label((rrccD.rrccEsFavorito() ? "★ " : "") + rrccD.getRrccNombre());
            rrccNombre.getStyleClass().add("rrcc-fila-titulo");
            Label rrccDetalle = new Label(rrccD.getRrccTipo().rrccEtiqueta() + "  ·  " + RRCCFormatos.rrccNumero(rrccD.getRrccPalabras()) + " palabras");
            rrccDetalle.getStyleClass().add("rrcc-texto-suave");
            VBox rrccCeldaNombre = new VBox(2, rrccNombre, rrccDetalle);
            rrccCeldaNombre.setMinWidth(260);

            HBox rrccLegibilidad = RRCCGraficos.rrccMedidor(rrccD.getRrccLegibilidad() / 100,
                    RRCCFormatos.rrccClaseLegibilidad(rrccD.getRrccLegibilidad()),
                    Math.round(rrccD.getRrccLegibilidad()) + "  " + rrccNivelCorto(rrccD.getRrccLegibilidad()));
            rrccLegibilidad.setPrefWidth(220);

            RRCCRevision rrccR = rrccUltimas.get(rrccD.getRrccId());
            Label rrccCalidad = rrccR == null ? RRCCFormatos.rrccChip("Sin revisar", "rrcc-chip-suave")
                    : RRCCFormatos.rrccChip(rrccR.getRrccPuntaje() + " / 100" + (rrccR.getRrccErrores() > 0 ? "  ·  " + rrccR.getRrccErrores() + " errores" : ""),
                    RRCCFormatos.rrccClasePuntaje(rrccR.getRrccPuntaje()));

            Optional<RRCCPlazo> rrccProximo = rrccPlazos.stream()
                    .filter(rrccP -> rrccP.rrccDocumento().getRrccId().equals(rrccD.getRrccId()))
                    .filter(rrccP -> !rrccP.rrccEvento().rrccFecha().isBefore(rrccHoy)).findFirst();
            Label rrccPlazo = new Label(rrccProximo.map(rrccP -> rrccP.rrccEvento().rrccFecha().format(RRCC_DIA) + "  (en "
                    + ChronoUnit.DAYS.between(rrccHoy, rrccP.rrccEvento().rrccFecha()) + " d)").orElse("—"));
            rrccPlazo.getStyleClass().add(rrccProximo.isPresent() ? "rrcc-dato-valor" : "rrcc-texto-suave");

            Button rrccAccion = new Button(rrccR == null ? "Revisar" : "Analizar");
            rrccAccion.getStyleClass().addAll("button-outlined", "rrcc-boton-fila");
            rrccAccion.setOnAction(rrccE -> {
                rrccContexto.rrccActivar(rrccD.getRrccId());
                rrccMain.rrccIrA(rrccR == null ? RRCCMainController.RRCC_VISTA_REVISION : RRCCMainController.RRCC_VISTA_ANALISIS);
            });

            javafx.scene.Node[] rrccCeldas = {rrccCeldaNombre, RRCCFormatos.rrccChipCategoria(rrccD.getRrccCategoria()),
                    rrccLegibilidad, rrccCalidad, rrccPlazo, rrccAccion};
            for (int rrccC = 0; rrccC < rrccCeldas.length; rrccC++) {
                HBox rrccContenedor = new HBox(rrccCeldas[rrccC]);
                rrccContenedor.setAlignment(Pos.CENTER_LEFT);
                rrccContenedor.getStyleClass().add("rrcc-estado-celda");
                rrccGridEstado.add(rrccContenedor, rrccC, rrccFila);
            }
            rrccFila++;
        }
    }

    private String rrccNivelCorto(double rrccLegibilidad) {
        return rrccLegibilidad >= 60 ? "accesible" : rrccLegibilidad >= 40 ? "exigente" : "técnica";
    }

    private void rrccPintarMatriz(List<RRCCDocumento> rrccDocs, double[][] rrccMatriz) {
        rrccCajaMatriz.getChildren().clear();
        if (rrccDocs.size() < 2) {
            rrccLblLecturaMatriz.setText("Lectura: se necesitan al menos dos documentos.");
            rrccCajaMatriz.getChildren().add(RRCCGraficos.rrccVacio("Se necesitan al menos 2 documentos",
                    "Importa más archivos para ver qué tan parecidos son entre sí."));
            return;
        }
        javafx.scene.layout.GridPane rrccGrid = new javafx.scene.layout.GridPane();
        rrccGrid.setHgap(3);
        rrccGrid.setVgap(3);
        rrccGrid.getStyleClass().add("rrcc-matriz");
        for (int rrccJ = 0; rrccJ < rrccDocs.size(); rrccJ++) {
            Label rrccCab = new Label("#" + (rrccJ + 1));
            rrccCab.getStyleClass().add("rrcc-matriz-cabecera");
            rrccCab.setMinWidth(RRCC_CELDA);
            rrccCab.setAlignment(Pos.CENTER);
            Tooltip.install(rrccCab, new Tooltip(rrccDocs.get(rrccJ).getRrccNombre()));
            rrccGrid.add(rrccCab, rrccJ + 1, 0);
        }
        for (int rrccI = 0; rrccI < rrccDocs.size(); rrccI++) {
            RRCCDocumento rrccA = rrccDocs.get(rrccI);
            String rrccNombre = rrccA.getRrccNombre().length() > 26 ? rrccA.getRrccNombre().substring(0, 26) + "..." : rrccA.getRrccNombre();
            Label rrccFilaCab = new Label("#" + (rrccI + 1) + "  " + rrccNombre);
            rrccFilaCab.getStyleClass().add("rrcc-matriz-cabecera");
            rrccFilaCab.setMinWidth(220);
            Tooltip.install(rrccFilaCab, new Tooltip(rrccA.getRrccNombre()));
            rrccGrid.add(rrccFilaCab, 0, rrccI + 1);
            for (int rrccJ = 0; rrccJ < rrccDocs.size(); rrccJ++) {
                RRCCDocumento rrccB = rrccDocs.get(rrccJ);
                double rrccS = rrccMatriz[rrccI][rrccJ];
                Label rrccCelda = new Label(rrccI == rrccJ ? "—" : Math.round(rrccS * 100) + "%");
                rrccCelda.setMinSize(RRCC_CELDA, 30);
                rrccCelda.setPrefSize(RRCC_CELDA, 30);
                rrccCelda.setAlignment(Pos.CENTER);
                rrccCelda.getStyleClass().add("rrcc-matriz-celda");
                if (rrccI == rrccJ) {
                    rrccCelda.getStyleClass().add("rrcc-matriz-diagonal");
                } else {
                    double rrccAlfa = 0.08 + 0.92 * rrccS;
                    rrccCelda.setStyle("-fx-background-color: rgba(91, 132, 177, " + String.format(java.util.Locale.ROOT, "%.2f", rrccAlfa) + ");"
                            + (rrccS > 0.5 ? " -fx-text-fill: white;" : ""));
                    Tooltip.install(rrccCelda, new Tooltip(rrccA.getRrccNombre() + "\n" + rrccB.getRrccNombre()
                            + "\nSimilitud de vocabulario: " + Math.round(rrccS * 100) + " %\nClic para comparar"));
                    rrccCelda.getStyleClass().add("rrcc-fila-clic");
                    rrccCelda.setOnMouseClicked(rrccE -> {
                        rrccContexto.rrccPrepararComparacion(rrccA.getRrccId(), rrccB.getRrccId());
                        rrccMain.rrccIrA(RRCCMainController.RRCC_VISTA_COMPARADOR);
                    });
                }
                rrccGrid.add(rrccCelda, rrccJ + 1, rrccI + 1);
            }
        }
        rrccCajaMatriz.getChildren().add(rrccGrid);
        List<String> rrccVersiones = new ArrayList<>();
        for (int rrccI = 0; rrccI < rrccDocs.size(); rrccI++) {
            for (int rrccJ = rrccI + 1; rrccJ < rrccDocs.size(); rrccJ++) {
                if (rrccMatriz[rrccI][rrccJ] >= RRCC_UMBRAL_VERSION) {
                    rrccVersiones.add("#" + (rrccI + 1) + " y #" + (rrccJ + 1) + " (" + Math.round(rrccMatriz[rrccI][rrccJ] * 100) + " %)");
                }
            }
        }
        rrccLblLecturaMatriz.setText(rrccVersiones.isEmpty()
                ? "Lectura: ningún par supera el " + Math.round(RRCC_UMBRAL_VERSION * 100) + " % de vocabulario común, los documentos tratan temas distintos."
                : "Lectura: " + String.join(", ", rrccVersiones) + " comparten casi todo su vocabulario; probablemente son versiones del mismo documento. Haz clic en la celda para ver qué cambió.");
    }

    private void rrccDespuesDeImportar() {
        rrccMain.rrccActualizarPie();
        rrccRefrescar();
    }

    @FXML
    private void rrccOnImportarArchivos() {
        rrccImportador.rrccElegirArchivos(rrccZonaArrastre.getScene().getWindow(), rrccR -> rrccDespuesDeImportar());
    }

    @FXML
    private void rrccOnImportarCarpeta() {
        rrccImportador.rrccElegirCarpeta(rrccZonaArrastre.getScene().getWindow(), rrccR -> rrccDespuesDeImportar());
    }
}
