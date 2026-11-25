package com.ejemplo.rrccmindscan.ui;

import atlantafx.base.theme.Styles;
import com.ejemplo.rrccmindscan.model.RRCCDocumento;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RRCCLector {

    public static final int RRCC_RADIO = 6000;
    public static final double RRCC_ANCHO = 820;

    private final RRCCNotificador rrccNotificador;

    public void rrccAbrir(RRCCDocumento rrccDoc, int rrccPosicion, int rrccLargo, String rrccMotivo) {
        String rrccContenido = rrccDoc.getRrccContenido();
        int rrccIni = Math.max(0, Math.min(rrccPosicion, rrccContenido.length()));
        int rrccFin = Math.max(rrccIni, Math.min(rrccContenido.length(), rrccIni + Math.max(0, rrccLargo)));
        int rrccDesde = Math.max(0, rrccIni - RRCC_RADIO);
        int rrccHasta = Math.min(rrccContenido.length(), rrccFin + RRCC_RADIO);

        TextFlow rrccFlujo = new TextFlow();
        rrccFlujo.getStyleClass().addAll("rrcc-flujo", "rrcc-lector-texto");
        if (rrccDesde > 0) {
            rrccFlujo.getChildren().add(rrccPieza("[... " + RRCCFormatos.rrccNumero(rrccDesde) + " caracteres antes ...]\n\n", "rrcc-resumen-tenue"));
        }
        rrccFlujo.getChildren().add(rrccPieza(rrccContenido.substring(rrccDesde, rrccIni), "rrcc-texto"));
        Text rrccMarca = rrccPieza(rrccContenido.substring(rrccIni, rrccFin), "rrcc-lector-marca");
        rrccFlujo.getChildren().add(rrccMarca);
        rrccFlujo.getChildren().add(rrccPieza(rrccContenido.substring(rrccFin, rrccHasta), "rrcc-texto"));
        if (rrccHasta < rrccContenido.length()) {
            rrccFlujo.getChildren().add(rrccPieza("\n\n[... continúa ...]", "rrcc-resumen-tenue"));
        }
        VBox rrccHoja = new VBox(rrccFlujo);
        rrccHoja.getStyleClass().add("rrcc-lector-hoja");
        ScrollPane rrccScroll = new ScrollPane(rrccHoja);
        rrccScroll.setFitToWidth(true);
        rrccScroll.getStyleClass().add("rrcc-pagina-scroll");
        VBox.setVgrow(rrccScroll, Priority.ALWAYS);

        Label rrccTitulo = new Label(rrccDoc.getRrccNombre());
        rrccTitulo.getStyleClass().add("rrcc-drawer-titulo");
        rrccTitulo.setWrapText(true);
        Label rrccSub = new Label(rrccMotivo);
        rrccSub.getStyleClass().add("rrcc-drawer-subtitulo");
        rrccSub.setWrapText(true);
        rrccSub.setMinHeight(Region.USE_PREF_SIZE);
        HBox rrccChips = new HBox(6, RRCCFormatos.rrccChipTipo(rrccDoc.getRrccTipo()), RRCCFormatos.rrccChipCategoria(rrccDoc.getRrccCategoria()),
                RRCCFormatos.rrccChip("Posición " + RRCCFormatos.rrccNumero(rrccIni) + " de " + RRCCFormatos.rrccNumero(rrccContenido.length()), "rrcc-chip-suave"));
        rrccChips.setAlignment(Pos.CENTER_LEFT);
        VBox rrccCabecera = new VBox(6, rrccTitulo, rrccSub, rrccChips);
        rrccCabecera.getStyleClass().add("rrcc-drawer-header");

        Button rrccIrMarca = new Button("Ir al fragmento");
        rrccIrMarca.getStyleClass().add(Styles.BUTTON_OUTLINED);
        double rrccRelativo = (rrccIni - rrccDesde) / (double) Math.max(1, rrccHasta - rrccDesde);
        rrccIrMarca.setOnAction(rrccE -> rrccScroll.setVvalue(rrccRelativo));
        Button rrccCerrar = new Button("Cerrar");
        rrccCerrar.getStyleClass().add(Styles.ACCENT);
        rrccCerrar.setOnAction(rrccE -> rrccNotificador.rrccCerrarPanel());
        Region rrccEspacio = new Region();
        HBox.setHgrow(rrccEspacio, Priority.ALWAYS);
        HBox rrccPie = new HBox(10, rrccIrMarca, rrccEspacio, rrccCerrar);
        rrccPie.getStyleClass().add("rrcc-drawer-footer");
        rrccPie.setAlignment(Pos.CENTER_RIGHT);

        VBox rrccCuerpo = new VBox(rrccScroll);
        rrccCuerpo.getStyleClass().add("rrcc-drawer-body");
        VBox.setVgrow(rrccCuerpo, Priority.ALWAYS);
        VBox rrccDrawer = new VBox(rrccCabecera, rrccCuerpo, rrccPie);
        rrccDrawer.getStyleClass().add("rrcc-drawer");
        rrccDrawer.setPrefWidth(RRCC_ANCHO);
        rrccDrawer.setMaxWidth(RRCC_ANCHO);
        rrccDrawer.setMaxHeight(Double.MAX_VALUE);
        rrccNotificador.rrccAbrirPanel(rrccDrawer);
        Platform.runLater(() -> Platform.runLater(() -> rrccScroll.setVvalue(rrccRelativo)));
    }

    private Text rrccPieza(String rrccContenido, String rrccClase) {
        Text rrccT = new Text(rrccContenido);
        rrccT.getStyleClass().addAll("rrcc-texto", rrccClase);
        return rrccT;
    }
}
