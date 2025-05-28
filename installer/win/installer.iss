[Setup]
AppName=Mass++
AppVersion=4.0.0
DefaultDirName={commonpf64}\mspp4
DefaultGroupName=Mass++
Compression=lzma2
SolidCompression=yes
SourceDir=.
OutputBaseFilename=Mass++Setup

[Files]
Source: "..\..\MassPlusPlus\*"; DestDir: "{app}"; Flags: recursesubdirs createallsubdirs

[Icons]
Name: "{group}\Mass++"; Filename: "{app}\MassPlusPlus.exe"