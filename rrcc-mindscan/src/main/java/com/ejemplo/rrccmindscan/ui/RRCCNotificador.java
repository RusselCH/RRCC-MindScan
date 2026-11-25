package com.ejemplo.rrccmindscan.ui;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.controls.Notification;
import atlantafx.base.theme.Styles;
import atlantafx.base.util.Animations;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.springframework.stereotype.Component;

@Component
public class RRCCNotificador {

    public static final Duration RRCC_DURACION_TOAST = Duration.seconds(4);
    public static final String RRCC_CERRANDO = "rrccCerrando";

    private ModalPane rrccModalPanel;
    private ModalPane rrccModalDialogo;
    private VBox rrccZonaToasts;

    public void rrccRegistrar(ModalPane rrccModalPanel, ModalPane rrccModalDialogo, VBox rrccZonaToasts) {
        this.rrccModalPanel = rrccModalPanel;
        this.rrccModalDialogo = rrccModalDialogo;
        this.rrccZonaToasts = rrccZonaToasts;
    }

    public void rrccExito(String rrccMensaje) {
        rrccToast(rrccMensaje, Styles.SUCCESS);
    }

    public void rrccAdvertencia(String rrccMensaje) {
        rrccToast(rrccMensaje, Styles.WARNING);
    }

    public void rrccError(String rrccMensaje) {
        rrccToast(rrccMensaje, Styles.DANGER);
    }

    public void rrccInfo(String rrccMensaje) {
        rrccToast(rrccMensaje, Styles.ACCENT);
    }

    private void rrccToast(String rrccMensaje, String rrccEstilo) {
        Notification rrccN = new Notification(rrccMensaje);
        rrccN.getStyleClass().addAll(rrccEstilo, Styles.ELEVATED_2, "rrcc-toast");
        rrccN.setPrefWidth(380);
        rrccN.setMaxWidth(380);
        rrccN.setOnClose(rrccE -> rrccCerrarToast(rrccN));
        rrccZonaToasts.getChildren().add(rrccN);
        Animations.fadeInUp(rrccN, Duration.millis(280)).play();
        PauseTransition rrccEspera = new PauseTransition(RRCC_DURACION_TOAST);
        rrccEspera.setOnFinished(rrccE -> rrccCerrarToast(rrccN));
        rrccEspera.play();
    }

    private void rrccCerrarToast(Notification rrccN) {
        if (!rrccZonaToasts.getChildren().contains(rrccN) || rrccN.getProperties().containsKey(RRCC_CERRANDO)) {
            return;
        }
        rrccN.getProperties().put(RRCC_CERRANDO, true);
        Timeline rrccSalida = Animations.fadeOutRight(rrccN, Duration.millis(250));
        rrccSalida.setOnFinished(rrccE -> rrccZonaToasts.getChildren().remove(rrccN));
        rrccSalida.play();
    }

    public void rrccConfirmar(String rrccTitulo, String rrccMensaje, String rrccTextoAccion, Runnable rrccAlConfirmar) {
        Label rrccLblTitulo = new Label(rrccTitulo);
        rrccLblTitulo.getStyleClass().add("rrcc-dialogo-titulo");
        rrccLblTitulo.setWrapText(true);
        Label rrccLblMensaje = new Label(rrccMensaje);
        rrccLblMensaje.getStyleClass().add("rrcc-dialogo-mensaje");
        rrccLblMensaje.setWrapText(true);

        Button rrccCancelar = new Button("Cancelar");
        rrccCancelar.getStyleClass().add(Styles.BUTTON_OUTLINED);
        rrccCancelar.setCancelButton(true);
        rrccCancelar.setOnAction(rrccE -> rrccModalDialogo.hide());

        Button rrccAceptar = new Button(rrccTextoAccion);
        rrccAceptar.getStyleClass().add(Styles.DANGER);
        rrccAceptar.setOnAction(rrccE -> {
            rrccModalDialogo.hide();
            rrccAlConfirmar.run();
        });

        Region rrccEspacio = new Region();
        HBox.setHgrow(rrccEspacio, Priority.ALWAYS);
        HBox rrccAcciones = new HBox(10, rrccEspacio, rrccCancelar, rrccAceptar);
        rrccAcciones.setAlignment(Pos.CENTER_RIGHT);

        VBox rrccDialogo = new VBox(14, rrccLblTitulo, rrccLblMensaje, rrccAcciones);
        rrccDialogo.getStyleClass().add("rrcc-dialogo");
        rrccDialogo.setMaxSize(460, Region.USE_PREF_SIZE);

        rrccModalDialogo.usePredefinedTransitionFactories(null);
        rrccModalDialogo.show(rrccDialogo);
        rrccAceptar.requestFocus();
    }

    public void rrccAbrirPanel(Node rrccContenido) {
        rrccModalPanel.usePredefinedTransitionFactories(Side.RIGHT, Duration.millis(320), Duration.millis(220));
        rrccModalPanel.show(rrccContenido);
    }

    public void rrccCerrarPanel() {
        rrccModalPanel.hide();
    }
}
