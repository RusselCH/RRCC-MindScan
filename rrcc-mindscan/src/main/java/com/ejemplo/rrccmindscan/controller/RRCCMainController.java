package com.ejemplo.rrccmindscan.controller;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.util.Animations;
import com.ejemplo.rrccmindscan.service.RRCCDocumentoService;
import com.ejemplo.rrccmindscan.ui.RRCCFormatos;
import com.ejemplo.rrccmindscan.ui.RRCCNotificador;
import com.ejemplo.rrccmindscan.ui.RRCCVista;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RRCCMainController {

    public static final String RRCC_VISTA_PANEL = "rrcc-panel-view";
    public static final String RRCC_VISTA_BIBLIOTECA = "rrcc-biblioteca-view";
    public static final String RRCC_VISTA_ANALISIS = "rrcc-analisis-view";
    public static final String RRCC_VISTA_BUSQUEDA = "rrcc-busqueda-view";
    public static final String RRCC_VISTA_COMPARADOR = "rrcc-comparador-view";
    public static final String RRCC_VISTA_REVISION = "rrcc-revision-view";
    public static final String RRCC_VISTA_HISTORIAL = "rrcc-historial-view";

    private final ApplicationContext rrccContexto;
    private final RRCCNotificador rrccNotificador;
    private final RRCCDocumentoService rrccDocumentos;

    @FXML private StackPane rrccRaiz;
    @FXML private StackPane rrccContenido;
    @FXML private ModalPane rrccModalPanel;
    @FXML private ModalPane rrccModalDialogo;
    @FXML private VBox rrccZonaNotificaciones;
    @FXML private ToggleGroup rrccMenu;
    @FXML private ToggleButton rrccNavPanel;
    @FXML private ToggleButton rrccNavBiblioteca;
    @FXML private ToggleButton rrccNavAnalisis;
    @FXML private ToggleButton rrccNavBusqueda;
    @FXML private ToggleButton rrccNavComparador;
    @FXML private ToggleButton rrccNavRevision;
    @FXML private ToggleButton rrccNavHistorial;
    @FXML private Region rrccIndicador;
    @FXML private Button rrccBtnTema;
    @FXML private Label rrccLblBiblioteca;

    private final Map<String, RRCCVistaCargada> rrccVistas = new HashMap<>();
    private boolean rrccOscuro = true;

    private record RRCCVistaCargada(Parent rrccNodo, RRCCVista rrccControlador) {
    }

    public void rrccIniciar() {
        rrccNotificador.rrccRegistrar(rrccModalPanel, rrccModalDialogo, rrccZonaNotificaciones);
        rrccModalPanel.setAlignment(Pos.TOP_RIGHT);
        rrccModalDialogo.setAlignment(Pos.CENTER);
        rrccActualizarPie();

        rrccNavPanel.setUserData(RRCC_VISTA_PANEL);
        rrccNavBiblioteca.setUserData(RRCC_VISTA_BIBLIOTECA);
        rrccNavAnalisis.setUserData(RRCC_VISTA_ANALISIS);
        rrccNavBusqueda.setUserData(RRCC_VISTA_BUSQUEDA);
        rrccNavComparador.setUserData(RRCC_VISTA_COMPARADOR);
        rrccNavRevision.setUserData(RRCC_VISTA_REVISION);
        rrccNavHistorial.setUserData(RRCC_VISTA_HISTORIAL);

        rrccMenu.selectedToggleProperty().addListener((rrccObs, rrccAnterior, rrccActual) -> {
            if (rrccActual == null) {
                rrccMenu.selectToggle(rrccAnterior);
                return;
            }
            rrccMostrar((String) rrccActual.getUserData());
            rrccMoverIndicador(rrccActual);
        });
        rrccMenu.selectToggle(rrccNavPanel);
        Platform.runLater(() -> rrccMoverIndicador(rrccNavPanel));
        rrccRegistrarAtajos();
    }

    private void rrccRegistrarAtajos() {
        if (rrccRaiz.getScene() == null) {
            return;
        }
        List<ToggleButton> rrccBotones = rrccBotonesMenu();
        for (int rrccI = 0; rrccI < rrccBotones.size(); rrccI++) {
            ToggleButton rrccB = rrccBotones.get(rrccI);
            rrccRaiz.getScene().getAccelerators().put(KeyCombination.keyCombination("Shortcut+" + (rrccI + 1)),
                    () -> rrccMenu.selectToggle(rrccB));
            Tooltip rrccAyuda = new Tooltip("Ctrl+" + (rrccI + 1));
            rrccB.setTooltip(rrccAyuda);
        }
        rrccRaiz.getScene().getAccelerators().put(KeyCombination.keyCombination("Shortcut+F"),
                () -> rrccIrA(RRCC_VISTA_BUSQUEDA));
        rrccRaiz.getScene().getAccelerators().put(KeyCombination.keyCombination("Shortcut+T"), this::rrccOnCambiarTema);
    }

    public void rrccIrA(String rrccVista) {
        RRCCVistaCargada rrccCargada = rrccVistas.get(rrccVista);
        Toggle rrccActual = rrccMenu.getSelectedToggle();
        if (rrccActual != null && rrccVista.equals(rrccActual.getUserData()) && rrccCargada != null) {
            rrccCargada.rrccControlador().rrccRefrescar();
            return;
        }
        rrccBotonesMenu().stream()
                .filter(rrccB -> rrccVista.equals(rrccB.getUserData()))
                .findFirst()
                .ifPresent(rrccMenu::selectToggle);
    }

    public void rrccActualizarPie() {
        long rrccTotal = rrccDocumentos.rrccContar();
        rrccLblBiblioteca.setText(rrccTotal == 1 ? "1 documento en la biblioteca"
                : RRCCFormatos.rrccNumero(rrccTotal) + " documentos en la biblioteca");
    }

    private void rrccMostrar(String rrccNombre) {
        try {
            RRCCVistaCargada rrccVista = rrccVistas.computeIfAbsent(rrccNombre, this::rrccCargar);
            rrccContenido.getChildren().setAll(rrccVista.rrccNodo());
            rrccVista.rrccControlador().rrccRefrescar();
            Animations.fadeInUp(rrccVista.rrccNodo(), Duration.millis(360)).play();
        } catch (RuntimeException rrccError) {
            rrccNotificador.rrccError("No se pudo abrir la vista: " + rrccError.getMessage());
        }
    }

    private RRCCVistaCargada rrccCargar(String rrccNombre) {
        FXMLLoader rrccLoader = new FXMLLoader(getClass().getResource("/view/" + rrccNombre + ".fxml"));
        rrccLoader.setControllerFactory(rrccContexto::getBean);
        rrccLoader.setClassLoader(getClass().getClassLoader());
        try {
            Parent rrccNodo = rrccLoader.load();
            RRCCVista rrccControlador = rrccLoader.getController();
            rrccControlador.rrccIniciar();
            return new RRCCVistaCargada(rrccNodo, rrccControlador);
        } catch (IOException rrccError) {
            throw new UncheckedIOException("No se pudo cargar " + rrccNombre, rrccError);
        }
    }

    private void rrccMoverIndicador(Toggle rrccToggle) {
        Node rrccBoton = (Node) rrccToggle;
        double rrccDestino = rrccBoton.getBoundsInParent().getMinY();
        rrccIndicador.setPrefHeight(rrccBoton.getBoundsInParent().getHeight());
        new Timeline(new KeyFrame(Duration.millis(280),
                new KeyValue(rrccIndicador.translateYProperty(), rrccDestino, Interpolator.SPLINE(0.4, 0, 0.2, 1))))
                .play();
    }

    @FXML
    private void rrccOnCambiarTema() {
        rrccOscuro = !rrccOscuro;
        Application.setUserAgentStylesheet(rrccOscuro
                ? new PrimerDark().getUserAgentStylesheet()
                : new PrimerLight().getUserAgentStylesheet());
        if (rrccOscuro) {
            rrccRaiz.getStyleClass().add("dark");
        } else {
            rrccRaiz.getStyleClass().remove("dark");
        }
        rrccBtnTema.setText(rrccOscuro ? "Modo claro" : "Modo oscuro");
    }

    private List<ToggleButton> rrccBotonesMenu() {
        return List.of(rrccNavPanel, rrccNavBiblioteca, rrccNavAnalisis, rrccNavBusqueda, rrccNavComparador, rrccNavRevision, rrccNavHistorial);
    }
}
