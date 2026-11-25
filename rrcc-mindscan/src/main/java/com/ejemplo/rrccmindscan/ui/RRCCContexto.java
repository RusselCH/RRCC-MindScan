package com.ejemplo.rrccmindscan.ui;

import org.springframework.stereotype.Component;

@Component
public class RRCCContexto {

    private Long rrccDocumentoActivo;
    private Long rrccComparacionA;
    private Long rrccComparacionB;
    private String rrccBusquedaPendiente;

    public void rrccActivar(Long rrccId) {
        this.rrccDocumentoActivo = rrccId;
    }

    public Long rrccTomarActivo() {
        Long rrccId = rrccDocumentoActivo;
        rrccDocumentoActivo = null;
        return rrccId;
    }

    public void rrccPrepararComparacion(Long rrccA, Long rrccB) {
        this.rrccComparacionA = rrccA;
        this.rrccComparacionB = rrccB;
    }

    public Long[] rrccTomarComparacion() {
        Long[] rrccPar = {rrccComparacionA, rrccComparacionB};
        rrccComparacionA = null;
        rrccComparacionB = null;
        return rrccPar;
    }

    public void rrccPrepararBusqueda(String rrccConsulta) {
        this.rrccBusquedaPendiente = rrccConsulta;
    }

    public String rrccTomarBusqueda() {
        String rrccConsulta = rrccBusquedaPendiente;
        rrccBusquedaPendiente = null;
        return rrccConsulta;
    }
}
