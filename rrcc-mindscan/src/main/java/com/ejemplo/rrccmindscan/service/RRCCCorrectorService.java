package com.ejemplo.rrccmindscan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RRCCCorrectorService {

    public static final String RRCC_REGLA_ESPACIOS = "Espacios múltiples";
    public static final String RRCC_REGLA_PUNTUACION = "Espacio antes de signo";
    public static final String RRCC_REGLA_SEPARACION = "Falta espacio después de signo";
    public static final String RRCC_REGLA_REPETIDA = "Palabra repetida";
    public static final String RRCC_REGLA_MAYUSCULA = "Mayúscula inicial";

    private static final Pattern RRCC_ESPACIOS = Pattern.compile("(?<=\\S)[ \\t]{2,}(?=\\S)");
    private static final Pattern RRCC_PUNTUACION = Pattern.compile("(?<=[\\p{L}\\p{N}])[ \\t]+([,;:.?!)])(?![.\\d])");
    private static final Pattern RRCC_SEPARACION = Pattern.compile("(?<=\\p{Ll})([,;])(?=\\p{L})");
    private static final Pattern RRCC_REPETIDA = Pattern.compile("(?iu)(?<![\\p{L}])(\\p{L}{2,})([ \\t]+)\\1(?![\\p{L}])");
    private static final Pattern RRCC_MAYUSCULA = Pattern.compile("(?<![\\d.][.])([.?!][ \\t]+)(\\p{Ll})");

    private final RRCCTextoService rrccTexto;

    public record RRCCCambio(String rrccRegla, String rrccAntes, String rrccDespues) {
    }

    public record RRCCResultadoCorreccion(String rrccOriginal, String rrccCorregido, List<RRCCCambio> rrccCambios,
                                          Map<String, Integer> rrccPorRegla) {
        public boolean rrccHayCambios() {
            return !rrccCambios.isEmpty();
        }
    }

    public RRCCResultadoCorreccion rrccCorregir(String rrccContenido) {
        String rrccTextoActual = rrccContenido == null ? "" : rrccContenido;
        List<RRCCCambio> rrccCambios = new ArrayList<>();
        rrccTextoActual = rrccAplicar(rrccTextoActual, RRCC_REPETIDA, RRCC_REGLA_REPETIDA, rrccM -> rrccM.group(1), rrccCambios);
        rrccTextoActual = rrccAplicar(rrccTextoActual, RRCC_ESPACIOS, RRCC_REGLA_ESPACIOS, rrccM -> " ", rrccCambios);
        rrccTextoActual = rrccAplicar(rrccTextoActual, RRCC_PUNTUACION, RRCC_REGLA_PUNTUACION, rrccM -> rrccM.group(1), rrccCambios);
        rrccTextoActual = rrccAplicar(rrccTextoActual, RRCC_SEPARACION, RRCC_REGLA_SEPARACION, rrccM -> rrccM.group(1) + " ", rrccCambios);
        rrccTextoActual = rrccAplicar(rrccTextoActual, RRCC_MAYUSCULA, RRCC_REGLA_MAYUSCULA,
                rrccM -> rrccM.group(1) + rrccM.group(2).toUpperCase(), rrccCambios);
        Map<String, Integer> rrccPorRegla = new LinkedHashMap<>();
        for (RRCCCambio rrccC : rrccCambios) {
            rrccPorRegla.merge(rrccC.rrccRegla(), 1, Integer::sum);
        }
        return new RRCCResultadoCorreccion(rrccContenido, rrccTextoActual, rrccCambios, rrccPorRegla);
    }

    private String rrccAplicar(String rrccEntrada, Pattern rrccPatron, String rrccRegla, Function<Matcher, String> rrccReemplazo,
                               List<RRCCCambio> rrccCambios) {
        Matcher rrccM = rrccPatron.matcher(rrccEntrada);
        StringBuilder rrccSalida = new StringBuilder();
        while (rrccM.find()) {
            String rrccNuevo = rrccReemplazo.apply(rrccM);
            if (!rrccNuevo.equals(rrccM.group())) {
                rrccCambios.add(new RRCCCambio(rrccRegla, rrccVentana(rrccEntrada, rrccM.start(), rrccM.end(), rrccM.group()),
                        rrccVentana(rrccEntrada, rrccM.start(), rrccM.end(), rrccNuevo)));
            }
            rrccM.appendReplacement(rrccSalida, Matcher.quoteReplacement(rrccNuevo));
        }
        rrccM.appendTail(rrccSalida);
        return rrccSalida.toString();
    }

    private String rrccVentana(String rrccFuente, int rrccIni, int rrccFin, String rrccCentro) {
        int rrccDesde = Math.max(0, rrccIni - 25);
        int rrccHasta = Math.min(rrccFuente.length(), rrccFin + 25);
        String rrccAntes = rrccFuente.substring(rrccDesde, rrccIni);
        String rrccDespues = rrccFuente.substring(rrccFin, rrccHasta);
        return rrccTexto.rrccRecortar(((rrccDesde > 0 ? "..." : "") + rrccAntes + "[" + rrccCentro.replace(' ', '·') + "]" + rrccDespues
                + (rrccHasta < rrccFuente.length() ? "..." : "")).replace('\n', ' '), 90);
    }
}
