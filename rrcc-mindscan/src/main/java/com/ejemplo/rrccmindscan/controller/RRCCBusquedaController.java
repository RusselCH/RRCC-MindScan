package com.ejemplo.rrccmindscan.controller;

import com.ejemplo.rrccmindscan.model.RRCCCategoria;
import com.ejemplo.rrccmindscan.model.RRCCDocumento;
import com.ejemplo.rrccmindscan.service.RRCCBuscadorService;
import com.ejemplo.rrccmindscan.service.RRCCBuscadorService.RRCCOpcionesBusqueda;
import com.ejemplo.rrccmindscan.service.RRCCBuscadorService.RRCCRespuestaBusqueda;
import com.ejemplo.rrccmindscan.service.RRCCBuscadorService.RRCCResultadoBusqueda;
import com.ejemplo.rrccmindscan.service.RRCCDocumentoService;
import com.ejemplo.rrccmindscan.service.RRCCTextoService;
import com.ejemplo.rrccmindscan.ui.*;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RRCCBusquedaController implements RRCCVista {

    public static final String RRCC_TODAS = "Todas";

    private final RRCCBuscadorService rrccBuscador;
    private final RRCCDocumentoService rrccDocumentos;
    private final RRCCTextoService rrccTexto;
    private final RRCCNotificador rrccNotificador;
    private final RRCCContexto rrccContexto;
    private final RRCCMainController rrccMain;

    @FXML private TextField rrccTxtConsulta;
    @FXML private CheckBox rrccChkFrase;
    @FXML private CheckBox rrccChkTolerante;
    @FXML private ComboBox<String> rrccCmbCategoria;
    @FXML private Label rrccLblEstado;
    @FXML private Label rrccLblCorreccion;
    @FXML private VBox rrccCajaResultados;

    @Override
    public void rrccIniciar() {
        rrccCmbCategoria.getItems().add(RRCC_TODAS);
        Arrays.stream(RRCCCategoria.values()).forEach(rrccC -> rrccCmbCategoria.getItems().add(rrccC.rrccEtiqueta()));
        rrccCmbCategoria.getSelectionModel().selectFirst();
        rrccChkTolerante.disableProperty().bind(rrccChkFrase.selectedProperty());
        rrccChkFrase.selectedProperty().addListener((rrccO, rrccA, rrccN) -> rrccRepetir());
        rrccChkTolerante.selectedProperty().addListener((rrccO, rrccA, rrccN) -> rrccRepetir());
        rrccCmbCategoria.valueProperty().addListener((rrccO, rrccA, rrccN) -> rrccRepetir());
        rrccMostrarInicio();
    }

    @Override
    public void rrccRefrescar() {
        String rrccPendiente = rrccContexto.rrccTomarBusqueda();
        if (rrccPendiente != null) {
            rrccTxtConsulta.setText(rrccPendiente);
            rrccChkFrase.setSelected(false);
            rrccOnBuscar();
        } else if (rrccTxtConsulta.getText() != null && !rrccTxtConsulta.getText().isBlank()) {
            rrccOnBuscar();
        }
        rrccTxtConsulta.requestFocus();
    }

    private void rrccRepetir() {
        if (rrccTxtConsulta.getText() != null && !rrccTxtConsulta.getText().isBlank()) {
            rrccOnBuscar();
        }
    }

    private void rrccMostrarInicio() {
        rrccCajaResultados.getChildren().setAll(RRCCGraficos.rrccVacio("Escribe lo que necesitas encontrar",
                "MindScan busca coincidencias en el texto extraído de cada PDF, Word y TXT, aunque la palabra tenga tildes, plurales o pequeños errores de tipeo."));
        rrccLblEstado.setText("");
        rrccLblCorreccion.setVisible(false);
        rrccLblCorreccion.setManaged(false);
    }

    @FXML
    private void rrccOnBuscar() {
        String rrccConsulta = rrccTxtConsulta.getText() == null ? "" : rrccTxtConsulta.getText().strip();
        if (rrccConsulta.isEmpty()) {
            rrccMostrarInicio();
            return;
        }
        RRCCOpcionesBusqueda rrccOpciones = new RRCCOpcionesBusqueda(rrccChkFrase.isSelected(),
                rrccChkTolerante.isSelected(), RRCCCategoria.rrccDesdeEtiqueta(rrccCmbCategoria.getValue()));
        rrccLblEstado.setText("Buscando...");
        RRCCTareas.rrccEjecutar(() -> rrccBuscador.rrccBuscar(rrccDocumentos.rrccListar(), rrccConsulta, rrccOpciones),
                this::rrccPintar, rrccMensaje -> rrccNotificador.rrccError("Error en la búsqueda: " + rrccMensaje));
    }

    private void rrccPintar(RRCCRespuestaBusqueda rrccR) {
        List<RRCCResultadoBusqueda> rrccResultados = rrccR.rrccResultados();
        rrccLblEstado.setText(rrccResultados.size() + (rrccResultados.size() == 1 ? " documento" : " documentos")
                + " en " + rrccR.rrccMilisegundos() + " ms");
        boolean rrccHayCorreccion = !rrccR.rrccCorrecciones().isEmpty();
        rrccLblCorreccion.setVisible(rrccHayCorreccion);
        rrccLblCorreccion.setManaged(rrccHayCorreccion);
        if (rrccHayCorreccion) {
            rrccLblCorreccion.setText("¿Quisiste decir...? Se incluyeron palabras parecidas:  " + String.join("   |   ", rrccR.rrccCorrecciones()));
        }
        rrccCajaResultados.getChildren().clear();
        if (rrccResultados.isEmpty()) {
            rrccCajaResultados.getChildren().add(RRCCGraficos.rrccVacio("Sin resultados",
                    rrccChkFrase.isSelected() ? "Ningún documento contiene esa frase exacta. Desmarca \"Frase exacta\" para buscar las palabras por separado."
                            : "Ningún documento contiene esos términos. Revisa la ortografía o activa la tolerancia a errores."));
            return;
        }
        double rrccMejor = rrccResultados.get(0).rrccPuntaje();
        int rrccPosicion = 1;
        for (RRCCResultadoBusqueda rrccRes : rrccResultados) {
            rrccCajaResultados.getChildren().add(rrccTarjeta(rrccRes, rrccPosicion++, rrccMejor, rrccR));
        }
    }

    private VBox rrccTarjeta(RRCCResultadoBusqueda rrccRes, int rrccPosicion, double rrccMejor, RRCCRespuestaBusqueda rrccR) {
        RRCCDocumento rrccDoc = rrccRes.rrccDocumento();
        Label rrccNumero = new Label(String.valueOf(rrccPosicion));
        rrccNumero.getStyleClass().add("rrcc-numero");
        rrccNumero.setMinWidth(30);
        rrccNumero.setAlignment(Pos.CENTER);
        Label rrccNombre = new Label(rrccDoc.getRrccNombre());
        rrccNombre.getStyleClass().add("rrcc-resultado-titulo");
        HBox rrccChips = new HBox(6, RRCCFormatos.rrccChipTipo(rrccDoc.getRrccTipo()), RRCCFormatos.rrccChipCategoria(rrccDoc.getRrccCategoria()),
                RRCCFormatos.rrccChip(rrccRes.rrccCoincidencias() + " coincidencias", "rrcc-chip-suave"));
        rrccChips.setAlignment(Pos.CENTER_LEFT);
        VBox rrccTitulo = new VBox(4, rrccNombre, rrccChips);
        HBox.setHgrow(rrccTitulo, Priority.ALWAYS);
        HBox rrccMedidor = RRCCGraficos.rrccMedidor(rrccRes.rrccPuntaje() / rrccMejor, rrccPosicion == 1 ? "rrcc-bueno" : "rrcc-acento",
                "Relevancia " + Math.round(rrccRes.rrccPuntaje() / rrccMejor * 100) + " %");
        rrccMedidor.setPrefWidth(250);
        rrccMedidor.setMinWidth(220);
        Button rrccAnalizar = new Button("Analizar");
        rrccAnalizar.getStyleClass().add("button-outlined");
        rrccAnalizar.setOnAction(rrccE -> {
            rrccContexto.rrccActivar(rrccDoc.getRrccId());
            rrccMain.rrccIrA(RRCCMainController.RRCC_VISTA_ANALISIS);
        });
        HBox rrccCabecera = new HBox(12, rrccNumero, rrccTitulo, rrccMedidor, rrccAnalizar);
        rrccCabecera.setAlignment(Pos.CENTER_LEFT);
        VBox rrccTarjeta = new VBox(10, rrccCabecera);
        for (String rrccFragmento : rrccRes.rrccFragmentos()) {
            TextFlow rrccFlujo = rrccR.rrccFrase() != null
                    ? RRCCGraficos.rrccResaltarFrase(rrccFragmento, rrccR.rrccFrase(), rrccTexto)
                    : RRCCGraficos.rrccResaltar(rrccFragmento, rrccRes.rrccClaves(), rrccTexto);
            rrccFlujo.getStyleClass().add("rrcc-fragmento");
            Region rrccMargen = new Region();
            rrccMargen.setMinWidth(30);
            HBox rrccFila = new HBox(12, rrccMargen, rrccFlujo);
            HBox.setHgrow(rrccFlujo, Priority.ALWAYS);
            rrccTarjeta.getChildren().add(rrccFila);
        }
        rrccTarjeta.getStyleClass().add("rrcc-tarjeta");
        return rrccTarjeta;
    }
}
