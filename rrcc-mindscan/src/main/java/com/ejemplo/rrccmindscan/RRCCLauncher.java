package com.ejemplo.rrccmindscan;

import javafx.application.Application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RRCCLauncher {

    public static final String RRCC_CARPETA_DATOS = "RRCCMindScan";

    public static void main(String[] rrccArgs) throws IOException {
        Files.createDirectories(Path.of(System.getProperty("user.home"), RRCC_CARPETA_DATOS));
        Application.launch(RRCCJavaFxApplication.class, rrccArgs);
    }
}
