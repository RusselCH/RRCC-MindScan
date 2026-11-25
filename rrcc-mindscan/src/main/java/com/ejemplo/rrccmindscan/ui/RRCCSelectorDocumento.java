package com.ejemplo.rrccmindscan.ui;

import com.ejemplo.rrccmindscan.model.RRCCDocumento;
import javafx.scene.control.ComboBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class RRCCSelectorDocumento {

    private final ComboBox<String> rrccCombo;
    private final List<RRCCDocumento> rrccDocumentos = new ArrayList<>();
    private Consumer<RRCCDocumento> rrccAlCambiar;
    private boolean rrccCargando;

    public RRCCSelectorDocumento(ComboBox<String> rrccCombo) {
        this.rrccCombo = rrccCombo;
        rrccCombo.setVisibleRowCount(14);
        rrccCombo.getSelectionModel().selectedIndexProperty().addListener((rrccObs, rrccAntes, rrccAhora) -> {
            if (!rrccCargando && rrccAlCambiar != null) {
                rrccAlCambiar.accept(rrccSeleccionado());
            }
        });
    }

    public void rrccAlCambiar(Consumer<RRCCDocumento> rrccAccion) {
        this.rrccAlCambiar = rrccAccion;
    }

    public void rrccCargar(List<RRCCDocumento> rrccLista) {
        Long rrccPrevio = rrccSeleccionado() == null ? null : rrccSeleccionado().getRrccId();
        rrccCargando = true;
        try {
            rrccDocumentos.clear();
            rrccDocumentos.addAll(rrccLista);
            rrccCombo.getItems().setAll(rrccLista.stream().map(RRCCFormatos::rrccEtiquetaDocumento).toList());
            rrccCombo.getSelectionModel().clearSelection();
            rrccMarcar(rrccPrevio);
        } finally {
            rrccCargando = false;
        }
    }

    public boolean rrccSeleccionar(Long rrccId) {
        boolean rrccCambio = rrccSeleccionado() == null || !Objects.equals(rrccSeleccionado().getRrccId(), rrccId);
        rrccCargando = true;
        try {
            rrccMarcar(rrccId);
        } finally {
            rrccCargando = false;
        }
        return rrccCambio && rrccSeleccionado() != null;
    }

    private void rrccMarcar(Long rrccId) {
        if (rrccId == null) {
            return;
        }
        for (int rrccI = 0; rrccI < rrccDocumentos.size(); rrccI++) {
            if (rrccId.equals(rrccDocumentos.get(rrccI).getRrccId())) {
                rrccCombo.getSelectionModel().select(rrccI);
                return;
            }
        }
    }

    public RRCCDocumento rrccSeleccionado() {
        int rrccI = rrccCombo.getSelectionModel().getSelectedIndex();
        return rrccI >= 0 && rrccI < rrccDocumentos.size() ? rrccDocumentos.get(rrccI) : null;
    }

    public List<RRCCDocumento> rrccDocumentos() {
        return List.copyOf(rrccDocumentos);
    }
}
