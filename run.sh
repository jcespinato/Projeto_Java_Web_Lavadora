#!/usr/bin/env bash
set -e
mkdir -p out
javac --add-modules jdk.httpserver -encoding UTF-8 -d out src/ConsumoLavadoraWeb.java
java --add-modules jdk.httpserver -cp out ConsumoLavadoraWeb
