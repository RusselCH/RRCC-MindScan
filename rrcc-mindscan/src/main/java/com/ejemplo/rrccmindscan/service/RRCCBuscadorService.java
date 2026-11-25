package com.ejemplo.rrccmindscan.service;

import com.ejemplo.rrccmindscan.model.RRCCCategoria;
import com.ejemplo.rrccmindscan.model.RRCCDocumento;
import com.ejemplo.rrccmindscan.service.RRCCTextoService.RRCCOracion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RRCCBuscadorService {

    public static final double RRCC_K1 = 1.4;
    public static final double RRCC_B = 0.75;
    public static final double RRCC_BONO_NOMBRE = 1.5;
    public static final int RRCC_MAX_FRAGMENTOS = 3;
    public static final int RRCC_LARGO_FRAGMENTO = 260;
    public static final int RRCC_MAX_SUGERENCIAS = 2;

    private final RRCCTextoService rrccTexto;
    private final Map<String, Map<String, Integer>> rrccCache = new ConcurrentHashMap<>();

    public record RRCCOpcionesBusqueda(boolean rrccFraseExacta, boolean rrccTolerante, RRCCCategoria rrccCategoria) {
    }

    public record RRCCResultadoBusqueda(RRCCDocumento rrccDocumento, double rrccPuntaje, int rrccCoincidencias,
                                        List<String> rrccFragmentos, Set<String> rrccClaves) {
    }

    public record RRCCRespuestaBusqueda(List<RRCCResultadoBusqueda> rrccResultados, List<String> rrccCorrecciones,
                                        long rrccMilisegundos, Set<String> rrccClaves, String rrccFrase) {
    }

    public RRCCRespuestaBusqueda rrccBuscar(List<RRCCDocumento> rrccTodos, String rrccConsulta, RRCCOpcionesBusqueda rrccOpciones) {
        long rrccT0 = System.currentTimeMillis();
        List<RRCCDocumento> rrccDocs = rrccTodos.stream()
                .filter(rrccD -> rrccOpciones.rrccCategoria() == null || rrccD.getRrccCategoria() == rrccOpciones.rrccCategoria())
                .toList();
        String rrccLimpia = rrccConsulta == null ? "" : rrccConsulta.replace("\"", " ").strip();
        if (rrccLimpia.isEmpty() || rrccDocs.isEmpty()) {
            return new RRCCRespuestaBusqueda(List.of(), List.of(), 0, Set.of(), null);
        }
        if (rrccOpciones.rrccFraseExacta()) {
            return rrccBuscarFrase(rrccDocs, rrccLimpia, rrccT0);
        }

        Set<String> rrccClaves = new LinkedHashSet<>(rrccTexto.rrccTerminos(rrccLimpia));
        if (rrccClaves.isEmpty()) {
            return new RRCCRespuestaBusqueda(List.of(), List.of(), System.currentTimeMillis() - rrccT0, Set.of(), null);
        }
        List<Map<String, Integer>> rrccTf = new ArrayList<>();
        Map<String, Integer> rrccDf = new HashMap<>();
        double rrccLargoTotal = 0;
        for (RRCCDocumento rrccD : rrccDocs) {
            Map<String, Integer> rrccC = rrccConteo(rrccD);
            rrccTf.add(rrccC);
            rrccC.keySet().forEach(rrccT -> rrccDf.merge(rrccT, 1, Integer::sum));
            rrccLargoTotal += rrccC.values().stream().mapToInt(Integer::intValue).sum();
        }
        List<String> rrccCorrecciones = new ArrayList<>();
        if (rrccOpciones.rrccTolerante()) {
            Set<String> rrccExpandidas = new LinkedHashSet<>(rrccClaves);
            for (String rrccQ : rrccClaves) {
                if (rrccDf.containsKey(rrccQ)) {
                    continue;
                }
                int rrccTolerancia = rrccQ.length() <= 5 ? 1 : 2;
                List<String> rrccCercanas = rrccDf.keySet().stream()
                        .filter(rrccV -> Math.abs(rrccV.length() - rrccQ.length()) <= rrccTolerancia)
                        .filter(rrccV -> rrccTexto.rrccLevenshtein(rrccQ, rrccV, rrccTolerancia) <= rrccTolerancia)
                        .sorted(Comparator.comparingInt((String rrccV) -> rrccTexto.rrccLevenshtein(rrccQ, rrccV, rrccTolerancia))
                                .thenComparing(rrccV -> -rrccDf.get(rrccV)))
                        .limit(RRCC_MAX_SUGERENCIAS)
                        .toList();
                if (!rrccCercanas.isEmpty()) {
                    rrccExpandidas.addAll(rrccCercanas);
                    rrccCorrecciones.add(rrccQ + " -> " + String.join(", ", rrccFormas(rrccCercanas, rrccDocs)));
                }
            }
            rrccClaves = rrccExpandidas;
        }
        int rrccN = rrccDocs.size();
        double rrccPromedio = Math.max(1, rrccLargoTotal / rrccN);
        List<RRCCResultadoBusqueda> rrccResultados = new ArrayList<>();
        for (int rrccI = 0; rrccI < rrccN; rrccI++) {
            RRCCDocumento rrccD = rrccDocs.get(rrccI);
            Map<String, Integer> rrccC = rrccTf.get(rrccI);
            double rrccLargo = rrccC.values().stream().mapToInt(Integer::intValue).sum();
            double rrccPuntaje = 0;
            int rrccCoincidencias = 0;
            Set<String> rrccPresentes = new HashSet<>();
            String rrccNombre = rrccTexto.rrccNormalizar(rrccD.getRrccNombre());
            for (String rrccQ : rrccClaves) {
                Integer rrccF = rrccC.get(rrccQ);
                if (rrccNombre.contains(rrccQ)) {
                    rrccPuntaje += RRCC_BONO_NOMBRE;
                    rrccPresentes.add(rrccQ);
                }
                if (rrccF == null) {
                    continue;
                }
                rrccPresentes.add(rrccQ);
                rrccCoincidencias += rrccF;
                int rrccD2 = rrccDf.getOrDefault(rrccQ, 0);
                double rrccIdf = Math.log(1 + (rrccN - rrccD2 + 0.5) / (rrccD2 + 0.5));
                rrccPuntaje += rrccIdf * (rrccF * (RRCC_K1 + 1))
                        / (rrccF + RRCC_K1 * (1 - RRCC_B + RRCC_B * rrccLargo / rrccPromedio));
            }
            if (rrccPuntaje <= 0) {
                continue;
            }
            rrccPuntaje *= 1 + 0.3 * (rrccPresentes.size() - 1);
            rrccResultados.add(new RRCCResultadoBusqueda(rrccD, rrccPuntaje, rrccCoincidencias,
                    rrccFragmentos(rrccD.getRrccContenido(), rrccPresentes), rrccPresentes));
        }
        rrccResultados.sort(Comparator.comparingDouble(RRCCResultadoBusqueda::rrccPuntaje).reversed());
        return new RRCCRespuestaBusqueda(rrccResultados, rrccCorrecciones, System.currentTimeMillis() - rrccT0, rrccClaves, null);
    }

    private RRCCRespuestaBusqueda rrccBuscarFrase(List<RRCCDocumento> rrccDocs, String rrccFrase, long rrccT0) {
        String[] rrccPartes = rrccTexto.rrccNormalizar(rrccFrase).split("\\s+");
        StringBuilder rrccRegex = new StringBuilder("(?<![\\p{L}\\p{N}])");
        for (int rrccI = 0; rrccI < rrccPartes.length; rrccI++) {
            if (rrccI > 0) {
                rrccRegex.append("[\\s\\p{Punct}]+");
            }
            rrccRegex.append(Pattern.quote(rrccPartes[rrccI]));
        }
        rrccRegex.append("(?![\\p{L}\\p{N}])");
        Pattern rrccPatron = Pattern.compile(rrccRegex.toString());
        List<RRCCResultadoBusqueda> rrccResultados = new ArrayList<>();
        for (RRCCDocumento rrccD : rrccDocs) {
            String rrccOriginal = rrccD.getRrccContenido();
            String rrccNormalizado = rrccTexto.rrccNormalizar(rrccOriginal);
            boolean rrccAlineado = rrccNormalizado.length() == rrccOriginal.length();
            Matcher rrccM = rrccPatron.matcher(rrccNormalizado);
            int rrccVeces = 0;
            List<String> rrccFragmentos = new ArrayList<>();
            while (rrccM.find()) {
                rrccVeces++;
                if (rrccFragmentos.size() < RRCC_MAX_FRAGMENTOS) {
                    String rrccFuente = rrccAlineado ? rrccOriginal : rrccNormalizado;
                    rrccFragmentos.add(rrccVentana(rrccFuente, rrccM.start(), rrccM.end()));
                }
            }
            if (rrccVeces > 0) {
                int rrccPalabras = Math.max(1, rrccD.getRrccPalabras());
                double rrccPuntaje = rrccVeces * (1 + Math.log(1 + rrccVeces)) / Math.log(Math.E + rrccPalabras / 1000.0);
                rrccResultados.add(new RRCCResultadoBusqueda(rrccD, rrccPuntaje, rrccVeces, rrccFragmentos,
                        new LinkedHashSet<>(rrccTexto.rrccTerminos(rrccFrase))));
            }
        }
        rrccResultados.sort(Comparator.comparingDouble(RRCCResultadoBusqueda::rrccPuntaje).reversed());
        return new RRCCRespuestaBusqueda(rrccResultados, List.of(), System.currentTimeMillis() - rrccT0,
                new LinkedHashSet<>(rrccTexto.rrccTerminos(rrccFrase)), rrccTexto.rrccNormalizar(rrccFrase));
    }

    private List<String> rrccFragmentos(String rrccContenido, Set<String> rrccClaves) {
        List<RRCCOracion> rrccOraciones = rrccTexto.rrccOraciones(rrccContenido);
        List<Object[]> rrccPuntuadas = new ArrayList<>();
        for (RRCCOracion rrccO : rrccOraciones) {
            Set<String> rrccEnOracion = new HashSet<>(rrccTexto.rrccTerminos(rrccO.rrccTexto()));
            int rrccDistintas = 0;
            for (String rrccC : rrccClaves) {
                if (rrccEnOracion.contains(rrccC)) {
                    rrccDistintas++;
                }
            }
            if (rrccDistintas > 0) {
                rrccPuntuadas.add(new Object[]{rrccO, rrccDistintas});
            }
        }
        rrccPuntuadas.sort((rrccA, rrccB) -> {
            int rrccPorCantidad = Integer.compare((Integer) rrccB[1], (Integer) rrccA[1]);
            return rrccPorCantidad != 0 ? rrccPorCantidad
                    : Integer.compare(((RRCCOracion) rrccA[0]).rrccIndice(), ((RRCCOracion) rrccB[0]).rrccIndice());
        });
        List<String> rrccLista = new ArrayList<>();
        for (int rrccI = 0; rrccI < Math.min(RRCC_MAX_FRAGMENTOS, rrccPuntuadas.size()); rrccI++) {
            rrccLista.add(rrccTexto.rrccRecortar(((RRCCOracion) rrccPuntuadas.get(rrccI)[0]).rrccTexto(), RRCC_LARGO_FRAGMENTO));
        }
        return rrccLista;
    }

    private String rrccVentana(String rrccTextoDoc, int rrccIni, int rrccFin) {
        int rrccRadio = (RRCC_LARGO_FRAGMENTO - (rrccFin - rrccIni)) / 2;
        int rrccDesde = Math.max(0, rrccIni - Math.max(20, rrccRadio));
        int rrccHasta = Math.min(rrccTextoDoc.length(), rrccFin + Math.max(20, rrccRadio));
        return (rrccDesde > 0 ? "..." : "") + rrccTextoDoc.substring(rrccDesde, rrccHasta).replaceAll("\\s+", " ").strip()
                + (rrccHasta < rrccTextoDoc.length() ? "..." : "");
    }

    private Map<String, Integer> rrccConteo(RRCCDocumento rrccD) {
        String rrccLlave = rrccD.getRrccId() + ":" + rrccD.getRrccHash();
        return rrccCache.computeIfAbsent(rrccLlave, rrccK -> rrccTexto.rrccConteoTerminos(rrccD.getRrccContenido()));
    }

    public void rrccLimpiarCache() {
        rrccCache.clear();
    }

    private List<String> rrccFormas(List<String> rrccClavesCercanas, List<RRCCDocumento> rrccDocs) {
        Map<String, String> rrccFormas = new HashMap<>();
        for (RRCCDocumento rrccD : rrccDocs) {
            if (rrccFormas.size() == rrccClavesCercanas.size()) {
                break;
            }
            rrccTexto.rrccFormasVisibles(rrccClavesCercanas, rrccD.getRrccContenido()).forEach(rrccFormas::putIfAbsent);
        }
        return rrccClavesCercanas.stream().map(rrccC -> rrccFormas.getOrDefault(rrccC, rrccC)).toList();
    }
}
