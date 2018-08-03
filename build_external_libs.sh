echo "Download and install cramtools-3.0"
# Remove previously downloaded cramtools    
rm -f cramtools-3.0.jar 

# Download cramtools 3.0 from GitHub

curl -LJO -o cramtools-3.0.jar  https://github.com/enasequence/cramtools/raw/master/cramtools-3.0.jar

# Install cramtools locally
mvn install:install-file -Dfile=cramtools-3.0.jar -DgroupId=com.github.cramtools -DartifactId=cramtools  -Dversion=3.0 -Dpackaging=jar

echo "Building and installing phenopackets-schema"
#building and installing phenopackets-schema
cd phenopacket-schema

#get specific commit fot avoiding errors before release
git reset --hard c67527e48f94fa71e8763c3a2a3234917b5c5bcb
./mvn clean install
cd ..

# we have already installed the package, do we need to reinstall it?
mvn install:install-file -Dfile=./phenopacket-schema/target/phenopacket-schema-0.0.7.jar -DgroupId=org.phenopackets.schema.v1 -DartifactId=phenopacket-schema -Dversion=0.0.7-SNAPSHOT -Dpackaging=jar

echo "Building and installing picard"
#building picard
git clone https://github.com/broadinstitute/picard.git
cd picard/
./gradlew shadowJar
cd ..
mvn install:install-file -Dfile=./picard/build/libs/picard.jar  -DgroupId=com.github.picard -DartifactId=picard  -Dversion=2.18.7-SNAPSHOT-all -Dpackaging=jar

echo "Building and installing ega-htsjdk"
#building ega-htsjdk
cd ega-htsjdk
mvn clean install -DskipTests

cd ..
mvn install:install-file -Dfile=./ega-htsjdk/target/ega-htsjdk-1.0-SNAPSHOT.jar -DgroupId=eu.elixir.ega.ebi -DartifactId=ega-htsjdk -Dversion=1.0-SNAPSHOT -Dpackaging=jar

echo "Build docker image for the htsget service"
cd ./htsget-service-snapshot
mvn clean package -DskipTests docker:build

