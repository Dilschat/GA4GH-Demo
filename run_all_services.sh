cd biosamples-v4-demo
#running htsget service

docker-compose up -d htsget

#running biosamples

sudo ./docker-integration.sh
# run example
curl localhost:8081/biosamples/samples/ga4gh?disease=lukemia&page=0 