package com.ejemplo.rrccmindscan.controller;

import com.ejemplo.rrccmindscan.model.RRCCDocumento;
import com.ejemplo.rrccmindscan.model.RRCCSeveridad;
import com.ejemplo.rrccmindscan.model.RRCCTipoHallazgo;
import com.ejemplo.rrccmindscan.service.RRCCCorrectorService;
import com.ejemplo.rrccmindscan.service.RRCCDocumentoService;
import com.ejemplo.rrccmindscan.service.RRCCHistorialService;
import com.ejemplo.rrccmindscan.service.RRCCReporteService;
import com.ejemplo.rrccmindscan.service.RRCCRevisorService;
import com.ejemplo.rrccmindscan.service.RRCCRevisorService.RRCCHallazgo;
import com.ejemplo.rrccmindscan.service.RRCCRevisorService.RRCCResultadoRevision;
import com.ejemplo.rrccmindscan.ui.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RRCCRevisionController implements RRCCVista {

    public static final String RRCC_TODAS = "Todas";
    public static final int RRCC_MAX_TEXTO_MARCADO = 30000;
    public static final String[] RRCC_AYUDAS = {
            "Oraciones parecidas donde una niega lo que la otra afirma o usan palabras opuestas",
            "La misma cantidad aparece con valores distintos en contextos similares",
            "Fechas que no existen en el calendario, como 31/02 o 30 de febrero",
            "Palabras escritas dos veces seguidas, como \"de de\"",
            "¿ ? ¡ ! ( ) y comillas que no se cierran dentro del párrafo",
            "Párrafos copiados dos veces en el documento",
            "Oraciones que superan el límite de palabras configurado",
            "Oraciones que empiezan con minúscula después de un punto",
            "Espacios antes de coma, punto, dos puntos o cierre de paréntesis",
            "Dos o más espacios seguidos entre palabras"};

    private final RRCCDocumentoService rrccDocumentos;
    private final RRCCRevisorService rrccRevisor;
    private final RRCCHistorialService rrccHistorial;
    private final RRCCReporteService rrccReporte;
    private final RRCCNotificador rrccNotificador;
    private final RRCCContexto rrccContexto;
    private final RRCCLector rrccLector;
    private final RRCCCorrectorService rrccCorrector;
    private final RRCCVentanaCorreccion rrccVentanaCorreccion;

    @FXML private ComboBox<String> rrccCmbDocumento;
    @FXML private Button rrccBtnExportar;
    @FXML private Button rrccBtnCorregir;
    @FXML private VBox rrccCajaVacia;
    @FXML private VBox rrccContenido;
    @FXML private StackPane rrccCajaAnillo;
    @FXML private Label rrccLblCalificacion;
    @FXML private Label rrccLblDetalle;
    @FXML private Label rrccLblErrores;
    @FXML private Label rrccLblAdvertencias;
    @FXML private Label rrccLblSugerencias;
    @FXML private BarChart<Number, String> rrccGraficoTipos;
    @FXML private VBox rrccCajaChecklist;
    @FXML private ComboBox<String> rrccCmbSeveridad;
    @FXML private TableView<RRCCHallazgo> rrccTabla;
    @FXML private TableColumn<RRCCHallazgo, Label> rrccColSeveridad;
    @FXML private TableColumn<RRCCHallazgo, String> rrccColTipo;
    @FXML private TableColumn<RRCCHallazgo, String> rrccColDescripcion;
    @FXML private TableColumn<RRCCHallazgo, String> rrccColSugerencia;
    @FXML private VBox rrccCajaDetalle;
    @FXML private VBox rrccCajaTextoMarcado;

    private RRCCSelectorDocumento rrccSelector;
    private RRCCDocumento rrccDocumento;
    private RRCCResultadoRevision rrccResultado;
    private final ObservableList<RRCCHallazgo> rrccHallazgos = FXCollections.observableArrayList();
    private FilteredList<RRCCHallazgo> rrccFiltrados;
    private final Map<Long, String> rrccUltimoGuardado = new HashMap<>();

    @Override
    public void rrccIniciar() {
        rrccSelector = new RRCCSelectorDocumento(rrccCmbDocumento);
        rrccSelector.rrccAlCambiar(rrccD -> rrccOnRevisar());
        rrccColSeveridad.setCellValueFactory(rrccC -> new SimpleObjectProperty<>(RRCCFormatos.rrccChipSeveridad(rrccC.getValue().rrccSeveridad())));
        rrccColTipo.setCellValueFactory(rrccC -> new SimpleStringProperty(rrccC.getValue().rrccTipo().rrccEtiqueta()));
        rrccColDescripcion.setCellValueFactory(rrccC -> new SimpleStringProperty(rrccC.getValue().rrccDescripcion()));
        rrccColSugerencia.setCellValueFactory(rrccC -> new SimpleStringProperty(rrccC.getValue().rrccSugerencia()));
        rrccColSeveridad.setSortable(false);
        rrccFiltrados = new FilteredList<>(rrccHallazgos, rrccH -> true);
        rrccTabla.setItems(rrccFiltrados);
        rrccTabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        rrccTabla.setPlaceholder(new Label("Sin hallazgos: el documento pasó todas las reglas."));
        rrccTabla.getSelectionModel().selectedItemProperty().addListener((rrccO, rrccA, rrccN) -> rrccMostrarDetalle(rrccN));
        rrccTabla.setOnMouseClicked(rrccE -> {
            RRCCHallazgo rrccH = rrccTabla.getSelectionModel().getSelectedItem();
            if (rrccE.getClickCount() == 2 && rrccH != null && rrccDocumento != null) {
                rrccLector.rrccAbrir(rrccDocumento, rrccH.rrccPosicion(), rrccH.rrccLargo(),
                        rrccH.rrccTipo().rrccEtiqueta() + ": " + rrccH.rrccDescripcion());
            }
        });

        rrccCmbSeveridad.getItems().add(RRCC_TODAS);
        for (RRCCSeveridad rrccS : RRCCSeveridad.values()) {
            rrccCmbSeveridad.getItems().add(rrccS.rrccEtiqueta());
        }
        rrccCmbSeveridad.getSelectionModel().selectFirst();
        rrccCmbSeveridad.valueProperty().addListener((rrccO, rrccA, rrccN) -> rrccFiltrados.setPredicate(rrccH ->
                rrccN == null || RRCC_TODAS.equals(rrccN) || rrccH.rrccSeveridad().rrccEtiqueta().equals(rrccN)));
        rrccGraficoTipos.setAnimated(false);
        rrccMostrar(false);
    }

    @Override
    public void rrccRefrescar() {
        rrccSelector.rrccCargar(rrccDocumentos.rrccListar());
        Long rrccPendiente = rrccContexto.rrccTomarActivo();
        if (rrccPendiente != null) {
            rrccSelector.rrccSeleccionar(rrccPendiente);
            rrccOnRevisar();
            return;
        }
        if (rrccResultado == null) {
            rrccCajaVacia.getChildren().setAll(RRCCGraficos.rrccVacio("Elige un documento para revisarlo",
                    "Prueba con \"Informe técnico laboratorio 3\": tiene contradicciones y cifras inconsistentes a propósito."));
        }
    }

    private void rrccMostrar(boolean rrccHay) {
        rrccContenido.setVisible(rrccHay);
        rrccContenido.setManaged(rrccHay);
        rrccCajaVacia.setVisible(!rrccHay);
        rrccCajaVacia.setManaged(!rrccHay);
        rrccBtnExportar.setDisable(!rrccHay);
        rrccBtnCorregir.setDisable(!rrccHay);
    }

    @FXML
    private void rrccOnRevisar() {
        RRCCDocumento rrccDoc = rrccSelector.rrccSeleccionado();
        if (rrccDoc == null) {
            rrccNotificador.rrccAdvertencia("Elige el documento que quieres revisar.");
            return;
        }
        RRCCTareas.rrccEjecutar(() -> rrccRevisor.rrccRevisar(rrccDoc.getRrccContenido()), rrccR -> {
            rrccDocumento = rrccDoc;
            rrccResultado = rrccR;
            rrccRegistrar(rrccDoc, rrccR);
            rrccPintar();
        }, rrccMensaje -> rrccNotificador.rrccError("No se pudo revisar: " + rrccMensaje));
    }

    private void rrccRegistrar(RRCCDocumento rrccDoc, RRCCResultadoRevision rrccR) {
        String rrccFirma = rrccDoc.getRrccHash() + "|" + rrccR.rrccPuntaje() + "|" + rrccR.rrccHallazgos().size();
        if (rrccFirma.equals(rrccUltimoGuardado.get(rrccDoc.getRrccId()))) {
            return;
        }
        rrccHistorial.rrccGuardarRevision(rrccDoc, rrccR);
        rrccUltimoGuardado.put(rrccDoc.getRrccId(), rrccFirma);
    }

    private void rrccPintar() {
        RRCCResultadoRevision rrccR = rrccResultado;
        rrccCajaAnillo.getChildren().setAll(RRCCGraficos.rrccAnillo(rrccR.rrccPuntaje() / 100.0,
                String.valueOf(rrccR.rrccPuntaje()), "de 100", RRCCFormatos.rrccClasePuntaje(rrccR.rrccPuntaje()), 40));
        rrccLblCalificacion.setText(rrccR.rrccCalificacion());
        rrccLblDetalle.setText(rrccDocumento.getRrccNombre() + "\n" + RRCCFormatos.rrccNumero(rrccR.rrccPalabras())
                + " palabras revisadas con 10 reglas");
        rrccLblErrores.setText(String.valueOf(rrccR.rrccErrores()));
        rrccLblAdvertencias.setText(String.valueOf(rrccR.rrccAdvertencias()));
        rrccLblSugerencias.setText(String.valueOf(rrccR.rrccSugerencias()));

        rrccGraficoTipos.getData().clear();
        XYChart.Series<Number, String> rrccSerie = new XYChart.Series<>();
        RRCCTipoHallazgo[] rrccTipos = RRCCTipoHallazgo.values();
        for (int rrccI = rrccTipos.length - 1; rrccI >= 0; rrccI--) {
            RRCCTipoHallazgo rrccT = rrccTipos[rrccI];
            int rrccCantidad = rrccR.rrccPorTipo().getOrDefault(rrccT, 0);
            XYChart.Data<Number, String> rrccDato = new XYChart.Data<>(rrccCantidad, rrccT.rrccEtiqueta());
            rrccDato.setExtraValue(rrccT.rrccSeveridad().rrccClaseCss());
            rrccSerie.getData().add(rrccDato);
        }
        rrccGraficoTipos.getData().add(rrccSerie);
        for (XYChart.Data<Number, String> rrccDato : rrccSerie.getData()) {
            if (rrccDato.getNode() != null) {
                rrccDato.getNode().getStyleClass().add("rrcc-barra-" + rrccDato.getExtraValue());
                Tooltip.install(rrccDato.getNode(), new Tooltip(rrccDato.getYValue() + ": " + rrccDato.getXValue()));
            }
        }

        rrccCajaChecklist.getChildren().clear();
        for (int rrccI = 0; rrccI < rrccTipos.length; rrccI++) {
            RRCCTipoHallazgo rrccT = rrccTipos[rrccI];
            int rrccCantidad = rrccR.rrccPorTipo().getOrDefault(rrccT, 0);
            Label rrccIcono = new Label(rrccCantidad == 0 ? "✓" : String.valueOf(rrccCantidad));
            rrccIcono.getStyleClass().addAll("rrcc-check", rrccCantidad == 0 ? "rrcc-check-ok" : rrccT.rrccSeveridad().rrccClaseCss());
            rrccIcono.setMinWidth(30);
            rrccIcono.setAlignment(Pos.CENTER);
            Label rrccNombre = new Label(rrccT.rrccEtiqueta());
            rrccNombre.getStyleClass().add("rrcc-check-nombre");
            Tooltip.install(rrccNombre, new Tooltip(RRCC_AYUDAS[rrccI]));
            HBox rrccFila = new HBox(10, rrccIcono, rrccNombre);
            rrccFila.setAlignment(Pos.CENTER_LEFT);
            rrccFila.getStyleClass().add("rrcc-check-fila");
            rrccFila.setOnMouseClicked(rrccE -> rrccFiltrados.setPredicate(rrccH -> rrccH.rrccTipo() == rrccT));
            rrccCajaChecklist.getChildren().add(rrccFila);
        }
        Hyperlink rrccTodos = new Hyperlink("Mostrar todos los hallazgos");
        rrccTodos.setOnAction(rrccE -> {
            rrccCmbSeveridad.getSelectionModel().selectFirst();
            rrccFiltrados.setPredicate(rrccH -> true);
        });
        rrccCajaChecklist.getChildren().add(rrccTodos);

        rrccHallazgos.setAll(rrccR.rrccHallazgos());
        rrccCmbSeveridad.getSelectionModel().selectFirst();
        rrccFiltrados.setPredicate(rrccH -> true);
        if (!rrccFiltrados.isEmpty()) {
            rrccTabla.getSelectionModel().selectFirst();
        } else {
            rrccMostrarDetalle(null);
        }
        rrccPintarTextoMarcado();
        rrccMostrar(true);
    }

    private void rrccMostrarDetalle(RRCCHallazgo rrccH) {
        rrccCajaDetalle.getChildren().clear();
        if (rrccH == null) {
            return;
        }
        Label rrccTitulo = new Label(rrccH.rrccTipo().rrccEtiqueta() + ": " + rrccH.rrccDescripcion());
        rrccTitulo.getStyleClass().add("rrcc-fila-titulo");
        rrccTitulo.setWrapText(true);
        Label rrccFragmento = new Label(rrccH.rrccFragmento());
        rrccFragmento.getStyleClass().add("rrcc-fragmento-hallazgo");
        rrccFragmento.setWrapText(true);
        rrccFragmento.setMaxWidth(Double.MAX_VALUE);
        Label rrccSugerencia = new Label("Sugerencia: " + rrccH.rrccSugerencia());
        rrccSugerencia.getStyleClass().add("rrcc-texto-suave");
        HBox rrccCabecera = new HBox(10, RRCCFormatos.rrccChipSeveridad(rrccH.rrccSeveridad()), rrccTitulo);
        rrccCabecera.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(rrccTitulo, Priority.ALWAYS);
        Button rrccVer = new Button("Ver en el documento");
        rrccVer.getStyleClass().add("button-outlined");
        rrccVer.setOnAction(rrccE -> rrccLector.rrccAbrir(rrccDocumento, rrccH.rrccPosicion(), rrccH.rrccLargo(),
                rrccH.rrccTipo().rrccEtiqueta() + ": " + rrccH.rrccDescripcion()));
        HBox rrccPie = new HBox(12, rrccSugerencia, rrccVer);
        rrccPie.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(rrccSugerencia, Priority.ALWAYS);
        rrccSugerencia.setMaxWidth(Double.MAX_VALUE);
        rrccCajaDetalle.getChildren().addAll(rrccCabecera, rrccFragmento, rrccPie);
    }

    private void rrccPintarTextoMarcado() {
        String rrccContenido = rrccDocumento.getRrccContenido();
        int rrccLimite = Math.min(rrccContenido.length(), RRCC_MAX_TEXTO_MARCADO);
        List<RRCCHallazgo> rrccOrdenados = new ArrayList<>(rrccResultado.rrccHallazgos());
        rrccOrdenados.sort(Comparator.comparingInt(RRCCHallazgo::rrccPosicion));
        TextFlow rrccFlujo = new TextFlow();
        rrccFlujo.getStyleClass().addAll("rrcc-flujo", "rrcc-lector-texto");
        int rrccCursor = 0;
        for (RRCCHallazgo rrccH : rrccOrdenados) {
            int rrccIni = rrccH.rrccPosicion();
            int rrccFin = Math.min(rrccLimite, rrccIni + Math.max(1, rrccH.rrccLargo()));
            if (rrccIni < rrccCursor || rrccIni >= rrccLimite) {
                continue;
            }
            rrccFlujo.getChildren().add(rrccPieza(rrccContenido.substring(rrccCursor, rrccIni), "rrcc-texto"));
            Text rrccMarca = rrccPieza(rrccContenido.substring(rrccIni, rrccFin), "rrcc-marca-" + rrccH.rrccSeveridad().rrccClaseCss());
            Tooltip rrccAyuda = new Tooltip(rrccH.rrccTipo().rrccEtiqueta() + ": " + rrccH.rrccDescripcion() + "\nSugerencia: " + rrccH.rrccSugerencia());
            rrccAyuda.setWrapText(true);
            rrccAyuda.setMaxWidth(420);
            Tooltip.install(rrccMarca, rrccAyuda);
            rrccMarca.setOnMouseClicked(rrccE -> rrccTabla.getSelectionModel().select(rrccH));
            rrccFlujo.getChildren().add(rrccMarca);
            rrccCursor = rrccFin;
        }
        rrccFlujo.getChildren().add(rrccPieza(rrccContenido.substring(rrccCursor, rrccLimite), "rrcc-texto"));
        if (rrccLimite < rrccContenido.length()) {
            rrccFlujo.getChildren().add(rrccPieza("\n\n[... se muestran los primeros " + RRCCFormatos.rrccNumero(rrccLimite)
                    + " caracteres; usa \"Ver en el documento\" para el resto ...]", "rrcc-resumen-tenue"));
        }
        rrccCajaTextoMarcado.getChildren().setAll(rrccFlujo);
    }

    private Text rrccPieza(String rrccContenido, String rrccClase) {
        Text rrccT = new Text(rrccContenido);
        rrccT.getStyleClass().addAll("rrcc-texto", rrccClase);
        return rrccT;
    }

    @FXML
    private void rrccOnCorregir() {
        RRCCDocumento rrccDoc = rrccDocumento;
        if (rrccDoc == null) {
            return;
        }
        RRCCTareas.rrccEjecutar(() -> rrccCorrector.rrccCorregir(rrccDoc.getRrccContenido()), rrccR -> {
            if (!rrccR.rrccHayCambios()) {
                rrccNotificador.rrccInfo("No hay nada que corregir automáticamente: espacios, signos y mayúsculas están en orden.");
                return;
            }
            rrccVentanaCorreccion.rrccAbrir(rrccDoc, rrccR, rrccNuevo -> {
                rrccSelector.rrccCargar(rrccDocumentos.rrccListar());
                rrccSelector.rrccSeleccionar(rrccNuevo.getRrccId());
                rrccOnRevisar();
            });
        }, rrccMensaje -> rrccNotificador.rrccError("No se pudo corregir: " + rrccMensaje));
    }

    @FXML
    private void rrccOnExportar() {
        if (rrccResultado == null) {
            return;
        }
        FileChooser rrccSelectorArchivo = new FileChooser();
        rrccSelectorArchivo.setTitle("Exportar revisión de calidad");
        rrccSelectorArchivo.setInitialFileName("RRCC_revision.xlsx");
        rrccSelectorArchivo.getExtensionFilters().add(new FileChooser.ExtensionFilter("Libro de Excel", "*.xlsx"));
        File rrccArchivo = rrccSelectorArchivo.showSaveDialog(rrccCmbDocumento.getScene().getWindow());
        if (rrccArchivo == null) {
            return;
        }
        RRCCDocumento rrccDoc = rrccDocumento;
        RRCCResultadoRevision rrccR = rrccResultado;
        RRCCTareas.rrccEjecutar(() -> {
            try {
                rrccReporte.rrccExportarRevision(rrccDoc, rrccR, rrccArchivo);
                return rrccArchivo;
            } catch (Exception rrccError) {
                throw new IllegalStateException(rrccError.getMessage(), rrccError);
            }
        }, rrccF -> rrccNotificador.rrccExito("Revisión exportada: " + rrccF.getName()),
                rrccMensaje -> rrccNotificador.rrccError("No se pudo exportar: " + rrccMensaje));
    }
}
