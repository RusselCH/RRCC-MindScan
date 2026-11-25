package com.ejemplo.rrccmindscan.service;

import com.ejemplo.rrccmindscan.config.RRCCAnalisisPropiedades;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RRCCTextoService {

    public static final Set<String> RRCC_PALABRAS_VACIAS = Set.of(
            "a", "al", "algo", "alguna", "algunas", "alguno", "algunos", "alli", "ante", "antes", "aqui",
            "asi", "aun", "cada", "como", "con", "contra", "cual", "cuales", "cuando", "cuanto", "cuantos",
            "cuantas", "de", "del", "desde", "donde", "dos", "durante", "e", "el", "ella", "ellas", "ellos",
            "en", "entre", "era", "eran", "es", "esa", "esas", "ese", "eso", "esos", "esta", "estaba", "estan",
            "estar", "estas", "este", "esto", "estos", "fue", "fueron", "ha", "haber", "hace", "hacer", "han",
            "hasta", "hay", "la", "las", "le", "les", "lo", "los", "mas", "me", "mediante", "mi", "mis",
            "misma", "mismo", "mucho", "muy", "nada", "ni", "no", "nos", "nosotros", "o", "os", "otra",
            "otras", "otro", "otros", "para", "parte", "pero", "poco", "por", "porque", "puede", "pueden",
            "que", "quien", "quienes", "se", "sea", "segun", "ser", "sera", "seran", "si", "sido", "siempre",
            "sin", "sino", "sobre", "solo", "son", "su", "sus", "tal", "tambien", "tan", "tanto", "te",
            "tiene", "tienen", "toda", "todas", "todo", "todos", "tras", "tres", "tu", "tus", "un", "una",
            "unas", "uno", "unos", "vez", "veces", "y", "ya", "yo", "debe", "deben", "dicho", "dicha", "fin",
            "the", "and", "of", "to", "in", "is", "for", "on", "with", "as", "by", "this", "that");

    public static final Set<String> RRCC_ABREVIATURAS = Set.of(
            "sr", "sra", "srta", "dr", "dra", "ing", "lic", "mg", "art", "arts", "etc", "pag", "pags", "num", "nro",
            "no", "av", "jr", "cia", "s.a", "s.a.c", "aprox", "ej", "fig", "inc", "dpto", "mz", "lt", "tel", "cel");

    public static final Pattern RRCC_PATRON_PALABRA = Pattern.compile("\\p{L}+");
    public static final Pattern RRCC_PATRON_TOKEN = Pattern.compile("[\\p{L}\\p{N}]+");
    public static final Pattern RRCC_PATRON_PARRAFO = Pattern.compile("\\n\\s*\\n");
    public static final Pattern RRCC_PATRON_VOCALES = Pattern.compile("[aeiou]+");
    public static final int RRCC_LARGO_TITULO = 90;
    public static final int RRCC_MINIMO_TERMINO = 3;

    private final RRCCAnalisisPropiedades rrccPropiedades;

    public RRCCTextoService(RRCCAnalisisPropiedades rrccPropiedades) {
        this.rrccPropiedades = rrccPropiedades;
    }

    public record RRCCOracion(int rrccIndice, String rrccTexto, int rrccInicio, int rrccFin) {
    }

    public record RRCCEstadisticas(int rrccPalabras, int rrccOraciones, int rrccParrafos, int rrccCaracteres,
                                   int rrccPalabrasUnicas, double rrccPalabrasPorOracion, double rrccSilabasPorPalabra,
                                   double rrccLegibilidad, String rrccNivel, double rrccMinutosLectura) {
    }

    public String rrccNormalizar(String rrccTexto) {
        if (rrccTexto == null) {
            return "";
        }
        String rrccSinMarcas = Normalizer.normalize(rrccTexto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return rrccSinMarcas.toLowerCase(Locale.ROOT);
    }

    public String rrccLimpiar(String rrccTexto) {
        String rrccT = Normalizer.normalize(rrccTexto == null ? "" : rrccTexto, Normalizer.Form.NFC);
        rrccT = rrccT.replace("\uFEFF", "").replace("\r\n", "\n").replace('\r', '\n').replace(' ', ' ').replace("\t", " ");
        rrccT = rrccT.replaceAll("(\\p{L})-\\n(\\p{Ll})", "$1$2");
        rrccT = rrccT.replaceAll("[ ]+\\n", "\n");
        rrccT = rrccT.replaceAll("\\n{3,}", "\n\n");
        return rrccT.strip();
    }

    public List<String> rrccPalabras(String rrccTexto) {
        List<String> rrccLista = new ArrayList<>();
        Matcher rrccM = RRCC_PATRON_PALABRA.matcher(rrccTexto == null ? "" : rrccTexto);
        while (rrccM.find()) {
            rrccLista.add(rrccM.group());
        }
        return rrccLista;
    }

    public String rrccRaiz(String rrccPalabraNormalizada) {
        String rrccP = rrccPalabraNormalizada;
        if (rrccP.length() > 5 && rrccP.endsWith("es") && !rrccP.endsWith("ies")) {
            return rrccP.substring(0, rrccP.length() - 2);
        }
        if (rrccP.length() > 3 && rrccP.endsWith("s")) {
            return rrccP.substring(0, rrccP.length() - 1);
        }
        return rrccP;
    }

    public String rrccClave(String rrccPalabra) {
        return rrccRaiz(rrccNormalizar(rrccPalabra));
    }

    public Map<String, String> rrccFormasVisibles(Collection<String> rrccClaves, String rrccTextoFuente) {
        Map<String, Map<String, Integer>> rrccCandidatas = new HashMap<>();
        for (String rrccP : rrccPalabras(rrccTextoFuente)) {
            String rrccC = rrccClave(rrccP);
            if (rrccClaves.contains(rrccC)) {
                rrccCandidatas.computeIfAbsent(rrccC, rrccK -> new HashMap<>()).merge(rrccP.toLowerCase(Locale.ROOT), 1, Integer::sum);
            }
        }
        Map<String, String> rrccFormas = new HashMap<>();
        rrccCandidatas.forEach((rrccC, rrccConteo) -> rrccFormas.put(rrccC,
                Collections.max(rrccConteo.entrySet(), Map.Entry.comparingByValue()).getKey()));
        return rrccFormas;
    }

    public List<String> rrccVisibles(List<String> rrccClaves, String rrccTextoFuente) {
        Map<String, String> rrccFormas = rrccFormasVisibles(rrccClaves, rrccTextoFuente);
        return rrccClaves.stream().map(rrccC -> rrccFormas.getOrDefault(rrccC, rrccC)).toList();
    }

    public boolean rrccEsTermino(String rrccPalabraNormalizada) {
        return rrccPalabraNormalizada.length() >= RRCC_MINIMO_TERMINO && !RRCC_PALABRAS_VACIAS.contains(rrccPalabraNormalizada);
    }

    public List<String> rrccTerminos(String rrccTexto) {
        List<String> rrccLista = new ArrayList<>();
        for (String rrccP : rrccPalabras(rrccTexto)) {
            String rrccN = rrccNormalizar(rrccP);
            if (rrccEsTermino(rrccN)) {
                rrccLista.add(rrccRaiz(rrccN));
            }
        }
        return rrccLista;
    }

    public Map<String, Integer> rrccConteoTerminos(String rrccTexto) {
        Map<String, Integer> rrccConteo = new HashMap<>();
        for (String rrccT : rrccTerminos(rrccTexto)) {
            rrccConteo.merge(rrccT, 1, Integer::sum);
        }
        return rrccConteo;
    }

    public List<String> rrccParrafos(String rrccTexto) {
        List<String> rrccLista = new ArrayList<>();
        for (String rrccP : RRCC_PATRON_PARRAFO.split(rrccTexto == null ? "" : rrccTexto)) {
            if (!rrccP.isBlank()) {
                rrccLista.add(rrccP.strip());
            }
        }
        return rrccLista;
    }

    public List<RRCCOracion> rrccOraciones(String rrccTexto) {
        List<RRCCOracion> rrccLista = new ArrayList<>();
        if (rrccTexto == null || rrccTexto.isBlank()) {
            return rrccLista;
        }
        int rrccN = rrccTexto.length();
        int rrccInicio = 0;
        for (int rrccI = 0; rrccI < rrccN; rrccI++) {
            char rrccC = rrccTexto.charAt(rrccI);
            int rrccCorte = -1;
            if (rrccC == '.' || rrccC == '!' || rrccC == '?') {
                int rrccJ = rrccI + 1;
                while (rrccJ < rrccN && ".!?\")»'”".indexOf(rrccTexto.charAt(rrccJ)) >= 0) {
                    rrccJ++;
                }
                boolean rrccFinal = rrccJ >= rrccN || Character.isWhitespace(rrccTexto.charAt(rrccJ));
                if (rrccFinal && !(rrccC == '.' && rrccEsAbreviatura(rrccTexto, rrccInicio, rrccI))) {
                    rrccCorte = rrccJ;
                    rrccI = rrccJ - 1;
                }
            } else if (rrccC == '\n') {
                String rrccSegmento = rrccTexto.substring(rrccInicio, rrccI).strip();
                boolean rrccLineaVacia = rrccI + 1 < rrccN && rrccTexto.substring(rrccI + 1, Math.min(rrccN, rrccI + 3)).contains("\n");
                boolean rrccTitulo = !rrccSegmento.isEmpty() && rrccSegmento.length() <= RRCC_LARGO_TITULO
                        && rrccSegmento.indexOf('\n') < 0 && !rrccSegmento.matches(".*[,;:]$")
                        && rrccI + 1 < rrccN && Character.isUpperCase(rrccSiguienteLetra(rrccTexto, rrccI + 1));
                if (rrccLineaVacia || rrccTitulo) {
                    rrccCorte = rrccI;
                }
            }
            if (rrccCorte >= 0) {
                rrccAgregar(rrccLista, rrccTexto, rrccInicio, rrccCorte);
                rrccInicio = rrccCorte;
            }
        }
        rrccAgregar(rrccLista, rrccTexto, rrccInicio, rrccN);
        return rrccLista;
    }

    private char rrccSiguienteLetra(String rrccTexto, int rrccDesde) {
        for (int rrccI = rrccDesde; rrccI < rrccTexto.length(); rrccI++) {
            char rrccC = rrccTexto.charAt(rrccI);
            if (Character.isLetterOrDigit(rrccC)) {
                return rrccC;
            }
            if (rrccC == '\n') {
                return 'a';
            }
        }
        return 'a';
    }

    private boolean rrccEsAbreviatura(String rrccTexto, int rrccInicio, int rrccPunto) {
        int rrccK = rrccPunto - 1;
        while (rrccK >= rrccInicio && !Character.isWhitespace(rrccTexto.charAt(rrccK)) && rrccTexto.charAt(rrccK) != '(') {
            rrccK--;
        }
        String rrccPalabra = rrccNormalizar(rrccTexto.substring(rrccK + 1, rrccPunto));
        if (rrccPalabra.isEmpty()) {
            return false;
        }
        if (rrccPalabra.length() == 1 && Character.isLetter(rrccPalabra.charAt(0))) {
            return true;
        }
        return RRCC_ABREVIATURAS.contains(rrccPalabra);
    }

    private void rrccAgregar(List<RRCCOracion> rrccLista, String rrccTexto, int rrccDesde, int rrccHasta) {
        if (rrccHasta <= rrccDesde) {
            return;
        }
        String rrccBruto = rrccTexto.substring(rrccDesde, rrccHasta);
        String rrccLimpio = rrccBruto.replaceAll("\\s+", " ").strip();
        if (rrccLimpio.isEmpty() || !RRCC_PATRON_TOKEN.matcher(rrccLimpio).find()) {
            return;
        }
        int rrccDesplazamiento = 0;
        while (rrccDesplazamiento < rrccBruto.length() && Character.isWhitespace(rrccBruto.charAt(rrccDesplazamiento))) {
            rrccDesplazamiento++;
        }
        rrccLista.add(new RRCCOracion(rrccLista.size(), rrccLimpio, rrccDesde + rrccDesplazamiento, rrccHasta));
    }

    public int rrccSilabas(String rrccPalabra) {
        String rrccN = rrccNormalizar(rrccPalabra).replace('y', 'i');
        Matcher rrccM = RRCC_PATRON_VOCALES.matcher(rrccN);
        int rrccGrupos = 0;
        while (rrccM.find()) {
            String rrccGrupo = rrccM.group();
            rrccGrupos += rrccGrupo.length() >= 2 && rrccHayHiato(rrccGrupo) ? 2 : 1;
        }
        return Math.max(1, rrccGrupos);
    }

    private boolean rrccHayHiato(String rrccGrupo) {
        int rrccFuertes = 0;
        for (char rrccC : rrccGrupo.toCharArray()) {
            if ("aeo".indexOf(rrccC) >= 0) {
                rrccFuertes++;
            }
        }
        return rrccFuertes >= 2;
    }

    public RRCCEstadisticas rrccEstadisticas(String rrccTexto) {
        List<String> rrccPalabras = rrccPalabras(rrccTexto);
        int rrccTotal = rrccPalabras.size();
        int rrccOraciones = Math.max(1, rrccOraciones(rrccTexto).size());
        int rrccParrafos = rrccParrafos(rrccTexto).size();
        Set<String> rrccUnicas = new HashSet<>();
        long rrccSilabas = 0;
        for (String rrccP : rrccPalabras) {
            rrccUnicas.add(rrccNormalizar(rrccP));
            rrccSilabas += rrccSilabas(rrccP);
        }
        double rrccPorOracion = rrccTotal == 0 ? 0 : (double) rrccTotal / rrccOraciones;
        double rrccSilabasPorPalabra = rrccTotal == 0 ? 0 : (double) rrccSilabas / rrccTotal;
        double rrccLegibilidad = rrccTotal == 0 ? 0
                : 206.84 - 0.60 * (rrccSilabasPorPalabra * 100) - 1.02 * rrccPorOracion;
        rrccLegibilidad = Math.max(0, Math.min(100, rrccLegibilidad));
        double rrccMinutos = rrccTotal / (double) rrccPropiedades.rrccPalabrasPorMinuto();
        return new RRCCEstadisticas(rrccTotal, rrccTotal == 0 ? 0 : rrccOraciones, rrccParrafos,
                rrccTexto == null ? 0 : rrccTexto.length(), rrccUnicas.size(), rrccRedondear(rrccPorOracion),
                rrccRedondear(rrccSilabasPorPalabra), rrccRedondear(rrccLegibilidad), rrccNivelLectura(rrccLegibilidad),
                rrccRedondear(rrccMinutos));
    }

    public String rrccNivelLectura(double rrccLegibilidad) {
        if (rrccLegibilidad >= 90) {
            return "Muy fácil";
        }
        if (rrccLegibilidad >= 80) {
            return "Fácil";
        }
        if (rrccLegibilidad >= 70) {
            return "Algo fácil";
        }
        if (rrccLegibilidad >= 60) {
            return "Normal";
        }
        if (rrccLegibilidad >= 50) {
            return "Algo difícil";
        }
        if (rrccLegibilidad >= 30) {
            return "Difícil";
        }
        return "Muy difícil";
    }

    public String rrccHash(byte[] rrccBytes) {
        try {
            byte[] rrccDigest = MessageDigest.getInstance("SHA-256").digest(rrccBytes);
            StringBuilder rrccHex = new StringBuilder();
            for (byte rrccB : rrccDigest) {
                rrccHex.append(String.format("%02x", rrccB));
            }
            return rrccHex.toString();
        } catch (NoSuchAlgorithmException rrccError) {
            throw new IllegalStateException(rrccError);
        }
    }

    public String rrccHash(String rrccTexto) {
        return rrccHash(rrccTexto.getBytes(StandardCharsets.UTF_8));
    }

    public Set<String> rrccTejas(String rrccTexto, int rrccK) {
        List<String> rrccT = new ArrayList<>();
        for (String rrccP : rrccPalabras(rrccTexto)) {
            rrccT.add(rrccNormalizar(rrccP));
        }
        Set<String> rrccTejas = new HashSet<>();
        for (int rrccI = 0; rrccI + rrccK <= rrccT.size(); rrccI++) {
            rrccTejas.add(String.join(" ", rrccT.subList(rrccI, rrccI + rrccK)));
        }
        if (rrccTejas.isEmpty() && !rrccT.isEmpty()) {
            rrccTejas.add(String.join(" ", rrccT));
        }
        return rrccTejas;
    }

    public double rrccJaccard(Set<String> rrccA, Set<String> rrccB) {
        if (rrccA.isEmpty() && rrccB.isEmpty()) {
            return 1.0;
        }
        int rrccComunes = 0;
        Set<String> rrccMenor = rrccA.size() <= rrccB.size() ? rrccA : rrccB;
        Set<String> rrccMayor = rrccMenor == rrccA ? rrccB : rrccA;
        for (String rrccX : rrccMenor) {
            if (rrccMayor.contains(rrccX)) {
                rrccComunes++;
            }
        }
        return (double) rrccComunes / (rrccA.size() + rrccB.size() - rrccComunes);
    }

    public double rrccCoseno(Map<String, Integer> rrccA, Map<String, Integer> rrccB) {
        if (rrccA.isEmpty() || rrccB.isEmpty()) {
            return 0.0;
        }
        double rrccProducto = 0;
        for (Map.Entry<String, Integer> rrccE : rrccA.entrySet()) {
            Integer rrccOtro = rrccB.get(rrccE.getKey());
            if (rrccOtro != null) {
                rrccProducto += (double) rrccE.getValue() * rrccOtro;
            }
        }
        return rrccProducto / (rrccNorma(rrccA) * rrccNorma(rrccB));
    }

    private double rrccNorma(Map<String, Integer> rrccV) {
        double rrccSuma = 0;
        for (int rrccX : rrccV.values()) {
            rrccSuma += (double) rrccX * rrccX;
        }
        return Math.sqrt(rrccSuma);
    }

    public int rrccLevenshtein(String rrccA, String rrccB, int rrccMaximo) {
        if (Math.abs(rrccA.length() - rrccB.length()) > rrccMaximo) {
            return rrccMaximo + 1;
        }
        int[] rrccPrevia = new int[rrccB.length() + 1];
        int[] rrccActual = new int[rrccB.length() + 1];
        for (int rrccJ = 0; rrccJ <= rrccB.length(); rrccJ++) {
            rrccPrevia[rrccJ] = rrccJ;
        }
        for (int rrccI = 1; rrccI <= rrccA.length(); rrccI++) {
            rrccActual[0] = rrccI;
            int rrccMinimoFila = rrccActual[0];
            for (int rrccJ = 1; rrccJ <= rrccB.length(); rrccJ++) {
                int rrccCosto = rrccA.charAt(rrccI - 1) == rrccB.charAt(rrccJ - 1) ? 0 : 1;
                rrccActual[rrccJ] = Math.min(Math.min(rrccActual[rrccJ - 1] + 1, rrccPrevia[rrccJ] + 1), rrccPrevia[rrccJ - 1] + rrccCosto);
                rrccMinimoFila = Math.min(rrccMinimoFila, rrccActual[rrccJ]);
            }
            if (rrccMinimoFila > rrccMaximo) {
                return rrccMaximo + 1;
            }
            int[] rrccTemporal = rrccPrevia;
            rrccPrevia = rrccActual;
            rrccActual = rrccTemporal;
        }
        return rrccPrevia[rrccB.length()];
    }

    public String rrccRecortar(String rrccTexto, int rrccMaximo) {
        if (rrccTexto == null) {
            return "";
        }
        String rrccT = rrccTexto.replaceAll("\\s+", " ").strip();
        return rrccT.length() <= rrccMaximo ? rrccT : rrccT.substring(0, Math.max(0, rrccMaximo - 3)).strip() + "...";
    }

    private static double rrccRedondear(double rrccValor) {
        return Math.round(rrccValor * 10.0) / 10.0;
    }
}
