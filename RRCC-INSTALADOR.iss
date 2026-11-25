#define RRCCNombre "RRCC MindScan"
#define RRCCVersion "2.0"
#define RRCCAutor "Rodrigo Russel Chura Chailña"
#define RRCCExe "RRCCMindScan.exe"

[Setup]
AppId={{6B1F2C7A-4E8D-4B6A-9C3E-2D5F7A1B8C90}
AppName={#RRCCNombre}
AppVersion={#RRCCVersion}
AppVerName={#RRCCNombre} {#RRCCVersion}
AppPublisher={#RRCCAutor}
AppCopyright=Copyright (C) 2026 {#RRCCAutor}
VersionInfoVersion={#RRCCVersion}.0.0
VersionInfoDescription=Instalador de {#RRCCNombre}
DefaultDirName={autopf}\{#RRCCNombre}
DefaultGroupName={#RRCCNombre}
DisableProgramGroupPage=yes
DisableWelcomePage=no
DisableDirPage=no
DisableReadyPage=no
LicenseFile=LICENCIA.txt
InfoBeforeFile=LEEME.txt
OutputDir=RRCC-INSTALADOR
OutputBaseFilename=RRCCMindScan-Instalador-v{#RRCCVersion}
SetupIconFile=RRCC-EXE\RRCCMindScan\RRCCMindScan.ico
UninstallDisplayIcon={app}\{#RRCCExe}
UninstallDisplayName={#RRCCNombre} {#RRCCVersion}
Compression=lzma2/max
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=lowest
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
MinVersion=10.0

[Languages]
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"

[Tasks]
Name: "escritorio"; Description: "Crear un acceso directo en el escritorio"; GroupDescription: "Accesos directos:"

[Files]
Source: "RRCC-EXE\RRCCMindScan\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#RRCCNombre}"; Filename: "{app}\{#RRCCExe}"
Name: "{group}\Desinstalar {#RRCCNombre}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#RRCCNombre}"; Filename: "{app}\{#RRCCExe}"; Tasks: escritorio

[Run]
Filename: "{app}\{#RRCCExe}"; Description: "Abrir {#RRCCNombre} ahora"; Flags: nowait postinstall skipifsilent
