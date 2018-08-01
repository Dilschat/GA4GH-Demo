package eu.elixir.ega.ebi.dataedge.service.ena.htsget.service;

import eu.elixir.ega.ebi.dataedge.dto.ena.dto.RawTicket;

public interface SequenceLinkService {
    public RawTicket getLinkToFile(String accession, String format);
}
