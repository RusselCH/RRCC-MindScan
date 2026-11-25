package com.ejemplo.rrccmindscan.controller;

import com.ejemplo.rrccmindscan.model.RRCCDocumento;
import com.ejemplo.rrccmindscan.model.RRCCTipoCambio;
import com.ejemplo.rrccmindscan.service.RRCCComparadorService;
import com.ejemplo.rrccmindscan.service.RRCCComparadorService.RRCCBloqueDiff;
import com.ejemplo.rrccmindscan.service.RRCCComparadorService.RRCCCompartido;
import com.ejemplo.rrccmindscan.service.RRCCComparadorService.RRCCFragmentoDiff;
import com.ejemplo.rrccmindscan.service.RRCCComparadorService.RRCCPasaje;
import com.ejemplo.rrccmindscan.service.RRCCComparadorService.RRCCResultadoComparacion;
import com.ejemplo.rrccmindscan.service.RRCCDocumentoService;
import com.ejemplo.rrccmindscan.service.RRCCHistorialService;
import com.ejemplo.rrccmindscan.service.RRCCReporteService;
import com.ejemplo.rrccmindscan.ui.*;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RRCCComparadorController implements RRCCVista {

    public static final int RRCC_MAX_FILAS = 600;

    private record RRCCParResultados(RRCCResultadoComparacion rrccComparacion, RRCCCompartido rrccCompartido) {
    }

    private final RRCCDocumentoService rrccDocumentos;
    private final RRCCComparadorService rrccComparador;
    private final RRCCHistorialService rrccHistorial;
    private final RRCCReporteService rrccReporte;
    private final RRCCNotificador rrccNotificador;
    private final RRCCContexto rrccContexto;

    @FXML private ComboBox<String> rrccCmbA;
    @FXML private ComboBox<String> rrccCmbB;
    @FXML private VBox rrccCajaVacia;
    @FXML private VBox rrccContenido;
    @FXML private StackPane rrccCajaAnillo;
    @FXML private Label rrccLblVeredicto;
    @FXML private Label rrccLblMetricas;
    @FXML private Label rrccLblIguales;
    @FXML private Label rrccLblAgregadas;
    @FXML private Label rrccLblEliminadas;
    @FXML private Label rrccLblModificadas;
    @FXML private PieChart rrccGraficoComposicion;
    @FXML private FlowPane rrccCajaNuevos;
    @FXML private FlowPane rrccCajaRetirados;
    @FXML private CheckBox rrccChkSoloCambios;
    @FXML private ToggleButton rrccTglLadoALado;
    @FXML private VBox rrccCajaCompartido;
    @FXML private VBox rrccCajaDiff;

    private RRCCSelectorDocumento rrccSelectorA;
    private RRCCSelectorDocumento rrccSelectorB;
    private RRCCDocumento rrccDocA;
    private RRCCDocumento rrccDocB;
    private RRCCResultadoComparacion rrccResultado;
    private RRCCCompartido rrccCompartido;
    private boolean rrccGuardado;

    @Override
    public void rrccIniciar() {
        rrccSelectorA = new RRCCSelectorDocumento(rrccCmbA);
        rrccSelectorB = new RRCCSelectorDocumento(rrccCmbB);
        rrccChkSoloCambios.selectedProperty().addListener((rrccO, rrccA, rrccN) -> rrccPintarDiff());
        rrccTglLadoALado.selectedProperty().addListener((rrccO, rrccA, rrccN) -> rrccPintarDiff());
        rrccGraficoComposicion.setAnimated(false);
        rrccMostrar(false);
    }

    @Override
    public void rrccRefrescar() {
        List<RRCCDocumento> rrccLista = rrccDocumentos.rrccListar();
        rrccSelectorA.rrccCargar(rrccLista);
        rrccSelectorB.rrccCargar(rrccLista);
        Long[] rrccPar = rrccContexto.rrccTomarComparacion();
        if (rrccPar[0] != null) {
            rrccSelectorA.rrccSeleccionar(rrccPar[0]);
            if (rrccPar[1] != null) {
                rrccSelectorB.rrccSeleccionar(rrccPar[1]);
                rrccOnComparar();
            } else {
                rrccCmbB.getSelectionModel().clearSelection();
                rrccCmbB.requestFocus();
                rrccNotificador.rrccInfo("Elige la versión B para comparar.");
            }
        }
        if (rrccResultado == null) {
            rrccCajaVacia.getChildren().setAll(RRCCGraficos.rrccVacio("Elige dos documentos",
                    "Por ejemplo, el borrador y la versión final de un reglamento, contrato o informe. MindScan te muestra exactamente qué cambió."));
        }
    }

    private void rrccMostrar(boolean rrccHayResultado) {
        rrccContenido.setVisible(rrccHayResultado);
        rrccContenido.setManaged(rrccHayResultado);
        rrccCajaVacia.setVisible(!rrccHayResultado);
        rrccCajaVacia.setManaged(!rrccHayResultado);
    }

    @FXML
    private void rrccOnIntercambiar() {
        RRCCDocumento rrccA = rrccSelectorA.rrccSeleccionado();
        RRCCDocumento rrccB = rrccSelectorB.rrccSeleccionado();
        if (rrccB != null) {
            rrccSelectorA.rrccSeleccionar(rrccB.getRrccId());
        }
        if (rrccA != null) {
            rrccSelectorB.rrccSeleccionar(rrccA.getRrccId());
        }
        if (rrccA != null && rrccB != null && rrccResultado != null) {
            rrccOnComparar();
        }
    }

    @FXML
    private void rrccOnComparar() {
        RRCCDocumento rrccA = rrccSelectorA.rrccSeleccionado();
        RRCCDocumento rrccB = rrccSelectorB.rrccSeleccionado();
        if (rrccA == null || rrccB == null) {
            rrccNotificador.rrccAdvertencia("Elige los dos documentos que quieres comparar.");
            return;
        }
        if (rrccA.getRrccId().equals(rrccB.getRrccId())) {
            rrccNotificador.rrccAdvertencia("Elige dos documentos distintos.");
            return;
        }
        RRCCTareas.rrccEjecutar(() -> new RRCCParResultados(
                        rrccComparador.rrccComparar(rrccA.getRrccContenido(), rrccB.getRrccContenido()),
                        rrccComparador.rrccTextoCompartido(rrccA.getRrccContenido(), rrccB.getRrccContenido())),
                rrccR -> {
                    rrccDocA = rrccA;
                    rrccDocB = rrccB;
                    rrccResultado = rrccR.rrccComparacion();
                    rrccCompartido = rrccR.rrccCompartido();
                    rrccGuardado = false;
                    rrccPintar();
                }, rrccMensaje -> rrccNotificador.rrccError("No se pudo comparar: " + rrccMensaje));
    }

    private void rrccPintar() {
        RRCCResultadoComparacion rrccR = rrccResultado;
        double rrccSim = rrccR.rrccSimilitudGlobal();
        String rrccClase = rrccSim >= 0.8 ? "rrcc-bueno" : rrccSim >= 0.5 ? "rrcc-medio" : "rrcc-malo";
        rrccCajaAnillo.getChildren().setAll(RRCCGraficos.rrccAnillo(rrccSim, Math.round(rrccSim * 100) + "%", "similitud", rrccClase, 40));
        rrccLblVeredicto.setText(rrccR.rrccCambios() == 0 ? "Los documentos tienen el mismo contenido"
                : rrccSim >= 0.8 ? "Son versiones del mismo documento con cambios puntuales"
                : rrccSim >= 0.5 ? "Comparten una base común con cambios importantes"
                : "Son documentos mayormente distintos");
        rrccLblMetricas.setText("Vocabulario (coseno): " + RRCCFormatos.rrccPorcentaje(rrccR.rrccSimilitudCoseno())
                + "   |   Frases (Jaccard 3-gramas): " + RRCCFormatos.rrccPorcentaje(rrccR.rrccSimilitudTejas()));
        rrccLblIguales.setText(String.valueOf(rrccR.rrccIguales()));
        rrccLblAgregadas.setText(String.valueOf(rrccR.rrccAgregadas()));
        rrccLblEliminadas.setText(String.valueOf(rrccR.rrccEliminadas()));
        rrccLblModificadas.setText(String.valueOf(rrccR.rrccModificadas()));

        rrccGraficoComposicion.getData().clear();
        rrccRebanada("Sin cambios", rrccR.rrccIguales(), "rrcc-pie-igual");
        rrccRebanada("Agregadas", rrccR.rrccAgregadas(), "rrcc-pie-agregado");
        rrccRebanada("Eliminadas", rrccR.rrccEliminadas(), "rrcc-pie-eliminado");
        rrccRebanada("Modificadas", rrccR.rrccModificadas(), "rrcc-pie-modificado");

        rrccCajaNuevos.getChildren().clear();
        rrccR.rrccTerminosNuevos().forEach(rrccT -> rrccCajaNuevos.getChildren().add(RRCCFormatos.rrccChip("+ " + rrccT, "rrcc-chip-agregado")));
        if (rrccR.rrccTerminosNuevos().isEmpty()) {
            rrccCajaNuevos.getChildren().add(new Label("Ninguno"));
        }
        rrccCajaRetirados.getChildren().clear();
        rrccR.rrccTerminosRetirados().forEach(rrccT -> rrccCajaRetirados.getChildren().add(RRCCFormatos.rrccChip("- " + rrccT, "rrcc-chip-eliminado")));
        if (rrccR.rrccTerminosRetirados().isEmpty()) {
            rrccCajaRetirados.getChildren().add(new Label("Ninguno"));
        }
        rrccPintarCompartido();
        rrccPintarDiff();
        rrccMostrar(true);
    }

    private void rrccPintarCompartido() {
        rrccCajaCompartido.getChildren().clear();
        RRCCCompartido rrccC = rrccCompartido;
        HBox rrccMedidores = new HBox(24,
                rrccMedidorCompartido("De A aparece en B", rrccC.rrccPorcentajeA()),
                rrccMedidorCompartido("De B aparece en A", rrccC.rrccPorcentajeB()));
        rrccMedidores.setAlignment(Pos.CENTER_LEFT);
        double rrccMayor = Math.max(rrccC.rrccPorcentajeA(), rrccC.rrccPorcentajeB());
        Label rrccLectura = new Label(rrccMayor >= 0.5 ? "Gran parte del texto es idéntico: uno parece derivado del otro."
                : rrccMayor >= 0.15 ? "Comparten pasajes literales: revisa si hay texto copiado o reutilizado."
                : rrccMayor > 0 ? "Solo coinciden algunas frases sueltas."
                : "No comparten ninguna secuencia literal de 6 palabras.");
        rrccLectura.getStyleClass().add("rrcc-texto-suave");
        rrccLectura.setWrapText(true);
        rrccCajaCompartido.getChildren().addAll(rrccMedidores, rrccLectura);
        for (RRCCPasaje rrccP : rrccC.rrccPasajes()) {
            Label rrccTexto = new Label("«" + rrccP.rrccTexto() + "»");
            rrccTexto.setWrapText(true);
            rrccTexto.getStyleClass().add("rrcc-pasaje-texto");
            HBox.setHgrow(rrccTexto, Priority.ALWAYS);
            rrccTexto.setMaxWidth(Double.MAX_VALUE);
            Label rrccLargo = RRCCFormatos.rrccChip(rrccP.rrccPalabras() + " palabras", "rrcc-chip-suave");
            rrccLargo.setMinWidth(Region.USE_PREF_SIZE);
            HBox rrccFila = new HBox(10, rrccLargo, rrccTexto);
            rrccFila.setAlignment(Pos.TOP_LEFT);
            rrccFila.getStyleClass().add("rrcc-pasaje");
            rrccCajaCompartido.getChildren().add(rrccFila);
        }
    }

    private VBox rrccMedidorCompartido(String rrccTitulo, double rrccFraccion) {
        Label rrccValor = new Label(RRCCFormatos.rrccPorcentaje(rrccFraccion));
        rrccValor.getStyleClass().add("rrcc-kpi-valor");
        Label rrccNombre = new Label(rrccTitulo);
        rrccNombre.getStyleClass().add("rrcc-texto-suave");
        ProgressBar rrccBarra = new ProgressBar(rrccFraccion);
        rrccBarra.setPrefWidth(220);
        return new VBox(4, rrccValor, rrccNombre, rrccBarra);
    }

    private void rrccRebanada(String rrccNombre, int rrccValor, String rrccClase) {
        if (rrccValor <= 0) {
            return;
        }
        PieChart.Data rrccD = new PieChart.Data(rrccNombre + " (" + rrccValor + ")", rrccValor);
        rrccGraficoComposicion.getData().add(rrccD);
        Runnable rrccPintarRebanada = () -> {
            if (rrccD.getNode() != null && !rrccD.getNode().getStyleClass().contains(rrccClase)) {
                rrccD.getNode().getStyleClass().add(rrccClase);
            }
        };
        rrccPintarRebanada.run();
        javafx.application.Platform.runLater(rrccPintarRebanada);
    }

    private void rrccPintarDiff() {
        rrccCajaDiff.getChildren().clear();
        if (rrccResultado == null) {
            return;
        }
        boolean rrccSoloCambios = rrccChkSoloCambios.isSelected();
        if (rrccTglLadoALado.isSelected()) {
            rrccCajaDiff.getChildren().add(rrccCabeceraLadoALado());
        }
        int rrccFilas = 0;
        int rrccOmitidas = 0;
        for (RRCCBloqueDiff rrccB : rrccResultado.rrccBloques()) {
            if (rrccSoloCambios && rrccB.rrccTipo() == RRCCTipoCambio.RRCC_IGUAL) {
                continue;
            }
            if (rrccFilas >= RRCC_MAX_FILAS) {
                rrccOmitidas++;
                continue;
            }
            rrccCajaDiff.getChildren().add(rrccFila(rrccB));
            rrccFilas++;
        }
        if (rrccFilas == 0) {
            rrccCajaDiff.getChildren().add(RRCCGraficos.rrccVacio("Sin diferencias", "Ambos documentos tienen las mismas oraciones."));
        }
        if (rrccOmitidas > 0) {
            Label rrccAviso = new Label("Se muestran las primeras " + RRCC_MAX_FILAS + " filas. Exporta a Word para ver las "
                    + rrccOmitidas + " restantes.");
            rrccAviso.getStyleClass().add("rrcc-aviso");
            rrccCajaDiff.getChildren().add(rrccAviso);
        }
    }

    private HBox rrccFila(RRCCBloqueDiff rrccB) {
        return rrccTglLadoALado.isSelected() ? rrccFilaLadoALado(rrccB) : rrccFilaUnificada(rrccB);
    }

    private HBox rrccCabeceraLadoALado() {
        Label rrccA = new Label("A · " + rrccDocA.getRrccNombre());
        Label rrccB = new Label("B · " + rrccDocB.getRrccNombre());
        for (Label rrccL : List.of(rrccA, rrccB)) {
            rrccL.getStyleClass().add("rrcc-diff-cabecera");
            rrccL.setMaxWidth(Double.MAX_VALUE);
            rrccL.setPrefWidth(0);
            HBox.setHgrow(rrccL, Priority.ALWAYS);
        }
        return new HBox(12, rrccA, rrccB);
    }

    private HBox rrccFilaLadoALado(RRCCBloqueDiff rrccB) {
        VBox rrccIzquierda = rrccColumna(rrccB, RRCCTipoCambio.RRCC_AGREGADO);
        VBox rrccDerecha = rrccColumna(rrccB, RRCCTipoCambio.RRCC_ELIMINADO);
        rrccIzquierda.setPrefWidth(0);
        rrccDerecha.setPrefWidth(0);
        HBox.setHgrow(rrccIzquierda, Priority.ALWAYS);
        HBox.setHgrow(rrccDerecha, Priority.ALWAYS);
        HBox rrccFila = new HBox(12, rrccIzquierda, rrccDerecha);
        rrccFila.setAlignment(Pos.TOP_LEFT);
        rrccFila.getStyleClass().addAll("rrcc-diff-fila", "rrcc-fila-" + rrccB.rrccTipo().name().substring(5).toLowerCase());
        return rrccFila;
    }

    private VBox rrccColumna(RRCCBloqueDiff rrccB, RRCCTipoCambio rrccExcluido) {
        VBox rrccCaja = new VBox();
        rrccCaja.getStyleClass().add("rrcc-diff-columna");
        if (rrccB.rrccTipo() == rrccExcluido) {
            rrccCaja.getStyleClass().add("rrcc-diff-columna-vacia");
            return rrccCaja;
        }
        List<RRCCFragmentoDiff> rrccVisibles = rrccB.rrccFragmentos().stream()
                .filter(rrccF -> rrccF.rrccTipo() != rrccExcluido)
                .toList();
        rrccCaja.getChildren().add(RRCCGraficos.rrccDiff(rrccVisibles));
        return rrccCaja;
    }

    private HBox rrccFilaUnificada(RRCCBloqueDiff rrccB) {
        String rrccSimbolo = switch (rrccB.rrccTipo()) {
            case RRCC_AGREGADO -> "+";
            case RRCC_ELIMINADO -> "-";
            case RRCC_MODIFICADO -> "~";
            case RRCC_IGUAL -> "=";
        };
        Label rrccMarca = new Label(rrccSimbolo);
        rrccMarca.getStyleClass().addAll("rrcc-diff-marca", "rrcc-marca-" + rrccB.rrccTipo().name().substring(5).toLowerCase());
        rrccMarca.setMinWidth(28);
        rrccMarca.setAlignment(Pos.CENTER);
        VBox rrccTextos = new VBox(4);
        TextFlow rrccFlujo = RRCCGraficos.rrccDiff(rrccB.rrccFragmentos());
        rrccTextos.getChildren().add(rrccFlujo);
        if (rrccB.rrccTipo() == RRCCTipoCambio.RRCC_MODIFICADO) {
            Label rrccAntes = new Label("Antes: " + rrccB.rrccTextoA());
            rrccAntes.setWrapText(true);
            rrccAntes.getStyleClass().add("rrcc-texto-suave");
            rrccTextos.getChildren().add(rrccAntes);
        }
        HBox.setHgrow(rrccTextos, Priority.ALWAYS);
        HBox rrccFila = new HBox(10, rrccMarca, rrccTextos);
        rrccFila.setAlignment(Pos.TOP_LEFT);
        rrccFila.getStyleClass().addAll("rrcc-diff-fila", "rrcc-fila-" + rrccB.rrccTipo().name().substring(5).toLowerCase());
        return rrccFila;
    }

    @FXML
    private void rrccOnGuardar() {
        if (rrccResultado == null) {
            return;
        }
        if (rrccGuardado) {
            rrccNotificador.rrccInfo("Esta comparación ya está en el historial.");
            return;
        }
        rrccHistorial.rrccGuardarComparacion(rrccDocA, rrccDocB, rrccResultado);
        rrccGuardado = true;
        rrccNotificador.rrccExito("Comparación guardada en el historial.");
    }

    @FXML
    private void rrccOnExportar() {
        if (rrccResultado == null) {
            return;
        }
        FileChooser rrccSelector = new FileChooser();
        rrccSelector.setTitle("Exportar informe de comparación");
        rrccSelector.setInitialFileName("RRCC_comparacion.docx");
        rrccSelector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento de Word", "*.docx"));
        File rrccArchivo = rrccSelector.showSaveDialog(rrccCmbA.getScene().getWindow());
        if (rrccArchivo == null) {
            return;
        }
        RRCCDocumento rrccA = rrccDocA;
        RRCCDocumento rrccB = rrccDocB;
        RRCCResultadoComparacion rrccR = rrccResultado;
        boolean rrccSolo = rrccChkSoloCambios.isSelected();
        RRCCTareas.rrccEjecutar(() -> {
            try {
                rrccReporte.rrccExportarComparacion(rrccA, rrccB, rrccR, rrccSolo, rrccArchivo);
                return rrccArchivo;
            } catch (Exception rrccError) {
                throw new IllegalStateException(rrccError.getMessage(), rrccError);
            }
        }, rrccF -> rrccNotificador.rrccExito("Informe guardado: " + rrccF.getName()),
                rrccMensaje -> rrccNotificador.rrccError("No se pudo exportar: " + rrccMensaje));
    }
}
