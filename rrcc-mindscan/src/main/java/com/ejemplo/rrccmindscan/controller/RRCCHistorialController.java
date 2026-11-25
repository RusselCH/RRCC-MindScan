package com.ejemplo.rrccmindscan.controller;

import com.ejemplo.rrccmindscan.model.RRCCComparacion;
import com.ejemplo.rrccmindscan.model.RRCCRevision;
import com.ejemplo.rrccmindscan.service.RRCCHistorialService;
import com.ejemplo.rrccmindscan.ui.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class RRCCHistorialController implements RRCCVista {

    public static final int RRCC_MAX_SERIES = 6;
    public static final int RRCC_LARGO_SERIE = 28;

    private final RRCCHistorialService rrccHistorial;
    private final RRCCNotificador rrccNotificador;
    private final RRCCContexto rrccContexto;
    private final RRCCMainController rrccMain;

    @FXML private LineChart<String, Number> rrccGraficoPuntajes;
    @FXML private Label rrccLblRevisiones;
    @FXML private Label rrccLblComparaciones;
    @FXML private Button rrccBtnEliminarRevision;
    @FXML private Button rrccBtnEliminarComparacion;
    @FXML private TableView<RRCCRevision> rrccTablaRevisiones;
    @FXML private TableColumn<RRCCRevision, String> rrccColFechaRev;
    @FXML private TableColumn<RRCCRevision, String> rrccColDocumentoRev;
    @FXML private TableColumn<RRCCRevision, Label> rrccColPuntajeRev;
    @FXML private TableColumn<RRCCRevision, Integer> rrccColErroresRev;
    @FXML private TableColumn<RRCCRevision, Integer> rrccColAdvertenciasRev;
    @FXML private TableColumn<RRCCRevision, Integer> rrccColSugerenciasRev;
    @FXML private TableView<RRCCComparacion> rrccTablaComparaciones;
    @FXML private TableColumn<RRCCComparacion, String> rrccColFechaComp;
    @FXML private TableColumn<RRCCComparacion, String> rrccColDocA;
    @FXML private TableColumn<RRCCComparacion, String> rrccColDocB;
    @FXML private TableColumn<RRCCComparacion, Label> rrccColSimilitud;
    @FXML private TableColumn<RRCCComparacion, String> rrccColCambios;

    @Override
    public void rrccIniciar() {
        rrccColFechaRev.setCellValueFactory(rrccC -> new SimpleStringProperty(rrccC.getValue().getRrccFecha().format(RRCCFormatos.RRCC_FECHA_HORA)));
        rrccColDocumentoRev.setCellValueFactory(rrccC -> new SimpleStringProperty(rrccC.getValue().getRrccDocumento().getRrccNombre()));
        rrccColPuntajeRev.setCellValueFactory(rrccC -> new SimpleObjectProperty<>(RRCCFormatos.rrccChip(
                String.valueOf(rrccC.getValue().getRrccPuntaje()), RRCCFormatos.rrccClasePuntaje(rrccC.getValue().getRrccPuntaje()))));
        rrccColErroresRev.setCellValueFactory(rrccC -> new SimpleObjectProperty<>(rrccC.getValue().getRrccErrores()));
        rrccColAdvertenciasRev.setCellValueFactory(rrccC -> new SimpleObjectProperty<>(rrccC.getValue().getRrccAdvertencias()));
        rrccColSugerenciasRev.setCellValueFactory(rrccC -> new SimpleObjectProperty<>(rrccC.getValue().getRrccSugerencias()));
        rrccColPuntajeRev.setSortable(false);
        rrccTablaRevisiones.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        rrccTablaRevisiones.setPlaceholder(new Label("Aún no hay revisiones. Usa Revisión de calidad."));
        rrccBtnEliminarRevision.disableProperty().bind(rrccTablaRevisiones.getSelectionModel().selectedItemProperty().isNull());
        rrccTablaRevisiones.setOnMouseClicked(rrccE -> {
            RRCCRevision rrccR = rrccTablaRevisiones.getSelectionModel().getSelectedItem();
            if (rrccE.getClickCount() == 2 && rrccR != null) {
                rrccContexto.rrccActivar(rrccR.getRrccDocumento().getRrccId());
                rrccMain.rrccIrA(RRCCMainController.RRCC_VISTA_REVISION);
            }
        });

        rrccColFechaComp.setCellValueFactory(rrccC -> new SimpleStringProperty(rrccC.getValue().getRrccFecha().format(RRCCFormatos.RRCC_FECHA_HORA)));
        rrccColDocA.setCellValueFactory(rrccC -> new SimpleStringProperty(rrccC.getValue().getRrccDocumentoA().getRrccNombre()));
        rrccColDocB.setCellValueFactory(rrccC -> new SimpleStringProperty(rrccC.getValue().getRrccDocumentoB().getRrccNombre()));
        rrccColSimilitud.setCellValueFactory(rrccC -> {
            double rrccS = rrccC.getValue().getRrccSimilitud();
            return new SimpleObjectProperty<>(RRCCFormatos.rrccChip(RRCCFormatos.rrccPorcentaje(rrccS),
                    rrccS >= 0.8 ? "rrcc-bueno" : rrccS >= 0.5 ? "rrcc-medio" : "rrcc-malo"));
        });
        rrccColCambios.setCellValueFactory(rrccC -> new SimpleStringProperty("+" + rrccC.getValue().getRrccAgregadas()
                + "  /  -" + rrccC.getValue().getRrccEliminadas() + "  /  ~" + rrccC.getValue().getRrccModificadas()));
        rrccColSimilitud.setSortable(false);
        rrccTablaComparaciones.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        rrccTablaComparaciones.setPlaceholder(new Label("Aún no hay comparaciones guardadas."));
        rrccBtnEliminarComparacion.disableProperty().bind(rrccTablaComparaciones.getSelectionModel().selectedItemProperty().isNull());
        rrccTablaComparaciones.setOnMouseClicked(rrccE -> {
            RRCCComparacion rrccC = rrccTablaComparaciones.getSelectionModel().getSelectedItem();
            if (rrccE.getClickCount() == 2 && rrccC != null) {
                rrccContexto.rrccPrepararComparacion(rrccC.getRrccDocumentoA().getRrccId(), rrccC.getRrccDocumentoB().getRrccId());
                rrccMain.rrccIrA(RRCCMainController.RRCC_VISTA_COMPARADOR);
            }
        });
    }

    @Override
    public void rrccRefrescar() {
        List<RRCCRevision> rrccRevisiones = rrccHistorial.rrccRevisiones();
        List<RRCCComparacion> rrccComparaciones = rrccHistorial.rrccComparaciones();
        rrccTablaRevisiones.getItems().setAll(rrccRevisiones);
        rrccTablaComparaciones.getItems().setAll(rrccComparaciones);
        rrccLblRevisiones.setText(rrccRevisiones.size() == 1 ? "1 revisión registrada" : rrccRevisiones.size() + " revisiones registradas");
        rrccLblComparaciones.setText(rrccComparaciones.size() == 1 ? "1 comparación guardada" : rrccComparaciones.size() + " comparaciones guardadas");
        rrccPintarEvolucion(rrccRevisiones);
    }

    private void rrccPintarEvolucion(List<RRCCRevision> rrccRevisiones) {
        rrccGraficoPuntajes.getData().clear();
        List<RRCCRevision> rrccCronologico = new ArrayList<>(rrccRevisiones);
        Collections.reverse(rrccCronologico);
        Map<Long, XYChart.Series<String, Number>> rrccSeries = new LinkedHashMap<>();
        for (RRCCRevision rrccR : rrccCronologico) {
            Long rrccId = rrccR.getRrccDocumento().getRrccId();
            if (!rrccSeries.containsKey(rrccId) && rrccSeries.size() >= RRCC_MAX_SERIES) {
                continue;
            }
            XYChart.Series<String, Number> rrccSerie = rrccSeries.computeIfAbsent(rrccId, rrccK -> {
                XYChart.Series<String, Number> rrccNueva = new XYChart.Series<>();
                String rrccNombre = rrccR.getRrccDocumento().getRrccNombre();
                rrccNueva.setName(rrccNombre.length() > RRCC_LARGO_SERIE ? rrccNombre.substring(0, RRCC_LARGO_SERIE) + "..." : rrccNombre);
                return rrccNueva;
            });
            rrccSerie.getData().add(new XYChart.Data<>(rrccR.getRrccFecha().format(RRCCFormatos.RRCC_FECHA_HORA), rrccR.getRrccPuntaje()));
        }
        rrccGraficoPuntajes.getData().addAll(rrccSeries.values());
    }

    @FXML
    private void rrccOnEliminarRevision() {
        RRCCRevision rrccR = rrccTablaRevisiones.getSelectionModel().getSelectedItem();
        if (rrccR == null) {
            return;
        }
        rrccNotificador.rrccConfirmar("Eliminar registro", "Se eliminará la revisión de \"" + rrccR.getRrccDocumento().getRrccNombre()
                + "\" del " + rrccR.getRrccFecha().format(RRCCFormatos.RRCC_FECHA_HORA) + ".", "Eliminar", () -> {
            rrccHistorial.rrccEliminarRevision(rrccR.getRrccId());
            rrccNotificador.rrccExito("Registro eliminado.");
            rrccRefrescar();
        });
    }

    @FXML
    private void rrccOnEliminarComparacion() {
        RRCCComparacion rrccC = rrccTablaComparaciones.getSelectionModel().getSelectedItem();
        if (rrccC == null) {
            return;
        }
        rrccNotificador.rrccConfirmar("Eliminar registro", "Se eliminará la comparación entre \"" + rrccC.getRrccDocumentoA().getRrccNombre()
                + "\" y \"" + rrccC.getRrccDocumentoB().getRrccNombre() + "\".", "Eliminar", () -> {
            rrccHistorial.rrccEliminarComparacion(rrccC.getRrccId());
            rrccNotificador.rrccExito("Registro eliminado.");
            rrccRefrescar();
        });
    }
}
