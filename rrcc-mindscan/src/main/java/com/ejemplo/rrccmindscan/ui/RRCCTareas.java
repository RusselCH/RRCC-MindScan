package com.ejemplo.rrccmindscan.ui;

import javafx.application.Platform;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class RRCCTareas {

    private RRCCTareas() {
    }

    public static <T> void rrccEjecutar(Supplier<T> rrccTrabajo, Consumer<T> rrccAlTerminar, Consumer<String> rrccAlFallar) {
        CompletableFuture.supplyAsync(rrccTrabajo).whenComplete((rrccResultado, rrccError) -> Platform.runLater(() -> {
            if (rrccError == null) {
                rrccAlTerminar.accept(rrccResultado);
                return;
            }
            Throwable rrccCausa = rrccError instanceof CompletionException && rrccError.getCause() != null
                    ? rrccError.getCause() : rrccError;
            rrccAlFallar.accept(rrccCausa.getMessage() == null ? rrccCausa.getClass().getSimpleName() : rrccCausa.getMessage());
        }));
    }
}
