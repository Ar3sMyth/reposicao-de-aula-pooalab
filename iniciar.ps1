$ErrorActionPreference = 'Stop'

function Testar-Java($caminho) {
    if (-not $caminho -or -not (Test-Path -LiteralPath $caminho -PathType Leaf)) {
        return $false
    }
    $ErrorActionPreference = 'Continue'
    $saida = & $caminho -version 2>&1 | Out-String
    return ($LASTEXITCODE -eq 0 -and $saida -match 'version "(?:1\.8|[89]|[1-9][0-9]+)(?=[."+-])')
}

function Instalar-Java {
    $arquitetura = switch ([Environment]::GetEnvironmentVariable('PROCESSOR_ARCHITECTURE')) {
        'AMD64' { 'x64' }
        'ARM64' { 'aarch64' }
        default { throw 'O download automatico requer Windows x64 ou ARM64. Instale Java 8 ou superior.' }
    }
    $pacote = @{
        x64 = @('21.0.13.1','2','e15ee1e54a5a3b38027a09cedfa87bc3348d7dd3ce50866dd3d9671d7fffc6c1')
        aarch64 = @('21.0.12.1','1','e82cc17e0bf89a25b0b0ed106d072f2ea420587d0a6870534b71b1dce3ae28c3')
    }
    $versao, $build, $sha256 = $pacote[$arquitetura]
    $pasta = (New-Item -ItemType Directory -Force -Path (Join-Path $PSScriptRoot '.java')).FullName
    $destino = Join-Path $pasta "temurin-$arquitetura-$versao-$build"
    $javaLocal = Join-Path $destino 'bin\java.exe'
    if (Testar-Java $javaLocal) { return $javaLocal }

    $temporario = Join-Path $pasta ([Guid]::NewGuid().ToString('N'))
    $arquivo = Join-Path $temporario 'java.zip'
    $extraido = Join-Path $temporario 'extraido'
    $url = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-$versao%2B$build/OpenJDK21U-jre_${arquitetura}_windows_hotspot_${versao}_$build.zip"

    Write-Host 'Baixando o Java Temurin para esta pasta. Aguarde a primeira instalacao.'
    $ProgressPreference = 'SilentlyContinue'
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    New-Item -ItemType Directory -Path $temporario | Out-Null
    try {
        Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $arquivo
        $algoritmo = [Security.Cryptography.SHA256]::Create()
        $stream = [IO.File]::OpenRead($arquivo)
        try {
            $hash = [BitConverter]::ToString($algoritmo.ComputeHash($stream)).Replace('-', '')
        } finally {
            $stream.Dispose()
            $algoritmo.Dispose()
        }
        if ($hash -ne $sha256) {
            throw 'O arquivo baixado nao passou na verificacao SHA-256.'
        }
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        [IO.Compression.ZipFile]::ExtractToDirectory($arquivo, $extraido)
        $raiz = Get-ChildItem -LiteralPath $extraido -Directory | Select-Object -First 1
        if (-not $raiz -or -not (Testar-Java (Join-Path $raiz.FullName 'bin\java.exe'))) {
            throw 'Nao foi possivel executar o Java baixado.'
        }
        try {
            [IO.Directory]::Move($raiz.FullName, $destino)
        } catch [IO.IOException] {
            if (-not (Testar-Java $javaLocal)) { throw }
        }
    } finally {
        $temporarioReal = (Resolve-Path -LiteralPath $temporario).Path
        if ([IO.Path]::GetDirectoryName($temporarioReal) -eq $pasta) {
            Remove-Item -LiteralPath $temporarioReal -Recurse -Force
        }
    }
    return $javaLocal
}

try {
    Set-Location -LiteralPath $PSScriptRoot
    $jar = Join-Path $PSScriptRoot 'reposicao-de-aula.jar'
    if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) {
        throw 'O JAR esta faltando. Extraia o ZIP completo antes de iniciar.'
    }
    $java = (Get-Command java.exe -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1).Source
    if (-not (Testar-Java $java)) {
        $java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' }
    }
    if (-not (Testar-Java $java)) { $java = Instalar-Java }
    & $java -jar $jar --porta=0 --abrir @args
    exit $LASTEXITCODE
} catch {
    Write-Host ("Falha ao iniciar: " + $_.Exception.Message) -ForegroundColor Red
    Write-Host 'Confira se a pasta descompactada permite gravacao. O primeiro download precisa de internet.'
    exit 1
}
