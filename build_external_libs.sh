# building and installing cramtools
curl -LJO -o cramtools-3.0.jar  https://github.com/enasequence/cramtools/blob/master/cramtools-3.0.jar?raw=true 
mv cramtools-3.0.jar?raw=true cramtools-3.0.jar
mvn install:install-file -Dfile=cramtools-3.0.jar -DgroupId=com.github.cramtools -DartifactId=cramtools  -Dversion=3.0 -Dpackaging=jar
#building and installing phenopackets-schema
cd phenopacket-schema
#get specific commit fot avoiding errors before release
git reset --hard c67527e48f94fa71e8763c3a2a3234917b5c5bcb
./mvnw clean install
cd ..
mvn install:install-file -Dfile=./phenopacket-schema/target/phenopacket-schema-0.0.7-SNAPSHOT.jar -DgroupId=org.phenopackets.schema.v1 -DartifactId=phenopacket-schema -Dversion=0.0.7-SNAPSHOT -Dpackaging=jar
#building picard
cd picard
./gradlew shadowJar
cd ..
mvn install:install-file -Dfile=./picard/build/libs/picard-2.18.11-SNAPSHOT-all.jar  -DgroupId=com.github.picard -DartifactId=picard  -Dversion=2.18.7-SNAPSHOT-all -Dpackaging=jar
#building ega-htsjdk
cd ega-htsjdk
mvn package -DskipTests
cd ..
mvn install:install-file -Dfile=./ega-htsjdk/target/ega-htsjdk-1.0-SNAPSHOT.jar -DgroupId=eu.elixir.ega.ebi -DartifactId=ega-htsjdk -Dversion=1.0-SNAPSHOT -Dpackaging=jar