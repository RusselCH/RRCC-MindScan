package com.ejemplo.rrccmindscan.service;

import com.ejemplo.rrccmindscan.config.RRCCAnalisisPropiedades;
import com.ejemplo.rrccmindscan.model.RRCCCategoria;
import com.ejemplo.rrccmindscan.model.RRCCTipoDato;
import com.ejemplo.rrccmindscan.service.RRCCTextoService.RRCCOracion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RRCCAnalizadorService {

    public static final double RRCC_AMORTIGUACION = 0.85;
    public static final int RRCC_ITERACIONES_RANGO = 50;
    public static final double RRCC_CONVERGENCIA = 1e-5;
    public static final double RRCC_BONO_INICIO = 0.15;
    public static final int RRCC_ORACIONES_INICIALES = 3;
    public static final int RRCC_MINIMO_CATEGORIA = 3;
    public static final int RRCC_RADIO_CONTEXTO = 45;
    public static final double RRCC_BM25_K1 = 1.5;
    public static final double RRCC_BM25_B = 0.75;
    public static final double RRCC_BONO_TIPO_PREGUNTA = 1.35;

    public static final String RRCC_MESES = "enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|setiembre|octubre|noviembre|diciembre";
    public static final Map<RRCCTipoDato, Pattern> RRCC_PATRONES = new LinkedHashMap<>();

    static {
        RRCC_PATRONES.put(RRCCTipoDato.RRCC_CORREO, Pattern.compile("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+"));
        RRCC_PATRONES.put(RRCCTipoDato.RRCC_URL, Pattern.compile("(?i)(?:https?://|www\\.)[^\\s<>\"]+"));
        RRCC_PATRONES.put(RRCCTipoDato.RRCC_RUC, Pattern.compile("(?<!\\d)(?:10|15|17|20)\\d{9}(?!\\d)"));
        RRCC_PATRONES.put(RRCCTipoDato.RRCC_DNI, Pattern.compile("(?i)(?<=DNI\\s?(?:N[°ºo.]?\\s?)?:?\\s?)\\d{8}(?!\\d)"));
        RRCC_PATRONES.put(RRCCTipoDato.RRCC_TELEFONO, Pattern.compile("(?<![\\d-])(?:\\+51\\s?)?9\\d{2}[\\s-]?\\d{3}[\\s-]?\\d{3}(?![\\d-])|\\(0\\d{1,2}\\)\\s?\\d{3}[\\s-]?\\d{3,4}"));
        RRCC_PATRONES.put(RRCCTipoDato.RRCC_FECHA, Pattern.compile("(?i)(?<!\\d)\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}(?!\\d)|\\b\\d{1,2}\\s+de\\s+(?:" + RRCC_MESES + ")(?:\\s+(?:de|del)\\s+\\d{4})?"));
        RRCC_PATRONES.put(RRCCTipoDato.RRCC_MONTO, Pattern.compile("(?i)(?:S/\\.?|US\\$|\\$|€)\\s?\\d{1,3}(?:[ ,.]\\d{3})*(?:[.,]\\d{1,2})?|\\b\\d{1,3}(?:[ ,.]\\d{3})*(?:[.,]\\d{1,2})?\\s?(?:soles|dolares|nuevos soles)\\b"));
        RRCC_PATRONES.put(RRCCTipoDato.RRCC_PORCENTAJE, Pattern.compile("\\b\\d{1,3}(?:[.,]\\d+)?\\s?%"));
    }

    public static final Map<Pattern, Pattern> RRCC_TIPOS_PREGUNTA = new LinkedHashMap<>();

    static {
        RRCC_TIPOS_PREGUNTA.put(Pattern.compile("\\b(cuando|fecha|plazo|dia|hasta)\\b"),
                Pattern.compile("(?i)\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\b\\d+\\s+(dias|meses|anos|semanas|horas)\\b|\\b(" + RRCC_MESES + ")\\b"));
        RRCC_TIPOS_PREGUNTA.put(Pattern.compile("\\b(monto|costo|precio|pago|total|presupuesto|importe|cuesta)\\b"),
                Pattern.compile("(?i)s/\\.?\\s?\\d|us\\$|\\d[\\d,.]*\\s?(soles|dolares)|\\d+[.,]\\d{2}\\b"));
        RRCC_TIPOS_PREGUNTA.put(Pattern.compile("\\b(cuantos|cuantas|cantidad)\\b"), Pattern.compile("\\d"));
        RRCC_TIPOS_PREGUNTA.put(Pattern.compile("\\b(quien|quienes|responsable|encargado)\\b"),
                Pattern.compile("\\b(responsable|encargad[oa]|jefe|coordinador[a]?|director[a]?|docente|ing\\.|dr\\.|lic\\.)"));
        RRCC_TIPOS_PREGUNTA.put(Pattern.compile("\\b(donde|lugar|ubicacion)\\b"),
                Pattern.compile("\\b(ciudad|ubicad[oa]|sede|aula|sala|laboratorio|oficina|piso|domicilio|carretera|avenida|calle|juliaca|lima|puno|arequipa|cusco|campus)\\b"));
        RRCC_TIPOS_PREGUNTA.put(Pattern.compile("\\b(porcentaje|por ciento|tasa)\\b"), Pattern.compile("%"));
    }

    private final RRCCTextoService rrccTexto;
    private final RRCCAnalisisPropiedades rrccPropiedades;

    public record RRCCTermino(String rrccClave, String rrccForma, int rrccFrecuencia, double rrccPeso) {
    }

    public record RRCCOracionPuntuada(int rrccIndice, String rrccTexto, double rrccPuntaje, boolean rrccSeleccionada) {
    }

    public record RRCCDato(RRCCTipoDato rrccTipo, String rrccValor, String rrccContexto, int rrccPosicion, int rrccOcurrencias) {
    }

    public record RRCCRespuesta(int rrccIndice, String rrccOracion, double rrccPuntaje, Set<String> rrccClaves) {
    }

    public record RRCCClasificacion(RRCCCategoria rrccCategoria, Map<RRCCCategoria, Integer> rrccPuntajes) {
    }

    public List<RRCCTermino> rrccPalabrasClave(String rrccTextoDoc, List<String> rrccCorpus, int rrccCantidad) {
        Map<String, Integer> rrccTf = new HashMap<>();
        Map<String, Map<String, Integer>> rrccFormas = new HashMap<>();
        for (String rrccP : rrccTexto.rrccPalabras(rrccTextoDoc)) {
            String rrccN = rrccTexto.rrccNormalizar(rrccP);
            if (!rrccTexto.rrccEsTermino(rrccN)) {
                continue;
            }
            String rrccClave = rrccTexto.rrccRaiz(rrccN);
            rrccTf.merge(rrccClave, 1, Integer::sum);
            rrccFormas.computeIfAbsent(rrccClave, rrccK -> new HashMap<>()).merge(rrccP.toLowerCase(Locale.ROOT), 1, Integer::sum);
        }
        if (rrccTf.isEmpty()) {
            return List.of();
        }
        List<Set<String>> rrccConjuntos = new ArrayList<>();
        for (String rrccDoc : rrccCorpus) {
            rrccConjuntos.add(new HashSet<>(rrccTexto.rrccTerminos(rrccDoc)));
        }
        int rrccN = Math.max(1, rrccConjuntos.size());
        int rrccTotal = rrccTf.values().stream().mapToInt(Integer::intValue).sum();
        List<RRCCTermino> rrccLista = new ArrayList<>();
        for (Map.Entry<String, Integer> rrccE : rrccTf.entrySet()) {
            long rrccDf = rrccConjuntos.stream().filter(rrccS -> rrccS.contains(rrccE.getKey())).count();
            double rrccIdf = Math.log((1.0 + rrccN) / (1.0 + rrccDf)) + 1.0;
            double rrccPeso = (rrccE.getValue() * 100.0 / rrccTotal) * rrccIdf;
            String rrccForma = rrccFormas.get(rrccE.getKey()).entrySet().stream()
                    .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(rrccE.getKey());
            rrccLista.add(new RRCCTermino(rrccE.getKey(), rrccForma, rrccE.getValue(), Math.round(rrccPeso * 100) / 100.0));
        }
        rrccLista.sort(Comparator.comparingDouble(RRCCTermino::rrccPeso).reversed()
                .thenComparing(RRCCTermino::rrccFrecuencia, Comparator.reverseOrder()));
        return rrccLista.subList(0, Math.min(rrccCantidad, rrccLista.size()));
    }

    public List<RRCCOracionPuntuada> rrccResumen(String rrccTextoDoc, double rrccProporcion) {
        List<RRCCOracion> rrccOraciones = rrccTexto.rrccOraciones(rrccTextoDoc);
        int rrccN = rrccOraciones.size();
        if (rrccN == 0) {
            return List.of();
        }
        List<Integer> rrccCandidatas = new ArrayList<>();
        for (int rrccI = 0; rrccI < rrccN; rrccI++) {
            rrccCandidatas.add(rrccI);
        }
        List<Set<String>> rrccTerminos = new ArrayList<>();
        for (RRCCOracion rrccO : rrccOraciones) {
            rrccTerminos.add(new HashSet<>(rrccTexto.rrccTerminos(rrccO.rrccTexto())));
        }
        if (rrccN > rrccPropiedades.rrccMaxOracionesGrafo()) {
            Map<String, Integer> rrccGlobal = rrccTexto.rrccConteoTerminos(rrccTextoDoc);
            rrccCandidatas.sort(Comparator.comparingDouble((Integer rrccI) -> -rrccPuntajeFrecuencia(rrccTerminos.get(rrccI), rrccGlobal)));
            rrccCandidatas = new ArrayList<>(rrccCandidatas.subList(0, rrccPropiedades.rrccMaxOracionesGrafo()));
            Collections.sort(rrccCandidatas);
        }
        int rrccM = rrccCandidatas.size();
        double[][] rrccPesos = new double[rrccM][rrccM];
        double[] rrccSalida = new double[rrccM];
        for (int rrccA = 0; rrccA < rrccM; rrccA++) {
            for (int rrccB = rrccA + 1; rrccB < rrccM; rrccB++) {
                double rrccS = rrccSimilitudTextRank(rrccTerminos.get(rrccCandidatas.get(rrccA)), rrccTerminos.get(rrccCandidatas.get(rrccB)));
                rrccPesos[rrccA][rrccB] = rrccS;
                rrccPesos[rrccB][rrccA] = rrccS;
                rrccSalida[rrccA] += rrccS;
                rrccSalida[rrccB] += rrccS;
            }
        }
        double[] rrccRango = new double[rrccM];
        Arrays.fill(rrccRango, 1.0);
        for (int rrccIter = 0; rrccIter < RRCC_ITERACIONES_RANGO; rrccIter++) {
            double[] rrccNuevo = new double[rrccM];
            double rrccCambio = 0;
            for (int rrccA = 0; rrccA < rrccM; rrccA++) {
                double rrccSuma = 0;
                for (int rrccB = 0; rrccB < rrccM; rrccB++) {
                    if (rrccPesos[rrccB][rrccA] > 0 && rrccSalida[rrccB] > 0) {
                        rrccSuma += rrccPesos[rrccB][rrccA] / rrccSalida[rrccB] * rrccRango[rrccB];
                    }
                }
                rrccNuevo[rrccA] = (1 - RRCC_AMORTIGUACION) + RRCC_AMORTIGUACION * rrccSuma;
                rrccCambio += Math.abs(rrccNuevo[rrccA] - rrccRango[rrccA]);
            }
            rrccRango = rrccNuevo;
            if (rrccCambio < RRCC_CONVERGENCIA) {
                break;
            }
        }
        double[] rrccPuntajes = new double[rrccN];
        for (int rrccA = 0; rrccA < rrccM; rrccA++) {
            int rrccIndice = rrccCandidatas.get(rrccA);
            double rrccBono = rrccIndice < RRCC_ORACIONES_INICIALES ? 1 + RRCC_BONO_INICIO : 1;
            int rrccLargo = rrccTexto.rrccPalabras(rrccOraciones.get(rrccIndice).rrccTexto()).size();
            double rrccPenalizacion = rrccLargo < 5 ? 0.5 : 1.0;
            rrccPuntajes[rrccIndice] = rrccRango[rrccA] * rrccBono * rrccPenalizacion;
        }
        int rrccElegir = Math.max(1, (int) Math.round(rrccN * Math.max(0.05, Math.min(1, rrccProporcion))));
        Integer[] rrccOrden = new Integer[rrccN];
        for (int rrccI = 0; rrccI < rrccN; rrccI++) {
            rrccOrden[rrccI] = rrccI;
        }
        Arrays.sort(rrccOrden, (rrccA, rrccB) -> Double.compare(rrccPuntajes[rrccB], rrccPuntajes[rrccA]));
        Set<Integer> rrccSeleccion = new HashSet<>();
        Set<String> rrccVistas = new HashSet<>();
        for (int rrccI = 0; rrccI < rrccN && rrccSeleccion.size() < rrccElegir; rrccI++) {
            String rrccHuella = rrccTexto.rrccNormalizar(rrccOraciones.get(rrccOrden[rrccI]).rrccTexto()).replaceAll("[^\\p{L}\\p{N}]+", "");
            if (rrccVistas.add(rrccHuella)) {
                rrccSeleccion.add(rrccOrden[rrccI]);
            }
        }
        double rrccMaximo = Arrays.stream(rrccPuntajes).max().orElse(1);
        List<RRCCOracionPuntuada> rrccResultado = new ArrayList<>();
        for (int rrccI = 0; rrccI < rrccN; rrccI++) {
            rrccResultado.add(new RRCCOracionPuntuada(rrccI, rrccOraciones.get(rrccI).rrccTexto(),
                    rrccMaximo == 0 ? 0 : rrccPuntajes[rrccI] / rrccMaximo, rrccSeleccion.contains(rrccI)));
        }
        return rrccResultado;
    }

    private double rrccPuntajeFrecuencia(Set<String> rrccTerminosOracion, Map<String, Integer> rrccGlobal) {
        if (rrccTerminosOracion.isEmpty()) {
            return 0;
        }
        double rrccSuma = 0;
        for (String rrccT : rrccTerminosOracion) {
            rrccSuma += rrccGlobal.getOrDefault(rrccT, 0);
        }
        return rrccSuma / Math.sqrt(rrccTerminosOracion.size());
    }

    private double rrccSimilitudTextRank(Set<String> rrccA, Set<String> rrccB) {
        if (rrccA.size() < 2 || rrccB.size() < 2) {
            return 0;
        }
        int rrccComunes = 0;
        for (String rrccX : rrccA) {
            if (rrccB.contains(rrccX)) {
                rrccComunes++;
            }
        }
        if (rrccComunes == 0) {
            return 0;
        }
        return rrccComunes / (Math.log(rrccA.size()) + Math.log(rrccB.size()));
    }

    public List<RRCCDato> rrccDatos(String rrccTextoDoc) {
        Map<String, RRCCDato> rrccUnicos = new LinkedHashMap<>();
        List<int[]> rrccOcupados = new ArrayList<>();
        for (Map.Entry<RRCCTipoDato, Pattern> rrccE : RRCC_PATRONES.entrySet()) {
            Matcher rrccM = rrccE.getValue().matcher(rrccTextoDoc);
            while (rrccM.find()) {
                int rrccIni = rrccM.start();
                int rrccFin = rrccM.end();
                if (rrccSolapa(rrccOcupados, rrccIni, rrccFin)) {
                    continue;
                }
                String rrccValor = rrccM.group().strip().replaceAll("[.,;:)]+$", "");
                if (rrccValor.isEmpty()) {
                    continue;
                }
                rrccOcupados.add(new int[]{rrccIni, rrccFin});
                String rrccLlave = rrccE.getKey().name() + "|" + rrccValor.toLowerCase(Locale.ROOT);
                RRCCDato rrccPrevio = rrccUnicos.get(rrccLlave);
                if (rrccPrevio != null) {
                    rrccUnicos.put(rrccLlave, new RRCCDato(rrccPrevio.rrccTipo(), rrccPrevio.rrccValor(), rrccPrevio.rrccContexto(),
                            rrccPrevio.rrccPosicion(), rrccPrevio.rrccOcurrencias() + 1));
                } else {
                    rrccUnicos.put(rrccLlave, new RRCCDato(rrccE.getKey(), rrccValor,
                            rrccContexto(rrccTextoDoc, rrccIni, rrccFin), rrccIni, 1));
                }
            }
        }
        List<RRCCDato> rrccLista = new ArrayList<>(rrccUnicos.values());
        rrccLista.sort(Comparator.comparingInt(RRCCDato::rrccPosicion));
        return rrccLista;
    }

    private boolean rrccSolapa(List<int[]> rrccOcupados, int rrccIni, int rrccFin) {
        for (int[] rrccR : rrccOcupados) {
            if (rrccIni < rrccR[1] && rrccR[0] < rrccFin) {
                return true;
            }
        }
        return false;
    }

    public String rrccContexto(String rrccTextoDoc, int rrccIni, int rrccFin) {
        int rrccDesde = Math.max(0, rrccIni - RRCC_RADIO_CONTEXTO);
        int rrccHasta = Math.min(rrccTextoDoc.length(), rrccFin + RRCC_RADIO_CONTEXTO);
        return (rrccDesde > 0 ? "..." : "") + rrccTextoDoc.substring(rrccDesde, rrccHasta).replaceAll("\\s+", " ").strip()
                + (rrccHasta < rrccTextoDoc.length() ? "..." : "");
    }

    public RRCCClasificacion rrccClasificar(String rrccTextoDoc) {
        Map<String, Integer> rrccConteo = new HashMap<>();
        for (String rrccP : rrccTexto.rrccPalabras(rrccTextoDoc)) {
            rrccConteo.merge(rrccTexto.rrccClave(rrccP), 1, Integer::sum);
        }
        Map<RRCCCategoria, Integer> rrccPuntajes = new EnumMap<>(RRCCCategoria.class);
        RRCCCategoria rrccMejor = RRCCCategoria.RRCC_GENERAL;
        int rrccMaximo = 0;
        for (RRCCCategoria rrccC : RRCCCategoria.values()) {
            int rrccSuma = 0;
            for (String rrccClave : rrccC.rrccPalabrasClave()) {
                rrccSuma += rrccConteo.getOrDefault(rrccTexto.rrccRaiz(rrccClave), 0);
            }
            rrccPuntajes.put(rrccC, rrccSuma);
            if (rrccSuma > rrccMaximo) {
                rrccMaximo = rrccSuma;
                rrccMejor = rrccC;
            }
        }
        return new RRCCClasificacion(rrccMaximo >= RRCC_MINIMO_CATEGORIA ? rrccMejor : RRCCCategoria.RRCC_GENERAL, rrccPuntajes);
    }

    public List<RRCCRespuesta> rrccResponder(String rrccTextoDoc, String rrccPregunta, int rrccCantidad) {
        List<String> rrccConsulta = new ArrayList<>(new LinkedHashSet<>(rrccTexto.rrccTerminos(rrccPregunta)));
        List<RRCCOracion> rrccOraciones = rrccTexto.rrccOraciones(rrccTextoDoc);
        String rrccPreguntaNormalizada = rrccTexto.rrccNormalizar(rrccPregunta);
        List<Pattern> rrccPistas = new ArrayList<>();
        RRCC_TIPOS_PREGUNTA.forEach((rrccDisparador, rrccPista) -> {
            if (rrccDisparador.matcher(rrccPreguntaNormalizada).find()) {
                rrccPistas.add(rrccPista);
            }
        });
        if ((rrccConsulta.isEmpty() && rrccPistas.isEmpty()) || rrccOraciones.isEmpty()) {
            return List.of();
        }
        List<Map<String, Integer>> rrccTf = new ArrayList<>();
        Map<String, Integer> rrccDf = new HashMap<>();
        double rrccLargoTotal = 0;
        for (RRCCOracion rrccO : rrccOraciones) {
            Map<String, Integer> rrccC = rrccTexto.rrccConteoTerminos(rrccO.rrccTexto());
            rrccTf.add(rrccC);
            rrccC.keySet().forEach(rrccT -> rrccDf.merge(rrccT, 1, Integer::sum));
            rrccLargoTotal += rrccC.values().stream().mapToInt(Integer::intValue).sum();
        }
        double rrccPromedio = Math.max(1, rrccLargoTotal / rrccOraciones.size());
        List<RRCCRespuesta> rrccRespuestas = new ArrayList<>();
        int rrccN = rrccOraciones.size();
        for (int rrccI = 0; rrccI < rrccN; rrccI++) {
            Map<String, Integer> rrccC = rrccTf.get(rrccI);
            double rrccLargo = rrccC.values().stream().mapToInt(Integer::intValue).sum();
            double rrccPuntaje = 0;
            Set<String> rrccCoinciden = new HashSet<>();
            for (String rrccQ : rrccConsulta) {
                Integer rrccF = rrccC.get(rrccQ);
                if (rrccF == null) {
                    continue;
                }
                rrccCoinciden.add(rrccQ);
                int rrccD = rrccDf.getOrDefault(rrccQ, 0);
                double rrccIdf = Math.log(1 + (rrccN - rrccD + 0.5) / (rrccD + 0.5));
                rrccPuntaje += rrccIdf * (rrccF * (RRCC_BM25_K1 + 1))
                        / (rrccF + RRCC_BM25_K1 * (1 - RRCC_BM25_B + RRCC_BM25_B * rrccLargo / rrccPromedio));
            }
            String rrccOracionNormalizada = rrccTexto.rrccNormalizar(rrccOraciones.get(rrccI).rrccTexto());
            boolean rrccTienePista = false;
            for (Pattern rrccPista : rrccPistas) {
                if (rrccPista.matcher(rrccOracionNormalizada).find()) {
                    rrccTienePista = true;
                    rrccPuntaje = rrccPuntaje * RRCC_BONO_TIPO_PREGUNTA + 0.6;
                }
            }
            if (rrccPuntaje <= 0 || (rrccCoinciden.isEmpty() && !rrccTienePista)) {
                continue;
            }
            rrccPuntaje *= 1 + 0.25 * Math.max(0, rrccCoinciden.size() - 1);
            if (rrccTexto.rrccPalabras(rrccOraciones.get(rrccI).rrccTexto()).size() < 5) {
                rrccPuntaje *= 0.3;
            }
            rrccRespuestas.add(new RRCCRespuesta(rrccI, rrccOraciones.get(rrccI).rrccTexto(), rrccPuntaje, rrccCoinciden));
        }
        rrccRespuestas.sort(Comparator.comparingDouble(RRCCRespuesta::rrccPuntaje).reversed());
        return rrccRespuestas.subList(0, Math.min(rrccCantidad, rrccRespuestas.size()));
    }
}
