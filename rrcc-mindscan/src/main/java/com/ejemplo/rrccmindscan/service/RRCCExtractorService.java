package com.ejemplo.rrccmindscan.service;

import com.ejemplo.rrccmindscan.config.RRCCAnalisisPropiedades;
import com.ejemplo.rrccmindscan.exception.RRCCNegocioException;
import com.ejemplo.rrccmindscan.model.RRCCTipoDocumento;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Service
@RequiredArgsConstructor
public class RRCCExtractorService {

    public static final int RRCC_PALABRAS_POR_PAGINA = 350;

    private final RRCCTextoService rrccTexto;
    private final RRCCAnalisisPropiedades rrccPropiedades;

    public record RRCCTextoExtraido(String rrccTexto, int rrccPaginas, RRCCTipoDocumento rrccTipo, long rrccTamano,
                                    String rrccHash) {
    }

    public RRCCTextoExtraido rrccExtraer(File rrccArchivo) {
        RRCCTipoDocumento rrccTipo = RRCCTipoDocumento.rrccDesdeNombre(rrccArchivo.getName());
        if (rrccTipo == null) {
            throw new RRCCNegocioException(rrccArchivo.getName() + ": formato no soportado (usa PDF, DOCX o TXT).");
        }
        long rrccMaximo = (long) rrccPropiedades.rrccTamanoMaximoMb() * 1024 * 1024;
        if (rrccArchivo.length() > rrccMaximo) {
            throw new RRCCNegocioException(rrccArchivo.getName() + ": supera el máximo de " + rrccPropiedades.rrccTamanoMaximoMb() + " MB.");
        }
        try {
            byte[] rrccBytes = Files.readAllBytes(rrccArchivo.toPath());
            String rrccContenido;
            int rrccPaginas;
            switch (rrccTipo) {
                case RRCC_PDF -> {
                    try (PDDocument rrccPdf = Loader.loadPDF(rrccBytes)) {
                        if (rrccPdf.isEncrypted()) {
                            throw new RRCCNegocioException(rrccArchivo.getName() + ": el PDF está protegido con contraseña.");
                        }
                        PDFTextStripper rrccExtractor = new PDFTextStripper();
                        rrccExtractor.setSortByPosition(true);
                        rrccContenido = rrccExtractor.getText(rrccPdf);
                        rrccPaginas = rrccPdf.getNumberOfPages();
                    }
                }
                case RRCC_WORD -> {
                    rrccContenido = rrccLeerWord(rrccBytes);
                    rrccPaginas = -1;
                }
                default -> {
                    rrccContenido = rrccDecodificar(rrccBytes);
                    rrccPaginas = -1;
                }
            }
            String rrccLimpio = rrccTexto.rrccLimpiar(rrccContenido);
            if (rrccTexto.rrccPalabras(rrccLimpio).size() < 5) {
                throw new RRCCNegocioException(rrccArchivo.getName() + ": no tiene texto seleccionable"
                        + (rrccTipo == RRCCTipoDocumento.RRCC_PDF ? " (parece un PDF escaneado como imagen)." : "."));
            }
            if (rrccPaginas < 1) {
                rrccPaginas = Math.max(1, (int) Math.ceil(rrccTexto.rrccPalabras(rrccLimpio).size() / (double) RRCC_PALABRAS_POR_PAGINA));
            }
            return new RRCCTextoExtraido(rrccLimpio, rrccPaginas, rrccTipo, rrccBytes.length, rrccTexto.rrccHash(rrccBytes));
        } catch (IOException rrccError) {
            throw new RRCCNegocioException(rrccArchivo.getName() + ": no se pudo leer (" + rrccError.getMessage() + ").");
        }
    }

    private String rrccLeerWord(byte[] rrccBytes) throws IOException {
        StringBuilder rrccSalida = new StringBuilder();
        try (XWPFDocument rrccDoc = new XWPFDocument(new ByteArrayInputStream(rrccBytes))) {
            for (IBodyElement rrccElemento : rrccDoc.getBodyElements()) {
                if (rrccElemento instanceof XWPFParagraph rrccParrafo) {
                    String rrccT = rrccParrafo.getText();
                    if (rrccT != null && !rrccT.isBlank()) {
                        rrccSalida.append(rrccT.strip()).append("\n\n");
                    }
                } else if (rrccElemento instanceof XWPFTable rrccTabla) {
                    for (XWPFTableRow rrccFila : rrccTabla.getRows()) {
                        StringBuilder rrccLinea = new StringBuilder();
                        for (XWPFTableCell rrccCelda : rrccFila.getTableCells()) {
                            if (rrccLinea.length() > 0) {
                                rrccLinea.append(" | ");
                            }
                            rrccLinea.append(rrccCelda.getText().strip());
                        }
                        rrccSalida.append(rrccLinea).append("\n");
                    }
                    rrccSalida.append("\n");
                }
            }
        }
        return rrccSalida.toString();
    }

    private String rrccDecodificar(byte[] rrccBytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(rrccBytes)).toString();
        } catch (CharacterCodingException rrccError) {
            return new String(rrccBytes, StandardCharsets.ISO_8859_1);
        }
    }
}
