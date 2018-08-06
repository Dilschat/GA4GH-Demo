package test;

import eu.elixir.ega.ebi.dataedge.dto.ena.dto.RawTicket;
import eu.elixir.ega.ebi.dataedge.service.ena.htsget.service.internal.ENATicketService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;


@RunWith(SpringRunner.class)
public class ENATicketServiceTest {
    @Test
    public void testTicket1(){
        ENATicketService linkService = new ENATicketService();
        RawTicket link = linkService.getLinkToFile("SAMN07666497","BAM");
        assertEquals(link.getFtpLink(), Arrays.asList("ftp.sra.ebi.ac.uk/vol1/fastq/SRR605/008/SRR6051208/SRR6051208.fastq.gz"));
        assertEquals(link.getBytesInFile(),Arrays.asList("1508774187"));
        assertEquals(link.getMd5Hash(),Arrays.asList("bd1a36383f9b5b7ff5a8df79e85de245"));
    }

    @Test
    public void testTicket2(){
        ENATicketService linkService = new ENATicketService();
        RawTicket link = linkService.getLinkToFile("SAMN07666499","BAM");
        assertEquals(link.getFtpLink(),Arrays.asList("ftp.sra.ebi.ac.uk/vol1/fastq/SRR605/006/SRR6051206/SRR6051206.fastq.gz"));
        assertEquals(link.getBytesInFile(),Arrays.asList("1873381403"));
        assertEquals(link.getMd5Hash(),Arrays.asList("bd9dea1db421daf7e6862041a3330ffb"));
    }

    @Test
    public void testTicket3(){
        ENATicketService linkService = new ENATicketService();
        RawTicket link = linkService.getLinkToFile("SAMEA92031418","BAM");
        assertEquals(link.getFtpLink(),Arrays.asList("ftp.sra.ebi.ac.uk/vol1/fastq/ERR184/007/ERR1841057/ERR1841057_1.fastq.gz","ftp.sra.ebi.ac.uk/vol1/fastq/ERR184/007/ERR1841057/ERR1841057_2.fastq.gz"));
        assertEquals(link.getBytesInFile(),Arrays.asList("893703903","886262011"));
        assertEquals(link.getMd5Hash(),Arrays.asList("f0a035e6f13cc175db65aefd87bef5f8","b0b47ea33d33b458db602d3ac8001d8b"));
    }
}