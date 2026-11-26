package com.ejemplo.rrccmindscan.service;

import com.ejemplo.rrccmindscan.exception.RRCCNegocioException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.util.Matrix;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class RRCCPdfService {

    public static final float RRCC_MARGEN = 56f;
    public static final float RRCC_TAMANO_TEXTO = 11f;
    public static final float RRCC_TAMANO_TITULO = 15f;
    public static final float RRCC_INTERLINEA = 1.45f;
    public static final int RRCC_LADO_MAXIMO_COMPRESION = 1600;

    public record RRCCResultadoPdf(List<File> rrccArchivos, long rrccBytesAntes, long rrccBytesDespues, String rrccResumen) {
    }

    public int rrccContarPaginas(File rrccPdf) {
        try (PDDocument rrccDoc = rrccAbrir(rrccPdf, null)) {
            return rrccDoc.getNumberOfPages();
        } catch (IOException rrccError) {
            throw new UncheckedIOException(rrccError);
        }
    }

    public boolean rrccEstaProtegido(File rrccPdf) {
        try (PDDocument rrccDoc = Loader.loadPDF(rrccPdf)) {
            return rrccDoc.isEncrypted();
        } catch (InvalidPasswordException rrccError) {
            return true;
        } catch (IOException rrccError) {
            throw new UncheckedIOException(rrccError);
        }
    }

    public RRCCResultadoPdf rrccUnir(List<File> rrccEntradas, File rrccDestino) {
        if (rrccEntradas.size() < 2) {
            throw new RRCCNegocioException("Elige al menos dos PDF para unir.");
        }
        try {
            PDFMergerUtility rrccUnion = new PDFMergerUtility();
            for (File rrccF : rrccEntradas) {
                rrccUnion.addSource(rrccF);
            }
            rrccUnion.setDestinationFileName(rrccDestino.getAbsolutePath());
            rrccUnion.mergeDocuments(IOUtils.createMemoryOnlyStreamCache());
            return rrccResultado(rrccEntradas, List.of(rrccDestino),
                    "Se unieron " + rrccEntradas.size() + " PDF en " + rrccContarPaginas(rrccDestino) + " páginas.");
        } catch (IOException rrccError) {
            throw new UncheckedIOException(rrccError);
        }
    }

    public RRCCResultadoPdf rrccDividir(File rrccEntrada, String rrccRangos, File rrccCarpeta) {
        try (PDDocument rrccDoc = rrccAbrir(rrccEntrada, null)) {
            int rrccTotal = rrccDoc.getNumberOfPages();
            List<List<Integer>> rrccGrupos = new ArrayList<>();
            if (rrccRangos == null || rrccRangos.isBlank()) {
                for (int rrccI = 1; rrccI <= rrccTotal; rrccI++) {
                    rrccGrupos.add(List.of(rrccI));
                }
            } else {
                for (String rrccParte : rrccRangos.split(",")) {
                    rrccGrupos.add(rrccPaginas(rrccParte, rrccTotal));
                }
            }
            List<File> rrccSalidas = new ArrayList<>();
            String rrccBase = rrccNombreBase(rrccEntrada);
            for (List<Integer> rrccGrupo : rrccGrupos) {
                String rrccSufijo = rrccGrupo.size() == 1 ? "pagina-" + rrccGrupo.get(0)
                        : "paginas-" + rrccGrupo.get(0) + "-a-" + rrccGrupo.get(rrccGrupo.size() - 1);
                File rrccSalida = new File(rrccCarpeta, rrccBase + "-" + rrccSufijo + ".pdf");
                rrccGuardarPaginas(rrccDoc, rrccGrupo, rrccSalida);
                rrccSalidas.add(rrccSalida);
            }
            return rrccResultado(List.of(rrccEntrada), rrccSalidas, "Se crearon " + rrccSalidas.size() + " PDF.");
        } catch (IOException rrccError) {
            throw new UncheckedIOException(rrccError);
        }
    }

    public RRCCResultadoPdf rrccExtraer(File rrccEntrada, String rrccRangos, File rrccDestino) {
        try (PDDocument rrccDoc = rrccAbrir(rrccEntrada, null)) {
            List<Integer> rrccElegidas = rrccPaginas(rrccObligatorio(rrccRangos), rrccDoc.getNumberOfPages());
            rrccGuardarPaginas(rrccDoc, rrccElegidas, rrccDestino);
            return rrccResultado(List.of(rrccEntrada), List.of(rrccDestino), "Se extrajeron " + rrccElegidas.size() + " páginas.");
        } catch (IOException rrccError) {
            throw new UncheckedIOException(rrccError);
        }
    }

    public RRCCResultadoPdf rrccEliminar(File rrccEntrada, String rrccRangos, File rrccDestino) {
        try (PDDocument rrccDoc = rrccAbrir(rrccEntrada, null)) {
            int rrccTotal = rrccDoc.getNumberOfPages();
            Set<Integer> rrccQuitar = new LinkedHashSet<>(rrccPaginas(rrccObligatorio(rrccRangos), rrccTotal));
            List<Integer> rrccQuedan = new ArrayList<>();
            for (int rrccI = 1; rrccI <= rrccTotal; rrccI++) {
                if (!rrccQuitar.contains(rrccI)) {
                    rrccQuedan.add(rrccI);
                }
            }
            if (rrccQuedan.isEmpty()) {
                throw new RRCCNegocioException("No se pueden eliminar todas las páginas del PDF.");
            }
            rrccGuardarPaginas(rrccDoc, rrccQuedan, rrccDestino);
            return rrccResultado(List.of(rrccEntrada), List.of(rrccDestino),
                    "Se eliminaron " + rrccQuitar.size() + " páginas; quedan " + rrccQuedan.size() + ".");
        } catch (IOException rrccError) {
            throw new UncheckedIOException(rrccError);
        }
    }

    public RRCCResultadoPdf rrccOrdenar(File rrccEntrada, String rrccOrden, File rrccDestino) {
        try (PDDocument rrccDoc = rrccAbrir(rrccEntrada, null)) {
            int rrccTotal = rrccDoc.getNumberOfPages();
            Set<Integer> rrccNuevoOrden = new LinkedHashSet<>(rrccPaginas(rrccObligatorio(rrccOrden), rrccTotal));
            for (int rrccI = 1; rrccI <= rrccTotal; rrccI++) {
                rrccNuevoOrden.add(rrccI);
            }
            rrccGuardarPaginas(rrccDoc, new ArrayList<>(rrccNuevoOrden), rrccDestino);
            return rrccResultado(List.of(rrccEntrada), List.of(rrccDestino), "Se reordenaron las " + rrccTotal + " páginas.");
        } catch (IOException rrccError) {
            throw new UncheckedIOException(rrccError);
        }
    }

    public RRCCResultadoPdf rrccRotar(File rrccEntrada, int rrccGrados, String rrccRangos, File rrccDestino) {
        try (PDDocument rrccDoc = rrccAbrir(rrccEntrada, null)) {
            int rrccTotal = rrccDoc.getNumberOfPages();
            List<Integer> rrccElegidas = rrccRangos == null || rrccRangos.isBlank()
                    ? rrccTodas(rrccTotal) : rrccPaginas(rrccRangos, rrccTotal);
            for (int rrccN : rrccElegidas) {
                PDPage rrccPagina = rrccDoc.getPage(rrccN - 1);
                rrccPagina.setRotation((rrccPagina.getRotation() + rrccGrados) % 360);
            }
            rrccDoc.save(rrccDestino);
            return rrccResultado(List.of(rrccEntrada), List.of(rrccDestino),
                    "Se rotaron " + rrccElegidas.size() + " páginas " + rrccGrados + "°.");
        } catch (IOException rrccError) {
            throw new UncheckedIOException(rrccError);
        }
    }

    public RRCCResultadoPdf rrccMarcaDeAgua(File rrccEntrada, String rrccTexto, File rrccDestino) {
        String rrccMarca = rrccObligatorio(rrccTexto).trim();
        try (PDDocument rrccDoc = rrccAbrir(rrccEntrada, null)) {
            PDFont rrccFuente = rrccFuente(rrccDoc, true);
            String rrccSeguro = rrccTextoSeguro(rrccMarca, rrccFuente);
            PDExtendedGraphicsState rrccTransparencia = new PDExtendedGraphicsState();
            rrccTransparencia.setNonStrokingAlphaConstant(0.16f);
            for (PDPage rrccPagina : rrccDoc.getPages()) {
                PDRectangle rrccCaja = rrccPagina.getMediaBox();
                float rrccDiagonal = (float) Math.hypot(rrccCaja.getWidth(), rrccCaja.getHeight());
                float rrccTamano = Math.min(72f, rrccDiagonal * 0.8f / Math.max(1f, rrccAncho(rrccFuente, rrccSeguro, 1f)));
                float rrccLargo = rrccAncho(rrccFuente, rrccSeguro, rrccTamano);
                double rrccAngulo = Math.atan2(rrccCaja.getHeight(), rrccCaja.getWidth());
                float rrccX = (float) (rrccCaja.getWidth() / 2 - Math.cos(rrccAngulo) * rrccLargo / 2 + Math.sin(rrccAngulo) * rrccTamano / 3);
                float rrccY = (float) (rrccCaja.getHeight() / 2 - Math.sin(rrccAngulo) * rrccLargo / 2 - Math.cos(rrccAngulo) * rrccTamano / 3);
                try (PDPageContentStream rrccLienzo = new PDPageContentStream(rrccDoc, rrccPagina,
                        PDPageContentStream.AppendMode.APPEND, true, true)) {
                    rrccLienzo.setGraphicsStateParameters(rrccTransparencia);
                    rrccLienzo.setNonStrokingColor(new Color(220, 38, 38));
                    rrccLienzo.beginText();
                    rrccLienzo.setFont(rrccFuente, rrccTamano);
                    rrccLienzo.setTextMatrix(Matrix.getRotateInstance(rrccAngulo, rrccX, rrccY));
                    rrccLienzo.showText(rrccSeguro);
                    rrccLienzo.endText();
                }
            }
            rrccDoc.save(rrccDestino);
            return rrccResultado(List.of(rrccEntrada), List.of(rrccDestino),
                    "Se agregó la marca de agua «" + rrccMarca + "» a " + rrccDoc.getNumberOfPages() + " páginas.");
        } catch (IOException rrccError) {
            throw new UncheckedIOException(rrccError);
        }
    }

    public RRCCResultadoPdf rrccNumerar(File rrccEntrada, String rrccFormato, File rrccDestino) {
        String rrccPlantilla = rrccFormato == null || rrccFormato.isBlank() ? "Página {n} de {total}" : rrccFormato;
        try (PDDocument rrccDoc = rrccAbrir(rrccEntrada, null)) {
            PDFont rrccFuente = rrccFuente(rrccDoc, false);
            int rrccTotal = rrccDoc.getNumberOfPages();
            for (int rrccI = 0; rrccI < rrccTotal; rrccI++) {
                PDPage rrccPagina = rrccDoc.getPage(rrccI);
                PDRectangle rrccCaja = rrccPagina.getMediaBox();
                String rrccTexto = rrccTextoSeguro(rrccPlantilla.replace("{n}", String.valueOf(rrccI + 1))
                        .replace("{total}", String.valueOf(rrccTotal)), rrccFuente);
                float rrccTamano = 10f;
                float rrccX = (rrccCaja.getWidth() - rrccAncho(rrccFuente, rrccTexto, rrccTamano)) / 2;
                try (PDPageContentStream rrccLienzo = new PDPageContentStream(rrccDoc, rrccPagina,
                        PDPageContentStream.AppendMode.APPEND, true, true)) {
                    rrccLienzo.setNonStrokingColor(new Color(71, 85, 105));
                    rrccLienzo.beginText();
                    rrccLienzo.setFont(rrccFuente, rrccTamano);
                    rrccLienzo.newLineAtOffset(rrccCaja.getLowerLeftX() + rrccX, rrccCaja.getLowerLeftY() + 22f);
                    rrccLienzo.showText(rrccTexto);
                    rrccLienzo.endText();
                }
            }
            rrccDoc.save(rrccDestino);
            return rrccResultado(List.of(rrccEntrada), List.of(rrccDestino), "Se numeraron " + rrccTotal + " páginas.");
        } catch (IOException rrccError) {
            throw new UncheckedIOException(rrccError);
        }
    }

    public RRCCResultadoPdf rrccProteger(File rrccEntrada, String rrccClave, File rrccDestino) {
        String rrccContrasena = rrccObligatorio(rrccClave);
        if (rrccContrasena.length() < 4) {
            throw new RRCCNegocioException("La contraseña debe tener al menos 4 caracteres.");
        }
        try (PDDocument rrccDoc = rrccAbrir(rrccEntrada, null)) {
            StandardProtectionPolicy rrccPolitica = new StandardProtectionPolicy(rrccContrasena, rrccContrasena, new AccessPermission());
            rrccPolitica.setEncryptionKeyLength(256);
            rrccDoc.protect(rrccPolitica);
            rrccDoc.save(rrccDestino);
            return rrccResultado(List.of(rrccEntrada), List.of(rrccDestino), "El PDF quedó protegido con contraseña (AES de 256 bits).");
        } catch (IOException rrccError) {
            throw new UncheckedIOException(rrccError);
        }
    }

    public RRCCResultadoPdf rrccDesbloquear(File rrccEntrada, String rrccClave, File rrccDestino) {
        try (PDDocument rrccDoc = rrccAbrir(rrccEntrada, rrccClave == null ? "" : rrccClave)) {
            if (!rrccDoc.isEncrypted()) {
                throw new RRCCNegocioException("Este PDF no tiene contraseña.");
            }
            rrccDoc.setAllSecurityToBeRemoved(true);
            rrccDoc.save(rrccDestino);
            return rrccResultado(List.of(rrccEntrada), List.of(rrccDestino), "Se quitó la contraseña del PDF.");
        } catch (IOException rrccError) {
            throw new UncheckedIOException(rrccError);
        }
    }

    public RRCCResultadoPdf rrccComprimir(File rrccEntrada, float rrccCalidad, File rrccDestino) {
        try (PDDocument rrccDoc = rrccAbrir(rrccEntrada, null)) {
            int rrccImagenes = 0;
            for (PDPage rrccPagina : rrccDoc.getPages()) {
                rrccImagenes += rrccComprimirRecursos(rrccDoc, rrccPagina.getResources(), rrccCalidad);
            }
            rrccDoc.save(rrccDestino);
            long rrccAntes = rrccEntrada.length();
            long rrccDespues = rrccDestino.length();
            if (rrccDespues >= rrccAntes) {
                Files.copy(rrccEntrada.toPath(), rrccDestino.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return new RRCCResultadoPdf(List.of(rrccDestino), rrccAntes, rrccAntes,
                        "El PDF ya estaba optimizado: no se pudo reducir más.");
            }
            return new RRCCResultadoPdf(List.of(rrccDestino), rrccAntes, rrccDespues,
                    "Se recomprimieron " + rrccImagenes + " imágenes.");
        } catch (IOException rrccError) {
            throw new UncheckedIOException(rrccError);
        }
    }

    public RRCCResultadoPdf rrccPdfAImagenes(File rrccEntrada, int rrccDpi, File rrccCarpeta) {
        try (PDDocument rrccDoc = rrccAbrir(rrccEntrada, null)) {
            PDFRenderer rrccRender = new PDFRenderer(rrccDoc);
            List<File> rrccSalidas = new ArrayList<>();
            String rrccBase = rrccNombreBase(rrccEntrada);
            for (int rrccI = 0; rrccI < rrccDoc.getNumberOfPages(); rrccI++) {
                BufferedImage rrccImagen = rrccRender.renderImageWithDPI(rrccI, rrccDpi, ImageType.RGB);
                File rrccSalida = new File(rrccCarpeta, rrccBase + "-pagina-" + (rrccI + 1) + ".png");
                ImageIO.write(rrccImagen, "png", rrccSalida);
                rrccSalidas.add(rrccSalida);
            }
            return rrccResultado(List.of(rrccEntrada), rrccSalidas, "Se crearon " + rrccSalidas.size() + " imágenes PNG a " + rrccDpi + " ppp.");
        } catch (IOException rrccError) {
            throw new UncheckedIOException(rrccError);
        }
    }

    public RRCCResultadoPdf rrccImagenesAPdf(List<File> rrccImagenes, File rrccDestino) {
        if (rrccImagenes.isEmpty()) {
            throw new RRCCNegocioException("Elige al menos una imagen.");
        }
        try (PDDocument rrccDoc = new PDDocument()) {
            for (File rrccF : rrccImagenes) {
                PDImageXObject rrccImagen = PDImageXObject.createFromFileByContent(rrccF, rrccDoc);
                boolean rrccHorizontal = rrccImagen.getWidth() > rrccImagen.getHeight();
                PDRectangle rrccHoja = rrccHorizontal ? new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()) : PDRectangle.A4;
                PDPage rrccPagina = new PDPage(rrccHoja);
                rrccDoc.addPage(rrccPagina);
                float rrccEspacioAncho = rrccHoja.getWidth() - 2 * 28f;
                float rrccEspacioAlto = rrccHoja.getHeight() - 2 * 28f;
                float rrccEscala = Math.min(rrccEspacioAncho / rrccImagen.getWidth(), rrccEspacioAlto / rrccImagen.getHeight());
                float rrccAncho = rrccImagen.getWidth() * rrccEscala;
                float rrccAlto = rrccImagen.getHeight() * rrccEscala;
                try (PDPageContentStream rrccLienzo = new PDPageContentStream(rrccDoc, rrccPagina)) {
                    rrccLienzo.drawImage(rrccImagen, (rrccHoja.getWidth() - rrccAncho) / 2, (rrccHoja.getHeight() - rrccAlto) / 2, rrccAncho, rrccAlto);
                }
            }
            rrccDoc.save(rrccDestino);
            return rrccResultado(rrccImagenes, List.of(rrccDestino), "Se creó un PDF de " + rrccImagenes.size() + " páginas.");
        } catch (IOException rrccError) {
            throw new UncheckedIOException(rrccError);
        }
    }

    public RRCCResultadoPdf rrccPdfAWord(File rrccEntrada, File rrccDestino) {
        try (PDDocument rrccDoc = rrccAbrir(rrccEntrada, null);
             XWPFDocument rrccWord = new XWPFDocument();
             OutputStream rrccSalida = new FileOutputStream(rrccDestino)) {
            PDFTextStripper rrccExtractor = new PDFTextStripper();
            rrccExtractor.setSortByPosition(true);
            int rrccTotal = rrccDoc.getNumberOfPages();
            int rrccConTexto = 0;
            for (int rrccI = 1; rrccI <= rrccTotal; rrccI++) {
                rrccExtractor.setStartPage(rrccI);
                rrccExtractor.setEndPage(rrccI);
                String rrccTexto = rrccExtractor.getText(rrccDoc);
                if (!rrccTexto.isBlank()) {
                    rrccConTexto++;
                }
                for (String rrccParrafo : rrccParrafos(rrccTexto)) {
                    XWPFRun rrccRun = rrccWord.createParagraph().createRun();
                    rrccRun.setFontFamily("Calibri");
                    rrccRun.setFontSize(11);
                    rrccRun.setText(rrccParrafo);
                }
                if (rrccI < rrccTotal) {
                    rrccWord.createParagraph().createRun().addBreak(BreakType.PAGE);
                }
            }
            if (rrccConTexto == 0) {
                throw new RRCCNegocioException("El PDF no tiene texto seleccionable (parece escaneado); no se puede convertir a Word.");
            }
            rrccWord.write(rrccSalida);
        } catch (IOException rrccError) {
            throw new UncheckedIOException(rrccError);
        }
        return rrccResultado(List.of(rrccEntrada), List.of(rrccDestino), "Se convirtió a Word conservando la división por páginas.");
    }

    public RRCCResultadoPdf rrccWordAPdf(File rrccEntrada, File rrccDestino) {
        try (InputStream rrccFlujo = new FileInputStream(rrccEntrada);
             XWPFDocument rrccWord = new XWPFDocument(rrccFlujo);
             PDDocument rrccDoc = new PDDocument()) {
            RRCCEscritorPdf rrccEscritor = new RRCCEscritorPdf(rrccDoc);
            for (IBodyElement rrccElemento : rrccWord.getBodyElements()) {
                if (rrccElemento instanceof XWPFParagraph rrccP) {
                    String rrccEstilo = rrccP.getStyle() == null ? "" : rrccP.getStyle().toLowerCase();
                    boolean rrccTitulo = rrccEstilo.contains("heading") || rrccEstilo.contains("ttulo") || rrccEstilo.contains("title");
                    rrccEscritor.rrccParrafo(rrccP.getText(), rrccTitulo);
                } else if (rrccElemento instanceof XWPFTable rrccTabla) {
                    for (XWPFTableRow rrccFila : rrccTabla.getRows()) {
                        List<String> rrccCeldas = rrccFila.getTableCells().stream().map(rrccC -> rrccC.getText().trim()).toList();
                        rrccEscritor.rrccParrafo(String.join("  |  ", rrccCeldas), false);
                    }
                }
            }
            rrccEscritor.rrccCerrar();
            rrccDoc.save(rrccDestino);
            return rrccResultado(List.of(rrccEntrada), List.of(rrccDestino),
                    "Se convirtió a PDF de " + rrccDoc.getNumberOfPages() + " páginas (texto y tablas).");
        } catch (IOException rrccError) {
            throw new UncheckedIOException(rrccError);
        }
    }

    public RRCCResultadoPdf rrccTextoAPdf(File rrccEntrada, File rrccDestino) {
        try (PDDocument rrccDoc = new PDDocument()) {
            String rrccTexto = Files.readString(rrccEntrada.toPath(), StandardCharsets.UTF_8);
            RRCCEscritorPdf rrccEscritor = new RRCCEscritorPdf(rrccDoc);
            for (String rrccLinea : rrccTexto.replace("\r", "").split("\n", -1)) {
                rrccEscritor.rrccParrafo(rrccLinea, false);
            }
            rrccEscritor.rrccCerrar();
            rrccDoc.save(rrccDestino);
            return rrccResultado(List.of(rrccEntrada), List.of(rrccDestino), "Se convirtió a PDF de " + rrccDoc.getNumberOfPages() + " páginas.");
        } catch (IOException rrccError) {
            throw new UncheckedIOException(rrccError);
        }
    }

    public List<Integer> rrccPaginas(String rrccRangos, int rrccTotal) {
        List<Integer> rrccLista = new ArrayList<>();
        for (String rrccParte : rrccRangos.replace(" ", "").split(",")) {
            if (rrccParte.isEmpty()) {
                continue;
            }
            String[] rrccExtremos = rrccParte.split("-", -1);
            try {
                int rrccDesde = Integer.parseInt(rrccExtremos[0]);
                int rrccHasta = rrccExtremos.length > 1 ? (rrccExtremos[1].isEmpty() ? rrccTotal : Integer.parseInt(rrccExtremos[1])) : rrccDesde;
                if (rrccExtremos.length > 2 || rrccDesde < 1 || rrccHasta > rrccTotal || rrccDesde > rrccHasta) {
                    throw new NumberFormatException();
                }
                for (int rrccI = rrccDesde; rrccI <= rrccHasta; rrccI++) {
                    rrccLista.add(rrccI);
                }
            } catch (NumberFormatException rrccError) {
                throw new RRCCNegocioException("Rango no válido: «" + rrccParte + "». El PDF tiene " + rrccTotal
                        + " páginas; usa por ejemplo 1-3, 5, 8-" + rrccTotal + ".");
            }
        }
        if (rrccLista.isEmpty()) {
            throw new RRCCNegocioException("Escribe qué páginas usar, por ejemplo 1-3, 5.");
        }
        return rrccLista;
    }

    private PDDocument rrccAbrir(File rrccPdf, String rrccClave) throws IOException {
        try {
            return rrccClave == null ? Loader.loadPDF(rrccPdf) : Loader.loadPDF(rrccPdf, rrccClave);
        } catch (InvalidPasswordException rrccError) {
            throw new RRCCNegocioException(rrccClave == null
                    ? "«" + rrccPdf.getName() + "» tiene contraseña. Quítala primero con la herramienta Desbloquear PDF."
                    : "La contraseña no es correcta.");
        }
    }

    private void rrccGuardarPaginas(PDDocument rrccOrigen, List<Integer> rrccNumeros, File rrccDestino) throws IOException {
        try (PDDocument rrccNuevo = new PDDocument()) {
            for (int rrccN : rrccNumeros) {
                rrccNuevo.importPage(rrccOrigen.getPage(rrccN - 1));
            }
            rrccNuevo.save(rrccDestino);
        }
    }

    private int rrccComprimirRecursos(PDDocument rrccDoc, PDResources rrccRecursos, float rrccCalidad) throws IOException {
        if (rrccRecursos == null) {
            return 0;
        }
        int rrccCambiadas = 0;
        for (COSName rrccNombre : rrccRecursos.getXObjectNames()) {
            PDXObject rrccObjeto = rrccRecursos.getXObject(rrccNombre);
            if (rrccObjeto instanceof PDImageXObject rrccImagen) {
                long rrccPesoAntes = rrccImagen.getCOSObject().getLength();
                BufferedImage rrccPixeles = rrccReducir(rrccAOpaco(rrccImagen.getImage()));
                PDImageXObject rrccJpeg = JPEGFactory.createFromImage(rrccDoc, rrccPixeles, rrccCalidad);
                if (rrccJpeg.getCOSObject().getLength() < rrccPesoAntes) {
                    rrccRecursos.put(rrccNombre, rrccJpeg);
                    rrccCambiadas++;
                }
            } else if (rrccObjeto instanceof PDFormXObject rrccFormulario) {
                rrccCambiadas += rrccComprimirRecursos(rrccDoc, rrccFormulario.getResources(), rrccCalidad);
            }
        }
        return rrccCambiadas;
    }

    private BufferedImage rrccAOpaco(BufferedImage rrccImagen) {
        BufferedImage rrccRgb = new BufferedImage(rrccImagen.getWidth(), rrccImagen.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D rrccG = rrccRgb.createGraphics();
        rrccG.setColor(Color.WHITE);
        rrccG.fillRect(0, 0, rrccImagen.getWidth(), rrccImagen.getHeight());
        rrccG.drawImage(rrccImagen, 0, 0, null);
        rrccG.dispose();
        return rrccRgb;
    }

    private BufferedImage rrccReducir(BufferedImage rrccImagen) {
        int rrccLado = Math.max(rrccImagen.getWidth(), rrccImagen.getHeight());
        if (rrccLado <= RRCC_LADO_MAXIMO_COMPRESION) {
            return rrccImagen;
        }
        double rrccEscala = (double) RRCC_LADO_MAXIMO_COMPRESION / rrccLado;
        int rrccAncho = (int) Math.round(rrccImagen.getWidth() * rrccEscala);
        int rrccAlto = (int) Math.round(rrccImagen.getHeight() * rrccEscala);
        BufferedImage rrccNueva = new BufferedImage(rrccAncho, rrccAlto, BufferedImage.TYPE_INT_RGB);
        Graphics2D rrccG = rrccNueva.createGraphics();
        rrccG.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        rrccG.drawImage(rrccImagen, 0, 0, rrccAncho, rrccAlto, null);
        rrccG.dispose();
        return rrccNueva;
    }

    private List<String> rrccParrafos(String rrccTexto) {
        List<String> rrccSalida = new ArrayList<>();
        StringBuilder rrccActual = new StringBuilder();
        for (String rrccLinea : rrccTexto.replace("\r", "").split("\n")) {
            String rrccLimpia = rrccLinea.strip();
            if (rrccLimpia.isEmpty()) {
                if (!rrccActual.isEmpty()) {
                    rrccSalida.add(rrccActual.toString());
                    rrccActual.setLength(0);
                }
                continue;
            }
            if (!rrccActual.isEmpty()) {
                char rrccFin = rrccActual.charAt(rrccActual.length() - 1);
                if (rrccFin == '.' || rrccFin == ':' || rrccLimpia.matches("^(\\d+[.)]|[-•*]).*")) {
                    rrccSalida.add(rrccActual.toString());
                    rrccActual.setLength(0);
                } else {
                    rrccActual.append(' ');
                }
            }
            rrccActual.append(rrccLimpia);
        }
        if (!rrccActual.isEmpty()) {
            rrccSalida.add(rrccActual.toString());
        }
        return rrccSalida;
    }

    private RRCCResultadoPdf rrccResultado(List<File> rrccEntradas, List<File> rrccSalidas, String rrccResumen) {
        long rrccAntes = rrccEntradas.stream().mapToLong(File::length).sum();
        long rrccDespues = rrccSalidas.stream().mapToLong(File::length).sum();
        return new RRCCResultadoPdf(rrccSalidas, rrccAntes, rrccDespues, rrccResumen);
    }

    private String rrccObligatorio(String rrccValor) {
        if (rrccValor == null || rrccValor.isBlank()) {
            throw new RRCCNegocioException("Completa el campo de opciones antes de procesar.");
        }
        return rrccValor;
    }

    private List<Integer> rrccTodas(int rrccTotal) {
        List<Integer> rrccLista = new ArrayList<>();
        for (int rrccI = 1; rrccI <= rrccTotal; rrccI++) {
            rrccLista.add(rrccI);
        }
        return rrccLista;
    }

    private String rrccNombreBase(File rrccArchivo) {
        String rrccNombre = rrccArchivo.getName();
        int rrccPunto = rrccNombre.lastIndexOf('.');
        return rrccPunto > 0 ? rrccNombre.substring(0, rrccPunto) : rrccNombre;
    }

    private static PDFont rrccFuente(PDDocument rrccDoc, boolean rrccNegrita) throws IOException {
        String rrccWindows = System.getenv("WINDIR");
        File rrccArchivo = new File(rrccWindows == null ? "C:\\Windows" : rrccWindows, "Fonts\\" + (rrccNegrita ? "arialbd.ttf" : "arial.ttf"));
        if (rrccArchivo.isFile()) {
            return PDType0Font.load(rrccDoc, rrccArchivo);
        }
        return new PDType1Font(rrccNegrita ? Standard14Fonts.FontName.HELVETICA_BOLD : Standard14Fonts.FontName.HELVETICA);
    }

    private static String rrccTextoSeguro(String rrccTexto, PDFont rrccFuente) {
        StringBuilder rrccSalida = new StringBuilder();
        rrccTexto.replace("\t", "    ").codePoints().forEach(rrccCodigo -> {
            String rrccCaracter = new String(Character.toChars(rrccCodigo));
            try {
                rrccFuente.encode(rrccCaracter);
                rrccSalida.append(rrccCaracter);
            } catch (IOException | IllegalArgumentException rrccError) {
                rrccSalida.append(Character.isWhitespace(rrccCodigo) ? " " : "?");
            }
        });
        return rrccSalida.toString();
    }

    private static float rrccAncho(PDFont rrccFuente, String rrccTexto, float rrccTamano) {
        try {
            return rrccFuente.getStringWidth(rrccTexto) / 1000f * rrccTamano;
        } catch (IOException rrccError) {
            throw new UncheckedIOException(rrccError);
        }
    }

    private static final class RRCCEscritorPdf {

        private final PDDocument rrccDoc;
        private final PDFont rrccNormal;
        private final PDFont rrccNegrita;
        private PDPageContentStream rrccLienzo;
        private float rrccY;

        private RRCCEscritorPdf(PDDocument rrccDoc) throws IOException {
            this.rrccDoc = rrccDoc;
            this.rrccNormal = rrccFuente(rrccDoc, false);
            this.rrccNegrita = rrccFuente(rrccDoc, true);
            rrccNuevaPagina();
        }

        private void rrccNuevaPagina() throws IOException {
            if (rrccLienzo != null) {
                rrccLienzo.close();
            }
            PDPage rrccPagina = new PDPage(PDRectangle.A4);
            rrccDoc.addPage(rrccPagina);
            rrccLienzo = new PDPageContentStream(rrccDoc, rrccPagina);
            rrccY = PDRectangle.A4.getHeight() - RRCC_MARGEN;
        }

        private void rrccParrafo(String rrccTexto, boolean rrccTitulo) throws IOException {
            PDFont rrccFuenteUsada = rrccTitulo ? rrccNegrita : rrccNormal;
            float rrccTamano = rrccTitulo ? RRCC_TAMANO_TITULO : RRCC_TAMANO_TEXTO;
            float rrccAlto = rrccTamano * RRCC_INTERLINEA;
            float rrccAnchoUtil = PDRectangle.A4.getWidth() - 2 * RRCC_MARGEN;
            String rrccSeguro = rrccTextoSeguro(rrccTexto == null ? "" : rrccTexto, rrccFuenteUsada);
            if (rrccTitulo) {
                rrccY -= rrccAlto * 0.4f;
            }
            for (String rrccLinea : rrccPartir(rrccSeguro, rrccFuenteUsada, rrccTamano, rrccAnchoUtil)) {
                if (rrccY - rrccAlto < RRCC_MARGEN) {
                    rrccNuevaPagina();
                }
                rrccY -= rrccAlto;
                if (!rrccLinea.isEmpty()) {
                    rrccLienzo.beginText();
                    rrccLienzo.setFont(rrccFuenteUsada, rrccTamano);
                    rrccLienzo.newLineAtOffset(RRCC_MARGEN, rrccY);
                    rrccLienzo.showText(rrccLinea);
                    rrccLienzo.endText();
                }
            }
            rrccY -= rrccAlto * 0.35f;
        }

        private List<String> rrccPartir(String rrccTexto, PDFont rrccFuenteUsada, float rrccTamano, float rrccAnchoUtil) {
            List<String> rrccLineas = new ArrayList<>();
            StringBuilder rrccActual = new StringBuilder();
            for (String rrccPalabra : rrccTexto.split(" ")) {
                String rrccPrueba = rrccActual.isEmpty() ? rrccPalabra : rrccActual + " " + rrccPalabra;
                if (rrccAncho(rrccFuenteUsada, rrccPrueba, rrccTamano) <= rrccAnchoUtil) {
                    rrccActual.setLength(0);
                    rrccActual.append(rrccPrueba);
                    continue;
                }
                if (!rrccActual.isEmpty()) {
                    rrccLineas.add(rrccActual.toString());
                    rrccActual.setLength(0);
                }
                String rrccResto = rrccPalabra;
                while (rrccAncho(rrccFuenteUsada, rrccResto, rrccTamano) > rrccAnchoUtil && rrccResto.length() > 1) {
                    int rrccCorte = rrccResto.length() - 1;
                    while (rrccCorte > 1 && rrccAncho(rrccFuenteUsada, rrccResto.substring(0, rrccCorte), rrccTamano) > rrccAnchoUtil) {
                        rrccCorte--;
                    }
                    rrccLineas.add(rrccResto.substring(0, rrccCorte));
                    rrccResto = rrccResto.substring(rrccCorte);
                }
                rrccActual.append(rrccResto);
            }
            rrccLineas.add(rrccActual.toString());
            return rrccLineas;
        }

        private void rrccCerrar() throws IOException {
            rrccLienzo.close();
        }
    }
}
