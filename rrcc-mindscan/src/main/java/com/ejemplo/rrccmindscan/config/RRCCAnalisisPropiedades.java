package com.ejemplo.rrccmindscan.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rrcc.analisis")
public record RRCCAnalisisPropiedades(
        Integer rrccPalabrasPorMinuto,
        Integer rrccPalabrasOracionLarga,
        Double rrccProporcionResumen,
        Integer rrccMaxOracionesGrafo,
        Double rrccUmbralSimilitudContradiccion,
        Integer rrccTamanoMaximoMb
) {
    public RRCCAnalisisPropiedades {
        rrccPalabrasPorMinuto = rrccPalabrasPorMinuto != null ? rrccPalabrasPorMinuto : 200;
        rrccPalabrasOracionLarga = rrccPalabrasOracionLarga != null ? rrccPalabrasOracionLarga : 40;
        rrccProporcionResumen = rrccProporcionResumen != null ? rrccProporcionResumen : 0.25;
        rrccMaxOracionesGrafo = rrccMaxOracionesGrafo != null ? rrccMaxOracionesGrafo : 400;
        rrccUmbralSimilitudContradiccion = rrccUmbralSimilitudContradiccion != null ? rrccUmbralSimilitudContradiccion : 0.45;
        rrccTamanoMaximoMb = rrccTamanoMaximoMb != null ? rrccTamanoMaximoMb : 50;
    }

    public static RRCCAnalisisPropiedades rrccPorDefecto() {
        return new RRCCAnalisisPropiedades(null, null, null, null, null, null);
    }
}
