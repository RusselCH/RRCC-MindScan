package com.ejemplo.rrccmindscan.ui;

import com.ejemplo.rrccmindscan.service.RRCCDocumentoService;
import com.ejemplo.rrccmindscan.service.RRCCDocumentoService.RRCCResultadoImportacion;
import javafx.scene.Node;
import javafx.scene.input.TransferMode;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class RRCCImportador {

    public static final String RRCC_ZONA_ACTIVA = "rrcc-zona-activa";
    public static final int RRCC_ERRORES_VISIBLES = 3;

    private final RRCCDocumentoService rrccDocumentos;
    private final RRCCNotificador rrccNotificador;
    private File rrccUltimaCarpeta;
    private boolean rrccOcupado;

    public void rrccElegirArchivos(Window rrccVentana, Consumer<RRCCResultadoImportacion> rrccAlTerminar) {
        FileChooser rrccSelector = new FileChooser();
        rrccSelector.setTitle("Importar documentos");
        rrccSelector.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documentos (PDF, Word, Texto)", "*.pdf", "*.docx", "*.txt", "*.md", "*.csv"),
                new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Word", "*.docx"),
                new FileChooser.ExtensionFilter("Texto", "*.txt", "*.md", "*.csv"));
        if (rrccUltimaCarpeta != null && rrccUltimaCarpeta.isDirectory()) {
            rrccSelector.setInitialDirectory(rrccUltimaCarpeta);
        }
        List<File> rrccArchivos = rrccSelector.showOpenMultipleDialog(rrccVentana);
        if (rrccArchivos != null && !rrccArchivos.isEmpty()) {
            rrccUltimaCarpeta = rrccArchivos.get(0).getParentFile();
            rrccImportar(rrccArchivos, rrccAlTerminar);
        }
    }

    public void rrccElegirCarpeta(Window rrccVentana, Consumer<RRCCResultadoImportacion> rrccAlTerminar) {
        DirectoryChooser rrccSelector = new DirectoryChooser();
        rrccSelector.setTitle("Importar una carpeta completa");
        if (rrccUltimaCarpeta != null && rrccUltimaCarpeta.isDirectory()) {
            rrccSelector.setInitialDirectory(rrccUltimaCarpeta);
        }
        File rrccCarpeta = rrccSelector.showDialog(rrccVentana);
        if (rrccCarpeta != null) {
            rrccUltimaCarpeta = rrccCarpeta;
            rrccImportar(List.of(rrccCarpeta), rrccAlTerminar);
        }
    }

    public void rrccHabilitarArrastre(Node rrccZona, Consumer<RRCCResultadoImportacion> rrccAlTerminar) {
        rrccZona.setOnDragOver(rrccE -> {
            if (rrccE.getDragboard().hasFiles()) {
                rrccE.acceptTransferModes(TransferMode.COPY);
            }
            rrccE.consume();
        });
        rrccZona.setOnDragEntered(rrccE -> {
            if (rrccE.getDragboard().hasFiles() && !rrccZona.getStyleClass().contains(RRCC_ZONA_ACTIVA)) {
                rrccZona.getStyleClass().add(RRCC_ZONA_ACTIVA);
            }
        });
        rrccZona.setOnDragExited(rrccE -> rrccZona.getStyleClass().remove(RRCC_ZONA_ACTIVA));
        rrccZona.setOnDragDropped(rrccE -> {
            boolean rrccHayArchivos = rrccE.getDragboard().hasFiles();
            if (rrccHayArchivos) {
                rrccImportar(List.copyOf(rrccE.getDragboard().getFiles()), rrccAlTerminar);
            }
            rrccE.setDropCompleted(rrccHayArchivos);
            rrccE.consume();
        });
    }

    public void rrccImportar(List<File> rrccEntradas, Consumer<RRCCResultadoImportacion> rrccAlTerminar) {
        if (rrccOcupado) {
            rrccNotificador.rrccAdvertencia("Ya hay una importación en curso, espera a que termine.");
            return;
        }
        rrccOcupado = true;
        rrccNotificador.rrccInfo("Leyendo y analizando los documentos...");
        RRCCTareas.rrccEjecutar(() -> rrccDocumentos.rrccImportar(rrccEntradas, null), rrccResultado -> {
            rrccOcupado = false;
            rrccInformar(rrccResultado);
            if (rrccAlTerminar != null) {
                rrccAlTerminar.accept(rrccResultado);
            }
        }, rrccMensaje -> {
            rrccOcupado = false;
            rrccNotificador.rrccError("No se pudo importar: " + rrccMensaje);
        });
    }

    private void rrccInformar(RRCCResultadoImportacion rrccR) {
        int rrccN = rrccR.rrccImportados().size();
        if (rrccN > 0) {
            rrccNotificador.rrccExito(rrccN == 1 ? "Se importó \"" + rrccR.rrccImportados().get(0).getRrccNombre() + "\"."
                    : "Se importaron " + rrccN + " documentos.");
        }
        if (rrccR.rrccDuplicados() > 0) {
            rrccNotificador.rrccInfo(rrccR.rrccDuplicados() + (rrccR.rrccDuplicados() == 1
                    ? " archivo ya estaba en la biblioteca (mismo contenido)." : " archivos ya estaban en la biblioteca (mismo contenido)."));
        }
        if (!rrccR.rrccErrores().isEmpty()) {
            List<String> rrccVisibles = rrccR.rrccErrores().subList(0, Math.min(RRCC_ERRORES_VISIBLES, rrccR.rrccErrores().size()));
            String rrccExtra = rrccR.rrccErrores().size() > RRCC_ERRORES_VISIBLES
                    ? "\n... y " + (rrccR.rrccErrores().size() - RRCC_ERRORES_VISIBLES) + " más." : "";
            rrccNotificador.rrccAdvertencia(String.join("\n", rrccVisibles) + rrccExtra);
        }
    }
}
