# How to deploy Demo application locally

*NOTE: This instruction assumes that you already have local working biosamples service.*


You can use for setting up local versions of biosamples and htsget service using [shell script](./build_external_libs.sh). This setting up needed only for first running.
Then for deploying all required services use [shell script](./run_all_services.sh). You can deploy this demo manually as alternative following instructions below:

## Phenopackets exportation

Build and install phenopackets
1. Build phenopacket-schema from [the github repository](https://github.com/phenopackets/phenopacket-schema) and following 
the [building instructions](https://github.com/phenopackets/phenopacket-schema#building). *NOTE* that you should get the specific commit with hash `c67527e48f94fa71e8763c3a2a3234917b5c5bcb`.

```bash
# Building and installing phenopackets

git clone https://github.com/phenopackets/phenopacket-schema.git
cd phenopacket-schema

# Get specific commit fot avoiding errors before release

git reset --hard c67527e48f94fa71e8763c3a2a3234917b5c5bcb
mvn clean install
```

2. Install built jar as local lib from the phenopacket-shchema:

```bash
# Install the package on your local machine

mvn install:install-file -Dfile=./target/phenopacket-schema-0.0.7-SNAPSHOT.jar -DgroupId=org.phenopackets.schema.v1 -DartifactId=phenopacket-schema -Dversion=0.0.7-SNAPSHOT -Dpackaging=jar
```

## Biosamples search

1. Get from Github the [EGA-dataedge](https://github.com/Dilschat/ega-dataedge/tree/experimental_deploy) project configured to run as a local servicebash

```bash
git clone -b experimental_deploy --single-branch https://github.com/Dilschat/ega-dataedge.git
```

2. Build and install ega-dataedge required tools

- Build and install [picard](https://github.com/broadinstitute/picard)

```bash
# Clone picard repository
git clone https://github.com/broadinstitute/picard.git

# Build and install picard locally
cd picard
gradle shadowJar
mvn install:install-file -Dfile=./build/libs/picard-2.18.11-SNAPSHOT-all.jar  -DgroupId=com.github.picard -DartifactId=picard  -Dversion=2.18.7-SNAPSHOT-all -Dpackaging=jar
```

- Build and install cramtools downloading [this file](https://github.com/enasequence/cramtools/blob/master/cramtools-3.0.jar?raw=true)

```bash
# Download the cramtools file
curl -LJO -o cramtools-3.0.jar https://github.com/enasequence/cramtools/raw/master/cramtools-3.0.jar 

# Build and install the cramtool files locally
mvn install:install-file -Dfile=cramtools-3.0.jar -DgroupId=com.github.cramtools -DartifactId=cramtools  -Dversion=3.0 -Dpackaging=jar
```

- Build and install [ega-htsjdk](https://github.com/EGA-archive/ega-htsjdk)

```bash
# Clone the git repository
git clone https://github.com/EGA-archive/ega-htsjdk.git

# Build and install ega-htsjdk locally
cd ega-htsjdk
mvn package -DskipTests
mvn install:install-file -Dfile=./target/ega-htsjdk-1.0-SNAPSHOT.jar -DgroupId=eu.elixir.ega.ebi -DartifactId=ega-htsjdk -Dversion=1.0-SNAPSHOT -Dpackaging=jar

```

3. Run htsget service using an IDE or in your terminal for using that in biosamples. Note that the HTSGET service will run on port 8080

4. Run biosamples
```bash
#run biosamples
cd biosamples-v4-demo
sudo ./docker-integration.sh

# run example
curl localhost:8081/biosamples/samples/ga4gh?disease=lukemia&page=0 
```
 
