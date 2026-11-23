#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes build/testes
case "$(javac -version 2>&1)" in
  "javac 1.8."*) set -- ;;
  *) set -- --release 8 ;;
esac
javac "$@" -encoding UTF-8 -d build/classes src/reposicao/*.java
cp resources/estilos.css resources/ucsal.svg build/classes/
javac -encoding UTF-8 -cp build/classes -d build/testes tests/reposicao/*.java
java -cp "build/classes:build/testes" reposicao.EnqueteTest
jar cfe reposicao-de-aula.jar reposicao.Aplicacao -C build/classes .
