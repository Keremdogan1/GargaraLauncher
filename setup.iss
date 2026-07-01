[Setup]
AppName=Gargara Launcher
AppVersion=3.2.5
VersionInfoVersion=3.2.5.0
VersionInfoCompany=Karsoft
VersionInfoDescription=Gargara Launcher Kurulumu
VersionInfoProductName=Gargara Launcher
DefaultDirName={autopf}\GargaraLauncher
DefaultGroupName=Gargara Launcher
OutputDir=../Releases
OutputBaseFilename=GargaraSetup_v3_2_5
Compression=lzma2/ultra64
SolidCompression=yes
SetupIconFile=src\main\resources\logo.ico
UninstallDisplayIcon={app}\logo.ico
PrivilegesRequired=admin

[Languages]
Name: "turkish"; MessagesFile: "compiler:Languages\Turkish.isl"

[Files]
Source: "target\GargaraLauncher-1.0-SNAPSHOT-jar-with-dependencies.jar"; DestDir: "{app}"; DestName: "GargaraLauncher.jar"; Flags: ignoreversion
Source: "target\jre\*"; DestDir: "{app}\jre"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "src\main\resources\logo.ico"; DestDir: "{app}"; Flags: ignoreversion
Source: "Launch.bat"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\Gargara Launcher"; Filename: "{app}\Launch.bat"; IconFilename: "{app}\logo.ico"
Name: "{userdesktop}\Gargara Launcher"; Filename: "{app}\Launch.bat"; IconFilename: "{app}\logo.ico"; Tasks: desktopicon

[Tasks]
Name: "desktopicon"; Description: "Masaüstüne Kısayol Oluştur"; GroupDescription: "Ek Seçenekler:"
Name: "downloadextras"; Description: "Opsiyonel Görsel Paketleri (Shader ve Texture) İndir ve Kur"; GroupDescription: "Ek Seçenekler:"

[Run]
Filename: "{app}\Launch.bat"; Description: "Gargara Launcher'ı Başlat"; Flags: postinstall nowait runhidden

[UninstallDelete]
Type: filesandordirs; Name: "{app}"
Type: filesandordirs; Name: "{userappdata}\.gargara_launcher"

[Code]
procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssPostInstall then
  begin
    if IsTaskSelected('downloadextras') then
    begin
      CreateDir(ExpandConstant('{userappdata}\.gargara_launcher'));
      SaveStringToFile(ExpandConstant('{userappdata}\.gargara_launcher\extras.flag'), 'true', False);
    end;
  end;
end;
