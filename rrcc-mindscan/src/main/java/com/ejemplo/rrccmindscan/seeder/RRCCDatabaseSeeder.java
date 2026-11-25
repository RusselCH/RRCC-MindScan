package com.ejemplo.rrccmindscan.seeder;

import com.ejemplo.rrccmindscan.service.RRCCDocumentoService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rrcc.seeder.habilitado", havingValue = "true", matchIfMissing = true)
public class RRCCDatabaseSeeder {

    private static final Logger RRCC_LOG = LoggerFactory.getLogger(RRCCDatabaseSeeder.class);

    public static final String RRCC_REGLAMENTO_V1 = """
            REGLAMENTO DE USO DEL LABORATORIO DE CÓMPUTO

            Artículo 1. Objeto. El presente reglamento establece las normas de uso del laboratorio de cómputo de la Escuela Profesional de Ingeniería de Sistemas.

            Artículo 2. Horario. El laboratorio atiende de lunes a viernes de 7:00 a 20:00 horas. Los sábados permanece cerrado.

            Artículo 3. Acceso. Los estudiantes deben presentar su carné universitario vigente para ingresar. El ingreso con alimentos o bebidas está prohibido.

            Artículo 4. Reservas. Las reservas para prácticas libres se solicitan con 48 horas de anticipación al correo laboratorio@upeu.edu.pe.

            Artículo 5. Equipos. Cada estudiante es responsable del equipo asignado durante la sesión. Está prohibido instalar software sin autorización del docente responsable.

            Artículo 6. Daños. En caso de daño por mal uso, el estudiante asumirá el costo de la reparación, que no excederá S/ 500.00.

            Artículo 7. Sanciones. El incumplimiento de este reglamento será sancionado con la suspensión del acceso por 15 días.

            Artículo 8. Vigencia. El presente reglamento entra en vigencia a partir del 1 de marzo de 2026.
            """;

    public static final String RRCC_REGLAMENTO_V2 = """
            REGLAMENTO DE USO DEL LABORATORIO DE CÓMPUTO

            Artículo 1. Objeto. El presente reglamento establece las normas de uso del laboratorio de cómputo de la Escuela Profesional de Ingeniería de Sistemas.

            Artículo 2. Horario. El laboratorio atiende de lunes a sábado de 7:00 a 21:00 horas. Los domingos permanece cerrado.

            Artículo 3. Acceso. Los estudiantes deben presentar su carné universitario vigente para ingresar. El ingreso con alimentos o bebidas está prohibido.

            Artículo 4. Reservas. Las reservas para prácticas libres se solicitan con 24 horas de anticipación mediante el sistema de reservas del campus virtual.

            Artículo 5. Equipos. Cada estudiante es responsable del equipo asignado durante la sesión. Está prohibido instalar software sin autorización del docente responsable.

            Artículo 6. Uso de la red. El acceso a la red inalámbrica del laboratorio requiere autenticación con el usuario institucional.

            Artículo 7. Sanciones. El incumplimiento de este reglamento será sancionado con la suspensión del acceso por 30 días y la comunicación a la Dirección de Escuela.

            Artículo 8. Vigencia. El presente reglamento entra en vigencia a partir del 1 de agosto de 2026.
            """;

    public static final String RRCC_INFORME = """
            INFORME TÉCNICO N.° 014-2026 SOBRE EL ESTADO DEL LABORATORIO 3

            Responsable: Ing. Carla Mendoza Quispe, coordinadora de infraestructura tecnológica. Correo: cmendoza@upeu.edu.pe, teléfono 951 234 567.

            1. Antecedentes

            El laboratorio 3 cuenta con 30 computadoras operativas y un servidor de archivos. La última revisión general se realizó el 31/02/2026 según el registro de mantenimiento. El objetivo de este informe es describir el estado actual de los equipos y proponer acciones de mejora.

            2. Diagnóstico

            Durante la inspección se verificó que el antivirus está actualizado en todos los equipos. Sin embargo, se detectaron fallas de de conexión en el switch principal que afectan a la fila C.  La velocidad promedio de la red fue de 85 Mbps , por debajo del valor esperado de 100 Mbps.

            Se comprobó que las 28 computadoras operativas del laboratorio tienen instalado el software académico requerido para el semestre. el sistema operativo de los equipos corresponde a la versión aprobada por la oficina de tecnología.

            El antivirus no está actualizado en todos los equipos y requiere una revisión inmediata.

            La memoria RAM de los equipos es suficiente para las prácticas de virtualización. La memoria RAM de los equipos es insuficiente para las prácticas de virtualización con más de dos máquinas virtuales simultáneas, por lo que se recomienda ampliarla.

            3. Presupuesto estimado

            La ampliación de memoria tiene un costo de S/ 4,800.00 y el cambio del switch principal asciende a S/ 2,350.50. El monto total representa el 12% del presupuesto anual de mantenimiento (ver detalle en https://intranet.upeu.edu.pe/mantenimiento).

            4. Recomendaciones

            Se recomienda programar el mantenimiento correctivo antes del 15 de diciembre de 2026, reemplazar el switch principal y ampliar la memoria de los equipos que se utilizan en los cursos de virtualización y redes, considerando que la mayoría de las prácticas de laboratorio del siguiente semestre requieren ejecutar varias máquinas virtuales al mismo tiempo para simular servicios de red, servidores web y bases de datos en un entorno controlado (lo que supone un uso intensivo de memoria.

            Se recomienda programar el mantenimiento correctivo antes del 15 de diciembre de 2026, reemplazar el switch principal y ampliar la memoria de los equipos que se utilizan en los cursos de virtualización y redes, considerando que la mayoría de las prácticas de laboratorio del siguiente semestre requieren ejecutar varias máquinas virtuales al mismo tiempo para simular servicios de red, servidores web y bases de datos en un entorno controlado (lo que supone un uso intensivo de memoria.
            """;

