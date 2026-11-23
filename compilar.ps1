$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    New-Item -ItemType Directory -Force build/classes,build/testes | Out-Null
    $fontes = @(Get-ChildItem src/reposicao -Filter '*.java' | ForEach-Object FullName)
    $testes = @(Get-ChildItem tests/reposicao -Filter '*.java' | ForEach-Object FullName)
    javac -encoding UTF-8 -d build/classes @fontes
    if ($LASTEXITCODE -ne 0) { throw 'A compilacao falhou.' }
    Copy-Item resources/estilos.css build/classes/estilos.css
    javac -encoding UTF-8 -cp build/classes -d build/testes @testes
    if ($LASTEXITCODE -ne 0) { throw 'A compilacao dos testes falhou.' }
    java -cp 'build/classes;build/testes' reposicao.EnqueteTest
    if ($LASTEXITCODE -ne 0) { throw 'Os testes falharam.' }
    jar cfe reposicao-de-aula.jar reposicao.Aplicacao -C build/classes .
    if ($LASTEXITCODE -ne 0) { throw 'A geracao do JAR falhou.' }
} finally {
    Pop-Location
}
