package eu.elixir.ega.ebi.dataedge.service.ena.htsget.service.internal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import eu.elixir.ega.ebi.dataedge.dto.ena.dto.RawTicket;

import java.io.IOException;

public class TicketSerializer extends StdSerializer<RawTicket> {

    private final String SERVICE_URL = "http://DOWNLOADER";

    protected TicketSerializer() { super(RawTicket.class);}

    protected TicketSerializer(Class<RawTicket> t) {
        super(t);
    }

    protected TicketSerializer(JavaType type) {
        super(type);
    }

    protected TicketSerializer(Class<?> t, boolean dummy) {
        super(t, dummy);
    }

    protected TicketSerializer(StdSerializer<?> src) {
        super(src);
    }

    @Override
    public void serialize(RawTicket linkToSequence, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeFieldName("htsget");
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("format", linkToSequence.getFormat());
        jsonGenerator.writeArrayFieldStart("urls");
        Integer i = 1;
        for (String url : linkToSequence.getFtpLink()) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("url", String.format("%s/sample?accession=%s&format=%s&part=%s", SERVICE_URL, linkToSequence.getAccession(), linkToSequence.getFormat(), i.toString()));
            jsonGenerator.writeEndObject();
            i++;
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeStringField("md5Hash", linkToSequence.getOverallHash());
        jsonGenerator.writeEndObject();
        jsonGenerator.writeEndObject();



    }
}