    public static final String RRCC_CONTRATO = """
            CONTRATO DE PRESTACIÓN DE SERVICIOS DE MANTENIMIENTO INFORMÁTICO

            Conste por el presente documento el contrato de prestación de servicios que celebran, de una parte, la Universidad Peruana Unión, con RUC 20138122256, con domicilio en Carretera Arequipa km 6, Juliaca, en adelante LA UNIVERSIDAD; y de la otra parte, TecnoSur Servicios Generales S.A.C., con RUC 20601234567, representada por su gerente general, el señor Luis Alberto Ramos Huanca, identificado con DNI 45781236, en adelante EL CONTRATISTA.

            Cláusula primera. Objeto. EL CONTRATISTA se obliga a brindar el servicio de mantenimiento preventivo y correctivo de los equipos de cómputo de los laboratorios 1, 2 y 3.

            Cláusula segunda. Plazo. El plazo de ejecución del servicio es de 90 días calendario, contados desde el 2 de enero de 2027.

            Cláusula tercera. Monto. El monto total del contrato asciende a S/ 18,500.00 incluido el IGV. El pago se realizará en tres armadas iguales de S/ 6,166.67 previa conformidad del área usuaria.

            Cláusula cuarta. Penalidades. En caso de retraso injustificado, EL CONTRATISTA pagará una penalidad diaria equivalente al 0.5% del monto contractual, hasta un máximo del 10%.

            Cláusula quinta. Obligaciones de EL CONTRATISTA. Asignar personal técnico calificado, reportar cada intervención mediante un informe firmado y mantener la confidencialidad de la información de LA UNIVERSIDAD.

            Cláusula sexta. Resolución. Cualquiera de las partes podrá resolver el contrato ante el incumplimiento de las obligaciones, previa comunicación notarial con 15 días de anticipación.

            Cláusula séptima. Jurisdicción. Para cualquier controversia, las partes se someten a la jurisdicción de los jueces de Juliaca. Contacto del contratista: soporte@tecnosur.pe, teléfono (051) 321 4567.
            """;

    public static final String RRCC_ACTA = """
            ACTA DE REUNIÓN N.° 07-2026 DEL COMITÉ ACADÉMICO

            En la ciudad de Juliaca, siendo las 10:00 horas del 18 de noviembre de 2026, se reunieron en la sala de docentes los miembros del comité académico de la Escuela Profesional de Ingeniería de Sistemas.

            Asistentes: Dr. Jorge Paredes Lima (director de escuela), Mg. Rosa Condori Apaza (coordinadora académica), Ing. Carla Mendoza Quispe (coordinadora de infraestructura) y el delegado estudiantil Marco Huamán.

            Agenda: 1) Resultados de la evaluación de medio semestre. 2) Estado de los laboratorios. 3) Programación de cursos de verano.

            Acuerdo 1. Se aprueba publicar los resultados de la evaluación de medio semestre en el campus virtual hasta el 25 de noviembre de 2026. Responsable: Mg. Rosa Condori Apaza.

            Acuerdo 2. Se aprueba solicitar a la Dirección General de Administración el presupuesto de S/ 7,150.50 para el mantenimiento del laboratorio 3. Responsable: Ing. Carla Mendoza Quispe.

            Acuerdo 3. Los cursos de verano se dictarán del 5 de enero al 14 de febrero de 2027 en modalidad presencial. La matrícula será obligatoria a través del sistema académico.

            Acuerdo 4. El delegado estudiantil coordinará la encuesta de satisfacción con un mínimo de 80% de participación.

            Siendo las 12:30 horas, se levantó la sesión y firmaron los asistentes en señal de conformidad.
            """;

    private final RRCCDocumentoService rrccDocumentos;

    @EventListener(ApplicationReadyEvent.class)
    public void rrccSembrar() {
        if (rrccDocumentos.rrccContar() > 0) {
            return;
        }
        rrccDocumentos.rrccImportarTexto("Reglamento laboratorio v1.txt", RRCC_REGLAMENTO_V1);
        rrccDocumentos.rrccImportarTexto("Reglamento laboratorio v2.txt", RRCC_REGLAMENTO_V2);
        rrccDocumentos.rrccImportarTexto("Informe tecnico laboratorio 3.txt", RRCC_INFORME);
        rrccDocumentos.rrccImportarTexto("Contrato mantenimiento TecnoSur.txt", RRCC_CONTRATO);
        rrccDocumentos.rrccImportarTexto("Acta comite academico 07-2026.txt", RRCC_ACTA);
        RRCC_LOG.info("Biblioteca de ejemplo creada con 5 documentos");
    }
}
