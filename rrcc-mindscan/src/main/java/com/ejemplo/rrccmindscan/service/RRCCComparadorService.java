package com.ejemplo.rrccmindscan.service;

import com.ejemplo.rrccmindscan.model.RRCCTipoCambio;
import com.ejemplo.rrccmindscan.service.RRCCTextoService.RRCCOracion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RRCCComparadorService {

    public static final int RRCC_MAX_ORACIONES = 2500;
    public static final double RRCC_UMBRAL_MODIFICADA = 0.4;
    public static final int RRCC_TERMINOS_REPORTE = 12;
    public static final int RRCC_TAMANO_NGRAMA = 6;
    public static final int RRCC_MINIMO_PASAJE = 10;
    public static final int RRCC_MAX_PASAJES = 6;

    private final RRCCTextoService rrccTexto;

    public record RRCCFragmentoDiff(RRCCTipoCambio rrccTipo, String rrccTexto) {
    }

    public record RRCCBloqueDiff(RRCCTipoCambio rrccTipo, String rrccTextoA, String rrccTextoB,
                                 List<RRCCFragmentoDiff> rrccFragmentos, double rrccSimilitud) {
    }

    public record RRCCResultadoComparacion(double rrccSimilitudCoseno, double rrccSimilitudTejas, double rrccSimilitudGlobal,
                                           List<RRCCBloqueDiff> rrccBloques, int rrccIguales, int rrccAgregadas,
                                           int rrccEliminadas, int rrccModificadas, List<String> rrccTerminosNuevos,
                                           List<String> rrccTerminosRetirados) {
        public int rrccCambios() {
            return rrccAgregadas + rrccEliminadas + rrccModificadas;
        }
    }

    public record RRCCPasaje(String rrccTexto, int rrccPalabras) {
    }

    public record RRCCCompartido(double rrccPorcentajeA, double rrccPorcentajeB, List<RRCCPasaje> rrccPasajes) {
    }

    public RRCCCompartido rrccTextoCompartido(String rrccTextoA, String rrccTextoB) {
        String[] rrccPalabrasA = rrccTextoA.strip().split("\\s+");
        String[] rrccPalabrasB = rrccTextoB.strip().split("\\s+");
        boolean[] rrccMarcaA = rrccMarcar(rrccPalabrasA, rrccNgramas(rrccPalabrasB));
        boolean[] rrccMarcaB = rrccMarcar(rrccPalabrasB, rrccNgramas(rrccPalabrasA));
        List<RRCCPasaje> rrccPasajes = new ArrayList<>();
        int rrccI = 0;
        while (rrccI < rrccPalabrasA.length) {
            if (!rrccMarcaA[rrccI]) {
                rrccI++;
                continue;
            }
            int rrccInicio = rrccI;
            while (rrccI < rrccPalabrasA.length && rrccMarcaA[rrccI]) {
                rrccI++;
            }
            int rrccLargo = rrccI - rrccInicio;
            if (rrccLargo >= RRCC_MINIMO_PASAJE) {
                rrccPasajes.add(new RRCCPasaje(String.join(" ", Arrays.copyOfRange(rrccPalabrasA, rrccInicio, rrccI)), rrccLargo));
            }
        }
        rrccPasajes.sort(Comparator.comparingInt(RRCCPasaje::rrccPalabras).reversed());
        return new RRCCCompartido(rrccProporcion(rrccMarcaA), rrccProporcion(rrccMarcaB),
                rrccPasajes.subList(0, Math.min(RRCC_MAX_PASAJES, rrccPasajes.size())));
    }

    private String rrccClavePalabra(String rrccPalabra) {
        return rrccTexto.rrccNormalizar(rrccPalabra).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private Set<String> rrccNgramas(String[] rrccPalabras) {
        Set<String> rrccConjunto = new HashSet<>();
        for (int rrccI = 0; rrccI + RRCC_TAMANO_NGRAMA <= rrccPalabras.length; rrccI++) {
            rrccConjunto.add(rrccNgrama(rrccPalabras, rrccI));
        }
        return rrccConjunto;
    }

    private String rrccNgrama(String[] rrccPalabras, int rrccDesde) {
        StringBuilder rrccClave = new StringBuilder();
        for (int rrccJ = rrccDesde; rrccJ < rrccDesde + RRCC_TAMANO_NGRAMA; rrccJ++) {
            rrccClave.append(rrccClavePalabra(rrccPalabras[rrccJ])).append(' ');
        }
        return rrccClave.toString();
    }

    private boolean[] rrccMarcar(String[] rrccPalabras, Set<String> rrccNgramasOtro) {
        boolean[] rrccMarca = new boolean[rrccPalabras.length];
        for (int rrccI = 0; rrccI + RRCC_TAMANO_NGRAMA <= rrccPalabras.length; rrccI++) {
            if (rrccNgramasOtro.contains(rrccNgrama(rrccPalabras, rrccI))) {
                Arrays.fill(rrccMarca, rrccI, rrccI + RRCC_TAMANO_NGRAMA, true);
            }
        }
        return rrccMarca;
    }

    private double rrccProporcion(boolean[] rrccMarca) {
        if (rrccMarca.length == 0) {
            return 0;
        }
        int rrccMarcadas = 0;
        for (boolean rrccM : rrccMarca) {
            if (rrccM) {
                rrccMarcadas++;
            }
        }
        return rrccMarcadas / (double) rrccMarca.length;
    }

    public RRCCResultadoComparacion rrccComparar(String rrccTextoA, String rrccTextoB) {
        List<String> rrccA = rrccOraciones(rrccTextoA);
        List<String> rrccB = rrccOraciones(rrccTextoB);
        List<String> rrccClavesA = rrccA.stream().map(this::rrccClaveOracion).toList();
        List<String> rrccClavesB = rrccB.stream().map(this::rrccClaveOracion).toList();

        List<int[]> rrccOperaciones = rrccLcs(rrccClavesA, rrccClavesB);
        List<RRCCBloqueDiff> rrccBloques = new ArrayList<>();
        List<Integer> rrccEliminadasRun = new ArrayList<>();
        List<Integer> rrccAgregadasRun = new ArrayList<>();
        for (int[] rrccOp : rrccOperaciones) {
            if (rrccOp[0] == 0) {
                rrccVaciarRun(rrccA, rrccB, rrccEliminadasRun, rrccAgregadasRun, rrccBloques);
                rrccBloques.add(new RRCCBloqueDiff(RRCCTipoCambio.RRCC_IGUAL, rrccA.get(rrccOp[1]), rrccB.get(rrccOp[2]),
                        List.of(new RRCCFragmentoDiff(RRCCTipoCambio.RRCC_IGUAL, rrccB.get(rrccOp[2]))), 1.0));
            } else if (rrccOp[0] == -1) {
                rrccEliminadasRun.add(rrccOp[1]);
            } else {
                rrccAgregadasRun.add(rrccOp[2]);
            }
        }
        rrccVaciarRun(rrccA, rrccB, rrccEliminadasRun, rrccAgregadasRun, rrccBloques);

        int rrccIguales = 0;
        int rrccAgregadas = 0;
        int rrccEliminadas = 0;
        int rrccModificadas = 0;
        for (RRCCBloqueDiff rrccBloque : rrccBloques) {
            switch (rrccBloque.rrccTipo()) {
                case RRCC_IGUAL -> rrccIguales++;
                case RRCC_AGREGADO -> rrccAgregadas++;
                case RRCC_ELIMINADO -> rrccEliminadas++;
                case RRCC_MODIFICADO -> rrccModificadas++;
            }
        }
        Map<String, Integer> rrccConteoA = rrccTexto.rrccConteoTerminos(rrccTextoA);
        Map<String, Integer> rrccConteoB = rrccTexto.rrccConteoTerminos(rrccTextoB);
        double rrccCoseno = rrccTexto.rrccCoseno(rrccConteoA, rrccConteoB);
        double rrccTejas = rrccTexto.rrccJaccard(rrccTexto.rrccTejas(rrccTextoA, 3), rrccTexto.rrccTejas(rrccTextoB, 3));
        double rrccEstructura = (rrccIguales + 0.5 * rrccModificadas) / Math.max(1, Math.max(rrccA.size(), rrccB.size()));
        double rrccGlobal = 0.4 * rrccCoseno + 0.3 * rrccTejas + 0.3 * rrccEstructura;
        return new RRCCResultadoComparacion(rrccCoseno, rrccTejas, Math.min(1, rrccGlobal), rrccBloques, rrccIguales,
                rrccAgregadas, rrccEliminadas, rrccModificadas,
                rrccTexto.rrccVisibles(rrccExclusivos(rrccConteoB, rrccConteoA), rrccTextoB),
                rrccTexto.rrccVisibles(rrccExclusivos(rrccConteoA, rrccConteoB), rrccTextoA));
    }

    private List<String> rrccOraciones(String rrccTextoDoc) {
        List<String> rrccLista = new ArrayList<>();
        for (RRCCOracion rrccO : rrccTexto.rrccOraciones(rrccTextoDoc)) {
            if (rrccLista.size() >= RRCC_MAX_ORACIONES) {
                break;
            }
            rrccLista.add(rrccO.rrccTexto());
        }
        return rrccLista;
    }

    private String rrccClaveOracion(String rrccOracion) {
        return rrccTexto.rrccNormalizar(rrccOracion).replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
    }

    private List<int[]> rrccLcs(List<String> rrccA, List<String> rrccB) {
        int rrccN = rrccA.size();
        int rrccM = rrccB.size();
        int[][] rrccTabla = new int[rrccN + 1][rrccM + 1];
        for (int rrccI = rrccN - 1; rrccI >= 0; rrccI--) {
            for (int rrccJ = rrccM - 1; rrccJ >= 0; rrccJ--) {
                rrccTabla[rrccI][rrccJ] = rrccA.get(rrccI).equals(rrccB.get(rrccJ))
                        ? rrccTabla[rrccI + 1][rrccJ + 1] + 1
                        : Math.max(rrccTabla[rrccI + 1][rrccJ], rrccTabla[rrccI][rrccJ + 1]);
            }
        }
        List<int[]> rrccOps = new ArrayList<>();
        int rrccI = 0;
        int rrccJ = 0;
        while (rrccI < rrccN && rrccJ < rrccM) {
            if (rrccA.get(rrccI).equals(rrccB.get(rrccJ))) {
                rrccOps.add(new int[]{0, rrccI++, rrccJ++});
            } else if (rrccTabla[rrccI + 1][rrccJ] >= rrccTabla[rrccI][rrccJ + 1]) {
                rrccOps.add(new int[]{-1, rrccI++, -1});
            } else {
                rrccOps.add(new int[]{1, -1, rrccJ++});
            }
        }
        while (rrccI < rrccN) {
            rrccOps.add(new int[]{-1, rrccI++, -1});
        }
        while (rrccJ < rrccM) {
            rrccOps.add(new int[]{1, -1, rrccJ++});
        }
        return rrccOps;
    }

    private void rrccVaciarRun(List<String> rrccA, List<String> rrccB, List<Integer> rrccEliminadas,
                               List<Integer> rrccAgregadas, List<RRCCBloqueDiff> rrccBloques) {
        if (rrccEliminadas.isEmpty() && rrccAgregadas.isEmpty()) {
            return;
        }
        Set<Integer> rrccUsadas = new HashSet<>();
        Map<Integer, Integer> rrccPareja = new HashMap<>();
        Map<Integer, Double> rrccSimilitud = new HashMap<>();
        for (int rrccE : rrccEliminadas) {
            Set<String> rrccPalabrasE = rrccConjunto(rrccA.get(rrccE));
            int rrccMejor = -1;
            double rrccMejorSim = RRCC_UMBRAL_MODIFICADA;
            for (int rrccAg : rrccAgregadas) {
                if (rrccUsadas.contains(rrccAg)) {
                    continue;
                }
                double rrccS = rrccTexto.rrccJaccard(rrccPalabrasE, rrccConjunto(rrccB.get(rrccAg)));
                if (rrccS >= rrccMejorSim) {
                    rrccMejorSim = rrccS;
                    rrccMejor = rrccAg;
                }
            }
            if (rrccMejor >= 0) {
                rrccUsadas.add(rrccMejor);
                rrccPareja.put(rrccE, rrccMejor);
                rrccSimilitud.put(rrccE, rrccMejorSim);
            }
        }
        int rrccSiguienteAgregada = 0;
        for (int rrccE : rrccEliminadas) {
            Integer rrccAg = rrccPareja.get(rrccE);
            if (rrccAg == null) {
                rrccBloques.add(new RRCCBloqueDiff(RRCCTipoCambio.RRCC_ELIMINADO, rrccA.get(rrccE), null,
                        List.of(new RRCCFragmentoDiff(RRCCTipoCambio.RRCC_ELIMINADO, rrccA.get(rrccE))), 0));
                continue;
            }
            while (rrccSiguienteAgregada < rrccAgregadas.size() && rrccAgregadas.get(rrccSiguienteAgregada) < rrccAg) {
                int rrccLibre = rrccAgregadas.get(rrccSiguienteAgregada++);
                if (!rrccUsadas.contains(rrccLibre)) {
                    rrccBloques.add(rrccBloqueAgregado(rrccB.get(rrccLibre)));
                }
            }
            rrccBloques.add(new RRCCBloqueDiff(RRCCTipoCambio.RRCC_MODIFICADO, rrccA.get(rrccE), rrccB.get(rrccAg),
                    rrccDiffPalabras(rrccA.get(rrccE), rrccB.get(rrccAg)), rrccSimilitud.get(rrccE)));
        }
        while (rrccSiguienteAgregada < rrccAgregadas.size()) {
            int rrccLibre = rrccAgregadas.get(rrccSiguienteAgregada++);
            if (!rrccUsadas.contains(rrccLibre)) {
                rrccBloques.add(rrccBloqueAgregado(rrccB.get(rrccLibre)));
            }
        }
        rrccEliminadas.clear();
        rrccAgregadas.clear();
    }

    private RRCCBloqueDiff rrccBloqueAgregado(String rrccOracion) {
        return new RRCCBloqueDiff(RRCCTipoCambio.RRCC_AGREGADO, null, rrccOracion,
                List.of(new RRCCFragmentoDiff(RRCCTipoCambio.RRCC_AGREGADO, rrccOracion)), 0);
    }

    private Set<String> rrccConjunto(String rrccOracion) {
        Set<String> rrccS = new HashSet<>();
        for (String rrccP : rrccTexto.rrccPalabras(rrccOracion)) {
            rrccS.add(rrccTexto.rrccNormalizar(rrccP));
        }
        for (String rrccNumero : rrccOracion.split("[^0-9]+")) {
            if (!rrccNumero.isEmpty()) {
                rrccS.add(rrccNumero);
            }
        }
        return rrccS;
    }

    public List<RRCCFragmentoDiff> rrccDiffPalabras(String rrccAntes, String rrccDespues) {
        String[] rrccA = rrccAntes.split("\\s+");
        String[] rrccB = rrccDespues.split("\\s+");
        List<String> rrccClavesA = Arrays.stream(rrccA).map(rrccX -> rrccTexto.rrccNormalizar(rrccX).replaceAll("[^\\p{L}\\p{N}]", "")).toList();
        List<String> rrccClavesB = Arrays.stream(rrccB).map(rrccX -> rrccTexto.rrccNormalizar(rrccX).replaceAll("[^\\p{L}\\p{N}]", "")).toList();
        List<RRCCFragmentoDiff> rrccFragmentos = new ArrayList<>();
        for (int[] rrccOp : rrccLcs(rrccClavesA, rrccClavesB)) {
            RRCCTipoCambio rrccTipo = rrccOp[0] == 0 ? RRCCTipoCambio.RRCC_IGUAL
                    : rrccOp[0] == -1 ? RRCCTipoCambio.RRCC_ELIMINADO : RRCCTipoCambio.RRCC_AGREGADO;
            String rrccPalabra = rrccOp[0] == -1 ? rrccA[rrccOp[1]] : rrccB[rrccOp[2]];
            if (!rrccFragmentos.isEmpty() && rrccFragmentos.get(rrccFragmentos.size() - 1).rrccTipo() == rrccTipo) {
                RRCCFragmentoDiff rrccUltimo = rrccFragmentos.remove(rrccFragmentos.size() - 1);
                rrccFragmentos.add(new RRCCFragmentoDiff(rrccTipo, rrccUltimo.rrccTexto() + " " + rrccPalabra));
            } else {
                rrccFragmentos.add(new RRCCFragmentoDiff(rrccTipo, rrccPalabra));
            }
        }
        return rrccFragmentos;
    }

    private List<String> rrccExclusivos(Map<String, Integer> rrccEn, Map<String, Integer> rrccNoEn) {
        return rrccEn.entrySet().stream()
                .filter(rrccE -> !rrccNoEn.containsKey(rrccE.getKey()))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(RRCC_TERMINOS_REPORTE)
                .map(Map.Entry::getKey)
                .toList();
    }
}
