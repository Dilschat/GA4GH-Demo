#running htsget service

# TODO write script for runnung ega-dataedge

#running biosamples
cd biosamples-v4-demo
sudo ./docker-integration.sh
# run example
curl localhost:8081/biosamples/samples/ga4gh?disease=lukemia&page=0 