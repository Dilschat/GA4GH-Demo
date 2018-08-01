package eu.elixir.ega.ebi.dataedge.dto.ena.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import eu.elixir.ega.ebi.dataedge.service.ena.htsget.service.internal.TicketSerializer;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Raw ticket to sequences. Can be serialized according htsget specs
 */
@JsonSerialize(using = TicketSerializer.class)
public class RawTicket {
    private String accession;
    private List<String> ftpLink;
    private List<String> bytesInFile;
    private List<String> md5Hashs;
    private String format;

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public List<String> getFtpLink() {
        return ftpLink;
    }

    public void setFtpLink(String ftpLink) {
        this.ftpLink = Arrays.asList(ftpLink.split(";"));
    }

    public List<String> getBytesInFile() {
        return bytesInFile;
    }

    public void setBytesInFile(String bytesInFile) {
        this.bytesInFile = Arrays.asList(bytesInFile.split(";"));
    }

    public List<String> getMd5Hash() {
        return md5Hashs;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setMd5Hash(String md5Hash) {
        this.md5Hashs = Arrays.asList(md5Hash.split(";"));
    }

    public String getOverallHash() {
        if (md5Hashs.size() == 0 || md5Hashs.size() > 1) {
            return "";
        } else {
            return md5Hashs.get(0); // return hash only if there only one file(hash)
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RawTicket that = (RawTicket) o;
        return bytesInFile == that.bytesInFile &&
                Objects.equals(ftpLink, that.ftpLink) &&
                Objects.equals(md5Hashs, that.md5Hashs);
    }

    @Override
    public int hashCode() {

        return Objects.hash(ftpLink, bytesInFile, md5Hashs);
    }
}
