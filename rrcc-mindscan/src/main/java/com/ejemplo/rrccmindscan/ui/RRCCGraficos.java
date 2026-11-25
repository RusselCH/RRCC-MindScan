package com.ejemplo.rrccmindscan.ui;

import com.ejemplo.rrccmindscan.service.RRCCAnalizadorService.RRCCTermino;
import com.ejemplo.rrccmindscan.service.RRCCComparadorService.RRCCFragmentoDiff;
import com.ejemplo.rrccmindscan.service.RRCCTextoService;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RRCCGraficos {

    public static final Pattern RRCC_PATRON_PIEZAS = Pattern.compile("[\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+");
    public static final double RRCC_FUENTE_MINIMA = 13;
    public static final double RRCC_FUENTE_MAXIMA = 38;

    private RRCCGraficos() {
    }

    public static StackPane rrccAnillo(double rrccFraccion, String rrccTexto, String rrccSubtexto, String rrccClase,
                                       double rrccRadio) {
        double rrccCentro = rrccRadio + 12;
        Arc rrccFondo = new Arc(rrccCentro, rrccCentro, rrccRadio, rrccRadio, 90, -360);
        rrccFondo.setType(ArcType.OPEN);
        rrccFondo.getStyleClass().add("rrcc-anillo-fondo");
        double rrccLimitado = Math.max(0, Math.min(1, rrccFraccion));
        Arc rrccValor = new Arc(rrccCentro, rrccCentro, rrccRadio, rrccRadio, 90, -360 * rrccLimitado);
        rrccValor.setType(ArcType.OPEN);
        rrccValor.getStyleClass().addAll("rrcc-anillo-valor", rrccClase);
        Label rrccPrincipal = new Label(rrccTexto);
        rrccPrincipal.getStyleClass().add("rrcc-anillo-texto");
        Label rrccSecundario = new Label(rrccSubtexto);
        rrccSecundario.getStyleClass().add("rrcc-anillo-subtexto");
        VBox rrccTextos = new VBox(0, rrccPrincipal, rrccSecundario);
        rrccTextos.setAlignment(Pos.CENTER);
        Pane rrccLienzo = new Pane(rrccFondo, rrccValor);
        rrccLienzo.setMinSize(rrccCentro * 2, rrccCentro * 2);
        rrccLienzo.setPrefSize(rrccCentro * 2, rrccCentro * 2);
        rrccLienzo.setMaxSize(rrccCentro * 2, rrccCentro * 2);
        StackPane rrccAnillo = new StackPane(rrccLienzo, rrccTextos);
        rrccAnillo.setMinSize(rrccRadio * 2 + 24, rrccRadio * 2 + 24);
        rrccAnillo.setPrefSize(rrccRadio * 2 + 24, rrccRadio * 2 + 24);
        return rrccAnillo;
    }

    public static <X> XYChart.Data<X, Number> rrccBarra(X rrccCategoria, Number rrccValor, String rrccClase, String rrccAyuda) {
        XYChart.Data<X, Number> rrccDato = new XYChart.Data<>(rrccCategoria, rrccValor);
        rrccDato.setExtraValue(new String[]{rrccClase, rrccAyuda});
        return rrccDato;
    }

    public static <X> void rrccPintar(XYChart.Series<X, Number> rrccSerie) {
        for (XYChart.Data<X, Number> rrccDato : rrccSerie.getData()) {
            if (!(rrccDato.getExtraValue() instanceof String[] rrccInfo)) {
                continue;
            }
            if (rrccDato.getNode() != null) {
                rrccAplicar(rrccDato.getNode(), rrccInfo);
            }
            rrccDato.nodeProperty().addListener((rrccO, rrccA, rrccNuevo) -> {
                if (rrccNuevo != null) {
                    rrccAplicar(rrccNuevo, rrccInfo);
                }
            });
        }
    }

    private static void rrccAplicar(Node rrccNodo, String[] rrccInfo) {
        if (!rrccNodo.getStyleClass().contains(rrccInfo[0])) {
            rrccNodo.getStyleClass().add(rrccInfo[0]);
        }
        if (rrccInfo[1] != null) {
            Tooltip.install(rrccNodo, new Tooltip(rrccInfo[1]));
        }
    }

    public static FlowPane rrccNube(List<RRCCTermino> rrccTerminos, Consumer<RRCCTermino> rrccAlClic) {
        FlowPane rrccNube = new FlowPane(10, 6);
        rrccNube.getStyleClass().add("rrcc-nube");
        rrccNube.setAlignment(Pos.CENTER);
        if (rrccTerminos.isEmpty()) {
            return rrccNube;
        }
        double rrccMaximo = rrccTerminos.stream().mapToDouble(RRCCTermino::rrccPeso).max().orElse(1);
        double rrccMinimo = rrccTerminos.stream().mapToDouble(RRCCTermino::rrccPeso).min().orElse(0);
        List<RRCCTermino> rrccMezclados = new java.util.ArrayList<>(rrccTerminos);
        java.util.Collections.shuffle(rrccMezclados, new java.util.Random(rrccTerminos.size() * 31L));
        for (RRCCTermino rrccT : rrccMezclados) {
            double rrccRelativo = rrccMaximo == rrccMinimo ? 1 : (rrccT.rrccPeso() - rrccMinimo) / (rrccMaximo - rrccMinimo);
            Label rrccPalabra = new Label(rrccT.rrccForma());
            int rrccNivel = (int) Math.min(4, Math.floor(rrccRelativo * 5));
            rrccPalabra.getStyleClass().addAll("rrcc-nube-palabra", "rrcc-nube-" + rrccNivel);
            rrccPalabra.setStyle("-fx-font-size: " + Math.round(RRCC_FUENTE_MINIMA + rrccRelativo
                    * (RRCC_FUENTE_MAXIMA - RRCC_FUENTE_MINIMA)) + "px;");
            Tooltip.install(rrccPalabra, new Tooltip(rrccT.rrccForma() + "\nAparece " + rrccT.rrccFrecuencia()
                    + " veces\nPeso TF-IDF: " + String.format("%.3f", rrccT.rrccPeso())
                    + (rrccAlClic != null ? "\nClic para buscarla en todos los documentos" : "")));
            if (rrccAlClic != null) {
                rrccPalabra.setOnMouseClicked(rrccE -> rrccAlClic.accept(rrccT));
            }
            rrccNube.getChildren().add(rrccPalabra);
        }
        return rrccNube;
    }

    public static TextFlow rrccResaltar(String rrccContenido, Set<String> rrccClaves, RRCCTextoService rrccTexto) {
        TextFlow rrccFlujo = new TextFlow();
        rrccFlujo.getStyleClass().add("rrcc-flujo");
        Matcher rrccM = RRCC_PATRON_PIEZAS.matcher(rrccContenido == null ? "" : rrccContenido);
        StringBuilder rrccNormal = new StringBuilder();
        while (rrccM.find()) {
            String rrccPieza = rrccM.group();
            boolean rrccEsPalabra = Character.isLetterOrDigit(rrccPieza.charAt(0));
            if (rrccEsPalabra && rrccClaves.contains(rrccTexto.rrccClave(rrccPieza))) {
                rrccVaciar(rrccFlujo, rrccNormal);
                Text rrccDestacado = new Text(rrccPieza);
                rrccDestacado.getStyleClass().addAll("rrcc-texto", "rrcc-resaltado");
                rrccFlujo.getChildren().add(rrccDestacado);
            } else {
                rrccNormal.append(rrccPieza);
            }
        }
        rrccVaciar(rrccFlujo, rrccNormal);
        return rrccFlujo;
    }

    public static TextFlow rrccResaltarFrase(String rrccContenido, String rrccFraseNormalizada, RRCCTextoService rrccTexto) {
        TextFlow rrccFlujo = new TextFlow();
        rrccFlujo.getStyleClass().add("rrcc-flujo");
        String rrccNormalizado = rrccTexto.rrccNormalizar(rrccContenido);
        if (rrccFraseNormalizada == null || rrccFraseNormalizada.isBlank() || rrccNormalizado.length() != rrccContenido.length()) {
            rrccFlujo.getChildren().add(rrccTextoSimple(rrccContenido));
            return rrccFlujo;
        }
        int rrccDesde = 0;
        int rrccIndice = rrccNormalizado.indexOf(rrccFraseNormalizada);
        while (rrccIndice >= 0) {
            if (rrccIndice > rrccDesde) {
                rrccFlujo.getChildren().add(rrccTextoSimple(rrccContenido.substring(rrccDesde, rrccIndice)));
            }
            Text rrccDestacado = new Text(rrccContenido.substring(rrccIndice, rrccIndice + rrccFraseNormalizada.length()));
            rrccDestacado.getStyleClass().addAll("rrcc-texto", "rrcc-resaltado");
            rrccFlujo.getChildren().add(rrccDestacado);
            rrccDesde = rrccIndice + rrccFraseNormalizada.length();
            rrccIndice = rrccNormalizado.indexOf(rrccFraseNormalizada, rrccDesde);
        }
        if (rrccDesde < rrccContenido.length()) {
            rrccFlujo.getChildren().add(rrccTextoSimple(rrccContenido.substring(rrccDesde)));
        }
        return rrccFlujo;
    }

    public static TextFlow rrccDiff(List<RRCCFragmentoDiff> rrccFragmentos) {
        TextFlow rrccFlujo = new TextFlow();
        rrccFlujo.getStyleClass().add("rrcc-flujo");
        for (RRCCFragmentoDiff rrccF : rrccFragmentos) {
            Text rrccT = new Text(rrccF.rrccTexto() + " ");
            rrccT.getStyleClass().addAll("rrcc-texto", rrccF.rrccTipo().rrccClaseCss());
            rrccFlujo.getChildren().add(rrccT);
        }
        return rrccFlujo;
    }

    private static void rrccVaciar(TextFlow rrccFlujo, StringBuilder rrccNormal) {
        if (!rrccNormal.isEmpty()) {
            rrccFlujo.getChildren().add(rrccTextoSimple(rrccNormal.toString()));
            rrccNormal.setLength(0);
        }
    }

    public static Text rrccTextoSimple(String rrccContenido) {
        Text rrccT = new Text(rrccContenido);
        rrccT.getStyleClass().add("rrcc-texto");
        return rrccT;
    }

    public static HBox rrccMedidor(double rrccFraccion, String rrccClase, String rrccEtiqueta) {
        Region rrccRelleno = new Region();
        rrccRelleno.getStyleClass().addAll("rrcc-medidor-relleno", rrccClase);
        Region rrccFondo = new Region();
        rrccFondo.getStyleClass().add("rrcc-medidor-fondo");
        StackPane rrccPista = new StackPane(rrccFondo, rrccRelleno);
        rrccPista.setAlignment(Pos.CENTER_LEFT);
        rrccPista.setPrefHeight(8);
        rrccPista.setMaxHeight(8);
        rrccPista.setPrefWidth(160);
        rrccRelleno.maxWidthProperty().bind(rrccPista.widthProperty().multiply(Math.max(0.02, Math.min(1, rrccFraccion))));
        Label rrccTexto = new Label(rrccEtiqueta);
        rrccTexto.getStyleClass().add("rrcc-medidor-texto");
        rrccTexto.setMinWidth(Region.USE_PREF_SIZE);
        HBox rrccCaja = new HBox(8, rrccPista, rrccTexto);
        rrccCaja.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(rrccPista, Priority.ALWAYS);
        return rrccCaja;
    }

    public static VBox rrccVacio(String rrccTitulo, String rrccDetalle) {
        Label rrccT = new Label(rrccTitulo);
        rrccT.getStyleClass().add("rrcc-vacio-titulo");
        Label rrccD = new Label(rrccDetalle);
        rrccD.getStyleClass().add("rrcc-texto-suave");
        rrccD.setWrapText(true);
        VBox rrccCaja = new VBox(6, rrccT, rrccD);
        rrccCaja.setAlignment(Pos.CENTER);
        rrccCaja.getStyleClass().add("rrcc-vacio-caja");
        return rrccCaja;
    }
}
