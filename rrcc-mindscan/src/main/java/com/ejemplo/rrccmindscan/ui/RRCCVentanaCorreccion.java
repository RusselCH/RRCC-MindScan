package com.ejemplo.rrccmindscan.ui;

import atlantafx.base.theme.Styles;
import com.ejemplo.rrccmindscan.model.RRCCDocumento;
import com.ejemplo.rrccmindscan.service.RRCCCorrectorService.RRCCCambio;
import com.ejemplo.rrccmindscan.service.RRCCCorrectorService.RRCCResultadoCorreccion;
import com.ejemplo.rrccmindscan.service.RRCCDocumentoService;
import com.ejemplo.rrccmindscan.service.RRCCReporteService;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class RRCCVentanaCorreccion {

    public static final int RRCC_MAX_CAMBIOS_VISIBLES = 120;
    public static final String RRCC_SUFIJO = " (corregido)";

    private final RRCCDocumentoService rrccDocumentos;
    private final RRCCReporteService rrccReporte;
    private final RRCCNotificador rrccNotificador;

    public void rrccAbrir(RRCCDocumento rrccDoc, RRCCResultadoCorreccion rrccR, Consumer<RRCCDocumento> rrccAlAgregar) {
        Label rrccTitulo = new Label("Corrección automática");
        rrccTitulo.getStyleClass().add("rrcc-drawer-titulo");
        Label rrccSub = new Label(rrccDoc.getRrccNombre() + "\n" + RRCCFormatos.rrccNumero(rrccR.rrccCambios().size())
                + " correcciones seguras: solo se tocan espacios, signos, mayúsculas y palabras repetidas. El original no se modifica.");
        rrccSub.getStyleClass().add("rrcc-drawer-subtitulo");
        rrccSub.setWrapText(true);
        rrccSub.setMinHeight(Region.USE_PREF_SIZE);
        FlowPane rrccChips = new FlowPane(6, 6);
        for (Map.Entry<String, Integer> rrccE : rrccR.rrccPorRegla().entrySet()) {
            rrccChips.getChildren().add(RRCCFormatos.rrccChip(rrccE.getKey() + ": " + rrccE.getValue(), "rrcc-chip-suave"));
        }
        VBox rrccCabecera = new VBox(6, rrccTitulo, rrccSub, rrccChips);
        rrccCabecera.getStyleClass().add("rrcc-drawer-header");

        VBox rrccLista = new VBox(8);
        int rrccMostrados = Math.min(RRCC_MAX_CAMBIOS_VISIBLES, rrccR.rrccCambios().size());
        for (int rrccI = 0; rrccI < rrccMostrados; rrccI++) {
            rrccLista.getChildren().add(rrccFilaCambio(rrccR.rrccCambios().get(rrccI)));
        }
        if (rrccR.rrccCambios().size() > rrccMostrados) {
            Label rrccMas = new Label("y " + RRCCFormatos.rrccNumero(rrccR.rrccCambios().size() - rrccMostrados) + " correcciones más.");
            rrccMas.getStyleClass().add("rrcc-texto-suave");
            rrccLista.getChildren().add(rrccMas);
        }
        ScrollPane rrccScroll = new ScrollPane(rrccLista);
        rrccScroll.setFitToWidth(true);
        rrccScroll.getStyleClass().add("rrcc-pagina-scroll");
        VBox.setVgrow(rrccScroll, Priority.ALWAYS);
        VBox rrccCuerpo = new VBox(rrccScroll);
        rrccCuerpo.getStyleClass().add("rrcc-drawer-body");
        VBox.setVgrow(rrccCuerpo, Priority.ALWAYS);

        Button rrccWord = new Button("Guardar en Word");
        rrccWord.getStyleClass().add(Styles.BUTTON_OUTLINED);
        rrccWord.setOnAction(rrccE -> rrccGuardar(rrccDoc, rrccR, rrccWord));
        Button rrccAgregar = new Button("Agregar a la biblioteca");
        rrccAgregar.getStyleClass().add(Styles.BUTTON_OUTLINED);
        rrccAgregar.setOnAction(rrccE -> rrccAgregar(rrccDoc, rrccR, rrccAlAgregar));
        Button rrccCerrar = new Button("Cerrar");
        rrccCerrar.getStyleClass().add(Styles.ACCENT);
        rrccCerrar.setOnAction(rrccE -> rrccNotificador.rrccCerrarPanel());
        Region rrccEspacio = new Region();
        HBox.setHgrow(rrccEspacio, Priority.ALWAYS);
        HBox rrccPie = new HBox(10, rrccWord, rrccAgregar, rrccEspacio, rrccCerrar);
        rrccPie.getStyleClass().add("rrcc-drawer-footer");
        rrccPie.setAlignment(Pos.CENTER_RIGHT);

        VBox rrccDrawer = new VBox(rrccCabecera, rrccCuerpo, rrccPie);
        rrccDrawer.getStyleClass().add("rrcc-drawer");
        rrccDrawer.setPrefWidth(RRCCLector.RRCC_ANCHO);
        rrccDrawer.setMaxWidth(RRCCLector.RRCC_ANCHO);
        rrccDrawer.setMaxHeight(Double.MAX_VALUE);
        rrccNotificador.rrccAbrirPanel(rrccDrawer);
    }

    private VBox rrccFilaCambio(RRCCCambio rrccC) {
        Label rrccRegla = RRCCFormatos.rrccChip(rrccC.rrccRegla(), "rrcc-chip-suave");
        Label rrccAntes = new Label(rrccVisible(rrccC.rrccAntes()));
        rrccAntes.getStyleClass().add("rrcc-cambio-antes");
        rrccAntes.setWrapText(true);
        Label rrccDespues = new Label(rrccVisible(rrccC.rrccDespues()));
        rrccDespues.getStyleClass().add("rrcc-cambio-despues");
        rrccDespues.setWrapText(true);
        Label rrccFlecha = new Label("→");
        rrccFlecha.getStyleClass().add("rrcc-texto-suave");
        rrccFlecha.setMinWidth(Region.USE_PREF_SIZE);
        for (Label rrccLado : new Label[]{rrccAntes, rrccDespues}) {
            rrccLado.setPrefWidth(0);
            rrccLado.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(rrccLado, Priority.ALWAYS);
        }
        HBox rrccLinea = new HBox(10, rrccAntes, rrccFlecha, rrccDespues);
        rrccLinea.setAlignment(Pos.CENTER_LEFT);
        VBox rrccFila = new VBox(6, rrccRegla, rrccLinea);
        rrccFila.getStyleClass().add("rrcc-cambio-fila");
        return rrccFila;
    }

    private String rrccVisible(String rrccTexto) {
        return rrccTexto.replace(" ", "·").replace("\t", "⇥");
    }

    private void rrccGuardar(RRCCDocumento rrccDoc, RRCCResultadoCorreccion rrccR, Button rrccOrigen) {
        FileChooser rrccSelector = new FileChooser();
        rrccSelector.setTitle("Guardar texto corregido");
        rrccSelector.setInitialFileName(rrccNombreBase(rrccDoc) + RRCC_SUFIJO + ".docx");
        rrccSelector.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documento de Word", "*.docx"),
                new FileChooser.ExtensionFilter("Texto plano", "*.txt"));
        File rrccArchivo = rrccSelector.showSaveDialog(rrccOrigen.getScene().getWindow());
        if (rrccArchivo == null) {
            return;
        }
        RRCCTareas.rrccEjecutar(() -> {
            try {
                if (rrccArchivo.getName().toLowerCase().endsWith(".txt")) {
                    Files.writeString(rrccArchivo.toPath(), rrccR.rrccCorregido(), StandardCharsets.UTF_8);
                } else {
                    rrccReporte.rrccExportarTexto(rrccNombreBase(rrccDoc), rrccR.rrccCorregido(), rrccArchivo);
                }
                return rrccArchivo;
            } catch (Exception rrccError) {
                throw new IllegalStateException(rrccError.getMessage(), rrccError);
            }
        }, rrccF -> rrccNotificador.rrccExito("Texto corregido guardado: " + rrccF.getName()),
                rrccMensaje -> rrccNotificador.rrccError("No se pudo guardar: " + rrccMensaje));
    }

    private void rrccAgregar(RRCCDocumento rrccDoc, RRCCResultadoCorreccion rrccR, Consumer<RRCCDocumento> rrccAlAgregar) {
        String rrccNombre = rrccNombreBase(rrccDoc) + RRCC_SUFIJO + ".txt";
        RRCCTareas.rrccEjecutar(() -> rrccDocumentos.rrccImportarTexto(rrccNombre, rrccR.rrccCorregido()), rrccNuevo -> {
            rrccNotificador.rrccCerrarPanel();
            rrccNotificador.rrccExito("Se agregó \"" + rrccNombre + "\" a la biblioteca.");
            rrccAlAgregar.accept(rrccNuevo);
        }, rrccMensaje -> rrccNotificador.rrccError("No se pudo agregar: " + rrccMensaje));
    }

    private String rrccNombreBase(RRCCDocumento rrccDoc) {
        String rrccNombre = rrccDoc.getRrccNombre();
        int rrccPunto = rrccNombre.lastIndexOf('.');
        return rrccPunto > 0 ? rrccNombre.substring(0, rrccPunto) : rrccNombre;
    }
}
