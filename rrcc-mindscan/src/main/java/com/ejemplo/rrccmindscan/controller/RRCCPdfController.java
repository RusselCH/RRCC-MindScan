package com.ejemplo.rrccmindscan.controller;

import com.ejemplo.rrccmindscan.service.RRCCPdfService;
import com.ejemplo.rrccmindscan.service.RRCCPdfService.RRCCResultadoPdf;
import com.ejemplo.rrccmindscan.ui.RRCCFormatos;
import com.ejemplo.rrccmindscan.ui.RRCCNotificador;
import com.ejemplo.rrccmindscan.ui.RRCCTareas;
import com.ejemplo.rrccmindscan.ui.RRCCVista;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class RRCCPdfController implements RRCCVista {

    public static final String RRCC_GRUPO_ORGANIZAR = "Organizar PDF";
    public static final String RRCC_GRUPO_OPTIMIZAR = "Optimizar";
    public static final String RRCC_GRUPO_A_PDF = "Convertir a PDF";
    public static final String RRCC_GRUPO_DESDE_PDF = "Convertir desde PDF";
    public static final String RRCC_GRUPO_EDITAR = "Editar PDF";
    public static final String RRCC_GRUPO_SEGURIDAD = "Seguridad";
    public static final List<String> RRCC_GRUPOS = List.of(RRCC_GRUPO_ORGANIZAR, RRCC_GRUPO_OPTIMIZAR, RRCC_GRUPO_A_PDF,
            RRCC_GRUPO_DESDE_PDF, RRCC_GRUPO_EDITAR, RRCC_GRUPO_SEGURIDAD);

    private enum RRCCOpcion { RRCC_NINGUNA, RRCC_RANGOS, RRCC_ORDEN, RRCC_ROTAR, RRCC_TEXTO, RRCC_FORMATO, RRCC_CLAVE, RRCC_CALIDAD, RRCC_DPI }

    private enum RRCCSalida { RRCC_ARCHIVO, RRCC_CARPETA }

    private record RRCCHerramienta(String rrccNombre, String rrccDescripcion, String rrccGrupo, String rrccTono,
                                   List<String> rrccExtensiones, boolean rrccVarios, RRCCOpcion rrccOpcion,
                                   RRCCSalida rrccSalida, String rrccExtensionSalida, String rrccSufijo) {
    }

    private record RRCCAccion(RRCCHerramienta rrccHerramienta, List<File> rrccArchivos, String rrccValor, File rrccDestino) {
    }

    private static final List<String> RRCC_PDF = List.of("pdf");

    private final RRCCPdfService rrccPdf;
    private final RRCCNotificador rrccNotificador;

    @FXML private VBox rrccCajaGrupos;
    @FXML private VBox rrccAreaTrabajo;
    @FXML private Label rrccLblHerramienta;
    @FXML private Label rrccLblDescripcion;
    @FXML private VBox rrccZonaArchivos;
    @FXML private ListView<String> rrccLista;
    @FXML private Label rrccLblArchivos;
    @FXML private HBox rrccBarraOrden;
    @FXML private VBox rrccCajaOpcion;
    @FXML private Label rrccLblOpcion;
    @FXML private Label rrccLblAyudaOpcion;
    @FXML private TextField rrccTxtOpcion;
    @FXML private PasswordField rrccTxtClave;
    @FXML private ComboBox<String> rrccCmbOpcion;
    @FXML private Button rrccBtnProcesar;
    @FXML private VBox rrccCajaResultado;

    private RRCCHerramienta rrccActual;
    private final List<File> rrccArchivos = new ArrayList<>();

    @Override
    public void rrccIniciar() {
        for (String rrccGrupo : RRCC_GRUPOS) {
            Label rrccTitulo = new Label(rrccGrupo);
            rrccTitulo.getStyleClass().add("rrcc-pdf-grupo");
            FlowPane rrccTarjetas = new FlowPane(12, 12);
            rrccHerramientas().stream().filter(rrccH -> rrccH.rrccGrupo().equals(rrccGrupo))
                    .forEach(rrccH -> rrccTarjetas.getChildren().add(rrccTarjeta(rrccH)));
            rrccCajaGrupos.getChildren().addAll(rrccTitulo, rrccTarjetas);
        }
        rrccZonaArchivos.setOnDragOver(rrccE -> {
            if (rrccE.getDragboard().hasFiles() && rrccActual != null) {
                rrccE.acceptTransferModes(TransferMode.COPY);
            }
            rrccE.consume();
        });
        rrccZonaArchivos.setOnDragDropped(rrccE -> {
            boolean rrccHay = rrccE.getDragboard().hasFiles();
            if (rrccHay) {
                rrccAgregar(rrccE.getDragboard().getFiles());
            }
            rrccE.setDropCompleted(rrccHay);
            rrccE.consume();
        });
        rrccMostrarArea(false);
    }

    @Override
    public void rrccRefrescar() {
    }

    private List<RRCCHerramienta> rrccHerramientas() {
        List<String> rrccImagenes = List.of("png", "jpg", "jpeg", "bmp", "gif");
        return List.of(
                new RRCCHerramienta("Unir PDF", "Combina varios PDF en uno solo, en el orden que elijas.", RRCC_GRUPO_ORGANIZAR, "rrcc-tono-1", RRCC_PDF, true, RRCCOpcion.RRCC_NINGUNA, RRCCSalida.RRCC_ARCHIVO, "pdf", "unido"),
                new RRCCHerramienta("Dividir PDF", "Separa cada página o cada rango en un PDF distinto.", RRCC_GRUPO_ORGANIZAR, "rrcc-tono-1", RRCC_PDF, false, RRCCOpcion.RRCC_RANGOS, RRCCSalida.RRCC_CARPETA, "pdf", ""),
                new RRCCHerramienta("Extraer páginas", "Crea un PDF nuevo solo con las páginas que elijas.", RRCC_GRUPO_ORGANIZAR, "rrcc-tono-1", RRCC_PDF, false, RRCCOpcion.RRCC_RANGOS, RRCCSalida.RRCC_ARCHIVO, "pdf", "extraido"),
                new RRCCHerramienta("Eliminar páginas", "Quita del PDF las páginas que no necesitas.", RRCC_GRUPO_ORGANIZAR, "rrcc-tono-1", RRCC_PDF, false, RRCCOpcion.RRCC_RANGOS, RRCCSalida.RRCC_ARCHIVO, "pdf", "sin-paginas"),
                new RRCCHerramienta("Ordenar páginas", "Cambia el orden de las páginas del PDF.", RRCC_GRUPO_ORGANIZAR, "rrcc-tono-1", RRCC_PDF, false, RRCCOpcion.RRCC_ORDEN, RRCCSalida.RRCC_ARCHIVO, "pdf", "ordenado"),
                new RRCCHerramienta("Rotar PDF", "Gira todas las páginas o solo algunas.", RRCC_GRUPO_ORGANIZAR, "rrcc-tono-1", RRCC_PDF, false, RRCCOpcion.RRCC_ROTAR, RRCCSalida.RRCC_ARCHIVO, "pdf", "rotado"),
                new RRCCHerramienta("Comprimir PDF", "Reduce el peso del PDF recomprimiendo sus imágenes.", RRCC_GRUPO_OPTIMIZAR, "rrcc-tono-2", RRCC_PDF, false, RRCCOpcion.RRCC_CALIDAD, RRCCSalida.RRCC_ARCHIVO, "pdf", "comprimido"),
                new RRCCHerramienta("Word a PDF", "Convierte un documento .docx en PDF.", RRCC_GRUPO_A_PDF, "rrcc-tono-3", List.of("docx"), false, RRCCOpcion.RRCC_NINGUNA, RRCCSalida.RRCC_ARCHIVO, "pdf", ""),
                new RRCCHerramienta("Imágenes a PDF", "Convierte fotos JPG o PNG en un PDF, una por página.", RRCC_GRUPO_A_PDF, "rrcc-tono-3", rrccImagenes, true, RRCCOpcion.RRCC_NINGUNA, RRCCSalida.RRCC_ARCHIVO, "pdf", "imagenes"),
                new RRCCHerramienta("Texto a PDF", "Convierte un archivo .txt en PDF.", RRCC_GRUPO_A_PDF, "rrcc-tono-3", List.of("txt", "md", "csv"), false, RRCCOpcion.RRCC_NINGUNA, RRCCSalida.RRCC_ARCHIVO, "pdf", ""),
                new RRCCHerramienta("PDF a Word", "Convierte el texto del PDF en un documento .docx editable.", RRCC_GRUPO_DESDE_PDF, "rrcc-tono-4", RRCC_PDF, false, RRCCOpcion.RRCC_NINGUNA, RRCCSalida.RRCC_ARCHIVO, "docx", ""),
                new RRCCHerramienta("PDF a imágenes", "Guarda cada página del PDF como imagen PNG.", RRCC_GRUPO_DESDE_PDF, "rrcc-tono-4", RRCC_PDF, false, RRCCOpcion.RRCC_DPI, RRCCSalida.RRCC_CARPETA, "png", ""),
                new RRCCHerramienta("Marca de agua", "Escribe un texto en diagonal sobre todas las páginas.", RRCC_GRUPO_EDITAR, "rrcc-tono-5", RRCC_PDF, false, RRCCOpcion.RRCC_TEXTO, RRCCSalida.RRCC_ARCHIVO, "pdf", "marca-de-agua"),
                new RRCCHerramienta("Números de página", "Agrega la numeración al pie de cada página.", RRCC_GRUPO_EDITAR, "rrcc-tono-5", RRCC_PDF, false, RRCCOpcion.RRCC_FORMATO, RRCCSalida.RRCC_ARCHIVO, "pdf", "numerado"),
                new RRCCHerramienta("Proteger PDF", "Pone una contraseña para abrir el PDF.", RRCC_GRUPO_SEGURIDAD, "rrcc-tono-6", RRCC_PDF, false, RRCCOpcion.RRCC_CLAVE, RRCCSalida.RRCC_ARCHIVO, "pdf", "protegido"),
                new RRCCHerramienta("Desbloquear PDF", "Quita la contraseña de un PDF que conoces.", RRCC_GRUPO_SEGURIDAD, "rrcc-tono-6", RRCC_PDF, false, RRCCOpcion.RRCC_CLAVE, RRCCSalida.RRCC_ARCHIVO, "pdf", "desbloqueado"));
    }

    private VBox rrccTarjeta(RRCCHerramienta rrccH) {
        Label rrccNombre = new Label(rrccH.rrccNombre());
        rrccNombre.getStyleClass().add("rrcc-pdf-tarjeta-titulo");
        Label rrccDescripcion = new Label(rrccH.rrccDescripcion());
        rrccDescripcion.getStyleClass().add("rrcc-texto-suave");
        rrccDescripcion.setWrapText(true);
        VBox rrccTarjeta = new VBox(6, rrccNombre, rrccDescripcion);
        rrccTarjeta.getStyleClass().addAll("rrcc-pdf-tarjeta", rrccH.rrccTono());
        rrccTarjeta.setPrefWidth(230);
        rrccTarjeta.setMinHeight(96);
        rrccTarjeta.setOnMouseClicked(rrccE -> rrccElegir(rrccH, rrccTarjeta));
        return rrccTarjeta;
    }

    private void rrccElegir(RRCCHerramienta rrccH, VBox rrccTarjeta) {
        rrccCajaGrupos.lookupAll(".rrcc-pdf-tarjeta").forEach(rrccN -> rrccN.getStyleClass().remove("rrcc-pdf-tarjeta-activa"));
        rrccTarjeta.getStyleClass().add("rrcc-pdf-tarjeta-activa");
        rrccActual = rrccH;
        rrccArchivos.clear();
        rrccLblHerramienta.setText(rrccH.rrccNombre());
        rrccLblDescripcion.setText(rrccH.rrccDescripcion());
        rrccBarraOrden.setVisible(rrccH.rrccVarios());
        rrccBarraOrden.setManaged(rrccH.rrccVarios());
        rrccCajaResultado.getChildren().clear();
        rrccConfigurarOpcion(rrccH.rrccOpcion());
        rrccActualizarLista();
        rrccMostrarArea(true);
        rrccAreaTrabajo.requestFocus();
    }

    private void rrccConfigurarOpcion(RRCCOpcion rrccOpcion) {
        boolean rrccHay = rrccOpcion != RRCCOpcion.RRCC_NINGUNA;
        rrccCajaOpcion.setVisible(rrccHay);
        rrccCajaOpcion.setManaged(rrccHay);
        rrccMostrar(rrccTxtOpcion, false);
        rrccMostrar(rrccTxtClave, false);
        rrccMostrar(rrccCmbOpcion, false);
        rrccTxtOpcion.clear();
        rrccTxtClave.clear();
        rrccCmbOpcion.getItems().clear();
        switch (rrccOpcion) {
            case RRCC_RANGOS -> rrccOpcionTexto("Páginas", "Ejemplo: 1-3, 5, 8-10. En «Dividir» cada rango será un PDF; déjalo vacío para separar todas las páginas.", "1-3, 5");
            case RRCC_ORDEN -> rrccOpcionTexto("Nuevo orden", "Escribe el orden de las páginas, por ejemplo 3, 1, 2. Las páginas que no escribas quedan al final.", "3, 1, 2");
            case RRCC_TEXTO -> rrccOpcionTexto("Texto de la marca", "Se escribe en diagonal y semitransparente en cada página.", "CONFIDENCIAL");
            case RRCC_FORMATO -> {
                rrccOpcionTexto("Formato", "Usa {n} para el número de página y {total} para el total.", "Página {n} de {total}");
                rrccTxtOpcion.setText("Página {n} de {total}");
            }
            case RRCC_CLAVE -> {
                rrccLblOpcion.setText("Contraseña");
                rrccLblAyudaOpcion.setText("En «Proteger» será la contraseña para abrir el PDF; en «Desbloquear», la contraseña actual.");
                rrccMostrar(rrccTxtClave, true);
            }
            case RRCC_ROTAR -> rrccOpcionLista("Giro", "Gira en sentido horario. Escribe páginas en el campo para rotar solo algunas; vacío = todas.",
                    List.of("90° a la derecha", "180°", "90° a la izquierda"), true);
            case RRCC_CALIDAD -> rrccOpcionLista("Nivel de compresión", "A mayor compresión, menor peso y algo menos de nitidez en las imágenes.",
                    List.of("Recomendada", "Alta compresión", "Baja compresión"), false);
            case RRCC_DPI -> rrccOpcionLista("Resolución", "Más resolución da imágenes más nítidas pero más pesadas.",
                    List.of("150 ppp (recomendada)", "72 ppp (pantalla)", "300 ppp (impresión)"), false);
            default -> {
            }
        }
    }

    private void rrccOpcionTexto(String rrccTitulo, String rrccAyuda, String rrccEjemplo) {
        rrccLblOpcion.setText(rrccTitulo);
        rrccLblAyudaOpcion.setText(rrccAyuda);
        rrccTxtOpcion.setPromptText(rrccEjemplo);
        rrccMostrar(rrccTxtOpcion, true);
    }

    private void rrccOpcionLista(String rrccTitulo, String rrccAyuda, List<String> rrccValores, boolean rrccConRangos) {
        rrccLblOpcion.setText(rrccTitulo);
        rrccLblAyudaOpcion.setText(rrccAyuda);
        rrccCmbOpcion.getItems().setAll(rrccValores);
        rrccCmbOpcion.getSelectionModel().selectFirst();
        rrccMostrar(rrccCmbOpcion, true);
        if (rrccConRangos) {
            rrccTxtOpcion.setPromptText("Páginas (opcional), por ejemplo 2, 4-5");
            rrccMostrar(rrccTxtOpcion, true);
        }
    }

    private void rrccMostrar(Node rrccNodo, boolean rrccVisible) {
        rrccNodo.setVisible(rrccVisible);
        rrccNodo.setManaged(rrccVisible);
    }

    private void rrccMostrarArea(boolean rrccVisible) {
        rrccAreaTrabajo.setVisible(rrccVisible);
        rrccAreaTrabajo.setManaged(rrccVisible);
    }

    @FXML
    private void rrccOnElegirArchivos() {
        if (rrccActual == null) {
            return;
        }
        FileChooser rrccSelector = new FileChooser();
        rrccSelector.setTitle(rrccActual.rrccNombre() + " - elegir archivos");
        rrccSelector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos permitidos",
                rrccActual.rrccExtensiones().stream().map(rrccX -> "*." + rrccX).toList()));
        Window rrccVentana = rrccLista.getScene().getWindow();
        if (rrccActual.rrccVarios()) {
            List<File> rrccElegidos = rrccSelector.showOpenMultipleDialog(rrccVentana);
            if (rrccElegidos != null) {
                rrccAgregar(rrccElegidos);
            }
        } else {
            File rrccElegido = rrccSelector.showOpenDialog(rrccVentana);
            if (rrccElegido != null) {
                rrccAgregar(List.of(rrccElegido));
            }
        }
    }

    private void rrccAgregar(List<File> rrccNuevos) {
        List<File> rrccValidos = rrccNuevos.stream().filter(rrccF -> rrccActual.rrccExtensiones().contains(rrccExtension(rrccF))).toList();
        if (rrccValidos.size() < rrccNuevos.size()) {
            rrccNotificador.rrccAdvertencia("Se ignoraron archivos que no son " + String.join(", ", rrccActual.rrccExtensiones()).toUpperCase(Locale.ROOT) + ".");
        }
        if (rrccValidos.isEmpty()) {
            return;
        }
        if (!rrccActual.rrccVarios()) {
            rrccArchivos.clear();
            rrccArchivos.add(rrccValidos.get(0));
        } else {
            rrccValidos.stream().filter(rrccF -> !rrccArchivos.contains(rrccF)).forEach(rrccArchivos::add);
        }
        rrccCajaResultado.getChildren().clear();
        rrccActualizarLista();
    }

    private void rrccActualizarLista() {
        List<String> rrccFilas = new ArrayList<>();
        for (int rrccI = 0; rrccI < rrccArchivos.size(); rrccI++) {
            File rrccF = rrccArchivos.get(rrccI);
            rrccFilas.add((rrccI + 1) + ".  " + rrccF.getName() + "   ·   " + RRCCFormatos.rrccBytes(rrccF.length()));
        }
        rrccLista.getItems().setAll(rrccFilas);
        rrccLista.setPrefHeight(Math.max(1, Math.min(6, rrccArchivos.size())) * 34 + 6);
        rrccBtnProcesar.setDisable(rrccArchivos.isEmpty());
        if (rrccArchivos.isEmpty()) {
            rrccLblArchivos.setText(rrccActual != null && rrccActual.rrccVarios()
                    ? "Arrastra aquí los archivos o pulsa «Elegir archivos». Puedes cambiar el orden antes de procesar."
                    : "Arrastra aquí el archivo o pulsa «Elegir archivo».");
            return;
        }
        File rrccPrimero = rrccArchivos.get(0);
        if (rrccArchivos.size() == 1 && "pdf".equals(rrccExtension(rrccPrimero))) {
            RRCCTareas.rrccEjecutar(() -> rrccPdf.rrccEstaProtegido(rrccPrimero) ? -1 : rrccPdf.rrccContarPaginas(rrccPrimero),
                    rrccPaginas -> rrccLblArchivos.setText(rrccPaginas < 0 ? "PDF protegido con contraseña."
                            : "El PDF tiene " + rrccPaginas + (rrccPaginas == 1 ? " página." : " páginas.")),
                    rrccMensaje -> rrccLblArchivos.setText("No se pudo leer el PDF: " + rrccMensaje));
        } else {
            rrccLblArchivos.setText(rrccArchivos.size() + " archivos · " + RRCCFormatos.rrccBytes(rrccArchivos.stream().mapToLong(File::length).sum()));
        }
    }

    @FXML
    private void rrccOnSubir() {
        rrccMover(-1);
    }

    @FXML
    private void rrccOnBajar() {
        rrccMover(1);
    }

    @FXML
    private void rrccOnQuitar() {
        int rrccI = rrccLista.getSelectionModel().getSelectedIndex();
        if (rrccI >= 0) {
            rrccArchivos.remove(rrccI);
            rrccActualizarLista();
        }
    }

    private void rrccMover(int rrccPaso) {
        int rrccI = rrccLista.getSelectionModel().getSelectedIndex();
        int rrccJ = rrccI + rrccPaso;
        if (rrccI < 0 || rrccJ < 0 || rrccJ >= rrccArchivos.size()) {
            return;
        }
        File rrccTmp = rrccArchivos.get(rrccI);
        rrccArchivos.set(rrccI, rrccArchivos.get(rrccJ));
        rrccArchivos.set(rrccJ, rrccTmp);
        rrccActualizarLista();
        rrccLista.getSelectionModel().select(rrccJ);
    }

    @FXML
    private void rrccOnProcesar() {
        if (rrccActual == null || rrccArchivos.isEmpty()) {
            return;
        }
        File rrccDestino = rrccElegirDestino();
        if (rrccDestino == null) {
            return;
        }
        RRCCAccion rrccAccion = new RRCCAccion(rrccActual, List.copyOf(rrccArchivos), rrccValorOpcion(), rrccDestino);
        rrccBtnProcesar.setDisable(true);
        rrccBtnProcesar.setText("Procesando…");
        RRCCTareas.rrccEjecutar(() -> rrccEjecutar(rrccAccion), rrccR -> {
            rrccRestaurarBoton();
            rrccMostrarResultado(rrccR);
            rrccNotificador.rrccExito(rrccAccion.rrccHerramienta().rrccNombre() + ": " + rrccR.rrccResumen());
        }, rrccMensaje -> {
            rrccRestaurarBoton();
            rrccNotificador.rrccError(rrccMensaje);
        });
    }

    private void rrccRestaurarBoton() {
        rrccBtnProcesar.setText("Procesar");
        rrccBtnProcesar.setDisable(false);
    }

    private File rrccElegirDestino() {
        File rrccPrimero = rrccArchivos.get(0);
        Window rrccVentana = rrccLista.getScene().getWindow();
        if (rrccActual.rrccSalida() == RRCCSalida.RRCC_CARPETA) {
            DirectoryChooser rrccCarpeta = new DirectoryChooser();
            rrccCarpeta.setTitle("Carpeta donde guardar los archivos");
            rrccCarpeta.setInitialDirectory(rrccPrimero.getParentFile());
            return rrccCarpeta.showDialog(rrccVentana);
        }
        FileChooser rrccGuardar = new FileChooser();
        rrccGuardar.setTitle("Guardar resultado");
        rrccGuardar.setInitialDirectory(rrccPrimero.getParentFile());
        String rrccBase = rrccPrimero.getName().replaceFirst("\\.[^.]+$", "");
        rrccGuardar.setInitialFileName(rrccBase + (rrccActual.rrccSufijo().isEmpty() ? "" : "-" + rrccActual.rrccSufijo())
                + "." + rrccActual.rrccExtensionSalida());
        rrccGuardar.getExtensionFilters().add(new FileChooser.ExtensionFilter(rrccActual.rrccExtensionSalida().toUpperCase(Locale.ROOT),
                "*." + rrccActual.rrccExtensionSalida()));
        File rrccDestino = rrccGuardar.showSaveDialog(rrccVentana);
        if (rrccDestino != null && rrccArchivos.contains(rrccDestino)) {
            rrccNotificador.rrccAdvertencia("Elige otro nombre: no se puede sobrescribir el archivo original.");
            return null;
        }
        return rrccDestino;
    }

    private String rrccValorOpcion() {
        return switch (rrccActual.rrccOpcion()) {
            case RRCC_CLAVE -> rrccTxtClave.getText();
            case RRCC_ROTAR, RRCC_CALIDAD, RRCC_DPI -> rrccCmbOpcion.getValue();
            default -> rrccTxtOpcion.getText();
        };
    }

    private RRCCResultadoPdf rrccEjecutar(RRCCAccion rrccA) {
        File rrccEntrada = rrccA.rrccArchivos().get(0);
        String rrccValor = rrccA.rrccValor();
        File rrccDestino = rrccA.rrccDestino();
        return switch (rrccA.rrccHerramienta().rrccNombre()) {
            case "Unir PDF" -> rrccPdf.rrccUnir(rrccA.rrccArchivos(), rrccDestino);
            case "Dividir PDF" -> rrccPdf.rrccDividir(rrccEntrada, rrccValor, rrccDestino);
            case "Extraer páginas" -> rrccPdf.rrccExtraer(rrccEntrada, rrccValor, rrccDestino);
            case "Eliminar páginas" -> rrccPdf.rrccEliminar(rrccEntrada, rrccValor, rrccDestino);
            case "Ordenar páginas" -> rrccPdf.rrccOrdenar(rrccEntrada, rrccValor, rrccDestino);
            case "Rotar PDF" -> rrccPdf.rrccRotar(rrccEntrada, rrccGrados(rrccCmbOpcionValor(rrccValor)), rrccTxtOpcionValor(), rrccDestino);
            case "Comprimir PDF" -> rrccPdf.rrccComprimir(rrccEntrada, rrccCalidad(rrccValor), rrccDestino);
            case "Word a PDF" -> rrccPdf.rrccWordAPdf(rrccEntrada, rrccDestino);
            case "Imágenes a PDF" -> rrccPdf.rrccImagenesAPdf(rrccA.rrccArchivos(), rrccDestino);
            case "Texto a PDF" -> rrccPdf.rrccTextoAPdf(rrccEntrada, rrccDestino);
            case "PDF a Word" -> rrccPdf.rrccPdfAWord(rrccEntrada, rrccDestino);
            case "PDF a imágenes" -> rrccPdf.rrccPdfAImagenes(rrccEntrada, rrccDpi(rrccValor), rrccDestino);
            case "Marca de agua" -> rrccPdf.rrccMarcaDeAgua(rrccEntrada, rrccValor, rrccDestino);
            case "Números de página" -> rrccPdf.rrccNumerar(rrccEntrada, rrccValor, rrccDestino);
            case "Proteger PDF" -> rrccPdf.rrccProteger(rrccEntrada, rrccValor, rrccDestino);
            case "Desbloquear PDF" -> rrccPdf.rrccDesbloquear(rrccEntrada, rrccValor, rrccDestino);
            default -> throw new IllegalStateException("Herramienta desconocida");
        };
    }

    private String rrccCmbOpcionValor(String rrccValor) {
        return rrccValor == null ? "" : rrccValor;
    }

    private String rrccTxtOpcionValor() {
        return rrccTxtOpcion.getText();
    }

    private int rrccGrados(String rrccValor) {
        return rrccValor.startsWith("180") ? 180 : rrccValor.contains("izquierda") ? 270 : 90;
    }

    private float rrccCalidad(String rrccValor) {
        return rrccValor != null && rrccValor.startsWith("Alta") ? 0.45f : rrccValor != null && rrccValor.startsWith("Baja") ? 0.85f : 0.65f;
    }

    private int rrccDpi(String rrccValor) {
        return rrccValor != null && rrccValor.startsWith("72") ? 72 : rrccValor != null && rrccValor.startsWith("300") ? 300 : 150;
    }

    private void rrccMostrarResultado(RRCCResultadoPdf rrccR) {
        Label rrccTitulo = new Label("Listo");
        rrccTitulo.getStyleClass().add("rrcc-tarjeta-titulo");
        Label rrccResumen = new Label(rrccR.rrccResumen());
        rrccResumen.setWrapText(true);
        String rrccPesos = RRCCFormatos.rrccBytes(rrccR.rrccBytesAntes()) + " → " + RRCCFormatos.rrccBytes(rrccR.rrccBytesDespues());
        if (rrccR.rrccBytesAntes() > 0 && rrccR.rrccBytesDespues() < rrccR.rrccBytesAntes()) {
            rrccPesos += "  (" + Math.round(100.0 * (rrccR.rrccBytesAntes() - rrccR.rrccBytesDespues()) / rrccR.rrccBytesAntes()) + " % menos)";
        }
        Label rrccPeso = new Label(rrccPesos);
        rrccPeso.getStyleClass().add("rrcc-texto-suave");
        VBox rrccLista2 = new VBox(4);
        rrccR.rrccArchivos().stream().limit(8).forEach(rrccF -> rrccLista2.getChildren().add(new Label("•  " + rrccF.getName())));
        if (rrccR.rrccArchivos().size() > 8) {
            rrccLista2.getChildren().add(new Label("… y " + (rrccR.rrccArchivos().size() - 8) + " archivos más"));
        }
        File rrccPrimero = rrccR.rrccArchivos().get(0);
        Button rrccAbrir = new Button(rrccR.rrccArchivos().size() == 1 ? "Abrir archivo" : "Abrir carpeta");
        rrccAbrir.getStyleClass().add("accent");
        rrccAbrir.setOnAction(rrccE -> rrccAbrirEnSistema(rrccR.rrccArchivos().size() == 1 ? rrccPrimero : rrccPrimero.getParentFile()));
        Button rrccCarpeta = new Button("Mostrar carpeta");
        rrccCarpeta.getStyleClass().add("button-outlined");
        rrccCarpeta.setOnAction(rrccE -> rrccAbrirEnSistema(rrccPrimero.getParentFile()));
        HBox rrccAcciones = new HBox(10, rrccAbrir, rrccCarpeta);
        rrccAcciones.setAlignment(Pos.CENTER_LEFT);
        VBox rrccTarjeta = new VBox(8, rrccTitulo, rrccResumen, rrccPeso, rrccLista2, rrccAcciones);
        rrccTarjeta.getStyleClass().addAll("rrcc-tarjeta", "rrcc-pdf-resultado");
        rrccCajaResultado.getChildren().setAll(rrccTarjeta);
    }

    private void rrccAbrirEnSistema(File rrccArchivo) {
        RRCCTareas.rrccEjecutar(() -> {
            try {
                new ProcessBuilder("explorer.exe", rrccArchivo.getAbsolutePath()).start();
                return true;
            } catch (IOException rrccError) {
                throw new IllegalStateException(rrccError.getMessage(), rrccError);
            }
        }, rrccOk -> {
        }, rrccMensaje -> rrccNotificador.rrccError("No se pudo abrir: " + rrccMensaje));
    }

    private static String rrccExtension(File rrccArchivo) {
        String rrccNombre = rrccArchivo.getName().toLowerCase(Locale.ROOT);
        int rrccPunto = rrccNombre.lastIndexOf('.');
        return rrccPunto < 0 ? "" : rrccNombre.substring(rrccPunto + 1);
    }

}
