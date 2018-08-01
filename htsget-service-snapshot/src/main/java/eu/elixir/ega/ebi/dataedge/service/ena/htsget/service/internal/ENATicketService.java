package eu.elixir.ega.ebi.dataedge.service.ena.htsget.service.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsunsoft.http.HttpRequest;
import com.jsunsoft.http.HttpRequestBuilder;
import com.jsunsoft.http.ResponseDeserializer;
import com.jsunsoft.http.ResponseHandler;
import eu.elixir.ega.ebi.dataedge.config.UnsupportedFormatException;
import eu.elixir.ega.ebi.dataedge.dto.ena.dto.RawTicket;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Class for creating ticket (htsget)
 */
@Service
public class ENATicketService {

    /**
     * gets link to fastq file for given accession and additional data for this accession (size of file and md5 hash),
     * thats represented by raw ticket that can be serialized to json ticket according htsget specs
     *
     * @param accession biosample_id
     * @param format    format of file
     * @return raw tiket to sequences
     */
    public RawTicket getLinkToFile(String accession, String format) {
        //TODO move link to properties
        if(!isThisSupportedFormat(format)){
            throw new UnsupportedFormatException("Format is not supported. Should be BAM or CRAM");
        }
        HttpRequest<String> request = HttpRequestBuilder.
                createGet(String.format("https://www.ebi.ac.uk/ena/portal/api/filereport?result=read_run&accession=%s&format=json",accession), String.class)
                .responseDeserializer(ResponseDeserializer.ignorableDeserializer())
                .build();
        ResponseHandler<String> responseHandler = request.execute();
        String jsonResponse = responseHandler.get();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = null;
        try {
            node = mapper.readTree(jsonResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String fastq_ftp = "";
        String fastq_bytes = "";
        String fastq_md5 = "";
        Iterator<JsonNode> nodes = node.iterator();

        while(nodes.hasNext()) {
            JsonNode currentNode = nodes.next();
            String current_fastq_ftp = currentNode.findValue("fastq_ftp").textValue();
            String current_fastq_bytes = currentNode.findValue("fastq_bytes").textValue();
            String current_fastq_md5 = currentNode.findValue("fastq_md5").textValue();
            if(!fastq_ftp.isEmpty()) {
                fastq_ftp = fastq_ftp.concat(";" + current_fastq_ftp);
            }else{
                fastq_ftp = current_fastq_ftp;
            }
            if(!fastq_bytes.isEmpty()) {
                fastq_bytes = fastq_bytes.concat(";" + current_fastq_bytes);
            }else {
                fastq_bytes = current_fastq_bytes;
            }
            if(fastq_md5.isEmpty()){
                fastq_md5 = current_fastq_md5;
            }else {
                fastq_md5 = fastq_md5.concat(";"+current_fastq_md5);
            }
        }

        RawTicket link = new RawTicket();
        link.setAccession(accession);
        link.setFtpLink(fastq_ftp);
        link.setBytesInFile(fastq_bytes);
        link.setMd5Hash(fastq_md5);
        link.setFormat(format);
        return link;
    }

    public boolean isPartOfFileExist(List<String>urls,String part){
        return urls.size() >= Integer.parseInt(part);
    }
    private boolean isThisSupportedFormat(String format){
        return format.equals("BAM") || format.equals("CRAM");
    }
}

