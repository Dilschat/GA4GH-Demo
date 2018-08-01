package htsjdk.samtools.seekablestream.cipher;

import org.bouncycastle.openpgp.*;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract class GPGCipherOutputStream<T> extends FilterOutputStream {

    private final OutputStream encryptedOutStream;
    private final OutputStream compressedOutputStream;

    public GPGCipherOutputStream(OutputStream outputStream, T key) throws IOException, PGPException {
        this(outputStream, key, "");
    }

    public GPGCipherOutputStream(OutputStream outputStream, T key, String filename) throws IOException, PGPException {
        super(null);
        PGPEncryptedDataGenerator encryptedDataGenerator = getPGPEncryptedDataGenerator(key);
        this.encryptedOutStream = encryptedDataGenerator.open(outputStream, new byte[1 << 16]);
        PGPCompressedDataGenerator compressedDataGenerator = new PGPCompressedDataGenerator(PGPCompressedData.ZLIB);
        this.compressedOutputStream = compressedDataGenerator.open(encryptedOutStream);
        PGPLiteralDataGenerator literalDataGenerator = new PGPLiteralDataGenerator();
        this.out = literalDataGenerator.open(compressedOutputStream, PGPLiteralData.BINARY, String.valueOf(filename), PGPLiteralData.NOW, new byte[1 << 16]);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
        compressedOutputStream.flush();
        encryptedOutStream.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
        compressedOutputStream.close();
        encryptedOutStream.close();
    }

    protected abstract PGPEncryptedDataGenerator getPGPEncryptedDataGenerator(T key);

}
