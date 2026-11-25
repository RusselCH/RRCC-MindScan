package com.ejemplo.rrccmindscan.controller;

import com.ejemplo.rrccmindscan.config.RRCCAnalisisPropiedades;
import com.ejemplo.rrccmindscan.model.RRCCCategoria;
import com.ejemplo.rrccmindscan.model.RRCCDocumento;
import com.ejemplo.rrccmindscan.model.RRCCTipoDato;
import com.ejemplo.rrccmindscan.service.RRCCAnalizadorService;
import com.ejemplo.rrccmindscan.service.RRCCAnalizadorService.RRCCClasificacion;
import com.ejemplo.rrccmindscan.service.RRCCAnalizadorService.RRCCDato;
import com.ejemplo.rrccmindscan.service.RRCCAnalizadorService.RRCCOracionPuntuada;
import com.ejemplo.rrccmindscan.service.RRCCAnalizadorService.RRCCRespuesta;
import com.ejemplo.rrccmindscan.service.RRCCAnalizadorService.RRCCTermino;
import com.ejemplo.rrccmindscan.service.RRCCDocumentoService;
import com.ejemplo.rrccmindscan.service.RRCCExtraccionService;
import com.ejemplo.rrccmindscan.service.RRCCExtraccionService.RRCCEntidad;
import com.ejemplo.rrccmindscan.service.RRCCExtraccionService.RRCCEvento;
import com.ejemplo.rrccmindscan.service.RRCCReporteService;
import com.ejemplo.rrccmindscan.service.RRCCTextoService;
import com.ejemplo.rrccmindscan.service.RRCCTextoService.RRCCEstadisticas;
import com.ejemplo.rrccmindscan.service.RRCCTextoService.RRCCOracion;
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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RRCCAnalisisController implements RRCCVista {

    public static final int RRCC_TERMINOS_NUBE = 45;
    public static final int RRCC_TERMINOS_GRAFICO = 12;
    public static final int RRCC_RESPUESTAS = 4;
    public static final int RRCC_LIMITE_TEXTO_COMPLETO = 1500;
    public static final int[][] RRCC_RANGOS_LARGO = {{1, 10}, {11, 20}, {21, 30}, {31, 40}, {41, 60}, {61, 100000}};
    public static final Object[][] RRCC_ESCALA = {
            {90, "Muy fácil", "Primaria"}, {80, "Fácil", "Secundaria inicial"}, {70, "Algo fácil", "Secundaria"},
            {60, "Normal", "Público general"}, {50, "Algo difícil", "Preuniversitario"},
            {30, "Difícil", "Universitario"}, {0, "Muy difícil", "Especialista"}};

    private final RRCCDocumentoService rrccDocumentos;
    private final RRCCAnalizadorService rrccAnalizador;
    private final RRCCTextoService rrccTexto;
    private final RRCCReporteService rrccReporte;
    private final RRCCNotificador rrccNotificador;
    private final RRCCContexto rrccContexto;
    private final RRCCMainController rrccMain;
    private final RRCCAnalisisPropiedades rrccPropiedades;
    private final RRCCExtraccionService rrccExtraccion;
    private final RRCCLector rrccLector;

    @FXML private Label rrccLblSubtitulo;
    @FXML private ComboBox<String> rrccCmbDocumento;
    @FXML private Button rrccBtnExportar;
    @FXML private StackPane rrccCajaCargando;
    @FXML private VBox rrccContenido;
    @FXML private Label rrccLblPalabras;
    @FXML private Label rrccLblDetallePalabras;
    @FXML private Label rrccLblOraciones;
    @FXML private Label rrccLblDetalleOraciones;
    @FXML private Label rrccLblLectura;
    @FXML private Label rrccLblDetalleLectura;
    @FXML private StackPane rrccCajaLegibilidad;
    @FXML private Label rrccLblNivel;
    @FXML private FlowPane rrccCajaCategoria;
    @FXML private ScrollPane rrccScroll;
    @FXML private VBox rrccSecciones;
    @FXML private VBox rrccIndice;
    @FXML private Slider rrccSldProporcion;
    @FXML private Label rrccLblProporcion;
    @FXML private ToggleButton rrccTglTextoCompleto;
    @FXML private Label rrccLblAhorro;
    @FXML private VBox rrccCajaResumen;
    @FXML private StackPane rrccCajaNube;
    @FXML private BarChart<Number, String> rrccGraficoTerminos;
    @FXML private FlowPane rrccCajaTiposDato;
    @FXML private Button rrccBtnCopiarDato;
    @FXML private Button rrccBtnVerDato;
    @FXML private Label rrccLblResumenCronologia;
    @FXML private VBox rrccCajaCronologia;
    @FXML private FlowPane rrccCajaTiposEntidad;
    @FXML private TableView<RRCCEntidad> rrccTablaEntidades;
    @FXML private TableColumn<RRCCEntidad, Label> rrccColTipoEntidad;
    @FXML private TableColumn<RRCCEntidad, String> rrccColNombreEntidad;
    @FXML private TableColumn<RRCCEntidad, Integer> rrccColVecesEntidad;
    @FXML private TableColumn<RRCCEntidad, String> rrccColContextoEntidad;
    @FXML private TableView<RRCCDato> rrccTablaDatos;
    @FXML private TableColumn<RRCCDato, Label> rrccColTipoDato;
    @FXML private TableColumn<RRCCDato, String> rrccColValorDato;
    @FXML private TableColumn<RRCCDato, Integer> rrccColVecesDato;
    @FXML private TableColumn<RRCCDato, String> rrccColContextoDato;
    @FXML private BarChart<String, Number> rrccGraficoLargos;
    @FXML private Label rrccLblInterpretacion;
    @FXML private GridPane rrccGridIndicadores;
    @FXML private VBox rrccCajaEscala;
    @FXML private TextField rrccTxtPregunta;
    @FXML private FlowPane rrccCajaSugerencias;
    @FXML private VBox rrccCajaRespuestas;

    private RRCCSelectorDocumento rrccSelector;
    private RRCCPaquete rrccActual;
    private final ObservableList<RRCCDato> rrccDatos = FXCollections.observableArrayList();
    private FilteredList<RRCCDato> rrccDatosFiltrados;
    private final ToggleGroup rrccGrupoTipos = new ToggleGroup();
    private long rrccSolicitud;

    private record RRCCPaquete(RRCCDocumento rrccDocumento, RRCCEstadisticas rrccEstadisticas, List<RRCCTermino> rrccTerminos,
                               List<RRCCOracionPuntuada> rrccResumen, List<RRCCDato> rrccDatos,
                               RRCCClasificacion rrccClasificacion, List<RRCCOracion> rrccOraciones,
                               List<RRCCEvento> rrccEventos, List<RRCCEntidad> rrccEntidades) {
    }

    @Override
    public void rrccIniciar() {
        rrccSelector = new RRCCSelectorDocumento(rrccCmbDocumento);
        rrccSelector.rrccAlCambiar(this::rrccAnalizar);

        rrccColTipoDato.setCellValueFactory(rrccC -> new SimpleObjectProperty<>(RRCCFormatos.rrccChip(
                rrccC.getValue().rrccTipo().rrccEtiqueta(), "rrcc-dato-" + rrccC.getValue().rrccTipo().name().substring(5).toLowerCase().replace('_', '-'))));
        rrccColValorDato.setCellValueFactory(rrccC -> new SimpleStringProperty(rrccC.getValue().rrccValor()));
        rrccColVecesDato.setCellValueFactory(rrccC -> new SimpleObjectProperty<>(rrccC.getValue().rrccOcurrencias()));
        rrccColContextoDato.setCellValueFactory(rrccC -> new SimpleStringProperty(rrccC.getValue().rrccContexto()));
        rrccColTipoDato.setSortable(false);
        rrccDatosFiltrados = new FilteredList<>(rrccDatos, rrccD -> true);
        rrccTablaDatos.setItems(rrccDatosFiltrados);
        rrccTablaDatos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        rrccTablaDatos.setPlaceholder(new Label("No se encontraron datos de este tipo."));
        rrccBtnCopiarDato.disableProperty().bind(rrccTablaDatos.getSelectionModel().selectedItemProperty().isNull());
        rrccBtnVerDato.disableProperty().bind(rrccTablaDatos.getSelectionModel().selectedItemProperty().isNull());
        rrccTablaDatos.setOnMouseClicked(rrccE -> {
            if (rrccE.getClickCount() == 2) {
                rrccOnVerDato();
            }
        });
        rrccColTipoEntidad.setCellValueFactory(rrccC -> new SimpleObjectProperty<>(RRCCFormatos.rrccChip(rrccC.getValue().rrccTipo(),
                rrccClaseEntidad(rrccC.getValue().rrccTipo()))));
        rrccColNombreEntidad.setCellValueFactory(rrccC -> new SimpleStringProperty(rrccC.getValue().rrccNombre()));
        rrccColVecesEntidad.setCellValueFactory(rrccC -> new SimpleObjectProperty<>(rrccC.getValue().rrccVeces()));
        rrccColContextoEntidad.setCellValueFactory(rrccC -> new SimpleStringProperty(rrccC.getValue().rrccContexto()));
        rrccColTipoEntidad.setSortable(false);
        rrccTablaEntidades.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        rrccTablaEntidades.setPlaceholder(new Label("No se detectaron nombres propios en este documento."));
        rrccTablaEntidades.setOnMouseClicked(rrccE -> {
            RRCCEntidad rrccEnt = rrccTablaEntidades.getSelectionModel().getSelectedItem();
            if (rrccE.getClickCount() == 2 && rrccEnt != null && rrccActual != null) {
                rrccLector.rrccAbrir(rrccActual.rrccDocumento(), rrccEnt.rrccPosicion(), rrccEnt.rrccNombre().length(),
                        rrccEnt.rrccTipo() + ": " + rrccEnt.rrccNombre() + " (" + rrccEnt.rrccVeces() + " menciones)");
            }
        });

        rrccSldProporcion.valueProperty().addListener((rrccO, rrccA, rrccN) -> rrccLblProporcion.setText(Math.round(rrccN.doubleValue() * 100) + " %"));
        rrccLblProporcion.setText(Math.round(rrccSldProporcion.getValue() * 100) + " %");
        rrccSldProporcion.valueChangingProperty().addListener((rrccO, rrccAntes, rrccCambiando) -> {
            if (!rrccCambiando) {
                rrccRecalcularResumen();
            }
        });
        rrccSldProporcion.setOnMouseReleased(rrccE -> rrccRecalcularResumen());
        rrccGraficoTerminos.setAnimated(false);
        rrccConstruirIndice();
        rrccMostrarContenido(false);
    }

    private void rrccConstruirIndice() {
        rrccIndice.getChildren().clear();
        List<Label> rrccEnlaces = new ArrayList<>();
        int rrccTono = 0;
        for (javafx.scene.Node rrccSeccion : rrccSecciones.getChildren()) {
            rrccSeccion.getStyleClass().add("rrcc-tono-" + (rrccTono++ % 7));
            String rrccNumero = "";
            String rrccTitulo = "";
            if (rrccSeccion instanceof VBox rrccCaja && !rrccCaja.getChildren().isEmpty()
                    && rrccCaja.getChildren().get(0) instanceof HBox rrccCabecera && rrccCabecera.getChildren().size() >= 2) {
                rrccNumero = ((Label) rrccCabecera.getChildren().get(0)).getText();
                rrccTitulo = ((Label) rrccCabecera.getChildren().get(1)).getText();
            }
            Label rrccEnlace = new Label(rrccNumero + "   " + rrccTitulo);
            rrccEnlace.getStyleClass().addAll("rrcc-indice-enlace", "rrcc-tono-" + ((rrccTono - 1) % 7));
            rrccEnlace.setMaxWidth(Double.MAX_VALUE);
            rrccEnlace.setOnMouseClicked(rrccE -> rrccIrASeccion(rrccSeccion));
            rrccEnlaces.add(rrccEnlace);
            rrccIndice.getChildren().add(rrccEnlace);
        }
        rrccScroll.vvalueProperty().addListener((rrccO, rrccA, rrccN) -> rrccMarcarSeccionActiva(rrccEnlaces));
        if (!rrccEnlaces.isEmpty()) {
            rrccEnlaces.get(0).getStyleClass().add("rrcc-indice-activo");
        }
    }

    private double rrccPosicionDe(javafx.scene.Node rrccNodo) {
        return rrccNodo.localToScene(0, 0).getY() - rrccScroll.getContent().localToScene(0, 0).getY();
    }

    private void rrccIrASeccion(javafx.scene.Node rrccSeccion) {
        double rrccRecorrido = rrccScroll.getContent().getBoundsInLocal().getHeight() - rrccScroll.getViewportBounds().getHeight();
        if (rrccRecorrido <= 0) {
            return;
        }
        rrccScroll.setVvalue(Math.max(0, Math.min(1, (rrccPosicionDe(rrccSeccion) - 16) / rrccRecorrido)));
    }

    private void rrccMarcarSeccionActiva(List<Label> rrccEnlaces) {
        double rrccRecorrido = rrccScroll.getContent().getBoundsInLocal().getHeight() - rrccScroll.getViewportBounds().getHeight();
        double rrccArriba = Math.max(0, rrccRecorrido) * rrccScroll.getVvalue() + 120;
        int rrccActiva = 0;
        for (int rrccI = 0; rrccI < rrccSecciones.getChildren().size(); rrccI++) {
            if (rrccPosicionDe(rrccSecciones.getChildren().get(rrccI)) <= rrccArriba) {
                rrccActiva = rrccI;
            }
        }
        for (int rrccI = 0; rrccI < rrccEnlaces.size(); rrccI++) {
            rrccEnlaces.get(rrccI).getStyleClass().remove("rrcc-indice-activo");
            if (rrccI == rrccActiva) {
                rrccEnlaces.get(rrccI).getStyleClass().add("rrcc-indice-activo");
            }
        }
    }

    @Override
    public void rrccRefrescar() {
        rrccSelector.rrccCargar(rrccDocumentos.rrccListar());
        Long rrccPendiente = rrccContexto.rrccTomarActivo();
        if (rrccPendiente != null) {
            rrccSelector.rrccSeleccionar(rrccPendiente);
        }
        RRCCDocumento rrccElegido = rrccSelector.rrccSeleccionado();
        if (rrccElegido == null && !rrccSelector.rrccDocumentos().isEmpty()) {
            rrccSelector.rrccSeleccionar(rrccSelector.rrccDocumentos().get(0).getRrccId());
            rrccElegido = rrccSelector.rrccSeleccionado();
        }
        if (rrccElegido == null) {
            rrccActual = null;
            rrccMostrarContenido(false);
            rrccLblSubtitulo.setText("Importa un documento en la Biblioteca para analizarlo.");
            return;
        }
        if (rrccActual == null || !rrccActual.rrccDocumento().getRrccId().equals(rrccElegido.getRrccId())) {
            rrccAnalizar(rrccElegido);
        }
    }

    private void rrccMostrarContenido(boolean rrccVisible) {
        rrccContenido.setVisible(rrccVisible);
        rrccContenido.setManaged(rrccVisible);
        rrccBtnExportar.setDisable(!rrccVisible);
    }

    private void rrccCargando(boolean rrccSi) {
        rrccCajaCargando.setVisible(rrccSi);
        rrccCajaCargando.setManaged(rrccSi);
    }

    private void rrccAnalizar(RRCCDocumento rrccDoc) {
        if (rrccDoc == null) {
            return;
        }
        long rrccNumero = ++rrccSolicitud;
        double rrccProporcion = rrccSldProporcion.getValue();
        rrccCargando(true);
        RRCCTareas.rrccEjecutar(() -> {
            List<String> rrccCorpus = rrccDocumentos.rrccListar().stream().map(RRCCDocumento::getRrccContenido).toList();
            String rrccT = rrccDoc.getRrccContenido();
            return new RRCCPaquete(rrccDoc, rrccTexto.rrccEstadisticas(rrccT),
                    rrccAnalizador.rrccPalabrasClave(rrccT, rrccCorpus, RRCC_TERMINOS_NUBE),
                    rrccAnalizador.rrccResumen(rrccT, rrccProporcion), rrccAnalizador.rrccDatos(rrccT),
                    rrccAnalizador.rrccClasificar(rrccT), rrccTexto.rrccOraciones(rrccT),
                    rrccExtraccion.rrccCronologia(rrccT), rrccExtraccion.rrccEntidades(rrccT));
        }, rrccPaquete -> {
            if (rrccNumero != rrccSolicitud) {
                return;
            }
            rrccCargando(false);
            rrccActual = rrccPaquete;
            rrccPintar();
        }, rrccMensaje -> {
            rrccCargando(false);
            rrccNotificador.rrccError("No se pudo analizar: " + rrccMensaje);
        });
    }

    private void rrccPintar() {
        RRCCEstadisticas rrccEst = rrccActual.rrccEstadisticas();
        RRCCDocumento rrccDoc = rrccActual.rrccDocumento();
        rrccLblSubtitulo.setText(rrccDoc.getRrccNombre() + "  |  " + rrccDoc.getRrccPaginas() + " páginas  |  "
                + rrccDoc.rrccTamanoLegible());
        rrccLblPalabras.setText(RRCCFormatos.rrccNumero(rrccEst.rrccPalabras()));
        rrccLblDetallePalabras.setText(RRCCFormatos.rrccNumero(rrccEst.rrccPalabrasUnicas()) + " distintas  |  "
                + RRCCFormatos.rrccNumero(rrccEst.rrccCaracteres()) + " caracteres");
        rrccLblOraciones.setText(RRCCFormatos.rrccNumero(rrccEst.rrccOraciones()));
        rrccLblDetalleOraciones.setText(rrccEst.rrccParrafos() + " párrafos  |  " + String.format("%.1f", rrccEst.rrccPalabrasPorOracion())
                + " palabras por oración");
        rrccLblLectura.setText(RRCCFormatos.rrccMinutos(rrccEst.rrccMinutosLectura()));
        rrccCajaLegibilidad.getChildren().setAll(RRCCGraficos.rrccAnillo(rrccEst.rrccLegibilidad() / 100,
                String.valueOf(Math.round(rrccEst.rrccLegibilidad())), "de 100",
                RRCCFormatos.rrccClaseLegibilidad(rrccEst.rrccLegibilidad()), 32));
        rrccLblNivel.setText(rrccEst.rrccNivel());
        RRCCCategoria rrccCategoria = rrccActual.rrccClasificacion().rrccCategoria();
        Label rrccChip = RRCCFormatos.rrccChipCategoria(rrccCategoria);
        Tooltip.install(rrccChip, new Tooltip(rrccActual.rrccClasificacion().rrccPuntajes().entrySet().stream()
                .filter(rrccE -> rrccE.getValue() > 0)
                .sorted(Map.Entry.<RRCCCategoria, Integer>comparingByValue().reversed())
                .map(rrccE -> rrccE.getKey().rrccEtiqueta() + ": " + rrccE.getValue() + " términos")
                .collect(Collectors.joining("\n", "Términos característicos encontrados\n", ""))));
        rrccCajaCategoria.getChildren().setAll(rrccChip);

        rrccPintarResumen();
        rrccPintarTerminos();
        rrccPintarDatos();
        rrccPintarEstadisticas();
        rrccPintarCronologia();
        rrccPintarEntidades();
        rrccPintarSugerencias();
        rrccCajaRespuestas.getChildren().clear();
        rrccTxtPregunta.clear();
        rrccMostrarContenido(true);
    }

    private void rrccRecalcularResumen() {
        if (rrccActual == null) {
            return;
        }
        RRCCPaquete rrccBase = rrccActual;
        double rrccProporcion = rrccSldProporcion.getValue();
        RRCCTareas.rrccEjecutar(() -> rrccAnalizador.rrccResumen(rrccBase.rrccDocumento().getRrccContenido(), rrccProporcion),
                rrccResumen -> {
                    if (rrccActual != rrccBase) {
                        return;
                    }
                    rrccActual = new RRCCPaquete(rrccBase.rrccDocumento(), rrccBase.rrccEstadisticas(), rrccBase.rrccTerminos(),
                            rrccResumen, rrccBase.rrccDatos(), rrccBase.rrccClasificacion(), rrccBase.rrccOraciones(),
                            rrccBase.rrccEventos(), rrccBase.rrccEntidades());
                    rrccPintarResumen();
                }, rrccMensaje -> rrccNotificador.rrccError(rrccMensaje));
    }

    private void rrccPintarResumen() {
        List<RRCCOracionPuntuada> rrccTodas = rrccActual.rrccResumen();
        List<RRCCOracionPuntuada> rrccElegidas = rrccTodas.stream().filter(RRCCOracionPuntuada::rrccSeleccionada).toList();
        int rrccPalabrasResumen = rrccElegidas.stream().mapToInt(rrccO -> rrccTexto.rrccPalabras(rrccO.rrccTexto()).size()).sum();
        int rrccPalabrasTotal = Math.max(1, rrccActual.rrccEstadisticas().rrccPalabras());
        double rrccMinutosResumen = rrccPalabrasResumen / (double) rrccPropiedades.rrccPalabrasPorMinuto();
        rrccLblAhorro.setText("TextRank eligió " + rrccElegidas.size() + " de " + rrccTodas.size() + " oraciones ("
                + RRCCFormatos.rrccPorcentaje(rrccPalabrasResumen / (double) rrccPalabrasTotal) + " del texto). Lees en "
                + RRCCFormatos.rrccMinutos(rrccMinutosResumen) + " en lugar de "
                + RRCCFormatos.rrccMinutos(rrccActual.rrccEstadisticas().rrccMinutosLectura()) + ".");
        rrccLblDetalleLectura.setText("Con el resumen: " + RRCCFormatos.rrccMinutos(rrccMinutosResumen));
        rrccCajaResumen.getChildren().clear();
        if (rrccTglTextoCompleto.isSelected()) {
            TextFlow rrccFlujo = new TextFlow();
            rrccFlujo.getStyleClass().addAll("rrcc-flujo", "rrcc-texto-completo");
            int rrccLimite = Math.min(rrccTodas.size(), RRCC_LIMITE_TEXTO_COMPLETO);
            for (int rrccI = 0; rrccI < rrccLimite; rrccI++) {
                RRCCOracionPuntuada rrccO = rrccTodas.get(rrccI);
                Text rrccT = new Text(rrccO.rrccTexto() + " ");
                rrccT.getStyleClass().addAll("rrcc-texto", rrccO.rrccSeleccionada() ? "rrcc-resumen-marcado" : "rrcc-resumen-tenue");
                rrccFlujo.getChildren().add(rrccT);
            }
            rrccCajaResumen.getChildren().add(rrccFlujo);
            return;
        }
        int rrccPosicion = 1;
        for (RRCCOracionPuntuada rrccO : rrccElegidas) {
            Label rrccNumero = new Label(String.valueOf(rrccPosicion++));
            rrccNumero.getStyleClass().add("rrcc-numero");
            rrccNumero.setMinWidth(30);
            rrccNumero.setAlignment(Pos.CENTER);
            Label rrccTextoOracion = new Label(rrccO.rrccTexto());
            rrccTextoOracion.setWrapText(true);
            rrccTextoOracion.getStyleClass().add("rrcc-oracion");
            HBox rrccMedidor = RRCCGraficos.rrccMedidor(rrccO.rrccPuntaje(), "rrcc-acento",
                    "Relevancia " + Math.round(rrccO.rrccPuntaje() * 100) + "  |  oración " + (rrccO.rrccIndice() + 1));
            rrccMedidor.setMaxWidth(360);
            VBox rrccTextos = new VBox(6, rrccTextoOracion, rrccMedidor);
            HBox.setHgrow(rrccTextos, Priority.ALWAYS);
            HBox rrccFila = new HBox(12, rrccNumero, rrccTextos);
            rrccFila.getStyleClass().add("rrcc-fila");
            rrccCajaResumen.getChildren().add(rrccFila);
        }
    }

    private void rrccPintarTerminos() {
        rrccCajaNube.getChildren().setAll(RRCCGraficos.rrccNube(rrccActual.rrccTerminos(), rrccT -> {
            rrccContexto.rrccPrepararBusqueda(rrccT.rrccForma());
            rrccMain.rrccIrA(RRCCMainController.RRCC_VISTA_BUSQUEDA);
        }));
        rrccGraficoTerminos.getData().clear();
        XYChart.Series<Number, String> rrccSerie = new XYChart.Series<>();
        List<RRCCTermino> rrccFrecuentes = rrccActual.rrccTerminos().stream()
                .sorted(Comparator.comparingInt(RRCCTermino::rrccFrecuencia).reversed())
                .limit(RRCC_TERMINOS_GRAFICO)
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.reverse(rrccFrecuentes);
        for (RRCCTermino rrccT : rrccFrecuentes) {
            XYChart.Data<Number, String> rrccDato = new XYChart.Data<>(rrccT.rrccFrecuencia(), rrccT.rrccForma());
            rrccDato.setExtraValue(new String[]{"rrcc-barra-acento", rrccT.rrccForma() + ": " + rrccT.rrccFrecuencia() + " veces"});
            rrccSerie.getData().add(rrccDato);
        }
        rrccGraficoTerminos.getData().add(rrccSerie);
        for (XYChart.Data<Number, String> rrccDato : rrccSerie.getData()) {
            if (rrccDato.getNode() != null && rrccDato.getExtraValue() instanceof String[] rrccInfo) {
                rrccDato.getNode().getStyleClass().add(rrccInfo[0]);
                Tooltip.install(rrccDato.getNode(), new Tooltip(rrccInfo[1]));
            }
        }
    }

    private void rrccPintarDatos() {
        rrccDatos.setAll(rrccActual.rrccDatos());
        rrccCajaTiposDato.getChildren().clear();
        rrccGrupoTipos.getToggles().clear();
        Map<RRCCTipoDato, Long> rrccConteo = rrccActual.rrccDatos().stream()
                .collect(Collectors.groupingBy(RRCCDato::rrccTipo, () -> new EnumMap<>(RRCCTipoDato.class), Collectors.counting()));
        ToggleButton rrccTodos = new ToggleButton("Todos (" + rrccActual.rrccDatos().size() + ")");
        rrccTodos.setToggleGroup(rrccGrupoTipos);
        rrccTodos.setOnAction(rrccE -> rrccDatosFiltrados.setPredicate(rrccD -> true));
        rrccTodos.getStyleClass().add("rrcc-filtro");
        rrccCajaTiposDato.getChildren().add(rrccTodos);
        for (Map.Entry<RRCCTipoDato, Long> rrccE : rrccConteo.entrySet()) {
            ToggleButton rrccB = new ToggleButton(rrccE.getKey().rrccEtiqueta() + " (" + rrccE.getValue() + ")");
            rrccB.setToggleGroup(rrccGrupoTipos);
            rrccB.getStyleClass().add("rrcc-filtro");
            rrccB.setOnAction(rrccEv -> rrccDatosFiltrados.setPredicate(rrccD -> rrccD.rrccTipo() == rrccE.getKey()));
            rrccCajaTiposDato.getChildren().add(rrccB);
        }
        rrccGrupoTipos.selectToggle(rrccTodos);
        rrccDatosFiltrados.setPredicate(rrccD -> true);
    }

    private void rrccPintarEstadisticas() {
        int[] rrccConteo = new int[RRCC_RANGOS_LARGO.length];
        List<Integer> rrccLargos = new ArrayList<>();
        for (RRCCOracion rrccO : rrccActual.rrccOraciones()) {
            int rrccLargo = rrccTexto.rrccPalabras(rrccO.rrccTexto()).size();
            rrccLargos.add(rrccLargo);
            for (int rrccI = 0; rrccI < RRCC_RANGOS_LARGO.length; rrccI++) {
                if (rrccLargo >= RRCC_RANGOS_LARGO[rrccI][0] && rrccLargo <= RRCC_RANGOS_LARGO[rrccI][1]) {
                    rrccConteo[rrccI]++;
                    break;
                }
            }
        }
        rrccGraficoLargos.getData().clear();
        XYChart.Series<String, Number> rrccSerie = new XYChart.Series<>();
        for (int rrccI = 0; rrccI < RRCC_RANGOS_LARGO.length; rrccI++) {
            int[] rrccR = RRCC_RANGOS_LARGO[rrccI];
            String rrccEtiqueta = rrccR[1] > 1000 ? "más de " + (rrccR[0] - 1) : rrccR[0] + "-" + rrccR[1];
            String rrccClase = rrccR[1] <= 20 ? "rrcc-bueno" : rrccR[1] <= 40 ? "rrcc-medio" : "rrcc-malo";
            rrccSerie.getData().add(RRCCGraficos.rrccBarra(rrccEtiqueta, rrccConteo[rrccI], rrccClase,
                    rrccConteo[rrccI] + " oraciones de " + rrccEtiqueta + " palabras"));
        }
        rrccGraficoLargos.getData().add(rrccSerie);
        RRCCGraficos.rrccPintar(rrccSerie);

        int rrccLimite = rrccPropiedades.rrccPalabrasOracionLarga();
        long rrccLargas = rrccLargos.stream().filter(rrccL -> rrccL > rrccLimite).count();
        int rrccMaxima = rrccLargos.stream().mapToInt(Integer::intValue).max().orElse(0);
        rrccLblInterpretacion.setText(rrccLargas == 0
                ? "Ninguna oración supera las " + rrccLimite + " palabras: el texto mantiene un ritmo de lectura cómodo."
                : rrccLargas + " oraciones superan las " + rrccLimite + " palabras (la más larga tiene " + rrccMaxima
                + "). Dividirlas mejoraría la legibilidad.");

        RRCCEstadisticas rrccEst = rrccActual.rrccEstadisticas();
        double rrccRiqueza = rrccEst.rrccPalabras() == 0 ? 0 : rrccEst.rrccPalabrasUnicas() / (double) rrccEst.rrccPalabras();
        String[][] rrccFilas = {
                {"Palabras por oración", String.format("%.1f", rrccEst.rrccPalabrasPorOracion())},
                {"Sílabas por palabra", String.format("%.2f", rrccEst.rrccSilabasPorPalabra())},
                {"Riqueza léxica", RRCCFormatos.rrccPorcentaje(rrccRiqueza) + " de palabras distintas"},
                {"Oración más larga", rrccMaxima + " palabras"},
                {"Índice Fernández-Huerta", String.format("%.1f", rrccEst.rrccLegibilidad())},
                {"Velocidad de lectura usada", rrccPropiedades.rrccPalabrasPorMinuto() + " palabras/min"}};
        rrccGridIndicadores.getChildren().clear();
        for (int rrccI = 0; rrccI < rrccFilas.length; rrccI++) {
            Label rrccClave = new Label(rrccFilas[rrccI][0]);
            rrccClave.getStyleClass().add("rrcc-dato-clave");
            Label rrccValor = new Label(rrccFilas[rrccI][1]);
            rrccValor.getStyleClass().add("rrcc-dato-valor");
            rrccGridIndicadores.add(rrccClave, 0, rrccI);
            rrccGridIndicadores.add(rrccValor, 1, rrccI);
        }
        rrccCajaEscala.getChildren().clear();
        double rrccLegibilidad = rrccEst.rrccLegibilidad();
        boolean rrccMarcado = false;
        for (Object[] rrccNivel : RRCC_ESCALA) {
            boolean rrccEsActual = !rrccMarcado && rrccLegibilidad >= (Integer) rrccNivel[0];
            rrccMarcado |= rrccEsActual;
            Label rrccL = new Label((rrccEsActual ? "➜  " : "     ") + rrccNivel[1] + "  (" + rrccNivel[0] + "+)  -  " + rrccNivel[2]);
            rrccL.getStyleClass().add(rrccEsActual ? "rrcc-escala-actual" : "rrcc-escala");
            rrccL.setMaxWidth(Double.MAX_VALUE);
            rrccCajaEscala.getChildren().add(rrccL);
        }
    }

    private String rrccClaseEntidad(String rrccTipo) {
        return switch (rrccTipo) {
            case RRCCExtraccionService.RRCC_PERSONA -> "rrcc-ent-persona";
            case RRCCExtraccionService.RRCC_ORGANIZACION -> "rrcc-ent-organizacion";
            case RRCCExtraccionService.RRCC_LUGAR -> "rrcc-ent-lugar";
            default -> "rrcc-chip-suave";
        };
    }

    private void rrccPintarEntidades() {
        List<RRCCEntidad> rrccLista = rrccActual.rrccEntidades();
        rrccTablaEntidades.getItems().setAll(rrccLista);
        rrccCajaTiposEntidad.getChildren().clear();
        Map<String, Long> rrccConteo = rrccLista.stream().collect(Collectors.groupingBy(RRCCEntidad::rrccTipo, LinkedHashMap::new, Collectors.counting()));
        rrccConteo.forEach((rrccTipo, rrccN) -> rrccCajaTiposEntidad.getChildren().add(
                RRCCFormatos.rrccChip(rrccN + " " + rrccTipo.toLowerCase() + (rrccN == 1 ? "" : rrccTipo.endsWith("n") ? "es" : "s"),
                        rrccClaseEntidad(rrccTipo))));
    }

    private void rrccPintarCronologia() {
        List<RRCCEvento> rrccEventos = rrccActual.rrccEventos();
        rrccCajaCronologia.getChildren().clear();
        if (rrccEventos.isEmpty()) {
            rrccLblResumenCronologia.setText("Sin fechas");
            rrccCajaCronologia.getChildren().add(RRCCGraficos.rrccVacio("Sin fechas",
                    "El documento no menciona fechas completas (día, mes y año)."));
            return;
        }
        LocalDate rrccHoy = LocalDate.now();
        long rrccVencidas = rrccEventos.stream().filter(rrccE -> rrccE.rrccFecha().isBefore(rrccHoy)).count();
        rrccLblResumenCronologia.setText(rrccEventos.size() + " fechas  |  " + rrccVencidas + " pasadas  |  "
                + (rrccEventos.size() - rrccVencidas) + " por venir");
        DateTimeFormatter rrccFormato = DateTimeFormatter.ofPattern("EEE d 'de' MMMM 'de' yyyy", RRCCFormatos.RRCC_LOCALE);
        boolean rrccHoyMarcado = false;
        for (int rrccI = 0; rrccI < rrccEventos.size(); rrccI++) {
            RRCCEvento rrccE = rrccEventos.get(rrccI);
            long rrccDias = ChronoUnit.DAYS.between(rrccHoy, rrccE.rrccFecha());
            if (!rrccHoyMarcado && rrccDias >= 0) {
                rrccCajaCronologia.getChildren().add(rrccMarcaHoy(rrccHoy, rrccFormato));
                rrccHoyMarcado = true;
            }
            String rrccEstado = rrccDias < 0 ? "rrcc-evento-pasado" : rrccDias <= 30 ? "rrcc-evento-proximo" : "rrcc-evento-futuro";
            String rrccCuando = rrccDias == 0 ? "Hoy" : rrccDias < 0 ? "Hace " + rrccTiempo(-rrccDias) : "En " + rrccTiempo(rrccDias);
            Region rrccPunto = new Region();
            rrccPunto.getStyleClass().addAll("rrcc-evento-punto", rrccEstado);
            Region rrccLinea = new Region();
            rrccLinea.getStyleClass().add("rrcc-evento-linea");
            VBox.setVgrow(rrccLinea, Priority.ALWAYS);
            VBox rrccEje = new VBox(rrccPunto, rrccLinea);
            rrccEje.setAlignment(Pos.TOP_CENTER);
            rrccEje.setMinWidth(22);
            Label rrccFecha = new Label(rrccE.rrccFecha().format(rrccFormato));
            rrccFecha.getStyleClass().add("rrcc-evento-fecha");
            Label rrccRelativo = RRCCFormatos.rrccChip(rrccCuando, rrccEstado);
            HBox rrccCabecera = new HBox(10, rrccFecha, rrccRelativo);
            rrccCabecera.setAlignment(Pos.CENTER_LEFT);
            if (rrccE.rrccAnioDeducido()) {
                rrccCabecera.getChildren().add(RRCCFormatos.rrccChip("año deducido", "rrcc-chip-suave"));
            }
            TextFlow rrccOracion = RRCCGraficos.rrccResaltarFrase(rrccE.rrccOracion(), rrccTexto.rrccNormalizar(rrccE.rrccTextoFecha()), rrccTexto);
            VBox rrccTarjeta = new VBox(6, rrccCabecera, rrccOracion);
            rrccTarjeta.getStyleClass().addAll("rrcc-evento", "rrcc-fila-clic");
            HBox.setHgrow(rrccTarjeta, Priority.ALWAYS);
            rrccTarjeta.setOnMouseClicked(rrccEv -> rrccLector.rrccAbrir(rrccActual.rrccDocumento(), rrccE.rrccPosicion(),
                    rrccE.rrccTextoFecha().length(), "Fecha: " + rrccE.rrccFecha().format(rrccFormato) + "  (" + rrccCuando.toLowerCase() + ")"));
            HBox rrccFila = new HBox(12, rrccEje, rrccTarjeta);
            rrccCajaCronologia.getChildren().add(rrccFila);
        }
        if (!rrccHoyMarcado) {
            rrccCajaCronologia.getChildren().add(rrccMarcaHoy(rrccHoy, rrccFormato));
        }
    }

    private HBox rrccMarcaHoy(LocalDate rrccHoy, DateTimeFormatter rrccFormato) {
        Label rrccHoyL = new Label("HOY  ·  " + rrccHoy.format(rrccFormato));
        rrccHoyL.getStyleClass().add("rrcc-evento-hoy");
        Region rrccRaya = new Region();
        rrccRaya.getStyleClass().add("rrcc-evento-raya");
        HBox.setHgrow(rrccRaya, Priority.ALWAYS);
        HBox rrccFila = new HBox(10, rrccHoyL, rrccRaya);
        rrccFila.setAlignment(Pos.CENTER_LEFT);
        rrccFila.getStyleClass().add("rrcc-evento-fila-hoy");
        return rrccFila;
    }

    private String rrccTiempo(long rrccDias) {
        if (rrccDias < 31) {
            return rrccDias + (rrccDias == 1 ? " día" : " días");
        }
        if (rrccDias < 365) {
            long rrccMeses = Math.round(rrccDias / 30.4);
            return rrccMeses + (rrccMeses == 1 ? " mes" : " meses");
        }
        double rrccAnios = rrccDias / 365.25;
        return String.format("%.1f años", rrccAnios);
    }

    @FXML
    private void rrccOnVerDato() {
        RRCCDato rrccD = rrccTablaDatos.getSelectionModel().getSelectedItem();
        if (rrccD != null && rrccActual != null) {
            int rrccPos = rrccActual.rrccDocumento().getRrccContenido().indexOf(rrccD.rrccValor(), Math.max(0, rrccD.rrccPosicion() - 5));
            rrccLector.rrccAbrir(rrccActual.rrccDocumento(), rrccPos < 0 ? rrccD.rrccPosicion() : rrccPos, rrccD.rrccValor().length(),
                    rrccD.rrccTipo().rrccEtiqueta() + ": " + rrccD.rrccValor() + "  (aparece " + rrccD.rrccOcurrencias() + " veces)");
        }
    }

    private void rrccPintarSugerencias() {
        rrccCajaSugerencias.getChildren().clear();
        Set<RRCCTipoDato> rrccTipos = rrccActual.rrccDatos().stream().map(RRCCDato::rrccTipo).collect(Collectors.toSet());
        List<String> rrccCandidatas = new ArrayList<>();
        if (rrccTipos.contains(RRCCTipoDato.RRCC_MONTO)) {
            rrccCandidatas.add("¿Cuánto es el monto total?");
        }
        if (rrccTipos.contains(RRCCTipoDato.RRCC_FECHA)) {
            rrccCandidatas.add("¿Cuándo es la fecha límite?");
        }
        if (rrccTipos.contains(RRCCTipoDato.RRCC_PORCENTAJE)) {
            rrccCandidatas.add("¿Cuál es el porcentaje?");
        }
        rrccActual.rrccEntidades().stream().filter(rrccE -> RRCCExtraccionService.RRCC_PERSONA.equals(rrccE.rrccTipo())).findFirst()
                .ifPresent(rrccE -> rrccCandidatas.add("¿Qué responsabilidad tiene " + rrccE.rrccNombre() + "?"));
        rrccActual.rrccEntidades().stream().filter(rrccE -> RRCCExtraccionService.RRCC_LUGAR.equals(rrccE.rrccTipo())).findFirst()
                .ifPresent(rrccE -> rrccCandidatas.add("¿Qué ocurre en " + rrccE.rrccNombre() + "?"));
        rrccActual.rrccTerminos().stream().limit(4)
                .forEach(rrccT -> rrccCandidatas.add("¿Qué dice sobre " + rrccT.rrccForma() + "?"));
        String rrccContenidoDoc = rrccActual.rrccDocumento().getRrccContenido();
        int rrccAgregadas = 0;
        for (String rrccP : rrccCandidatas) {
            if (rrccAgregadas >= 6 || rrccAnalizador.rrccResponder(rrccContenidoDoc, rrccP, 1).isEmpty()) {
                continue;
            }
            Button rrccB = new Button(rrccP);
            rrccB.getStyleClass().add("rrcc-sugerencia");
            rrccB.setOnAction(rrccE -> {
                rrccTxtPregunta.setText(rrccP);
                rrccOnPreguntar();
            });
            rrccCajaSugerencias.getChildren().add(rrccB);
            rrccAgregadas++;
        }
    }

    @FXML
    private void rrccOnPreguntar() {
        if (rrccActual == null) {
            return;
        }
        String rrccPregunta = rrccTxtPregunta.getText() == null ? "" : rrccTxtPregunta.getText().strip();
        rrccCajaRespuestas.getChildren().clear();
        if (rrccPregunta.isEmpty()) {
            rrccNotificador.rrccAdvertencia("Escribe una pregunta.");
            return;
        }
        List<RRCCRespuesta> rrccRespuestas = rrccAnalizador.rrccResponder(rrccActual.rrccDocumento().getRrccContenido(),
                rrccPregunta, RRCC_RESPUESTAS);
        if (rrccRespuestas.isEmpty()) {
            rrccCajaRespuestas.getChildren().add(RRCCGraficos.rrccVacio("Sin coincidencias",
                    "Ninguna oración contiene los términos de la pregunta. Prueba con otras palabras o usa la Búsqueda global."));
            return;
        }
        double rrccMejor = rrccRespuestas.get(0).rrccPuntaje();
        List<RRCCOracion> rrccOraciones = rrccActual.rrccOraciones();
        for (int rrccI = 0; rrccI < rrccRespuestas.size(); rrccI++) {
            RRCCRespuesta rrccR = rrccRespuestas.get(rrccI);
            Label rrccEtiqueta = new Label(rrccI == 0 ? "Respuesta más probable" : "Alternativa " + rrccI);
            rrccEtiqueta.getStyleClass().add(rrccI == 0 ? "rrcc-respuesta-principal" : "rrcc-respuesta-etiqueta");
            TextFlow rrccTextoR = RRCCGraficos.rrccResaltar(rrccR.rrccOracion(), rrccR.rrccClaves(), rrccTexto);
            rrccTextoR.getStyleClass().add("rrcc-respuesta-texto");
            VBox rrccTarjeta = new VBox(8, rrccEtiqueta, rrccTextoR);
            int rrccIndice = rrccR.rrccIndice();
            if (rrccIndice > 0 || rrccIndice < rrccOraciones.size() - 1) {
                String rrccAntes = rrccIndice > 0 ? rrccTexto.rrccRecortar(rrccOraciones.get(rrccIndice - 1).rrccTexto(), 160) : "";
                String rrccDespues = rrccIndice < rrccOraciones.size() - 1
                        ? rrccTexto.rrccRecortar(rrccOraciones.get(rrccIndice + 1).rrccTexto(), 160) : "";
                Label rrccContextoL = new Label("Contexto:  " + (rrccAntes.isEmpty() ? "" : "... " + rrccAntes + "  ")
                        + "[oración " + (rrccIndice + 1) + "]" + (rrccDespues.isEmpty() ? "" : "  " + rrccDespues + " ..."));
                rrccContextoL.setWrapText(true);
                rrccContextoL.getStyleClass().add("rrcc-texto-suave");
                rrccTarjeta.getChildren().add(rrccContextoL);
            }
            HBox rrccMedidor = RRCCGraficos.rrccMedidor(rrccR.rrccPuntaje() / rrccMejor, rrccI == 0 ? "rrcc-bueno" : "rrcc-acento",
                    "Relevancia " + Math.round(rrccR.rrccPuntaje() / rrccMejor * 100) + " %  |  coinciden: "
                            + String.join(", ", rrccR.rrccClaves()));
            rrccMedidor.setMaxWidth(460);
            rrccTarjeta.getChildren().add(rrccMedidor);
            rrccTarjeta.getStyleClass().addAll("rrcc-fila", rrccI == 0 ? "rrcc-fila-destacada" : "rrcc-fila-normal");
            rrccCajaRespuestas.getChildren().add(rrccTarjeta);
        }
    }

    @FXML
    private void rrccOnCambiarVistaResumen() {
        if (rrccActual != null) {
            rrccPintarResumen();
        }
    }

    @FXML
    private void rrccOnCopiarResumen() {
        if (rrccActual == null) {
            return;
        }
        String rrccResumen = rrccActual.rrccResumen().stream().filter(RRCCOracionPuntuada::rrccSeleccionada)
                .map(RRCCOracionPuntuada::rrccTexto).collect(Collectors.joining("\n"));
        rrccCopiar(rrccResumen);
        rrccNotificador.rrccExito("Resumen copiado al portapapeles.");
    }

    @FXML
    private void rrccOnCopiarDato() {
        RRCCDato rrccD = rrccTablaDatos.getSelectionModel().getSelectedItem();
        if (rrccD != null) {
            rrccCopiar(rrccD.rrccValor());
            rrccNotificador.rrccExito("Copiado: " + rrccD.rrccValor());
        }
    }

    private void rrccCopiar(String rrccContenidoCopia) {
        ClipboardContent rrccContenidoPortapapeles = new ClipboardContent();
        rrccContenidoPortapapeles.putString(rrccContenidoCopia);
        Clipboard.getSystemClipboard().setContent(rrccContenidoPortapapeles);
    }

    @FXML
    private void rrccOnExportar() {
        if (rrccActual == null) {
            return;
        }
        FileChooser rrccSelectorArchivo = new FileChooser();
        rrccSelectorArchivo.setTitle("Exportar informe de análisis");
        String rrccBase = rrccActual.rrccDocumento().getRrccNombre().replaceAll("\\.[A-Za-z0-9]+$", "").replaceAll("[^\\p{L}\\p{N} _-]", "");
        rrccSelectorArchivo.setInitialFileName("RRCC_analisis_" + rrccBase + ".docx");
        rrccSelectorArchivo.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento de Word", "*.docx"));
        File rrccArchivo = rrccSelectorArchivo.showSaveDialog(rrccCmbDocumento.getScene().getWindow());
        if (rrccArchivo == null) {
            return;
        }
        RRCCPaquete rrccP = rrccActual;
        RRCCTareas.rrccEjecutar(() -> {
            try {
                rrccReporte.rrccExportarAnalisis(rrccP.rrccDocumento(), rrccP.rrccEstadisticas(),
                        rrccP.rrccTerminos().subList(0, Math.min(20, rrccP.rrccTerminos().size())), rrccP.rrccResumen(),
                        rrccP.rrccDatos(), rrccArchivo);
                return rrccArchivo;
            } catch (Exception rrccError) {
                throw new IllegalStateException(rrccError.getMessage(), rrccError);
            }
        }, rrccF -> rrccNotificador.rrccExito("Informe guardado: " + rrccF.getName()),
                rrccMensaje -> rrccNotificador.rrccError("No se pudo exportar: " + rrccMensaje));
    }
}
