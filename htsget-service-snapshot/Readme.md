# EGA.Data.API.v3.DATAEDGE

This is the Edge Server (DATAEDGE). It enforces user authentication for some edpoints by requiring an EGA Bearer Token for API access. DATAEDGE is the service providing streaming file downloads and provides the back end for a FUSE layer implementation. Data downloads can be served as whole-file downloads, or be specifying byte ranges of the underlying file.

This project contains the EGA API public GPG key so that i can validate information provided by Central EGA (CEGA) services, which will sign the content with the EGA API provate key.

Dependency: 
* CONFIG (`https://github.com/EGA-archive/ega-config-server`). The `'bootstrap.properties'` file must be modified to point to a running configuration service, which will be able to serve the `application.properties` file for this service `DATAEDGE`
* EUREKA (`https://github.com/EGA-archive/ega-eureka-service`). DATAEDGE service will contact the other services via EUREKA and registers itself with it.
* PERMISSIONSDATABASE (``). This service provides certain information to advanced data access.
* FILEDATABASE (`https://github.com/EGA-archive/ega-filedatabase-service`). This service handles download requests and logging. Users can download previously requested files. Downloads are logged via DOWNLOADER.
* RES (`https://github.com/EGA-archive/ega-res`). All downloads originate from the RES service, which prepares all outgoing data so that it is encrypted, either with a user's public GPG key or with the key selected upon requesting the download.
* EGA AAI. This is an Authentication and Authorisation Infrastructure service (OpenID Connect IdP) available at Central EGA.

### Documentation

This is the edge service to provide direct access to EGA archive files, via RES. This service offers endpoints secured by OAuth2 tokens for direct access to files, and unsecured endpoints for downloading request tickets (which is the main functionality).

[GET]	`/stats/load`
[GET]	`/files/{file_id}` [id, destinationFormat, destinationKey, startCoordinate, endCoordinate]` Supports Range header
[HEAD]	`/files/{file_id}` 
[GET] 	`/files/{file_id}/header`

[GET] 	`/files/byid/{type}` [accession format chr start end fields tags notags header destinationFormat destinationKey]
[GET] 	`/variant/byid/{type}` [accession format chr start end fields tags notags header destinationFormat destinationKey]

[GET] 	`/tickets/files/{file_id}` [format referenceIndex referenceName referenceMD5 start end fields tags notags]
[GET] 	`/tickets/variants/{file_id}` [format referenceIndex referenceName referenceMD5 start end fields tags notags]

[GET]	`/metadata/datasests` 
[GET]	`/metadata/datasests/{dataset_id}/files`
[GET]	`/metadata/files/{file_id}`

[GET]	`/session/{session_id}`

### Todos

 - Write Tests
- Speed up / chunk htsget

### EXPERIMENTAL Deploy

The service can be deployed directly to a Docker container, using these instructions:

`wget https://raw.github.com/elixir-europe/ega-data-api-v3-dataedge/master/docker/runfromsource.sh`  
`wget https://raw.github.com/elixir-europe/ega-data-api-v3-dataedge/master/docker/build.sh`  
`chmod +x runfromsource.sh`  
`chmod +x build.sh`  
`./runfromsource.sh`  

These commands perform a series of actions:  
	1. Pull a build environment from Docker Hub  
	2. Run the 'build.sh' script inside of a transient build environment container.  
	3. The source code is pulled from GitHub and built  
	4. A Deploy Docker Image is built and the compiled service is added to the image  
	5. The deploy image is started; the service is automatically started inside the container  

The Docker image can also be obtained directly from Docker Hub:  

`sudo docker run -d -p 9059:9059 alexandersenf/ega_dataedge`  or by running the `./runfromimage.sh` file.