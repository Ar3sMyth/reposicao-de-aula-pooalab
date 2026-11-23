#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes build/testes
javac -encoding UTF-8 -d build/classes src/reposicao/*.java
cp resources/estilos.css build/classes/
javac -encoding UTF-8 -cp build/classes -d build/testes tests/reposicao/*.java
java -cp "build/classes:build/testes" reposicao.EnqueteTest
jar cfe reposicao-de-aula.jar reposicao.Aplicacao -C build/classes .
