package com.ejemplo.rrccmindscan.controller;

import atlantafx.base.theme.Styles;
import com.ejemplo.rrccmindscan.config.RRCCAnalisisPropiedades;
import com.ejemplo.rrccmindscan.exception.RRCCNegocioException;
import com.ejemplo.rrccmindscan.model.RRCCCategoria;
import com.ejemplo.rrccmindscan.model.RRCCDocumento;
import com.ejemplo.rrccmindscan.model.RRCCTipoDocumento;
import com.ejemplo.rrccmindscan.service.RRCCBuscadorService;
import com.ejemplo.rrccmindscan.service.RRCCDocumentoService;
import com.ejemplo.rrccmindscan.service.RRCCTextoService;
import com.ejemplo.rrccmindscan.ui.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RRCCBibliotecaController implements RRCCVista {

    public static final String RRCC_TODAS = "Todas las categorías";
    public static final String RRCC_TODOS_TIPOS = "Todos los tipos";
    public static final int RRCC_LARGO_VISTA = 4000;

    private final RRCCDocumentoService rrccDocumentos;
    private final RRCCBuscadorService rrccBuscador;
    private final RRCCTextoService rrccTexto;
    private final RRCCImportador rrccImportador;
    private final RRCCNotificador rrccNotificador;
    private final RRCCContexto rrccContexto;
    private final RRCCMainController rrccMain;
    private final RRCCAnalisisPropiedades rrccPropiedades;
    private final RRCCLector rrccLector;

    @FXML private Label rrccLblSubtitulo;
    @FXML private TextField rrccTxtFiltro;
    @FXML private ComboBox<String> rrccCmbCategoria;
    @FXML private ComboBox<String> rrccCmbTipo;
    @FXML private TableView<RRCCDocumento> rrccTabla;
    @FXML private CheckBox rrccChkFavoritos;
    @FXML private TableColumn<RRCCDocumento, Label> rrccColFavorito;
    @FXML private VBox rrccCajaNotas;
    @FXML private Label rrccLblNotas;
    @FXML private Button rrccBtnLeer;
    @FXML private TableColumn<RRCCDocumento, String> rrccColNombre;
    @FXML private TableColumn<RRCCDocumento, Label> rrccColTipo;
    @FXML private TableColumn<RRCCDocumento, Label> rrccColCategoria;
    @FXML private TableColumn<RRCCDocumento, Integer> rrccColPaginas;
    @FXML private TableColumn<RRCCDocumento, Integer> rrccColPalabras;
    @FXML private TableColumn<RRCCDocumento, Label> rrccColLegibilidad;
    @FXML private TableColumn<RRCCDocumento, String> rrccColFecha;
    @FXML private VBox rrccDetalle;
    @FXML private Label rrccLblNombre;
    @FXML private FlowPane rrccCajaChips;
    @FXML private GridPane rrccGridDatos;
    @FXML private TextArea rrccTxtVista;
    @FXML private Button rrccBtnAnalizar;
    @FXML private Button rrccBtnRevisar;
    @FXML private Button rrccBtnComparar;
    @FXML private Button rrccBtnEditar;
    @FXML private Button rrccBtnEliminar;

    private final ObservableList<RRCCDocumento> rrccDatos = FXCollections.observableArrayList();
    private FilteredList<RRCCDocumento> rrccFiltrados;

    @Override
    public void rrccIniciar() {
        rrccColFavorito.setCellValueFactory(rrccC -> new SimpleObjectProperty<>(rrccEstrella(rrccC.getValue())));
        rrccColFavorito.setSortable(false);
        rrccChkFavoritos.selectedProperty().addListener((rrccO, rrccA, rrccN) -> rrccAplicarFiltro());
        rrccColNombre.setCellValueFactory(rrccC -> new SimpleStringProperty(rrccC.getValue().getRrccNombre()));
        rrccColTipo.setCellValueFactory(rrccC -> new SimpleObjectProperty<>(RRCCFormatos.rrccChipTipo(rrccC.getValue().getRrccTipo())));
        rrccColCategoria.setCellValueFactory(rrccC -> new SimpleObjectProperty<>(RRCCFormatos.rrccChipCategoria(rrccC.getValue().getRrccCategoria())));
        rrccColPaginas.setCellValueFactory(rrccC -> new SimpleObjectProperty<>(rrccC.getValue().getRrccPaginas()));
        rrccColPalabras.setCellValueFactory(rrccC -> new SimpleObjectProperty<>(rrccC.getValue().getRrccPalabras()));
        rrccColLegibilidad.setCellValueFactory(rrccC -> new SimpleObjectProperty<>(RRCCFormatos.rrccChip(
                String.valueOf(Math.round(rrccC.getValue().getRrccLegibilidad())),
                RRCCFormatos.rrccClaseLegibilidad(rrccC.getValue().getRrccLegibilidad()))));
        rrccColFecha.setCellValueFactory(rrccC -> new SimpleStringProperty(rrccC.getValue().getRrccFechaImportacion().format(RRCCFormatos.RRCC_FECHA_HORA)));
        rrccColTipo.setSortable(false);
        rrccColCategoria.setSortable(false);
        rrccColLegibilidad.setSortable(false);

        rrccFiltrados = new FilteredList<>(rrccDatos, rrccD -> true);
        SortedList<RRCCDocumento> rrccOrdenados = new SortedList<>(rrccFiltrados);
        rrccOrdenados.comparatorProperty().bind(rrccTabla.comparatorProperty());
        rrccTabla.setItems(rrccOrdenados);
        rrccTabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        rrccTabla.setPlaceholder(RRCCGraficos.rrccVacio("La biblioteca está vacía",
                "Importa archivos PDF, Word o TXT, o arrástralos aquí."));
        rrccTabla.getSelectionModel().selectedItemProperty().addListener((rrccO, rrccA, rrccNuevo) -> rrccMostrarDetalle(rrccNuevo));
        rrccTabla.setOnMouseClicked(rrccE -> {
            if (rrccE.getClickCount() == 2 && rrccTabla.getSelectionModel().getSelectedItem() != null) {
                rrccOnAnalizar();
            }
        });

        rrccCmbCategoria.getItems().add(RRCC_TODAS);
        Arrays.stream(RRCCCategoria.values()).forEach(rrccC -> rrccCmbCategoria.getItems().add(rrccC.rrccEtiqueta()));
        rrccCmbCategoria.getSelectionModel().selectFirst();
        rrccCmbTipo.getItems().add(RRCC_TODOS_TIPOS);
        Arrays.stream(RRCCTipoDocumento.values()).forEach(rrccT -> rrccCmbTipo.getItems().add(rrccT.rrccEtiqueta()));
        rrccCmbTipo.getSelectionModel().selectFirst();
        rrccTxtFiltro.textProperty().addListener((rrccO, rrccA, rrccN) -> rrccAplicarFiltro());
        rrccCmbCategoria.valueProperty().addListener((rrccO, rrccA, rrccN) -> rrccAplicarFiltro());
        rrccCmbTipo.valueProperty().addListener((rrccO, rrccA, rrccN) -> rrccAplicarFiltro());

        rrccImportador.rrccHabilitarArrastre(rrccTabla, rrccR -> rrccDespuesDeImportar(rrccR.rrccImportados()));
        rrccMostrarDetalle(null);
    }

    @Override
    public void rrccRefrescar() {
        RRCCDocumento rrccPrevio = rrccTabla.getSelectionModel().getSelectedItem();
        rrccDatos.setAll(rrccDocumentos.rrccListar());
        long rrccPalabras = rrccDatos.stream().mapToLong(RRCCDocumento::getRrccPalabras).sum();
        rrccLblSubtitulo.setText(rrccDatos.size() + " documentos  |  " + RRCCFormatos.rrccNumero(rrccPalabras)
                + " palabras indexadas para búsqueda, comparación y revisión");
        if (rrccPrevio != null) {
            rrccDatos.stream().filter(rrccD -> rrccD.getRrccId().equals(rrccPrevio.getRrccId())).findFirst()
                    .ifPresent(rrccD -> rrccTabla.getSelectionModel().select(rrccD));
        }
        if (rrccTabla.getSelectionModel().getSelectedItem() == null && !rrccTabla.getItems().isEmpty()) {
            rrccTabla.getSelectionModel().selectFirst();
        }
    }

    private void rrccAplicarFiltro() {
        String rrccFiltro = rrccTexto.rrccNormalizar(rrccTxtFiltro.getText()).strip();
        RRCCCategoria rrccCategoria = RRCCCategoria.rrccDesdeEtiqueta(rrccCmbCategoria.getValue());
        String rrccTipo = rrccCmbTipo.getValue();
        rrccFiltrados.setPredicate(rrccD -> (rrccFiltro.isEmpty() || rrccTexto.rrccNormalizar(rrccD.getRrccNombre()).contains(rrccFiltro))
                && (rrccCategoria == null || rrccD.getRrccCategoria() == rrccCategoria)
                && (rrccTipo == null || RRCC_TODOS_TIPOS.equals(rrccTipo) || rrccD.getRrccTipo().rrccEtiqueta().equals(rrccTipo))
                && (!rrccChkFavoritos.isSelected() || rrccD.rrccEsFavorito()));
    }

    private Label rrccEstrella(RRCCDocumento rrccDoc) {
        Label rrccEstrella = new Label(rrccDoc.rrccEsFavorito() ? "★" : "☆");
        rrccEstrella.getStyleClass().addAll("rrcc-estrella", rrccDoc.rrccEsFavorito() ? "rrcc-estrella-activa" : "rrcc-estrella-vacia");
        Tooltip.install(rrccEstrella, new Tooltip(rrccDoc.rrccEsFavorito() ? "Quitar de favoritos" : "Marcar como favorito"));
        rrccEstrella.setOnMouseClicked(rrccE -> {
            rrccE.consume();
            rrccDocumentos.rrccAlternarFavorito(rrccDoc.getRrccId());
            rrccRefrescar();
        });
        return rrccEstrella;
    }

    private void rrccMostrarDetalle(RRCCDocumento rrccDoc) {
        boolean rrccHay = rrccDoc != null;
        boolean rrccConNotas = rrccHay && rrccDoc.getRrccNotas() != null && !rrccDoc.getRrccNotas().isBlank();
        rrccCajaNotas.setVisible(rrccConNotas);
        rrccCajaNotas.setManaged(rrccConNotas);
        rrccLblNotas.setText(rrccConNotas ? rrccDoc.getRrccNotas() : "");
        for (Button rrccB : List.of(rrccBtnAnalizar, rrccBtnRevisar, rrccBtnComparar, rrccBtnEditar, rrccBtnEliminar, rrccBtnLeer)) {
            rrccB.setDisable(!rrccHay);
        }
        rrccCajaChips.getChildren().clear();
        rrccGridDatos.getChildren().clear();
        if (!rrccHay) {
            rrccLblNombre.setText("Selecciona un documento");
            rrccTxtVista.setText("");
            return;
        }
        rrccLblNombre.setText((rrccDoc.rrccEsFavorito() ? "★ " : "") + rrccDoc.getRrccNombre());
        rrccCajaChips.getChildren().addAll(RRCCFormatos.rrccChipTipo(rrccDoc.getRrccTipo()),
                RRCCFormatos.rrccChipCategoria(rrccDoc.getRrccCategoria()),
                RRCCFormatos.rrccChip("Legibilidad " + Math.round(rrccDoc.getRrccLegibilidad()),
                        RRCCFormatos.rrccClaseLegibilidad(rrccDoc.getRrccLegibilidad())));
        String[][] rrccFilas = {
                {"Páginas", String.valueOf(rrccDoc.getRrccPaginas())},
                {"Palabras", RRCCFormatos.rrccNumero(rrccDoc.getRrccPalabras())},
                {"Oraciones", RRCCFormatos.rrccNumero(rrccDoc.getRrccOraciones())},
                {"Lectura", RRCCFormatos.rrccMinutos(rrccDoc.getRrccPalabras() / (double) rrccPropiedades.rrccPalabrasPorMinuto())},
                {"Tamaño", rrccDoc.rrccTamanoLegible()},
                {"Nivel", rrccTexto.rrccNivelLectura(rrccDoc.getRrccLegibilidad())},
                {"Ubicación", rrccDoc.getRrccRuta() == null ? "Documento de ejemplo" : rrccDoc.getRrccRuta()},
                {"Huella SHA-256", rrccDoc.getRrccHash().substring(0, 16) + "..."}};
        for (int rrccI = 0; rrccI < rrccFilas.length; rrccI++) {
            Label rrccClave = new Label(rrccFilas[rrccI][0]);
            rrccClave.getStyleClass().add("rrcc-dato-clave");
            Label rrccValor = new Label(rrccFilas[rrccI][1]);
            rrccValor.getStyleClass().add("rrcc-dato-valor");
            rrccValor.setWrapText(true);
            rrccGridDatos.add(rrccClave, 0, rrccI);
            rrccGridDatos.add(rrccValor, 1, rrccI);
        }
        String rrccContenido = rrccDoc.getRrccContenido();
        rrccTxtVista.setText(rrccContenido.length() > RRCC_LARGO_VISTA
                ? rrccContenido.substring(0, RRCC_LARGO_VISTA) + "\n\n[... continúa, usa Analizar para ver todo]" : rrccContenido);
        rrccTxtVista.positionCaret(0);
    }

    private void rrccDespuesDeImportar(List<RRCCDocumento> rrccNuevos) {
        rrccMain.rrccActualizarPie();
        rrccRefrescar();
        if (!rrccNuevos.isEmpty()) {
            Long rrccId = rrccNuevos.get(0).getRrccId();
            rrccDatos.stream().filter(rrccD -> rrccD.getRrccId().equals(rrccId)).findFirst().ifPresent(rrccD -> {
                rrccTabla.getSelectionModel().select(rrccD);
                rrccTabla.scrollTo(rrccD);
            });
        }
    }

    private RRCCDocumento rrccSeleccionado() {
        return rrccTabla.getSelectionModel().getSelectedItem();
    }

    @FXML
    private void rrccOnImportarArchivos() {
        rrccImportador.rrccElegirArchivos(rrccTabla.getScene().getWindow(), rrccR -> rrccDespuesDeImportar(rrccR.rrccImportados()));
    }

    @FXML
    private void rrccOnImportarCarpeta() {
        rrccImportador.rrccElegirCarpeta(rrccTabla.getScene().getWindow(), rrccR -> rrccDespuesDeImportar(rrccR.rrccImportados()));
    }

    @FXML
    private void rrccOnAnalizar() {
        if (rrccSeleccionado() != null) {
            rrccContexto.rrccActivar(rrccSeleccionado().getRrccId());
            rrccMain.rrccIrA(RRCCMainController.RRCC_VISTA_ANALISIS);
        }
    }

    @FXML
    private void rrccOnRevisar() {
        if (rrccSeleccionado() != null) {
            rrccContexto.rrccActivar(rrccSeleccionado().getRrccId());
            rrccMain.rrccIrA(RRCCMainController.RRCC_VISTA_REVISION);
        }
    }

    @FXML
    private void rrccOnLeer() {
        if (rrccSeleccionado() != null) {
            rrccLector.rrccAbrir(rrccSeleccionado(), 0, 0, "Lectura completa del texto extraído");
        }
    }

    @FXML
    private void rrccOnComparar() {
        if (rrccSeleccionado() != null) {
            rrccContexto.rrccPrepararComparacion(rrccSeleccionado().getRrccId(), null);
            rrccMain.rrccIrA(RRCCMainController.RRCC_VISTA_COMPARADOR);
        }
    }

    @FXML
    private void rrccOnEditar() {
        RRCCDocumento rrccDoc = rrccSeleccionado();
        if (rrccDoc == null) {
            return;
        }
        Label rrccTitulo = new Label("Editar documento");
        rrccTitulo.getStyleClass().add("rrcc-drawer-titulo");
        Label rrccSub = new Label("La categoría se asignó automáticamente; puedes corregirla, agregar notas o marcarlo como favorito.");
        rrccSub.getStyleClass().add("rrcc-drawer-subtitulo");
        VBox rrccCabecera = new VBox(4, rrccTitulo, rrccSub);
        rrccCabecera.getStyleClass().add("rrcc-drawer-header");

        TextField rrccNombre = new TextField(rrccDoc.getRrccNombre());
        ComboBox<String> rrccCategoria = new ComboBox<>();
        Arrays.stream(RRCCCategoria.values()).forEach(rrccC -> rrccCategoria.getItems().add(rrccC.rrccEtiqueta()));
        rrccCategoria.setValue(rrccDoc.getRrccCategoria().rrccEtiqueta());
        rrccCategoria.setMaxWidth(Double.MAX_VALUE);
        TextArea rrccNotas = new TextArea(rrccDoc.getRrccNotas() == null ? "" : rrccDoc.getRrccNotas());
        rrccNotas.setWrapText(true);
        rrccNotas.setPrefRowCount(8);
        rrccNotas.setPromptText("Ej.: revisar la cláusula de penalidades antes de la firma");
        CheckBox rrccFavorito = new CheckBox("Marcar como favorito ★");
        rrccFavorito.setSelected(rrccDoc.rrccEsFavorito());
        VBox rrccCuerpo = new VBox(14, rrccCampo("Nombre", rrccNombre), rrccCampo("Categoría", rrccCategoria),
                rrccCampo("Notas personales", rrccNotas), rrccFavorito);
        rrccCuerpo.getStyleClass().add("rrcc-drawer-body");
        VBox.setVgrow(rrccCuerpo, Priority.ALWAYS);

        Button rrccCancelar = new Button("Cancelar");
        rrccCancelar.getStyleClass().add(Styles.BUTTON_OUTLINED);
        rrccCancelar.setOnAction(rrccE -> rrccNotificador.rrccCerrarPanel());
        Button rrccGuardar = new Button("Guardar cambios");
        rrccGuardar.getStyleClass().add(Styles.ACCENT);
        rrccGuardar.setDefaultButton(true);
        rrccGuardar.setOnAction(rrccE -> {
            try {
                rrccDocumentos.rrccRenombrar(rrccDoc.getRrccId(), rrccNombre.getText());
                rrccDocumentos.rrccCambiarCategoria(rrccDoc.getRrccId(), RRCCCategoria.rrccDesdeEtiqueta(rrccCategoria.getValue()));
                rrccDocumentos.rrccGuardarNotas(rrccDoc.getRrccId(), rrccNotas.getText());
                if (rrccFavorito.isSelected() != rrccDoc.rrccEsFavorito()) {
                    rrccDocumentos.rrccAlternarFavorito(rrccDoc.getRrccId());
                }
                rrccNotificador.rrccCerrarPanel();
                rrccNotificador.rrccExito("Documento actualizado.");
                rrccRefrescar();
            } catch (RRCCNegocioException rrccError) {
                rrccNotificador.rrccError(rrccError.getMessage());
            }
        });
        HBox rrccPie = new HBox(10, rrccCancelar, rrccGuardar);
        rrccPie.getStyleClass().add("rrcc-drawer-footer");
        rrccPie.setAlignment(Pos.CENTER_RIGHT);

        VBox rrccDrawer = new VBox(rrccCabecera, rrccCuerpo, rrccPie);
        rrccDrawer.getStyleClass().add("rrcc-drawer");
        rrccDrawer.setPrefWidth(420);
        rrccDrawer.setMaxWidth(420);
        rrccDrawer.setMaxHeight(Double.MAX_VALUE);
        rrccNotificador.rrccAbrirPanel(rrccDrawer);
        rrccNombre.requestFocus();
    }

    private VBox rrccCampo(String rrccEtiqueta, Control rrccControl) {
        Label rrccL = new Label(rrccEtiqueta);
        rrccL.getStyleClass().add("rrcc-campo-label");
        VBox rrccCaja = new VBox(6, rrccL, rrccControl);
        rrccCaja.getStyleClass().add("rrcc-campo");
        return rrccCaja;
    }

    @FXML
    private void rrccOnEliminar() {
        RRCCDocumento rrccDoc = rrccSeleccionado();
        if (rrccDoc == null) {
            return;
        }
        rrccNotificador.rrccConfirmar("Eliminar documento", "Se quitará \"" + rrccDoc.getRrccNombre()
                        + "\" de la biblioteca junto con sus comparaciones y revisiones. El archivo original no se borra.", "Eliminar",
                () -> {
                    try {
                        rrccDocumentos.rrccEliminar(rrccDoc.getRrccId());
                        rrccBuscador.rrccLimpiarCache();
                        rrccNotificador.rrccExito("Documento eliminado de la biblioteca.");
                        rrccMain.rrccActualizarPie();
                        rrccTabla.getSelectionModel().clearSelection();
                        rrccRefrescar();
                    } catch (RRCCNegocioException rrccError) {
                        rrccNotificador.rrccError(rrccError.getMessage());
                    }
                });
    }
}
