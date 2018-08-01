package eu.elixir.ega.ebi.dataedge.rest.ena.htsget.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.elixir.ega.ebi.dataedge.config.UnsupportedFormatException;
import eu.elixir.ega.ebi.dataedge.dto.ena.dto.RawTicket;
import eu.elixir.ega.ebi.dataedge.service.ena.htsget.service.internal.ENATicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ga4gh")
public class ENATicketController {

    private ENATicketService linkService;

    @Autowired
    ENATicketController(ENATicketService linkService){
        this.linkService = linkService;
    }

    @RequestMapping(value = "sample/{Biosample_ID}", method = RequestMethod.GET, produces = "application/json")
    public RawTicket getTicket(@PathVariable("Biosample_ID") String biosampleID,
                               @RequestParam(name = "format", required = false, defaultValue = "BAM") String format) throws JsonProcessingException {
        if(!(format.equals("BAM")||format.equals("CRAM"))){
            throw new UnsupportedFormatException(format);
        }
        return linkService.getLinkToFile(biosampleID,format);
    }

}
