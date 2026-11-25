package com.ejemplo.rrccmindscan.service;

import com.ejemplo.rrccmindscan.config.RRCCAnalisisPropiedades;
import com.ejemplo.rrccmindscan.model.RRCCCategoria;
import com.ejemplo.rrccmindscan.model.RRCCDocumento;
import com.ejemplo.rrccmindscan.model.RRCCTipoDato;
import com.ejemplo.rrccmindscan.model.RRCCTipoDocumento;
import com.ejemplo.rrccmindscan.model.RRCCTipoHallazgo;
import com.ejemplo.rrccmindscan.seeder.RRCCDatabaseSeeder;
import com.ejemplo.rrccmindscan.service.RRCCAnalizadorService.RRCCDato;
import com.ejemplo.rrccmindscan.service.RRCCAnalizadorService.RRCCOracionPuntuada;
import com.ejemplo.rrccmindscan.service.RRCCAnalizadorService.RRCCRespuesta;
import com.ejemplo.rrccmindscan.service.RRCCAnalizadorService.RRCCTermino;
import com.ejemplo.rrccmindscan.service.RRCCBuscadorService.RRCCOpcionesBusqueda;
import com.ejemplo.rrccmindscan.service.RRCCBuscadorService.RRCCRespuestaBusqueda;
import com.ejemplo.rrccmindscan.service.RRCCComparadorService.RRCCResultadoComparacion;
import com.ejemplo.rrccmindscan.service.RRCCRevisorService.RRCCResultadoRevision;
import com.ejemplo.rrccmindscan.service.RRCCTextoService.RRCCEstadisticas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class RRCCMotoresTextoTest {

    private RRCCTextoService rrccTexto;
    private RRCCAnalizadorService rrccAnalizador;
    private RRCCBuscadorService rrccBuscador;
    private RRCCComparadorService rrccComparador;
    private RRCCRevisorService rrccRevisor;
    private List<RRCCDocumento> rrccBiblioteca;

    @BeforeEach
    void rrccPreparar() {
        RRCCAnalisisPropiedades rrccPropiedades = RRCCAnalisisPropiedades.rrccPorDefecto();
        rrccTexto = new RRCCTextoService(rrccPropiedades);
        rrccAnalizador = new RRCCAnalizadorService(rrccTexto, rrccPropiedades);
        rrccBuscador = new RRCCBuscadorService(rrccTexto);
        rrccComparador = new RRCCComparadorService(rrccTexto);
        rrccRevisor = new RRCCRevisorService(rrccTexto, rrccPropiedades);
        rrccBiblioteca = new ArrayList<>();
        rrccAgregar("Reglamento v1", RRCCDatabaseSeeder.RRCC_REGLAMENTO_V1);
        rrccAgregar("Reglamento v2", RRCCDatabaseSeeder.RRCC_REGLAMENTO_V2);
        rrccAgregar("Informe tecnico", RRCCDatabaseSeeder.RRCC_INFORME);
        rrccAgregar("Contrato", RRCCDatabaseSeeder.RRCC_CONTRATO);
        rrccAgregar("Acta", RRCCDatabaseSeeder.RRCC_ACTA);
    }

    private void rrccAgregar(String rrccNombre, String rrccContenido) {
        String rrccLimpio = rrccTexto.rrccLimpiar(rrccContenido);
        RRCCEstadisticas rrccEst = rrccTexto.rrccEstadisticas(rrccLimpio);
        rrccBiblioteca.add(RRCCDocumento.builder()
                .rrccId((long) rrccBiblioteca.size() + 1)
                .rrccNombre(rrccNombre)
                .rrccTipo(RRCCTipoDocumento.RRCC_TEXTO)
                .rrccCategoria(rrccAnalizador.rrccClasificar(rrccLimpio).rrccCategoria())
                .rrccTamanoBytes((long) rrccLimpio.length())
                .rrccPaginas(1)
                .rrccPalabras(rrccEst.rrccPalabras())
                .rrccOraciones(rrccEst.rrccOraciones())
                .rrccLegibilidad(rrccEst.rrccLegibilidad())
                .rrccHash(rrccTexto.rrccHash(rrccLimpio))
                .rrccFechaImportacion(LocalDateTime.now())
                .rrccContenido(rrccLimpio)
                .build());
    }

    private String rrccContenido(int rrccIndice) {
        return rrccBiblioteca.get(rrccIndice).getRrccContenido();
    }

    @Test
    void rrccNormalizaYCalculaDistancias() {
        assertEquals("informacion tecnica", rrccTexto.rrccNormalizar("Información Técnica"));
        assertEquals(1, rrccTexto.rrccLevenshtein("contrato", "contrto", 2));
        assertTrue(rrccTexto.rrccLevenshtein("servidor", "memoria", 2) > 2);
        assertEquals(1.0, rrccTexto.rrccJaccard(Set.of("a", "b"), Set.of("b", "a")));
    }

    @Test
    void rrccSeparaOracionesSinCortarAbreviaturas() {
        List<RRCCTextoService.RRCCOracion> rrccOraciones = rrccTexto.rrccOraciones(
                "La Ing. Carla Mendoza revisó el equipo. El costo fue S/ 4,800.00 en total. ¿Se aprobó?");
        assertEquals(3, rrccOraciones.size());
        assertTrue(rrccOraciones.get(0).rrccTexto().contains("Carla Mendoza"));
    }

    @Test
    void rrccCalculaLegibilidad() {
        RRCCEstadisticas rrccEst = rrccTexto.rrccEstadisticas(rrccContenido(0));
        assertTrue(rrccEst.rrccPalabras() > 100);
        assertTrue(rrccEst.rrccLegibilidad() > 0 && rrccEst.rrccLegibilidad() <= 100);
        assertTrue(rrccEst.rrccMinutosLectura() > 0);
    }

    @Test
    void rrccClasificaPorCategoria() {
        assertEquals(RRCCCategoria.RRCC_LEGAL, rrccBiblioteca.get(3).getRrccCategoria());
        assertEquals(RRCCCategoria.RRCC_ADMINISTRATIVO, rrccBiblioteca.get(4).getRrccCategoria());
        assertEquals(RRCCCategoria.RRCC_TECNICO, rrccBiblioteca.get(2).getRrccCategoria());
    }

    @Test
    void rrccObtienePalabrasClave() {
        List<String> rrccCorpus = rrccBiblioteca.stream().map(RRCCDocumento::getRrccContenido).toList();
        List<RRCCTermino> rrccTerminos = rrccAnalizador.rrccPalabrasClave(rrccContenido(2), rrccCorpus, 10);
        Set<String> rrccClaves = rrccTerminos.stream().map(RRCCTermino::rrccClave).collect(Collectors.toSet());
        assertTrue(rrccClaves.contains("memoria") || rrccClaves.contains("switch") || rrccClaves.contains("equipo"));
    }

    @Test
    void rrccGeneraResumenProporcional() {
        List<RRCCOracionPuntuada> rrccResumen = rrccAnalizador.rrccResumen(rrccContenido(3), 0.25);
        long rrccElegidas = rrccResumen.stream().filter(RRCCOracionPuntuada::rrccSeleccionada).count();
        assertTrue(rrccElegidas >= 2 && rrccElegidas < rrccResumen.size());
    }

    @Test
    void rrccExtraeDatosDelContrato() {
        List<RRCCDato> rrccDatos = rrccAnalizador.rrccDatos(rrccContenido(3));
        Set<RRCCTipoDato> rrccTipos = rrccDatos.stream().map(RRCCDato::rrccTipo).collect(Collectors.toSet());
        assertTrue(rrccTipos.containsAll(Set.of(RRCCTipoDato.RRCC_RUC, RRCCTipoDato.RRCC_DNI, RRCCTipoDato.RRCC_MONTO,
                RRCCTipoDato.RRCC_CORREO, RRCCTipoDato.RRCC_PORCENTAJE, RRCCTipoDato.RRCC_FECHA, RRCCTipoDato.RRCC_TELEFONO)));
        assertEquals(2, rrccDatos.stream().filter(rrccD -> rrccD.rrccTipo() == RRCCTipoDato.RRCC_RUC).count());
    }

    @Test
    void rrccRespondePreguntas() {
        List<RRCCRespuesta> rrccPlazo = rrccAnalizador.rrccResponder(rrccContenido(3), "¿Cuál es el plazo del servicio?", 3);
        assertTrue(rrccPlazo.get(0).rrccOracion().contains("90"));
        List<RRCCRespuesta> rrccMonto = rrccAnalizador.rrccResponder(rrccContenido(3), "¿Cuánto es el monto total del contrato?", 3);
        assertTrue(rrccMonto.get(0).rrccOracion().contains("18,500.00"));
    }

    @Test
    void rrccBuscaConRelevancia() {
        RRCCRespuestaBusqueda rrccR = rrccBuscador.rrccBuscar(rrccBiblioteca, "switch memoria",
                new RRCCOpcionesBusqueda(false, false, null));
        assertEquals("Informe tecnico", rrccR.rrccResultados().get(0).rrccDocumento().getRrccNombre());
        assertFalse(rrccR.rrccResultados().get(0).rrccFragmentos().isEmpty());
    }

    @Test
    void rrccBuscaConErroresDeTipeo() {
        RRCCRespuestaBusqueda rrccR = rrccBuscador.rrccBuscar(rrccBiblioteca, "contrtista penalidad",
                new RRCCOpcionesBusqueda(false, true, null));
        assertFalse(rrccR.rrccCorrecciones().isEmpty());
        assertEquals("Contrato", rrccR.rrccResultados().get(0).rrccDocumento().getRrccNombre());
    }

    @Test
    void rrccBuscaFraseExacta() {
        RRCCRespuestaBusqueda rrccR = rrccBuscador.rrccBuscar(rrccBiblioteca, "suspension del acceso",
                new RRCCOpcionesBusqueda(true, false, null));
        assertEquals(2, rrccR.rrccResultados().size());
    }

    @Test
    void rrccComparaVersiones() {
        RRCCResultadoComparacion rrccR = rrccComparador.rrccComparar(rrccContenido(0), rrccContenido(1));
        assertTrue(rrccR.rrccModificadas() >= 3);
        assertTrue(rrccR.rrccAgregadas() >= 1);
        assertTrue(rrccR.rrccEliminadas() >= 1);
        assertTrue(rrccR.rrccIguales() >= 4);
        assertTrue(rrccR.rrccSimilitudGlobal() > 0.5 && rrccR.rrccSimilitudGlobal() < 0.98);
        RRCCResultadoComparacion rrccMismo = rrccComparador.rrccComparar(rrccContenido(0), rrccContenido(0));
        assertEquals(0, rrccMismo.rrccCambios());
    }

    @Test
    void rrccRevisaInformeConErrores() {
        RRCCResultadoRevision rrccR = rrccRevisor.rrccRevisar(rrccContenido(2));
        Set<RRCCTipoHallazgo> rrccTipos = rrccR.rrccPorTipo().keySet();
        assertTrue(rrccTipos.containsAll(Set.of(RRCCTipoHallazgo.RRCC_CONTRADICCION, RRCCTipoHallazgo.RRCC_CIFRA_INCONSISTENTE,
                RRCCTipoHallazgo.RRCC_FECHA_INVALIDA, RRCCTipoHallazgo.RRCC_PALABRA_REPETIDA, RRCCTipoHallazgo.RRCC_ESPACIOS,
                RRCCTipoHallazgo.RRCC_PUNTUACION, RRCCTipoHallazgo.RRCC_MAYUSCULA, RRCCTipoHallazgo.RRCC_PARRAFO_DUPLICADO,
                RRCCTipoHallazgo.RRCC_ORACION_LARGA, RRCCTipoHallazgo.RRCC_SIGNOS_DESBALANCEADOS)));
        assertTrue(rrccR.rrccPuntaje() < 70);
    }

    @Test
    void rrccConstruyeCronologiaYEntidades() {
        RRCCExtraccionService rrccExtraccion = new RRCCExtraccionService(rrccTexto);
        var rrccEventos = rrccExtraccion.rrccCronologia(rrccContenido(4));
        assertEquals(4, rrccEventos.size());
        assertTrue(rrccEventos.get(0).rrccFecha().isBefore(rrccEventos.get(3).rrccFecha()));
        var rrccEntidades = rrccExtraccion.rrccEntidades(rrccContenido(4));
        assertTrue(rrccEntidades.stream().anyMatch(rrccE -> rrccE.rrccNombre().equals("Rosa Condori Apaza")
                && rrccE.rrccTipo().equals(RRCCExtraccionService.RRCC_PERSONA)));
        assertTrue(rrccEntidades.stream().anyMatch(rrccE -> rrccE.rrccTipo().equals(RRCCExtraccionService.RRCC_ORGANIZACION)));
    }

    @Test
    void rrccRevisaDocumentoLimpio() {
        RRCCResultadoRevision rrccR = rrccRevisor.rrccRevisar(rrccContenido(0));
        assertEquals(0, rrccR.rrccErrores());
        assertTrue(rrccR.rrccPuntaje() >= 90);
    }

    @Test
    void rrccCorrigeErroresDeFormaSinCambiarPalabras() {
        RRCCCorrectorService rrccCorrector = new RRCCCorrectorService(rrccTexto);
        var rrccR = rrccCorrector.rrccCorregir("El  informe de de la sede , fue aprobado. luego se archivó.");
        assertEquals("El informe de la sede, fue aprobado. Luego se archivó.", rrccR.rrccCorregido());
        assertEquals(4, rrccR.rrccCambios().size());
        assertFalse(rrccCorrector.rrccCorregir("Texto correcto. Sin problemas.").rrccHayCambios());
    }

    @Test
    void rrccDetectaTextoIdenticoCompartido() {
        String rrccComun = "el comité aprobó por unanimidad el presupuesto anual de la oficina regional de planificación";
        var rrccC = rrccComparador.rrccTextoCompartido("Introducción breve. " + rrccComun + ".", rrccComun + ". Cierre distinto del acta.");
        assertFalse(rrccC.rrccPasajes().isEmpty());
        assertTrue(rrccC.rrccPorcentajeA() > 0.5);
        assertTrue(rrccComparador.rrccTextoCompartido(rrccContenido(0), rrccContenido(4)).rrccPorcentajeA() < 0.2);
    }
}
