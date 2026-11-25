package com.ejemplo.rrccmindscan.service;

import com.ejemplo.rrccmindscan.config.RRCCAnalisisPropiedades;
import com.ejemplo.rrccmindscan.model.RRCCSeveridad;
import com.ejemplo.rrccmindscan.model.RRCCTipoHallazgo;
import com.ejemplo.rrccmindscan.service.RRCCTextoService.RRCCOracion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RRCCRevisorService {

    public static final int RRCC_MAX_POR_TIPO = 150;
    public static final int RRCC_MAX_ORACIONES_CRUCE = 700;
    public static final int RRCC_CONTEXTO = 70;
    public static final double RRCC_UMBRAL_CIFRAS = 0.3;

    public static final Set<String> RRCC_NEGACIONES = Set.of(
            "no", "nunca", "jamas", "ningun", "ninguna", "ninguno", "tampoco", "nadie", "nada", "sin");

    public static final String[][] RRCC_ANTONIMOS = {
            {"obligatorio", "opcional"}, {"obligatoria", "opcional"}, {"permitido", "prohibido"},
            {"permitida", "prohibida"}, {"aprobado", "rechazado"}, {"aprobada", "rechazada"},
            {"aumento", "disminuyo"}, {"aumentaron", "disminuyeron"}, {"incluye", "excluye"},
            {"mayor", "menor"}, {"superior", "inferior"}, {"activo", "inactivo"}, {"habilitado", "deshabilitado"},
            {"cumple", "incumple"}, {"suficiente", "insuficiente"}, {"posible", "imposible"},
            {"valido", "invalido"}, {"completo", "incompleto"}, {"correcto", "incorrecto"}};

    public static final Map<String, Integer> RRCC_MESES = Map.ofEntries(
            Map.entry("enero", 1), Map.entry("febrero", 2), Map.entry("marzo", 3), Map.entry("abril", 4),
            Map.entry("mayo", 5), Map.entry("junio", 6), Map.entry("julio", 7), Map.entry("agosto", 8),
            Map.entry("septiembre", 9), Map.entry("setiembre", 9), Map.entry("octubre", 10),
            Map.entry("noviembre", 11), Map.entry("diciembre", 12));

    private static final Pattern RRCC_PATRON_ESPACIOS = Pattern.compile("(?<=\\S) {2,}(?=\\S)");
    private static final Pattern RRCC_PATRON_PUNTUACION = Pattern.compile("(?<=[\\p{L}\\p{N}]) +[,;:.?!)](?![.\\d])");
    private static final Pattern RRCC_PATRON_FECHA_NUMERICA = Pattern.compile("(?<![\\d/.-])(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})(?![\\d/-])");
    private static final Pattern RRCC_PATRON_FECHA_TEXTO = Pattern.compile("(?iu)(?<!\\d)(\\d{1,2})\\s+de\\s+(\\p{L}+)\\s+(?:de|del)\\s+(\\d{4})");
    private static final Pattern RRCC_PATRON_VINETA = Pattern.compile("(?<![\\p{L}\\p{N}])\\p{Alnum}{1,2}\\)");
    private static final Pattern RRCC_PATRON_CIFRA = Pattern.compile("(?<![\\d/.,:-])(\\d+(?:[.,]\\d+)?)\\s+(?:de\\s+)?(\\p{L}+)");

    private final RRCCTextoService rrccTexto;
    private final RRCCAnalisisPropiedades rrccPropiedades;

    public record RRCCHallazgo(RRCCTipoHallazgo rrccTipo, String rrccDescripcion, String rrccFragmento,
                               int rrccPosicion, int rrccLargo, String rrccSugerencia) {
        public RRCCSeveridad rrccSeveridad() {
            return rrccTipo.rrccSeveridad();
        }
    }

    public record RRCCResultadoRevision(int rrccPuntaje, List<RRCCHallazgo> rrccHallazgos, int rrccErrores,
                                        int rrccAdvertencias, int rrccSugerencias,
                                        Map<RRCCTipoHallazgo, Integer> rrccPorTipo, int rrccPalabras) {
        public String rrccCalificacion() {
            if (rrccPuntaje >= 90) {
                return "Excelente";
            }
            if (rrccPuntaje >= 75) {
                return "Bueno";
            }
            if (rrccPuntaje >= 55) {
                return "Regular";
            }
            return "Requiere revisión";
        }
    }

    public RRCCResultadoRevision rrccRevisar(String rrccContenido) {
        String rrccT = rrccContenido == null ? "" : rrccContenido;
        List<RRCCHallazgo> rrccLista = new ArrayList<>();
        List<RRCCOracion> rrccOraciones = rrccTexto.rrccOraciones(rrccT);
        rrccPalabrasRepetidas(rrccT, rrccLista);
        rrccPatronSimple(rrccT, RRCC_PATRON_ESPACIOS, RRCCTipoHallazgo.RRCC_ESPACIOS,
                "Hay más de un espacio entre palabras", "Dejar un solo espacio", rrccLista);
        rrccPatronSimple(rrccT, RRCC_PATRON_PUNTUACION, RRCCTipoHallazgo.RRCC_PUNTUACION,
                "Espacio antes de un signo de puntuación", "Pegar el signo a la palabra anterior", rrccLista);
        rrccMayusculas(rrccT, rrccOraciones, rrccLista);
        rrccOracionesLargas(rrccOraciones, rrccLista);
        rrccSignos(rrccT, rrccLista);
        rrccFechas(rrccT, rrccLista);
        rrccParrafosDuplicados(rrccT, rrccLista);
        rrccContradicciones(rrccOraciones, rrccLista);
        rrccCifras(rrccOraciones, rrccLista);

        rrccLista.sort(Comparator.comparingInt((RRCCHallazgo rrccH) -> -rrccH.rrccSeveridad().rrccPeso())
                .thenComparingInt(RRCCHallazgo::rrccPosicion));
        Map<RRCCTipoHallazgo, Integer> rrccPorTipo = new EnumMap<>(RRCCTipoHallazgo.class);
        int rrccErrores = 0;
        int rrccAdvertencias = 0;
        int rrccSugerencias = 0;
        int rrccPenalidad = 0;
        for (RRCCHallazgo rrccH : rrccLista) {
            rrccPorTipo.merge(rrccH.rrccTipo(), 1, Integer::sum);
            rrccPenalidad += rrccH.rrccSeveridad().rrccPeso();
            switch (rrccH.rrccSeveridad()) {
                case RRCC_ERROR -> rrccErrores++;
                case RRCC_ADVERTENCIA -> rrccAdvertencias++;
                case RRCC_SUGERENCIA -> rrccSugerencias++;
            }
        }
        int rrccPalabras = rrccTexto.rrccPalabras(rrccT).size();
        double rrccDescuento = Math.min(100, rrccPenalidad * 300.0 / Math.max(rrccPalabras, 300));
        int rrccPuntaje = (int) Math.round(100 - rrccDescuento);
        return new RRCCResultadoRevision(rrccPuntaje, rrccLista, rrccErrores, rrccAdvertencias, rrccSugerencias,
                rrccPorTipo, rrccPalabras);
    }

    private boolean rrccAgregar(List<RRCCHallazgo> rrccLista, RRCCHallazgo rrccNuevo) {
        long rrccDelTipo = rrccLista.stream().filter(rrccH -> rrccH.rrccTipo() == rrccNuevo.rrccTipo()).count();
        if (rrccDelTipo >= RRCC_MAX_POR_TIPO) {
            return false;
        }
        rrccLista.add(rrccNuevo);
        return true;
    }

    private String rrccContexto(String rrccT, int rrccIni, int rrccFin) {
        int rrccDesde = Math.max(0, rrccIni - RRCC_CONTEXTO / 2);
        int rrccHasta = Math.min(rrccT.length(), rrccFin + RRCC_CONTEXTO / 2);
        while (rrccDesde > 0 && !Character.isWhitespace(rrccT.charAt(rrccDesde - 1)) && rrccIni - rrccDesde < RRCC_CONTEXTO) {
            rrccDesde--;
        }
        while (rrccHasta < rrccT.length() && !Character.isWhitespace(rrccT.charAt(rrccHasta)) && rrccHasta - rrccFin < RRCC_CONTEXTO) {
            rrccHasta++;
        }
        return (rrccDesde > 0 ? "..." : "") + rrccT.substring(rrccDesde, rrccHasta).replace('\n', ' ')
                + (rrccHasta < rrccT.length() ? "..." : "");
    }

    private void rrccPalabrasRepetidas(String rrccT, List<RRCCHallazgo> rrccLista) {
        Matcher rrccM = RRCCTextoService.RRCC_PATRON_PALABRA.matcher(rrccT);
        int rrccFinAnterior = -1;
        int rrccIniAnterior = -1;
        String rrccAnterior = null;
        while (rrccM.find()) {
            String rrccActual = rrccTexto.rrccNormalizar(rrccM.group());
            if (rrccAnterior != null && rrccAnterior.equals(rrccActual) && rrccActual.length() > 1
                    && rrccT.substring(rrccFinAnterior, rrccM.start()).matches("[ \\t]+")) {
                rrccAgregar(rrccLista, new RRCCHallazgo(RRCCTipoHallazgo.RRCC_PALABRA_REPETIDA,
                        "La palabra \"" + rrccM.group() + "\" aparece dos veces seguidas",
                        rrccContexto(rrccT, rrccIniAnterior, rrccM.end()), rrccIniAnterior, rrccM.end() - rrccIniAnterior,
                        "Eliminar la repetición"));
            }
            rrccAnterior = rrccActual;
            rrccIniAnterior = rrccM.start();
            rrccFinAnterior = rrccM.end();
        }
    }

    private void rrccPatronSimple(String rrccT, Pattern rrccPatron, RRCCTipoHallazgo rrccTipo, String rrccDescripcion,
                                  String rrccSugerencia, List<RRCCHallazgo> rrccLista) {
        Matcher rrccM = rrccPatron.matcher(rrccT);
        while (rrccM.find()) {
            if (!rrccAgregar(rrccLista, new RRCCHallazgo(rrccTipo, rrccDescripcion,
                    rrccContexto(rrccT, rrccM.start(), rrccM.end()), rrccM.start(), rrccM.end() - rrccM.start(), rrccSugerencia))) {
                return;
            }
        }
    }

    private void rrccMayusculas(String rrccT, List<RRCCOracion> rrccOraciones, List<RRCCHallazgo> rrccLista) {
        for (RRCCOracion rrccO : rrccOraciones) {
            int rrccPrevio = rrccO.rrccInicio() - 1;
            while (rrccPrevio >= 0 && Character.isWhitespace(rrccT.charAt(rrccPrevio))) {
                rrccPrevio--;
            }
            if (rrccPrevio < 0 || ".?!".indexOf(rrccT.charAt(rrccPrevio)) < 0) {
                continue;
            }
            if (rrccPrevio >= 1 && Character.isDigit(rrccT.charAt(rrccPrevio - 1))) {
                continue;
            }
            String rrccTextoOracion = rrccO.rrccTexto();
            for (int rrccI = 0; rrccI < rrccTextoOracion.length(); rrccI++) {
                char rrccC = rrccTextoOracion.charAt(rrccI);
                if (Character.isLetter(rrccC)) {
                    if (Character.isLowerCase(rrccC)) {
                        rrccAgregar(rrccLista, new RRCCHallazgo(RRCCTipoHallazgo.RRCC_MAYUSCULA,
                                "La oración empieza con minúscula",
                                rrccTexto.rrccRecortar(rrccTextoOracion, 110), rrccO.rrccInicio() + rrccI, 1,
                                "Escribir \"" + Character.toUpperCase(rrccC) + "\" en mayúscula"));
                    }
                    break;
                }
                if (Character.isDigit(rrccC)) {
                    break;
                }
            }
        }
    }

    private void rrccOracionesLargas(List<RRCCOracion> rrccOraciones, List<RRCCHallazgo> rrccLista) {
        int rrccLimite = rrccPropiedades.rrccPalabrasOracionLarga();
        for (RRCCOracion rrccO : rrccOraciones) {
            int rrccPalabras = rrccTexto.rrccPalabras(rrccO.rrccTexto()).size();
            if (rrccPalabras > rrccLimite) {
                rrccAgregar(rrccLista, new RRCCHallazgo(RRCCTipoHallazgo.RRCC_ORACION_LARGA,
                        "Oración de " + rrccPalabras + " palabras (límite " + rrccLimite + ")",
                        rrccTexto.rrccRecortar(rrccO.rrccTexto(), 140), rrccO.rrccInicio(),
                        rrccO.rrccFin() - rrccO.rrccInicio(), "Dividir en dos o más oraciones"));
            }
        }
    }

    private void rrccSignos(String rrccT, List<RRCCHallazgo> rrccLista) {
        Matcher rrccM = RRCCTextoService.RRCC_PATRON_PARRAFO.matcher(rrccT);
        int rrccInicio = 0;
        List<int[]> rrccRangos = new ArrayList<>();
        while (rrccM.find()) {
            rrccRangos.add(new int[]{rrccInicio, rrccM.start()});
            rrccInicio = rrccM.end();
        }
        rrccRangos.add(new int[]{rrccInicio, rrccT.length()});
        char[][] rrccPares = {{'¿', '?'}, {'¡', '!'}, {'(', ')'}, {'[', ']'}, {'«', '»'}, {'“', '”'}};
        for (int[] rrccR : rrccRangos) {
            String rrccP = rrccT.substring(rrccR[0], rrccR[1]);
            if (rrccP.isBlank()) {
                continue;
            }
            List<String> rrccProblemas = new ArrayList<>();
            for (char[] rrccPar : rrccPares) {
                int rrccAbre = 0;
                int rrccCierra = 0;
                for (int rrccI = 0; rrccI < rrccP.length(); rrccI++) {
                    char rrccC = rrccP.charAt(rrccI);
                    if (rrccC == rrccPar[0]) {
                        rrccAbre++;
                    } else if (rrccC == rrccPar[1]) {
                        rrccCierra++;
                    }
                }
                boolean rrccSoloCierreValido = (rrccPar[0] == '¿' || rrccPar[0] == '¡') && rrccAbre == 0;
                if (rrccPar[1] == ')') {
                    rrccCierra -= (int) RRCC_PATRON_VINETA.matcher(rrccP).results().count();
                    rrccCierra = Math.max(rrccCierra, 0);
                }
                if (rrccAbre != rrccCierra && !rrccSoloCierreValido) {
                    rrccProblemas.add(rrccPar[0] + " " + rrccAbre + " / " + rrccPar[1] + " " + rrccCierra);
                }
            }
            long rrccComillas = rrccP.chars().filter(rrccC -> rrccC == '"').count();
            if (rrccComillas % 2 != 0) {
                rrccProblemas.add("comillas \" impares (" + rrccComillas + ")");
            }
            if (!rrccProblemas.isEmpty()) {
                rrccAgregar(rrccLista, new RRCCHallazgo(RRCCTipoHallazgo.RRCC_SIGNOS_DESBALANCEADOS,
                        "Signos sin pareja: " + String.join(", ", rrccProblemas),
                        rrccTexto.rrccRecortar(rrccP.replace('\n', ' '), 140), rrccR[0], rrccR[1] - rrccR[0],
                        "Revisar que cada signo de apertura tenga su cierre"));
            }
        }
    }

    private void rrccFechas(String rrccT, List<RRCCHallazgo> rrccLista) {
        Matcher rrccM = RRCC_PATRON_FECHA_NUMERICA.matcher(rrccT);
        while (rrccM.find()) {
            int rrccDia = Integer.parseInt(rrccM.group(1));
            int rrccMes = Integer.parseInt(rrccM.group(2));
            int rrccAnio = Integer.parseInt(rrccM.group(3));
            rrccValidarFecha(rrccT, rrccDia, rrccMes, rrccAnio, rrccM.start(), rrccM.end(), rrccLista);
        }
        Matcher rrccTxt = RRCC_PATRON_FECHA_TEXTO.matcher(rrccT);
        while (rrccTxt.find()) {
            Integer rrccMes = RRCC_MESES.get(rrccTexto.rrccNormalizar(rrccTxt.group(2)));
            if (rrccMes == null) {
                continue;
            }
            rrccValidarFecha(rrccT, Integer.parseInt(rrccTxt.group(1)), rrccMes, Integer.parseInt(rrccTxt.group(3)),
                    rrccTxt.start(), rrccTxt.end(), rrccLista);
        }
    }

    private void rrccValidarFecha(String rrccT, int rrccDia, int rrccMes, int rrccAnio, int rrccIni, int rrccFin,
                                  List<RRCCHallazgo> rrccLista) {
        String rrccMotivo = null;
        if (rrccMes < 1 || rrccMes > 12) {
            rrccMotivo = "el mes " + rrccMes + " no existe";
        } else {
            try {
                LocalDate.of(rrccAnio, rrccMes, rrccDia);
            } catch (DateTimeException rrccError) {
                rrccMotivo = "el día " + rrccDia + " no existe en ese mes";
            }
        }
        if (rrccMotivo != null) {
            rrccAgregar(rrccLista, new RRCCHallazgo(RRCCTipoHallazgo.RRCC_FECHA_INVALIDA,
                    "Fecha imposible \"" + rrccT.substring(rrccIni, rrccFin) + "\": " + rrccMotivo,
                    rrccContexto(rrccT, rrccIni, rrccFin), rrccIni, rrccFin - rrccIni, "Corregir la fecha"));
        }
    }

    private void rrccParrafosDuplicados(String rrccT, List<RRCCHallazgo> rrccLista) {
        Map<String, Integer> rrccVistos = new HashMap<>();
        Matcher rrccM = RRCCTextoService.RRCC_PATRON_PARRAFO.matcher(rrccT);
        int rrccInicio = 0;
        List<int[]> rrccRangos = new ArrayList<>();
        while (rrccM.find()) {
            rrccRangos.add(new int[]{rrccInicio, rrccM.start()});
            rrccInicio = rrccM.end();
        }
        rrccRangos.add(new int[]{rrccInicio, rrccT.length()});
        int rrccNumero = 0;
        for (int[] rrccR : rrccRangos) {
            String rrccP = rrccT.substring(rrccR[0], rrccR[1]);
            if (rrccP.isBlank()) {
                continue;
            }
            rrccNumero++;
            if (rrccTexto.rrccPalabras(rrccP).size() < 6) {
                continue;
            }
            String rrccClave = rrccTexto.rrccNormalizar(rrccP).replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
            Integer rrccPrevio = rrccVistos.putIfAbsent(rrccClave, rrccNumero);
            if (rrccPrevio != null) {
                rrccAgregar(rrccLista, new RRCCHallazgo(RRCCTipoHallazgo.RRCC_PARRAFO_DUPLICADO,
                        "El párrafo " + rrccNumero + " repite el párrafo " + rrccPrevio,
                        rrccTexto.rrccRecortar(rrccP.replace('\n', ' '), 140), rrccR[0], rrccR[1] - rrccR[0],
                        "Eliminar el párrafo repetido"));
            }
        }
    }

    private Set<String> rrccContenido(String rrccOracion) {
        Set<String> rrccS = new HashSet<>(rrccTexto.rrccTerminos(rrccOracion));
        for (String[] rrccPar : RRCC_ANTONIMOS) {
            rrccS.remove(rrccTexto.rrccRaiz(rrccPar[0]));
            rrccS.remove(rrccTexto.rrccRaiz(rrccPar[1]));
        }
        return rrccS;
    }

    private Set<String> rrccNormalizadas(String rrccOracion) {
        Set<String> rrccS = new HashSet<>();
        for (String rrccP : rrccTexto.rrccPalabras(rrccOracion)) {
            rrccS.add(rrccTexto.rrccNormalizar(rrccP));
        }
        return rrccS;
    }

    private void rrccContradicciones(List<RRCCOracion> rrccOraciones, List<RRCCHallazgo> rrccLista) {
        int rrccN = Math.min(rrccOraciones.size(), RRCC_MAX_ORACIONES_CRUCE);
        List<Set<String>> rrccTerminos = new ArrayList<>();
        List<Set<String>> rrccPalabras = new ArrayList<>();
        for (int rrccI = 0; rrccI < rrccN; rrccI++) {
            rrccTerminos.add(rrccContenido(rrccOraciones.get(rrccI).rrccTexto()));
            rrccPalabras.add(rrccNormalizadas(rrccOraciones.get(rrccI).rrccTexto()));
        }
        double rrccUmbral = rrccPropiedades.rrccUmbralSimilitudContradiccion();
        for (int rrccI = 0; rrccI < rrccN; rrccI++) {
            if (rrccTerminos.get(rrccI).size() < 3) {
                continue;
            }
            for (int rrccJ = rrccI + 1; rrccJ < rrccN; rrccJ++) {
                if (rrccTerminos.get(rrccJ).size() < 3) {
                    continue;
                }
                double rrccSim = rrccSimilitud(rrccTerminos.get(rrccI), rrccTerminos.get(rrccJ));
                if (rrccSim < rrccUmbral || rrccPalabras.get(rrccI).equals(rrccPalabras.get(rrccJ))) {
                    continue;
                }
                String rrccMotivo = rrccMotivoContradiccion(rrccPalabras.get(rrccI), rrccPalabras.get(rrccJ));
                if (rrccMotivo == null) {
                    continue;
                }
                RRCCOracion rrccA = rrccOraciones.get(rrccI);
                RRCCOracion rrccB = rrccOraciones.get(rrccJ);
                rrccAgregar(rrccLista, new RRCCHallazgo(RRCCTipoHallazgo.RRCC_CONTRADICCION,
                        "Oraciones " + (rrccA.rrccIndice() + 1) + " y " + (rrccB.rrccIndice() + 1) + " dicen lo contrario ("
                                + rrccMotivo + ", similitud " + Math.round(rrccSim * 100) + "%)",
                        "\"" + rrccTexto.rrccRecortar(rrccA.rrccTexto(), 110) + "\"  vs  \""
                                + rrccTexto.rrccRecortar(rrccB.rrccTexto(), 110) + "\"",
                        rrccA.rrccInicio(), rrccA.rrccFin() - rrccA.rrccInicio(), "Unificar el criterio en ambas oraciones"));
            }
        }
    }

    private String rrccMotivoContradiccion(Set<String> rrccA, Set<String> rrccB) {
        boolean rrccNegA = rrccA.stream().anyMatch(RRCC_NEGACIONES::contains);
        boolean rrccNegB = rrccB.stream().anyMatch(RRCC_NEGACIONES::contains);
        if (rrccNegA != rrccNegB) {
            return "una niega y la otra afirma";
        }
        for (String[] rrccPar : RRCC_ANTONIMOS) {
            if ((rrccA.contains(rrccPar[0]) && rrccB.contains(rrccPar[1]) && !rrccA.contains(rrccPar[1]))
                    || (rrccA.contains(rrccPar[1]) && rrccB.contains(rrccPar[0]) && !rrccA.contains(rrccPar[0]))) {
                return "\"" + rrccPar[0] + "\" frente a \"" + rrccPar[1] + "\"";
            }
        }
        return null;
    }

    private void rrccCifras(List<RRCCOracion> rrccOraciones, List<RRCCHallazgo> rrccLista) {
        Map<String, List<Object[]>> rrccPorClave = new LinkedHashMap<>();
        int rrccN = Math.min(rrccOraciones.size(), RRCC_MAX_ORACIONES_CRUCE);
        for (int rrccI = 0; rrccI < rrccN; rrccI++) {
            RRCCOracion rrccO = rrccOraciones.get(rrccI);
            Matcher rrccM = RRCC_PATRON_CIFRA.matcher(rrccO.rrccTexto());
            while (rrccM.find()) {
                String rrccPalabra = rrccTexto.rrccNormalizar(rrccM.group(2));
                if (!rrccTexto.rrccEsTermino(rrccPalabra) || RRCC_MESES.containsKey(rrccPalabra)) {
                    continue;
                }
                String rrccValor = rrccM.group(1).replace(',', '.');
                rrccPorClave.computeIfAbsent(rrccTexto.rrccRaiz(rrccPalabra), rrccK -> new ArrayList<>())
                        .add(new Object[]{rrccValor, rrccO, rrccM.group()});
            }
        }
        Set<String> rrccReportados = new HashSet<>();
        for (Map.Entry<String, List<Object[]>> rrccE : rrccPorClave.entrySet()) {
            List<Object[]> rrccApariciones = rrccE.getValue();
            for (int rrccI = 0; rrccI < rrccApariciones.size(); rrccI++) {
                for (int rrccJ = rrccI + 1; rrccJ < rrccApariciones.size(); rrccJ++) {
                    Object[] rrccA = rrccApariciones.get(rrccI);
                    Object[] rrccB = rrccApariciones.get(rrccJ);
                    if (rrccA[0].equals(rrccB[0]) || rrccA[1] == rrccB[1]) {
                        continue;
                    }
                    RRCCOracion rrccOa = (RRCCOracion) rrccA[1];
                    RRCCOracion rrccOb = (RRCCOracion) rrccB[1];
                    double rrccSim = rrccSimilitud(rrccSinNumeros(rrccOa.rrccTexto()), rrccSinNumeros(rrccOb.rrccTexto()));
                    String rrccLlave = rrccE.getKey() + "|" + rrccA[0] + "|" + rrccB[0];
                    if (rrccSim < RRCC_UMBRAL_CIFRAS || !rrccReportados.add(rrccLlave)) {
                        continue;
                    }
                    rrccAgregar(rrccLista, new RRCCHallazgo(RRCCTipoHallazgo.RRCC_CIFRA_INCONSISTENTE,
                            "Se indica \"" + rrccA[2] + "\" y luego \"" + rrccB[2] + "\" en un contexto similar",
                            "\"" + rrccTexto.rrccRecortar(rrccOa.rrccTexto(), 110) + "\"  vs  \""
                                    + rrccTexto.rrccRecortar(rrccOb.rrccTexto(), 110) + "\"",
                            rrccOb.rrccInicio(), rrccOb.rrccFin() - rrccOb.rrccInicio(), "Verificar cuál cifra es la correcta"));
                }
            }
        }
    }

    private double rrccSimilitud(Set<String> rrccA, Set<String> rrccB) {
        if (rrccA.isEmpty() || rrccB.isEmpty()) {
            return 0;
        }
        long rrccComunes = rrccA.stream().filter(rrccB::contains).count();
        double rrccSolapamiento = rrccComunes / (double) Math.min(rrccA.size(), rrccB.size());
        return (rrccTexto.rrccJaccard(rrccA, rrccB) + rrccSolapamiento) / 2;
    }

    private Set<String> rrccSinNumeros(String rrccOracion) {
        return new HashSet<>(rrccTexto.rrccTerminos(rrccOracion));
    }
}
