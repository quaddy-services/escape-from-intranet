Name "escape-from-intranet"

# Paths are relative to nsis file (src\main\nsis)

OutFile "..\..\..\target\escape-from-intranet-setup.exe"

SetOverwrite on
AllowSkipFiles off

Icon "..\resources\online_ezx_icon.ico"

RequestExecutionLevel user

InstallDir "$LOCALAPPDATA\quaddy-services\escape-from-intranet"

Page directory
Page instfiles

Section "Installer"

SetShellVarContext current

SetOutPath "$INSTDIR"
# OutPath is workingdirectory for ShortCuts, too

ClearErrors
File ..\resources\online_ezx_icon.ico
IfErrors 0 +3
  MessageBox MB_ICONSTOP "online_ezx_icon.ico can not be copied it is in use"
  Abort
   
ClearErrors
File ..\..\..\target\escape-from-intranet.jar
IfErrors 0 +3
  MessageBox MB_ICONSTOP "escape-from-intranet.jar can not be copied"
  Abort

CreateDirectory "$SMPROGRAMS\Quaddy Services"

var /GLOBAL JAVAEXE

# javaw is on the path ? 
SearchPath $JAVAEXE "javaw.exe"
DetailPrint 'JAVAEXE via SearchPath=$JAVAEXE'
# in case it is in the path, let windows search for javaw.exe each time it is starte
# (to avoid setting versioned path informations e.g.
#   C:\Program Files\Amazon Corretto\jdk11.0.3_7\bin )
IfFileExists $JAVAEXE JreFound

var /GLOBAL ProgramData
ReadEnvStr $ProgramData ProgramData
DetailPrint 'ProgramData=$ProgramData'
StrCpy $JAVAEXE "$ProgramData\Oracle\Java\javapath\javaw.exe"
DetailPrint 'JAVAEXE via ProgramData=$JAVAEXE'
IfFileExists $JAVAEXE JreFound

ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
DetailPrint 'javaCurrentVersion=$R1'
ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R1" "JavaHome"
DetailPrint 'JavaHome=$R0'
StrCpy $JAVAEXE "$R0\bin\javaw.exe"

DetailPrint 'JAVAEXE via Reg=$JAVAEXE'

IfFileExists $JAVAEXE JreFound

  MessageBox MB_ICONSTOP "No javaw.exe here: $JAVAEXE"
  Abort

JreFound:


# In case of other working dir for Shortcut: SetOutPath "$INSTDIR"
CreateShortCut \
   "$SMPROGRAMS\Quaddy Services\escape-from-intranet.lnk" \
    "$JAVAEXE" '-jar "$INSTDIR\escape-from-intranet.jar"' \
   '$INSTDIR\online_ezx_icon.ico'

CreateShortCut \
   "$SMPROGRAMS\Startup\escape-from-intranet.lnk" \
    "$JAVAEXE" '-jar "$INSTDIR\escape-from-intranet.jar' \
   '$INSTDIR\online_ezx_icon.ico'

CreateShortCut \
   "$SMPROGRAMS\Quaddy Services\Uninstall escape-from-intranet.lnk" \
    "$INSTDIR\Uninst.exe" \
    '$INSTDIR\Uninst.exe'

WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\escape-from-intranet" \
                 "DisplayName" "escape-from-intranet"

WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\escape-from-intranet" \
                 "DisplayVersion" "${displayVersion}"

WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\escape-from-intranet" \
                 "Publisher" "Quaddy Services"

WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\escape-from-intranet" \
                 "UninstallString" "$INSTDIR\Uninst.exe"

WriteUninstaller $INSTDIR\Uninst.exe


SectionEnd

# start application

Function .onInstSuccess
	# start application
	ExpandEnvStrings $5 "$JAVAEXE"
	Exec '"$5" -jar "$INSTDIR\escape-from-intranet.jar"'
FunctionEnd


UninstPage uninstConfirm
UninstPage instfiles
Section "Uninstall"
  SetShellVarContext current

  DetailPrint 'INSTDIR=$INSTDIR'

  DeleteRegKey HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\escape-from-intranet"
  Delete "$INSTDIR\install.log"
  Delete "$INSTDIR\Uninst.exe" ; delete self (see explanation below why this works)

  Delete "$INSTDIR\escape-from-intranet.jar"
  Delete "$INSTDIR\online_ezx_icon.ico"
  RMDir "$INSTDIR"

  Delete "$SMPROGRAMS\Startup\escape-from-intranet.lnk"
  
  Delete "$SMPROGRAMS\Quaddy Services\escape-from-intranet.lnk"
  Delete "$SMPROGRAMS\Quaddy Services\Uninstall escape-from-intranet.lnk"
  RMDir "$SMPROGRAMS\Quaddy Services"

SectionEnd

