# How to deploy Demo application locally

*NOTE: This instruction assumes that you already have local working biosamples service.*


You can use for setting up local versions of biosamples and htsget service using [build_external_libs.sh](./build_external_libs.sh). This setting up needed only for first running. Then for deploying all required services use [run_all_services.sh](./run_all_services.sh). You can deploy this demo manually as alternative following instructions below:


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

3. Run htsget service using an IDE or in docker container:
```bash
#run htsget
docker-compose up -d htsget
```

 Note that the HTSGET service will run on port 8086

4. Run biosamples
```bash
#run biosamples
cd biosamples-v4-demo
sudo ./docker-integration.sh

# run example
curl localhost:8081/biosamples/samples/ga4gh?disease=lukemia&page=0 
```
 
