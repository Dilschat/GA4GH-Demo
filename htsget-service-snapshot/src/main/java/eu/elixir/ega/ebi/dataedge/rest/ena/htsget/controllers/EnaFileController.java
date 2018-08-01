package eu.elixir.ega.ebi.dataedge.rest.ena.htsget.controllers;

import eu.elixir.ega.ebi.dataedge.config.NotFoundException;
import eu.elixir.ega.ebi.dataedge.dto.ena.dto.RawTicket;
import eu.elixir.ega.ebi.dataedge.service.ena.htsget.service.internal.ENAFtpDownloader;
import eu.elixir.ega.ebi.dataedge.service.ena.htsget.service.internal.ENATicketService;
import eu.elixir.ega.ebi.dataedge.service.ena.htsget.service.internal.FastqConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;


@RestController
@RequestMapping("/sample")
public class EnaFileController {

    private ENATicketService ticketService;
    private ENAFtpDownloader downloader;
    private FastqConverter converter;
    private static final String HEADER_KEY = "Content-Disposition";
    private static final String HEADER_VALUE = "attachment; filename=\"%s_%s.%s\"";

    @Autowired
    public EnaFileController(ENATicketService ticketService, ENAFtpDownloader downloader, FastqConverter converter) {
        this.ticketService = ticketService;
        this.downloader = downloader;
        this.converter = converter;
    }

    @RequestMapping()
    public void getStream(@RequestParam(name = "accession") String accession,
                          @RequestParam(name = "format", defaultValue = "BAM") String format,
                          @RequestParam(name = "part") String part,
                          HttpServletResponse response) throws IOException {

        RawTicket ticket = ticketService.getLinkToFile(accession, format);
        ServletOutputStream responseStream = response.getOutputStream();
        List<String> urls = ticket.getFtpLink();
        if(!ticketService.isPartOfFileExist(urls,part)){
            throw new NotFoundException("This part of file does not exist",part);
        }
        InputStream fileStream = downloader.getFastqFile(urls.get(Integer.parseInt(part) - 1));
        if(format.equals("BAM")) {
           converter.convertToBam(fileStream, responseStream);
        }else if(format.equals("CRAM")) {
            converter.convertToCram(fileStream, responseStream);
        }
    }

    @RequestMapping("/download")
    public void getFile(@RequestParam(name = "accession") String accession,
                        @RequestParam(name = "format", defaultValue = "BAM") String format,
                        @RequestParam(name = "part") String part,
                        HttpServletResponse response) throws IOException {

        RawTicket ticket = ticketService.getLinkToFile(accession, format);
        ServletOutputStream responseStream = response.getOutputStream();
        response.setHeader(HEADER_KEY, String.format(HEADER_VALUE, accession, part, format));
        List<String> urls = ticket.getFtpLink();
        if(!ticketService.isPartOfFileExist(urls,part)){
                throw new NotFoundException("This part of file does not exist",part);
        }
        InputStream fileStream = downloader.getFastqFile(urls.get(Integer.parseInt(part) - 1));
        if(format.equals("BAM")) {
            converter.convertToBam(fileStream, responseStream);
        }
        else if(format.equals("CRAM")){
            converter.convertToCram(fileStream, responseStream);
        }
    }

}
