#!/bin/bash
git clone https://github.com/elixir-europe/ega-data-api-v3-cipher.git
mvn -f /ega-data-api-v3-cipher/pom.xml install
git clone https://github.com/elixir-europe/ega-data-api-v3-dataedge.git
mvn -f /ega-data-api-v3-dataedge/pom.xml install
mv /ega-data-api-v3-dataedge/target/DataEdgeService-0.0.1-SNAPSHOT.war /EGA_build
mv /ega-data-api-v3-dataedge/docker/dsedged.sh /EGA_build
mv /ega-data-api-v3-dataedge/docker/Dockerfile_Deploy /EGA_build
