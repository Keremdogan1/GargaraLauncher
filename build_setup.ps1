$ErrorActionPreference = 'Stop'

# 1. Download and Extract OpenJDK 17 JRE
$webClient = New-Object System.Net.WebClient
if (-not (Test-Path "target\jre")) {
    Write-Host "Downloading JRE..."
    $webClient.DownloadFile("https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.11%2B9/OpenJDK17U-jre_x64_windows_hotspot_17.0.11_9.zip", "target\jre.zip")
    Write-Host "Extracting JRE..."
    if (Test-Path "target\jre_extracted") { Remove-Item -Path "target\jre_extracted" -Recurse -Force }
    Expand-Archive -Path "target\jre.zip" -DestinationPath "target\jre_extracted" -Force
    Rename-Item -Path "target\jre_extracted\jdk-17.0.11+9-jre" -NewName "jre"
    if (Test-Path "target\jre") { Remove-Item -Path "target\jre" -Recurse -Force }
    Move-Item -Path "target\jre_extracted\jre" -Destination "target"
} else {
    Write-Host "JRE already exists in target\jre, skipping download."
}

# 2. Download and Install Inno Setup locally
Write-Host "Downloading Inno Setup..."
$webClient.DownloadFile("https://jrsoftware.org/download.php/is.exe", "target\inno.exe")
Write-Host "Installing Inno Setup..."
Start-Process -FilePath "target\inno.exe" -ArgumentList "/VERYSILENT /DIR=`"c:\Programming_Folder\Java\GargaraInstaller\target\inno`"" -Wait -NoNewWindow





# 5. Compile the setup
Write-Host "Compiling Inno Setup..."
& "target\inno\ISCC.exe" "setup.iss"
Write-Host "Done!"
