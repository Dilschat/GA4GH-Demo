package htsjdk.samtools.seekablestream.cipher;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.openpgp.*;

import java.io.*;
import java.util.concurrent.TimeUnit;

public abstract class GPGCipherInputStream<T> extends InputStream {

    private static final int TIMEOUT = 100;

    private final TimeLimiter timeLimiter;
    private final InputStream seekableInputStream;
    private final OutputStream encryptedOutStream;
    private final OutputStream compressedOutputStream;
    private final OutputStream literalDataOutStream;
    private final PipedInputStream pipedInputStream;
    private final PipedOutputStream pipedOutputStream;

    public GPGCipherInputStream(InputStream inputStream, T key) throws IOException, PGPException {
        this(inputStream, key, "");
    }

    public GPGCipherInputStream(InputStream inputStream, T key, String filename) throws IOException, PGPException {
        this.timeLimiter = new SimpleTimeLimiter();
        this.seekableInputStream = inputStream;
        PGPEncryptedDataGenerator encryptedDataGenerator = getPGPEncryptedDataGenerator(key);
        this.pipedInputStream = new PipedInputStream();
        this.pipedOutputStream = new PipedOutputStream(pipedInputStream);
        this.encryptedOutStream = encryptedDataGenerator.open(pipedOutputStream, new byte[1 << 16]);
        PGPCompressedDataGenerator compressedDataGenerator = new PGPCompressedDataGenerator(PGPCompressedData.ZLIB);
        this.compressedOutputStream = compressedDataGenerator.open(encryptedOutStream);
        PGPLiteralDataGenerator literalDataGenerator = new PGPLiteralDataGenerator();
        this.literalDataOutStream = literalDataGenerator.open(compressedOutputStream, PGPLiteralData.BINARY, String.valueOf(filename), PGPLiteralData.NOW, new byte[1 << 16]);
        new Thread(() -> {
            try {
                IOUtils.copyLarge(seekableInputStream, literalDataOutStream);
            } catch (IOException ignored) {
            }
        }).start();
    }

    protected abstract PGPEncryptedDataGenerator getPGPEncryptedDataGenerator(T key);

    @Override
    public int read() throws IOException {
        try {
            return timeLimiter.callWithTimeout(pipedInputStream::read, TIMEOUT, TimeUnit.MILLISECONDS, true);
        } catch (UncheckedTimeoutException e) {
            literalDataOutStream.flush();
            literalDataOutStream.close();
            compressedOutputStream.close();
            encryptedOutStream.close();
            pipedOutputStream.close();
            return -1;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public int available() throws IOException {
        return pipedInputStream.available();
    }

    public void close() throws IOException {
        seekableInputStream.close();
    }

}
