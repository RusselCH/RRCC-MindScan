package com.ejemplo.rrccmindscan.service;

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
public class RRCCExtraccionService {

    public static final String RRCC_PERSONA = "Persona";
    public static final String RRCC_ORGANIZACION = "Organización";
    public static final String RRCC_LUGAR = "Lugar";
    public static final String RRCC_OTRO = "Otro";
    public static final int RRCC_MAX_ENTIDADES = 60;

    public static final Map<String, Integer> RRCC_MESES = Map.ofEntries(
            Map.entry("enero", 1), Map.entry("febrero", 2), Map.entry("marzo", 3), Map.entry("abril", 4),
            Map.entry("mayo", 5), Map.entry("junio", 6), Map.entry("julio", 7), Map.entry("agosto", 8),
            Map.entry("septiembre", 9), Map.entry("setiembre", 9), Map.entry("octubre", 10),
            Map.entry("noviembre", 11), Map.entry("diciembre", 12));

    public static final Set<String> RRCC_TITULOS = Set.of("ing", "dr", "dra", "mg", "lic", "sr", "sra", "srta",
            "señor", "señora", "senor", "senora", "abog", "prof", "arq", "don", "doña");
    public static final Set<String> RRCC_MARCAS_ORGANIZACION = Set.of("universidad", "escuela", "facultad", "dirección",
            "direccion", "oficina", "comité", "comite", "ministerio", "municipalidad", "empresa", "s.a.c", "s.a.c.", "sac",
            "s.a.", "e.i.r.l", "eirl", "instituto", "colegio", "gerencia", "área", "area", "consejo", "asociación",
            "asociacion", "banco", "sunat", "sociedad", "servicios", "corporación", "corporacion");
    public static final Set<String> RRCC_LUGARES = Set.of("juliaca", "puno", "lima", "arequipa", "cusco", "tacna",
            "perú", "peru", "moquegua", "trujillo", "chiclayo", "piura", "ica", "huancayo", "ayacucho");
    public static final Set<String> RRCC_CONECTORES = Set.of("de", "del", "la", "las", "los", "y", "e");
    public static final Set<String> RRCC_IGNORAR = Set.of("el", "la", "los", "las", "un", "una", "en", "se", "para",
            "por", "con", "durante", "sin", "este", "esta", "cada", "cualquier", "artículo", "articulo", "cláusula",
            "clausula", "acuerdo", "asistentes", "agenda", "responsable", "correo", "contacto", "objeto", "plazo",
            "monto", "horario", "acceso", "reservas", "equipos", "daños", "sanciones", "vigencia", "antecedentes",
            "diagnóstico", "diagnostico", "presupuesto", "recomendaciones", "conste", "siendo", "también", "sin embargo",
            "ruc", "dni", "igv", "ram", "mbps", "gb", "mb", "kb", "pdf", "usb", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo", "sábados", "domingos");

    private static final Pattern RRCC_FECHA_NUMERICA = Pattern.compile("(?<![\\d/.-])(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})(?![\\d/-])");
    private static final Pattern RRCC_FECHA_TEXTO = Pattern.compile("(?iu)(?<!\\d)(\\d{1,2})\\s+de\\s+(\\p{L}+)(?:\\s+(?:de|del)\\s+(\\d{4}))?");
    private static final Pattern RRCC_ANIO = Pattern.compile("(?<!\\d)(19|20)\\d{2}(?!\\d)");

    private final RRCCTextoService rrccTexto;

    public record RRCCEvento(LocalDate rrccFecha, String rrccTextoFecha, String rrccOracion, int rrccPosicion,
                             boolean rrccAnioDeducido) {
    }

    public record RRCCEntidad(String rrccNombre, String rrccTipo, int rrccVeces, String rrccContexto, int rrccPosicion) {
    }

    public List<RRCCEvento> rrccCronologia(String rrccContenido) {
        String rrccT = rrccContenido == null ? "" : rrccContenido;
        List<RRCCOracion> rrccOraciones = rrccTexto.rrccOraciones(rrccT);
        List<RRCCEvento> rrccEventos = new ArrayList<>();
        Set<String> rrccVistos = new HashSet<>();
        int rrccAnioReferencia = rrccAnioDominante(rrccT);
        Matcher rrccM = RRCC_FECHA_NUMERICA.matcher(rrccT);
        while (rrccM.find()) {
            LocalDate rrccF = rrccCrear(Integer.parseInt(rrccM.group(3)), Integer.parseInt(rrccM.group(2)), Integer.parseInt(rrccM.group(1)));
            if (rrccF != null) {
                rrccAgregar(rrccEventos, rrccVistos, rrccF, rrccM.group(), rrccOraciones, rrccM.start(), false);
            }
        }
        Matcher rrccTx = RRCC_FECHA_TEXTO.matcher(rrccT);
        while (rrccTx.find()) {
            Integer rrccMes = RRCC_MESES.get(rrccTexto.rrccNormalizar(rrccTx.group(2)));
            if (rrccMes == null) {
                continue;
            }
            boolean rrccDeducido = rrccTx.group(3) == null;
            int rrccAnio = rrccDeducido ? rrccAnioCercano(rrccT, rrccTx.end(), rrccAnioReferencia) : Integer.parseInt(rrccTx.group(3));
            if (rrccAnio <= 0) {
                continue;
            }
            LocalDate rrccF = rrccCrear(rrccAnio, rrccMes, Integer.parseInt(rrccTx.group(1)));
            if (rrccF != null) {
                rrccAgregar(rrccEventos, rrccVistos, rrccF, rrccTx.group(), rrccOraciones, rrccTx.start(), rrccDeducido);
            }
        }
        rrccEventos.sort(Comparator.comparing(RRCCEvento::rrccFecha).thenComparingInt(RRCCEvento::rrccPosicion));
        return rrccEventos;
    }

    private void rrccAgregar(List<RRCCEvento> rrccEventos, Set<String> rrccVistos, LocalDate rrccF, String rrccTextoFecha,
                             List<RRCCOracion> rrccOraciones, int rrccPosicion, boolean rrccDeducido) {
        RRCCOracion rrccO = rrccOracionEn(rrccOraciones, rrccPosicion);
        String rrccOracion = rrccO == null ? rrccTextoFecha : rrccO.rrccTexto();
        if (rrccVistos.add(rrccF + "|" + rrccOracion)) {
            rrccEventos.add(new RRCCEvento(rrccF, rrccTextoFecha.strip(), rrccOracion, rrccPosicion, rrccDeducido));
        }
    }

    private RRCCOracion rrccOracionEn(List<RRCCOracion> rrccOraciones, int rrccPosicion) {
        for (RRCCOracion rrccO : rrccOraciones) {
            if (rrccPosicion >= rrccO.rrccInicio() && rrccPosicion < rrccO.rrccFin()) {
                return rrccO;
            }
        }
        return null;
    }

    private LocalDate rrccCrear(int rrccAnio, int rrccMes, int rrccDia) {
        try {
            return LocalDate.of(rrccAnio, rrccMes, rrccDia);
        } catch (DateTimeException rrccError) {
            return null;
        }
    }

    private int rrccAnioDominante(String rrccT) {
        Map<Integer, Integer> rrccConteo = new HashMap<>();
        Matcher rrccM = RRCC_ANIO.matcher(rrccT);
        while (rrccM.find()) {
            rrccConteo.merge(Integer.parseInt(rrccM.group()), 1, Integer::sum);
        }
        return rrccConteo.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(-1);
    }

    private int rrccAnioCercano(String rrccT, int rrccDesde, int rrccPorDefecto) {
        Matcher rrccM = RRCC_ANIO.matcher(rrccT);
        int rrccMejor = -1;
        int rrccDistancia = Integer.MAX_VALUE;
        while (rrccM.find()) {
            int rrccD = Math.abs(rrccM.start() - rrccDesde);
            if (rrccD < rrccDistancia && rrccD < 160) {
                rrccDistancia = rrccD;
                rrccMejor = Integer.parseInt(rrccM.group());
            }
        }
        return rrccMejor > 0 ? rrccMejor : rrccPorDefecto;
    }

    public List<RRCCEntidad> rrccEntidades(String rrccContenido) {
        String rrccT = rrccContenido == null ? "" : rrccContenido;
        Map<String, int[]> rrccConteo = new LinkedHashMap<>();
        Map<String, String> rrccTipos = new HashMap<>();
        Map<String, String> rrccNombres = new HashMap<>();
        int rrccCursor = 0;
        for (String rrccLinea : rrccT.split("\n", -1)) {
            int rrccBase = rrccCursor;
            rrccCursor += rrccLinea.length() + 1;
            if (rrccLinea.strip().length() > 3 && rrccLinea.strip().equals(rrccLinea.strip().toUpperCase(Locale.ROOT))) {
                continue;
            }
            List<int[]> rrccPalabras = new ArrayList<>();
            Matcher rrccM = Pattern.compile("[\\p{L}.]+").matcher(rrccLinea);
            while (rrccM.find()) {
                rrccPalabras.add(new int[]{rrccM.start(), rrccM.end()});
            }
            int rrccI = 0;
            while (rrccI < rrccPalabras.size()) {
                String rrccP = rrccLinea.substring(rrccPalabras.get(rrccI)[0], rrccPalabras.get(rrccI)[1]);
                if (!rrccEsMayuscula(rrccP) || RRCC_TITULOS.contains(rrccTexto.rrccNormalizar(rrccP).replace(".", ""))) {
                    rrccI++;
                    continue;
                }
                int rrccFin = rrccI;
                int rrccJ = rrccI + 1;
                while (rrccJ < rrccPalabras.size() && rrccJ - rrccI < 7) {
                    String rrccQ = rrccLinea.substring(rrccPalabras.get(rrccJ)[0], rrccPalabras.get(rrccJ)[1]);
                    String rrccEntre = rrccLinea.substring(rrccPalabras.get(rrccJ - 1)[1], rrccPalabras.get(rrccJ)[0]);
                    String rrccAnterior = rrccLinea.substring(rrccPalabras.get(rrccJ - 1)[0], rrccPalabras.get(rrccJ - 1)[1]);
                    if (!rrccEntre.isBlank() || (rrccAnterior.endsWith(".") && !rrccAnterior.matches("(\\p{L}\\.)+"))) {
                        break;
                    }
                    if (rrccEsMayuscula(rrccQ)) {
                        rrccFin = rrccJ;
                    } else if (!RRCC_CONECTORES.contains(rrccQ)) {
                        break;
                    }
                    rrccJ++;
                }
                String rrccNombre = rrccLinea.substring(rrccPalabras.get(rrccI)[0], rrccPalabras.get(rrccFin)[1]).replaceAll("[.]+$", "");
                String rrccPrevio = rrccI > 0 ? rrccTexto.rrccNormalizar(rrccLinea.substring(rrccPalabras.get(rrccI - 1)[0],
                        rrccPalabras.get(rrccI - 1)[1])).replace(".", "") : "";
                boolean rrccInicioOracion = rrccI == 0 || rrccLinea.substring(0, rrccPalabras.get(rrccI)[0]).strip().matches(".*[.:;?!]$");
                int rrccPiezas = rrccFin - rrccI + 1;
                String rrccNorm = rrccTexto.rrccNormalizar(rrccNombre);
                boolean rrccSigla = rrccNombre.length() >= 2 && rrccNombre.replace(".", "").chars().filter(Character::isUpperCase).count() >= 2
                        && rrccPiezas == 1;
                boolean rrccValida = !RRCC_IGNORAR.contains(rrccNorm) && rrccNombre.length() > 2
                        && (rrccPiezas >= 2 || rrccSigla || RRCC_TITULOS.contains(rrccPrevio) || RRCC_LUGARES.contains(rrccNorm)
                        || (!rrccInicioOracion && rrccNombre.length() > 3));
                if (rrccValida) {
                    String rrccTipo = rrccClasificar(rrccNombre, rrccPrevio);
                    int[] rrccDatos = rrccConteo.computeIfAbsent(rrccNorm, rrccK -> new int[]{0, 0});
                    if (rrccDatos[0] == 0) {
                        rrccDatos[1] = rrccBase + rrccPalabras.get(rrccI)[0];
                        rrccNombres.put(rrccNorm, rrccNombre);
                    }
                    rrccDatos[0]++;
                    if (!RRCC_OTRO.equals(rrccTipo) || !rrccTipos.containsKey(rrccNorm)) {
                        rrccTipos.put(rrccNorm, rrccTipo);
                    }
                }
                rrccI = rrccFin + 1;
            }
        }
        List<RRCCEntidad> rrccLista = new ArrayList<>();
        for (Map.Entry<String, int[]> rrccE : rrccConteo.entrySet()) {
            if (RRCC_OTRO.equals(rrccTipos.get(rrccE.getKey())) && rrccE.getValue()[0] < 2) {
                continue;
            }
            int rrccPos = rrccE.getValue()[1];
            String rrccNombre = rrccNombres.get(rrccE.getKey());
            rrccLista.add(new RRCCEntidad(rrccNombre, rrccTipos.get(rrccE.getKey()), rrccE.getValue()[0],
                    rrccContexto(rrccT, rrccPos, rrccPos + rrccNombre.length()), rrccPos));
        }
        rrccLista.sort(Comparator.comparingInt((RRCCEntidad rrccE) -> rrccOrdenTipo(rrccE.rrccTipo()))
                .thenComparing(Comparator.comparingInt(RRCCEntidad::rrccVeces).reversed())
                .thenComparing(RRCCEntidad::rrccNombre));
        return rrccLista.subList(0, Math.min(RRCC_MAX_ENTIDADES, rrccLista.size()));
    }

    private int rrccOrdenTipo(String rrccTipo) {
        return switch (rrccTipo) {
            case RRCC_PERSONA -> 0;
            case RRCC_ORGANIZACION -> 1;
            case RRCC_LUGAR -> 2;
            default -> 3;
        };
    }

    private boolean rrccEsMayuscula(String rrccPalabra) {
        return !rrccPalabra.isEmpty() && Character.isUpperCase(rrccPalabra.charAt(0));
    }

    private String rrccClasificar(String rrccNombre, String rrccPrevio) {
        String rrccNorm = rrccTexto.rrccNormalizar(rrccNombre);
        for (String rrccMarca : RRCC_MARCAS_ORGANIZACION) {
            if (rrccNorm.contains(rrccTexto.rrccNormalizar(rrccMarca))) {
                return RRCC_ORGANIZACION;
            }
        }
        if (RRCC_LUGARES.contains(rrccNorm) || rrccNorm.matches("(carretera|avenida|av|jiron|jr|calle|pasaje|plaza|distrito|provincia|region) .*")) {
            return RRCC_LUGAR;
        }
        if (RRCC_TITULOS.contains(rrccPrevio)) {
            return RRCC_PERSONA;
        }
        String[] rrccPartes = rrccNombre.split("\\s+");
        boolean rrccTodasNombres = rrccPartes.length >= 2 && rrccPartes.length <= 5
                && Arrays.stream(rrccPartes).allMatch(rrccP -> rrccP.length() > 1 && Character.isUpperCase(rrccP.charAt(0))
                && rrccP.substring(1).chars().allMatch(Character::isLowerCase));
        if (rrccTodasNombres) {
            return RRCC_PERSONA;
        }
        if (rrccNombre.replace(".", "").chars().filter(Character::isUpperCase).count() >= 2 && !rrccNombre.contains(" ")) {
            return RRCC_ORGANIZACION;
        }
        return RRCC_OTRO;
    }

    private String rrccContexto(String rrccT, int rrccIni, int rrccFin) {
        int rrccDesde = Math.max(0, rrccIni - 50);
        int rrccHasta = Math.min(rrccT.length(), rrccFin + 50);
        return (rrccDesde > 0 ? "..." : "") + rrccT.substring(rrccDesde, rrccHasta).replaceAll("\\s+", " ").strip()
                + (rrccHasta < rrccT.length() ? "..." : "");
    }
}
