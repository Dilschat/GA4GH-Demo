package htsjdk.samtools.seekablestream.ebi;

import java.io.IOException;

public interface BufferedInputStream {

    int read(byte[] bytes) throws IOException;

    int available();

    int getBufferSize();

}
