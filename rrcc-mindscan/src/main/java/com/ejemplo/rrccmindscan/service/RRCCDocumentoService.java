package com.ejemplo.rrccmindscan.service;

import com.ejemplo.rrccmindscan.exception.RRCCNegocioException;
import com.ejemplo.rrccmindscan.model.RRCCCategoria;
import com.ejemplo.rrccmindscan.model.RRCCDocumento;
import com.ejemplo.rrccmindscan.model.RRCCTipoDocumento;
import com.ejemplo.rrccmindscan.repository.RRCCComparacionRepository;
import com.ejemplo.rrccmindscan.repository.RRCCDocumentoRepository;
import com.ejemplo.rrccmindscan.repository.RRCCRevisionRepository;
import com.ejemplo.rrccmindscan.service.RRCCExtractorService.RRCCTextoExtraido;
import com.ejemplo.rrccmindscan.service.RRCCTextoService.RRCCEstadisticas;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class RRCCDocumentoService {

    public static final int RRCC_MAX_ARCHIVOS_CARPETA = 300;
    public static final int RRCC_PROFUNDIDAD_CARPETA = 4;

    private final RRCCDocumentoRepository rrccRepositorio;
    private final RRCCComparacionRepository rrccComparacionRepositorio;
    private final RRCCRevisionRepository rrccRevisionRepositorio;
    private final RRCCExtractorService rrccExtractor;
    private final RRCCTextoService rrccTexto;
    private final RRCCAnalizadorService rrccAnalizador;

    public record RRCCResultadoImportacion(List<RRCCDocumento> rrccImportados, List<String> rrccErrores, int rrccDuplicados) {
    }

    public record RRCCResumenBiblioteca(int rrccDocumentos, long rrccPalabras, long rrccPaginas, long rrccBytes,
                                        double rrccLegibilidadPromedio, Map<RRCCCategoria, Integer> rrccPorCategoria,
                                        Map<RRCCTipoDocumento, Integer> rrccPorTipo) {
    }

    public List<RRCCDocumento> rrccListar() {
        return rrccRepositorio.rrccListarTodos();
    }

    public RRCCDocumento rrccObtener(Long rrccId) {
        return rrccRepositorio.findById(rrccId)
                .orElseThrow(() -> new RRCCNegocioException("El documento ya no existe en la biblioteca."));
    }

    public long rrccContar() {
        return rrccRepositorio.rrccContarTodos();
    }

    public List<File> rrccExpandir(List<File> rrccEntradas) {
        List<File> rrccArchivos = new ArrayList<>();
        for (File rrccF : rrccEntradas) {
            rrccRecorrer(rrccF, 0, rrccArchivos);
        }
        return rrccArchivos;
    }

    private void rrccRecorrer(File rrccF, int rrccNivel, List<File> rrccArchivos) {
        if (rrccArchivos.size() >= RRCC_MAX_ARCHIVOS_CARPETA || rrccF == null || !rrccF.exists()) {
            return;
        }
        if (rrccF.isDirectory()) {
            if (rrccNivel > RRCC_PROFUNDIDAD_CARPETA) {
                return;
            }
            File[] rrccHijos = rrccF.listFiles();
            if (rrccHijos == null) {
                return;
            }
            Arrays.sort(rrccHijos, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
            for (File rrccH : rrccHijos) {
                rrccRecorrer(rrccH, rrccNivel + 1, rrccArchivos);
            }
        } else if (RRCCTipoDocumento.rrccDesdeNombre(rrccF.getName()) != null && !rrccF.getName().startsWith("~$")) {
            rrccArchivos.add(rrccF);
        }
    }

    public RRCCResultadoImportacion rrccImportar(List<File> rrccEntradas, Consumer<Double> rrccProgreso) {
        List<File> rrccArchivos = rrccExpandir(rrccEntradas);
        List<RRCCDocumento> rrccImportados = new ArrayList<>();
        List<String> rrccErrores = new ArrayList<>();
        int rrccDuplicados = 0;
        if (rrccArchivos.isEmpty()) {
            rrccErrores.add("No se encontraron archivos PDF, DOCX o TXT.");
        }
        for (int rrccI = 0; rrccI < rrccArchivos.size(); rrccI++) {
            File rrccF = rrccArchivos.get(rrccI);
            try {
                RRCCTextoExtraido rrccExtraido = rrccExtractor.rrccExtraer(rrccF);
                if (rrccRepositorio.rrccContarPorHash(rrccExtraido.rrccHash()) > 0) {
                    rrccDuplicados++;
                } else {
                    rrccImportados.add(rrccGuardar(rrccF.getName(), rrccF.getAbsolutePath(), rrccExtraido));
                }
            } catch (RRCCNegocioException rrccError) {
                rrccErrores.add(rrccError.getMessage());
            } catch (RuntimeException rrccError) {
                rrccErrores.add(rrccF.getName() + ": " + rrccError.getClass().getSimpleName()
                        + (rrccError.getMessage() == null ? "" : " - " + rrccError.getMessage()));
            }
            if (rrccProgreso != null) {
                rrccProgreso.accept((rrccI + 1.0) / rrccArchivos.size());
            }
        }
        return new RRCCResultadoImportacion(rrccImportados, rrccErrores, rrccDuplicados);
    }

    public RRCCDocumento rrccImportarTexto(String rrccNombre, String rrccContenido) {
        String rrccLimpio = rrccTexto.rrccLimpiar(rrccContenido);
        byte[] rrccBytes = rrccContenido.getBytes(StandardCharsets.UTF_8);
        int rrccPaginas = Math.max(1, (int) Math.ceil(rrccTexto.rrccPalabras(rrccLimpio).size()
                / (double) RRCCExtractorService.RRCC_PALABRAS_POR_PAGINA));
        RRCCTextoExtraido rrccExtraido = new RRCCTextoExtraido(rrccLimpio, rrccPaginas, RRCCTipoDocumento.RRCC_TEXTO,
                rrccBytes.length, rrccTexto.rrccHash(rrccBytes));
        return rrccGuardar(rrccNombre, null, rrccExtraido);
    }

    private RRCCDocumento rrccGuardar(String rrccNombre, String rrccRuta, RRCCTextoExtraido rrccExtraido) {
        RRCCEstadisticas rrccEst = rrccTexto.rrccEstadisticas(rrccExtraido.rrccTexto());
        RRCCDocumento rrccDoc = RRCCDocumento.builder()
                .rrccNombre(rrccNombre.length() > 250 ? rrccNombre.substring(0, 250) : rrccNombre)
                .rrccRuta(rrccRuta)
                .rrccTipo(rrccExtraido.rrccTipo())
                .rrccCategoria(rrccAnalizador.rrccClasificar(rrccExtraido.rrccTexto()).rrccCategoria())
                .rrccTamanoBytes(rrccExtraido.rrccTamano())
                .rrccPaginas(rrccExtraido.rrccPaginas())
                .rrccPalabras(rrccEst.rrccPalabras())
                .rrccOraciones(rrccEst.rrccOraciones())
                .rrccLegibilidad(rrccEst.rrccLegibilidad())
                .rrccHash(rrccExtraido.rrccHash())
                .rrccFechaImportacion(LocalDateTime.now())
                .rrccContenido(rrccExtraido.rrccTexto())
                .build();
        return rrccRepositorio.save(rrccDoc);
    }

    @Transactional
    public void rrccEliminar(Long rrccId) {
        RRCCDocumento rrccDoc = rrccObtener(rrccId);
        rrccComparacionRepositorio.rrccEliminarDeDocumento(rrccId);
        rrccRevisionRepositorio.rrccEliminarDeDocumento(rrccId);
        rrccRepositorio.delete(rrccDoc);
    }

    @Transactional
    public RRCCDocumento rrccCambiarCategoria(Long rrccId, RRCCCategoria rrccCategoria) {
        if (rrccCategoria == null) {
            throw new RRCCNegocioException("Selecciona una categoría.");
        }
        RRCCDocumento rrccDoc = rrccObtener(rrccId);
        rrccDoc.setRrccCategoria(rrccCategoria);
        return rrccRepositorio.save(rrccDoc);
    }

    @Transactional
    public RRCCDocumento rrccRenombrar(Long rrccId, String rrccNombre) {
        if (rrccNombre == null || rrccNombre.isBlank()) {
            throw new RRCCNegocioException("El nombre no puede quedar vacío.");
        }
        RRCCDocumento rrccDoc = rrccObtener(rrccId);
        rrccDoc.setRrccNombre(rrccNombre.strip());
        return rrccRepositorio.save(rrccDoc);
    }

    @Transactional
    public RRCCDocumento rrccAlternarFavorito(Long rrccId) {
        RRCCDocumento rrccDoc = rrccObtener(rrccId);
        rrccDoc.setRrccFavorito(!rrccDoc.rrccEsFavorito());
        return rrccRepositorio.save(rrccDoc);
    }

    @Transactional
    public RRCCDocumento rrccGuardarNotas(Long rrccId, String rrccNotas) {
        RRCCDocumento rrccDoc = rrccObtener(rrccId);
        rrccDoc.setRrccNotas(rrccNotas == null || rrccNotas.isBlank() ? null : rrccNotas.strip());
        return rrccRepositorio.save(rrccDoc);
    }

    public double[][] rrccMatrizSimilitud(List<RRCCDocumento> rrccDocs) {
        int rrccN = rrccDocs.size();
        List<Map<String, Integer>> rrccConteos = new ArrayList<>();
        for (RRCCDocumento rrccD : rrccDocs) {
            rrccConteos.add(rrccTexto.rrccConteoTerminos(rrccD.getRrccContenido()));
        }
        double[][] rrccMatriz = new double[rrccN][rrccN];
        for (int rrccI = 0; rrccI < rrccN; rrccI++) {
            rrccMatriz[rrccI][rrccI] = 1;
            for (int rrccJ = rrccI + 1; rrccJ < rrccN; rrccJ++) {
                double rrccS = rrccTexto.rrccCoseno(rrccConteos.get(rrccI), rrccConteos.get(rrccJ));
                rrccMatriz[rrccI][rrccJ] = rrccS;
                rrccMatriz[rrccJ][rrccI] = rrccS;
            }
        }
        return rrccMatriz;
    }

    public RRCCResumenBiblioteca rrccResumen(List<RRCCDocumento> rrccDocs) {
        Map<RRCCCategoria, Integer> rrccPorCategoria = new EnumMap<>(RRCCCategoria.class);
        Map<RRCCTipoDocumento, Integer> rrccPorTipo = new EnumMap<>(RRCCTipoDocumento.class);
        long rrccPalabras = 0;
        long rrccPaginas = 0;
        long rrccBytes = 0;
        double rrccLegibilidad = 0;
        for (RRCCDocumento rrccD : rrccDocs) {
            rrccPorCategoria.merge(rrccD.getRrccCategoria(), 1, Integer::sum);
            rrccPorTipo.merge(rrccD.getRrccTipo(), 1, Integer::sum);
            rrccPalabras += rrccD.getRrccPalabras();
            rrccPaginas += rrccD.getRrccPaginas();
            rrccBytes += rrccD.getRrccTamanoBytes();
            rrccLegibilidad += rrccD.getRrccLegibilidad();
        }
        return new RRCCResumenBiblioteca(rrccDocs.size(), rrccPalabras, rrccPaginas, rrccBytes,
                rrccDocs.isEmpty() ? 0 : rrccLegibilidad / rrccDocs.size(), rrccPorCategoria, rrccPorTipo);
    }
}
