Name "escape-from-intranet"

# Paths are relative to nsis file (src\main\nsis)

OutFile "..\..\..\target\escape-from-intranet.exe"

Icon "..\resources\online_ezx_icon.ico"

RequestExecutionLevel user

InstallDir "$LOCALAPPDATA\quaddy-services\escape-from-intranet"

Page directory
Page instfiles

Section "Installer"

SetShellVarContext current

SetOutPath "$INSTDIR"
# OutPath is workingdirectory for ShortCuts, too
File ..\resources\online_ezx_icon.ico
File ..\..\..\target\escape-from-intranet.jar

CreateDirectory "$SMPROGRAMS\Quaddy Services"

ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
DetailPrint 'javaCurrentVersion=$R1'
ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R1" "JavaHome"
DetailPrint 'JavaHome=$R0'
var /GLOBAL JAVAEXE
StrCpy $JAVAEXE "$R0\bin\javaw.exe"

DetailPrint 'JAVAEXE=$JAVAEXE'

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

WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\escape-from-intranet" \
                 "DisplayName" "escape-from-intranet"

WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\escape-from-intranet" \
                 "DisplayVersion" "${displayVersion}"

WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\escape-from-intranet" \
                 "Publisher" "Quaddy Services"

WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\escape-from-intranet" \
                 "UninstallString" "$\"$INSTDIR\Uninst.exe$\""

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
  Delete $INSTDIR\Uninst.exe ; delete self (see explanation below why this works)

  Delete $INSTDIR\escape-from-intranet.jar
  Delete $INSTDIR\online_ezx_icon.ico
  RMDir $INSTDIR

  Delete "$SMPROGRAMS\Quaddy Services\escape-from-intranet.lnk"
  Delete "$SMPROGRAMS\Startup\escape-from-intranet.lnk"
  RMDir "$SMPROGRAMS\Quaddy Services"

SectionEnd

