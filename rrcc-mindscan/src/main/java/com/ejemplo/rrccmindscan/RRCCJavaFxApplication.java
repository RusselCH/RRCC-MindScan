package com.ejemplo.rrccmindscan;

import atlantafx.base.theme.PrimerDark;
import com.ejemplo.rrccmindscan.controller.RRCCMainController;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Objects;

public class RRCCJavaFxApplication extends Application {

    public static final String RRCC_TITULO = "RRCC MindScan | Analizador de documentos";
    public static final String RRCC_HOJA_ESTILOS = "/css/rrcc-app.css";

    private ConfigurableApplicationContext rrccContexto;
    private Stage rrccSplash;
    private Label rrccLblEstado;

    @Override
    public void start(Stage rrccStage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        rrccMostrarSplash();
        String[] rrccArgs = getParameters().getRaw().toArray(new String[0]);
        Thread rrccHilo = new Thread(() -> {
            try {
                rrccEstado("Iniciando base de datos y motores de análisis...");
                rrccContexto = new SpringApplicationBuilder(RRCCMindScanApplication.class)
                        .web(WebApplicationType.NONE)
                        .headless(false)
                        .run(rrccArgs);
                rrccEstado("Preparando la interfaz...");
                Platform.runLater(() -> rrccAbrirPrincipal(rrccStage));
            } catch (RuntimeException rrccError) {
                Platform.runLater(() -> rrccMostrarError(rrccError));
            }
        }, "rrcc-arranque");
        rrccHilo.setDaemon(true);
        rrccHilo.start();
    }

    private void rrccMostrarSplash() {
        Label rrccMarca = new Label("RRCC MindScan");
        rrccMarca.getStyleClass().add("rrcc-splash-titulo");
        Label rrccLema = new Label("Analiza, compara y revisa documentos sin inteligencia artificial");
        rrccLema.getStyleClass().add("rrcc-splash-lema");
        Label rrccAlgoritmos = new Label("TF-IDF  ·  TextRank  ·  BM25  ·  Levenshtein  ·  LCS  ·  Fernández-Huerta");
        rrccAlgoritmos.getStyleClass().add("rrcc-splash-algoritmos");
        ProgressBar rrccBarra = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
        rrccBarra.setPrefWidth(360);
        rrccBarra.getStyleClass().add("rrcc-splash-barra");
        rrccLblEstado = new Label("Cargando...");
        rrccLblEstado.getStyleClass().add("rrcc-splash-estado");
        Label rrccVersion = new Label("Versión 2.0 - RRCC");
        rrccVersion.getStyleClass().add("rrcc-splash-version");
        VBox rrccCaja = new VBox(10, rrccMarca, rrccLema, rrccAlgoritmos, rrccBarra, rrccLblEstado, rrccVersion);
        rrccCaja.setAlignment(Pos.CENTER);
        rrccCaja.getStyleClass().add("rrcc-splash");
        Scene rrccEscena = new Scene(rrccCaja, 560, 320);
        rrccEscena.getStylesheets().add(Objects.requireNonNull(getClass().getResource(RRCC_HOJA_ESTILOS)).toExternalForm());
        rrccSplash = new Stage(StageStyle.UNDECORATED);
        rrccSplash.setTitle(RRCC_TITULO);
        rrccSplash.setScene(rrccEscena);
        rrccSplash.centerOnScreen();
        rrccSplash.show();
    }

    private void rrccEstado(String rrccMensaje) {
        Platform.runLater(() -> {
            if (rrccLblEstado != null) {
                rrccLblEstado.setText(rrccMensaje);
            }
        });
    }

    private void rrccAbrirPrincipal(Stage rrccStage) {
        try {
            FXMLLoader rrccLoader = new FXMLLoader(getClass().getResource("/view/rrcc-main-view.fxml"));
            rrccLoader.setControllerFactory(rrccContexto::getBean);
            rrccLoader.setClassLoader(getClass().getClassLoader());
            Parent rrccRaiz = rrccLoader.load();
            RRCCMainController rrccMain = rrccLoader.getController();
            Scene rrccEscena = new Scene(rrccRaiz);
            rrccEscena.getStylesheets().add(Objects.requireNonNull(getClass().getResource(RRCC_HOJA_ESTILOS)).toExternalForm());
            rrccStage.setTitle(RRCC_TITULO);
            rrccStage.setScene(rrccEscena);
            rrccStage.setMinWidth(1240);
            rrccStage.setMinHeight(760);
            rrccStage.setMaximized(true);
            rrccMain.rrccIniciar();
            rrccStage.show();
            FadeTransition rrccSalida = new FadeTransition(Duration.millis(250), rrccSplash.getScene().getRoot());
            rrccSalida.setToValue(0);
            rrccSalida.setOnFinished(rrccE -> rrccSplash.close());
            rrccSalida.play();
        } catch (Exception rrccError) {
            rrccMostrarError(rrccError);
        }
    }

    private void rrccMostrarError(Throwable rrccError) {
        rrccLblEstado.setText("No se pudo iniciar: " + rrccError.getMessage());
        rrccLblEstado.setWrapText(true);
        rrccLblEstado.getStyleClass().add("rrcc-splash-error");
        rrccError.printStackTrace();
    }

    @Override
    public void stop() {
        if (rrccContexto != null) {
            rrccContexto.close();
        }
        Platform.exit();
        System.exit(0);
    }
}
