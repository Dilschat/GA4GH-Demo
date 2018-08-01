/*
 * The MIT License
 *
 * Copyright (c) 2013 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package htsjdk.samtools.seekablestream.cipher;

import htsjdk.HtsjdkTest;
import htsjdk.samtools.seekablestream.SeekableFileStream;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.util.io.pem.PemReader;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.*;
import java.security.Security;
import java.util.Arrays;
import java.util.Iterator;

public class GPGCipherStreamTest extends HtsjdkTest {

    @BeforeTest
    private void init() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void testGPGSymmetricEncryptionOfPlainFileOutputStream() throws Exception {
        File file = new File("src/test/resources/htsjdk/samtools/seekablestream/cipher/lorem.raw");
        SeekableFileStream seekableFileStream = new SeekableFileStream(file);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OutputStream gpgSymmetricCipherOutputStream = new GPGSymmetricCipherOutputStream(outputStream, "password", file.getName());
        IOUtils.copyLarge(seekableFileStream, gpgSymmetricCipherOutputStream);
        gpgSymmetricCipherOutputStream.flush();
        gpgSymmetricCipherOutputStream.close();
        // check for header to be correct, don't decrypt the whole file
        Assert.assertEquals(Arrays.copyOfRange(outputStream.toByteArray(), 0, 6), new byte[]{-116, 13, 4, 9, 3, 2});
    }

    @Test
    public void testGPGAsymmetricEncryptionOfPlainFileOutputStream() throws Exception {
        PGPPublicKey pgpPublicKey = readPublicKey("src/test/resources/htsjdk/samtools/seekablestream/cipher/public.key");
        File file = new File("src/test/resources/htsjdk/samtools/seekablestream/cipher/lorem.raw");
        SeekableFileStream seekableFileStream = new SeekableFileStream(file);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OutputStream gpgSymmetricCipherOutputStream = new GPGAsymmetricCipherOutputStream(outputStream, pgpPublicKey, file.getName());
        IOUtils.copyLarge(seekableFileStream, gpgSymmetricCipherOutputStream);
        gpgSymmetricCipherOutputStream.flush();
        gpgSymmetricCipherOutputStream.close();
        // check for header to be correct, don't decrypt the whole file
        Assert.assertEquals(Arrays.copyOfRange(outputStream.toByteArray(), 0, 13), new byte[]{-123, 2, 12, 3, 50, 10, 33, 105, -36, -54, -126, -127, 1});
    }

    @Test
    public void testGPGSymmetricEncryptionOfPlainFileInputStream() throws Exception {
        File file = new File("src/test/resources/htsjdk/samtools/seekablestream/cipher/lorem.raw");
        SeekableFileStream seekableFileStream = new SeekableFileStream(file);
        GPGSymmetricCipherInputStream gpgSymmetricCipherStream = new GPGSymmetricCipherInputStream(seekableFileStream, "password", file.getName());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IOUtils.copyLarge(gpgSymmetricCipherStream, outputStream);
        gpgSymmetricCipherStream.close();
        outputStream.close();
        // check for header to be correct, don't decrypt the whole file
        Assert.assertEquals(Arrays.copyOfRange(outputStream.toByteArray(), 0, 6), new byte[]{-116, 13, 4, 9, 3, 2});
    }

    @Test
    public void testGPGAsymmetricEncryptionOfPlainFileInputStream() throws Exception {
        PGPPublicKey pgpPublicKey = readPublicKey("src/test/resources/htsjdk/samtools/seekablestream/cipher/public.key");
        File file = new File("src/test/resources/htsjdk/samtools/seekablestream/cipher/lorem.raw");
        SeekableFileStream seekableFileStream = new SeekableFileStream(file);
        GPGAsymmetricCipherInputStream gpgAsymmetricCipherStream = new GPGAsymmetricCipherInputStream(seekableFileStream, pgpPublicKey, file.getName());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IOUtils.copyLarge(gpgAsymmetricCipherStream, outputStream);
        gpgAsymmetricCipherStream.close();
        outputStream.close();
        // check for header to be correct, don't decrypt the whole file
        Assert.assertEquals(Arrays.copyOfRange(outputStream.toByteArray(), 0, 13), new byte[]{-123, 2, 12, 3, 50, 10, 33, 105, -36, -54, -126, -127, 1});
    }

    @Test
    public void testGPGEncryptionOfAESDecryptedStream() throws Exception {
        byte[] privateKeyBytes;
        try (PemReader pemReader = new PemReader(new InputStreamReader(new FileInputStream("src/test/resources/htsjdk/samtools/seekablestream/cipher/ega.sec")))) {
            privateKeyBytes = pemReader.readPemObject().getContent();
        }
        File encryptedFile = new File("src/test/resources/htsjdk/samtools/seekablestream/cipher/lorem.aes.enc");
        SeekableFileStream encryptedFileStream = new SeekableFileStream(encryptedFile);
        SeekableAESCipherStream seekableAESCipherStream = new SeekableAESCipherStream(encryptedFileStream, privateKeyBytes);

        PGPPublicKey pgpPublicKey = readPublicKey("src/test/resources/htsjdk/samtools/seekablestream/cipher/public.key");
        GPGAsymmetricCipherInputStream gpgAsymmetricCipherStream = new GPGAsymmetricCipherInputStream(seekableAESCipherStream, pgpPublicKey);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IOUtils.copyLarge(gpgAsymmetricCipherStream, outputStream, new byte[874]);
        gpgAsymmetricCipherStream.close();
        outputStream.close();
        // check for header to be correct, don't decrypt the whole file
        Assert.assertEquals(Arrays.copyOfRange(outputStream.toByteArray(), 0, 13), new byte[]{-123, 2, 12, 3, 50, 10, 33, 105, -36, -54, -126, -127, 1});
    }

    private PGPPublicKey readPublicKey(String publicKeyFilePath) throws IOException, PGPException {
        InputStream in = new FileInputStream(new File(publicKeyFilePath));
        in = PGPUtil.getDecoderStream(in);
        PGPPublicKeyRingCollection pgpPublicKeyRings = new PGPPublicKeyRingCollection(in, new BcKeyFingerprintCalculator());
        PGPPublicKey pgpPublicKey = null;
        Iterator keyRings = pgpPublicKeyRings.getKeyRings();
        while (pgpPublicKey == null && keyRings.hasNext()) {
            PGPPublicKeyRing kRing = (PGPPublicKeyRing) keyRings.next();
            Iterator publicKeys = kRing.getPublicKeys();
            while (publicKeys.hasNext()) {
                PGPPublicKey key = (PGPPublicKey) publicKeys.next();
                if (key.isEncryptionKey()) {
                    pgpPublicKey = key;
                    break;
                }
            }
        }
        if (pgpPublicKey == null) {
            throw new IllegalArgumentException("Can't find encryption key in key ring.");
        }
        return pgpPublicKey;
    }

}
