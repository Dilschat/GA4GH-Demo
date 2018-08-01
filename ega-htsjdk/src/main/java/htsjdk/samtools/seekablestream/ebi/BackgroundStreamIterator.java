package htsjdk.samtools.seekablestream.ebi;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

class BackgroundStreamIterator implements Iterator<byte[]> {

    private BufferedInputStream bytes;

    public BackgroundStreamIterator(BufferedInputStream bytes) {
        this.bytes = bytes;
    }

    @Override
    public boolean hasNext() {
        return bytes.available() > 0;
    }

    @Override
    public byte[] next() {
        byte[] next = new byte[bytes.getBufferSize()];
        int length = 0;
        try {
            length = bytes.read(next);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (length != next.length) {
            next = Arrays.copyOf(next, length);
        }
        return next;
    }

}
