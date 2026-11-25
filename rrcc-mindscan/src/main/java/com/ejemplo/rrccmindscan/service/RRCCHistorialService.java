package com.ejemplo.rrccmindscan.service;

import com.ejemplo.rrccmindscan.model.RRCCComparacion;
import com.ejemplo.rrccmindscan.model.RRCCDocumento;
import com.ejemplo.rrccmindscan.model.RRCCRevision;
import com.ejemplo.rrccmindscan.repository.RRCCComparacionRepository;
import com.ejemplo.rrccmindscan.repository.RRCCRevisionRepository;
import com.ejemplo.rrccmindscan.service.RRCCComparadorService.RRCCResultadoComparacion;
import com.ejemplo.rrccmindscan.service.RRCCRevisorService.RRCCResultadoRevision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RRCCHistorialService {

    private final RRCCComparacionRepository rrccComparaciones;
    private final RRCCRevisionRepository rrccRevisiones;

    @Transactional
    public RRCCComparacion rrccGuardarComparacion(RRCCDocumento rrccA, RRCCDocumento rrccB, RRCCResultadoComparacion rrccR) {
        return rrccComparaciones.save(RRCCComparacion.builder()
                .rrccDocumentoA(rrccA)
                .rrccDocumentoB(rrccB)
                .rrccFecha(LocalDateTime.now())
                .rrccSimilitud(rrccR.rrccSimilitudGlobal())
                .rrccIguales(rrccR.rrccIguales())
                .rrccAgregadas(rrccR.rrccAgregadas())
                .rrccEliminadas(rrccR.rrccEliminadas())
                .rrccModificadas(rrccR.rrccModificadas())
                .build());
    }

    @Transactional
    public RRCCRevision rrccGuardarRevision(RRCCDocumento rrccDoc, RRCCResultadoRevision rrccR) {
        RRCCRevision rrccUltima = rrccRevisiones.rrccListarRecientes().stream()
                .filter(rrccV -> rrccV.getRrccDocumento().getRrccId().equals(rrccDoc.getRrccId()))
                .findFirst().orElse(null);
        if (rrccUltima != null && rrccUltima.getRrccPuntaje() == rrccR.rrccPuntaje()
                && rrccUltima.getRrccErrores() == rrccR.rrccErrores()
                && rrccUltima.getRrccAdvertencias() == rrccR.rrccAdvertencias()
                && rrccUltima.getRrccSugerencias() == rrccR.rrccSugerencias()) {
            return rrccUltima;
        }
        return rrccRevisiones.save(RRCCRevision.builder()
                .rrccDocumento(rrccDoc)
                .rrccFecha(LocalDateTime.now())
                .rrccPuntaje(rrccR.rrccPuntaje())
                .rrccErrores(rrccR.rrccErrores())
                .rrccAdvertencias(rrccR.rrccAdvertencias())
                .rrccSugerencias(rrccR.rrccSugerencias())
                .build());
    }

    public List<RRCCComparacion> rrccComparaciones() {
        return rrccComparaciones.rrccListarRecientes();
    }

    public List<RRCCRevision> rrccRevisiones() {
        return rrccRevisiones.rrccListarRecientes();
    }

    @Transactional
    public void rrccEliminarComparacion(Long rrccId) {
        rrccComparaciones.deleteById(rrccId);
    }

    @Transactional
    public void rrccEliminarRevision(Long rrccId) {
        rrccRevisiones.deleteById(rrccId);
    }
}
